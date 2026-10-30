package com.church.presenter.churchpresentermobile.tv

import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideEnvelope
import com.church.presenter.churchpresentermobile.model.SlideMessageType
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A TV showing a phone's slides: what it makes of each message, and how it
 * behaves when the phone comes and goes.
 *
 * The rules are the browser page's (`app.js`), so a TV and a browser on the
 * same phone must never disagree — and the failure worth catching is a black
 * room: the TV dropping the slide because the phone locked for a moment.
 */
class PhoneLiveStreamTest {

    private val json = Json { encodeDefaults = true }

    private fun message(
        rev: Long,
        body: String? = "Amazing grace",
        type: SlideMessageType = SlideMessageType.SLIDE,
        v: Int = 1,
    ) = json.encodeToString(
        SlideEnvelope.serializer(),
        SlideEnvelope(v = v, type = type, rev = rev, slide = body?.let { Slide(body = it) }),
    )

    // ── Addresses typed with a remote ────────────────────────────────────

    @Test
    fun `a bare address uses the phone's usual port`() {
        val source = parseAddress("192.168.1.20")

        assertEquals("192.168.1.20", source?.host)
        assertEquals(8766, source?.port)
        assertEquals("ws://192.168.1.20:8766/live", source?.liveUrl)
    }

    @Test
    fun `an address with a port keeps it`() {
        // The phone takes the next port when its usual one is busy.
        assertEquals(8767, parseAddress("192.168.1.20:8767")?.port)
    }

    @Test
    fun `an address copied from the phone's settings is accepted`() {
        // Settings → Display shows it as a link.
        assertEquals("192.168.1.20", parseAddress(" http://192.168.1.20:8766/ ")?.host)
    }

    @Test
    fun `anything that is not an address is refused before connecting`() {
        listOf("", "   ", "192.168.1.20:port", "192.168.1.20:70000", "192.168 .1.20").forEach {
            assertNull(parseAddress(it), "'$it'")
        }
    }

    // ── One message ──────────────────────────────────────────────────────

    @Test
    fun `a slide is shown and its revision remembered`() {
        val (outcome, rev) = readEnvelope(message(rev = 4), lastRev = 3)

        assertEquals("Amazing grace", assertIs<EnvelopeOutcome.Show>(outcome).slide.body)
        assertEquals(4, rev)
    }

    @Test
    fun `an older revision is a late repeat and is dropped`() {
        val (outcome, rev) = readEnvelope(message(rev = 2), lastRev = 5)

        assertEquals(EnvelopeOutcome.Ignore, outcome)
        assertEquals(5, rev)
    }

    @Test
    fun `a keep-alive changes nothing on screen`() {
        assertEquals(EnvelopeOutcome.Ignore, readEnvelope(message(rev = 1, type = SlideMessageType.PING), -1).first)
    }

    @Test
    fun `a clear with no slide blanks the screen`() {
        val outcome = readEnvelope(message(rev = 1, body = null, type = SlideMessageType.CLEAR), -1).first

        assertEquals(Slide.BLANK, assertIs<EnvelopeOutcome.Show>(outcome).slide)
    }

    @Test
    fun `the phone ending the presentation says so`() {
        assertEquals(EnvelopeOutcome.Ended, readEnvelope(message(rev = 1, type = SlideMessageType.BYE), -1).first)
    }

    @Test
    fun `a newer protocol is reported rather than half drawn`() {
        assertEquals(EnvelopeOutcome.TooNew, readEnvelope(message(rev = 1, v = 99), -1).first)
    }

    @Test
    fun `an unreadable message is ignored, never fatal`() {
        assertEquals(EnvelopeOutcome.Ignore, readEnvelope("not json", 3).first)
    }

    // ── Reconnecting ─────────────────────────────────────────────────────

    @Test
    fun `the wait between attempts doubles up to five seconds`() {
        assertEquals(1_000, nextReconnectDelay(RECONNECT_MIN_MS))
        assertEquals(RECONNECT_MAX_MS, nextReconnectDelay(4_000))
        assertEquals(RECONNECT_MAX_MS, nextReconnectDelay(RECONNECT_MAX_MS))
    }

    @Test
    fun `a dropped phone keeps its last slide on screen and is tried again`() = runTest {
        val source = PhoneSource("Pixel", "192.168.1.20", 8766)
        var attempts = 0
        val states = phoneLiveStates(source) { _, onText ->
            attempts++
            if (attempts == 1) onText(message(rev = 1, body = "Verse one"))
            error("Wi-Fi dropped")
        }.take(5).toList()

        assertEquals(LiveState.Connecting, states[0])
        assertEquals("Verse one", assertIs<LiveState.Showing>(states[1]).slide.body)
        // Lost, but still holding the verse rather than going black.
        assertEquals("Verse one", assertIs<LiveState.Lost>(states[2]).lastSlide?.body)
        assertIs<LiveState.Lost>(states[3])
        assertTrue(attempts >= 3, "kept trying: $attempts")
    }

    @Test
    fun `each new connection starts its revisions afresh`() = runTest {
        // The phone's counter restarts when its server does; a TV that kept
        // the old one would ignore every slide after the phone came back.
        val source = PhoneSource("Pixel", "192.168.1.20", 8766)
        var attempts = 0
        val states = phoneLiveStates(source) { _, onText ->
            attempts++
            onText(message(rev = if (attempts == 1) 9 else 1, body = "Attempt $attempts"))
            error("closed")
        }.take(4).toList()

        assertEquals("Attempt 2", assertIs<LiveState.Showing>(states[3]).slide.body)
    }
}
