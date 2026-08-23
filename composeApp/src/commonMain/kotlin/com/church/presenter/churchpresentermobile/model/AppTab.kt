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
         * Standalone keeps only Photos, and even that is a different screen: the
         * remote one browses the desktop's picture folders, the local one picks
         * from this device and projects from here.
         *
         * The rest read from or write to a desktop. Q&A, the dictionary and the
         * web viewer need its data; announcements look local but are not — that
         * screen adds to the *desktop's* schedule or shows on the *desktop's*
         * screen, both swallowed by StandaloneEngine.handleRemoteAction while
         * reporting success, and its timer and countdown types have no local
         * renderer at all. Writing and projecting an announcement on the device
         * is what the Library tab is for.
         */
        fun forMode(mode: AppMode): List<MoreDestination> = when (mode) {
            AppMode.REMOTE -> listOf(PICTURES, QA, DICTIONARY, ANNOUNCEMENTS, WEB)
            AppMode.STANDALONE -> listOf(PICTURES)
        }
    }
}
