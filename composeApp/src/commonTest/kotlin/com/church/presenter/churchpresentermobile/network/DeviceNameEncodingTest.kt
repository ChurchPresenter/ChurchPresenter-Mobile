package com.church.presenter.churchpresentermobile.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Getting a device's name into an HTTP header intact.
 *
 * This is not a nicety. Android runs on OkHttp, which refuses to build a request whose header
 * value holds any character outside printable ASCII — so before this encoding, a phone whose
 * owner had named it in Cyrillic (or with one accented letter) failed **every** call the app
 * made: songs, Bible, schedule, Q&A and the live socket, all with no crash to point at.
 *
 * The desktop's `decodeDeviceName` is the other half of this; the bytes below are the exact
 * ones the two sides agree on.
 */
class DeviceNameEncodingTest {

    @Test
    fun anAsciiNameIsLeftExactlyAsItIs() {
        // The common case stays readable on the wire and in a log.
        assertEquals("Sound desk iPad", encodeDeviceName("Sound desk iPad"))
    }

    @Test
    fun aCyrillicNameSurvivesAsPercentEncodedUtf8() {
        assertEquals(
            "%D0%A1%D0%B5%D1%80%D1%91%D0%B6%D0%B8%D0%BD Pixel",
            encodeDeviceName("Серёжин Pixel"),
        )
    }

    @Test
    fun oneAccentedLetterIsEnoughToNeedEncoding() {
        // The case that makes this more than a Cyrillic problem: OkHttp rejects 0xE9 too.
        assertEquals("Jos%C3%A9's iPhone", encodeDeviceName("José's iPhone"))
    }

    @Test
    fun anEmojiNameIsEncodedAsItsFourUtf8Bytes() {
        assertEquals("Sound desk %F0%9F%8E%9B", encodeDeviceName("Sound desk 🎛"))
    }

    @Test
    fun aPercentSignIsEscapedSoItCannotBeReadBackAsAnEscape() {
        assertEquals("100%25 volume", encodeDeviceName("100% volume"))
    }

    @Test
    fun everyByteSentIsOneAnHttpHeaderCanCarry() {
        // The property that matters, stated directly rather than inferred from the cases above.
        val names = listOf(
            "Серёжин Pixel", "José's iPhone", "Sound desk 🎛", "100% volume", "Ω", "普通话平板",
        )
        for (name in names) {
            val encoded = encodeDeviceName(name)
            assertTrue(
                encoded.all { it.code in PRINTABLE_ASCII },
                "$name encoded to a value OkHttp would refuse: $encoded",
            )
        }
    }

    @Test
    fun anEmptyNameEncodesToNothingAtAll() {
        // Callers omit the header entirely when blank; this must not invent anything.
        assertEquals("", encodeDeviceName(""))
    }

    private companion object {
        val PRINTABLE_ASCII = 0x20..0x7E
    }
}
