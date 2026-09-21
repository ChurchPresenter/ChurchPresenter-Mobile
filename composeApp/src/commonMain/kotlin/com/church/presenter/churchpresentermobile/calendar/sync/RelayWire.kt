package com.church.presenter.churchpresentermobile.calendar.sync

import com.church.presenter.churchpresentermobile.model.PresetSummary
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The relay's shapes. The plaintext inside a record is this app's own `PlannedService`. */
@Serializable
data class SealedRecord(
    val id: String,
    val keepUntil: String,
    val box: String,
    val updatedAt: String = "",
    val updatedBy: String = "",
    val rev: Long = 0L,
)

@Serializable
data class RemoteTombstone(val id: String, val deletedAt: String)

@Serializable
data class PresetIndex(val presets: List<PresetSummary> = emptyList())

@Serializable
data class ChangesResponse(
    val rev: Long,
    val records: List<SealedRecord> = emptyList(),
    val tombstones: List<RemoteTombstone> = emptyList(),
    val presetsBox: String = "",
)

@Serializable
data class WriteResponse(val rev: Long)

/** `POST /api/calendar/enroll` on the desktop, over the LAN. */
@Serializable
data class EnrollBody(val deviceName: String, val code: String)

/** What the desktop answers; the token and key come by QR. */
@Serializable
data class EnrollReply(val relayUrl: String, val instanceId: String)

@Serializable
data class PushTokenBody(val pushToken: String)

/** The record id the desktop seals its preset index under. */
const val PRESETS_RECORD = "presets"

/**
 * What this phone holds once enrolled, persisted in settings. [cursor] is the relay revision
 * everything local is in step with.
 */
@Serializable
data class CalendarSyncState(
    val relayUrl: String = "",
    val instanceId: String = "",
    val deviceToken: String = "",
    val instanceKey: String = "",
    val cursor: Long = 0L,
    val lastSyncAt: String = "",
    /** The push token the relay last accepted, so it is re-sent only when it changes. */
    val registeredPushToken: String = "",
) {
    val isEnrolled: Boolean
        get() = relayUrl.isNotBlank() && instanceId.isNotBlank() && deviceToken.isNotBlank() && instanceKey.isNotBlank()

    fun toJson(): String = stateJson.encodeToString(serializer(), this)

    companion object {
        private val stateJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        fun fromJson(text: String): CalendarSyncState =
            runCatching { stateJson.decodeFromString(serializer(), text) }.getOrDefault(CalendarSyncState())

        /**
         * Reads the enrollment QR the desktop shows after approval:
         * `churchpresenter://calendar-enroll?relay=…&instance=…&token=…&key=…`.
         */
        fun fromQr(text: String): CalendarSyncState? {
            val query = text.substringAfter("calendar-enroll?", "").takeIf { it.isNotEmpty() } ?: return null
            val params = query.split('&').mapNotNull { pair ->
                val key = pair.substringBefore('=')
                val value = pair.substringAfter('=', "")
                if (key.isEmpty()) null else key to value
            }.toMap()
            val relay = Sanitize.relayUrl(params["relay"].orEmpty()) ?: return null
            val instance = params["instance"].orEmpty().takeIf(Sanitize::isId) ?: return null
            val token = Sanitize.secret(params["token"].orEmpty()) ?: return null
            val key = Sanitize.secret(params["key"].orEmpty()) ?: return null
            return CalendarSyncState(relayUrl = relay, instanceId = instance, deviceToken = token, instanceKey = key)
        }
    }
}
