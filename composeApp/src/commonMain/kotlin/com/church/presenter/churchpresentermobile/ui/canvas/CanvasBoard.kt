package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.model.CanvasBounds
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasPoint
import com.church.presenter.churchpresentermobile.model.CanvasTool
import com.church.presenter.churchpresentermobile.model.LassoShape
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import com.church.presenter.churchpresentermobile.viewmodel.CanvasUiState
import kotlin.math.roundToInt

/**
 * The page being drawn on: a dotted board with the page's elements on it, scaled
 * for zoom from its top-left corner.
 *
 * What one finger does depends on the tool — see [SelectGestures] and
 * [ToolGestures]. Two fingers pan the page and pinch to zoom.
 *
 * @param textSizes Where each text box landed once laid out, shared with the
 *   screen so the Arrange buttons can measure the selection too.
 * @param onFrame A picture of the page for the outputs, while the canvas is live.
 */
@Composable
internal fun CanvasBoard(
    state: CanvasUiState,
    onIntent: (CanvasIntent) -> Unit,
    textSizes: SnapshotStateMap<String, CanvasBounds>,
    paths: InkPaths,
    modifier: Modifier = Modifier,
    onFrame: (ImageBitmap) -> Unit = {},
) {
    val colors = canvasColors
    val scale = state.zoom / PERCENT
    val visible = state.page.elements.filterNot { it.hidden }
    val measure: (CanvasElement) -> CanvasBounds = { elementBounds(it, textSizes, paths) }
    val measureBox: (CanvasElement) -> CanvasBounds = { elementBox(it, textSizes, paths) }
    // The last lasso's outline stays drawn round what it picked up, as the design shows.
    var lasso by remember { mutableStateOf(emptyList<CanvasPoint>()) }
    // Where two fingers have moved the page to, in pixels.
    var pan by remember { mutableStateOf(Offset.Zero) }
    val zoom by rememberUpdatedState(state.zoom)
    Box(
        modifier = modifier
            .testTag(CanvasTags.BOARD)
            .clipToBounds()
            .background(colors.board)
            .dotGrid(colors.boardDots)
            .panAndPinch(
                onPan = { pan += it },
                onZoom = { factor, centroid ->
                    val old = zoom / PERCENT
                    val new = (old * factor).coerceIn(MIN_ZOOM, MAX_ZOOM)
                    // Keep the point under the fingers where it is.
                    pan = centroid - (centroid - pan) * (new / old)
                    onIntent(CanvasIntent.SetZoom((new * PERCENT).roundToInt()))
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = pan.x
                    translationY = pan.y
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                },
        ) {
            CapturedPage(
                live = state.live,
                paper = colors.board,
                changes = state.page,
                onFrame = onFrame,
            ) {
                visible.forEach { element ->
                    key(element.id) {
                        CanvasElementView(
                            element = element,
                            editing = element.id == state.editingId,
                            paths = paths,
                            onIntent = onIntent,
                            onTextBounds = { id, bounds -> textSizes[id] = bounds },
                        )
                    }
                }
            }
            if (state.tool == CanvasTool.SELECT) SelectGestures(state, onIntent, measure, measureBox)
            ToolGestures(state = state, onIntent = onIntent, measure = measure, paths = paths, onLasso = { lasso = it })
            if (state.tool == CanvasTool.LASSO && state.selectedIds.isNotEmpty() && lasso.isNotEmpty()) {
                val rectangle = state.options.lasso.shape == LassoShape.RECTANGLE
                Canvas(Modifier.fillMaxSize()) { drawLasso(lasso, rectangle, colors.accent) }
            }
            union(state.selection.filterNot { it.hidden }.map(measure))?.let { bounds ->
                val single = state.selected?.takeIf { !it.hidden }?.let { TurnedBox(measureBox(it), it.angle) }
                SelectionOverlay(state = state, bounds = bounds, single = single, onIntent = onIntent)
            }
        }
    }
}

private const val PERCENT = 100f
private const val MIN_ZOOM = 0.25f
private const val MAX_ZOOM = 4f

/**
 * Two fingers on the board pan the page and pinch to zoom, whichever tool is out.
 * Caught in the Initial pass, before the tool sees the touch, and only once a second
 * finger is down — so one finger still draws, and a stroke a second finger joins is
 * dropped rather than drawn across the page.
 */
private fun Modifier.panAndPinch(
    onPan: (Offset) -> Unit,
    onZoom: (factor: Float, centroid: Offset) -> Unit,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pressed = event.changes.filter { it.pressed }
            if (pressed.isEmpty()) break
            if (pressed.size >= 2) {
                val factor = event.calculateZoom()
                val panBy = event.calculatePan()
                if (factor != 1f) onZoom(factor, event.calculateCentroid())
                if (panBy != Offset.Zero) onPan(panBy)
                event.changes.forEach { it.consume() }
            }
        }
    }
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
