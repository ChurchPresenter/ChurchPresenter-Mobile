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

/** The document with a copy of page [index] placed right after it. */
internal fun CanvasDocument.withPageDuplicated(index: Int, newId: () -> String): CanvasDocument {
    val page = pages.getOrNull(index) ?: return this
    val copy = CanvasPage(page.elements.map { it.withId(newId()) })
    return copy(pages = pages.take(index + 1) + copy + pages.drop(index + 1))
}

/** The document without page [index] — unless it is the only page, which a canvas always keeps. */
internal fun CanvasDocument.withoutPage(index: Int): CanvasDocument =
    if (pages.size <= 1 || index !in pages.indices) this else copy(pages = pages.filterIndexed { i, _ -> i != index })

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
        append("M${first.x.svgNumber()} ${first.y.svgNumber()}")
        rest.forEach { append(" L${it.x.svgNumber()} ${it.y.svgNumber()}") }
    }
}

/** A coordinate to one decimal place, which is finer than a finger and keeps a long stroke's data short. */
internal fun Float.svgNumber(): String {
    val tenths = kotlin.math.round(this * TENTHS).toLong()
    val whole = tenths / TENTHS.toLong()
    val fraction = kotlin.math.abs(tenths % TENTHS.toLong())
    val sign = if (tenths < 0 && whole == 0L) "-" else ""
    return if (fraction == 0L) "$sign$whole" else "$sign$whole.$fraction"
}

private const val TENTHS = 10f

/**
 * [points] eased by a moving average, so a shaky hand draws a smooth line. [amount]
 * runs 0–1: none leaves the stroke as drawn, full averages each point with up to
 * [MAX_SMOOTHING_REACH] neighbours either side. The ends stay put, so a stroke
 * still starts and stops exactly where the pen did.
 */
internal fun smoothed(points: List<CanvasPoint>, amount: Float): List<CanvasPoint> {
    val reach = kotlin.math.round(amount.coerceIn(0f, 1f) * MAX_SMOOTHING_REACH).toInt()
    if (reach == 0 || points.size < MIN_POINTS_TO_SMOOTH) return points
    return points.mapIndexed { i, point ->
        if (i == 0 || i == points.lastIndex) return@mapIndexed point
        val window = points.subList((i - reach).coerceAtLeast(0), (i + reach + 1).coerceAtMost(points.size))
        CanvasPoint(
            x = window.sumOf { it.x.toDouble() }.toFloat() / window.size,
            y = window.sumOf { it.y.toDouble() }.toFloat() / window.size,
        )
    }
}

private const val MAX_SMOOTHING_REACH = 4f

/** A stroke needs a point between its two ends before there is anything to smooth. */
private const val MIN_POINTS_TO_SMOOTH = 3

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
