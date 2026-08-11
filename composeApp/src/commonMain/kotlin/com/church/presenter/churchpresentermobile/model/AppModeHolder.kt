package com.church.presenter.churchpresentermobile.model

import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "AppModeHolder"

/**
 * Process-wide holder for the current [AppMode].
 *
 * Follows the same shape as [com.church.presenter.churchpresentermobile.DeepLinkHandler]
 * and [com.church.presenter.churchpresentermobile.TabNavigationHandler]: a singleton
 * exposing a [StateFlow] that both Composables and non-Compose collaborators can read.
 *
 * The persisted value lives in [AppSettings.appMode]; this holder is the
 * reactive view of it, so a mode change is observed everywhere at once without
 * anything having to re-read settings.
 */
object AppModeHolder {
    private val _mode = MutableStateFlow(AppMode.REMOTE)

    /** The mode the app is running in right now. */
    val mode: StateFlow<AppMode> = _mode.asStateFlow()

    /** Seeds the holder from persisted settings. Call once, early in [com.church.presenter.churchpresentermobile.App]. */
    fun init(settings: AppSettings) {
        _mode.value = settings.appMode
        Logger.d(TAG, "init — mode=${_mode.value}")
    }

    /** Persists and publishes a new mode. */
    fun set(settings: AppSettings, mode: AppMode) {
        settings.appMode = mode
        // Read back rather than trusting the argument: AppSettings coerces to
        // REMOTE on platforms that cannot present, so the holder must agree.
        _mode.value = settings.appMode
        Logger.d(TAG, "set — requested=$mode effective=${_mode.value}")
    }

    /** Test hook — resets the singleton so tests don't leak state into each other. */
    fun resetForTest() {
        _mode.value = AppMode.REMOTE
    }
}
