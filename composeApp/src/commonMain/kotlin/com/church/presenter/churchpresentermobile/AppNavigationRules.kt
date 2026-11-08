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
 * The tab the app opens on: one a shortcut asked for, if this mode has it, or
 * else the mode's first.
 *
 * It was always Songs, which is first in remote but not in standalone — and
 * opening on a page that is not the strip's first left the header and strip on
 * one tab with another tab's content under them.
 */
internal fun startTab(requested: AppTab?, tabs: List<AppTab>): AppTab =
    requested?.takeIf { it in tabs } ?: tabs.first()

/**
 * The tab to show, given what was selected and what the strip now holds.
 *
 * `selectedTab` is remembered across a mode switch, so it can name a tab the
 * new strip does not have.
 */
internal fun settledTab(selected: AppTab, tabs: List<AppTab>): AppTab =
    if (selected in tabs) selected else tabs.first()

/**
 * The More destination to keep open after a mode switch, or after the window
 * changes shape.
 *
 * Standalone cannot fill the desktop-backed screens, so one left open would
 * strand the operator somewhere the launcher no longer offers a way back to.
 * The canvas likewise closes when the window narrows past [tablet] — an iPad
 * entering Split View — since it has no layout for the narrower window.
 */
internal fun settledMoreDestination(
    current: MoreDestination?,
    mode: AppMode,
    tablet: Boolean = false,
): MoreDestination? = current?.takeIf { it in MoreDestination.forMode(mode, tablet) }

/** The page in the pager that shows [tab], or the first page when it has none. */
internal fun pageForTab(tab: AppTab, tabs: List<AppTab>): Int =
    tabs.indexOf(tab).coerceAtLeast(0)

/** The tab a settled pager page names, falling back to the first. */
internal fun tabForPage(page: Int, tabs: List<AppTab>): AppTab =
    tabs.getOrNull(page) ?: tabs.first()

/** The name a tab reports to the "Pages and screens" report. */
internal fun tabScreenName(tab: AppTab): String = when (tab) {
    AppTab.LIBRARY -> AnalyticsScreen.LIBRARY
    AppTab.ANNOUNCEMENTS -> AnalyticsScreen.ANNOUNCEMENTS
    AppTab.SONGS -> AnalyticsScreen.SONGS
    AppTab.BIBLE -> AnalyticsScreen.BIBLE_BOOKS
    AppTab.MEDIA -> AnalyticsScreen.MEDIA
    AppTab.PRESENTATION -> AnalyticsScreen.PRESENTATIONS
    AppTab.CALENDAR -> AnalyticsScreen.CALENDAR
    AppTab.MORE -> AnalyticsScreen.MORE
}

/**
 * The name a More sub-screen reports, or null for the launcher grid, which is
 * already covered by the More tab.
 */
internal fun moreScreenName(destination: MoreDestination?): String? = when (destination) {
    MoreDestination.CALENDAR -> AnalyticsScreen.CALENDAR
    MoreDestination.CANVAS -> AnalyticsScreen.CANVAS
    MoreDestination.PICTURES -> AnalyticsScreen.PICTURES
    MoreDestination.QA -> AnalyticsScreen.QA_ADMIN
    MoreDestination.DICTIONARY -> AnalyticsScreen.DICTIONARY
    MoreDestination.ANNOUNCEMENTS -> AnalyticsScreen.ANNOUNCEMENTS
    MoreDestination.WEB -> AnalyticsScreen.WEB
    MoreDestination.REPORT -> AnalyticsScreen.CCLI_REPORT
    MoreDestination.CONTACT -> AnalyticsScreen.CONTACT
    null -> null
}

/** Which full-screen step of startup, if any, is covering the app. */
internal enum class StartupGate { SPLASH, MODE_PICKER, STATUS, CONNECT_SETUP }

/** Which overlay, if any, is drawn over the tabs — the topmost when several are. */
internal enum class AppOverlay { CONTACT, BIBLES, SETTINGS, CANVAS, SCHEDULE_DRAWER }

/** Which Library editor, if any, has replaced the Library list. */
internal enum class LibraryEditor { SONG, ANNOUNCEMENT }

/** Everything that decides which screen the operator is looking at. */
internal data class ScreenLocation(
    val gate: StartupGate? = null,
    val overlay: AppOverlay? = null,
    val tab: AppTab,
    val moreDestination: MoreDestination? = null,
    val inSongDetail: Boolean = false,
    val bibleHasBook: Boolean = false,
    val bibleHasChapter: Boolean = false,
    val libraryEditor: LibraryEditor? = null,
)

/**
 * The one name the "Pages and screens" report should show for [where], or null
 * where the screen on show reports its own pages.
 *
 * Settings and the Bibles manager return null: each has pages of its own that
 * only it knows about, and logs them itself. Everything else is decided here, so
 * that leaving an overlay re-reports the screen underneath it — which scattered
 * per-screen effects never did, leaving the time spent there credited to
 * whichever overlay was opened last.
 */
internal fun currentScreenName(where: ScreenLocation): String? = when {
    where.gate != null -> when (where.gate) {
        StartupGate.SPLASH -> AnalyticsScreen.SPLASH
        StartupGate.MODE_PICKER -> AnalyticsScreen.MODE_PICKER
        StartupGate.STATUS -> AnalyticsScreen.STATUS
        StartupGate.CONNECT_SETUP -> AnalyticsScreen.CONNECT_SETUP
    }
    where.overlay != null -> when (where.overlay) {
        AppOverlay.CONTACT -> AnalyticsScreen.CONTACT
        AppOverlay.BIBLES, AppOverlay.SETTINGS -> null
        AppOverlay.CANVAS -> AnalyticsScreen.CANVAS
        AppOverlay.SCHEDULE_DRAWER -> AnalyticsScreen.SCHEDULE
    }
    else -> tabContentScreenName(where)
}

/** The screen inside the tab strip: a tab, or the detail it has drilled into. */
private fun tabContentScreenName(where: ScreenLocation): String = when (where.tab) {
    AppTab.SONGS -> if (where.inSongDetail) AnalyticsScreen.SONG_DETAIL else AnalyticsScreen.SONGS
    AppTab.BIBLE -> bibleScreenName(where.bibleHasBook, where.bibleHasChapter)
    AppTab.MORE -> moreScreenName(where.moreDestination) ?: AnalyticsScreen.MORE
    AppTab.LIBRARY -> when (where.libraryEditor) {
        LibraryEditor.SONG -> AnalyticsScreen.SONG_EDITOR
        LibraryEditor.ANNOUNCEMENT -> AnalyticsScreen.ANNOUNCEMENT_EDITOR
        null -> AnalyticsScreen.LIBRARY
    }
    else -> tabScreenName(where.tab)
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

/**
 * Whether [tab] hangs its own header over its panes, leaving the shell nothing
 * to draw across the top.
 *
 * Only true in a two-pane layout, and only for the tabs converted to it: a split
 * tab's two panes want two different headers — the tab's own on the list, the
 * open item's on the detail — and one bar spanning both can be neither. Tabs
 * still to be split keep the single bar, so this list grows one tab at a time
 * rather than the shell having to know which of the two shapes each tab is in.
 */
internal fun tabDrawsOwnHeader(tab: AppTab, twoPane: Boolean): Boolean =
    twoPane && tab in TABS_WITH_PANE_HEADERS

/**
 * The tabs that lay themselves out in panes.
 *
 * Everything but [AppTab.PRESENTATION], which the tablet design does not cover —
 * it keeps the single bar and the one-screen layout until it does — and
 * [AppTab.ANNOUNCEMENTS], a single list that the shell's one header serves.
 */
private val TABS_WITH_PANE_HEADERS = setOf(
    AppTab.SONGS,
    AppTab.BIBLE,
    AppTab.MEDIA,
    AppTab.MORE,
    AppTab.LIBRARY,
    AppTab.CALENDAR,
)

/**
 * Whether [tabs] earn a tab strip — the bottom bar on a phone, the rail on a tablet.
 *
 * Calendar mode is one tab; a bar holding only the screen already on show takes a strip of the
 * screen and offers nowhere to go.
 */
internal fun showsTabBar(tabs: List<AppTab>): Boolean = tabs.size > 1

/**
 * Where Settings' Contact us leads: More's Contact where there is a More tab, otherwise the
 * contact form over the app (true).
 */
internal fun contactOpensOverlay(tabs: List<AppTab>): Boolean = AppTab.MORE !in tabs
