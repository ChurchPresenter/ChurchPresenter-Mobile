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
import com.church.presenter.churchpresentermobile.MainActivity
import com.church.presenter.churchpresentermobile.R
import com.church.presenter.churchpresentermobile.model.AUTO_FIT_FLOOR
import com.church.presenter.churchpresentermobile.model.BODY_LINE_HEIGHT
import com.church.presenter.churchpresentermobile.model.FOOTER_GAP
import com.church.presenter.churchpresentermobile.model.FOOTER_WIDTH_FRACTION
import com.church.presenter.churchpresentermobile.model.REFERENCE_GAP
import com.church.presenter.churchpresentermobile.model.REFERENCE_TRACKING
import com.church.presenter.churchpresentermobile.model.REFERENCE_WIDTH_FRACTION
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideTextAlign
import com.church.presenter.churchpresentermobile.model.SlideVerticalAlign
import com.church.presenter.churchpresentermobile.model.displayBody
import com.church.presenter.churchpresentermobile.model.showsReference
import com.church.presenter.churchpresentermobile.model.widthFraction
import com.church.presenter.churchpresentermobile.present.NowShowing
import com.church.presenter.churchpresentermobile.present.ScreenState

/** The size of the picture; the system's media player crops it to its own shape. */
private const val ART_WIDTH = 800
private const val ART_HEIGHT = 450

/**
 * Shown as the player's title while a slide is up. A blank one would make the
 * system write "ChurchPresenter is running" across the picture.
 */
private const val NO_TITLE = "\u200B"

/**
 * The presenting notification: a media player with the slide as its album art and
 * Close as its one button, and no words, since the picture says what is on screen.
 * With nothing loaded there is no picture, so it says the screens are connected
 * and, where there is one, gives the browser address to type.
 *
 * The picture is drawn from the slide's own colours and words rather than
 * captured: the slide lives on another display (a Cast receiver, an HDMI
 * screen) that this process cannot read back.
 */
internal fun buildPresentationNotification(
    context: Context,
    url: String?,
    nowShowing: NowShowing?,
    media: PresentationMediaSession?,
): Notification {
    val openApp = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Notification.Builder(context, PRESENTATION_CHANNEL_ID)
    } else {
        @Suppress("DEPRECATION")
        Notification.Builder(context)
    }
    builder
        .setSmallIcon(android.R.drawable.ic_menu_slideshow)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setShowWhen(false)
        .setContentIntent(openApp)
        .addAction(
            Notification.Action.Builder(
                Icon.createWithResource(context, android.R.drawable.ic_menu_close_clear_cancel),
                context.getString(R.string.presentation_close_app),
                PresentationForegroundService.closeAppIntent(context),
            ).build()
        )
    val style = Notification.MediaStyle().setShowActionsInCompactView(0)
    media?.let { style.setMediaSession(it.token) }
    builder.setStyle(style)

    // Drawing can fail on a starved device; a notification without its picture
    // still does its job, one that fails to post does not.
    val art = nowShowing?.let { runCatching { slideArt(it) }.getOrNull() }
    val idleTitle = context.getString(R.string.presentation_service_title)
    media?.show(art, if (art != null) NO_TITLE else idleTitle)
    if (art != null) {
        builder.setLargeIcon(art)
    } else {
        builder
            .setContentTitle(idleTitle)
            .setContentText(url ?: context.getString(R.string.presentation_service_text))
    }
    return builder.build()
}

/** The slide as a picture, edge to edge. */
private fun slideArt(nowShowing: NowShowing): Bitmap {
    val bitmap = Bitmap.createBitmap(ART_WIDTH, ART_HEIGHT, Bitmap.Config.ARGB_8888)
    drawSlide(Canvas(bitmap), nowShowing, ART_WIDTH.toFloat(), ART_HEIGHT.toFloat())
    return bitmap
}

/**
 * The slide at [width] × [height], laid out by the same fractions as the renderer
 * the room sees: its size of words, margin, alignment and reference line. Blanked
 * or held back the room sees only the backdrop, so that is all this draws.
 */
private fun drawSlide(canvas: Canvas, nowShowing: NowShowing, width: Float, height: Float) {
    val background = Paint().apply {
        shader = LinearGradient(
            0f, 0f, width, height,
            parseColor(nowShowing.backgroundTop, DEFAULT_TOP),
            parseColor(nowShowing.backgroundBottom, DEFAULT_BOTTOM),
            Shader.TileMode.CLAMP,
        )
    }
    canvas.drawRect(0f, 0f, width, height, background)

    val slide = nowShowing.slide
    val words = slide.displayBody()
    if (nowShowing.state != ScreenState.LIVE || words.isBlank()) return

    val marginX = width * slide.theme.margin.horizontal
    val marginY = height * slide.theme.margin.vertical
    val textWidth = (width - 2 * marginX).toInt()
    val referenceHeight = width * REFERENCE_WIDTH_FRACTION * LINE_HEIGHT_OF_ONE
    val footerHeight = width * FOOTER_WIDTH_FRACTION * LINE_HEIGHT_OF_ONE
    val zoneHeight = height - 2 * marginY - height * (REFERENCE_GAP + FOOTER_GAP) - referenceHeight - footerHeight

    val layout = fittedWords(words, nowShowing, textWidth, width, zoneHeight)
    val top = marginY + when (slide.theme.verticalAlign) {
        SlideVerticalAlign.TOP -> 0f
        SlideVerticalAlign.MIDDLE -> (zoneHeight - layout.height) / 2
        SlideVerticalAlign.BOTTOM -> zoneHeight - layout.height
    }
    canvas.save()
    canvas.clipRect(0f, marginY, width, marginY + zoneHeight)
    canvas.translate(marginX, top)
    layout.draw(canvas)
    canvas.restore()

    val reference = slide.reference?.takeIf { it.isNotBlank() && slide.showsReference() } ?: return
    canvas.translate(marginX, marginY + zoneHeight + height * REFERENCE_GAP)
    referenceLine(reference.uppercase(), slide, textWidth, width).draw(canvas)
}

/** The words at their size, or as far below it as auto-fit may go to fit [zoneHeight]. */
private fun fittedWords(
    words: String,
    nowShowing: NowShowing,
    textWidth: Int,
    slideWidth: Float,
    zoneHeight: Float,
): StaticLayout {
    val slide = nowShowing.slide
    val full = slideWidth * slide.textSize.widthFraction
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = parseColor(nowShowing.textColor, DEFAULT_TEXT)
        textSize = full
        typeface = if (nowShowing.serif) Typeface.SERIF else Typeface.SANS_SERIF
    }
    var layout = textLayout(words, paint, textWidth, slide, BODY_LINE_HEIGHT)
    while (slide.theme.autoFitText && layout.height > zoneHeight && paint.textSize > full * AUTO_FIT_FLOOR) {
        paint.textSize *= AUTO_FIT_STEP
        layout = textLayout(words, paint, textWidth, slide, BODY_LINE_HEIGHT)
    }
    return layout
}

private fun referenceLine(reference: String, slide: Slide, textWidth: Int, slideWidth: Float): StaticLayout {
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = parseColor(slide.theme.accentColor, DEFAULT_TEXT)
        alpha = REFERENCE_OPACITY
        textSize = slideWidth * REFERENCE_WIDTH_FRACTION
        typeface = Typeface.SANS_SERIF
        letterSpacing = REFERENCE_TRACKING
    }
    return textLayout(reference, paint, textWidth, slide, 1f)
}

private fun textLayout(text: String, paint: TextPaint, width: Int, slide: Slide, lineHeight: Float): StaticLayout =
    StaticLayout.Builder
        .obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
        .setAlignment(
            when (slide.theme.textAlign) {
                SlideTextAlign.LEFT -> Layout.Alignment.ALIGN_NORMAL
                SlideTextAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
                SlideTextAlign.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
            }
        )
        .setLineSpacing(0f, lineHeight)
        .setIncludePad(false)
        .build()

private fun parseColor(hex: String, fallback: Int): Int =
    runCatching { Color.parseColor(hex) }.getOrDefault(fallback)

/** A line of small text, as a multiple of its size. */
private const val LINE_HEIGHT_OF_ONE = 1.2f
private const val AUTO_FIT_STEP = 0.9f

/** The reference's opacity, out of 255: the renderer's 0.72. */
private const val REFERENCE_OPACITY = 184
private const val DEFAULT_TOP = 0xFF2A1D5E.toInt()
private const val DEFAULT_BOTTOM = 0xFF05060D.toInt()
private const val DEFAULT_TEXT = 0xFFFFFFFF.toInt()

/** The channel the presenting notification posts to. */
internal const val PRESENTATION_CHANNEL_ID = "presentation_server"
