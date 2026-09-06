package com.church.presenter.churchpresentermobile.ui

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
 * Tests [SegmentedControl] — the filter pill above a list.
 *
 * It reads colours from `LocalAppColors`, so every case renders inside [AppTheme]:
 * a bare `setContent` would take the composition-local default and test a
 * configuration the app never shows.
 */
@OptIn(ExperimentalTestApi::class)
class SegmentedControlTest {

    private fun themed(content: @Composable () -> Unit): @Composable () -> Unit = {
        AppTheme(themeMode = ThemeMode.DARK) { content() }
    }

    @Test
    fun aSegmentedControlShowsEveryOption() = runComposeUiTest {
        setContent(themed { SegmentedControl(listOf("All", "Hebrew", "Greek"), 0, {}) })

        onNodeWithText("All").assertExists()
        onNodeWithText("Hebrew").assertExists()
        onNodeWithText("Greek").assertExists()
    }

    @Test
    fun tappingASegmentReportsItsIndex() = runComposeUiTest {
        var picked = -1
        setContent(themed { SegmentedControl(listOf("All", "Hebrew", "Greek"), 0, { picked = it }) })

        onNodeWithText("Greek").performClick()

        assertEquals(2, picked)
    }

    @Test
    fun everySegmentIsTappableIncludingTheSelectedOne() = runComposeUiTest {
        // Re-tapping the active segment must not be a dead zone; screens rely on
        // it to re-run a search.
        var picked = -1
        setContent(themed { SegmentedControl(listOf("All", "Hebrew"), 0, { picked = it }) })

        onNodeWithText("All").assertHasClickAction().performClick()

        assertEquals(0, picked)
    }

    @Test
    fun aSingleOptionControlStillRenders() = runComposeUiTest {
        setContent(themed { SegmentedControl(listOf("Only"), 0, {}) })

        onNodeWithText("Only").assertExists()
    }

    @Test
    fun anEmptyOptionListRendersNothingRatherThanCrashing() = runComposeUiTest {
        setContent(themed { SegmentedControl(emptyList(), 0, {}) })

        onNodeWithText("Only").assertDoesNotExist()
    }

    @Test
    fun aSegmentedControlHandlesManyOptions() = runComposeUiTest {
        val options = List(6) { "Opt$it" }
        setContent(themed { SegmentedControl(options, 0, {}) })

        options.forEach { onNodeWithText(it).assertExists() }
    }

    @Test
    fun aSegmentedControlSelectionOutOfRangeStillRenders() = runComposeUiTest {
        // Reachable for a frame while a filter list is reloading.
        setContent(themed { SegmentedControl(listOf("All", "Hebrew"), selectedIndex = 5, onSelect = {}) })

        onNodeWithText("All").assertExists()
    }
}
