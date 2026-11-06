package com.church.presenter.churchpresentermobile.tv

import com.church.presenter.churchpresentermobile.network.apiRunCatching
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/** The port a TV listens on for a phone's requests; mDNS tells the phone if it moved. */
const val TV_CONTROL_PORT = 8771

/** Asks a TV to show something. The body is a [TvSource]. */
const val TV_SHOW_PATH = "/show"

/** Asks a TV to stop and go back to its home screen. */
const val TV_HOME_PATH = "/home"

private val controlJson = Json { ignoreUnknownKeys = true }

/** The body of a show request, or null for anything a TV should not act on. */
internal fun readShowRequest(body: String): TvSource? =
    runCatching { controlJson.decodeFromString<TvSource>(body) }.getOrNull()
        ?.takeIf { it.host.isNotBlank() && it.port in 1..65_535 }

/** Tells [tv] to show [source]. */
suspend fun showOnTv(client: HttpClient, tv: FoundService, source: TvSource): Result<Unit> = apiRunCatching {
    val response = client.post("http://${tv.host}:${tv.port}$TV_SHOW_PATH") {
        contentType(ContentType.Application.Json)
        setBody(controlJson.encodeToString(TvSource.serializer(), source))
    }
    check(response.status.isSuccess()) { "the TV answered ${response.status.value}" }
}

/** Tells [tv] to stop showing anything and go back to its home screen. */
suspend fun clearTv(client: HttpClient, tv: FoundService): Result<Unit> = apiRunCatching {
    val response = client.post("http://${tv.host}:${tv.port}$TV_HOME_PATH")
    check(response.status.isSuccess()) { "the TV answered ${response.status.value}" }
}
