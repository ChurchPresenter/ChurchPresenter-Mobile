package com.church.presenter.churchpresentermobile.present

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Unreachable placeholder, matching the web target's.
 *
 * [supportsEmbeddedServer] is false on this target, so nothing constructs it.
 */
actual class LocalWebServer actual constructor(assets: WebAssets, photos: PhotoSource) {

    private val _clientCount = MutableStateFlow(0)
    actual val clientCount: StateFlow<Int> = _clientCount.asStateFlow()

    actual suspend fun start(preferredPort: Int, candidates: List<Int>): Int =
        throw UnsupportedOperationException("The JVM test host does not serve the display page")

    actual suspend fun stop() = Unit

    actual fun publish(json: String) = Unit
}
