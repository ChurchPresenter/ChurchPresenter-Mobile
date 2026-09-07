package com.church.presenter.churchpresentermobile.present.sink

import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideBackdrop
import com.church.presenter.churchpresentermobile.model.SlideEnvelope
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.model.SlideMessageType
import com.church.presenter.churchpresentermobile.model.SlideTheme
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Frames going out to the browsers watching.
 *
 * Everything the display page draws arrives in this JSON, so the two things
 * worth holding are that a frame reaches the server at all, and that what
 * reaches it still carries the fields a page renders from. A slide whose theme
 * or backdrop was dropped on the way out looks, from the hall, exactly like a
 * theme that was never applied.
 */
class WebPageSinkRenderTest {

    private fun songSlide(body: String = "Amazing grace", theme: SlideTheme = SlideTheme()) =
        SlideEnvelope(rev = 3L, slide = Slide(kind = SlideKind.SONG, body = body, theme = theme))

    // ── Reaching the server ──────────────────────────────────────────────

    @Test
    fun `a frame reaches a serving sink`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.render(envelope("Amazing grace"))

        assertEquals(1, server.published.size)
    }

    @Test
    fun `each frame is published in turn`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.render(envelope("Verse one"))
        sink.render(envelope("Verse two"))

        assertEquals(2, server.published.size)
    }

    @Test
    fun `frames keep the order they were rendered in`() = runTest {
        // A page applies frames by revision, but publishing them out of order
        // would still show the wrong verse for a moment.
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.render(envelope("Verse one"))
        sink.render(envelope("Verse two"))

        assertTrue(server.published[0].contains("Verse one"))
        assertTrue(server.published[1].contains("Verse two"))
    }

    @Test
    fun `a frame before anything is serving publishes nothing`() = runTest {
        // There is no server yet; the frame is kept for whoever connects.
        val server = FakeDisplayServer()
        val sink = webSink(server = server)

        sink.render(envelope())

        assertTrue(server.published.isEmpty())
    }

    @Test
    fun `a frame before anything is serving is not a crash`() = runTest {
        val sink = webSink()

        sink.render(envelope())

        assertEquals(0, sink.status.value.clientCount)
    }

    @Test
    fun `a frame after detaching publishes nothing`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()
        sink.detach()

        sink.render(envelope())

        assertTrue(server.published.isEmpty())
    }

    @Test
    fun `a frame after detaching is still remembered for next time`() = runTest {
        // Unplugging the TV does not stop the service; the next display gets
        // whatever is showing.
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()
        sink.detach()

        sink.render(envelope("Verse three"))
        sink.attach()

        assertTrue(server.published.single().contains("Verse three"))
    }

    @Test
    fun `a server that fails mid-publish does not take the caller down`() = runTest {
        // Sinks are broadcast to in a loop; one throwing would stop the others
        // from ever seeing the slide.
        val server = FakeDisplayServer(failToPublish = IllegalStateException("socket closed"))
        val sink = webSink(server = server)
        sink.attach()

        sink.render(envelope())

        assertTrue(server.published.isEmpty())
    }

    @Test
    fun `a failed publish still leaves the sink attached`() = runTest {
        // One dropped frame is not a reason to tell the operator the screen is
        // gone.
        val server = FakeDisplayServer(failToPublish = IllegalStateException("socket closed"))
        val sink = webSink(server = server)
        sink.attach()

        sink.render(envelope())

        assertTrue(sink.status.value.isAttached)
    }

    @Test
    fun `a frame after a failed one still goes out`() = runTest {
        val server = FakeDisplayServer(failToPublish = IllegalStateException("socket closed"))
        val sink = webSink(server = server)
        sink.attach()
        sink.render(envelope("Dropped"))

        server.failToPublish = null
        sink.render(envelope("Delivered"))

        assertTrue(server.published.single().contains("Delivered"))
    }

    // ── What the page renders from ───────────────────────────────────────

    @Test
    fun `the words are in the frame`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.render(songSlide("Amazing grace, how sweet the sound"))

        assertTrue(server.published.single().contains("Amazing grace, how sweet the sound"))
    }

    @Test
    fun `the revision is in the frame`() = runTest {
        // A page drops any frame that is not newer than the last one applied,
        // so without this every display freezes on its first slide.
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.render(SlideEnvelope(rev = 42L, slide = Slide(body = "Amazing grace")))

        assertTrue(server.published.single().contains("42"))
    }

    @Test
    fun `the message type is in the frame`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.render(SlideEnvelope(type = SlideMessageType.CLEAR, rev = 2L))

        assertTrue(server.published.single().contains("CLEAR"))
    }

    @Test
    fun `the protocol version is in the frame`() = runTest {
        // A stale page reads this to refuse politely rather than misrender.
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.render(envelope())

        assertTrue(server.published.single().contains("\"v\""))
    }

    @Test
    fun `the slide kind is in the frame`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.render(SlideEnvelope(slide = Slide(kind = SlideKind.BIBLE, body = "In the beginning")))

        assertTrue(server.published.single().contains("BIBLE"))
    }

    @Test
    fun `the theme rides with the slide`() = runTest {
        // It is not sent separately, so a display that connects mid-song still
        // draws the church's own colours.
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.render(songSlide(theme = SlideTheme(textColor = "#FFEE00")))

        assertTrue(server.published.single().contains("#FFEE00"))
    }

    @Test
    fun `the gradient rides with the slide`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.render(songSlide(theme = SlideTheme(gradientTop = "#102030")))

        assertTrue(server.published.single().contains("#102030"))
    }

    @Test
    fun `the backdrop choice is in the frame`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.render(SlideEnvelope(slide = Slide(body = "x", backdrop = SlideBackdrop.BLACK)))

        assertTrue(server.published.single().contains("BLACK"))
    }

    @Test
    fun `a backdrop photo's address is in the frame`() = runTest {
        // The browser fetches it from this phone, so the address has to survive
        // the trip.
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.render(
            SlideEnvelope(
                slide = Slide(
                    body = "x",
                    backdrop = SlideBackdrop.IMAGE,
                    backdropUrl = "http://192.168.1.50:8080/photo/p1",
                )
            )
        )

        assertTrue(server.published.single().contains("http://192.168.1.50:8080/photo/p1"))
    }

    @Test
    fun `a web slide's page address is in the frame`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.render(
            SlideEnvelope(
                slide = Slide(kind = SlideKind.WEB, mediaUrl = "https://example.org/notices")
            )
        )

        assertTrue(server.published.single().contains("https://example.org/notices"))
    }

    @Test
    fun `a blanked slide says so in the frame`() = runTest {
        // The page hides its text on this flag; losing it leaves the words up
        // while the operator believes the screen is black.
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.render(SlideEnvelope(slide = Slide(body = "Amazing grace", isBlank = true)))

        assertTrue(server.published.single().contains("\"isBlank\":true"))
    }

    @Test
    fun `a slide held back says so in the frame`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.render(SlideEnvelope(slide = Slide(body = "Amazing grace", isLive = false)))

        assertTrue(server.published.single().contains("\"isLive\":false"))
    }

    @Test
    fun `the reference line is in the frame`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.render(SlideEnvelope(slide = Slide(body = "x", reference = "AMAZING GRACE · VERSE 2")))

        assertTrue(server.published.single().contains("AMAZING GRACE · VERSE 2"))
    }

    @Test
    fun `the slide's position in its deck is in the frame`() = runTest {
        // "2 of 5" on a display is read off these.
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.render(SlideEnvelope(slide = Slide(body = "x", index = 1, total = 5)))

        assertTrue(server.published.single().contains("\"total\":5"))
    }

    @Test
    fun `defaults are written out rather than left to the page`() = runTest {
        // Two renderers guessing at their own defaults is how the phone and the
        // browser end up looking different.
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.render(SlideEnvelope(slide = Slide(body = "x")))

        assertTrue(server.published.single().contains("\"textSize\""))
    }

    @Test
    fun `an empty frame is still valid JSON to publish`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.render(SlideEnvelope())

        assertTrue(server.published.single().startsWith("{"))
    }

    @Test
    fun `a frame with no slide at all is still published`() = runTest {
        // CLEAR carries no slide; a display still has to be told.
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.render(SlideEnvelope(type = SlideMessageType.CLEAR, rev = 9L, slide = null))

        assertEquals(1, server.published.size)
    }

    @Test
    fun `words with quotes in them survive being encoded`() = runTest {
        // A notice can contain anything the operator typed.
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.render(envelope("""He said "come in""""))

        assertTrue(server.published.single().contains("\\\"come in\\\""))
    }

    @Test
    fun `line breaks survive being encoded`() = runTest {
        // Line breaks are significant on a slide; losing them reflows the verse.
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.render(envelope("Line one\nLine two"))

        assertTrue(server.published.single().contains("\\n"))
    }

    @Test
    fun `accented words survive being encoded`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.render(envelope("Święty"))

        assertTrue(server.published.single().contains("Święty"))
    }

    @Test
    fun `the frame is one JSON object rather than a list`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()

        sink.render(envelope())

        val frame = server.published.single()
        assertTrue(frame.startsWith("{") && frame.endsWith("}"))
    }

    @Test
    fun `re-attaching replays the last frame rather than all of them`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()
        sink.render(envelope("Verse one"))
        sink.render(envelope("Verse two"))
        sink.detach()

        sink.attach()

        assertTrue(server.published.last().contains("Verse two"))
    }

    @Test
    fun `rendering after a re-attach reaches the new server`() = runTest {
        val server = FakeDisplayServer()
        val sink = webSink(server = server)
        sink.attach()
        sink.detach()
        sink.attach()

        sink.render(envelope("After the reconnect"))

        assertTrue(server.published.last().contains("After the reconnect"))
    }
}
