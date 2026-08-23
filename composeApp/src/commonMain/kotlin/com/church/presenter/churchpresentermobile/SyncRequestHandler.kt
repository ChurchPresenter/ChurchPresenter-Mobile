package com.church.presenter.churchpresentermobile

import com.church.presenter.churchpresentermobile.ui.library.SyncSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A request from another tab to open the Library's sync sheet.
 *
 * The sheet's open/closed state is local to `LibraryScreen`, so the Songs and Bible empty states
 * cannot reach it directly. Rather than thread a callback down through every screen, this mirrors
 * [TabNavigationHandler]: the empty state asks, the Library tab answers when it next composes,
 * and the request is consumed once so a later visit does not reopen the sheet.
 */
object SyncRequestHandler {

    private val _requested = MutableStateFlow<SyncSection?>(null)
    val requested: StateFlow<SyncSection?> = _requested.asStateFlow()

    /** Asks the Library tab to open its sync sheet on [section]. */
    fun request(section: SyncSection) {
        _requested.value = section
    }

    fun consume() {
        _requested.value = null
    }
}
