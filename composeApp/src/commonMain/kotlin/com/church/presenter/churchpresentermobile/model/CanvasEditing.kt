package com.church.presenter.churchpresentermobile.model

/*
 * The edits a canvas page takes, as plain functions over [CanvasDocument].
 *
 * Kept out of the ViewModel so each is a value in and a value out: the ViewModel
 * only decides *which* edit and keeps the undo history, and a test can check what
 * an edit does without a dispatcher or a composition.
 */

/** [pageIndex] with [transform] applied to its elements; any other page is untouched. */
internal fun CanvasDocument.editPage(
    pageIndex: Int,
    transform: (List<CanvasElement>) -> List<CanvasElement>,
): CanvasDocument = copy(
    pages = pages.mapIndexed { index, page ->
        if (index == pageIndex) page.copy(elements = transform(page.elements)) else page
    },
)

/** The document with a blank page added after the last one. */
internal fun CanvasDocument.withNewPage(): CanvasDocument = copy(pages = pages + CanvasPage())

/** [elements] without the one called [id]. */
internal fun List<CanvasElement>.without(id: String): List<CanvasElement> = filterNot { it.id == id }

/** [elements] with [id] moved to the end, which is the top of the drawing order. */
internal fun List<CanvasElement>.broughtToFront(id: String): List<CanvasElement> {
    val element = firstOrNull { it.id == id } ?: return this
    return without(id) + element
}

/** [elements] with the one called [id] replaced by [transform] of itself. */
internal fun List<CanvasElement>.updated(id: String, transform: (CanvasElement) -> CanvasElement): List<CanvasElement> =
    map { if (it.id == id) transform(it) else it }

/**
 * [elements] with a copy of [id] placed just above it, nudged by [DUPLICATE_OFFSET]
 * so the copy is visibly a second item rather than hidden exactly behind the first.
 */
internal fun List<CanvasElement>.withDuplicate(id: String, newId: String): List<CanvasElement> {
    val index = indexOfFirst { it.id == id }
    if (index < 0) return this
    val copy = this[index].duplicated(newId, DUPLICATE_OFFSET, DUPLICATE_OFFSET)
    return take(index + 1) + copy + drop(index + 1)
}

/** How far a duplicate lands from its original, in dp. */
internal const val DUPLICATE_OFFSET = 24f

/**
 * SVG path data for a hand-drawn stroke through [points]: a move to the first,
 * a line to each one after. A single point becomes a dot — a zero-length segment
 * whose round cap draws it.
 */
internal fun strokePathData(points: List<CanvasPoint>): String {
    if (points.isEmpty()) return ""
    val first = points.first()
    val rest = points.drop(1).ifEmpty { listOf(first) }
    return buildString {
        append("M${first.x.svg()} ${first.y.svg()}")
        rest.forEach { append(" L${it.x.svg()} ${it.y.svg()}") }
    }
}

/** A coordinate to one decimal place, which is finer than a finger and keeps a long stroke's data short. */
private fun Float.svg(): String {
    val tenths = kotlin.math.round(this * TENTHS).toLong()
    val whole = tenths / TENTHS.toLong()
    val fraction = kotlin.math.abs(tenths % TENTHS.toLong())
    val sign = if (tenths < 0 && whole == 0L) "-" else ""
    return if (fraction == 0L) "$sign$whole" else "$sign$whole.$fraction"
}

private const val TENTHS = 10f

/**
 * The zoom levels the − and + buttons step through, in percent.
 *
 * Steps rather than a free value because a button press should land somewhere
 * round; pinch zoom, when it arrives, can sit between them.
 */
internal val CanvasZoomSteps = listOf(25, 50, 75, 100, 125, 150, 200, 300, 400)

/** The next step above [zoom], or the top step when there is none. */
internal fun zoomedIn(zoom: Int): Int = CanvasZoomSteps.firstOrNull { it > zoom } ?: CanvasZoomSteps.last()

/** The next step below [zoom], or the bottom step when there is none. */
internal fun zoomedOut(zoom: Int): Int = CanvasZoomSteps.lastOrNull { it < zoom } ?: CanvasZoomSteps.first()
