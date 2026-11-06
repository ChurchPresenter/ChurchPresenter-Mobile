package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideBackdrop
import com.church.presenter.churchpresentermobile.model.SlideDeckBuilder
import com.church.presenter.churchpresentermobile.model.SlideFont
import com.church.presenter.churchpresentermobile.model.SlideTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * What the notification shade is told about the screens.
 *
 * It reads like a music player's, so the thing to get right is the same as
 * there: the right song, the words actually on the wall, and an honest LIVE /
 * BLANK / OFF. A shade claiming words are up while the screen is blanked sends
 * an operator looking for a fault that is not there.
 */
class NowShowingTest {

    private fun verse(
        body: String = "Amazing grace, how sweet the sound\nThat saved a wretch like me\nI once was lost",
        index: Int = 0,
        total: Int = 4,
        isBlank: Boolean = false,
        isLive: Boolean = true,
        backdrop: SlideBackdrop = SlideBackdrop.GRADIENT,
        theme: SlideTheme = SlideTheme(),
    ) = Slide(
        body = body,
        index = index,
        total = total,
        isBlank = isBlank,
        isLive = isLive,
        backdrop = backdrop,
        theme = theme,
    )

    @Test
    fun `nothing loaded shows nothing`() {
        assertNull(nowShowing(deckTitle = "", slide = Slide.BLANK))
    }

    @Test
    fun `the deck names it, like a track title`() {
        assertEquals("Amazing Grace", nowShowing("Amazing Grace", verse())?.title)
    }

    @Test
    fun `the words are the first two lines on screen`() {
        // Two is what the shade has room for; the third is a paragraph.
        assertEquals(
            "Amazing grace, how sweet the sound / That saved a wretch like me",
            nowShowing("Amazing Grace", verse())?.words,
        )
    }

    @Test
    fun `blank lines do not count as lines`() {
        val showing = nowShowing("Notices", verse(body = "\n  \nCoffee after\n\nin the hall"))

        assertEquals("Coffee after / in the hall", showing?.words)
    }

    @Test
    fun `the place in the deck is counted from one`() {
        assertEquals("2/4", nowShowing("Amazing Grace", verse(index = 1))?.position)
    }

    @Test
    fun `a single slide has no place to give`() {
        assertNull(nowShowing("Welcome", verse(total = 1))?.position)
    }

    @Test
    fun `a slide on the wall is live`() {
        assertEquals(ScreenState.LIVE, nowShowing("Amazing Grace", verse())?.state)
    }

    @Test
    fun `a blanked slide says so`() {
        assertEquals(ScreenState.BLANK, nowShowing("Amazing Grace", verse(isBlank = true))?.state)
    }

    @Test
    fun `output held back is off, even when also blanked`() {
        // Nothing reaches the room either way; "off" is the one to fix first.
        assertEquals(ScreenState.OFF, nowShowing("Amazing Grace", verse(isLive = false, isBlank = true))?.state)
    }

    @Test
    fun `the picture uses the slide's own colours and face`() {
        val theme = SlideTheme(
            gradientTop = "#112233",
            gradientBottom = "#445566",
            textColor = "#FFEE00",
            font = SlideFont.SANS,
        )

        val showing = assertNotNull(nowShowing("Amazing Grace", verse(theme = theme)))

        assertEquals("#112233", showing.backgroundTop)
        assertEquals("#445566", showing.backgroundBottom)
        assertEquals("#FFEE00", showing.textColor)
        assertEquals(false, showing.serif)
    }

    @Test
    fun `a black backdrop is pictured black, not as the theme's gradient`() {
        val showing = assertNotNull(nowShowing("Amazing Grace", verse(backdrop = SlideBackdrop.BLACK)))

        assertEquals("#000000", showing.backgroundTop)
        assertEquals("#000000", showing.backgroundBottom)
    }

    @Test
    fun `a slide with no deck title falls back to its reference`() {
        assertEquals("John 3:16", nowShowing("", verse().copy(reference = "John 3:16"))?.title)
    }

    // ── Following the engine ─────────────────────────────────────────────

    @Test
    fun `every step reaches the shade, and a repeat does not`() = runTest {
        val engine = StandaloneEngine(MutableStateFlow(AppMode.STANDALONE), SinkRegistry())
        val reported = mutableListOf<NowShowing?>()
        val job = launch { reportNowShowing(engine) { reported += it } }
        runCurrent()

        engine.setDeck(SlideDeckBuilder.fromAnnouncements(listOf("One", "Two"), "Notices"))
        runCurrent()
        engine.next()
        runCurrent()
        engine.showSlide(1)
        runCurrent()
        job.cancel()

        assertNull(reported.first(), "nothing was loaded to begin with")
        assertEquals(listOf("One", "Two"), reported.drop(1).map { it?.words })
    }
}
