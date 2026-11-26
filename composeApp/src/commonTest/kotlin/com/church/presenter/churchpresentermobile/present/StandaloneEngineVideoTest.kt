package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.LocalVideo
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideDeck
import com.church.presenter.churchpresentermobile.model.SlideDeckBuilder
import com.church.presenter.churchpresentermobile.model.SlideEnvelope
import com.church.presenter.churchpresentermobile.model.SlideKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** How a video's transport reaches the screens through the engine. */
class StandaloneEngineVideoTest {

    private var now = 0L
    private val published = mutableListOf<SlideEnvelope>()
    private val engine = StandaloneEngine(MutableStateFlow(AppMode.STANDALONE), SinkRegistry()) { published += it }
    private val transport = VideoTransport(clock = { now }).also {
        engine.videoCue = it::cueFor
        it.onChange = engine::refreshVideo
    }
    private val video = LocalVideo(id = "v1", title = "Welcome", fileName = "v1.mp4", durationMs = 60_000L)
    private val url = "http://10.0.0.2:8766/video/v1.mp4"

    private fun loadVideo() {
        engine.loadDeck(SlideDeckBuilder.fromLocalVideo(url, video.title, video.id))
        transport.load(video, url)
    }

    @Test
    fun `loading a video puts nothing on the screen`() {
        loadVideo()
        transport.play()
        assertTrue(published.isEmpty(), "play before Live is a preview on the phone only")
    }

    @Test
    fun `going live sends the video with where it has got to`() {
        loadVideo()
        transport.play()
        now += 7_000L
        engine.goLive()

        val cue = assertNotNull(published.last().slide!!.video)
        assertTrue(cue.playing)
        assertEquals(7_000L, cue.positionMs)
        assertEquals(url, published.last().slide!!.mediaUrl)
    }

    @Test
    fun `transport changes reach the screen while the video is up`() {
        loadVideo()
        engine.goLive()
        val before = published.size
        transport.play()
        transport.setVolume(0.3f)
        assertEquals(before + 2, published.size)
        assertEquals(0.3f, published.last().slide!!.video?.volume)
    }

    @Test
    fun `transport changes never put a browsed song up`() {
        loadVideo()
        engine.goLive()
        val song = Slide(kind = SlideKind.SONG, body = "words")
        engine.loadDeck(SlideDeck(kind = SlideKind.SONG, title = "Song", slides = listOf(song)))
        val before = published.size
        transport.pause()
        assertEquals(before, published.size)
        assertEquals(SlideKind.VIDEO, engine.currentSlide.value.kind)
    }

    @Test
    fun `a video slide the transport did not load has no cue`() {
        engine.setDeck(SlideDeckBuilder.fromLocalVideo("http://elsewhere/clip.mp4", "Other", "x"))
        assertNull(published.last().slide!!.video)
    }

    @Test
    fun `slides that are not video never carry a cue`() {
        loadVideo()
        engine.setDeck(SlideDeck(kind = SlideKind.SONG, slides = listOf(Slide(kind = SlideKind.SONG, body = "x"))))
        assertNull(published.last().slide!!.video)
    }

    @Test
    fun `unblanking lands the screen where the video is now`() {
        loadVideo()
        engine.goLive()
        transport.play()
        engine.setBlank(true)
        now += 20_000L
        engine.setBlank(false)
        assertEquals(20_000L, published.last().slide!!.video?.positionMs)
    }

    @Test
    fun `the notification never sees the moving position`() {
        loadVideo()
        engine.goLive()
        val showing = nowShowing(engine.deck.value.title, engine.currentSlide.value)
        assertNull(showing?.slide?.video)
    }

    @Test
    fun `a local file address is accepted for a video stored on the phone, and nothing else is`() {
        assertEquals(1, SlideDeckBuilder.fromLocalVideo("file:///data/v.mp4", "v", "id").slides.size)
        assertTrue(SlideDeckBuilder.fromLocalVideo("javascript:alert(1)", "v", "id").isEmpty)
    }
}
