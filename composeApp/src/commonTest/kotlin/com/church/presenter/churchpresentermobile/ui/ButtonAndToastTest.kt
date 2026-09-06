package com.church.presenter.churchpresentermobile.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the reusable building blocks every screen is assembled from.
 *
 * They read colours from `LocalAppColors`, so each case wraps its content in
 * [AppTheme] — a bare `setContent` would take the composition-local default and
 * test a configuration the app never renders.
 */
/**
 * Tests [OutlineActionButton] — the outline action button and the live toast.
 *
 * It reads colours from `LocalAppColors`, so every case renders inside [AppTheme]:
 * a bare `setContent` would take the composition-local default and test a
 * configuration the app never shows.
 */
@OptIn(ExperimentalTestApi::class)
class ButtonAndToastTest {

    private fun themed(content: @Composable () -> Unit): @Composable () -> Unit = {
        AppTheme(themeMode = ThemeMode.DARK) { content() }
    }

    @Test
    fun anActionButtonShowsItsLabelAndIsTappable() = runComposeUiTest {
        var clicks = 0
        setContent(themed {
            OutlineActionButton(label = "Sync now", icon = Icons.Filled.Refresh, onClick = { clicks++ })
        })

        onNodeWithText("Sync now").assertHasClickAction().performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun anActionButtonCanBeTappedRepeatedly() = runComposeUiTest {
        var clicks = 0
        setContent(themed {
            OutlineActionButton(label = "Retry", icon = Icons.Filled.Refresh, onClick = { clicks++ })
        })

        onNodeWithText("Retry").performClick()
        onNodeWithText("Retry").performClick()

        assertEquals(2, clicks)
    }

    @Test
    fun aToastShowsItsMessage() = runComposeUiTest {
        setContent(themed { LiveToast(message = "Song is live") })

        onNodeWithText("Song is live").assertExists()
    }

    @Test
    fun aToastRendersAnEmptyMessageWithoutCrashing() = runComposeUiTest {
        setContent(themed { LiveToast(message = "") })

        onNodeWithText("Song is live").assertDoesNotExist()
    }

    @Test
    fun aToastRendersALongMessage() = runComposeUiTest {
        val long = "Failed to project song: Server not reachable. Check the IP address and port."
        setContent(themed { LiveToast(message = long) })

        onNodeWithText(long).assertExists()
    }
}
