package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.getPlatform
import com.church.presenter.churchpresentermobile.util.appVersion
import com.church.presenter.churchpresentermobile.util.isDebugBuild
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Sends an anonymous, city-level ping to the ChurchPresenter live world map
 * (churchpresenter.org/map) when the app is opened. Mirrors the desktop app's
 * LiveMapReporter so mobile launches show up on the same map, tagged as mobile.
 *
 * No personal data is transmitted — Cloudflare derives a city-level coordinate
 * server-side from the network layer. No IP address is stored.
 */
object PingReporter {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val client by lazy { createHttpClient() }

    private const val PING_URL = "https://www.churchpresenter.org/api/ping"

    /**
     * Fired once per app launch, before/independent of any desktop connection.
     *
     * @param installId Stable anonymous install id (see [com.church.presenter.churchpresentermobile.model.AppSettings.deviceId]),
     * sent as the X-Install-Id header so the server dedupes repeat launches to one row per install.
     */
    fun pingOnOpen(installId: String) = send(installId, connected = false)

    /**
     * Fired once when the app first connects to a desktop over the LAN this
     * session. Sends `connected=true`, which the server uses to flip this
     * install's launch row so standalone (never-connected) mobile use stays
     * distinguishable from paired use. Reconnects must NOT re-fire this.
     */
    fun pingConnected(installId: String) = send(installId, connected = true)

    private fun send(installId: String, connected: Boolean) {
        val url = buildString {
            append(PING_URL)
            append("?platform=mobile")
            append("&os=${getPlatform().os}")
            append("&version=$appVersion")
            if (isDebugBuild) append("&src=dev")
            if (connected) append("&connected=true")
        }
        scope.launch {
            apiRunCatching {
                client.get(url) {
                    if (installId.isNotBlank()) header("X-Install-Id", installId)
                }
            }
        }
    }
}
