package com.church.presenter.churchpresentermobile.model

import kotlin.math.abs
import kotlin.math.round

/*
 * Moving and resizing on the page: where a dragged selection lands once Snap to
 * grid and Smart guides have had their say, and the box a resize handle drags out.
 */

/** The board's grid, in dp — the same spacing as its dots. */
internal const val CANVAS_GRID = 28f

/** How close, in dp, an edge must come to another's before a smart guide pulls it in. */
internal const val GUIDE_REACH = 6f

/**
 * How far to move a selection that started at [start] when the finger has moved
 * ([dx], [dy]).
 *
 * With [grid], the selection's top-left corner lands on the grid. With [guides]
 * (the other elements' bounds), an edge or centre that comes within
 * [GUIDE_REACH] of another element's edge or centre is pulled into line with it,
 * which takes priority over the grid on that axis.
 */
internal fun snappedMove(
    start: CanvasBounds,
    dx: Float,
    dy: Float,
    grid: Boolean,
    guides: List<CanvasBounds>,
): Pair<Float, Float> {
    var x = dx
    var y = dy
    if (grid) {
        x = round((start.left + dx) / CANVAS_GRID) * CANVAS_GRID - start.left
        y = round((start.top + dy) / CANVAS_GRID) * CANVAS_GRID - start.top
    }
    if (guides.isNotEmpty()) {
        guidePull(start.xLines().map { it + dx }, guides.flatMap { it.xLines() })?.let { x = dx + it }
        guidePull(start.yLines().map { it + dy }, guides.flatMap { it.yLines() })?.let { y = dy + it }
    }
    return x to y
}

private fun CanvasBounds.xLines() = listOf(left, (left + right) / 2, right)
private fun CanvasBounds.yLines() = listOf(top, (top + bottom) / 2, bottom)

/** The smallest nudge that puts one of [moving] on one of [fixed], if any is within reach. */
private fun guidePull(moving: List<Float>, fixed: List<Float>): Float? =
    moving.flatMap { m -> fixed.map { f -> f - m } }
        .filter { abs(it) <= GUIDE_REACH }
        .minByOrNull { abs(it) }

/** The smallest a resize may make an element, in dp. */
internal const val MIN_ELEMENT_SIZE = 24f

/**
 * The box a resize handle drags [start] out to. [handle] counts round from the
 * top-left corner clockwise — corners on the even numbers, edge midpoints on the
 * odd — and each moves only the edges it sits on.
 */
internal fun resizedBounds(start: CanvasBounds, handle: Int, dx: Float, dy: Float): CanvasBounds {
    val movesLeft = handle in LEFT_HANDLES
    val movesRight = handle in RIGHT_HANDLES
    val movesTop = handle in TOP_HANDLES
    val movesBottom = handle in BOTTOM_HANDLES
    var left = if (movesLeft) start.left + dx else start.left
    var right = if (movesRight) start.right + dx else start.right
    var top = if (movesTop) start.top + dy else start.top
    var bottom = if (movesBottom) start.bottom + dy else start.bottom
    if (right - left < MIN_ELEMENT_SIZE) {
        if (movesLeft) left = right - MIN_ELEMENT_SIZE else right = left + MIN_ELEMENT_SIZE
    }
    if (bottom - top < MIN_ELEMENT_SIZE) {
        if (movesTop) top = bottom - MIN_ELEMENT_SIZE else bottom = top + MIN_ELEMENT_SIZE
    }
    return CanvasBounds(left, top, right, bottom)
}

private val LEFT_HANDLES = setOf(0, 6, 7)
private val RIGHT_HANDLES = setOf(2, 3, 4)
private val TOP_HANDLES = setOf(0, 1, 2)
private val BOTTOM_HANDLES = setOf(4, 5, 6)

/** Whether [element] has a box of its own to resize — pictures, shapes and notes. */
internal fun resizable(element: CanvasElement): Boolean =
    element is CanvasElement.Image || element is CanvasElement.Shape || element is CanvasElement.Sticky

/** [element] fitted to [bounds]; anything without a box of its own is left as it is. */
internal fun resized(element: CanvasElement, bounds: CanvasBounds): CanvasElement = when (element) {
    is CanvasElement.Image ->
        element.copy(x = bounds.left, y = bounds.top, width = bounds.width, height = bounds.height)
    is CanvasElement.Shape ->
        element.copy(x = bounds.left, y = bounds.top, width = bounds.width, height = bounds.height)
    is CanvasElement.Sticky ->
        element.copy(x = bounds.left, y = bounds.top, width = bounds.width, height = bounds.height)
    else -> element
}
