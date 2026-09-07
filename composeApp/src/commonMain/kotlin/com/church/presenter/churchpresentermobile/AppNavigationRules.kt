package com.church.presenter.churchpresentermobile

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppTab
import com.church.presenter.churchpresentermobile.model.MoreDestination
import com.church.presenter.churchpresentermobile.util.AnalyticsScreen

/**
 * The decisions `App()` makes about where the operator is, as plain functions.
 *
 * Each of these sits inside a composable that cannot be run in a test — it
 * builds a WebSocket, a server and a dozen ViewModels on the way past — while
 * being exactly the part that can be wrong. A tab left selected after a mode
 * switch strands the operator on a strip that no longer contains it; a screen
 * name that drifts silently splits one screen's figures across two rows of a
 * report nobody is watching.
 */

/**
 * The tab to show, given what was selected and what the strip now holds.
 *
 * `selectedTab` is remembered across a mode switch, so it can name a tab the
 * new strip does not have.
 */
internal fun settledTab(selected: AppTab, tabs: List<AppTab>): AppTab =
    if (selected in tabs) selected else tabs.first()

/**
 * The More destination to keep open after a mode switch.
 *
 * Standalone cannot fill the desktop-backed screens, so one left open would
 * strand the operator somewhere the launcher no longer offers a way back to.
 */
internal fun settledMoreDestination(current: MoreDestination?, mode: AppMode): MoreDestination? =
    current?.takeIf { it in MoreDestination.forMode(mode) }

/** The page in the pager that shows [tab], or the first page when it has none. */
internal fun pageForTab(tab: AppTab, tabs: List<AppTab>): Int =
    tabs.indexOf(tab).coerceAtLeast(0)

/** The tab a settled pager page names, falling back to the first. */
internal fun tabForPage(page: Int, tabs: List<AppTab>): AppTab =
    tabs.getOrNull(page) ?: tabs.first()

/** The name a tab reports to the "Pages and screens" report. */
internal fun tabScreenName(tab: AppTab): String = when (tab) {
    AppTab.PRESENT -> AnalyticsScreen.STANDALONE
    AppTab.LIBRARY -> AnalyticsScreen.LIBRARY
    AppTab.SONGS -> AnalyticsScreen.SONGS
    AppTab.BIBLE -> AnalyticsScreen.BIBLE_BOOKS
    AppTab.MEDIA -> AnalyticsScreen.MEDIA
    AppTab.PRESENTATION -> AnalyticsScreen.PRESENTATIONS
    AppTab.MORE -> AnalyticsScreen.MORE
}

/**
 * The name a More sub-screen reports, or null for one that reports nothing.
 *
 * Contact posts to a public endpoint rather than being a screen of the app's
 * own data, and the launcher grid itself is already covered by the More tab.
 */
internal fun moreScreenName(destination: MoreDestination?): String? = when (destination) {
    MoreDestination.PICTURES -> AnalyticsScreen.PICTURES
    MoreDestination.QA -> AnalyticsScreen.QA_ADMIN
    MoreDestination.DICTIONARY -> AnalyticsScreen.DICTIONARY
    MoreDestination.ANNOUNCEMENTS -> AnalyticsScreen.ANNOUNCEMENTS
    MoreDestination.WEB -> AnalyticsScreen.WEB
    MoreDestination.CONTACT -> null
    null -> null
}

/** How deep into the Bible the operator is, in the report's terms. */
internal fun bibleScreenName(hasBook: Boolean, hasChapter: Boolean): String = when {
    hasChapter -> AnalyticsScreen.BIBLE_VERSES
    hasBook -> AnalyticsScreen.BIBLE_CHAPTERS
    else -> AnalyticsScreen.BIBLE_BOOKS
}

/**
 * Whether the app starts in demo mode.
 *
 * A debug build never does, whatever the remote flag says: a developer working
 * against a live desktop must not have it swapped for canned content.
 */
internal fun startsInDemoMode(isDebug: Boolean, remoteFlag: Boolean): Boolean =
    !isDebug && remoteFlag

/** The device id sent with a live-map ping — blank when the user has opted out. */
internal fun pingDeviceId(telemetryEnabled: Boolean, deviceId: String): String =
    if (telemetryEnabled) deviceId else ""

/**
 * Whether a scanned connect link should open the settings screen.
 *
 * Not when the connect-setup screen is already showing: it handled the scan
 * itself, and opening settings over it would bury the confirmation.
 */
internal fun deepLinkOpensSettings(showingConnectSetup: Boolean): Boolean = !showingConnectSetup

/** Whether re-tapping [tab] should return the More tab to its launcher grid. */
internal fun tapReturnsToMoreLauncher(tab: AppTab, selected: AppTab): Boolean =
    tab == AppTab.MORE && selected == AppTab.MORE
