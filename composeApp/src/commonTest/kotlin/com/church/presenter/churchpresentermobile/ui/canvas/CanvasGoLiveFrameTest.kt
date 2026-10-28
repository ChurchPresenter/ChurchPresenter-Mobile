package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.present.CanvasProjection
import com.church.presenter.churchpresentermobile.present.CanvasShowResult
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import com.church.presenter.churchpresentermobile.viewmodel.CanvasViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Going live with a page already drawn sends a picture of that page. On an iPad nothing
 * redrew the board when Go Live was pressed, so the first frame was an empty layer and the
 * screen went blank until the next stroke.
 */
@OptIn(ExperimentalTestApi::class)
class CanvasGoLiveFrameTest {

    /** Somewhere to project that takes every frame. */
    private val screen = object : CanvasProjection {
        override val asksFirst = false
        override val showing = MutableStateFlow(false)
        override suspend fun show(png: ByteArray, paper: String, title: String, takeOver: Boolean) =
            CanvasShowResult.SHOWN
        override suspend fun stop() = Unit
    }

    @Test
    fun goingLiveSendsThePageAlreadyDrawn() = runComposeUiTest {
        val vm = CanvasViewModel(sampleCanvas(), projector = screen)
        val frames = mutableListOf<ImageBitmap>()
        setContent {
            AppTheme(themeMode = ThemeMode.DARK) {
                Box(Modifier.fillMaxSize()) {
                    val state by vm.state.collectAsState()
                    CanvasEditor(
                        state = state,
                        onIntent = vm::dispatch,
                        onBack = {},
                        canProject = true,
                        onFrame = { picture, _ -> frames += picture },
                    )
                }
            }
        }
        waitForIdle()

        vm.dispatch(CanvasIntent.SetLive(true))
        mainClock.advanceTimeBy(FRAME_WAIT_MS)
        waitForIdle()

        val first = assertNotNull(frames.firstOrNull(), "going live sent a frame without another edit")
        val pixels = first.toPixelMap()
        val paper = pixels[0, 0]
        val drawn = (0 until pixels.width step SAMPLE).sumOf { x ->
            (0 until pixels.height step SAMPLE).count { y -> pixels[x, y] != paper }
        }
        assertTrue(drawn > 0, "the frame has the page's elements on it, not just paper")
    }

    private companion object {
        const val FRAME_WAIT_MS = 1_000L
        const val SAMPLE = 4
    }
}
