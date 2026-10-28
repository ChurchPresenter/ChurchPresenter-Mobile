package com.church.presenter.churchpresentermobile.model

import kotlin.math.hypot

/*
 * Which elements a lasso or an eraser touches. The board measures each element and
 * hands over plain rectangles and points; the deciding is done here, where a test
 * can draw a lasso without a finger.
 */

/** The smallest rectangle round [points], or null for none. */
internal fun boundsOf(points: List<CanvasPoint>): CanvasBounds? {
    if (points.isEmpty()) return null
    return CanvasBounds(
        left = points.minOf { it.x },
        top = points.minOf { it.y },
        right = points.maxOf { it.x },
        bottom = points.maxOf { it.y },
    )
}

internal fun CanvasBounds.contains(point: CanvasPoint): Boolean =
    point.x in left..right && point.y in top..bottom

internal fun CanvasBounds.contains(other: CanvasBounds): Boolean =
    other.left >= left && other.right <= right && other.top >= top && other.bottom <= bottom

internal fun CanvasBounds.intersects(other: CanvasBounds): Boolean =
    other.left <= right && other.right >= left && other.top <= bottom && other.bottom >= top

/** Whether [point] is inside the closed outline [polygon] (even-odd rule). */
internal fun inPolygon(point: CanvasPoint, polygon: List<CanvasPoint>): Boolean {
    var inside = false
    var j = polygon.lastIndex
    for (i in polygon.indices) {
        val a = polygon[i]
        val b = polygon[j]
        val crosses = (a.y > point.y) != (b.y > point.y) &&
            point.x < (b.x - a.x) * (point.y - a.y) / (b.y - a.y) + a.x
        if (crosses) inside = !inside
        j = i
    }
    return inside
}

/**
 * The ids in [candidates] a lasso through [lasso] picks up.
 *
 * A rectangle lasso is the box the finger dragged out; a freeform one is the loop
 * it drew, closed back to its start. An element is picked when it lies wholly
 * inside — or, with [partial], when any of it does.
 */
internal fun lassoPick(
    candidates: Map<String, CanvasBounds>,
    lasso: List<CanvasPoint>,
    rectangle: Boolean,
    partial: Boolean,
): List<String> {
    val box = boundsOf(lasso) ?: return emptyList()
    return candidates.filter { (_, bounds) ->
        if (rectangle) {
            if (partial) box.intersects(bounds) else box.contains(bounds)
        } else {
            val corners = bounds.corners()
            val inside = corners.count { inPolygon(it, lasso) }
            if (partial) inside > 0 || lasso.any { bounds.contains(it) } else inside == corners.size
        }
    }.keys.toList()
}

private fun CanvasBounds.corners(): List<CanvasPoint> = listOf(
    CanvasPoint(left, top),
    CanvasPoint(right, top),
    CanvasPoint(right, bottom),
    CanvasPoint(left, bottom),
)

/**
 * The strokes an eraser dragged along [path] with a tip of [radius] touches.
 *
 * @param strokes Points sampled along each stroke, keyed by element id; each
 *   stroke's own half-width is added to the reach so a broad stroke is easy to hit.
 */
internal fun strokeHits(
    strokes: Map<String, Pair<List<CanvasPoint>, Float>>,
    path: List<CanvasPoint>,
    radius: Float,
): List<String> = strokes.filter { (_, stroke) ->
    val (samples, halfWidth) = stroke
    val reach = radius + halfWidth
    samples.any { s -> path.any { p -> hypot(s.x - p.x, s.y - p.y) <= reach } }
}.keys.toList()

/** The ids in [candidates] the box an area eraser dragged out along [path] touches. */
internal fun areaHits(candidates: Map<String, CanvasBounds>, path: List<CanvasPoint>): List<String> {
    val box = boundsOf(path) ?: return emptyList()
    return candidates.filter { (_, bounds) -> box.intersects(bounds) }.keys.toList()
}

/**
 * The ids in [candidates] an eraser with a tip of [radius] passed over along [path]
 * — for things with a box rather than a line, such as shapes, notes and pictures.
 */
internal fun boundsHits(candidates: Map<String, CanvasBounds>, path: List<CanvasPoint>, radius: Float): List<String> =
    candidates.filter { (_, b) ->
        val reach = CanvasBounds(b.left - radius, b.top - radius, b.right + radius, b.bottom + radius)
        path.any { reach.contains(it) }
    }.keys.toList()
