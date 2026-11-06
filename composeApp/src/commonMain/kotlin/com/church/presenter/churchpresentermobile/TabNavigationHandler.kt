package com.church.presenter.churchpresentermobile

import com.church.presenter.churchpresentermobile.model.AppTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cross-platform singleton that lets native platform code (Android shortcut
 * intents, iOS Quick-Action callbacks) request a tab switch.
 *
 * Usage:
 *   - Platform layer calls [navigateTo] when a shortcut is activated.
 *   - [App] composable observes [requestedTab] and calls [consume] after handling.
 */
object TabNavigationHandler {

    private val _requestedTab = MutableStateFlow<AppTab?>(null)

    /** Emits the tab to navigate to, or null when nothing is pending. */
    val requestedTab: StateFlow<AppTab?> = _requestedTab.asStateFlow()

    /** Request navigation to [tab]. Safe to call from any thread. */
    fun navigateTo(tab: AppTab) {
        _requestedTab.value = tab
    }

    /** Mark the pending request as handled so it is not applied again. */
    fun consume() {
        _requestedTab.value = null
    }
}

