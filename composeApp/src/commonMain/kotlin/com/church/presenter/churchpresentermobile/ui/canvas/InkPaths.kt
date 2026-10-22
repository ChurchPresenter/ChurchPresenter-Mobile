package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.vector.PathParser
import com.church.presenter.churchpresentermobile.model.CanvasBounds
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasPoint

/**
 * Ink paths parsed once per stroke and kept, along with points sampled along each
 * for the eraser. Shared by everything that needs a stroke's shape — drawing it,
 * its selection frame, a lasso, the eraser — so a stroke is parsed once, not once
 * per use per frame.
 */
internal class InkPaths {
    private val paths = HashMap<String, Path>()
    private val samples = HashMap<String, List<CanvasPoint>>()

    /** [pathData] as a path, in dp. */
    fun path(pathData: String): Path = paths.getOrPut(pathData) { PathParser().parsePathString(pathData).toPath() }

    /** Points every [SAMPLE_STEP] dp along [pathData], for hit-testing a stroke by distance. */
    fun samples(pathData: String): List<CanvasPoint> = samples.getOrPut(pathData) {
        val measure = PathMeasure().apply { setPath(path(pathData), false) }
        val length = measure.length
        if (length <= 0f) {
            val bounds = path(pathData).getBounds()
            listOf(CanvasPoint(bounds.center.x, bounds.center.y))
        } else {
            generateSequence(0f) { it + SAMPLE_STEP }.takeWhile { it <= length }
                .map { measure.getPosition(it) }
                .map { CanvasPoint(it.x, it.y) }
                .toList() + measure.getPosition(length).let { CanvasPoint(it.x, it.y) }
        }
    }

    private companion object {
        const val SAMPLE_STEP = 6f
    }
}

@Composable
internal fun rememberInkPaths(): InkPaths = remember { InkPaths() }

/**
 * Where [element] lies on the page, in dp.
 *
 * @param textSizes Text has no size until it is laid out; the board notes each
 *   text box's laid-out bounds here.
 */
internal fun elementBounds(
    element: CanvasElement,
    textSizes: Map<String, CanvasBounds>,
    paths: InkPaths,
): CanvasBounds = when (element) {
    is CanvasElement.Text -> textSizes[element.id]
        ?: CanvasBounds(element.x, element.y, element.x + MIN_TEXT_BOX, element.y + MIN_TEXT_BOX)
    is CanvasElement.Ink -> {
        val box = paths.path(element.pathData).getBounds()
        val reach = maxOf(element.width / 2f, MIN_INK_REACH)
        CanvasBounds(
            left = element.x + box.left - reach,
            top = element.y + box.top - reach,
            right = element.x + box.right + reach,
            bottom = element.y + box.bottom + reach,
        )
    }
    is CanvasElement.Image -> box(element.x, element.y, element.width, element.height)
    is CanvasElement.Shape -> box(element.x, element.y, element.width, element.height)
    is CanvasElement.Sticky -> box(element.x, element.y, element.width, element.height)
}

private fun box(x: Float, y: Float, width: Float, height: Float) = CanvasBounds(x, y, x + width, y + height)

/** The least a stroke reaches beyond its line, in dp — a finger is wider than a 2dp line. */
private const val MIN_INK_REACH = 8f

/** A text box not laid out yet still gets something to tap. */
private const val MIN_TEXT_BOX = 40f

/** The box round all of [bounds], or null for none. */
internal fun union(bounds: Collection<CanvasBounds>): CanvasBounds? =
    if (bounds.isEmpty()) {
        null
    } else {
        CanvasBounds(
            left = bounds.minOf { it.left },
            top = bounds.minOf { it.top },
            right = bounds.maxOf { it.right },
            bottom = bounds.maxOf { it.bottom },
        )
    }
