package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.present.CanvasOutput
import com.church.presenter.churchpresentermobile.present.CanvasProjector
import com.church.presenter.churchpresentermobile.present.SinkRegistry
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Go Live end to end: the board's picture of the page reaches the standalone
 * engine as a canvas slide, and End Live takes it off again.
 *
 * Runs a real ViewModel whose frame is encoded on a worker thread, which the wasm
 * runtime's single thread cannot finish inside the test clock — so, like the
 * other ViewModel-backed UI tests, it is left out of the wasm run (build.gradle.kts).
 */
@OptIn(ExperimentalTestApi::class)
class CanvasGoLiveTest {

    @Test
    fun goLiveProjectsThePageAndEndLiveTakesItOff() = runComposeUiTest {
        val engine = StandaloneEngine(MutableStateFlow(AppMode.STANDALONE), SinkRegistry())
        val output = CanvasOutput(MutableStateFlow("http://192.168.1.50:8080"))
        val vm = showCanvas(canProject = true, projector = CanvasProjector(output, engine))
        try {
            click(CanvasTags.GO_LIVE)
            assertTrue(vm.state.value.live)

            awaitThat { engine.currentSlide.value.kind == SlideKind.CANVAS }
            assertTrue(output.owns(engine.currentSlide.value.mediaUrl), "the slide shows the canvas's own frame")

            click(CanvasTags.GO_LIVE)

            assertFalse(vm.state.value.live)
            assertNotEquals(SlideKind.CANVAS, engine.currentSlide.value.kind, "End Live cleared the screen")
        } finally {
            vm.viewModelScope.cancel()
        }
    }
}
