package com.church.presenter.churchpresentermobile.util

/**
 * All Firebase Remote Config parameter keys.
 * Use these constants everywhere instead of raw strings so typos are caught at compile time.
 */
object RemoteConfigKeys {
    const val MAINTENANCE_MODE              = "maintenance_mode"
    const val MIN_APP_VERSION               = "min_app_version"
    const val ANNOUNCEMENT_BANNER           = "announcement_banner"
    const val FEATURE_BIBLE_ENABLED         = "feature_bible_enabled"
    const val FEATURE_SONGS_ENABLED         = "feature_songs_enabled"
    const val FEATURE_PICTURES_ENABLED      = "feature_pictures_enabled"
    const val FEATURE_PRESENTATION_ENABLED  = "feature_presentation_enabled"
    /** When true the app displays pre-built demo content without making any API calls. */
    const val IS_DEMO_MODE                  = "is_demo_mode"
}

/**
 * Default values for every Remote Config key.
 * Used in [RemoteConfig.init] so the app works before the first successful fetch.
 */
object RemoteConfigDefaults {
    const val MAINTENANCE_MODE              = false
    const val MIN_APP_VERSION               = 1L
    const val ANNOUNCEMENT_BANNER           = ""
    const val FEATURE_BIBLE_ENABLED         = true
    const val FEATURE_SONGS_ENABLED         = true
    const val FEATURE_PICTURES_ENABLED      = true
    const val FEATURE_PRESENTATION_ENABLED  = true
    /** Demo mode is off by default. */
    const val IS_DEMO_MODE                  = false

    /** Minimum fetch interval for production builds (seconds). */
    const val FETCH_INTERVAL_PRODUCTION = 3600L
    /** Minimum fetch interval for debug / development builds (seconds). */
    const val FETCH_INTERVAL_DEBUG      = 0L
}

