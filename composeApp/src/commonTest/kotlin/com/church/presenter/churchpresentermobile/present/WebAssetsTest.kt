package com.church.presenter.churchpresentermobile.present

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebAssetsTest {

    // ── Path normalization ───────────────────────────────────────────────

    @Test
    fun `the root path serves the display page`() {
        assertEquals("index.html", WebAssets.normalize("/"))
        assertEquals("index.html", WebAssets.normalize(""))
        assertEquals("index.html", WebAssets.normalize("/index.html"))
    }

    @Test
    fun `an asset resolves whether or not it is under a directory`() {
        assertEquals("app.js", WebAssets.normalize("/app.js"))
        assertEquals("app.js", WebAssets.normalize("/assets/app.js"))
        assertEquals("style.css", WebAssets.normalize("/static/css/style.css"))
    }

    /** A browser cache-buster must not 404. */
    @Test
    fun `query strings and fragments are stripped`() {
        assertEquals("app.js", WebAssets.normalize("/app.js?v=42"))
        assertEquals("style.css", WebAssets.normalize("/style.css#top"))
        assertEquals("index.html", WebAssets.normalize("/?token=abc"))
    }

    // ── Content types ────────────────────────────────────────────────────

    @Test
    fun `text assets are served with a charset so a TV browser does not guess`() {
        assertTrue(WebAssets.contentTypeFor("index.html").startsWith("text/html"))
        assertTrue(WebAssets.contentTypeFor("index.html").contains("charset=utf-8"))
        assertTrue(WebAssets.contentTypeFor("style.css").contains("charset=utf-8"))
        assertTrue(WebAssets.contentTypeFor("app.js").contains("charset=utf-8"))
    }

    @Test
    fun `a javascript asset is served as javascript`() {
        assertEquals("application/javascript; charset=utf-8", WebAssets.contentTypeFor("app.js"))
    }

    @Test
    fun `fonts and images have their real types`() {
        assertEquals("font/woff2", WebAssets.contentTypeFor("Inter.woff2"))
        assertEquals("image/png", WebAssets.contentTypeFor("logo.png"))
        assertEquals("image/svg+xml", WebAssets.contentTypeFor("mark.svg"))
    }

    @Test
    fun `an unknown extension falls back to a safe binary type`() {
        assertEquals("application/octet-stream", WebAssets.contentTypeFor("mystery.xyz"))
        assertEquals("application/octet-stream", WebAssets.contentTypeFor("noextension"))
    }

    // ── Lookup ───────────────────────────────────────────────────────────

    private fun assets() = WebAssets(
        mapOf(
            "index.html" to WebAsset(byteArrayOf(1), "text/html"),
            "app.js" to WebAsset(byteArrayOf(2), "application/javascript"),
        )
    )

    @Test
    fun `lookup resolves through normalization`() {
        val a = assets()
        assertEquals(1, a.forPath("/")?.bytes?.first())
        assertEquals(2, a.forPath("/assets/app.js?v=3")?.bytes?.first())
    }

    @Test
    fun `an unknown path returns null so the server can answer 404`() {
        assertNull(assets().forPath("/nope.js"))
    }

    @Test
    fun `an empty bundle is detectable so the sink can refuse to start`() {
        assertTrue(WebAssets(emptyMap()).isEmpty)
        assertTrue(!assets().isEmpty)
    }

    @Test
    fun `every bundled file name has a real content type`() {
        WebAssets.FILE_NAMES.forEach { name ->
            assertTrue(
                WebAssets.contentTypeFor(name) != "application/octet-stream",
                "$name would be served as an opaque download",
            )
        }
    }

    // ── Content types ────────────────────────────────────────────────────

    @Test
    fun eachBundledFileTypeGetsItsOwnContentType() {
        // A browser told the wrong type for a script or stylesheet refuses to run
        // it, and the display page comes up unstyled.
        assertEquals("text/html; charset=utf-8", WebAssets.contentTypeFor("index.html"))
        assertEquals("application/javascript; charset=utf-8", WebAssets.contentTypeFor("app.js"))
        assertEquals("text/css; charset=utf-8", WebAssets.contentTypeFor("style.css"))
        assertEquals("font/woff2", WebAssets.contentTypeFor("inter.woff2"))
        assertEquals("font/woff", WebAssets.contentTypeFor("inter.woff"))
        assertEquals("image/png", WebAssets.contentTypeFor("logo.png"))
        assertEquals("image/svg+xml", WebAssets.contentTypeFor("icon.svg"))
    }

    @Test
    fun bothJpegSpellingsAreRecognised() {
        assertEquals("image/jpeg", WebAssets.contentTypeFor("photo.jpg"))
        assertEquals("image/jpeg", WebAssets.contentTypeFor("photo.jpeg"))
    }

    @Test
    fun anUnknownOrMissingExtensionFallsBackToBytes() {
        assertEquals("application/octet-stream", WebAssets.contentTypeFor("data.bin"))
        assertEquals("application/octet-stream", WebAssets.contentTypeFor("LICENSE"))
        assertEquals("application/octet-stream", WebAssets.contentTypeFor(""))
    }

    @Test
    fun onlyTheLastExtensionDecidesTheType() {
        assertEquals("application/javascript; charset=utf-8", WebAssets.contentTypeFor("app.min.js"))
    }
}
