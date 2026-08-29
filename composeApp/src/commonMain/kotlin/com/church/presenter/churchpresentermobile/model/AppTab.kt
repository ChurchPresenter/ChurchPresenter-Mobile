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
    MORE;

    companion object {
        /**
         * The tabs shown in [mode], in strip order.
         *
         * Remote keeps exactly the tabs it has always had. Standalone drops the
         * ones that only mean something with a desktop attached (Media casting
         * and the desktop's presentation decks) and adds the local controller and
         * library. More survives in both, but with different contents — see
         * [MoreDestination.forMode].
         */
        fun forMode(mode: AppMode): List<AppTab> = when (mode) {
            AppMode.REMOTE -> listOf(SONGS, BIBLE, MEDIA, PRESENTATION, MORE)
            AppMode.STANDALONE -> listOf(PRESENT, SONGS, BIBLE, LIBRARY, MORE)
        }
    }
}

/** Secondary destinations reached from the [AppTab.MORE] launcher. */
enum class MoreDestination {
    PICTURES,
    QA,
    DICTIONARY,
    ANNOUNCEMENTS,
    WEB;

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
         */
        fun forMode(mode: AppMode): List<MoreDestination> = when (mode) {
            AppMode.REMOTE -> listOf(PICTURES, QA, DICTIONARY, ANNOUNCEMENTS, WEB)
            AppMode.STANDALONE -> listOf(PICTURES, ANNOUNCEMENTS, WEB)
        }
    }
}
