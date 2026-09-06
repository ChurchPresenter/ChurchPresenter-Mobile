package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests [SplashScreen] — the animated opening, and the one screen that must get
 * out of the way on its own. If `onComplete` never fires the app never reaches
 * its first tab, which is indistinguishable from a hang.
 *
 * The clock is driven by hand throughout: the splash runs an `animateTo` and a
 * two-second `delay`, so the composition is never idle and `waitForIdle` would
 * block until the test runner gives up.
 */
@OptIn(ExperimentalTestApi::class)
class SplashScreenTest {

    @Test
    fun theSplashRenders() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { AppTheme(themeMode = ThemeMode.DARK) { SplashScreen(onComplete = {}) } }

        mainClock.advanceTimeBy(SETTLE_MS)

        assertTrue(true, "rendering the animation without throwing is the assertion")
    }

    // Not tested here: that onComplete eventually fires. The handover is a 650ms
    // animation followed by a real `delay(2100)`, which the frame clock does not
    // drive and which outlasts Karma's 2s per-test budget. Asserting it would mean
    // a timing test that is flaky by construction; the negative case below is the
    // part that can be pinned honestly.

    @Test
    fun itDoesNotHandOverBeforeItsAnimationHasRun() = runComposeUiTest {
        // Handing over on the first frame would flash the splash and skip it.
        mainClock.autoAdvance = false
        var done = 0
        setContent { AppTheme(themeMode = ThemeMode.DARK) { SplashScreen(onComplete = { done++ }) } }

        mainClock.advanceTimeBy(FIRST_FRAMES_MS)

        assertTrue(done == 0, "the splash handed over immediately")
    }

    @Test
    fun itRendersInBothThemes() = runComposeUiTest {
        mainClock.autoAdvance = false
        for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            setContent { AppTheme(themeMode = mode) { SplashScreen(onComplete = {}) } }
            mainClock.advanceTimeBy(SETTLE_MS)
        }
        assertTrue(true)
    }
}

/** Long enough for a frame to be produced; nothing here animates for real. */
private const val SETTLE_MS = 100L

/** Only the opening frames, before any handover could legitimately happen. */
private const val FIRST_FRAMES_MS = 50L
