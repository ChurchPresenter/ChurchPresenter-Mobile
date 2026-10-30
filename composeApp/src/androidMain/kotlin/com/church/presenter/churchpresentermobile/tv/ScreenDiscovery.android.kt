package com.church.presenter.churchpresentermobile.tv

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import com.church.presenter.churchpresentermobile.util.ActivityHolder
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "ScreenDiscovery"

/**
 * The list after [source] was found: added, or put in place of an earlier entry
 * of the same name — a phone that moved to a new address or port is the same
 * phone, and listing it twice would offer the TV a dead one.
 */
internal fun List<PhoneSource>.withFound(source: PhoneSource): List<PhoneSource> =
    filterNot { it.name == source.name } + source

/** The list after the phone called [name] stopped advertising. */
internal fun List<PhoneSource>.withoutLost(name: String): List<PhoneSource> =
    filterNot { it.name == name }

private fun nsd(): NsdManager? =
    ActivityHolder.current?.applicationContext?.getSystemService(Context.NSD_SERVICE) as? NsdManager

/** Advertises the phone's display server as [PHONE_SERVICE_TYPE], named for the device. */
actual object PhoneAdvert {
    private var listener: NsdManager.RegistrationListener? = null

    actual fun start(port: Int) {
        stop()
        val manager = nsd() ?: return
        val info = NsdServiceInfo().apply {
            serviceName = "ChurchPresenter · ${Build.MODEL}"
            serviceType = PHONE_SERVICE_TYPE
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

    actual fun stop() {
        val registration = listener ?: return
        listener = null
        runCatching { nsd()?.unregisterService(registration) }
    }
}

/**
 * Browses for [PHONE_SERVICE_TYPE] and resolves each phone to an address.
 *
 * Resolved one at a time: before Android 14, `NsdManager` refuses a second
 * resolve while one is running, and two phones found in the same instant would
 * otherwise lose one of them.
 */
actual class PhoneFinder actual constructor() {
    private val _found = MutableStateFlow<List<PhoneSource>>(emptyList())
    actual val found: StateFlow<List<PhoneSource>> = _found.asStateFlow()

    private var discovery: NsdManager.DiscoveryListener? = null
    private val waiting = ArrayDeque<NsdServiceInfo>()
    private var resolving = false

    actual fun start() {
        if (discovery != null) return
        val manager = nsd() ?: return
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, error: Int) =
                Logger.e(TAG, "discovery failed to start: $error")
            override fun onStopDiscoveryFailed(serviceType: String, error: Int) = Unit
            override fun onServiceFound(info: NsdServiceInfo) = enqueue(manager, info)
            override fun onServiceLost(info: NsdServiceInfo) =
                _found.update { it.withoutLost(info.serviceName) }
        }
        runCatching { manager.discoverServices(PHONE_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener) }
            .onSuccess { discovery = listener }
            .onFailure { Logger.e(TAG, "discovery failed: ${it.message}") }
    }

    actual fun stop() {
        val listener = discovery ?: return
        discovery = null
        runCatching { nsd()?.stopServiceDiscovery(listener) }
        synchronized(waiting) { waiting.clear() }
    }

    private fun enqueue(manager: NsdManager, info: NsdServiceInfo) {
        synchronized(waiting) {
            waiting.addLast(info)
            if (resolving) return
            resolving = true
        }
        resolveNext(manager)
    }

    @Suppress("DEPRECATION") // resolveService: the replacement is API 34+, and this app runs on 24+.
    private fun resolveNext(manager: NsdManager) {
        val next = synchronized(waiting) {
            waiting.removeFirstOrNull().also { if (it == null) resolving = false }
        } ?: return
        manager.resolveService(next, object : NsdManager.ResolveListener {
            override fun onResolveFailed(info: NsdServiceInfo, error: Int) {
                Logger.d(TAG, "could not resolve ${info.serviceName}: $error")
                resolveNext(manager)
            }

            override fun onServiceResolved(info: NsdServiceInfo) {
                val host = info.host?.hostAddress
                if (host != null) {
                    _found.update { it.withFound(PhoneSource(info.serviceName, host, info.port)) }
                }
                resolveNext(manager)
            }
        })
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
