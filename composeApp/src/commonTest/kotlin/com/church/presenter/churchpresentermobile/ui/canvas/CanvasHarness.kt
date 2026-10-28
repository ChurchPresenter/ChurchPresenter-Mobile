package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.model.CanvasDocument
import com.church.presenter.churchpresentermobile.model.CanvasTool
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.present.CanvasProjector
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import com.church.presenter.churchpresentermobile.viewmodel.CanvasChrome
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import com.church.presenter.churchpresentermobile.viewmodel.CanvasViewModel

/*
 * Shared setup for the canvas's UI tests: a sample canvas with one of every kind
 * of element, the editor driven by a real ViewModel, and the gestures that act on
 * the board.
 *
 * The sample canvas itself is in [Sample].
 */

/**
 * Shows the canvas editor, driven by a real [CanvasViewModel] over [document], and
 * returns the ViewModel so a test can read what each gesture did to the canvas.
 *
 * No store and — unless given — no projector, so nothing here runs a coroutine:
 * the editing is all plain functions, and the test reads the result straight away.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showCanvas(
    document: CanvasDocument = sampleCanvas(),
    canProject: Boolean = false,
    projector: CanvasProjector? = null,
    onBack: () -> Unit = {},
): CanvasViewModel {
    val vm = CanvasViewModel(document, projector = projector)
    setContent {
        AppTheme(themeMode = ThemeMode.DARK) {
            Box(Modifier.fillMaxSize()) {
                val state by vm.state.collectAsState()
                CanvasEditor(
                    state = state,
                    onIntent = vm::dispatch,
                    onBack = onBack,
                    canProject = canProject,
                    onFrame = vm::showFrame,
                )
            }
        }
    }
    return vm
}

/** Picks [tool] on the rail — which, for a tool not already out, also opens its options. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.pick(tool: CanvasTool) = click(CanvasTags.tool(tool))

/** Sends [intent] straight to the ViewModel, for setting a scene up rather than testing how it is reached. */
internal fun CanvasViewModel.given(vararg intents: CanvasIntent) = intents.forEach(::dispatch)

/** The pressable controls in the open options panel, as they stand now. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.panelControls(): SemanticsNodeInteractionCollection =
    onAllNodes(
        hasClickAction() and isEnabled() and hasAnyAncestor(hasTestTag(CanvasTags.OPTIONS)) and
            // The panel's own minimize button folds every other control away; it is tested on its own.
            !hasTestTag(CanvasTags.chrome(CanvasChrome.OPTIONS, minimized = false)),
    )

/**
 * Presses every pressable control in the open options panel once, top to bottom,
 * reading the panel afresh before each press — a press can relabel or re-enable
 * the controls after it. [afterEach] runs after every press, for a test that
 * wants to deal with a dialog or a menu the press opened.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.pressEveryControl(afterEach: () -> Unit = {}) {
    var index = 0
    while (index < panelControls().fetchSemanticsNodes().size) {
        panelControls()[index].performSemanticsAction(SemanticsActions.OnClick)
        afterEach()
        index++
    }
}

/** Runs [block] on the board, with page coordinates (dp at 100% zoom) turned into touch positions. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.onBoard(block: TouchInjectionScope.() -> Unit) =
    onNodeWithTag(CanvasTags.BOARD).performTouchInput(block)

/** The point ([x], [y]) on the page, in this touch's pixels. */
internal fun TouchInjectionScope.page(x: Float, y: Float): Offset = Offset(x.dp.toPx(), y.dp.toPx())

/** A one-finger drag along the page from ([x1], [y1]) to ([x2], [y2]). */
internal fun TouchInjectionScope.dragOnPage(x1: Float, y1: Float, x2: Float, y2: Float) =
    swipe(page(x1, y1), page(x2, y2), durationMillis = DRAG_MS)

private const val DRAG_MS = 200L
