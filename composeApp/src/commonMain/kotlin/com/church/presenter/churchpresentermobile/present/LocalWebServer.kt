package com.church.presenter.churchpresentermobile.present

import kotlinx.coroutines.flow.StateFlow

/**
 * The phone's own HTTP + WebSocket server for the projected display page.
 *
 * A TV browser, a laptop driving a projector, or a second phone opens
 * `http://<phone-ip>:<port>` and becomes an audience screen. The page it
 * receives is bundled in the app, so the whole thing works on a hall network
 * with no internet at all.
 *
 * Besides the bundled page it serves the photos the operator picked this
 * session, so the browser display and the in-process one load an image from the
 * identical address — see [PhotoSource].
 *
 * Implemented once for Android and iOS on Ktor's CIO engine (see mobileMain).
 * The web target has an actual that reports "unsupported" — a browser tab
 * cannot listen on a socket.
 */
expect class LocalWebServer(assets: WebAssets, photos: PhotoSource) {

    /** How many display pages are currently connected. */
    val clientCount: StateFlow<Int>

    /**
     * Binds and starts serving, returning the port actually bound.
     *
     * Tries [preferredPort] first, then the caller's [candidates], then an
     * ephemeral port — a service should not fail to start just because another
     * app holds the usual port.
     *
     * @throws IllegalStateException when no port could be bound at all.
     */
    suspend fun start(preferredPort: Int, candidates: List<Int>): Int

    /** Stops serving and disconnects every display. Idempotent. */
    suspend fun stop()

    /**
     * Pushes [json] to every connected display and remembers it as the current
     * frame, so a page that connects later is brought straight up to date
     * rather than sitting on the standby screen until the next slide change.
     */
    fun publish(json: String)
}
