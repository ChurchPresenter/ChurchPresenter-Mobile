package com.church.presenter.churchpresentermobile.screenshot

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppTab
import com.church.presenter.churchpresentermobile.ui.BottomTabBar
import com.church.presenter.churchpresentermobile.ui.ScreenHeader
import kotlin.test.Test

/**
 * The app's chrome: the strip along the bottom and the header along the top.
 *
 * Both are on screen for the whole session, and both change shape with what the
 * app is doing — a different set of tabs per [AppMode], a header that grows a
 * back arrow or a settings gear depending on where the operator is. Those are
 * the states captured here.
 */
class ChromeScreenshotTest {

    @Test
    fun bottomBarRemoteMode() = screenshot("bottom-tab-bar__remote") {
        BottomTabBar(selectedTab = AppTab.SONGS, onTabSelected = {})
    }

    @Test
    fun bottomBarRemoteModeLastTabSelected() = screenshot("bottom-tab-bar__remote-more-selected") {
        BottomTabBar(selectedTab = AppTab.MORE, onTabSelected = {})
    }

    @Test
    fun bottomBarStandaloneMode() = screenshot("bottom-tab-bar__standalone") {
        // A different tab set entirely — the phone drives its own output, so
        // Present and Library replace Media and the desktop's decks.
        BottomTabBar(
            selectedTab = AppTab.PRESENT,
            onTabSelected = {},
            tabs = AppTab.forMode(AppMode.STANDALONE),
        )
    }

    @Test
    fun headerTitleOnly() = screenshot("screen-header__title-only") {
        ScreenHeader(title = "Songs")
    }

    @Test
    fun headerWithSubtitle() = screenshot("screen-header__with-subtitle") {
        ScreenHeader(title = "Songs", subtitle = "412 in your library")
    }

    @Test
    fun headerWithBack() = screenshot("screen-header__with-back") {
        ScreenHeader(title = "Amazing Grace", subtitle = "Hymns", onBack = {})
    }

    @Test
    fun headerWithMenuAndSettings() = screenshot("screen-header__with-menu-and-settings") {
        ScreenHeader(title = "Songs", onMenu = {}, onSettings = {})
    }

    @Test
    fun headerCompact() = screenshot("screen-header__compact") {
        // largeTitle = false is what a pushed detail screen uses; the title sits
        // on one line with the controls rather than below them.
        ScreenHeader(title = "Settings", largeTitle = false, onBack = {})
    }
}
