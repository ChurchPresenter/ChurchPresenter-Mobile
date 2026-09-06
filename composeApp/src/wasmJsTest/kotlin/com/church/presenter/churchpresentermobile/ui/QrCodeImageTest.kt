package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests [QrCodeImage] — the code a phone scans to reach the standalone display.
 *
 * The content is a URL the operator cannot retype from across a hall, so the
 * component has to render whatever it is handed rather than refusing awkward
 * input.
 */
@OptIn(ExperimentalTestApi::class)
class QrCodeImageTest {

    private fun themed(content: @Composable () -> Unit): @Composable () -> Unit = {
        AppTheme(themeMode = ThemeMode.DARK) { content() }
    }

    @Test
    fun aDisplayUrlRenders() = runComposeUiTest {
        setContent(themed { QrCodeImage(content = "http://192.168.1.42:8080/display") })

        mainClock.advanceTimeBy(SETTLE_MS)
        assertTrue(true, "rendering without throwing is the assertion")
    }

    @Test
    fun anEmptyStringDoesNotCrash() = runComposeUiTest {
        // Reachable for a frame before the server has bound a port.
        setContent(themed { QrCodeImage(content = "") })

        mainClock.advanceTimeBy(SETTLE_MS)
        assertTrue(true)
    }

    @Test
    fun aLongUrlRenders() = runComposeUiTest {
        val long = "http://192.168.1.42:8080/display?" + "a=1&".repeat(LONG_QUERY_PAIRS)
        setContent(themed { QrCodeImage(content = long) })

        mainClock.advanceTimeBy(SETTLE_MS)
        assertTrue(true)
    }

    @Test
    fun everySizeRenders() = runComposeUiTest {
        for (size in listOf(48.dp, 160.dp, 320.dp)) {
            setContent(themed { QrCodeImage(content = "http://x/display", size = size) })
            mainClock.advanceTimeBy(SETTLE_MS)
        }
        assertTrue(true)
    }

    @Test
    fun itRendersInBothThemes() = runComposeUiTest {
        for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            setContent { AppTheme(themeMode = mode) { QrCodeImage(content = "http://x/display") } }
            mainClock.advanceTimeBy(SETTLE_MS)
        }
        assertTrue(true)
    }
}

/** Long enough for a frame to be produced; nothing here animates for real. */
private const val SETTLE_MS = 100L

/** Enough query pairs to push the code into its densest error-correction size. */
private const val LONG_QUERY_PAIRS = 50
