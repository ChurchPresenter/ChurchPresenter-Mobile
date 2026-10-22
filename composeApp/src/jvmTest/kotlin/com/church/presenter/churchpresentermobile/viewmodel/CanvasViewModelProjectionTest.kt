package com.church.presenter.churchpresentermobile.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.CanvasDocument
import com.church.presenter.churchpresentermobile.model.SlideDeckBuilder
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.present.CanvasOutput
import com.church.presenter.churchpresentermobile.present.CanvasProjector
import com.church.presenter.churchpresentermobile.present.SinkRegistry
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import com.church.presenter.churchpresentermobile.testutil.tearDown
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Projecting the canvas from the frames the board captures.
 *
 * On the JVM rather than in commonTest: a frame is an [ImageBitmap] encoded to PNG
 * by Skia, which the desktop JVM has and neither the js Karma runtime nor the
 * Android unit-test JVM (with its stubbed android.jar) can provide. This run is the
 * one JaCoCo measures, so the projection path is still counted.
 */
class CanvasViewModelProjectionTest {

    private class Fixture {
        val baseUrl = MutableStateFlow<String?>("http://phone:8080")
        val engine = StandaloneEngine(MutableStateFlow(AppMode.STANDALONE), SinkRegistry(), publish = {})
        val output = CanvasOutput(baseUrl)
        val vm = CanvasViewModel(
            document = CanvasDocument(title = "Sermon"),
            projector = CanvasProjector(output, engine),
            frameWork = EmptyCoroutineContext,
            saveWork = EmptyCoroutineContext,
        )
        val frame = ImageBitmap(8, 6)
        val slide get() = engine.currentSlide.value
    }

    @Test
    fun goLiveTakesTheScreenWithTheNextFrame() = runVmTest {
        val f = Fixture()
        try {
            runCurrent()
            f.vm.showFrame(f.frame, "#FFFFFF")
            advanceUntilIdle()
            assertEquals(SlideKind.BLANK, f.slide.kind, "a frame while not live goes nowhere")

            f.vm.dispatch(CanvasIntent.SetLive(true))
            f.vm.showFrame(f.frame, "#FFFFFF")
            advanceUntilIdle()
            assertEquals(SlideKind.CANVAS, f.slide.kind)
            assertEquals("#FFFFFF", f.slide.paper)
            assertEquals("Sermon", f.engine.deck.value.title)
            assertTrue(f.output.owns(f.slide.mediaUrl))
            val png = f.output.source.photo(f.slide.mediaUrl!!.substringAfterLast('/'))!!.bytes
            assertEquals(0x89.toByte(), png[0], "the frame is served as a PNG")
        } finally {
            tearDown(f.vm)
        }
    }

    @Test
    fun laterFramesRefreshTheScreen() = runVmTest {
        val f = Fixture()
        try {
            runCurrent()
            f.vm.dispatch(CanvasIntent.SetLive(true))
            f.vm.showFrame(f.frame, "#FFFFFF")
            advanceUntilIdle()
            val first = f.slide.mediaUrl
            f.vm.showFrame(f.frame, "#FFFFFF")
            advanceUntilIdle()
            assertNotEquals(first, f.slide.mediaUrl)
            assertTrue(f.vm.state.value.live)
        } finally {
            tearDown(f.vm)
        }
    }

    @Test
    fun anotherSlideTakingTheScreenEndsLive() = runVmTest {
        val f = Fixture()
        try {
            runCurrent()
            f.vm.dispatch(CanvasIntent.SetLive(true))
            f.vm.showFrame(f.frame, "#FFFFFF")
            advanceUntilIdle()
            f.engine.setDeck(SlideDeckBuilder.fromAnnouncements(listOf("Welcome")))
            advanceUntilIdle()
            assertFalse(f.vm.state.value.live, "the canvas stepped aside")

            f.vm.showFrame(f.frame, "#FFFFFF")
            advanceUntilIdle()
            assertEquals(SlideKind.ANNOUNCEMENT, f.slide.kind, "and a later stroke does not take the screen back")
        } finally {
            tearDown(f.vm)
        }
    }

    @Test
    fun goingLiveWithTheServerDownComesStraightBackOff() = runVmTest {
        val f = Fixture()
        try {
            runCurrent()
            f.baseUrl.value = null
            f.vm.dispatch(CanvasIntent.SetLive(true))
            f.vm.showFrame(f.frame, "#FFFFFF")
            advanceUntilIdle()
            assertFalse(f.vm.state.value.live)
            assertEquals(SlideKind.BLANK, f.slide.kind)
        } finally {
            tearDown(f.vm)
        }
    }
}
