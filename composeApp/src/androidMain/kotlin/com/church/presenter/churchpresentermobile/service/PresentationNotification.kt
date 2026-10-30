package com.church.presenter.churchpresentermobile.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import com.church.presenter.churchpresentermobile.MainActivity
import com.church.presenter.churchpresentermobile.R
import com.church.presenter.churchpresentermobile.present.NowShowing
import com.church.presenter.churchpresentermobile.present.ScreenState

/** The presenting notification's three lines, as decided by [notificationText]. */
internal data class PresentationNotification(val title: String, val text: String, val subText: String?)

/**
 * What the notification says, read like a music player's: the song where the
 * track title goes, the words on screen under it, and LIVE / BLANK / OFF with
 * the deck position beside the app name.
 *
 * With nothing loaded there is no song to name, so it says the screens are
 * connected and, where there is one, gives the browser address to type.
 *
 * The decision the notification encodes, pulled out of `Notification.Builder`
 * so it can be tested without a device.
 */
internal fun notificationText(
    nowShowing: NowShowing?,
    url: String?,
    idleTitle: String,
    idleText: String,
    stateLabel: (ScreenState) -> String,
): PresentationNotification {
    if (nowShowing == null) return PresentationNotification(idleTitle, url ?: idleText, null)
    val state = stateLabel(nowShowing.state)
    return PresentationNotification(
        title = nowShowing.title.ifBlank { idleTitle },
        // Blanked or held back, the room sees no words — so the shade does not
        // claim it does, and points at where to go instead.
        text = when {
            nowShowing.state != ScreenState.LIVE -> url ?: idleText
            nowShowing.words.isNotBlank() -> nowShowing.words
            else -> url ?: idleText
        },
        subText = listOfNotNull(state, nowShowing.position).joinToString(" · "),
    )
}

/** Square, for the album-art spot beside the text. */
private const val ART_SQUARE_PX = 256

/** 16:9, for the expanded notification — the slide as the room sees it. */
private const val ART_WIDE_W = 640
private const val ART_WIDE_H = 360

/**
 * The presenting notification, built for [nowShowing].
 *
 * The picture is drawn from the slide's own colours and words rather than
 * captured: the slide lives on another display (a Cast receiver, an HDMI
 * screen) that this process cannot read back, and the drawing is close enough
 * to recognise the verse at a glance, which is all a notification is for.
 */
internal fun buildPresentationNotification(context: Context, url: String?, nowShowing: NowShowing?): Notification {
    val openApp = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val text = notificationText(
        nowShowing = nowShowing,
        url = url,
        idleTitle = context.getString(R.string.presentation_service_title),
        idleText = context.getString(R.string.presentation_service_text),
        stateLabel = { state ->
            context.getString(
                when (state) {
                    ScreenState.LIVE -> R.string.presentation_state_live
                    ScreenState.BLANK -> R.string.presentation_state_blank
                    ScreenState.OFF -> R.string.presentation_state_off
                }
            )
        },
    )

    val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Notification.Builder(context, PRESENTATION_CHANNEL_ID)
    } else {
        @Suppress("DEPRECATION")
        Notification.Builder(context)
    }
    builder
        .setContentTitle(text.title)
        .setContentText(text.text)
        .setSmallIcon(android.R.drawable.ic_menu_slideshow)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setShowWhen(false)
        .setContentIntent(openApp)
    text.subText?.let(builder::setSubText)

    if (nowShowing != null) {
        // Drawing can fail on a starved device; a notification without its
        // picture still does its job, one that fails to post does not.
        runCatching {
            builder.setLargeIcon(slideArt(nowShowing, ART_SQUARE_PX, ART_SQUARE_PX))
            builder.setStyle(
                Notification.BigPictureStyle()
                    .bigPicture(slideArt(nowShowing, ART_WIDE_W, ART_WIDE_H))
                    // Expanded, the wide picture replaces the square one.
                    .bigLargeIcon(null as Icon?)
            )
        }
    }
    return builder.build()
}

/**
 * The slide, small: its backdrop gradient with its words centred over it, in
 * its own colours and face. Blanked or held back the room sees only the
 * backdrop, so that is all this draws.
 */
private fun slideArt(nowShowing: NowShowing, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val background = Paint().apply {
        shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            parseColor(nowShowing.backgroundTop, DEFAULT_TOP),
            parseColor(nowShowing.backgroundBottom, DEFAULT_BOTTOM),
            Shader.TileMode.CLAMP,
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), background)

    if (nowShowing.state != ScreenState.LIVE || nowShowing.words.isBlank()) return bitmap

    val margin = width * ART_MARGIN
    val textWidth = (width - 2 * margin).toInt()
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = parseColor(nowShowing.textColor, DEFAULT_TEXT)
        textSize = height * ART_TEXT_SIZE
        typeface = if (nowShowing.serif) Typeface.SERIF else Typeface.SANS_SERIF
        setShadowLayer(height * ART_SHADOW, 0f, height * ART_SHADOW / 2, SHADOW_COLOUR)
    }
    // One slide line per drawn line, as on the screen.
    val words = nowShowing.words.replace(" / ", "\n")
    val layout = StaticLayout.Builder
        .obtain(words, 0, words.length, paint, textWidth)
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setMaxLines(ART_MAX_LINES)
        .setEllipsize(TextUtils.TruncateAt.END)
        .build()
    canvas.save()
    canvas.translate(margin, (height - layout.height) / 2f)
    layout.draw(canvas)
    canvas.restore()
    return bitmap
}

private fun parseColor(hex: String, fallback: Int): Int =
    runCatching { Color.parseColor(hex) }.getOrDefault(fallback)

private const val ART_MARGIN = 0.08f
private const val ART_TEXT_SIZE = 0.14f
private const val ART_SHADOW = 0.03f
private const val ART_MAX_LINES = 4
private const val SHADOW_COLOUR = 0x8C000000.toInt()
private const val DEFAULT_TOP = 0xFF2A1D5E.toInt()
private const val DEFAULT_BOTTOM = 0xFF05060D.toInt()
private const val DEFAULT_TEXT = 0xFFFFFFFF.toInt()

/** The channel the presenting notification posts to. */
internal const val PRESENTATION_CHANNEL_ID = "presentation_server"
