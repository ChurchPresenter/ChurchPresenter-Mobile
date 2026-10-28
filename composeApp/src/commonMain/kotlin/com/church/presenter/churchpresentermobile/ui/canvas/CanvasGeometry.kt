package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.model.CanvasShape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * The canvas's plain arithmetic: the outline of each shape, and the mapping
 * between a slider's knob and the value it stands for.
 */

/** The outline each [CanvasShape] is drawn and clipped to. */
internal fun canvasShape(shape: CanvasShape): Shape = when (shape) {
    CanvasShape.RECTANGLE -> RectangleShape
    CanvasShape.ROUNDED -> RoundedCornerShape(14.dp)
    CanvasShape.SPEECH -> SpeechBubble
    CanvasShape.ELLIPSE -> GenericShape { size, _ -> addOval(Rect(Offset.Zero, size)) }
    CanvasShape.TRIANGLE -> polygon(TrianglePoints)
    CanvasShape.DIAMOND -> polygon(DiamondPoints)
    CanvasShape.HEXAGON -> polygon(HexagonPoints)
    CanvasShape.STAR -> polygon(StarPoints)
}

/**
 * A speech bubble: a rounded body over the top four-fifths, and a tail from its
 * bottom edge down to the bottom-left. One closed outline, so the fill, the border
 * and the clip all follow the tail — it was a plain rounded box before.
 */
private val SpeechBubble: Shape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val body = h * SPEECH_BODY
    val r = minOf(w, body) * SPEECH_CORNER
    moveTo(r, 0f)
    lineTo(w - r, 0f)
    quadraticTo(w, 0f, w, r)
    lineTo(w, body - r)
    quadraticTo(w, body, w - r, body)
    lineTo(w * SPEECH_TAIL_RIGHT, body)
    lineTo(w * SPEECH_TAIL_TIP, h)
    lineTo(w * SPEECH_TAIL_LEFT, body)
    lineTo(r, body)
    quadraticTo(0f, body, 0f, body - r)
    lineTo(0f, r)
    quadraticTo(0f, 0f, r, 0f)
    close()
}

/* The bubble's proportions: how much of the height the body takes, its corner, and where the tail meets and points. */
private const val SPEECH_BODY = 0.8f
private const val SPEECH_CORNER = 0.18f
private const val SPEECH_TAIL_LEFT = 0.22f
private const val SPEECH_TAIL_RIGHT = 0.4f
private const val SPEECH_TAIL_TIP = 0.16f

/* Each outline's corners, as shares of the shape's width and height. */
private val TrianglePoints = listOf(0.5f to 0f, 1f to 1f, 0f to 1f)
private val DiamondPoints = listOf(0.5f to 0f, 1f to 0.5f, 0.5f to 1f, 0f to 0.5f)
private val HexagonPoints = listOf(0.25f to 0f, 0.75f to 0f, 1f to 0.5f, 0.75f to 1f, 0.25f to 1f, 0f to 0.5f)

/** A shape through [points], each a share of the width and height. */
private fun polygon(points: List<Pair<Float, Float>>): Shape = GenericShape { size, _ ->
    points.forEachIndexed { index, (x, y) ->
        if (index == 0) moveTo(x * size.width, y * size.height) else lineTo(x * size.width, y * size.height)
    }
    close()
}

/** A five-pointed star's ten corners, alternating the outer and inner radius round the centre. */
private val StarPoints: List<Pair<Float, Float>> = (0 until STAR_CORNERS).map { i ->
    val radius = if (i % 2 == 0) STAR_OUTER else STAR_INNER
    val angle = -PI / 2 + i * PI / (STAR_CORNERS / 2)
    (STAR_CENTRE + radius * cos(angle).toFloat()) to (STAR_CENTRE + radius * sin(angle).toFloat())
}

private const val STAR_CORNERS = 10
private const val STAR_CENTRE = 0.5f
private const val STAR_OUTER = 0.5f
private const val STAR_INNER = 0.2f

/** Where [value] sits in [range], 0–1, for a slider's knob. */
internal fun rangeFraction(value: Float, range: ClosedFloatingPointRange<Float>): Float =
    ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

/** The value a slider at [fraction] stands for in [range], to the nearest whole dp. */
internal fun rangeValue(fraction: Float, range: ClosedFloatingPointRange<Float>): Float =
    (range.start + fraction.coerceIn(0f, 1f) * (range.endInclusive - range.start)).roundToInt().toFloat()
