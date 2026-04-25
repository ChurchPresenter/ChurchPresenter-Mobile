package com.church.presenter.churchpresentermobile.util

/**
 * Multiplatform Firebase Analytics abstraction.
 *
 * Android  → FirebaseAnalytics
 * iOS      → FirebaseAnalytics (via [IosAnalyticsBridge] set from Swift)
 * JS/WASM  → console no-op stubs
 *
 * Event name rules (Firebase limits):
 *  - max 40 characters, only letters/digits/underscores, must start with a letter
 *
 * Param value rules:
 *  - max 100 characters (enforced automatically by [logEvent])
 */
expect object Analytics {

    /** Call once at startup (after FirebaseApp.configure on iOS / Application.onCreate on Android). */
    fun init()

    /**
     * Log a custom event to Firebase Analytics.
     *
     * @param name   Event name — max 40 chars, alphanumeric + underscore, starts with letter.
     * @param params Optional key-value pairs; values are automatically truncated to 100 chars.
     */
    fun logEvent(name: String, params: Map<String, String> = emptyMap())
}

// ── Convenience event constants ───────────────────────────────────────────────

object AnalyticsEvent {
    // Navigation
    const val TAB_SELECTED               = "tab_selected"
    const val SCHEDULE_DRAWER_OPENED     = "schedule_drawer_opened"

    // Songs
    const val SONG_OPENED                = "song_opened"
    const val SONG_PROJECTED             = "song_projected"
    const val SONG_DISPLAY_CLEARED       = "song_display_cleared"
    const val SONG_VERSE_SELECTED        = "song_verse_selected"
    const val SONG_ADDED_TO_SCHEDULE     = "song_added_to_schedule"

    // Bible
    const val BIBLE_BOOK_SELECTED        = "bible_book_selected"
    const val BIBLE_CHAPTER_SELECTED     = "bible_chapter_selected"
    const val BIBLE_PROJECTED            = "bible_projected"
    const val BIBLE_DISPLAY_CLEARED      = "bible_display_cleared"
    const val BIBLE_ADDED_TO_SCHEDULE    = "bible_added_to_schedule"

    // Pictures
    const val PICTURE_FOLDER_OPENED      = "picture_folder_opened"
    const val PICTURE_SELECTED           = "picture_selected"
    const val PICTURE_ADDED_TO_SCHEDULE  = "picture_added_to_schedule"
    const val PHOTO_UPLOADED             = "photo_uploaded"

    // Presentations
    const val SLIDE_SELECTED             = "slide_selected"
    const val PRESENTATION_ADDED         = "presentation_added_to_schedule"
    const val PRESENTATION_UPLOADED      = "presentation_uploaded"

    // Settings
    const val SETTINGS_SAVED             = "settings_saved"
}

// ── Convenience param keys ────────────────────────────────────────────────────

object AnalyticsParam {
    const val TAB_NAME    = "tab_name"
    const val VERSE_INDEX = "verse_index"
    const val SLIDE_INDEX = "slide_index"
}

