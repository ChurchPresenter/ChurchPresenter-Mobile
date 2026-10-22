package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.model.CanvasBounds
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasPoint
import com.church.presenter.churchpresentermobile.model.CanvasTool
import com.church.presenter.churchpresentermobile.model.EraserMode
import com.church.presenter.churchpresentermobile.model.InkKind
import com.church.presenter.churchpresentermobile.model.LassoPicks
import com.church.presenter.churchpresentermobile.model.LassoShape
import com.church.presenter.churchpresentermobile.model.areaHits
import com.church.presenter.churchpresentermobile.model.boundsHits
import com.church.presenter.churchpresentermobile.model.boundsOf
import com.church.presenter.churchpresentermobile.model.contains
import com.church.presenter.churchpresentermobile.model.lassoPick
import com.church.presenter.churchpresentermobile.model.strokeHits
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import com.church.presenter.churchpresentermobile.viewmodel.CanvasUiState
import kotlin.math.hypot

/**
 * What a finger does on the board for the active tool, from drawing a stroke to
 * dragging out a shape. The select tool is not here: it works by tapping the
 * elements themselves (see [SelectTargets]).
 *
 * @param measure Where an element lies on the page, for the tools that hit-test.
 * @param onLasso Where the last lasso ran, so the board can keep its outline drawn.
 */
@Composable
internal fun ToolGestures(
    state: CanvasUiState,
    onIntent: (CanvasIntent) -> Unit,
    measure: (CanvasElement) -> CanvasBounds,
    paths: InkPaths,
    onLasso: (List<CanvasPoint>) -> Unit,
) {
    val colors = canvasColors
    val options = state.options
    val visible = state.page.elements.filterNot { it.hidden }
    when (state.tool) {
        CanvasTool.SELECT -> Unit
        CanvasTool.PEN, CanvasTool.HIGHLIGHTER -> {
            val marker = state.tool == CanvasTool.HIGHLIGHTER
            val color = when {
                marker -> options.highlighter.custom?.let { colors.inkColor(options.pen.ink, it) }
                    ?: highlightColor(options.highlighter.color)
                else -> colors.inkColor(options.pen.ink, options.pen.custom)
            }
            val width = if (marker) options.highlighter.width else options.pen.width
            val opacity = if (marker) options.highlighter.opacity else options.pen.opacity
            GestureLayer(
                key = state.tool,
                onTap = { onIntent(CanvasIntent.Stroke(listOf(it))) },
                onDragEnd = { points, pressure -> onIntent(CanvasIntent.Stroke(points, pressure)) },
            ) { points -> drawPolyline(points, color, width, opacity) }
        }
        CanvasTool.ERASER -> EraserGestures(state, visible, onIntent, measure, paths)
        CanvasTool.LASSO -> {
            val rectangle = options.lasso.shape == LassoShape.RECTANGLE
            val pick: (List<CanvasPoint>) -> Unit = { lasso ->
                val candidates = visible.filter { element ->
                    val ink = element is CanvasElement.Ink && element.kind != InkKind.ERASER
                    when (options.lasso.picks) {
                        LassoPicks.EVERYTHING -> element !is CanvasElement.Ink || ink
                        LassoPicks.INK -> ink
                        LassoPicks.OBJECTS -> element !is CanvasElement.Ink
                    } && (!element.locked || options.select.selectLocked)
                }.associate { it.id to measure(it) }
                onLasso(lasso)
                onIntent(CanvasIntent.SelectMany(lassoPick(candidates, lasso, rectangle, options.lasso.partial)))
            }
            GestureLayer(
                key = state.tool,
                onTap = { onLasso(emptyList()); onIntent(CanvasIntent.Select(null)) },
                onDragEnd = { points, _ -> pick(points) },
            ) { points -> drawLasso(points, rectangle, colors.accent) }
        }
        CanvasTool.SHAPES -> GestureLayer(
            key = state.tool,
            onTap = { onIntent(CanvasIntent.PlaceShape(defaultShapeAt(it))) },
            onDragEnd = { points, _ ->
                val box = boundsOf(listOf(points.first(), points.last()))
                    ?.takeIf { it.width >= MIN_SHAPE && it.height >= MIN_SHAPE }
                onIntent(CanvasIntent.PlaceShape(box ?: defaultShapeAt(points.last())))
            },
        ) { points -> drawLasso(points, rectangle = true, color = colors.accent) }
        CanvasTool.LINE -> GestureLayer(
            key = state.tool,
            onTap = { onIntent(CanvasIntent.PlaceLine(it, CanvasPoint(it.x + DEFAULT_LINE, it.y))) },
            onDragEnd = { points, _ -> onIntent(CanvasIntent.PlaceLine(points.first(), points.last())) },
        ) { points ->
            if (points.size > 1) {
                drawPolyline(
                    listOf(points.first(), points.last()),
                    colors.inkColor(options.line.ink, options.line.custom),
                    options.line.width,
                    1f,
                )
            }
        }
        CanvasTool.TEXT, CanvasTool.STICKY -> {
            val text = state.tool == CanvasTool.TEXT
            GestureLayer(
                key = state.tool,
                onTap = { at ->
                    // Tapping a box or note of the tool's own kind types into it; anywhere else starts a new one.
                    val hit = visible.lastOrNull { element ->
                        val kind = if (text) element is CanvasElement.Text else element is CanvasElement.Sticky
                        kind && !element.locked && measure(element).contains(at)
                    }
                    onIntent(
                        when {
                            hit != null -> CanvasIntent.StartEditing(hit.id)
                            text -> CanvasIntent.PlaceText(at)
                            else -> CanvasIntent.PlaceSticky(at)
                        },
                    )
                },
                onDragEnd = { _, _ -> },
            ) {}
        }
        CanvasTool.IMAGE -> GestureLayer(
            key = state.tool,
            onTap = { onIntent(CanvasIntent.InsertImage(options.image.source, it)) },
            onDragEnd = { _, _ -> },
        ) {}
    }
}

/** The shape a tap with the shape tool places: a default-sized one centred on the finger. */
private fun defaultShapeAt(at: CanvasPoint) = CanvasBounds(
    left = at.x - DEFAULT_SHAPE_WIDTH / 2,
    top = at.y - DEFAULT_SHAPE_HEIGHT / 2,
    right = at.x + DEFAULT_SHAPE_WIDTH / 2,
    bottom = at.y + DEFAULT_SHAPE_HEIGHT / 2,
)

private const val DEFAULT_SHAPE_WIDTH = 200f
private const val DEFAULT_SHAPE_HEIGHT = 140f
private const val MIN_SHAPE = 20f
private const val DEFAULT_LINE = 160f

/**
 * The eraser in its three modes. Stroke takes away everything the eraser passes
 * over, as it goes; Area takes away everything the box dragged out touches; Pixel
 * paints the board back over whatever it crosses, as a stroke of its own. With
 * "Erase highlighter only" on, only highlighter strokes are taken.
 */
@Composable
private fun EraserGestures(
    state: CanvasUiState,
    visible: List<CanvasElement>,
    onIntent: (CanvasIntent) -> Unit,
    measure: (CanvasElement) -> CanvasBounds,
    paths: InkPaths,
) {
    val colors = canvasColors
    val eraser = state.options.eraser
    val targets = visible.filter { element ->
        val kind = (element as? CanvasElement.Ink)?.kind
        val wanted = if (eraser.highlighterOnly) kind == InkKind.HIGHLIGHTER else kind != InkKind.ERASER
        wanted && !element.locked
    }
    val radius = eraser.size / 2
    val hits: (List<CanvasPoint>) -> List<String> = { path ->
        val dense = densified(path, radius / 2)
        val strokes = targets.filterIsInstance<CanvasElement.Ink>().associate { ink ->
            val samples = paths.samples(ink.pathData).map { CanvasPoint(it.x + ink.x, it.y + ink.y) }
            ink.id to (samples to ink.width / 2)
        }
        val boxes = targets.filterNot { it is CanvasElement.Ink }.associate { it.id to measure(it) }
        strokeHits(strokes, dense, radius) + boundsHits(boxes, dense, radius)
    }
    when (eraser.mode) {
        EraserMode.PIXEL -> GestureLayer(
            key = eraser.mode,
            onTap = { onIntent(CanvasIntent.Stroke(listOf(it))) },
            onDragEnd = { points, _ -> onIntent(CanvasIntent.Stroke(points)) },
        ) { points -> drawPolyline(points, colors.board, eraser.size, 1f) }
        EraserMode.STROKE -> GestureLayer(
            key = eraser.mode,
            onTap = { at -> hits(listOf(at)).takeIf { it.isNotEmpty() }?.let { onIntent(CanvasIntent.Erase(it)) } },
            onDragEnd = { _, _ -> },
            onDragMove = { recent, session ->
                hits(recent).takeIf { it.isNotEmpty() }?.let { onIntent(CanvasIntent.Erase(it, session)) }
            },
        ) { points -> drawPolyline(points, colors.muted, eraser.size, ERASER_TRAIL) }
        EraserMode.AREA -> GestureLayer(
            key = eraser.mode,
            onTap = { at -> hits(listOf(at)).takeIf { it.isNotEmpty() }?.let { onIntent(CanvasIntent.Erase(it)) } },
            onDragEnd = { path, _ ->
                onIntent(CanvasIntent.Erase(areaHits(targets.associate { it.id to measure(it) }, path)))
            },
        ) { points -> drawLasso(points, rectangle = true, color = colors.danger) }
    }
}

/** How visible the stroke eraser's trail is while dragging. */
private const val ERASER_TRAIL = 0.25f

/**
 * [path] with points filled in so no two are more than [step] apart — a quick
 * swipe reports points far apart, and the eraser must not jump over what lies between.
 */
private fun densified(path: List<CanvasPoint>, step: Float): List<CanvasPoint> {
    if (path.size < 2 || step <= 0f) return path
    return path.zipWithNext().flatMap { (a, b) ->
        val gap = hypot(b.x - a.x, b.y - a.y)
        val count = (gap / step).toInt().coerceAtLeast(1)
        (0 until count).map { i ->
            val t = i.toFloat() / count
            CanvasPoint(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
        }
    } + path.last()
}

/** Numbers each drag, so the steps of one drag share a run on the undo history. */
internal object GestureSessions {
    private var last = 0

    fun next(): Int = ++last
}

/**
 * Catches a tap or a drag across the board and shows [preview] while the drag is
 * under way. Points are handed over in dp on the page.
 *
 * Tracked by hand rather than with two competing detectors, so a touch is exactly
 * one of a tap or a drag. A second finger landing hands the touch to the board's
 * pan and zoom, which consumes it — the drag is then dropped, not finished.
 *
 * @param onDragEnd The whole drag, and its average stylus pressure (1 for a finger).
 * @param onDragMove The last stretch of a drag as it happens, with the drag's
 *   session number — for an eraser that works as it goes.
 */
@Composable
private fun GestureLayer(
    key: Any,
    onTap: (CanvasPoint) -> Unit,
    onDragEnd: (List<CanvasPoint>, Float) -> Unit,
    onDragMove: ((List<CanvasPoint>, Int) -> Unit)? = null,
    preview: DrawScope.(List<CanvasPoint>) -> Unit,
) {
    val points = remember { mutableStateListOf<CanvasPoint>() }
    // The handlers read the latest state without restarting the gesture mid-stroke.
    val tap by rememberUpdatedState(onTap)
    val dragEnd by rememberUpdatedState(onDragEnd)
    val dragMove by rememberUpdatedState(onDragMove)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(key) {
                fun Offset.toPoint() = CanvasPoint(x / density, y / density)
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val session = GestureSessions.next()
                    var dragging = false
                    var pressure = down.pressure
                    points.clear()
                    points.add(down.position.toPoint())
                    try {
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (change.isConsumed) break
                            if (!change.pressed) {
                                if (dragging) dragEnd(points.toList(), pressure / points.size) else tap(points.first())
                                break
                            }
                            val travelled = (change.position - down.position).getDistance()
                            if (!dragging && travelled > viewConfiguration.touchSlop) dragging = true
                            if (dragging) {
                                change.consume()
                                points.add(change.position.toPoint())
                                pressure += change.pressure
                                dragMove?.invoke(points.takeLast(2), session)
                            }
                        }
                    } finally {
                        points.clear()
                    }
                }
            }
            .drawBehind { if (points.isNotEmpty()) preview(points) },
    )
}

/** A line through [points] (in dp), as a stroke [width] dp wide would draw it. */
internal fun DrawScope.drawPolyline(points: List<CanvasPoint>, color: Color, width: Float, alpha: Float) {
    if (points.isEmpty()) return
    val path = Path().apply {
        moveTo(points.first().x.dp.toPx(), points.first().y.dp.toPx())
        points.drop(1).ifEmpty { listOf(points.first()) }.forEach { lineTo(it.x.dp.toPx(), it.y.dp.toPx()) }
    }
    drawPath(
        path = path,
        color = color,
        alpha = alpha,
        style = Stroke(width = width.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

/** A dashed loop through [points], or the dashed box they span for a [rectangle]. */
internal fun DrawScope.drawLasso(points: List<CanvasPoint>, rectangle: Boolean, color: Color) {
    val dash = Stroke(
        width = 2.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
    )
    if (rectangle) {
        val box = boundsOf(points) ?: return
        drawRect(
            color = color,
            topLeft = Offset(box.left.dp.toPx(), box.top.dp.toPx()),
            size = Size(box.width.dp.toPx(), box.height.dp.toPx()),
            style = dash,
        )
    } else if (points.size > 1) {
        val path = Path().apply {
            moveTo(points.first().x.dp.toPx(), points.first().y.dp.toPx())
            points.drop(1).forEach { lineTo(it.x.dp.toPx(), it.y.dp.toPx()) }
            close()
        }
        drawPath(path, color, style = dash)
    }
}
