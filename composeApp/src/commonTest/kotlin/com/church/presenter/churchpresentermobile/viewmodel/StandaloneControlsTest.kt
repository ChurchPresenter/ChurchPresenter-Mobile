package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideDeck
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.model.SlideTextSize
import com.church.presenter.churchpresentermobile.present.SinkRegistry
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The standalone controller's buttons, and what the audience screen is left
 * showing after each one.
 *
 * The ViewModel keeps no projection state of its own — it re-exposes the
 * engine's, which outlives any one screen and is also written to by the Songs
 * and Bible tabs. What is checked here is that every control reaches the engine
 * and that the flows the controller reads report the result, because a button
 * that changes the engine without the screen following looks to the operator
 * like the tap was missed.
 */
class StandaloneControlsTest {

    private fun engine() = StandaloneEngine(
        mode = MutableStateFlow(AppMode.STANDALONE),
        registry = SinkRegistry(),
        publish = {},
    )

    private fun vm(engine: StandaloneEngine = engine()) =
        StandaloneViewModel(engine, SinkRegistry(), AppSettings(InMemorySettingsStorage()))

    private fun deck(vararg bodies: String) = SlideDeck(
        kind = SlideKind.SONG,
        title = "Amazing Grace",
        slides = bodies.mapIndexed { i, body ->
            Slide(kind = SlideKind.SONG, body = body, index = i, total = bodies.size)
        },
    )

    // ── Nothing loaded ───────────────────────────────────────────────────

    @Test
    fun `the controller opens on an empty deck showing nothing`() = runVmTest {
        val viewModel = vm()

        assertTrue(viewModel.deck.value.isEmpty)
        assertEquals(0, viewModel.index.value)
        assertEquals(SlideKind.BLANK, viewModel.currentSlide.value.kind)
        assertEquals("", viewModel.currentSlide.value.body)
    }

    @Test
    fun `no output is attached before a screen is added`() = runVmTest {
        assertTrue(vm().sinks.value.isEmpty())
    }

    // ── Stepping through a deck ──────────────────────────────────────────

    @Test
    fun `a deck loaded into the engine is what the controller lists`() = runVmTest {
        val standaloneEngine = engine()
        val viewModel = vm(standaloneEngine)

        standaloneEngine.loadDeck(deck("verse one", "verse two"))

        assertEquals(2, viewModel.deck.value.slides.size)
        assertEquals("Amazing Grace", viewModel.deck.value.title)
    }

    @Test
    fun `tapping a section shows it`() = runVmTest {
        val standaloneEngine = engine()
        val viewModel = vm(standaloneEngine)
        standaloneEngine.loadDeck(deck("verse one", "verse two", "verse three"))

        viewModel.showSlide(2)

        assertEquals(2, viewModel.index.value)
        assertEquals("verse three", viewModel.currentSlide.value.body)
    }

    @Test
    fun `next advances one slide`() = runVmTest {
        val standaloneEngine = engine()
        val viewModel = vm(standaloneEngine)
        standaloneEngine.loadDeck(deck("one", "two"))

        viewModel.next()

        assertEquals(1, viewModel.index.value)
    }

    @Test
    fun `next at the end of the deck stays on the last slide`() = runVmTest {
        // The operator holding the forward button through the last verse must not
        // fall off the end into a blank screen.
        val standaloneEngine = engine()
        val viewModel = vm(standaloneEngine)
        standaloneEngine.loadDeck(deck("one", "two"))

        viewModel.next()
        viewModel.next()
        viewModel.next()

        assertEquals(1, viewModel.index.value)
        assertEquals("two", viewModel.currentSlide.value.body)
    }

    @Test
    fun `previous steps back one slide`() = runVmTest {
        val standaloneEngine = engine()
        val viewModel = vm(standaloneEngine)
        standaloneEngine.loadDeck(deck("one", "two", "three"))
        viewModel.showSlide(2)

        viewModel.previous()

        assertEquals(1, viewModel.index.value)
    }

    @Test
    fun `previous at the start of the deck stays put`() = runVmTest {
        val standaloneEngine = engine()
        val viewModel = vm(standaloneEngine)
        standaloneEngine.loadDeck(deck("one", "two"))

        viewModel.previous()

        assertEquals(0, viewModel.index.value)
    }

    @Test
    fun `an out-of-range tap is clamped rather than blanking the screen`() = runVmTest {
        val standaloneEngine = engine()
        val viewModel = vm(standaloneEngine)
        standaloneEngine.loadDeck(deck("one", "two"))

        viewModel.showSlide(99)

        assertEquals(1, viewModel.index.value)
    }

    // ── Blank, live and text size ────────────────────────────────────────

    @Test
    fun `blanking hides the words and unblanking brings them back`() = runVmTest {
        // The control used more than any other: something goes wrong on the
        // platform and the screen has to go dark in one tap, then come back.
        val standaloneEngine = engine()
        val viewModel = vm(standaloneEngine)
        standaloneEngine.loadDeck(deck("one"))

        viewModel.toggleBlank()
        assertTrue(viewModel.isBlank.value)
        assertTrue(viewModel.currentSlide.value.isHidden)

        viewModel.toggleBlank()
        assertFalse(viewModel.isBlank.value)
        assertFalse(viewModel.currentSlide.value.isHidden)
    }

    @Test
    fun `holding output back hides the slide without losing it`() = runVmTest {
        // Preview: the operator can line the next verse up while the audience
        // still sees the previous state.
        val standaloneEngine = engine()
        val viewModel = vm(standaloneEngine)
        standaloneEngine.loadDeck(deck("one", "two"))

        viewModel.setLive(false)

        assertFalse(viewModel.isLive.value)
        assertTrue(viewModel.currentSlide.value.isHidden)
        assertEquals("one", viewModel.currentSlide.value.body)
    }

    @Test
    fun `going live shows the slide again`() = runVmTest {
        val standaloneEngine = engine()
        val viewModel = vm(standaloneEngine)
        standaloneEngine.loadDeck(deck("one"))
        viewModel.setLive(false)

        viewModel.setLive(true)

        assertTrue(viewModel.isLive.value)
        assertFalse(viewModel.currentSlide.value.isHidden)
    }

    @Test
    fun `the chosen text size rides on the slide`() = runVmTest {
        // The size has to reach the audience screen, not just the controller —
        // it travels inside the slide for exactly that reason.
        val standaloneEngine = engine()
        val viewModel = vm(standaloneEngine)
        standaloneEngine.loadDeck(deck("one"))

        viewModel.setTextSize(SlideTextSize.LARGE)

        assertEquals(SlideTextSize.LARGE, viewModel.textSize.value)
        assertEquals(SlideTextSize.LARGE, viewModel.currentSlide.value.textSize)
    }

    @Test
    fun `the text size survives loading a different deck`() = runVmTest {
        // A church that needs large text needs it for every song, not just the
        // one that was open when they chose it.
        val standaloneEngine = engine()
        val viewModel = vm(standaloneEngine)
        standaloneEngine.loadDeck(deck("one"))
        viewModel.setTextSize(SlideTextSize.SMALL)

        standaloneEngine.loadDeck(deck("a different song"))

        assertEquals(SlideTextSize.SMALL, viewModel.textSize.value)
    }

    // ── Clearing ─────────────────────────────────────────────────────────

    @Test
    fun `clearing unloads the deck and leaves nothing on the screen`() = runVmTest {
        // Note this is not the blackout toggle: isBlank stays false, and the
        // screen is empty because there is no longer a slide to draw.
        val standaloneEngine = engine()
        val viewModel = vm(standaloneEngine)
        standaloneEngine.loadDeck(deck("one", "two"))
        viewModel.showSlide(1)

        viewModel.clear()

        assertTrue(viewModel.deck.value.isEmpty)
        assertEquals(0, viewModel.index.value)
        assertEquals(SlideKind.BLANK, viewModel.currentSlide.value.kind)
        assertEquals("", viewModel.currentSlide.value.body)
    }

    @Test
    fun `clearing lifts a blackout rather than leaving it armed`() = runVmTest {
        // Otherwise the next item loaded would go up to a screen still blacked
        // out, and the operator would be looking for a second button to press.
        val standaloneEngine = engine()
        val viewModel = vm(standaloneEngine)
        standaloneEngine.loadDeck(deck("one"))
        viewModel.toggleBlank()

        viewModel.clear()

        assertFalse(viewModel.isBlank.value)
    }

    @Test
    fun `clearing an already empty controller is harmless`() = runVmTest {
        val viewModel = vm()

        viewModel.clear()

        assertTrue(viewModel.deck.value.isEmpty)
    }

    @Test
    fun `a deck can be loaded again after a clear`() = runVmTest {
        // Clearing between items is the normal rhythm of a service, not an end.
        val standaloneEngine = engine()
        val viewModel = vm(standaloneEngine)
        standaloneEngine.loadDeck(deck("one"))
        viewModel.clear()

        standaloneEngine.loadDeck(deck("next song", "second verse"))
        viewModel.showSlide(0)

        assertEquals(2, viewModel.deck.value.slides.size)
        assertEquals("next song", viewModel.currentSlide.value.body)
    }

    @Test
    fun `opening a song lists it without putting it in front of the congregation`() = runVmTest {
        // Loading used to project, so tapping a song in the list showed it to the
        // room before anyone pressed anything. Browsing and projecting are now
        // separate: the screen holds until showSlide.
        val standaloneEngine = engine()
        val viewModel = vm(standaloneEngine)
        standaloneEngine.loadDeck(deck("one"))
        viewModel.showSlide(0)

        standaloneEngine.loadDeck(deck("a different song"))

        assertEquals("a different song", viewModel.deck.value.slides.single().body)
        assertEquals("one", viewModel.currentSlide.value.body, "the screen should not have moved")
    }
}
