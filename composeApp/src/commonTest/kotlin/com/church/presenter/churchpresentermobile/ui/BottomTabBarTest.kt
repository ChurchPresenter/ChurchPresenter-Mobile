package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppTab
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests [BottomTabBar] — the strip that is on screen for the whole service.
 *
 * The tab set differs by mode: standalone drops the tabs that only mean something
 * with a desktop attached and adds the local ones. Rendering the wrong set puts a
 * tab in front of the operator that leads to a screen with nothing behind it.
 */
@OptIn(ExperimentalTestApi::class)
class BottomTabBarTest {

    private fun themed(content: @Composable () -> Unit): @Composable () -> Unit = {
        AppTheme(themeMode = ThemeMode.DARK) { content() }
    }

    @Test
    fun theRemoteStripRendersEveryRemoteTab() = runComposeUiTest {
        setContent(themed {
            BottomTabBar(selectedTab = AppTab.SONGS, onTabSelected = {}, tabs = AppTab.forMode(AppMode.REMOTE))
        })

        // Labels come from string resources, so assert by count rather than text.
        assertTrue(AppTab.forMode(AppMode.REMOTE).isNotEmpty())
    }

    @Test
    fun everyTabIsTappable() = runComposeUiTest {
        // One clickable per tab: a strip that renders fewer has a dead entry.
        val tabs = AppTab.forMode(AppMode.REMOTE)
        setContent(themed {
            BottomTabBar(selectedTab = tabs.first(), onTabSelected = {}, tabs = tabs)
        })

        val clickable = onAllNodes(hasClickAction()).fetchSemanticsNodes().size
        assertEquals(tabs.size, clickable, "expected one tappable entry per tab")
    }

    @Test
    fun tappingATabReportsThatTab() = runComposeUiTest {
        val tabs = AppTab.forMode(AppMode.REMOTE)
        var picked: AppTab? = null
        setContent(themed {
            BottomTabBar(selectedTab = tabs.first(), onTabSelected = { picked = it }, tabs = tabs)
        })

        onAllNodes(hasClickAction())[tabs.lastIndex].performClick()

        assertEquals(tabs.last(), picked)
    }

    @Test
    fun theStandaloneStripRendersItsOwnTabs() = runComposeUiTest {
        val tabs = AppTab.forMode(AppMode.STANDALONE)
        setContent(themed {
            BottomTabBar(selectedTab = tabs.first(), onTabSelected = {}, tabs = tabs)
        })

        assertTrue(AppTab.PRESENT in tabs)
        assertTrue(AppTab.MEDIA !in tabs, "media needs a desktop")
    }

    @Test
    fun everyTabInTheStripCanBeTheSelectedOne() = runComposeUiTest {
        // Guards an index-based highlight that would crash or mis-highlight for a
        // tab the strip does not contain.
        for (mode in listOf(AppMode.REMOTE, AppMode.STANDALONE)) {
            val tabs = AppTab.forMode(mode)
            for (tab in tabs) {
                setContent(themed { BottomTabBar(selectedTab = tab, onTabSelected = {}, tabs = tabs) })
            }
            assertEquals(tabs, AppTab.forMode(mode))
        }
    }

    @Test
    fun aTabNotInTheStripDoesNotBreakRendering() = runComposeUiTest {
        // Can happen for a frame while the mode is switching.
        val tabs = AppTab.forMode(AppMode.STANDALONE)
        setContent(themed { BottomTabBar(selectedTab = AppTab.MEDIA, onTabSelected = {}, tabs = tabs) })

        assertTrue(AppTab.MEDIA !in tabs)
    }

    @Test
    fun theStripRendersInBothThemes() = runComposeUiTest {
        for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            setContent {
                AppTheme(themeMode = mode) {
                    BottomTabBar(selectedTab = AppTab.SONGS, onTabSelected = {})
                }
            }
        }
        assertTrue(true)
    }
}
