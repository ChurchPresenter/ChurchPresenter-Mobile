package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.runtime.Composable
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.VideoEndAction
import com.church.presenter.churchpresentermobile.network.PickedMediaFile
import com.church.presenter.churchpresentermobile.present.FakeVideoFiles
import com.church.presenter.churchpresentermobile.present.SinkRegistry
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.present.VideoFiles
import com.church.presenter.churchpresentermobile.present.VideoLibrary
import com.church.presenter.churchpresentermobile.present.VideoProbe
import com.church.presenter.churchpresentermobile.present.VideoTransport
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.ui.standalone.LocalMediaScreen
import com.church.presenter.churchpresentermobile.viewmodel.LocalMediaViewModel
import com.church.presenter.churchpresentermobile.viewmodel.VideoAction
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

/**
 * The standalone Media tab, over a real [LocalMediaViewModel], transport and
 * engine, with the videos held in memory.
 *
 * The states are the ones the operator moves through: nothing stored yet, a
 * library with one clip loaded and waiting, and that clip up on the hall screen
 * and playing on a loop. The clock is fixed, so the scrubber sits at the same
 * place in every capture.
 */
class LocalMediaScreenshotTest {

    private class Fixture(seeded: List<Pair<String, VideoProbe>>) {
        var now = 0L
        val engine = StandaloneEngine(MutableStateFlow(AppMode.STANDALONE), SinkRegistry()) { }
        var ids = 0
        val library: VideoLibrary
        val transport = VideoTransport(clock = { now })
        val viewModel: LocalMediaViewModel

        init {
            engine.videoCue = { null }
            transport.onChange = engine::refreshVideo
            var index = 0
            val files = object : VideoFiles by FakeVideoFiles() {
                override suspend fun probe(name: String, thumbName: String): VideoProbe? =
                    seeded.getOrNull(index++)?.second
            }
            library = VideoLibrary(files, InMemoryFileStorage(), newId = { "v${++ids}" }, now = { 0L })
            library.serveFrom("http://192.168.1.20:8766")
            // Imported oldest first, so the list reads newest first as it does in the app.
            runBlocking {
                seeded.forEach { (name, _) ->
                    library.import(PickedMediaFile(name, 64L) { channel, _ -> channel.writeFully(ByteArray(64)) })
                }
            }
            viewModel = LocalMediaViewModel(library, transport, engine)
        }

        fun content(twoPane: Boolean = false): @Composable () -> Unit = {
            LocalMediaScreen(library, transport, engine, twoPane = twoPane, providedViewModel = viewModel)
        }
    }

    private val clips = listOf(
        "Worship loop - violet.mp4" to VideoProbe(30_000L, 1920, 1080),
        "Missions report - Kenya.mov" to VideoProbe(134_000L, 3840, 2160),
        "Countdown - 5 min.mp4" to VideoProbe(300_000L, 1920, 1080),
        "Welcome video.mp4" to VideoProbe(180_000L, 1920, 1080),
    )

    @Test
    fun empty() = screenshot("local-media__empty", content = Fixture(emptyList()).content())

    @Test
    fun loadedNotOnScreen() {
        val f = Fixture(clips)
        f.viewModel.select(f.library.videos.value.first())
        f.viewModel.onAction(VideoAction.SeekTo(42_000L))
        screenshot("local-media__loaded", content = f.content())
    }

    @Test
    fun liveAndLooping() {
        val f = Fixture(clips)
        f.viewModel.select(f.library.videos.value.last())
        f.viewModel.onAction(VideoAction.SetEndAction(VideoEndAction.LOOP))
        f.viewModel.onAction(VideoAction.SeekTo(12_000L))
        f.viewModel.onAction(VideoAction.ToggleLive)
        screenshot("local-media__live-looping", content = f.content())
    }

    @Test
    fun tablet() {
        val f = Fixture(clips)
        f.viewModel.select(f.library.videos.value.first())
        f.viewModel.onAction(VideoAction.ToggleLive)
        screenshot(
            name = "local-media-tablet__live",
            width = null,
            surface = Screenshots.TABLET_SURFACE,
            content = f.content(twoPane = true),
        )
    }
}
