package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.SlideDeck
import com.church.presenter.churchpresentermobile.model.SlideEnvelope
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.LocalVideo
import com.church.presenter.churchpresentermobile.model.VideoEndAction
import com.church.presenter.churchpresentermobile.model.VideoSound
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
    fun `clear takes the video down even after it was taken off-air`() = runVmTest {
        withHarness { h ->
            h.viewModel.import(picked("a.mp4"))
            runCurrent()
            h.viewModel.onAction(VideoAction.ToggleLive)
            // "On screen" tapped: the screen goes off-air with the video still loaded.
            h.viewModel.onAction(VideoAction.ToggleLive)
            h.viewModel.onAction(VideoAction.Clear)
            runCurrent()

            assertTrue(h.engine.deck.value.isEmpty, "the video must not stay loaded behind the screen")
        }
    }

    @Test
    fun `clear leaves a song alone when the video was never put up`() = runVmTest {
        withHarness { h ->
            h.viewModel.import(picked("a.mp4"))
            runCurrent()
            val song = Slide(kind = SlideKind.SONG, body = "x")
            h.engine.setDeck(SlideDeck(kind = SlideKind.SONG, slides = listOf(song)))

            h.viewModel.onAction(VideoAction.Clear)
            runCurrent()

            assertEquals(SlideKind.SONG, h.engine.currentSlide.value.kind)
        }
    }

    @Test
    fun `a player built now gets the position as of now`() = runVmTest {
        withHarness { h ->
            assertNull(h.viewModel.currentCue())
            h.viewModel.import(picked("a.mp4"))
            runCurrent()
            h.viewModel.onAction(VideoAction.TogglePlay)
            h.now += 30_000L
            // The flow still holds the cue from when Play was pressed; a preview
            // built from it a minute later started that far behind.
            assertEquals(0L, h.viewModel.previewCue.value?.positionMs)
            assertEquals(30_000L, h.viewModel.currentCue()?.positionMs)
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
    fun `the transport buttons reach the transport`() = runVmTest {
        withHarness { h ->
            h.viewModel.import(picked("a.mp4"))
            runCurrent()
            h.viewModel.onAction(VideoAction.SeekTo(50_000L))
            h.viewModel.onAction(VideoAction.Back10)
            assertEquals(40_000L, h.transport.positionMs())
            h.viewModel.onAction(VideoAction.Forward10)
            assertEquals(50_000L, h.transport.positionMs())
            h.viewModel.onAction(VideoAction.TogglePlay)
            h.viewModel.onAction(VideoAction.Stop)
            assertEquals(0L, h.transport.positionMs())
            assertFalse(h.transport.state.value.playing)
            h.viewModel.onAction(VideoAction.SetSound(VideoSound.OFF))
            h.viewModel.onAction(VideoAction.SetEndAction(VideoEndAction.HOLD))
            assertEquals(VideoEndAction.HOLD, h.transport.state.value.endAction)
        }
    }

    @Test
    fun `blank reaches the screen`() = runVmTest {
        withHarness { h ->
            h.viewModel.import(picked("a.mp4"))
            runCurrent()
            h.viewModel.onAction(VideoAction.ToggleLive)
            h.viewModel.onAction(VideoAction.ToggleBlank)
            assertTrue(h.engine.isBlank.value)
            // Live from blank brings it back rather than taking it off-air.
            h.viewModel.onAction(VideoAction.ToggleLive)
            assertFalse(h.engine.isBlank.value)
            assertTrue(h.engine.isLive.value)
        }
    }

    @Test
    fun `live and clear do nothing with no video loaded`() = runVmTest {
        withHarness { h ->
            h.viewModel.onAction(VideoAction.ToggleLive)
            h.viewModel.onAction(VideoAction.Clear)
            assertTrue(h.published.isEmpty())
        }
    }

    @Test
    fun `selecting the loaded video again does not reload it`() = runVmTest {
        withHarness { h ->
            h.viewModel.import(picked("a.mp4"))
            runCurrent()
            val seq = h.transport.state.value.seq
            h.viewModel.select(h.library.videos.value.single())
            assertEquals(seq, h.transport.state.value.seq)
        }
    }

    @Test
    fun `a video with nowhere to play from is reported`() = runVmTest {
        withHarness { h ->
            val stranded = VideoLibrary(null, InMemoryFileStorage(), newId = { "x" }, now = { 0L })
            val vm = LocalMediaViewModel(stranded, h.transport, h.engine)
            try {
                assertFalse(vm.isSupported)
                vm.select(LocalVideo("x", "x", "x.mp4"))
                assertNotNull(vm.message.value)
            } finally {
                tearDown(vm)
            }
        }
    }

    @Test
    fun `a second import while one is copying is ignored`() = runVmTest {
        withHarness { h ->
            h.viewModel.import(picked("a.mp4"))
            h.viewModel.import(picked("b.mp4"))
            runCurrent()
            assertEquals(listOf("a"), h.library.videos.value.map { it.title })
        }
    }

    @Test
    fun `deleting a video that is not loaded leaves the player alone`() = runVmTest {
        withHarness { h ->
            h.viewModel.import(picked("a.mp4"))
            runCurrent()
            h.viewModel.import(picked("b.mp4"))
            runCurrent()
            val other = h.library.videos.value.first { it.title == "a" }
            h.viewModel.delete(other)
            assertEquals("b", h.viewModel.transportState.value.video?.title)
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
