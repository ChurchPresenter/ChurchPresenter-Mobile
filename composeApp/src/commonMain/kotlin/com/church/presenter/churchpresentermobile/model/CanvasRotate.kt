package com.church.presenter.churchpresentermobile.model

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

/*
 * Turning elements: the geometry behind the rotate knob, as plain functions over
 * points and boxes so it can be checked without a composition.
 *
 * Every element turns about the centre of its own unturned box. Angles are degrees
 * clockwise — the board's y axis points down, so that is the direction a positive
 * angle turns on screen.
 */

/** [angle] brought into (-180, 180], so the same turn is always stored the same way. */
internal fun normalizedAngle(angle: Float): Float {
    val turned = angle % FULL_TURN
    return when {
        turned > HALF_TURN -> turned - FULL_TURN
        turned <= -HALF_TURN -> turned + FULL_TURN
        // Adding zero turns -0 into 0, so upright is one value, not two that compare unequal.
        else -> turned + 0f
    }
}

/** [angle] to the nearest [step] — the knob's snap, which also lands on every quarter turn. */
internal fun snappedAngle(angle: Float, step: Float = ROTATE_SNAP): Float =
    normalizedAngle(round(angle / step) * step)

/** A quarter turn clockwise past [angle]: what tapping the knob, or the picture's Rotate, does. */
internal fun quarterTurned(angle: Float): Float = normalizedAngle(angle + QUARTER_TURN)

/**
 * The angle a knob dragged to [at] asks for, turning about [centre]. The knob sits
 * straight above an unturned element, so a finger straight above is 0°.
 */
internal fun knobAngle(centre: CanvasPoint, at: CanvasPoint): Float {
    val radians = atan2(at.y - centre.y, at.x - centre.x)
    return normalizedAngle((radians * DEGREES_PER_RADIAN).toFloat() + QUARTER_TURN)
}

/** [point] turned [angle] degrees clockwise about [pivot]. */
internal fun rotatedPoint(point: CanvasPoint, angle: Float, pivot: CanvasPoint): CanvasPoint {
    if (angle == 0f) return point
    val radians = angle / DEGREES_PER_RADIAN
    val c = cos(radians).toFloat()
    val s = sin(radians).toFloat()
    val dx = point.x - pivot.x
    val dy = point.y - pivot.y
    return CanvasPoint(pivot.x + dx * c - dy * s, pivot.y + dx * s + dy * c)
}

internal val CanvasBounds.centre: CanvasPoint get() = CanvasPoint((left + right) / 2, (top + bottom) / 2)

/** The upright box round [box] turned [angle] degrees about its centre — what a turned element covers. */
internal fun rotatedBounds(box: CanvasBounds, angle: Float): CanvasBounds {
    if (abs(normalizedAngle(angle)) < EPSILON) return box
    val pivot = box.centre
    val corners = listOf(
        CanvasPoint(box.left, box.top),
        CanvasPoint(box.right, box.top),
        CanvasPoint(box.right, box.bottom),
        CanvasPoint(box.left, box.bottom),
    ).map { rotatedPoint(it, angle, pivot) }
    return CanvasBounds(
        left = corners.minOf { it.x },
        top = corners.minOf { it.y },
        right = corners.maxOf { it.x },
        bottom = corners.maxOf { it.y },
    )
}

/** Whether [at] lands on [box] turned [angle] degrees about its centre. */
internal fun turnedContains(box: CanvasBounds, angle: Float, at: CanvasPoint): Boolean =
    box.contains(rotatedPoint(at, -angle, box.centre))

/**
 * [start] resized by dragging [handle] by ([dx], [dy]) on the page, for an element
 * turned [angle] degrees.
 *
 * The drag is read in the element's own turned frame, so pulling a turned box's
 * edge stretches it along that edge. The handle opposite stays where it is on the
 * page: resizing moves the box's centre, the centre is what the element turns
 * about, so the box is shifted back by however far that would have carried it.
 */
internal fun rotatedResize(start: CanvasBounds, handle: Int, dx: Float, dy: Float, angle: Float): CanvasBounds {
    if (angle == 0f) return resizedBounds(start, handle, dx, dy)
    val local = rotatedPoint(CanvasPoint(dx, dy), -angle, CanvasPoint(0f, 0f))
    val resized = resizedBounds(start, handle, local.x, local.y)
    val opposite = (handle + HANDLE_COUNT / 2) % HANDLE_COUNT
    val before = rotatedPoint(handlePoint(start, opposite), angle, start.centre)
    val after = rotatedPoint(handlePoint(resized, opposite), angle, resized.centre)
    val shiftX = before.x - after.x
    val shiftY = before.y - after.y
    return CanvasBounds(resized.left + shiftX, resized.top + shiftY, resized.right + shiftX, resized.bottom + shiftY)
}

/**
 * Handle [index] of [box]: the corners and edge midpoints clockwise from the top
 * left, the order the selection frame draws them in.
 */
internal fun handlePoint(box: CanvasBounds, index: Int): CanvasPoint {
    val c = box.centre
    return listOf(
        CanvasPoint(box.left, box.top),
        CanvasPoint(c.x, box.top),
        CanvasPoint(box.right, box.top),
        CanvasPoint(box.right, c.y),
        CanvasPoint(box.right, box.bottom),
        CanvasPoint(c.x, box.bottom),
        CanvasPoint(box.left, box.bottom),
        CanvasPoint(box.left, c.y),
    )[index]
}

/** Eight handles: four corners and four edge midpoints. */
internal const val HANDLE_COUNT = 8

/** The knob snaps to this many degrees. */
internal const val ROTATE_SNAP = 15f

private const val QUARTER_TURN = 90f
private const val HALF_TURN = 180f
private const val FULL_TURN = 360f
private const val DEGREES_PER_RADIAN = 180.0 / PI
private const val EPSILON = 0.01f
