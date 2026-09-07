package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The notice and countdown payload the phone sends to the desktop.
 *
 * Twenty-one fields, nineteen of them defaulted. That is what lets an older
 * desktop accept a payload from a newer phone, but it is also how a countdown
 * loses its target time: a field that never reaches the wire is filled in by
 * the desktop's own default and the timer counts to the wrong moment, with
 * nothing on either side to say so.
 *
 * Every field is asserted on the encoded form rather than on the object,
 * because the object is not what crosses the wire.
 */
class AnnouncementPayloadTest {

    /** Matches the encoder in AnnouncementService: defaults must be written out. */
    private val json = Json { encodeDefaults = true }

    private fun encode(payload: AnnouncementItemPayload) =
        json.encodeToString(AnnouncementItemPayload.serializer(), payload)

    private fun notice() = AnnouncementItemPayload(id = "a1", announcementText = "Shared lunch today")

    // ── A plain notice ───────────────────────────────────────────────────

    @Test
    fun `a notice names itself as an announcement`() {
        // The desktop routes on this; anything else is dropped as unknown.
        assertEquals("announcement", notice().type)
        assertTrue(""""type":"announcement"""" in encode(notice()))
    }

    @Test
    fun `a notice carries its id and its words`() {
        val encoded = encode(notice())

        assertTrue(""""id":"a1"""" in encoded, encoded)
        assertTrue(""""announcementText":"Shared lunch today"""" in encoded, encoded)
    }

    @Test
    fun `a notice defaults to white on black`() {
        // The colours a hall projector shows most legibly, and the ones the
        // composer opens on.
        val payload = notice()

        assertEquals("#FFFFFF", payload.textColor)
        assertEquals("#000000", payload.backgroundColor)
    }

    @Test
    fun `chosen colours reach the wire`() {
        val encoded = encode(notice().copy(textColor = "#FFCC00", backgroundColor = "#101010"))

        assertTrue(""""textColor":"#FFCC00"""" in encoded, encoded)
        assertTrue(""""backgroundColor":"#101010"""" in encoded, encoded)
    }

    @Test
    fun `the text size and the way it arrives reach the wire`() {
        val payload = notice().copy(fontSize = 72, animationType = "FADE", animationDuration = 250)
        val encoded = encode(payload)

        assertTrue(""""fontSize":72""" in encoded, encoded)
        assertTrue(""""animationType":"FADE"""" in encoded, encoded)
        assertTrue(""""animationDuration":250""" in encoded, encoded)
    }

    @Test
    fun `a notice is not a timer unless it says so`() {
        // The desktop draws a clock face for a timer; a notice mistaken for one
        // would count down from nothing.
        assertFalse(notice().isTimer)
        assertTrue(""""isTimer":false""" in encode(notice()), encode(notice()))
    }

    // ── A countdown ──────────────────────────────────────────────────────

    private fun countdown() = notice().copy(
        isTimer = true,
        timerMode = "duration",
        timerHours = 1,
        timerMinutes = 30,
        timerSeconds = 15,
        timerExpiredText = "Starting now",
        timerTextColor = "#00FF00",
    )

    @Test
    fun `a countdown carries every part of its duration`() {
        // Losing any one of the three silently shortens the countdown.
        val encoded = encode(countdown())

        assertTrue(""""timerHours":1""" in encoded, encoded)
        assertTrue(""""timerMinutes":30""" in encoded, encoded)
        assertTrue(""""timerSeconds":15""" in encoded, encoded)
    }

    @Test
    fun `a countdown says what to show when it reaches zero`() {
        assertTrue(""""timerExpiredText":"Starting now"""" in encode(countdown()))
    }

    @Test
    fun `a countdown carries its own text colour`() {
        // Separate from the notice's, so a timer can be green over the same
        // background a notice is white on.
        assertEquals("#00FF00", countdown().timerTextColor)
        assertTrue(""""timerTextColor":"#00FF00"""" in encode(countdown()))
    }

    @Test
    fun `a count to a time of day carries that time`() {
        val payload = countdown().copy(
            timerMode = "target",
            targetHour = 10,
            targetMinute = 45,
            targetSecond = 30,
        )
        val encoded = encode(payload)

        assertTrue(""""timerMode":"target"""" in encoded, encoded)
        assertTrue(""""targetHour":10""" in encoded, encoded)
        assertTrue(""""targetMinute":45""" in encoded, encoded)
        assertTrue(""""targetSecond":30""" in encoded, encoded)
    }

    @Test
    fun `a live clock carries the format it should be drawn in`() {
        val encoded = encode(notice().copy(isTimer = true, timerMode = "clock", liveClockFormat = "HH:mm"))

        assertTrue(""""liveClockFormat":"HH:mm"""" in encoded, encoded)
    }

    @Test
    fun `a countdown defaults to counting a duration, not to a clock time`() {
        // "duration" is what the composer opens on; defaulting to a target time
        // would make an untouched timer count to midnight.
        assertEquals("duration", notice().timerMode)
        assertEquals("HH:mm:ss", notice().liveClockFormat)
    }

    // ── The schedule row's label ─────────────────────────────────────────

    @Test
    fun `the display text is what the schedule row shows`() {
        val encoded = encode(notice().copy(displayText = "Notice — Shared lunch"))

        assertTrue(""""displayText":"Notice — Shared lunch"""" in encoded, encoded)
    }

    @Test
    fun `a payload with no label still encodes the field`() {
        // Omitting it lets the desktop fall back to its own guess, which is how
        // a schedule row ends up reading "announcement".
        assertTrue(""""displayText":""""" in encode(notice()), encode(notice()))
    }

    // ── The whole thing ──────────────────────────────────────────────────

    @Test
    fun `every field a countdown was composed with survives the round trip`() {
        // The composer reopens from these; a field lost on the way out is one
        // the operator has to set again without being told why.
        val payload = countdown().copy(displayText = "Countdown", fontSize = 96)

        val back = json.decodeFromString(
            AnnouncementItemPayload.serializer(),
            encode(payload),
        )

        assertEquals(payload, back)
    }

    @Test
    fun `the request wrapper carries the item under the key the desktop reads`() {
        val encoded = json.encodeToString(AnnouncementRequest.serializer(), AnnouncementRequest(notice()))

        assertTrue(encoded.startsWith("""{"item":"""), encoded)
    }
}
