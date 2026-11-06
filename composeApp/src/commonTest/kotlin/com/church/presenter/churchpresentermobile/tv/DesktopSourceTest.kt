package com.church.presenter.churchpresentermobile.tv

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What a TV makes of a desktop: the header on each picture, the address of each
 * output, and which outputs exist. The format is the desktop's
 * (`COMPANION_API.md`, "Browser Source"), so a wrong byte here shows as a picture
 * in the wrong place on the church's TV.
 */
class DesktopSourceTest {

    private val desktop = TvSource("Sanctuary PC", "192.168.1.10", 8765, SourceKind.DESKTOP)

    /** A message: six big-endian numbers, then one byte standing in for the picture. */
    private fun message(vararg numbers: Int): ByteArray =
        numbers.flatMap { n ->
            listOf(n ushr 24, n ushr 16, n ushr 8, n).map { it.toByte() }
        }.toByteArray() + byteArrayOf(1)

    @Test
    fun `the header's six numbers are read in order`() {
        assertEquals(
            FrameHeader(x = 10, y = 20, rectWidth = 300, rectHeight = 200, fullWidth = 1920, fullHeight = 1080),
            parseFrameHeader(message(10, 20, 300, 200, 1920, 1080)),
        )
    }

    @Test
    fun `numbers over a byte are read big-endian`() {
        assertEquals(1920, assertNotNull(parseFrameHeader(message(0, 0, 1920, 1080, 1920, 1080))).rectWidth)
    }

    @Test
    fun `a message with no picture after its header is refused`() {
        assertNull(parseFrameHeader(ByteArray(FRAME_HEADER_BYTES)))
    }

    @Test
    fun `a rectangle outside the output is refused`() {
        // Painting it would throw, on the TV, in the middle of a service.
        assertNull(parseFrameHeader(message(1900, 0, 100, 100, 1920, 1080)))
    }

    @Test
    fun `an output of no size is refused`() {
        assertNull(parseFrameHeader(message(0, 0, 0, 0, 0, 0)))
    }

    @Test
    fun `a desktop's address names its output`() {
        assertEquals("ws://192.168.1.10:8765/api/browser-source/2/ws", desktop.copy(output = 2).liveUrl)
    }

    @Test
    fun `an output behind a key carries it`() {
        assertTrue(desktop.copy(apiKey = "secret").liveUrl.endsWith("?apiKey=secret"))
    }

    @Test
    fun `a phone's address is its live socket`() {
        assertEquals(
            "ws://192.168.1.20:8766/live",
            TvSource("Pixel", "192.168.1.20", 8766, SourceKind.PHONE).liveUrl,
        )
    }

    @Test
    fun `outputs are found by what their pages answer`() = runTest {
        val answers = mapOf(1 to 200, 2 to 404, 3 to 401)

        val outputs = findOutputs(desktop) { url -> answers[url.substringAfterLast('/').toInt()] }

        assertEquals(listOf(DesktopOutput(1, needsKey = false), DesktopOutput(3, needsKey = true)), outputs)
    }

    @Test
    fun `a desktop that cannot be reached has no outputs`() = runTest {
        assertTrue(findOutputs(desktop) { null }.isEmpty())
    }

    @Test
    fun `a show request is read back into a source`() {
        val body = """{"name":"Sanctuary PC","host":"192.168.1.10","port":8765,"kind":"DESKTOP","output":2}"""

        assertEquals(desktop.copy(output = 2), readShowRequest(body))
    }

    @Test
    fun `a show request a TV should not act on is refused`() {
        listOf(
            "not json",
            """{"name":"x","host":"","port":8765}""",
            """{"name":"x","host":"192.168.1.10","port":0}""",
        ).forEach { assertNull(readShowRequest(it), it) }
    }
}
