package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import kotlin.test.Test

/**
 * Tests the reusable building blocks every screen is assembled from.
 *
 * They read colours from `LocalAppColors`, so each case wraps its content in
 * [AppTheme] — a bare `setContent` would take the composition-local default and
 * test a configuration the app never renders.
 */
/**
 * Tests [OverlineRow] — the small-caps section headings.
 *
 * It reads colours from `LocalAppColors`, so every case renders inside [AppTheme]:
 * a bare `setContent` would take the composition-local default and test a
 * configuration the app never shows.
 */
@OptIn(ExperimentalTestApi::class)
class OverlineRowTest {

    private fun themed(content: @Composable () -> Unit): @Composable () -> Unit = {
        AppTheme(themeMode = ThemeMode.DARK) { content() }
    }

    @Test
    fun anOverlineLabelIsUpperCased() {
        // The design calls for small-caps headings; the component does the casing
        // so call sites pass ordinary text.
        runComposeUiTest {
            setContent(themed { OverlineRow(label = "Songbooks") })

            onNodeWithText("SONGBOOKS").assertExists()
        }
    }

    @Test
    fun anOverlineRowShowsItsTrailingTextWhenGiven() = runComposeUiTest {
        setContent(themed { OverlineRow(label = "Songs", trailing = "240") })

        onNodeWithText("240").assertExists()
    }

    @Test
    fun anOverlineRowWithNoTrailingTextShowsOnlyTheLabel() = runComposeUiTest {
        setContent(themed { OverlineRow(label = "Songs") })

        onNodeWithText("SONGS").assertExists()
        onNodeWithText("240").assertDoesNotExist()
    }

    @Test
    fun theComponentsRenderInBothThemes() = runComposeUiTest {
        // They read every colour from LocalAppColors, so a token missing from one
        // palette shows up here rather than on a phone.
        for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            setContent {
                AppTheme(themeMode = mode) {
                    OverlineRow(label = "Songs", trailing = "1")
                }
            }
            onNodeWithText("SONGS").assertExists()
        }
    }

    @Test
    fun anOverlineLabelAlreadyUpperCasedIsUnchanged() = runComposeUiTest {
        setContent(themed { OverlineRow(label = "API") })

        onNodeWithText("API").assertExists()
    }

    @Test
    fun anOverlineTrailingCountOfZeroStillRenders() = runComposeUiTest {
        setContent(themed { OverlineRow(label = "Songs", trailing = "0") })

        onNodeWithText("0").assertExists()
    }
}
