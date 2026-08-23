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
         * ones that only mean something with a desktop attached (Media casting,
         * the desktop's presentation decks, and More — see [MoreDestination.forMode],
         * every entry of which reads from or writes to a desktop) and adds the
         * local controller and library. Settings is a header gear on every tab,
         * so nothing is stranded by dropping More.
         */
        fun forMode(mode: AppMode): List<AppTab> = when (mode) {
            AppMode.REMOTE -> listOf(SONGS, BIBLE, MEDIA, PRESENTATION, MORE)
            AppMode.STANDALONE -> listOf(PRESENT, SONGS, BIBLE, LIBRARY)
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
         * Standalone has none. Q&A, the dictionary, pictures and the web viewer
         * all read from the desktop; announcements looked local but are not —
         * this screen composes an announcement and either adds it to the
         * *desktop's* schedule or shows it on the *desktop's* screen, and
         * StandaloneEngine.handleRemoteAction swallows both while reporting
         * success. The on-device equivalent is the Library tab, which projects an
         * announcement through SlideDeckBuilder.fromAnnouncement.
         */
        fun forMode(mode: AppMode): List<MoreDestination> = when (mode) {
            AppMode.REMOTE -> listOf(PICTURES, QA, DICTIONARY, ANNOUNCEMENTS, WEB)
            AppMode.STANDALONE -> emptyList()
        }
    }
}
