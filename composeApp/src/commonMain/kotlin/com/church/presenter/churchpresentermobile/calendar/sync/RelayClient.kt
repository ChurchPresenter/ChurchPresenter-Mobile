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

    /** The relay did not accept the client key; fetch a fresh one and try again. */
    class ClientKey : RelayFailure("relay refused the client key")

    class Rejected(status: Int, detail: String) : RelayFailure("relay rejected the request ($status): $detail")
}

/** This phone's half of the relay protocol. */
class RelayClient(
    private val state: CalendarSyncState,
    private val clientKey: suspend () -> String,
    private val client: HttpClient = createHttpClient(),
) {
    private val base = "${state.relayUrl.trimEnd('/')}/i/${state.instanceId}"

    /** Tells the relay where a silent "changed" push reaches this phone. */
    suspend fun registerPushToken(pushToken: String) {
        val key = clientKey()
        val response = client.put("$base/devices/me/push") {
            auth(key)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(PushTokenBody.serializer(), PushTokenBody(pushToken)))
        }
        checked(response)
    }

    /** Tells the relay what this phone calls itself, sealed so only the desktop can read it. */
    suspend fun registerName(nameBox: String) {
        val key = clientKey()
        val response = client.put("$base/devices/me/name") {
            auth(key)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(DeviceNameBody.serializer(), DeviceNameBody(nameBox)))
        }
        checked(response)
    }

    suspend fun changes(since: Long): ChangesResponse {
        val key = clientKey()
        val response = client.get("$base/changes?since=$since") { auth(key) }
        return json.decodeFromString(ChangesResponse.serializer(), checked(response).bodyAsText())
    }

    /** Writes one record; [ifRev] is the revision our copy came from, 0 for a service the relay has never seen. */
    suspend fun putRecord(record: SealedRecord, ifRev: Long): Long {
        val key = clientKey()
        val response = client.put("$base/records/${record.id}") {
            auth(key)
            header(HttpHeaders.IfMatch, ifRev.toString())
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(SealedRecord.serializer(), record))
        }
        return json.decodeFromString(WriteResponse.serializer(), checked(response).bodyAsText()).rev
    }

    suspend fun deleteRecord(id: String): Long {
        val key = clientKey()
        val response = client.delete("$base/records/$id") { auth(key) }
        return json.decodeFromString(WriteResponse.serializer(), checked(response).bodyAsText()).rev
    }

    private fun HttpRequestBuilder.auth(clientKey: String) {
        header(HttpHeaders.Authorization, "Bearer ${state.deviceToken}")
        header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        if (clientKey.isNotBlank()) header(CLIENT_KEY_HEADER, clientKey)
    }

    private suspend fun checked(response: HttpResponse): HttpResponse = when (response.status) {
        HttpStatusCode.Unauthorized ->
            if (response.bodyAsText().contains(CLIENT_KEY_ERROR)) {
                throw RelayFailure.ClientKey()
            } else {
                throw RelayFailure.Unauthorized()
            }
        HttpStatusCode.Forbidden -> throw RelayFailure.Unauthorized()
        HttpStatusCode.Conflict, HttpStatusCode.PreconditionFailed -> throw RelayFailure.Conflict()
        else ->
            if (response.status.isSuccess()) {
                response
            } else {
                throw RelayFailure.Rejected(response.status.value, response.bodyAsText().take(MAX_ERROR_CHARS))
            }
    }

    private companion object {
        const val MAX_ERROR_CHARS = 200
        const val CLIENT_KEY_HEADER = "X-Client-Key"
        const val CLIENT_KEY_ERROR = "\"client_key\""
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
            HttpStatusCode.Conflict -> if (response.bodyAsText().contains(SYNC_OFF_ERROR)) throw EnrollSyncOff()
            else -> Unit
        }
        if (!response.status.isSuccess()) {
            throw RelayFailure.Rejected(response.status.value, response.bodyAsText().take(MAX_ERROR_CHARS))
        }
        val reply = json.decodeFromString(EnrollReply.serializer(), response.bodyAsText())
        val relay = Sanitize.relayUrl(reply.relayUrl) ?: throw RelayFailure.Rejected(response.status.value, "relay url")
        val instance = reply.instanceId.takeIf(Sanitize::isId)
            ?: throw RelayFailure.Rejected(response.status.value, "instance id")
        EnrollReply(
            relayUrl = relay,
            instanceId = instance,
            deviceId = reply.deviceId.takeIf(Sanitize::isId).orEmpty(),
            deviceToken = Sanitize.secret(reply.deviceToken).orEmpty(),
            instanceKey = Sanitize.secret(reply.instanceKey).orEmpty(),
        )
    }

    private companion object {
        const val MAX_ERROR_CHARS = 200
        const val SYNC_OFF_ERROR = "\"sync_off\""
    }
}

/** The operator said no. */
class EnrollDenied : Exception("enrollment denied")

/** The desktop's calendar sync switch is off, so it has nothing to enroll this phone into. */
class EnrollSyncOff : Exception("calendar sync is off on the desktop")
