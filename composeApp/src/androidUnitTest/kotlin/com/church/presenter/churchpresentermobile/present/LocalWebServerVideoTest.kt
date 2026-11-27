package com.church.presenter.churchpresentermobile.present

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Videos served by the phone's own server, over a real socket.
 *
 * A TV's player streams a video a range at a time, and AVPlayer refuses to play
 * at all from a server that answers a range with the whole file — so the status
 * codes and headers are the behaviour, not a detail of it.
 */
class LocalWebServerVideoTest {

    // Bigger than one 256 KB chunk, so a whole-file answer is written in pieces.
    private val clip = ByteArray(600_000) { (it % 251).toByte() }

    private val videos = VideoSource { name ->
        if (name != "v1.mp4") {
            null
        } else {
            ServedVideo(clip.size.toLong(), "video/mp4") { offset, length ->
                val start = offset.toInt()
                clip.copyOfRange(start, minOf(start + length, clip.size))
            }
        }
    }

    private val assets = WebAssets(mapOf("index.html" to WebAsset("<html/>".encodeToByteArray(), "text/html")))
    private val client = HttpClient(OkHttp)
    private val servers = mutableListOf<LocalWebServer>()

    @AfterTest
    fun stopEverything() = runBlocking {
        servers.forEach { it.stop() }
        client.close()
    }

    private suspend fun port(): Int {
        val server = LocalWebServer(assets, PhotoSource.NONE, videos).also { servers += it }
        return server.start(0, emptyList())
    }

    private fun live(block: suspend () -> Unit) = runBlocking { withTimeout(TEST_BUDGET_MS) { block() } }

    @Test
    fun `a whole video is served with its length and says it takes ranges`() = live {
        val response = client.get("http://127.0.0.1:${port()}/$VIDEO_ROUTE/v1.mp4")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("bytes", response.headers[HttpHeaders.AcceptRanges])
        assertTrue(response.headers[HttpHeaders.ContentType].orEmpty().startsWith("video/mp4"))
        assertEquals(clip.toList(), response.readRawBytes().toList())
    }

    @Test
    fun `a range is answered with exactly that range`() = live {
        val response = client.get("http://127.0.0.1:${port()}/$VIDEO_ROUTE/v1.mp4") {
            header(HttpHeaders.Range, "bytes=100-199")
        }

        assertEquals(HttpStatusCode.PartialContent, response.status)
        assertEquals("bytes 100-199/${clip.size}", response.headers[HttpHeaders.ContentRange])
        assertEquals(clip.copyOfRange(100, 200).toList(), response.readRawBytes().toList())
    }

    @Test
    fun `the two-byte probe AVPlayer opens with is honoured`() = live {
        val response = client.get("http://127.0.0.1:${port()}/$VIDEO_ROUTE/v1.mp4") {
            header(HttpHeaders.Range, "bytes=0-1")
        }

        assertEquals(HttpStatusCode.PartialContent, response.status)
        assertEquals(2, response.readRawBytes().size)
    }

    @Test
    fun `a range past the end is refused with the file's size`() = live {
        val response = client.get("http://127.0.0.1:${port()}/$VIDEO_ROUTE/v1.mp4") {
            header(HttpHeaders.Range, "bytes=${clip.size}-")
        }

        assertEquals(HttpStatusCode.RequestedRangeNotSatisfiable, response.status)
        assertEquals("bytes */${clip.size}", response.headers[HttpHeaders.ContentRange])
    }

    @Test
    fun `an unknown video is a 404, not the display page`() = live {
        val response = client.get("http://127.0.0.1:${port()}/$VIDEO_ROUTE/other.mp4")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    private companion object {
        const val TEST_BUDGET_MS = 20_000L
    }
}
