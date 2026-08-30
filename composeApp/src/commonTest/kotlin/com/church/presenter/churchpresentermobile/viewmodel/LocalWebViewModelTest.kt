package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.present.SinkRegistry
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
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

        for (bad in listOf("javascript:alert(1)", "file:///etc/passwd", "data:text/html,<h1>x", "")) {
            vm.setUrl(bad)
            assertFalse(vm.canProject.value, "'$bad' must not be projectable")
            vm.project()
            advanceUntilIdle()
            assertTrue(engine.deck.value.slides.isEmpty(), "'$bad' must not become a slide")
            assertNull(vm.projecting.value)
        }
    }

    @Test
    fun anAddressTypedWithoutItsSchemeStillReachesTheScreen() = runVmTest {
        // What people actually type. The Go Live button used to do nothing at all for this.
        val engine = engine()
        val vm = LocalWebViewModel(engine)

        vm.setUrl("example.org")
        // canProject is a stateIn flow, so it holds its initial false until the dispatcher runs.
        advanceUntilIdle()
        assertTrue(vm.canProject.value)

        vm.project()
        advanceUntilIdle()

        assertEquals("https://example.org", engine.currentSlide.value.mediaUrl)
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

    // ── Sites that will not be framed ────────────────────────────────────────

    @Test
    fun aSiteThatRefusesFramingIsNamedAfterItIsProjected() = runVmTest {
        val engine = engine()
        val vm = LocalWebViewModel(engine, refusesFraming = { true })

        vm.setUrl("google.com")
        vm.project()
        advanceUntilIdle()

        // Projected anyway: a screen attached to this phone shows the page, so
        // the warning is about one output rather than a refusal to try.
        assertEquals(SlideKind.WEB, engine.deck.value.kind)
        assertEquals("google.com", vm.refusedByFraming.value)
    }

    @Test
    fun anOrdinarySiteIsNotWarnedAbout() = runVmTest {
        val vm = LocalWebViewModel(engine(), refusesFraming = { false })

        vm.setUrl("example.org")
        vm.project()
        advanceUntilIdle()

        assertNull(vm.refusedByFraming.value)
    }

    @Test
    fun typingAgainDropsTheWarning() = runVmTest {
        val vm = LocalWebViewModel(engine(), refusesFraming = { true })
        vm.setUrl("google.com")
        vm.project()
        advanceUntilIdle()

        vm.setUrl("example.org")

        assertNull(vm.refusedByFraming.value, "the warning belonged to the old address")
    }

    @Test
    fun aLateAnswerAboutAnAddressAlreadyReplacedIsIgnored() = runVmTest {
        // The check is a network round trip. If the operator has moved on by the
        // time it lands, naming the old site would be worse than saying nothing.
        val vm = LocalWebViewModel(engine(), refusesFraming = { url ->
            if (url.contains("slow")) { delay(5_000) }
            true
        })
        vm.setUrl("slow.example")
        vm.project()

        advanceTimeBy(100)
        vm.setUrl("example.org")
        vm.project()
        advanceUntilIdle()

        assertEquals("example.org", vm.refusedByFraming.value)
    }
}
