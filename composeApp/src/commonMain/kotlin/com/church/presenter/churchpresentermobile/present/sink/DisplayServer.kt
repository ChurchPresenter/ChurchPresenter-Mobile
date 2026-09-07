package com.church.presenter.churchpresentermobile.present.sink

import com.church.presenter.churchpresentermobile.present.LocalWebServer
import com.church.presenter.churchpresentermobile.present.PhotoSource
import com.church.presenter.churchpresentermobile.present.WebAssets
import kotlinx.coroutines.flow.StateFlow

/**
 * The embedded display server, as [WebPageSink] uses it.
 *
 * Named as an interface purely so the sink can be tested: [LocalWebServer] is an
 * `expect class`, which cannot be subclassed, and the JVM actual the test suite
 * runs on throws from `start` because a desktop host does not serve the display
 * page. Without this seam every attach in a test fails, and the path that
 * actually matters — a bound port becoming the URL the operator reads out to a
 * TV — could not be exercised at all.
 *
 * The four members are exactly what the sink calls, so this adds no capability
 * and no second implementation ships: [localWebServerFactory] wraps the real
 * one, and the only other implementation is the fake in the tests.
 */
interface DisplayServer {

    /** How many display pages are currently connected. */
    val clientCount: StateFlow<Int>

    /** Binds and starts serving, returning the port actually bound. */
    suspend fun start(preferredPort: Int, candidates: List<Int>): Int

    /** Stops serving and disconnects every display. Idempotent. */
    suspend fun stop()

    /** Pushes [json] to every connected display, and remembers it as the current frame. */
    fun publish(json: String)
}

/** Builds the platform's real server, as [WebPageSink] does when nothing is injected. */
internal val localWebServerFactory: (WebAssets, PhotoSource) -> DisplayServer =
    { assets, photos -> LocalWebServerDisplay(LocalWebServer(assets, photos)) }

/** Adapts the platform's [LocalWebServer] to [DisplayServer]. */
private class LocalWebServerDisplay(private val delegate: LocalWebServer) : DisplayServer {

    override val clientCount: StateFlow<Int> get() = delegate.clientCount

    override suspend fun start(preferredPort: Int, candidates: List<Int>): Int =
        delegate.start(preferredPort, candidates)

    override suspend fun stop() = delegate.stop()

    override fun publish(json: String) = delegate.publish(json)
}
