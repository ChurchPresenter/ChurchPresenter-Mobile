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
         * and the desktop's presentation decks) and adds the local controller
         * and library.
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
         * Q&A, the dictionary, pictures and the web viewer all read from the
         * desktop, so standalone shows only what it can actually serve.
         */
        fun forMode(mode: AppMode): List<MoreDestination> = when (mode) {
            AppMode.REMOTE -> listOf(PICTURES, QA, DICTIONARY, ANNOUNCEMENTS, WEB)
            AppMode.STANDALONE -> listOf(ANNOUNCEMENTS)
        }
    }
}
