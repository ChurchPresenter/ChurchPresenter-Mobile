package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasPoint
import com.church.presenter.churchpresentermobile.model.CanvasTool
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import com.church.presenter.churchpresentermobile.viewmodel.CanvasUiState

/**
 * The page being drawn on: a dotted board with the page's elements on it, scaled
 * for zoom from its top-left corner.
 *
 * What a touch does depends on the tool. The select tool picks the element under
 * the finger (or clears the selection on empty board); the pen and highlighter
 * draw a stroke. The other tools have options to set but nothing to draw yet.
 */
@Composable
internal fun CanvasBoard(state: CanvasUiState, onIntent: (CanvasIntent) -> Unit, modifier: Modifier = Modifier) {
    val colors = canvasColors
    // Text has no size until it is laid out; selection needs one to draw its frame.
    val textSizes = remember { mutableStateMapOf<String, Rect>() }
    val scale = state.zoom / PERCENT
    val visible = state.page.elements.filterNot { it.hidden }
    Box(
        modifier = modifier
            .testTag(CanvasTags.BOARD)
            .clipToBounds()
            .background(colors.board)
            .dotGrid(colors.boardDots),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .then(
                    if (state.tool == CanvasTool.SELECT) {
                        Modifier.pointerInput(Unit) { detectTapGestures { onIntent(CanvasIntent.Select(null)) } }
                    } else {
                        Modifier
                    },
                ),
        ) {
            visible.forEach { element ->
                key(element.id) { MeasuredElement(element, textSizes) }
            }
            when (state.tool) {
                CanvasTool.SELECT -> SelectTargets(state, visible, textSizes, onIntent)
                CanvasTool.PEN, CanvasTool.HIGHLIGHTER -> DrawingLayer(state, onIntent)
                else -> Unit
            }
            state.selected?.takeUnless { it.hidden }?.let { element ->
                SelectionOverlay(element = element, bounds = elementBounds(element, textSizes), onIntent = onIntent)
            }
        }
    }
}

private const val PERCENT = 100f

/** An element, with a text element's laid-out size noted in [textSizes] for its selection frame. */
@Composable
private fun MeasuredElement(element: CanvasElement, textSizes: SnapshotStateMap<String, Rect>) {
    val density = LocalDensity.current
    val modifier = if (element is CanvasElement.Text) {
        Modifier.onSizeChanged { size ->
            with(density) {
                textSizes[element.id] = Rect(
                    element.x,
                    element.y,
                    element.x + size.width.toDp().value,
                    element.y + size.height.toDp().value,
                )
            }
        }
    } else {
        Modifier
    }
    CanvasElementView(element, modifier)
}

/**
 * Where [element] lies on the page, in dp.
 *
 * Composable because an ink stroke's bounds come from its parsed path, which is
 * remembered per stroke rather than parsed again on every frame.
 */
@Composable
internal fun elementBounds(element: CanvasElement, textSizes: Map<String, Rect>): Rect = when (element) {
    is CanvasElement.Text -> textSizes[element.id] ?: Rect(element.x, element.y, element.x, element.y)
    is CanvasElement.Ink -> inkBounds(element, rememberInkPath(element.pathData))
    is CanvasElement.Image -> Rect(element.x, element.y, element.x + element.width, element.y + element.height)
    is CanvasElement.Shape -> Rect(element.x, element.y, element.x + element.width, element.y + element.height)
    is CanvasElement.Sticky -> Rect(element.x, element.y, element.x + element.width, element.y + element.height)
}

/**
 * A tap target over each element for the select tool, in drawing order so the
 * top-most element under a finger is the one picked. Locked elements are left
 * out unless the select options say to pick them up too.
 */
@Composable
private fun SelectTargets(
    state: CanvasUiState,
    visible: List<CanvasElement>,
    textSizes: Map<String, Rect>,
    onIntent: (CanvasIntent) -> Unit,
) {
    val selectable = visible.filter { !it.locked || state.options.select.selectLocked }
    selectable.forEach { element ->
        key(element.id) {
            val bounds = elementBounds(element, textSizes)
            Box(
                modifier = Modifier
                    .offset(bounds.left.dp, bounds.top.dp)
                    .size(bounds.width.dp, bounds.height.dp)
                    .clickable(interactionSource = null, indication = null) {
                        onIntent(CanvasIntent.Select(element.id))
                    },
            )
        }
    }
}

/**
 * Catches a pen or highlighter stroke and draws it as it goes. The finished stroke
 * is handed over as points in dp on the page; a tap is a stroke of one point, a dot.
 */
@Composable
private fun DrawingLayer(state: CanvasUiState, onIntent: (CanvasIntent) -> Unit) {
    val colors = canvasColors
    val points = remember { mutableStateListOf<CanvasPoint>() }
    val highlighter = state.tool == CanvasTool.HIGHLIGHTER
    val color: Color = if (highlighter) {
        highlightColor(state.options.highlighter.color)
    } else {
        colors.inkColor(state.options.pen.ink)
    }
    val width = if (highlighter) state.options.highlighter.width else state.options.pen.width
    val opacity = if (highlighter) state.options.highlighter.opacity else state.options.pen.opacity
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(state.tool) {
                fun Offset.toPoint() = CanvasPoint(x / density, y / density)
                detectDragGestures(
                    onDragStart = { points.clear(); points.add(it.toPoint()) },
                    onDrag = { change, _ -> points.add(change.position.toPoint()) },
                    onDragEnd = {
                        onIntent(CanvasIntent.Stroke(points.toList()))
                        points.clear()
                    },
                    onDragCancel = { points.clear() },
                )
            }
            .pointerInput(state.tool) {
                detectTapGestures { onIntent(CanvasIntent.Stroke(listOf(CanvasPoint(it.x / density, it.y / density)))) }
            }
            .drawBehind {
                if (points.isEmpty()) return@drawBehind
                val path = Path().apply {
                    moveTo(points.first().x.dp.toPx(), points.first().y.dp.toPx())
                    points.drop(1).forEach { lineTo(it.x.dp.toPx(), it.y.dp.toPx()) }
                }
                drawPath(
                    path = path,
                    color = color,
                    alpha = opacity,
                    style = Stroke(width = width.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            },
    )
}

/** The design's dot grid: a faint dot every 28dp, which is what makes a blank board read as paper. */
private fun Modifier.dotGrid(color: Color): Modifier = drawBehind {
    val step = 28.dp.toPx()
    val radius = 1.4.dp.toPx()
    var y = step / 2
    while (y < size.height) {
        var x = step / 2
        while (x < size.width) {
            drawCircle(color, radius, Offset(x, y))
            x += step
        }
        y += step
    }
}
