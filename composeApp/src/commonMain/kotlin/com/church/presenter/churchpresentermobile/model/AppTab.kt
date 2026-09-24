package com.church.presenter.churchpresentermobile.model

/**
 * Represents the top-level navigation tabs in the app.
 *
 * Not every tab exists in every [AppMode] — use [AppTab.forMode] rather than
 * [AppTab.entries] anywhere the tab strip is built.
 */
enum class AppTab {
    /** Standalone-only: the live controller that drives the phone's own output. */
    PRESENT,
    SONGS,
    BIBLE,
    MEDIA,
    PRESENTATION,
    /** Standalone-only: the on-device content library. */
    LIBRARY,
    /** Calendar-mode only: the planner as the whole app, rather than a More entry. */
    CALENDAR,
    /**
     * Calendar-mode only: Contact as a tab of its own. It was More's only entry there, and a
     * launcher with one tile is a detour rather than a menu.
     */
    CONTACT,
    MORE;

    companion object {
        /**
         * The tabs shown in [mode], in strip order.
         *
         * Remote keeps exactly the tabs it has always had. Standalone drops the
         * ones that only mean something with a desktop attached (Media casting
         * and the desktop's presentation decks) and adds the local controller and
         * library. More survives in both, but with different contents — see
         * [MoreDestination.forMode]. Calendar mode is the planner and nothing
         * else: every other tab either drives a desktop or projects from here.
         */
        fun forMode(mode: AppMode): List<AppTab> = when (mode) {
            AppMode.REMOTE -> listOf(SONGS, BIBLE, MEDIA, PRESENTATION, MORE)
            AppMode.STANDALONE -> listOf(PRESENT, SONGS, BIBLE, LIBRARY, MORE)
            AppMode.CALENDAR -> listOf(CALENDAR, CONTACT)
        }
    }
}

/** Secondary destinations reached from the [AppTab.MORE] launcher. */
enum class MoreDestination {
    /** Planned services on a month grid, each with a run of show. Works with or without a desktop. */
    CALENDAR,
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
         * Standalone keeps Photos and Web, and both are different screens from
         * the remote ones: the remote pair asks a desktop to browse its picture
         * folders and open a page, the local pair picks from this device and puts
         * the page on this device's own outputs.
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
         */
        fun forMode(mode: AppMode): List<MoreDestination> = when (mode) {
            // CONTACT is in both: it posts to a public endpoint on the
            // internet, so it needs no desktop, and a user who hits a problem in
            // standalone is exactly the one with something to report.
            AppMode.REMOTE -> listOf(CALENDAR, PICTURES, QA, DICTIONARY, ANNOUNCEMENTS, WEB, CONTACT)
            AppMode.STANDALONE -> listOf(CALENDAR, PICTURES, ANNOUNCEMENTS, WEB, REPORT, CONTACT)
            // Calendar mode has no More tab — Calendar and Contact are both tabs —
            // so this list only settles a Contact left open from another mode.
            AppMode.CALENDAR -> listOf(CONTACT)
        }
    }
}
