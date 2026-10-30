package com.church.presenter.churchpresentermobile.model

/**
 * Represents the top-level navigation tabs in the app.
 *
 * Not every tab exists in every [AppMode] — use [AppTab.forMode] rather than
 * [AppTab.entries] anywhere the tab strip is built.
 */
enum class AppTab {
    /** Standalone-only: this device's running order, what the service goes through. */
    SCHEDULE,
    SONGS,
    BIBLE,
    MEDIA,
    PRESENTATION,
    /** Calendar-mode only: the planner as the whole app, rather than a More entry. */
    CALENDAR,
    MORE;

    companion object {
        /**
         * The tabs shown in [mode], in strip order.
         *
         * Remote keeps exactly the tabs it has always had. Standalone drops the
         * ones that only mean something with a desktop attached (Media casting
         * and the desktop's presentation decks) and opens on its running order:
         * the schedule is what a service is run from, so it is a tab rather than
         * a drawer only one tab's swipe reached. There is no Present tab — the
         * live controls ride along every tab in the live bar, and what the
         * screens look like is set once, in Settings. More survives in both, but
         * with different contents (the Library lives there in standalone) — see
         * [MoreDestination.forMode]. Calendar mode is the planner and nothing
         * else: every other tab either drives a desktop or projects from here.
         * With one tab there is no strip at all (see [showsTabBar]); Contact is
         * reached from Settings, as it is in every mode.
         */
        fun forMode(mode: AppMode): List<AppTab> = when (mode) {
            AppMode.REMOTE -> listOf(SONGS, BIBLE, MEDIA, PRESENTATION, MORE)
            AppMode.STANDALONE -> listOf(SCHEDULE, BIBLE, SONGS, MORE)
            AppMode.CALENDAR -> listOf(CALENDAR)
        }
    }
}

/** Secondary destinations reached from the [AppTab.MORE] launcher. */
enum class MoreDestination {
    /** Standalone-only: the songs and notices written on this device, and copying from a computer. */
    LIBRARY,
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
         * The Library leads standalone's list. It was a tab, but writing a song or
         * copying a songbook is preparation, not something done mid-service, and
         * what it holds already reaches the Songs tab and Notices.
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
         * Announcements appear in both, but they are two different screens. The
         * remote one adds to the *desktop's* schedule and drives the *desktop's*
         * screen, and its timer and countdown types have no local renderer;
         * standalone gets Notices instead, which projects a notice written in the
         * Library onto this device's own outputs. The Library keeps notices; this
         * is where one goes live, so browsing the library cannot project by
         * accident.
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
                AppMode.STANDALONE -> listOf(LIBRARY, CALENDAR, CANVAS, PICTURES, ANNOUNCEMENTS, REPORT, CONTACT)
                // Calendar mode has no More tab, so this list only settles a Contact
                // left open from another mode.
                AppMode.CALENDAR -> listOf(CONTACT)
            }
            return if (tablet) all else all - CANVAS
        }
    }
}
