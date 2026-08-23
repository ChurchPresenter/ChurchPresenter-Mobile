package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header

/**
 * Says who this device is on a request to the desktop.
 *
 * The id is what the desktop keys approval and blocking on; the name is what it
 * shows the operator while they decide. Both travel together because either one
 * alone is a worse prompt: an id nobody recognises, or a name that cannot be
 * remembered between services.
 *
 * One helper rather than a `header(...)` line per service, so every service
 * identifies the device the same way and a new one cannot quietly forget to.
 */
fun HttpRequestBuilder.identifyDevice(settings: AppSettings) {
    header(ApiConstants.DEVICE_ID_HEADER, settings.deviceId)
    // Nothing rather than an empty header: the desktop falls back to the id, and
    // a blank name would be a worse label than the UUID it replaced.
    settings.reportedDeviceName.takeIf { it.isNotBlank() }?.let { name ->
        header(ApiConstants.DEVICE_NAME_HEADER, encodeDeviceName(name))
    }
}

private const val FIRST_PRINTABLE_ASCII = 0x20
private const val LAST_PRINTABLE_ASCII = 0x7E
private const val HEX_DIGITS = "0123456789ABCDEF"
private const val NIBBLE_BITS = 4
private const val LOW_NIBBLE = 0x0F
private const val BYTE_MASK = 0xFF

/**
 * Makes a device name safe to put in an HTTP header, percent-encoding its UTF-8 bytes.
 *
 * A device name is whatever its owner typed into the OS, so outside English-speaking
 * churches it is usually not ASCII — and an HTTP header cannot carry that. OkHttp,
 * which is this app's Android engine, **refuses to build the request at all**: a
 * phone called "Серёжин Pixel" — or "José's iPhone", one accent is enough — would
 * throw on every call the app makes, not merely fail to be named. Darwin does send
 * it, and the desktop then reads the raw UTF-8 back as ISO-8859-1 and shows mojibake.
 *
 * Printable ASCII is left exactly as it is, so the common case stays readable in a
 * log and on the wire; `%` is escaped so decoding cannot mistake a real one for an
 * escape. The desktop undoes this (`decodeDeviceName`, `:companion-server`) and
 * treats a value with no `%` as already-decoded, so an older client that sends a
 * plain name still works and this can ship on either side first.
 */
internal fun encodeDeviceName(name: String): String {
    if (name.all { it.code in FIRST_PRINTABLE_ASCII..LAST_PRINTABLE_ASCII && it != '%' }) return name
    return buildString {
        for (byte in name.encodeToByteArray()) {
            val code = byte.toInt() and BYTE_MASK
            if (code in FIRST_PRINTABLE_ASCII..LAST_PRINTABLE_ASCII && code != '%'.code) {
                append(code.toChar())
            } else {
                append('%').append(HEX_DIGITS[code shr NIBBLE_BITS]).append(HEX_DIGITS[code and LOW_NIBBLE])
            }
        }
    }
}
