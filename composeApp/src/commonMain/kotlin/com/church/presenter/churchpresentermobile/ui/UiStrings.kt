package com.church.presenter.churchpresentermobile.ui

/**
 * Central repository for all user-facing strings used in the UI layer.
 * Static strings are `const val`; strings that embed runtime data are plain `val` functions.
 */
object UiStrings {

    // ── Song Detail — FAB speed-dial labels ──────────────────────────────
    const val ADD_TO_SCHEDULE       = "Add to Schedule"
    const val SCHEDULE_REQUEST_SENT = "Request Sent ✓"
    const val PROJECT_TO_SCREEN     = "Project to Screen"
    const val STOP_PROJECTING       = "Stop Projecting"
    const val FAB_ACTIONS           = "Actions"
    const val FAB_CLOSE_ACTIONS     = "Close actions"
    const val PROJECTING_BADGE      = "▶  Projecting"
    const val NO_LYRICS_AVAILABLE   = "No lyrics available"

    // ── Toast / snackbar messages ─────────────────────────────────────────
    const val TOAST_SONG_LIVE           = "Song is now live on screen"
    const val TOAST_REQUEST_FAILED      = "Request failed"
    const val TOAST_NO_SONG_SELECTED    = "No song selected"

    fun toastSongAddedToSchedule(title: String) = "\"$title\" added to schedule"
    fun toastFailedToProject(reason: String)     = "Failed to project song: $reason"
    fun toastFailedToAddSchedule(reason: String) = "Failed to add to schedule: $reason"
}

