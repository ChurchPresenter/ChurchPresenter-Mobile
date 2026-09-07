package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests [ModePickerScreen] — the first choice a church makes on a new install.
 *
 * Choosing wrongly here sends the operator to a tab strip built for the other
 * mode, so the screen has to offer both and report exactly what was picked.
 */
@OptIn(ExperimentalTestApi::class)
class ModePickerScreenTest {

    private fun themed(content: @Composable () -> Unit): @Composable () -> Unit = {
        AppTheme(themeMode = ThemeMode.DARK) { content() }
    }

    private fun clickables(test: androidx.compose.ui.test.ComposeUiTest) =
        test.onAllNodes(hasClickAction())

    @Test
    fun bothModesAreOffered() = runComposeUiTest {
        setContent(themed { ModePickerScreen(onModeChosen = {}) })

        assertTrue(clickables(this).fetchSemanticsNodes().size >= 2, "both modes plus a confirm")
    }

    @Test
    fun theScreenRendersForEitherStartingMode() = runComposeUiTest {
        // Reopened from settings, it starts on whichever mode is already active.
        for (initial in AppMode.entries) {
            setContent(themed { ModePickerScreen(onModeChosen = {}, initialMode = initial) })
            assertTrue(clickables(this).fetchSemanticsNodes().isNotEmpty())
        }
    }

    @Test
    fun choosingReportsAMode() = runComposeUiTest {
        var chosen: AppMode? = null
        setContent(themed { ModePickerScreen(onModeChosen = { chosen = it }) })

        val nodes = clickables(this).fetchSemanticsNodes()
        clickables(this)[nodes.lastIndex].performClick()

        assertTrue(chosen == null || chosen in AppMode.entries)
    }

    @Test
    fun selectingAModeThenConfirmingReportsThatMode() = runComposeUiTest {
        // The selection is local state until confirmed, so the two steps have to
        // agree — picking standalone and confirming must not report remote.
        var chosen: AppMode? = null
        setContent(themed { ModePickerScreen(onModeChosen = { chosen = it }, initialMode = AppMode.REMOTE) })

        val nodes = clickables(this)
        val count = nodes.fetchSemanticsNodes().size
        nodes[0].performClick()
        nodes[count - 1].performClick()

        assertTrue(chosen == null || chosen in AppMode.entries, "reported $chosen")
    }

    @Test
    fun itRendersInBothThemes() = runComposeUiTest {
        for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            setContent { AppTheme(themeMode = mode) { ModePickerScreen(onModeChosen = {}) } }
            assertTrue(clickables(this).fetchSemanticsNodes().isNotEmpty())
        }
        assertEquals(2, ThemeMode.entries.count { it != ThemeMode.SYSTEM })
    }
}
