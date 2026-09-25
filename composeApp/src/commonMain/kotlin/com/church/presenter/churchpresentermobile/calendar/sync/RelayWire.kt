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
    /** The revision this page is complete up to; the cursor for the next call, whether or not [more]. */
    val rev: Long,
    val records: List<SealedRecord> = emptyList(),
    val tombstones: List<RemoteTombstone> = emptyList(),
    val presetsBox: String = "",
    /** A full page: call again from [rev] for the rest. A client that ignores this skips rows. */
    val more: Boolean = false,
)

@Serializable
data class WriteResponse(val rev: Long)

/** `POST /api/calendar/enroll` on the desktop, over the LAN. */
@Serializable
data class EnrollBody(val deviceName: String, val code: String)

/**
 * What the desktop answers once the operator allows it over the LAN: where the calendar lives. The
 * token and the key are never taken from here, even from a desktop that sends them: this answer
 * crosses plain HTTP on a shared WiFi, where anyone could read them -- or answer in the desktop's
 * place with a relay and a key of their own. They come only from the QR on the desktop's screen.
 */
@Serializable
data class EnrollReply(
    val relayUrl: String,
    val instanceId: String,
    val deviceId: String = "",
    val deviceToken: String = "",
    val instanceKey: String = "",
) {
    /** The enrollment this reply completes, or null when the desktop kept the keys for its QR. */
    fun toState(): CalendarSyncState? {
        val state = CalendarSyncState(
            relayUrl = relayUrl,
            instanceId = instanceId,
            deviceToken = deviceToken,
            instanceKey = instanceKey,
            deviceId = deviceId,
        )
        return state.takeIf { it.isEnrolled }
    }
}

@Serializable
data class PushTokenBody(val pushToken: String)

/** `PUT /devices/me/name` -- this phone's name for the desktop's list, sealed under its device id. */
@Serializable
data class DeviceNameBody(val nameBox: String)

/** The record id the desktop seals its preset index under. */
const val PRESETS_RECORD = "presets"

/** The prefix of a song catalog record: `catalog:<songbook>` or `catalog:<songbook>:<part>`. */
const val CATALOG_PREFIX = "catalog:"

/** One song as the catalog carries it: number, title, its usual length if measured, a second-language title if any. */
@Serializable
data class CatalogSong(val n: String, val t: String, val s: Int? = null, val t2: String? = null)

/** One songbook (or one part of a large one) as the desktop keeps it, on the relay and over the LAN alike. */
@Serializable
data class CatalogRecord(val songbook: String, val part: Int = 0, val songs: List<CatalogSong> = emptyList())

/** Body of GET /api/song-catalog on the desktop. */
@Serializable
data class SongCatalogRecordsResponse(val books: List<CatalogRecord> = emptyList())

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
    /** This phone's id at the relay -- what its name is sealed under. Empty for an old enrollment. */
    val deviceId: String = "",
    /** The name the relay last accepted, so it is re-sent only when it changes. */
    val registeredName: String = "",
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
            val query = text.substringAfter("calendar-enroll?", "")
            val params = query.split('&').mapNotNull { pair ->
                val key = pair.substringBefore('=')
                val value = pair.substringAfter('=', "")
                if (key.isEmpty()) null else key to value
            }.toMap()
            // Every field or none: a QR missing any one of them is not an enrollment, and a
            // half-filled state would have this phone talking to the relay with nothing to say.
            val relay = Sanitize.relayUrl(params["relay"].orEmpty())
            val instance = params["instance"].orEmpty().takeIf(Sanitize::isId)
            val token = Sanitize.secret(params["token"].orEmpty())
            val key = Sanitize.secret(params["key"].orEmpty())
            val fields = listOf(relay, instance, token, key)
            if (query.isEmpty() || fields.any { it == null }) return null
            return CalendarSyncState(
                relayUrl = relay.orEmpty(),
                instanceId = instance.orEmpty(),
                deviceToken = token.orEmpty(),
                instanceKey = key.orEmpty(),
                deviceId = params["device"].orEmpty().takeIf(Sanitize::isId).orEmpty(),
            )
        }
    }
}
