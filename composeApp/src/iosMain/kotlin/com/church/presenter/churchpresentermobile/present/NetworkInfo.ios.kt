package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.posix.AF_INET
import platform.posix.NI_MAXHOST
import platform.posix.NI_NUMERICHOST
import platform.darwin.freeifaddrs
import platform.darwin.getifaddrs
import platform.posix.getnameinfo
import platform.darwin.ifaddrs

private const val TAG = "NetworkInfo"

/**
 * Walks the interface list with `getifaddrs`.
 *
 * `en0` is Wi-Fi on a device; `bridge100` is the personal-hotspot interface,
 * which matters in a hall with no router — the phone hosts the network and the
 * TV joins it. Anything else (notably `pdp_ip0`, the mobile-data interface) is
 * only a last resort, since no TV in the room can route to it.
 *
 * Addresses are formatted with `getnameinfo(NI_NUMERICHOST)` rather than
 * `inet_ntop`, which Kotlin/Native does not expose.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun localIpAddress(): String? = runCatching {
    memScoped {
        val list = alloc<CPointerVar<ifaddrs>>()
        if (getifaddrs(list.ptr) != 0) return@memScoped null

        val found = mutableListOf<Pair<String, String>>()
        try {
            var cursor = list.value
            while (cursor != null) {
                val entry = cursor.pointed
                val address = entry.ifa_addr
                val name = entry.ifa_name?.toKString()

                if (address != null && name != null && address.pointed.sa_family.toInt() == AF_INET) {
                    val host = allocArray<ByteVar>(NI_MAXHOST)
                    val ok = getnameinfo(
                        address,
                        address.pointed.sa_len.toUInt(),
                        host,
                        NI_MAXHOST.toUInt(),
                        null,
                        0u,
                        NI_NUMERICHOST,
                    ) == 0
                    val text = if (ok) host.toKString() else ""
                    if (text.isNotBlank() && text != LOOPBACK) found += name to text
                }
                cursor = entry.ifa_next
            }
        } finally {
            freeifaddrs(list.value)
        }

        val preferred = found.firstOrNull { (name, _) -> PREFERRED_INTERFACES.any(name::startsWith) }
        (preferred ?: found.firstOrNull())?.second
    }
}.onFailure { Logger.e(TAG, "failed to read local IP: ${it.message}") }.getOrNull()

private const val LOOPBACK = "127.0.0.1"

/** en0/en1 = Wi-Fi, bridge100 = personal hotspot. */
private val PREFERRED_INTERFACES = listOf("en0", "en1", "bridge")
