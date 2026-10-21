package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.display_picker_hue
import churchpresentermobile.composeapp.generated.resources.display_picker_opacity
import churchpresentermobile.composeapp.generated.resources.display_picker_saturation
import com.church.presenter.churchpresentermobile.ui.HEX_RADIX
import org.jetbrains.compose.resources.stringResource
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Saturation across, brightness down, for the current hue. */
@Composable
internal fun SaturationSquare(hue: Float, saturation: Float, value: Float, onChange: (Float, Float) -> Unit) {
    val description = stringResource(Res.string.display_picker_saturation)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(SQUARE_HEIGHT)
            .testTag(DisplayTextTags.PICKER_SQUARE)
            .semantics { contentDescription = description }
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f))))
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            .trackDrag { position, width, height ->
                onChange((position.x / width).coerceIn(0f, 1f), 1f - (position.y / height).coerceIn(0f, 1f))
            },
    ) {
        Thumb(
            x = inset(maxWidth, saturation),
            y = inset(maxHeight, 1f - value),
            fill = Color.hsv(hue, saturation, value),
        )
    }
}

@Composable
internal fun HueBar(hue: Float, onChange: (Float) -> Unit) {
    val description = stringResource(Res.string.display_picker_hue)
    Bar(
        fill = Brush.horizontalGradient(HUE_STOPS),
        fraction = hue / FULL_TURN,
        thumb = Color.hsv(hue, 1f, 1f),
        tag = DisplayTextTags.PICKER_HUE,
        description = description,
    ) { onChange(it * FULL_TURN) }
}

@Composable
internal fun AlphaBar(opaque: Color, alpha: Float, onChange: (Float) -> Unit) {
    val description = stringResource(Res.string.display_picker_opacity)
    Bar(
        fill = Brush.horizontalGradient(listOf(opaque.copy(alpha = 0f), opaque)),
        fraction = alpha,
        thumb = opaque.copy(alpha = alpha),
        tag = DisplayTextTags.PICKER_ALPHA,
        description = description,
        under = PICKER_CHECKER,
    ) { onChange(it) }
}

@Composable
private fun Bar(
    fill: Brush,
    fraction: Float,
    thumb: Color,
    tag: String,
    description: String,
    under: Color = Color.Transparent,
    onChange: (Float) -> Unit,
) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(BAR_HEIGHT)
            .testTag(tag)
            .semantics { contentDescription = description }
            .trackDrag { position, width, _ -> onChange((position.x / width).coerceIn(0f, 1f)) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(BAR_TRACK)
                .clip(RoundedCornerShape(BAR_TRACK / 2))
                .background(under)
                .background(fill),
        )
        Thumb(x = inset(maxWidth, fraction), y = maxHeight / 2, fill = thumb)
    }
}

/** [fraction] of [length], kept half a thumb from either end so the thumb is never cut off. */
private fun inset(length: Dp, fraction: Float): Dp = THUMB / 2 + (length - THUMB) * fraction.coerceIn(0f, 1f)

/** A white-ringed circle centred on (x, y). */
@Composable
private fun Thumb(x: Dp, y: Dp, fill: Color) {
    Box(
        Modifier
            .offset(x = x - THUMB / 2, y = y - THUMB / 2)
            .size(THUMB)
            .clip(CircleShape)
            .background(Color.White)
            .padding(3.dp)
            .clip(CircleShape)
            .background(fill.copy(alpha = 1f)),
    )
}

/** Reports every position of a press and a drag, in pixels, with the area's size. */
private fun Modifier.trackDrag(onPosition: (Offset, Float, Float) -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown()
        val width = size.width.toFloat()
        val height = size.height.toFloat()
        onPosition(down.position, width, height)
        drag(down.id) { change ->
            change.consume()
            onPosition(change.position, width, height)
        }
    }
}

/** A colour as hue, saturation, value and alpha — the axes the picker's controls move along. */
internal data class Hsva(val hue: Float, val saturation: Float, val value: Float, val alpha: Float) {
    companion object {
        fun of(color: Color): Hsva {
            val maxC = max(color.red, max(color.green, color.blue))
            val minC = min(color.red, min(color.green, color.blue))
            val delta = maxC - minC
            val hue = when {
                delta == 0f -> 0f
                maxC == color.red -> SIXTY * (((color.green - color.blue) / delta).mod(HUE_SECTORS))
                maxC == color.green -> SIXTY * (((color.blue - color.red) / delta) + 2f)
                else -> SIXTY * (((color.red - color.green) / delta) + 4f)
            }
            return Hsva(
                hue = hue.coerceIn(0f, FULL_TURN),
                saturation = if (maxC == 0f) 0f else delta / maxC,
                value = maxC,
                alpha = color.alpha,
            )
        }
    }
}

/** `#RRGGBB` when opaque, `#AARRGGBB` otherwise — the forms [parseHexColor] reads. */
internal fun colorToHex(color: Color): String {
    fun channel(v: Float) = (v * PICKER_MAX_CHANNEL).roundToInt().coerceIn(0, PICKER_MAX_CHANNEL.toInt())
        .toString(HEX_RADIX).uppercase().padStart(2, '0')
    val rgb = channel(color.red) + channel(color.green) + channel(color.blue)
    return if (color.alpha >= OPAQUE) "#$rgb" else "#" + channel(color.alpha) + rgb
}

private val SQUARE_HEIGHT = 170.dp
private val BAR_HEIGHT = 28.dp
private val BAR_TRACK = 16.dp
private val THUMB = 24.dp
private val HUE_STOPS = listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { Color.hsv(it, 1f, 1f) }
private const val FULL_TURN = 360f
private const val SIXTY = 60f
private const val HUE_SECTORS = 6f
private const val OPAQUE = 0.999f
