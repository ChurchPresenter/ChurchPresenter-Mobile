package com.church.presenter.churchpresentermobile.calendar.sync

import com.church.presenter.churchpresentermobile.model.PlannedService
import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val KEY_BYTES = 32
private const val NONCE_BYTES = 12
private const val TAG_BITS = 128
private const val MAX_BOX_CHARS = 256 * 1024

/**
 * Seals what goes to the relay and opens what comes back, under the instance key from enrollment.
 * Wire form, shared with the desktop's `Envelope`: base64 of `nonce(12) ‖ ciphertext ‖ tag(16)`
 * with `"<instanceId>/<recordId>"` as associated data.
 */
@OptIn(ExperimentalEncodingApi::class)
class Sealing private constructor(private val key: AES.GCM.Key, private val instanceId: String) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    suspend fun seal(service: PlannedService): SealedRecord {
        val plain = json.encodeToString(
            PlannedService.serializer(),
            service.copy(updatedAt = "", rev = 0L),
        ).encodeToByteArray()
        return SealedRecord(
            id = service.id,
            keepUntil = keepUntil(service.date),
            box = sealBytes(plain, service.id),
        )
    }

    suspend fun sealText(text: String, recordId: String): String = sealBytes(text.encodeToByteArray(), recordId)

    /** The service inside a record, with the relay's stamps, or null when it does not open cleanly. */
    suspend fun open(record: SealedRecord): PlannedService? {
        val bytes = openBytes(record.box, record.id) ?: return null
        val service = runCatching { json.decodeFromString(
            PlannedService.serializer(),
            bytes.decodeToString(),
        ) }.getOrNull()
            ?: return null
        if (service.id != record.id) return null
        return service.copy(updatedAt = record.updatedAt, rev = record.rev)
    }

    /** A songbook record from the relay, or null when it does not open cleanly. */
    suspend fun openCatalog(record: SealedRecord): CatalogRecord? {
        val bytes = openBytes(record.box, record.id) ?: return null
        return runCatching { json.decodeFromString(CatalogRecord.serializer(), bytes.decodeToString()) }.getOrNull()
    }

    suspend fun openPresets(box: String): PresetIndex? {
        val bytes = openBytes(box, PRESETS_RECORD) ?: return null
        return runCatching { json.decodeFromString(PresetIndex.serializer(), bytes.decodeToString()) }.getOrNull()
    }

    // The library draws the nonce itself and prepends it to the ciphertext.
    private suspend fun sealBytes(plaintext: ByteArray, recordId: String): String =
        Base64.encode(key.cipher(TAG_BITS.bits).encrypt(plaintext, aad(recordId)))

    private suspend fun openBytes(box: String, recordId: String): ByteArray? {
        if (box.isEmpty() || box.length > MAX_BOX_CHARS) return null
        val bytes = runCatching { Base64.decode(box) }.getOrNull() ?: return null
        if (bytes.size < NONCE_BYTES + TAG_BITS / Byte.SIZE_BITS) return null
        return runCatching { key.cipher(TAG_BITS.bits).decrypt(bytes, aad(recordId)) }.getOrNull()
    }

    private fun aad(recordId: String): ByteArray = "$instanceId/$recordId".encodeToByteArray()

    companion object {
        /** The key as the enrollment QR carries it: URL-safe base64, no padding, 32 bytes. */
        suspend fun fromEncodedKey(encoded: String, instanceId: String): Sealing? {
            val raw = runCatching { Base64.UrlSafe.decode(encoded.padEnd((encoded.length + 3) / 4 * 4, '=')) }.getOrNull()
                ?: return null
            if (raw.size != KEY_BYTES) return null
            val key = CryptographyProvider.Default.get(AES.GCM).keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, raw)
            return Sealing(key, instanceId)
        }

        private fun keepUntil(date: String): String =
            runCatching { LocalDate.parse(date).plus(RETENTION_DAYS, DateTimeUnit.DAY).toString() }
                .getOrDefault(date)

        private const val RETENTION_DAYS = 90
    }
}
