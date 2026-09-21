package com.church.presenter.churchpresentermobile.calendar.sync

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.ApiConstants
import com.church.presenter.churchpresentermobile.network.createActionHttpClient
import com.church.presenter.churchpresentermobile.network.createHttpClient
import com.church.presenter.churchpresentermobile.network.identifyDevice
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/** Why a relay call did not go through, as the sync engine tells them apart. */
sealed class RelayFailure(message: String) : Exception(message) {
    /** The device token was refused: this phone was revoked, or the desktop started over. */
    class Unauthorized : RelayFailure("relay refused the device token")

    /** The record moved on since our copy; pull and reapply. */
    class Conflict : RelayFailure("record changed since last sync")

    class Rejected(status: Int, detail: String) : RelayFailure("relay rejected the request ($status): $detail")
}

/** This phone's half of the relay protocol. */
class RelayClient(
    private val state: CalendarSyncState,
    private val client: HttpClient = createHttpClient(),
) {
    private val base = "${state.relayUrl.trimEnd('/')}/i/${state.instanceId}"

    suspend fun changes(since: Long): ChangesResponse {
        val response = client.get("$base/changes?since=$since") { auth() }
        return json.decodeFromString(ChangesResponse.serializer(), checked(response).bodyAsText())
    }

    /** Writes one record; [ifRev] is the revision our copy came from, 0 for a service the relay has never seen. */
    suspend fun putRecord(record: SealedRecord, ifRev: Long): Long {
        val response = client.put("$base/records/${record.id}") {
            auth()
            header(HttpHeaders.IfMatch, ifRev.toString())
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(SealedRecord.serializer(), record))
        }
        return json.decodeFromString(WriteResponse.serializer(), checked(response).bodyAsText()).rev
    }

    suspend fun deleteRecord(id: String): Long {
        val response = client.delete("$base/records/$id") { auth() }
        return json.decodeFromString(WriteResponse.serializer(), checked(response).bodyAsText()).rev
    }

    private fun HttpRequestBuilder.auth() {
        header(HttpHeaders.Authorization, "Bearer ${state.deviceToken}")
        header(HttpHeaders.Accept, ContentType.Application.Json.toString())
    }

    private suspend fun checked(response: HttpResponse): HttpResponse = when (response.status) {
        HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> throw RelayFailure.Unauthorized()
        HttpStatusCode.Conflict, HttpStatusCode.PreconditionFailed -> throw RelayFailure.Conflict()
        else -> if (response.status.isSuccess()) response else throw RelayFailure.Rejected(response.status.value, response.bodyAsText().take(MAX_ERROR_CHARS))
    }

    private companion object {
        const val MAX_ERROR_CHARS = 200
    }
}

/** Asking the desktop to be enrolled, with the code this phone is showing; held open while the operator decides. */
class EnrollService(
    private val settings: AppSettings,
    private val client: HttpClient = createActionHttpClient(),
) {
    suspend fun enroll(deviceName: String, code: String): Result<EnrollReply> = runCatching {
        val response = client.post("${settings.apiBaseUrl}/calendar/enroll") {
            identifyDevice(settings)
            settings.apiKey.takeIf { it.isNotBlank() }?.let { header(ApiConstants.API_KEY_HEADER, it) }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(EnrollBody.serializer(), EnrollBody(deviceName, code)))
        }
        when (response.status) {
            HttpStatusCode.Forbidden -> throw EnrollDenied()
            else -> if (!response.status.isSuccess()) throw RelayFailure.Rejected(response.status.value, response.bodyAsText().take(MAX_ERROR_CHARS))
        }
        val reply = json.decodeFromString(EnrollReply.serializer(), response.bodyAsText())
        val relay = Sanitize.relayUrl(reply.relayUrl) ?: throw RelayFailure.Rejected(response.status.value, "relay url")
        val instance = reply.instanceId.takeIf(Sanitize::isId) ?: throw RelayFailure.Rejected(response.status.value, "instance id")
        EnrollReply(relayUrl = relay, instanceId = instance)
    }

    private companion object {
        const val MAX_ERROR_CHARS = 200
    }
}

/** The operator said no. */
class EnrollDenied : Exception("enrollment denied")
