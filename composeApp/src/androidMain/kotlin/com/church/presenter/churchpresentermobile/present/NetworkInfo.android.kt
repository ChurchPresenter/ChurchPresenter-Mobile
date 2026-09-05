package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.util.Logger
import java.net.Inet4Address
import java.net.NetworkInterface

private const val TAG = "NetworkInfo"

/**
 * Reads the address straight off the network interfaces rather than asking
 * WifiManager, which would need ACCESS_WIFI_STATE and still miss the case that
 * matters most in a hall with no router: the phone acting as its own hotspot.
 *
 * Wi-Fi and hotspot interfaces are preferred over anything else so a phone with
 * mobile data active does not advertise an address no TV can reach.
 */
actual fun localIpAddress(): String? = runCatching {
    val candidates = NetworkInterface.getNetworkInterfaces()
        .asSequence()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { iface ->
            iface.inetAddresses.asSequence()
                .filterIsInstance<Inet4Address>()
                .filterNot { it.isLoopbackAddress }
                .map { iface.name to it.hostAddress }
        }
        .filter { (_, address) -> !address.isNullOrBlank() }
        .toList()

    val preferred = candidates.firstOrNull { (name, _) -> PREFERRED_PREFIXES.any(name::startsWith) }
    (preferred ?: candidates.firstOrNull())?.second
}.onFailure { Logger.e(TAG, "failed to read local IP: ${it.message}") }.getOrNull()

/** wlan = Wi-Fi client, ap/swlan = tethering hotspot. */
private val PREFERRED_PREFIXES = listOf("wlan", "ap", "swlan")
