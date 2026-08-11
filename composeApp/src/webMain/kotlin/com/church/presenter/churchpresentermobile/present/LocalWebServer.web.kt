package com.church.presenter.churchpresentermobile.present

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Unreachable placeholder for the js/wasmJs targets.
 *
 * A browser tab cannot bind a listening socket, so [supportsEmbeddedServer] is
 * false on web and nothing ever constructs this. It exists only so the shared
 * standalone code compiles for every target.
 */
actual class LocalWebServer actual constructor(assets: WebAssets) {

    private val _clientCount = MutableStateFlow(0)
    actual val clientCount: StateFlow<Int> = _clientCount.asStateFlow()

    actual suspend fun start(preferredPort: Int, candidates: List<Int>): Int =
        throw UnsupportedOperationException("A browser cannot host the presentation server")

    actual suspend fun stop() = Unit

    actual fun publish(json: String) = Unit
}
