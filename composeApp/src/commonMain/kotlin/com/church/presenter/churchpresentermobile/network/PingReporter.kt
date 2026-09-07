package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.getPlatform
import com.church.presenter.churchpresentermobile.util.appVersion
import com.church.presenter.churchpresentermobile.util.isDebugBuild
// Aliased so the buildPingUrl parameters below can carry the obvious names
// without shadowing the build-time values they default to.
import com.church.presenter.churchpresentermobile.util.buildType as defaultBuildType
import com.church.presenter.churchpresentermobile.util.commitHash as defaultCommitHash
import com.church.presenter.churchpresentermobile.util.repoSlug as defaultRepoSlug
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    /**
     * @param repoSlug The git origin this build was made from, as "owner/name"
     * (see the provenance helpers in build.gradle.kts). The app is open-source
     * and hardcodes [PING_URL], so without this a fork's launches are
     * indistinguishable from real installs on the live map. Only the slug is
     * sent — never the remote URL, which can embed credentials.
     * @param commit Short commit hash the build was made from.
     * @param buildType "release", "snapshot", "dirty" or "nogit" — separates a
     * real end-user run from a self-compiled one, including inside a fork
     * (src=dev only covers debug builds of our own source).
     *
     * Each is omitted when unknown; the server treats a missing value the same
     * as an unrecognised one and still counts the ping.
     */
    internal fun buildPingUrl(
        os: String,
        version: String,
        isDevBuild: Boolean,
        connected: Boolean,
        repoSlug: String = defaultRepoSlug,
        commit: String = defaultCommitHash,
        buildType: String = defaultBuildType,
    ): String = buildString {
        append(PING_URL)
        append("?platform=mobile")
        append("&os=$os")
        append("&version=$version")
        if (isDevBuild) append("&src=dev")
        if (connected) append("&connected=true")
        if (repoSlug.isNotBlank() && repoSlug != "unknown") append("&repo=$repoSlug")
        if (commit.isNotBlank() && commit != "unknown") append("&commit=$commit")
        if (buildType.isNotBlank() && buildType != "unknown") append("&build=$buildType")
    }

    private fun send(installId: String, connected: Boolean) {
        send(installId, connected, client, scope)
    }

    /**
     * The ping itself, over a supplied client and scope.
     *
     * `internal` so what goes on the wire can be checked without a request
     * reaching the live map — see the seam guidance in AGENT.md. Returns the job
     * so a test can wait for it; nothing in the app looks at it.
     */
    internal fun send(
        installId: String,
        connected: Boolean,
        httpClient: HttpClient,
        into: CoroutineScope,
    ): Job {
        val url = buildPingUrl(getPlatform().os, appVersion, isDebugBuild, connected)
        return into.launch {
            apiRunCatching {
                httpClient.get(url) {
                    if (installId.isNotBlank()) header("X-Install-Id", installId)
                }
            }
        }
    }
}
