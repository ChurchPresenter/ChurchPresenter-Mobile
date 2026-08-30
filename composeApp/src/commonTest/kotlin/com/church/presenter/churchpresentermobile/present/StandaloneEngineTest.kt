package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideBackdrop
import com.church.presenter.churchpresentermobile.model.SlideDeck
import com.church.presenter.churchpresentermobile.model.SlideEnvelope
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.model.SlideMessageType
import com.church.presenter.churchpresentermobile.model.SlideTextSize
import com.church.presenter.churchpresentermobile.network.WsMessageType
import com.church.presenter.churchpresentermobile.testutil.FakeOutputSink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StandaloneEngineTest {

    private class Fixture(mode: AppMode = AppMode.STANDALONE) {
        val modeFlow = MutableStateFlow(mode)
        val registry = SinkRegistry()
        val sink = FakeOutputSink()
        val published = mutableListOf<SlideEnvelope>()
        val engine: StandaloneEngine

        init {
            registry.register(sink)
            engine = StandaloneEngine(modeFlow, registry) { published += it }
        }
    }

    private fun deckOf(count: Int, kind: SlideKind = SlideKind.SONG) = SlideDeck(
        kind = kind,
        title = "deck",
        slides = List(count) { Slide(kind = kind, body = "slide $it", reference = "ref $it") },
    )

    // ── Mode gating ──────────────────────────────────────────────────────

    @Test
    fun `every mutator is a no-op in remote mode`() {
        val f = Fixture(AppMode.REMOTE)

        f.engine.setDeck(deckOf(3))
        f.engine.showSlide(1)
        f.engine.setBlank(true)
        f.engine.setLive(false)
        f.engine.setTextSize(SlideTextSize.LARGE)
        f.engine.setBackdrop(SlideBackdrop.BLACK)
        f.engine.clear()

        assertTrue(f.published.isEmpty(), "remote mode must never publish a slide")
        assertTrue(f.sink.rendered.isEmpty(), "remote mode must never reach a sink")
        assertTrue(f.engine.deck.value.isEmpty)
        assertEquals(Slide.BLANK, f.engine.currentSlide.value)
    }

    @Test
    fun `switching to standalone at runtime activates the engine`() {
        val f = Fixture(AppMode.REMOTE)
        f.engine.setDeck(deckOf(2))
        assertTrue(f.published.isEmpty())

        f.modeFlow.value = AppMode.STANDALONE
        f.engine.setDeck(deckOf(2))

        assertEquals(1, f.published.size)
        assertEquals("slide 0", f.published.last().slide?.body)
    }

    // ── Deck loading and navigation ──────────────────────────────────────

    @Test
    fun `setDeck projects the first slide`() {
        val f = Fixture()
        f.engine.setDeck(deckOf(4))

        assertEquals(0, f.engine.index.value)
        assertEquals("slide 0", f.engine.currentSlide.value.body)
        assertEquals(4, f.engine.currentSlide.value.total)
    }

    @Test
    fun `setDeck clears a previous blank`() {
        val f = Fixture()
        f.engine.setDeck(deckOf(2))
        f.engine.setBlank(true)
        assertTrue(f.engine.isBlank.value)

        f.engine.setDeck(deckOf(3))
        assertFalse(f.engine.isBlank.value)
    }

    @Test
    fun `setDeck preserves the live flag`() {
        val f = Fixture()
        f.engine.setLive(false)
        f.engine.setDeck(deckOf(2))
        assertFalse(f.engine.isLive.value)
    }

    @Test
    fun `next stops at the end of the deck instead of wrapping`() {
        val f = Fixture()
        f.engine.setDeck(deckOf(3))

        f.engine.next()
        f.engine.next()
        assertEquals(2, f.engine.index.value)

        f.engine.next()
        assertEquals(2, f.engine.index.value)
        assertEquals("slide 2", f.engine.currentSlide.value.body)
    }

    @Test
    fun `previous stops at the start of the deck instead of wrapping`() {
        val f = Fixture()
        f.engine.setDeck(deckOf(3))

        f.engine.previous()
        assertEquals(0, f.engine.index.value)
        assertEquals("slide 0", f.engine.currentSlide.value.body)
    }

    @Test
    fun `showSlide clamps an out-of-range index`() {
        val f = Fixture()
        f.engine.setDeck(deckOf(3))

        f.engine.showSlide(99)
        assertEquals(2, f.engine.index.value)

        f.engine.showSlide(-5)
        assertEquals(0, f.engine.index.value)
    }

    @Test
    fun `showSlide on an empty deck emits the blank slide`() {
        val f = Fixture()
        f.engine.showSlide(2)

        assertEquals(0, f.engine.index.value)
        assertEquals(SlideKind.BLANK, f.engine.currentSlide.value.kind)
    }

    @Test
    fun `showSlide clears a blank`() {
        val f = Fixture()
        f.engine.setDeck(deckOf(3))
        f.engine.setBlank(true)

        f.engine.showSlide(1)
        assertFalse(f.engine.isBlank.value)
        assertFalse(f.engine.currentSlide.value.isBlank)
    }

    // ── Blank / live ─────────────────────────────────────────────────────

    @Test
    fun `toggleBlank flips the blank flag onto the projected slide`() {
        val f = Fixture()
        f.engine.setDeck(deckOf(2))

        f.engine.toggleBlank()
        assertTrue(f.engine.currentSlide.value.isBlank)
        assertTrue(f.engine.currentSlide.value.isHidden)

        f.engine.toggleBlank()
        assertFalse(f.engine.currentSlide.value.isBlank)
        assertFalse(f.engine.currentSlide.value.isHidden)
    }

    @Test
    fun `blanking keeps the deck and index intact so restoring resumes in place`() {
        val f = Fixture()
        f.engine.setDeck(deckOf(4))
        f.engine.showSlide(2)

        f.engine.setBlank(true)
        assertEquals(2, f.engine.index.value)
        assertEquals("slide 2", f.engine.currentSlide.value.body)

        f.engine.setBlank(false)
        assertEquals("slide 2", f.engine.currentSlide.value.body)
        assertFalse(f.engine.currentSlide.value.isHidden)
    }

    @Test
    fun `not live hides the slide without blanking it`() {
        val f = Fixture()
        f.engine.setDeck(deckOf(2))
        f.engine.setLive(false)

        val slide = f.engine.currentSlide.value
        assertFalse(slide.isBlank)
        assertTrue(slide.isHidden)
    }

    // ── Styling overrides ────────────────────────────────────────────────

    @Test
    fun `operator styling is applied to every subsequent slide`() {
        val f = Fixture()
        f.engine.setTextSize(SlideTextSize.LARGE)
        f.engine.setBackdrop(SlideBackdrop.IMAGE, "http://phone/assets/bg.jpg")
        f.engine.setDeck(deckOf(3))
        f.engine.next()

        val slide = f.engine.currentSlide.value
        assertEquals(SlideTextSize.LARGE, slide.textSize)
        assertEquals(SlideBackdrop.IMAGE, slide.backdrop)
        assertEquals("http://phone/assets/bg.jpg", slide.backdropUrl)
    }

    @Test
    fun `setBackdrop without a url drops a stale image url`() {
        val f = Fixture()
        f.engine.setDeck(deckOf(1))
        f.engine.setBackdrop(SlideBackdrop.IMAGE, "http://phone/assets/bg.jpg")

        f.engine.setBackdrop(SlideBackdrop.BLACK)
        assertEquals(null, f.engine.currentSlide.value.backdropUrl)
    }

    // ── Emission ─────────────────────────────────────────────────────────

    @Test
    fun `revision increases monotonically across every emission`() {
        val f = Fixture()
        f.engine.setDeck(deckOf(3))
        f.engine.next()
        f.engine.setBlank(true)
        f.engine.setLive(false)
        f.engine.setTextSize(SlideTextSize.SMALL)

        val revisions = f.published.map { it.rev }
        assertEquals(5, revisions.size)
        assertEquals(revisions.sorted(), revisions)
        assertEquals(revisions.distinct(), revisions)
    }

    @Test
    fun `every emission reaches both the publisher and the sinks`() {
        val f = Fixture()
        f.engine.setDeck(deckOf(2))
        f.engine.next()

        assertEquals(2, f.published.size)
        assertEquals(2, f.sink.rendered.size)
        assertEquals(f.published.map { it.rev }, f.sink.rendered.map { it.rev })
    }

    @Test
    fun `index and total are stamped onto the projected slide`() {
        val f = Fixture()
        f.engine.setDeck(deckOf(5))
        f.engine.showSlide(3)

        val slide = f.published.last().slide!!
        assertEquals(3, slide.index)
        assertEquals(5, slide.total)
    }

    // ── clear / remote actions ───────────────────────────────────────────

    @Test
    fun `clear unloads the deck and emits a CLEAR envelope`() {
        val f = Fixture()
        f.engine.setDeck(deckOf(3))
        f.engine.next()

        f.engine.clear()

        assertTrue(f.engine.deck.value.isEmpty)
        assertEquals(0, f.engine.index.value)
        assertEquals(SlideMessageType.CLEAR, f.published.last().type)
        assertEquals(SlideKind.BLANK, f.engine.currentSlide.value.kind)
    }

    @Test
    fun `a remote clear action blanks the local screen`() {
        val f = Fixture()
        f.engine.setDeck(deckOf(3))

        val result = f.engine.handleRemoteAction(WsMessageType.CLEAR, "{}")

        assertTrue(result.isSuccess)
        assertEquals(SlideMessageType.CLEAR, f.published.last().type)
        assertTrue(f.engine.deck.value.isEmpty)
    }

    @Test
    fun `desktop-only actions are swallowed successfully and change nothing`() {
        val f = Fixture()
        f.engine.setDeck(deckOf(3))
        val before = f.published.size

        val types = listOf(
            WsMessageType.ADD_TO_SCHEDULE,
            WsMessageType.ADD_BATCH_TO_SCHEDULE,
            WsMessageType.BIBLE_HOLD,
            WsMessageType.MEDIA_PLAY_PAUSE,
            WsMessageType.MEDIA_STOP,
            WsMessageType.SELECT_PICTURE,
        )
        types.forEach { type ->
            assertTrue(f.engine.handleRemoteAction(type, "{}").isSuccess, "$type should not fail")
        }

        assertEquals(before, f.published.size, "desktop-only actions must not emit a slide")
        assertEquals("slide 0", f.engine.currentSlide.value.body)
    }

    // ── Slides that carry their own image ────────────────────────────────

    @Test
    fun `a photo slide keeps its own image rather than the operator's backdrop`() {
        // The backdrop control chooses what sits behind *text*. A photo slide is
        // the picture itself, so applying the global backdrop would repaint every
        // photo in the set with the same one.
        val f = Fixture()
        f.engine.setBackdrop(SlideBackdrop.GRADIENT)
        f.engine.setDeck(
            SlideDeck(
                kind = SlideKind.IMAGE,
                slides = listOf(
                    Slide(kind = SlideKind.IMAGE, backdrop = SlideBackdrop.IMAGE, backdropUrl = "http://phone/photo/a"),
                    Slide(kind = SlideKind.IMAGE, backdrop = SlideBackdrop.IMAGE, backdropUrl = "http://phone/photo/b"),
                ),
            )
        )

        assertEquals(SlideBackdrop.IMAGE, f.engine.currentSlide.value.backdrop)
        assertEquals("http://phone/photo/a", f.engine.currentSlide.value.backdropUrl)

        f.engine.next()

        assertEquals("http://phone/photo/b", f.engine.currentSlide.value.backdropUrl)
    }

    @Test
    fun `a text slide still takes the operator's backdrop`() {
        val f = Fixture()
        f.engine.setDeck(deckOf(2))

        f.engine.setBackdrop(SlideBackdrop.BLACK)

        assertEquals(SlideBackdrop.BLACK, f.engine.currentSlide.value.backdrop)
    }

    @Test
    fun `an image backdrop with no url falls back to the operator's choice`() {
        val f = Fixture()
        f.engine.setBackdrop(SlideBackdrop.BLACK)

        f.engine.setDeck(
            SlideDeck(
                kind = SlideKind.IMAGE,
                slides = listOf(Slide(kind = SlideKind.IMAGE, backdrop = SlideBackdrop.IMAGE, backdropUrl = null)),
            )
        )

        assertEquals(SlideBackdrop.BLACK, f.engine.currentSlide.value.backdrop)
    }
    // ── Browsing versus projecting ───────────────────────────────────────

    @Test
    fun loadingADeckPutsNothingOnTheAudienceScreen() {
        // Tapping a song in the list used to project it. Opening something to look at it is not
        // the same as showing it to a congregation.
        val f = Fixture()

        f.engine.loadDeck(deckOf(3))

        assertTrue(f.published.isEmpty())
        assertTrue(f.sink.rendered.isEmpty())
    }

    @Test
    fun loadingADeckStillFillsTheSectionList() {
        // The operator has to see what they are about to show, so the deck itself is loaded —
        // it is only the projecting that waits.
        val f = Fixture()

        f.engine.loadDeck(deckOf(3))

        assertEquals(3, f.engine.deck.value.slides.size)
        assertEquals(0, f.engine.index.value)
    }

    @Test
    fun goingLiveProjectsWhatWasLoaded() {
        val f = Fixture()
        f.engine.loadDeck(deckOf(3))

        f.engine.goLive()

        assertEquals(1, f.published.size)
        assertEquals("slide 0", f.published.single().slide?.body)
    }

    @Test
    fun goingLiveTakesTheScreenBackOnAir() {
        // Both ways of being off-air have to give way. The audience page hides
        // everything while isBlank or !isLive, so an output the operator had
        // taken off-air used to ignore "go live" entirely.
        val f = Fixture()
        f.engine.loadDeck(deckOf(3))
        f.engine.setLive(false)
        f.published.clear()

        f.engine.goLive()

        assertTrue(f.engine.isLive.value, "go live must put the output back on air")
        assertEquals("slide 0", f.published.last().slide?.body)
        assertFalse(f.published.last().slide?.isLive == false)
    }

    @Test
    fun goingLiveKeepsTheSlideTheOperatorHadMovedTo() {
        val f = Fixture()
        f.engine.loadDeck(deckOf(3))
        f.engine.showSlide(2)
        f.published.clear()

        f.engine.goLive()

        assertEquals("slide 2", f.published.single().slide?.body)
    }

    @Test
    fun presentingSomethingOutrightStillProjectsAtOnce() {
        // setDeck is what "present this now" uses — the library, a web page, a set of photos.
        // Splitting browsing off must not have made those wait for a second press.
        val f = Fixture()

        f.engine.setDeck(deckOf(2))

        assertEquals(1, f.published.size)
        assertEquals("slide 0", f.published.single().slide?.body)
    }

    @Test
    fun loadingADeckClearsABlackedOutScreenOnlyWhenItGoesLive() {
        val f = Fixture()
        f.engine.setDeck(deckOf(2))
        f.engine.setBlank(true)
        f.published.clear()

        f.engine.loadDeck(deckOf(2))

        // Nothing was published, so the audience is still looking at the blank.
        assertTrue(f.published.isEmpty())
    }

    // ── The reference line ───────────────────────────────────────────────

    @Test
    fun theReferenceLineIsShownUnlessTheOperatorTurnsItOff() {
        val f = Fixture()
        f.engine.setDeck(deckOf(1))

        assertTrue(f.published.single().slide!!.theme.showSongReference)
    }

    @Test
    fun turningTheReferenceOffReachesTheProjectedSlide() {
        // Some churches want the words and nothing else on screen.
        val f = Fixture()
        f.engine.setDeck(deckOf(1))

        f.engine.setTheme(f.engine.theme.value.copy(showSongReference = false))

        assertFalse(f.published.last().slide!!.theme.showSongReference)
        // The reference itself is still carried — it is the renderers that stop drawing it, so
        // turning it back on needs no reload.
        assertEquals("ref 0", f.published.last().slide?.reference)
    }

    // ── Blank and Clear are different acts ───────────────────────────────────

    @Test
    fun blankingHidesTheWordsButKeepsTheSong() {
        // The operator wants the screen dark for a moment; the next press has to
        // bring the same verse straight back, so the deck must survive.
        val f = Fixture()
        f.engine.setDeck(deckOf(3))
        f.engine.showSlide(1)

        f.engine.toggleBlank()

        assertTrue(f.published.last().slide!!.isBlank)
        assertEquals(3, f.engine.deck.value.slides.size, "blanking must not put the song down")
        assertEquals(1, f.engine.index.value, "nor lose the operator's place in it")
    }

    @Test
    fun clearingPutsTheSongDown() {
        val f = Fixture()
        f.engine.setDeck(deckOf(3))
        f.engine.showSlide(1)

        f.engine.clear()

        assertEquals(0, f.engine.deck.value.slides.size)
        assertEquals(0, f.engine.index.value)
        assertFalse(f.published.last().slide!!.isBlank, "an empty screen is not a blanked one")
    }

    @Test
    fun blankingAfterAClearStillLeavesNothingLoaded() {
        val f = Fixture()
        f.engine.setDeck(deckOf(2))
        f.engine.clear()

        f.engine.toggleBlank()

        assertEquals(0, f.engine.deck.value.slides.size)
    }
}
