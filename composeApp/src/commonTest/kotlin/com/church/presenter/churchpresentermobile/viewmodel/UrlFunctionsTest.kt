package com.church.presenter.churchpresentermobile.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals

/** Tests the pure top-level URL helpers in MediaViewModel and WebViewModel. */
class UrlFunctionsTest {

    // ── mediaTitleFrom ───────────────────────────────────────────────────────

    @Test
    fun mediaTitlePrettifiesFileName() {
        assertEquals("My Cool Clip", mediaTitleFrom("http://x/videos/My_Cool-Clip.mp4?t=1"))
    }

    @Test
    fun mediaTitleStripsQueryHashAndTrailingSlash() {
        assertEquals("clip", mediaTitleFrom("https://h/clip.mp4#frag"))
        assertEquals("song", mediaTitleFrom("https://h/song.mp3?a=b&c=d"))
    }

    @Test
    fun mediaTitleKeepsInnerDots() {
        assertEquals("video.final", mediaTitleFrom("https://h/video.final.mp4"))
    }

    @Test
    fun mediaTitleFallsBackToDomainWhenNameBlank() {
        assertEquals("host", mediaTitleFrom("http://host/___"))
    }

    // ── mediaKindFrom ────────────────────────────────────────────────────────

    @Test
    fun mediaKindClassifiesAudioVideoAndOther() {
        assertEquals("Audio", mediaKindFrom("https://h/song.MP3?x=1"))
        assertEquals("Audio", mediaKindFrom("https://h/a.flac"))
        assertEquals("Video", mediaKindFrom("https://h/clip.webm"))
        assertEquals("Video", mediaKindFrom("https://h/movie.MKV"))
        assertEquals("Media", mediaKindFrom("https://h/file.xyz"))
        assertEquals("Media", mediaKindFrom("https://h/noextension"))
    }

    // ── normalizeUrl ─────────────────────────────────────────────────────────

    @Test
    fun normalizeAddsHttpsWhenSchemeMissing() {
        assertEquals("https://example.com", normalizeUrl("example.com"))
        assertEquals("https://example.com", normalizeUrl("  example.com  "))
    }

    @Test
    fun normalizeKeepsExistingScheme() {
        assertEquals("http://x.local", normalizeUrl("http://x.local"))
        assertEquals("https://x.local", normalizeUrl("https://x.local"))
    }

    @Test
    fun normalizeEmptyStaysEmpty() {
        assertEquals("", normalizeUrl(""))
        assertEquals("", normalizeUrl("   "))
    }

    // ── domainOf ─────────────────────────────────────────────────────────────

    @Test
    fun domainStripsSchemePathAndWww() {
        assertEquals("grace.org", domainOf("https://www.grace.org/notes/1"))
        assertEquals("grace.org", domainOf("grace.org"))
        assertEquals("grace.org", domainOf("http://grace.org"))
    }

    @Test
    fun domainRetainsPortSegment() {
        assertEquals("host:8080", domainOf("http://host:8080/a"))
    }
}
