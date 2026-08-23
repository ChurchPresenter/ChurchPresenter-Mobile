package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.present.SinkRegistry
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A link on the audience screen, put there by the phone rather than a desktop. */
class LocalWebViewModelTest {

    private fun engine() = StandaloneEngine(
        mode = MutableStateFlow(AppMode.STANDALONE),
        registry = SinkRegistry(),
        publish = {},
    )

    @Test
    fun aPageIsFramedAndAVideoIsPlayed() = runVmTest {
        // Operators paste both kinds into the same box; classifying their own
        // link before pasting it is a question the app can answer itself.
        val engine = engine()
        val vm = LocalWebViewModel(engine)

        vm.setUrl("https://example.org/notices")
        vm.project()
        advanceUntilIdle()
        assertEquals(SlideKind.WEB, engine.currentSlide.value.kind)

        vm.setUrl("https://example.org/bumper.mp4")
        vm.project()
        advanceUntilIdle()
        assertEquals(SlideKind.VIDEO, engine.currentSlide.value.kind)
        assertEquals("https://example.org/bumper.mp4", engine.currentSlide.value.mediaUrl)
    }

    @Test
    fun aQueryStringDoesNotHideTheVideoExtension() = runVmTest {
        val engine = engine()
        val vm = LocalWebViewModel(engine)

        vm.setUrl("https://cdn.example.org/clip.mp4?token=abc#t=10")
        vm.project()
        advanceUntilIdle()

        assertEquals(SlideKind.VIDEO, engine.currentSlide.value.kind)
    }

    @Test
    fun onlyHttpLinksReachTheScreen() = runVmTest {
        // The address is handed to an embedded browser and to an iframe on the
        // display page. Anything else is code execution or disk access there.
        val engine = engine()
        val vm = LocalWebViewModel(engine)

        for (bad in listOf("javascript:alert(1)", "file:///etc/passwd", "data:text/html,<h1>x", "example.org", "")) {
            vm.setUrl(bad)
            assertFalse(vm.canProject.value, "'$bad' must not be projectable")
            vm.project()
            advanceUntilIdle()
            assertTrue(engine.deck.value.slides.isEmpty(), "'$bad' must not become a slide")
            assertNull(vm.projecting.value)
        }
    }

    @Test
    fun clearingTakesItOffTheScreen() = runVmTest {
        val engine = engine()
        val vm = LocalWebViewModel(engine)
        vm.setUrl("https://example.org")
        vm.project()
        advanceUntilIdle()

        vm.clearDisplay()
        advanceUntilIdle()

        assertNull(vm.projecting.value)
        assertTrue(engine.deck.value.slides.isEmpty())
    }
}
