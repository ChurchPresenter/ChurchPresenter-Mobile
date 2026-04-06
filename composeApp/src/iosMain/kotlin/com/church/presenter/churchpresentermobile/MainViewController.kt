package com.church.presenter.churchpresentermobile

import androidx.compose.ui.window.ComposeUIViewController
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.AppTab

fun MainViewController() = ComposeUIViewController { App() }

/**
 * Called from Swift's onOpenURL handler to process a churchpresenter:// deep link.
 * Writes parsed settings to storage; the running App() picks them up via
 * DeepLinkHandler.appliedCount.
 */
fun handleDeepLinkUrl(url: String) {
    DeepLinkHandler.handle(url, AppSettings())
}

/**
 * Called from Swift's application(_:performActionForShortcutItem:completionHandler:)
 * when the user activates a home-screen quick action.
 *
 * @param tab  "songs" | "bible"
 */
fun navigateToTab(tab: String) {
    when (tab) {
        "songs" -> TabNavigationHandler.navigateTo(AppTab.SONGS)
        "bible" -> TabNavigationHandler.navigateTo(AppTab.BIBLE)
    }
}

