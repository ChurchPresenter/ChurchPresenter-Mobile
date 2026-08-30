package com.church.presenter.churchpresentermobile.model

import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "ChordsPreference"

/**
 * Process-wide holder for "show chords on this phone".
 *
 * Follows [AppModeHolder]: a singleton exposing a [StateFlow] that both
 * Composables and non-Compose collaborators can read. It has to be shared
 * because the switch and the words are on different screens — the toggle sits in
 * the Look sheet on the Present tab, and the chords are read in the Songs tab —
 * so a value held by either screen's ViewModel would not reach the other.
 *
 * Deliberately separate from [SlideTheme]: the theme is serialised into every
 * slide and travels to the browser screen and any attached display, and chords
 * belong to whoever is playing rather than to the congregation.
 */
object ChordsPreference {
    private val _showChords = MutableStateFlow(false)

    /** Whether the words are drawn with their chords. */
    val showChords: StateFlow<Boolean> = _showChords.asStateFlow()

    /** Seeds the holder from persisted settings. Call once, early in App. */
    fun init(settings: AppSettings) {
        _showChords.value = settings.showChords
        Logger.d(TAG, "init — showChords=${_showChords.value}")
    }

    /** Persists and publishes a new value. */
    fun set(settings: AppSettings, show: Boolean) {
        settings.showChords = show
        _showChords.value = show
        Logger.d(TAG, "set — showChords=$show")
    }

    /** Test hook — resets the singleton so tests don't leak state into each other. */
    fun resetForTest() {
        _showChords.value = false
    }
}
