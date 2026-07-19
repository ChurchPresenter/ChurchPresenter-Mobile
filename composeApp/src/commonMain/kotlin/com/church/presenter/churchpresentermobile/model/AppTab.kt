package com.church.presenter.churchpresentermobile.model

/** Represents the top-level navigation tabs in the app. */
enum class AppTab {
    SONGS,
    BIBLE,
    PICTURES,
    PRESENTATION,
    MORE
}

/** Secondary destinations reached from the [AppTab.MORE] launcher. */
enum class MoreDestination {
    QA,
    DICTIONARY
}

