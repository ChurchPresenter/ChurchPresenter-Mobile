package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the media wire payloads and the playback snapshot pushed over the socket.
 *
 * The desktop reads these fields by name and infers a `MediaItem` from the
 * presence of `mediaUrl`, so the key names are a contract: renaming one stops
 * the item projecting rather than failing loudly.
 */
class MediaTest {

    /** The encoder the services use — `encodeDefaults` is what keeps `type` on the wire. */
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    // ── MediaItemPayload ─────────────────────────────────────────────────

    @Test
    fun `a request serialises to the shape the desktop reads`() {
        val request = MediaRequest(
            MediaItemPayload(
                id = "m1",
                mediaUrl = "https://example.org/clip.mp4",
                mediaTitle = "Clip",
                mediaType = "url",
                displayText = "Clip",
            ),
        )

        val item = json.parseToJsonElement(json.encodeToString(request)).jsonObject["item"]!!.jsonObject

        assertEquals("media", item["type"]?.jsonPrimitive?.content)
        assertEquals("m1", item["id"]?.jsonPrimitive?.content)
        assertEquals("https://example.org/clip.mp4", item["mediaUrl"]?.jsonPrimitive?.content)
        assertEquals("Clip", item["mediaTitle"]?.jsonPrimitive?.content)
        assertEquals("url", item["mediaType"]?.jsonPrimitive?.content)
    }

    @Test
    fun `defaults are a blank id for the server to fill and a url-typed item`() {
        val payload = MediaItemPayload(mediaUrl = "https://example.org/clip.mp4")

        assertEquals("", payload.id)
        assertEquals("media", payload.type)
        assertEquals("url", payload.mediaType)
        assertEquals("", payload.mediaTitle)
    }

    @Test
    fun `only the media url is required to decode`() {
        val payload = json.decodeFromString<MediaItemPayload>("""{"mediaUrl":"file:///clip.mp4"}""")

        assertEquals("file:///clip.mp4", payload.mediaUrl)
        assertEquals("url", payload.mediaType)
    }

    @Test
    fun `each desktop media type survives a round-trip`() {
        // "url", "local" and "audio" are the desktop's own Constants.MEDIA_TYPE_* values.
        for (type in listOf("url", "local", "audio")) {
            val payload = MediaItemPayload(mediaUrl = "x", mediaType = type)
            assertEquals(payload, json.decodeFromString<MediaItemPayload>(json.encodeToString(payload)))
        }
    }

    @Test
    fun `a request round-trips`() {
        val request = MediaRequest(MediaItemPayload(id = "m1", mediaUrl = "x", mediaTitle = "T"))

        assertEquals(request, json.decodeFromString<MediaRequest>(json.encodeToString(request)))
    }

    // ── MediaUploadResponse ──────────────────────────────────────────────

    @Test
    fun `an upload response defaults to not-ok, so a silent body is not read as success`() {
        val response = MediaUploadResponse()

        assertFalse(response.ok)
        assertEquals("", response.path)
        assertEquals("local", response.mediaType)
    }

    @Test
    fun `an upload response carries the desktop's own file path`() {
        val body = """{"ok":true,"path":"/Users/av/Movies/clip.mp4","name":"clip.mp4","mediaType":"local"}"""

        val response = json.decodeFromString<MediaUploadResponse>(body)

        assertTrue(response.ok)
        assertEquals("/Users/av/Movies/clip.mp4", response.path)
        assertEquals("clip.mp4", response.name)
    }

    // ── MediaPlaybackState ───────────────────────────────────────────────

    @Test
    fun `a fresh playback state is idle`() {
        val state = MediaPlaybackState()

        assertFalse(state.isLive)
        assertFalse(state.isLoaded)
        assertFalse(state.isPlaying)
        assertEquals(0L, state.positionMs)
        assertEquals(0L, state.durationMs)
        assertEquals(1f, state.volume)
        assertFalse(state.muted)
        assertEquals("", state.source)
    }

    @Test
    fun `a pushed playback state decodes every field`() {
        val body = """
            {"isLive":true,"isLoaded":true,"isPlaying":true,"title":"Sermon clip",
             "positionMs":12500,"durationMs":180000,"volume":0.5,"muted":false,
             "mediaType":"local","source":"/Users/av/Movies/clip.mp4"}
        """.trimIndent()

        val state = json.decodeFromString<MediaPlaybackState>(body)

        assertTrue(state.isLive)
        assertTrue(state.isPlaying)
        assertEquals("Sermon clip", state.title)
        assertEquals(12_500L, state.positionMs)
        assertEquals(180_000L, state.durationMs)
        assertEquals(0.5f, state.volume)
        assertEquals("/Users/av/Movies/clip.mp4", state.source)
    }

    @Test
    fun `a partial push leaves the rest at their defaults`() {
        // The desktop sends what changed; missing fields must not fail the decode.
        val state = json.decodeFromString<MediaPlaybackState>("""{"isPlaying":true}""")

        assertTrue(state.isPlaying)
        assertFalse(state.isLoaded)
        assertEquals(1f, state.volume)
        assertEquals("", state.title)
    }

    @Test
    fun `a playback state round-trips`() {
        val state = MediaPlaybackState(
            isLive = true,
            isLoaded = true,
            isPlaying = false,
            title = "Clip",
            positionMs = 1_000L,
            durationMs = 2_000L,
            volume = 0.25f,
            muted = true,
            mediaType = "audio",
            source = "https://example.org/a.mp3",
        )

        assertEquals(state, json.decodeFromString<MediaPlaybackState>(json.encodeToString(state)))
    }

    @Test
    fun `loaded but paused is distinguishable from nothing loaded`() {
        // The transport bar reads these separately: one shows a play button, the
        // other shows no bar at all.
        val paused = MediaPlaybackState(isLoaded = true, isPlaying = false, durationMs = 5_000L)
        val nothing = MediaPlaybackState()

        assertTrue(paused.isLoaded)
        assertFalse(paused.isPlaying)
        assertFalse(nothing.isLoaded)
    }
}
