package com.church.presenter.churchpresentermobile.ui.tv

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.tv.LiveState
import com.church.presenter.churchpresentermobile.tv.SourceKind
import com.church.presenter.churchpresentermobile.tv.TvSource
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.showScreen
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The two things a TV does for whoever is not there: say what it is on, and come back by itself. */
@OptIn(ExperimentalTestApi::class)
class TvInfoAndBootTest {

    private val phone = TvSource("Pixel", "192.168.1.20", 8766, SourceKind.PHONE)

    private fun settings() = AppSettings(InMemorySettingsStorage())

    // ── Holding OK ───────────────────────────────────────────────────────

    @Test
    fun aPressOfOkIsNotAHold() {
        val key = HeldOkKey()

        assertFalse(key.update(isOk = true, isDown = true))
        assertFalse(key.update(isOk = true, isDown = false))
        assertFalse(key.update(isOk = true, isDown = true))
    }

    @Test
    fun aRepeatedKeyDownIsAHold() {
        val key = HeldOkKey()
        key.update(isOk = true, isDown = true)

        assertTrue(key.update(isOk = true, isDown = true))
    }

    @Test
    fun otherKeysAreNeverAHold() {
        val key = HeldOkKey()

        assertFalse(key.update(isOk = false, isDown = true))
        assertFalse(key.update(isOk = false, isDown = true))
    }

    @Test
    fun theInfoCardDrawsWhileConnecting() = runComposeUiTest {
        showScreen { TvInfoOverlay(phone, LiveState.Connecting) }

        assertTrue(exists(TvTags.INFO))
    }

    // ── Starting with the TV ─────────────────────────────────────────────

    @Test
    fun aTvOnlyStartsItselfWhenAskedTo() {
        val settings = settings()
        settings.tvLastSource = phone

        assertFalse(settings.tvResumesOnBoot)
    }

    @Test
    fun aTvShowingNothingStaysOutOfTheWayAtBoot() {
        val settings = settings()
        settings.tvStartOnBoot = true

        assertFalse(settings.tvResumesOnBoot)
    }

    @Test
    fun aTvThatWasShowingSomethingStartsItself() {
        val live = settings().apply {
            tvStartOnBoot = true
            tvLastSource = phone
        }
        val signage = settings().apply {
            tvStartOnBoot = true
            tvSignageOn = true
        }

        assertTrue(live.tvResumesOnBoot)
        assertTrue(signage.tvResumesOnBoot)
    }

    @Test
    fun theHomeScreenTurnsStartingWithTheTvOnAndOff() = runComposeUiTest {
        val settings = settings()
        showScreen { TvApp(settings) }

        click(TvTags.HOME_BOOT)
        waitForIdle()
        assertTrue(settings.tvStartOnBoot)

        click(TvTags.HOME_BOOT)
        waitForIdle()
        assertFalse(settings.tvStartOnBoot)
    }
}
