package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.church.presenter.churchpresentermobile.model.CanvasTool
import com.church.presenter.churchpresentermobile.ui.AppBackHandler
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import com.church.presenter.churchpresentermobile.viewmodel.CanvasPanel
import com.church.presenter.churchpresentermobile.viewmodel.CanvasUiState
import com.church.presenter.churchpresentermobile.viewmodel.CanvasViewModel
import kotlin.math.roundToInt

/**
 * The canvas: a whiteboard of pages to sketch a talk on and put on screen.
 *
 * Tablet only — the caller opens it only in the two-pane layout, where it fills the
 * window as the design draws it. Owns its [CanvasViewModel]; there is no backend
 * yet, so the canvas lives in memory and [onGoLive] is the caller's to wire.
 *
 * @param onBack Back to the More launcher, from the "‹ More" link or the system back gesture.
 */
@Composable
fun CanvasScreen(onBack: () -> Unit, onGoLive: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: CanvasViewModel = viewModel { CanvasViewModel() }
    val state by viewModel.state.collectAsState()
    AppBackHandler(enabled = true, onBack = onBack)
    CanvasEditor(
        state = state,
        onIntent = viewModel::dispatch,
        onBack = onBack,
        onMore = {},
        onGoLive = onGoLive,
        modifier = modifier,
    )
}

/**
 * The canvas drawn from [state], with every request handed to [onIntent].
 *
 * Plain data in, callbacks out, so a test or a screenshot can put the editor in
 * any state without a ViewModel.
 */
@Composable
internal fun CanvasEditor(
    state: CanvasUiState,
    onIntent: (CanvasIntent) -> Unit,
    onBack: () -> Unit,
    onMore: () -> Unit,
    onGoLive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = canvasColors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.chrome)
            .navigationBarsPadding()
            .testTag(CanvasTags.SCREEN),
    ) {
        CanvasTopBar(state = state, onIntent = onIntent, onBack = onBack, onMore = onMore, onGoLive = onGoLive)
        CanvasWorkspace(state = state, onIntent = onIntent, modifier = Modifier.weight(1f).fillMaxWidth())
    }
}

/**
 * The board and everything floating over it: the tool rail on the left with its
 * options beside it, the pen dock and page strip along the bottom, and the
 * Layers or Insert panel down the right.
 *
 * The page strip steps aside while a right-hand panel is out — the Layers panel
 * runs to the bottom edge, where the strip would sit under it.
 */
@Composable
private fun CanvasWorkspace(state: CanvasUiState, onIntent: (CanvasIntent) -> Unit, modifier: Modifier) {
    val density = LocalDensity.current
    // Where the workspace and the active tool's button are, in root pixels, so the
    // options panel can hang its pointer level with the button.
    var workspaceTop by remember { mutableFloatStateOf(0f) }
    var toolCentre by remember { mutableFloatStateOf(0f) }
    var optionsHeight by remember { mutableIntStateOf(0) }
    BoxWithConstraints(modifier = modifier.onGloballyPositioned { workspaceTop = it.boundsInRoot().top }) {
        CanvasBoard(state = state, onIntent = onIntent, modifier = Modifier.fillMaxSize())
        CanvasToolRail(
            active = state.tool,
            onPick = { onIntent(CanvasIntent.PickTool(it)) },
            onActiveCentre = { toolCentre = it },
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp),
        )
        if (state.optionsOpen) {
            val placement = with(density) {
                optionsPlacement(
                    toolCentre = toolCentre - workspaceTop,
                    panelHeight = optionsHeight.toFloat(),
                    areaHeight = constraints.maxHeight.toFloat(),
                    margin = OPTIONS_MARGIN.toPx(),
                    pointerFromTop = POINTER_FROM_TOP.toPx(),
                )
            }
            ToolOptionsPanel(
                tool = state.tool,
                options = state.options,
                onOptions = { onIntent(CanvasIntent.SetOptions(it)) },
                pointerY = with(density) { placement.pointer.toDp() },
                modifier = Modifier
                    .offset { IntOffset(OPTIONS_LEFT.roundToPx(), placement.top.roundToInt()) }
                    .onSizeChanged { optionsHeight = it.height },
            )
        }
        if (state.tool == CanvasTool.PEN || state.tool == CanvasTool.HIGHLIGHTER) {
            PenDock(
                state = state,
                onIntent = onIntent,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 96.dp, bottom = 18.dp),
            )
        }
        when (state.panel) {
            CanvasPanel.LAYERS -> LayersPanel(
                state = state,
                onIntent = onIntent,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = PANEL_TOP, end = 20.dp, bottom = 20.dp),
            )
            CanvasPanel.INSERT_IMAGE -> InsertImagePanel(
                state = state,
                onIntent = onIntent,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = PANEL_TOP, end = 20.dp),
            )
            CanvasPanel.NONE -> PageStrip(
                state = state,
                onIntent = onIntent,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 18.dp),
            )
        }
    }
}

/** The options panel's left edge: past the rail (16 + 62) and a 12dp gap. */
private val OPTIONS_LEFT = 90.dp

/** How close the options panel may come to the board's top and bottom edges. */
private val OPTIONS_MARGIN = 16.dp

/** Where the pointer sits on a panel that is free to line up with its tool. */
private val POINTER_FROM_TOP = 24.dp

/** How far below the bar the right-hand panels start, as the design places them. */
private val PANEL_TOP = 76.dp

/** Where the options panel goes, and where on it the pointer is, in pixels from the workspace's top. */
internal data class OptionsPlacement(val top: Float, val pointer: Float)

/**
 * Puts the options panel level with its tool, pointer and all — unless that would
 * run it off the board, in which case it slides back on and the pointer moves
 * along it to stay on the tool.
 *
 * Pulled out of the layout as plain numbers so the clamping can be tested without
 * a composition.
 */
internal fun optionsPlacement(
    toolCentre: Float,
    panelHeight: Float,
    areaHeight: Float,
    margin: Float,
    pointerFromTop: Float,
): OptionsPlacement {
    val lowest = (areaHeight - panelHeight - margin).coerceAtLeast(margin)
    val top = (toolCentre - pointerFromTop).coerceIn(margin, lowest)
    // The pointer stays on the panel's edge even when the tool has scrolled past it.
    val pointer = (toolCentre - top).coerceIn(pointerFromTop / 2, panelHeight.coerceAtLeast(pointerFromTop))
    return OptionsPlacement(top = top, pointer = pointer)
}
