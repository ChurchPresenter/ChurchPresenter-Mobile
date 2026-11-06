package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideTheme
import com.church.presenter.churchpresentermobile.ui.isShowing
import com.church.presenter.churchpresentermobile.ui.showScreen
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The clock in the corner of an output, drawn on every screen the Clock switch reaches. */
@OptIn(ExperimentalTestApi::class)
class OutputClockTest {

    private val slide = Slide(body = "Amazing grace")

    @Test
    fun theTimeIsTwentyFourHourAndPadded() {
        assertEquals("09:05", clockText(LocalDateTime(2026, 11, 5, 9, 5)))
        assertEquals("21:40", clockText(LocalDateTime(2026, 11, 5, 21, 40)))
    }

    @Test
    fun anOutputShowsTheClock() = runComposeUiTest {
        showScreen {
            CompositionLocalProvider(LocalFixedClock provides "10:30") { StandaloneOutputScreen(slide) }
        }

        assertTrue(isShowing("10:30"))
    }

    @Test
    fun turningTheClockOffRemovesIt() = runComposeUiTest {
        showScreen {
            CompositionLocalProvider(LocalFixedClock provides "10:30") {
                StandaloneOutputScreen(slide.copy(theme = SlideTheme(showClock = false)))
            }
        }

        assertFalse(isShowing("10:30"))
    }

    @Test
    fun aPresetsThumbnailHasNoClock() = runComposeUiTest {
        showScreen {
            CompositionLocalProvider(LocalFixedClock provides "10:30") {
                StandaloneOutputScreen(slide, showClock = false)
            }
        }

        assertFalse(isShowing("10:30"))
    }
}
