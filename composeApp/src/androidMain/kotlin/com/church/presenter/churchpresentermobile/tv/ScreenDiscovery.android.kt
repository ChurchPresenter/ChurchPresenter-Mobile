package com.church.presenter.churchpresentermobile.tv

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import com.church.presenter.churchpresentermobile.util.ActivityHolder
import com.church.presenter.churchpresentermobile.util.Logger
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.serverConfig
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "ScreenDiscovery"

/** How long the TV's listener is given to finish a request when it stops. */
private const val STOP_GRACE_MS = 100L

/**
 * The list after [service] was found: added, or put in place of an earlier entry
 * of the same name — something that moved to a new address or port is the same
 * thing, and listing it twice would offer a dead one.
 */
internal fun List<FoundService>.withFound(service: FoundService): List<FoundService> =
    filterNot { it.name == service.name } + service

/** The list after whatever is called [name] stopped advertising. */
internal fun List<FoundService>.withoutLost(name: String): List<FoundService> =
    filterNot { it.name == name }

private fun nsd(): NsdManager? =
    ActivityHolder.current?.applicationContext?.getSystemService(Context.NSD_SERVICE) as? NsdManager

/** One service registered with NSD, for as long as it is held. */
private class Advert(private val type: String) {
    private var listener: NsdManager.RegistrationListener? = null

    fun start(name: String, port: Int) {
        stop()
        val manager = nsd() ?: return
        val info = NsdServiceInfo().apply {
            serviceName = name
            serviceType = type
            setPort(port)
        }
        val registration = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) =
                Logger.d(TAG, "advertising ${info.serviceName} on $port")
            override fun onRegistrationFailed(info: NsdServiceInfo, error: Int) =
                Logger.e(TAG, "could not advertise: $error")
            override fun onServiceUnregistered(info: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(info: NsdServiceInfo, error: Int) = Unit
        }
        runCatching { manager.registerService(info, NsdManager.PROTOCOL_DNS_SD, registration) }
            .onSuccess { listener = registration }
            .onFailure { Logger.e(TAG, "could not advertise: ${it.message}") }
    }

    fun stop() {
        val registration = listener ?: return
        listener = null
        runCatching { nsd()?.unregisterService(registration) }
    }
}

/** Advertises the phone's display server as [PHONE_SERVICE_TYPE], named for the device. */
actual object PhoneAdvert {
    private val advert = Advert(PHONE_SERVICE_TYPE)

    actual fun start(port: Int) = advert.start("ChurchPresenter · ${Build.MODEL}", port)

    actual fun stop() = advert.stop()
}

/**
 * Resolves services one at a time, for every finder in the process: before
 * Android 14, `NsdManager` refuses a second resolve while one is running, and a
 * TV browses for phones and desktops at once.
 */
private object Resolver {
    private class Job(val info: NsdServiceInfo, val onResolved: (FoundService) -> Unit)

    private val waiting = ArrayDeque<Job>()
    private var resolving = false

    fun enqueue(manager: NsdManager, info: NsdServiceInfo, onResolved: (FoundService) -> Unit) {
        synchronized(waiting) {
            waiting.addLast(Job(info, onResolved))
            if (resolving) return
            resolving = true
        }
        next(manager)
    }

    @Suppress("DEPRECATION") // resolveService: the replacement is API 34+, and this app runs on 24+.
    private fun next(manager: NsdManager) {
        val job = synchronized(waiting) {
            waiting.removeFirstOrNull().also { if (it == null) resolving = false }
        } ?: return
        manager.resolveService(job.info, object : NsdManager.ResolveListener {
            override fun onResolveFailed(info: NsdServiceInfo, error: Int) {
                Logger.d(TAG, "could not resolve ${info.serviceName}: $error")
                next(manager)
            }

            override fun onServiceResolved(info: NsdServiceInfo) {
                info.host?.hostAddress?.let { job.onResolved(FoundService(info.serviceName, it, info.port)) }
                next(manager)
            }
        })
    }
}

/** Browses for one service type and resolves each to an address. */
actual class ServiceFinder actual constructor(private val serviceType: String) {
    private val _found = MutableStateFlow<List<FoundService>>(emptyList())
    actual val found: StateFlow<List<FoundService>> = _found.asStateFlow()

    private var discovery: NsdManager.DiscoveryListener? = null

    actual fun start() {
        if (discovery != null) return
        val manager = nsd() ?: return
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, error: Int) =
                Logger.e(TAG, "discovery failed to start: $error")
            override fun onStopDiscoveryFailed(serviceType: String, error: Int) = Unit
            override fun onServiceFound(info: NsdServiceInfo) =
                Resolver.enqueue(manager, info) { service -> _found.update { it.withFound(service) } }
            override fun onServiceLost(info: NsdServiceInfo) =
                _found.update { it.withoutLost(info.serviceName) }
        }
        runCatching { manager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener) }
            .onSuccess { discovery = listener }
            .onFailure { Logger.e(TAG, "discovery failed: ${it.message}") }
    }

    actual fun stop() {
        val listener = discovery ?: return
        discovery = null
        runCatching { nsd()?.stopServiceDiscovery(listener) }
    }
}

/**
 * Listens on [TV_CONTROL_PORT] and advertises [TV_SERVICE_TYPE], named as the TV
 * names itself, so a phone lists it as "Living Room" rather than by model.
 *
 * Anyone on the church's network can point the TV at a source, the same reach a
 * phone's display page has; a source still has to exist and answer to be shown.
 */
actual class TvControlServer actual constructor() {
    private val _requested = MutableStateFlow<TvSource?>(null)
    actual val requested: StateFlow<TvSource?> = _requested.asStateFlow()
    private val _home = MutableStateFlow(0)
    actual val home: StateFlow<Int> = _home.asStateFlow()

    private val advert = Advert(TV_SERVICE_TYPE)
    private var server: EmbeddedServer<*, *>? = null

    actual fun start() {
        if (server != null) return
        // CIO reports a failed bind on a root coroutine: without this handler it
        // goes to the uncaught-exception handler and takes the app down. A TV that
        // cannot listen still shows slides; it just cannot be picked from a phone.
        val handler = CoroutineExceptionHandler { _, e -> Logger.e(TAG, "TV listener failed: ${e.message}", e) }
        val config = serverConfig {
            parentCoroutineContext = handler
            module {
                routing {
                    post(TV_SHOW_PATH) {
                        val source = readShowRequest(call.receiveText())
                        if (source == null) {
                            call.respond(HttpStatusCode.BadRequest)
                        } else {
                            _requested.value = source
                            call.respond(HttpStatusCode.NoContent)
                        }
                    }
                    post(TV_HOME_PATH) {
                        _home.update { it + 1 }
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
            }
        }
        val engine = embeddedServer(CIO, config) {
            connector { port = TV_CONTROL_PORT }
            reuseAddress = true
        }
        runCatching { engine.start(wait = false) }
            .onSuccess {
                server = engine
                advert.start(tvName(), TV_CONTROL_PORT)
            }
            .onFailure { Logger.e(TAG, "TV listener failed: ${it.message}", it) }
    }

    actual fun stop() {
        advert.stop()
        server?.stop(STOP_GRACE_MS, STOP_GRACE_MS)
        server = null
    }

    actual fun consume() {
        _requested.value = null
    }

    private fun tvName(): String {
        val resolver = ActivityHolder.current?.contentResolver
        return resolver?.let { Settings.Global.getString(it, Settings.Global.DEVICE_NAME) } ?: Build.MODEL
    }
}

/** Sets the window's keep-screen-on flag for as long as this is composed. */
@Composable
actual fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

/**
 * True on Android TV and Google TV — the Bravia, a Chromecast with Google TV,
 * a Shield, a Fire TV — where the app starts as a screen rather than a remote.
 */
internal fun Context.isTelevision(): Boolean =
    (getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)
        ?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
