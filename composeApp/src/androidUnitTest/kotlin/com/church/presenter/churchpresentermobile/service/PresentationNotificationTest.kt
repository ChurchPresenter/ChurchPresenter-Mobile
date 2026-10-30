package com.church.presenter.churchpresentermobile.service

import com.church.presenter.churchpresentermobile.present.NowShowing
import com.church.presenter.churchpresentermobile.present.ScreenState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * What the presenting notification says — the part of it that can be wrong.
 *
 * Drawing the picture needs a real `Bitmap`, which the unit-test `android.jar`
 * does not have; the words do not, and they are what an operator reads with the
 * phone in a pocket: which song, which words, and whether the room sees them.
 */
class PresentationNotificationTest {

    private fun showing(
        title: String = "Amazing Grace",
        words: String = "Amazing grace, how sweet the sound",
        position: String? = "2/5",
        state: ScreenState = ScreenState.LIVE,
    ) = NowShowing(
        title = title,
        words = words,
        position = position,
        state = state,
        backgroundTop = "#2A1D5E",
        backgroundBottom = "#05060D",
        textColor = "#FFFFFF",
        serif = true,
    )

    private fun text(nowShowing: NowShowing?, url: String? = "http://10.1.10.162:8766") = notificationText(
        nowShowing = nowShowing,
        url = url,
        idleTitle = "Presenting to a screen",
        idleText = "Your screens stay connected",
        stateLabel = { it.name },
    )

    @Test
    fun `with nothing loaded it says the screens are connected and where to look`() {
        val text = text(null)

        assertEquals("Presenting to a screen", text.title)
        assertEquals("http://10.1.10.162:8766", text.text)
        assertNull(text.subText)
    }

    @Test
    fun `with nothing loaded and no browser it still says something`() {
        assertEquals("Your screens stay connected", text(null, url = null).text)
    }

    @Test
    fun `a live slide reads like a playing song`() {
        val text = text(showing())

        assertEquals("Amazing Grace", text.title)
        assertEquals("Amazing grace, how sweet the sound", text.text)
        assertEquals("LIVE · 2/5", text.subText)
    }

    @Test
    fun `a blanked slide does not claim its words are up`() {
        val text = text(showing(state = ScreenState.BLANK))

        assertEquals("http://10.1.10.162:8766", text.text)
        assertEquals("BLANK · 2/5", text.subText)
    }

    @Test
    fun `output held back says off`() {
        assertEquals("OFF · 2/5", text(showing(state = ScreenState.OFF)).subText)
    }

    @Test
    fun `a single slide gives the state alone`() {
        assertEquals("LIVE", text(showing(position = null)).subText)
    }

    @Test
    fun `a slide with no words falls back to the address`() {
        // A picture slide, or a blank section: there is nothing to quote.
        assertEquals("http://10.1.10.162:8766", text(showing(words = "")).text)
    }

    @Test
    fun `an untitled deck keeps the indicator's own title`() {
        assertEquals("Presenting to a screen", text(showing(title = "")).title)
    }
}
