package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.SlideDeck
import com.church.presenter.churchpresentermobile.model.SlideEnvelope
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.VideoEndAction
import com.church.presenter.churchpresentermobile.network.PickedMediaFile
import com.church.presenter.churchpresentermobile.present.FakeVideoFiles
import com.church.presenter.churchpresentermobile.present.SinkRegistry
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.present.VideoLibrary
import com.church.presenter.churchpresentermobile.present.VideoTransport
import com.church.presenter.churchpresentermobile.present.cueFor
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import com.church.presenter.churchpresentermobile.testutil.tearDown
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalMediaViewModelTest {

    private class Harness {
        var now = 0L
        val published = mutableListOf<SlideEnvelope>()
        val engine = StandaloneEngine(MutableStateFlow(AppMode.STANDALONE), SinkRegistry()) { published += it }
        val files = FakeVideoFiles()
        var ids = 0
        val library = VideoLibrary(files, InMemoryFileStorage(), newId = { "v${++ids}" }, now = { 0L })
            .also { it.serveFrom("http://10.0.0.2:8766") }
        val transport = VideoTransport(clock = { now }).also {
            engine.videoCue = it::cueFor
            it.onChange = engine::refreshVideo
        }
        val viewModel = LocalMediaViewModel(library, transport, engine)
    }

    private fun picked(name: String) = PickedMediaFile(name, 8L) { channel, _ -> channel.writeFully(ByteArray(8)) }

    private suspend fun TestScope.withHarness(block: suspend TestScope.(Harness) -> Unit) {
        val h = Harness()
        try {
            block(h)
        } finally {
            tearDown(h.viewModel)
        }
    }

    @Test
    fun `an imported video is loaded into the player but not put on screen`() = runVmTest {
        withHarness { h ->
            h.viewModel.import(picked("Welcome.mp4"))
            runCurrent()

            assertEquals("Welcome", h.viewModel.transportState.value.video?.title)
            assertEquals(SlideKind.VIDEO, h.engine.deck.value.kind)
            assertTrue(h.published.isEmpty(), "loading is browsing, not projecting")
            assertEquals(LocalVideoStatus.READY, h.viewModel.status.value)
            assertNull(h.viewModel.importProgress.value)
        }
    }

    @Test
    fun `live puts the loaded video up, and pressing it again takes the screen off-air`() = runVmTest {
        withHarness { h ->
            h.viewModel.import(picked("a.mp4"))
            runCurrent()

            h.viewModel.onAction(VideoAction.ToggleLive)
            runCurrent()
            assertTrue(h.viewModel.isOnScreen.value)
            assertEquals(SlideKind.VIDEO, h.published.last().slide!!.kind)

            h.viewModel.onAction(VideoAction.ToggleLive)
            runCurrent()
            assertFalse(h.engine.isLive.value)
        }
    }

    @Test
    fun `live brings the video back after a song was opened`() = runVmTest {
        withHarness { h ->
            h.viewModel.import(picked("a.mp4"))
            runCurrent()
            val song = Slide(kind = SlideKind.SONG, body = "x")
            h.engine.loadDeck(SlideDeck(kind = SlideKind.SONG, slides = listOf(song)))

            h.viewModel.onAction(VideoAction.ToggleLive)
            runCurrent()
            assertEquals(SlideKind.VIDEO, h.published.last().slide!!.kind)
        }
    }

    @Test
    fun `status follows the screen and the transport`() = runVmTest {
        withHarness { h ->
            assertEquals(LocalVideoStatus.EMPTY, h.viewModel.status.value)
            h.viewModel.import(picked("a.mp4"))
            runCurrent()
            h.viewModel.onAction(VideoAction.TogglePlay)
            runCurrent()
            assertEquals(LocalVideoStatus.PREVIEWING, h.viewModel.status.value)
            h.viewModel.onAction(VideoAction.ToggleLive)
            runCurrent()
            assertEquals(LocalVideoStatus.PLAYING_LIVE, h.viewModel.status.value)
            h.viewModel.onAction(VideoAction.TogglePlay)
            runCurrent()
            assertEquals(LocalVideoStatus.PAUSED_LIVE, h.viewModel.status.value)
        }
    }

    @Test
    fun `transport buttons reach the screen as a cue`() = runVmTest {
        withHarness { h ->
            h.viewModel.import(picked("a.mp4"))
            runCurrent()
            h.viewModel.onAction(VideoAction.ToggleLive)
            h.viewModel.onAction(VideoAction.SeekTo(30_000L))
            h.viewModel.onAction(VideoAction.ToggleLoop)
            h.viewModel.onAction(VideoAction.SetVolume(0.4f))
            runCurrent()

            val cue = assertNotNull(h.published.last().slide!!.video)
            assertEquals(30_000L, cue.positionMs)
            assertEquals(VideoEndAction.LOOP, cue.endAction)
            assertEquals(0.4f, cue.volume)
        }
    }

    @Test
    fun `clear stops the video and takes it off the screen`() = runVmTest {
        withHarness { h ->
            h.viewModel.import(picked("a.mp4"))
            runCurrent()
            h.viewModel.onAction(VideoAction.ToggleLive)
            h.viewModel.onAction(VideoAction.TogglePlay)
            h.viewModel.onAction(VideoAction.Clear)
            runCurrent()

            assertFalse(h.viewModel.transportState.value.playing)
            assertTrue(h.engine.deck.value.isEmpty)
            assertFalse(h.viewModel.isOnScreen.value)
        }
    }

    @Test
    fun `deleting the video on screen clears the screen and empties the player`() = runVmTest {
        withHarness { h ->
            h.viewModel.import(picked("a.mp4"))
            runCurrent()
            h.viewModel.onAction(VideoAction.ToggleLive)
            runCurrent()

            h.viewModel.delete(h.library.videos.value.single())
            runCurrent()

            assertTrue(h.library.videos.value.isEmpty())
            assertFalse(h.viewModel.transportState.value.isLoaded)
            assertTrue(h.engine.deck.value.isEmpty)
        }
    }

    @Test
    fun `a failed import is reported, not swallowed`() = runVmTest {
        withHarness { h ->
            h.files.failImport = true
            h.viewModel.import(picked("a.mp4"))
            runCurrent()
            assertNotNull(h.viewModel.message.value)
            assertNull(h.viewModel.importProgress.value)
            h.viewModel.clearMessage()
            assertNull(h.viewModel.message.value)
        }
    }
}
