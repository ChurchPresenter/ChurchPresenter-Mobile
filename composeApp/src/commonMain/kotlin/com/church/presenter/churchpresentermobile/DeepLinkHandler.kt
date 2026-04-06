package com.church.presenter.churchpresentermobile

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "DeepLinkHandler"

/**
 * Parses and applies `churchpresenter://connect` deep links.
 *
 * Supported URL formats:
 *   churchpresenter://connect?host=192.168.1.50&port=8765&apikey=344fh
 *   churchpresenter://connect?host=192.168.1.50&port=8765
 *
 * Call [handle] from each platform's URL-open callback.
 * Observe [appliedCount] in the UI to react to settings changes.
 */
object DeepLinkHandler {

    private val _appliedCount = MutableStateFlow(0)

    /**
     * Incremented each time a valid deep link is successfully applied.
     * Compose UI should observe this and call settingsSaveToken++ when it changes.
     */
    val appliedCount: StateFlow<Int> = _appliedCount.asStateFlow()

    /**
     * Parses [url] and, if it is a valid `churchpresenter://connect` link,
     * writes host, port and optional apiKey directly to [settings].
     *
     * @return true if the link was recognised and settings were updated.
     */
    fun handle(url: String, settings: AppSettings): Boolean {
        Logger.d(TAG, "handle url=$url")

        if (!url.lowercase().startsWith("churchpresenter://connect")) return false

        val queryString = url.substringAfter("?", "")
        if (queryString.isBlank()) {
            Logger.e(TAG, "Deep link missing query string — ignoring")
            return false
        }

        val params = queryString.split("&").mapNotNull { pair ->
            val idx = pair.indexOf('=')
            if (idx < 0) null else pair.substring(0, idx).lowercase() to pair.substring(idx + 1)
        }.toMap()

        val host = params["host"]?.trim()?.takeIf { it.isNotBlank() }
        val port = params["port"]?.trim()?.toIntOrNull()

        if (host == null || port == null || port !in 1..65535) {
            Logger.e(TAG, "Deep link missing valid host/port — ignoring (host=$host port=${params["port"]})")
            return false
        }

        settings.host   = host
        settings.port   = port
        params["apikey"]?.trim()?.let { settings.apiKey = it }

        Logger.d(TAG, "Applied deep link — host=$host port=$port apiKey=${params["apikey"] ?: "(none)"}")
        _appliedCount.value += 1
        return true
    }
}

