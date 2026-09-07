package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests [ScreenHeader] — the bar at the top of every screen.
 *
 * Its three actions are all optional, and each is meant to appear only when the
 * screen supplies a handler. A button that renders without one is a control the
 * operator can press to no effect.
 */
@OptIn(ExperimentalTestApi::class)
class ScreenHeaderTest {

    private fun themed(content: @Composable () -> Unit): @Composable () -> Unit = {
        AppTheme(themeMode = ThemeMode.DARK) { content() }
    }

    private fun clickables(test: androidx.compose.ui.test.ComposeUiTest): Int =
        test.onAllNodes(hasClickAction()).fetchSemanticsNodes().size

    @Test
    fun theTitleIsShown() = runComposeUiTest {
        setContent(themed { ScreenHeader(title = "Songs") })

        onNodeWithText("Songs").assertExists()
    }

    @Test
    fun aSubtitleIsShownWhenGiven() = runComposeUiTest {
        setContent(themed { ScreenHeader(title = "Songs", subtitle = "240 in Hymns") })

        onNodeWithText("Songs").assertExists()
        onNodeWithText("240 in Hymns").assertExists()
    }

    @Test
    fun aHeaderWithNoSubtitleShowsOnlyTheTitle() = runComposeUiTest {
        setContent(themed { ScreenHeader(title = "Songs") })

        onNodeWithText("240 in Hymns").assertDoesNotExist()
    }

    // Content descriptions come from string resources, so these assert by
    // presence and behaviour rather than by a label the test would have to
    // duplicate — and which translation would then break.

    @Test
    fun aTopLevelScreenOffersNoActions() = runComposeUiTest {
        setContent(themed { ScreenHeader(title = "Songs") })

        assertEquals(0, clickables(this), "a plain header should have no buttons")
    }

    @Test
    fun theBackButtonAppearsOnlyWhenTheScreenCanGoBack() = runComposeUiTest {
        var backs = 0
        setContent(themed { ScreenHeader(title = "Detail", onBack = { backs++ }) })

        assertEquals(1, clickables(this))
        onAllNodes(hasClickAction())[0].performClick()
        assertEquals(1, backs)
    }

    @Test
    fun theSettingsButtonAppearsOnlyWithAHandler() = runComposeUiTest {
        var opened = 0
        setContent(themed { ScreenHeader(title = "Songs", onSettings = { opened++ }) })

        assertEquals(1, clickables(this))
        onAllNodes(hasClickAction())[0].performClick()
        assertEquals(1, opened)
    }

    @Test
    fun eachHandlerAddsExactlyOneButton() = runComposeUiTest {
        setContent(themed { ScreenHeader(title = "Songs", onBack = {}, onSettings = {}) })

        assertEquals(2, clickables(this))
    }

    @Test
    fun everyTitleSizeAndThemeRenders() = runComposeUiTest {
        // largeTitle is the top-level look, the compact one is used on sheets;
        // both read their colours from the palette, so both are checked in both.
        for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            for (large in listOf(true, false)) {
                setContent { AppTheme(themeMode = mode) { ScreenHeader("Songs", largeTitle = large) } }
                onNodeWithText("Songs").assertExists()
            }
        }
    }
}
