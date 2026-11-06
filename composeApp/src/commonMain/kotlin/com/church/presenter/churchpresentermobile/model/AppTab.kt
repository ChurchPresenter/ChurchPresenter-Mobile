package com.church.presenter.churchpresentermobile.model

/**
 * Represents the top-level navigation tabs in the app.
 *
 * Not every tab exists in every [AppMode] — use [AppTab.forMode] rather than
 * [AppTab.entries] anywhere the tab strip is built.
 */
enum class AppTab {
    SONGS,
    BIBLE,
    MEDIA,
    PRESENTATION,
    /** Standalone-only: the songs and notices on this device, and copying from a computer. */
    LIBRARY,
    /** Standalone-only: puts a notice written in the Library on this device's own outputs. */
    ANNOUNCEMENTS,
    /** Calendar-mode only: the planner as the whole app, rather than a More entry. */
    CALENDAR,
    MORE;

    companion object {
        /**
         * The tabs shown in [mode], in strip order.
         *
         * Remote keeps exactly the tabs it has always had. Standalone drops the
         * ones that only mean something with a desktop attached (Media casting
         * and the desktop's presentation decks) and adds this device's Library and
         * Announcements. There is no Present tab — the live controls ride along
         * every tab in the live bar, and what the screens look like is set once,
         * in Settings. The running order is the schedule drawer, as in remote.
         * More survives in both, with different contents — see
         * [MoreDestination.forMode]. Calendar mode is the planner and nothing
         * else: every other tab either drives a desktop or projects from here.
         * With one tab there is no strip at all (see [showsTabBar]); Contact is
         * reached from Settings, as it is in every mode.
         */
        fun forMode(mode: AppMode): List<AppTab> = when (mode) {
            AppMode.REMOTE -> listOf(SONGS, BIBLE, MEDIA, PRESENTATION, MORE)
            AppMode.STANDALONE -> listOf(BIBLE, SONGS, LIBRARY, ANNOUNCEMENTS, MORE)
            AppMode.CALENDAR -> listOf(CALENDAR)
        }
    }
}

/** Secondary destinations reached from the [AppTab.MORE] launcher. */
enum class MoreDestination {
    /** Planned services on a month grid, each with a run of show. Works with or without a desktop. */
    CALENDAR,
    /** Tablet-only: a whiteboard of pages to sketch on. Needs no desktop, so both modes have it. */
    CANVAS,
    PICTURES,
    QA,
    DICTIONARY,
    ANNOUNCEMENTS,
    WEB,
    /** Standalone-only: the CCLI report of what this device has projected. */
    REPORT,
    CONTACT;

    companion object {
        /**
         * The "More" entries available in [mode].
         *
         * Standalone keeps Photos, a different screen from the remote one: the
         * remote one asks a desktop to browse its picture folders, the local one
         * picks from this device.
         *
         * Web is remote-only. It hands a page to the desktop to open; standalone
         * once had a screen that put a page on this device's own outputs, but it
         * did not work reliably enough to keep.
         *
         * Q&A and the dictionary need the desktop's data.
         *
         * Announcements here are the remote composer, which adds to the *desktop's*
         * schedule and drives the *desktop's* screen. Standalone has them as a tab
         * of its own ([AppTab.ANNOUNCEMENTS]) instead.
         *
         * The CCLI report is standalone-only for the mirror-image reason: in
         * remote mode the desktop is what projects, and it keeps its own report.
         *
         * The canvas is listed only when [tablet] — the two-pane layout, which
         * an iPad or Android tablet reaches and a phone never does. Its tool rail,
         * dock and side panels are drawn for a tablet's board and have no phone
         * arrangement.
         */
        fun forMode(mode: AppMode, tablet: Boolean = false): List<MoreDestination> {
            val all = when (mode) {
                // CONTACT is in both: it posts to a public endpoint on the
                // internet, so it needs no desktop, and a user who hits a problem in
                // standalone is exactly the one with something to report.
                AppMode.REMOTE -> listOf(CALENDAR, CANVAS, PICTURES, QA, DICTIONARY, ANNOUNCEMENTS, WEB, CONTACT)
                AppMode.STANDALONE -> listOf(CALENDAR, CANVAS, PICTURES, REPORT, CONTACT)
                // Calendar mode has no More tab, so this list only settles a Contact
                // left open from another mode.
                AppMode.CALENDAR -> listOf(CONTACT)
            }
            return if (tablet) all else all - CANVAS
        }
    }
}
