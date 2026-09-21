package com.church.presenter.churchpresentermobile.calendar.sync

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock

private const val CONFIG_URL = "https://www.churchpresenter.org/api/relay-config"
private const val REFRESH_AFTER_MS = 24 * 60 * 60 * 1000L
private val KEY_SHAPE = Regex("^[A-Za-z0-9+/=_-]{16,256}$")
private val json = Json { ignoreUnknownKeys = true }

@Serializable
private data class RelayConfig(val clientKey: String = "")

/**
 * The relay's shared client key, fetched from the website and cached in settings: refreshed
 * once a day, and again when the relay stops accepting it. Comes from the website rather than
 * being built in, so a rotation needs no app update.
 */
class ClientKeySource(
    private val settings: AppSettings,
    private val client: HttpClient = createHttpClient(),
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    suspend fun current(): String {
        val cached = settings.relayClientKey
        if (cached.isNotBlank() && now() - settings.relayClientKeyFetchedAt < REFRESH_AFTER_MS) return cached
        return refresh() ?: cached
    }

    /** Fetches a fresh key; null when the website could not be reached or answered nonsense. */
    suspend fun refresh(): String? {
        val key = runCatching {
            val response = client.get(CONFIG_URL)
            if (!response.status.isSuccess()) return null
            json.decodeFromString(RelayConfig.serializer(), response.bodyAsText()).clientKey
        }.getOrNull() ?: return null
        if (!KEY_SHAPE.matches(key)) return null
        settings.relayClientKey = key
        settings.relayClientKeyFetchedAt = now()
        return key
    }
}
