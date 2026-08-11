package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SlideTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ── Slide ────────────────────────────────────────────────────────────

    @Test
    fun `default slide is blank and hidden`() {
        val slide = Slide()
        assertEquals(SlideKind.BLANK, slide.kind)
        assertEquals("", slide.body)
        assertNull(slide.reference)
        assertEquals(SlideTextSize.MEDIUM, slide.textSize)
        assertEquals(SlideBackdrop.GRADIENT, slide.backdrop)
        assertTrue(slide.isLive)
        assertFalse(slide.isBlank)
    }

    @Test
    fun `isHidden is true when blanked`() {
        assertTrue(Slide(body = "text", isBlank = true).isHidden)
    }

    @Test
    fun `isHidden is true when not live`() {
        assertTrue(Slide(body = "text", isLive = false).isHidden)
    }

    @Test
    fun `isHidden is false when live and not blanked`() {
        assertFalse(Slide(body = "text").isHidden)
    }

    @Test
    fun `Slide BLANK constant is blanked`() {
        assertTrue(Slide.BLANK.isBlank)
        assertTrue(Slide.BLANK.isHidden)
        assertEquals(SlideKind.BLANK, Slide.BLANK.kind)
    }

    @Test
    fun `slide survives a JSON round trip`() {
        val original = Slide(
            kind = SlideKind.BIBLE,
            body = "For God so loved the world,\nthat he gave his only begotten Son",
            reference = "JOHN 3:16",
            footer = "KJV",
            textSize = SlideTextSize.LARGE,
            backdrop = SlideBackdrop.IMAGE,
            backdropUrl = "http://192.168.1.44:8766/assets/bg.jpg",
            isBlank = true,
            isLive = false,
            theme = SlideTheme(font = SlideFont.SANS, brandLine = "Grace Community Church"),
            sourceId = "John:3:16",
            index = 2,
            total = 4,
        )
        assertEquals(original, json.decodeFromString<Slide>(json.encodeToString(original)))
    }

    @Test
    fun `newlines in the body are preserved across serialization`() {
        val slide = Slide(body = "line one\nline two\n\nline four")
        val decoded = json.decodeFromString<Slide>(json.encodeToString(slide))
        assertEquals(4, decoded.body.split("\n").size)
        assertEquals(slide.body, decoded.body)
    }

    @Test
    fun `unknown fields from a newer sender are ignored`() {
        val decoded = json.decodeFromString<Slide>(
            """{"kind":"SONG","body":"Amazing grace","transition":"crossfade","opacity":0.5}"""
        )
        assertEquals(SlideKind.SONG, decoded.kind)
        assertEquals("Amazing grace", decoded.body)
    }

    @Test
    fun `missing fields fall back to defaults`() {
        val decoded = json.decodeFromString<Slide>("""{"body":"just a body"}""")
        assertEquals("just a body", decoded.body)
        assertEquals(SlideKind.BLANK, decoded.kind)
        assertEquals(SlideTheme(), decoded.theme)
    }

    // ── SlideDeck ────────────────────────────────────────────────────────

    @Test
    fun `empty deck reports empty and yields no slide`() {
        assertTrue(SlideDeck.EMPTY.isEmpty)
        assertNull(SlideDeck.EMPTY.slideAt(0))
    }

    @Test
    fun `clampIndex on an empty deck is zero`() {
        assertEquals(0, SlideDeck.EMPTY.clampIndex(7))
        assertEquals(0, SlideDeck.EMPTY.clampIndex(-3))
    }

    @Test
    fun `clampIndex holds the index inside the deck`() {
        val deck = deckOf(3)
        assertEquals(0, deck.clampIndex(-1))
        assertEquals(0, deck.clampIndex(0))
        assertEquals(2, deck.clampIndex(2))
        assertEquals(2, deck.clampIndex(99))
    }

    @Test
    fun `slideAt returns null outside the deck`() {
        val deck = deckOf(2)
        assertEquals("s0", deck.slideAt(0)?.body)
        assertNull(deck.slideAt(2))
        assertNull(deck.slideAt(-1))
    }

    @Test
    fun `deck survives a JSON round trip`() {
        val deck = deckOf(3).copy(kind = SlideKind.SONG, title = "Sections — 42 Amazing Grace")
        assertEquals(deck, json.decodeFromString<SlideDeck>(json.encodeToString(deck)))
    }

    // ── SlideEnvelope ────────────────────────────────────────────────────

    @Test
    fun `envelope defaults to the current protocol version`() {
        assertEquals(SLIDE_PROTOCOL_VERSION, SlideEnvelope().v)
        assertEquals(SlideMessageType.SLIDE, SlideEnvelope().type)
    }

    @Test
    fun `INITIAL envelope clears to a blank slide at revision zero`() {
        assertEquals(SlideMessageType.CLEAR, SlideEnvelope.INITIAL.type)
        assertEquals(0L, SlideEnvelope.INITIAL.rev)
        assertTrue(SlideEnvelope.INITIAL.slide?.isBlank == true)
    }

    @Test
    fun `envelope survives a JSON round trip`() {
        val envelope = SlideEnvelope(
            type = SlideMessageType.SLIDE,
            rev = 42L,
            slide = Slide(kind = SlideKind.SONG, body = "verse", index = 1, total = 4),
        )
        assertEquals(envelope, json.decodeFromString<SlideEnvelope>(json.encodeToString(envelope)))
    }

    @Test
    fun `an envelope with no slide decodes cleanly`() {
        val decoded = json.decodeFromString<SlideEnvelope>("""{"type":"PING","rev":9}""")
        assertEquals(SlideMessageType.PING, decoded.type)
        assertEquals(9L, decoded.rev)
        assertNull(decoded.slide)
    }

    private fun deckOf(count: Int) = SlideDeck(
        kind = SlideKind.SONG,
        title = "deck",
        slides = List(count) { Slide(kind = SlideKind.SONG, body = "s$it", index = it, total = count) },
    )
}
