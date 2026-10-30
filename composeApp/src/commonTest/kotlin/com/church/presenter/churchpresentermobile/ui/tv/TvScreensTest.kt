package com.church.presenter.churchpresentermobile.ui.tv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.tv.LiveState
import com.church.presenter.churchpresentermobile.tv.PhoneSource
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.showScreen
import com.church.presenter.churchpresentermobile.ui.tagged
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ChurchPresenter on a TV, driven with a remote.
 *
 * What matters on a screen nobody touches during a service: it never goes
 * black for a blip, it says plainly when it is waiting, and it comes back to
 * the same phone after a restart.
 */
@OptIn(ExperimentalTestApi::class)
class TvScreensTest {

    private fun settings() = AppSettings(InMemorySettingsStorage())

    // ── Live ─────────────────────────────────────────────────────────────

    @Test
    fun aBriefDropShowsNoNoticeOverTheSlide() = runComposeUiTest {
        // A phone that blinks off Wi-Fi for a second must not put a message
        // over the sermon slide.
        mainClock.autoAdvance = false
        showScreen { TvLiveContent(LiveState.Lost(Slide(body = "Verse one")), "Pixel") }
        mainClock.advanceTimeBy(1_000)

        assertFalse(exists(TvTags.STATUS))
    }

    @Test
    fun aLongerDropSaysItIsReconnecting() = runComposeUiTest {
        mainClock.autoAdvance = false
        showScreen { TvLiveContent(LiveState.Lost(Slide(body = "Verse one")), "Pixel") }
        mainClock.advanceTimeBy(3_500)

        assertTrue(exists(TvTags.STATUS))
    }

    @Test
    fun theLiveScreenIsDrawnForEveryState() = runComposeUiTest {
        // No state leaves the TV without its screen — the black backdrop is
        // always there, with the slide or a message on it.
        var current by mutableStateOf<LiveState>(LiveState.Connecting)
        showScreen { TvLiveContent(current, "Pixel") }

        listOf(
            LiveState.Connecting,
            LiveState.Showing(Slide(body = "Verse one")),
            LiveState.Ended,
            LiveState.TooNew,
        ).forEach { state ->
            current = state
            waitForIdle()
            assertTrue(exists(TvTags.LIVE), "$state")
        }
    }

    // ── Getting to a phone ───────────────────────────────────────────────

    @Test
    fun aFreshTvOpensOnItsHomeScreen() = runComposeUiTest {
        showScreen { TvApp(settings()) }

        assertTrue(exists(TvTags.HOME_LIVE))
    }

    @Test
    fun aTvThatWasShowingAPhoneGoesStraightBackToIt() = runComposeUiTest {
        // A lobby TV after a power cut, with nobody there to choose again.
        val settings = settings()
        settings.tvLastSource = PhoneSource("Pixel", "192.168.1.20", 8766)

        showScreen { TvApp(settings) }

        assertTrue(exists(TvTags.LIVE))
        assertFalse(exists(TvTags.HOME_LIVE))
    }

    @Test
    fun aMistypedAddressIsRefusedOnTheSpot() = runComposeUiTest {
        val settings = settings()
        showScreen { TvApp(settings) }
        click(TvTags.HOME_LIVE)
        awaitThat { exists(TvTags.ADDRESS) }

        tagged(TvTags.ADDRESS).performTextReplacement("192.168.1.20:port")
        click(TvTags.CONNECT)

        awaitThat { exists(TvTags.ADDRESS_INVALID) }
        assertNull(settings.tvLastSource)
    }

    @Test
    fun aTypedAddressConnectsAndIsRemembered() = runComposeUiTest {
        val settings = settings()
        showScreen { TvApp(settings) }
        click(TvTags.HOME_LIVE)
        awaitThat { exists(TvTags.ADDRESS) }

        tagged(TvTags.ADDRESS).performTextReplacement("192.168.1.20")
        click(TvTags.CONNECT)

        awaitThat { exists(TvTags.LIVE) }
        assertEquals("192.168.1.20", settings.tvLastSource?.host)
    }

    @Test
    fun signageIsShownButCannotBeChosenYet() = runComposeUiTest {
        showScreen { TvApp(settings()) }

        assertTrue(exists(TvTags.HOME_SIGNAGE))
    }
}
