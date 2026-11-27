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
import com.church.presenter.churchpresentermobile.present.cueFor
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.ui.standalone.LocalMediaScreen
import com.church.presenter.churchpresentermobile.viewmodel.LocalMediaViewModel
import com.church.presenter.churchpresentermobile.viewmodel.VideoAction
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

    private class Fixture(seeded: List<Clip>) {
        var now = 0L
        val engine = StandaloneEngine(MutableStateFlow(AppMode.STANDALONE), SinkRegistry()) { }
        var ids = 0
        val library: VideoLibrary
        val transport = VideoTransport(clock = { now })
        val viewModel: LocalMediaViewModel

        init {
            engine.videoCue = transport::cueFor
            transport.onChange = engine::refreshVideo
            var index = 0
            val fake = FakeVideoFiles()
            val files = object : VideoFiles by fake {
                // The bytes stay tiny; the size the list shows is the clip's real one.
                override suspend fun importFrom(
                    file: PickedMediaFile,
                    name: String,
                    onProgress: (Long) -> Unit,
                ): Long {
                    fake.importFrom(PickedMediaFile(file.fileName, 0L) { _, _ -> }, name, onProgress)
                    return file.sizeBytes
                }

                override suspend fun probe(name: String, thumbName: String): VideoProbe? =
                    seeded.getOrNull(index++)?.probe
            }
            library = VideoLibrary(files, InMemoryFileStorage(), newId = { "v${++ids}" }, now = { 0L })
            library.serveFrom("http://192.168.1.20:8766")
            // Imported oldest first, so the list reads newest first as it does in the app.
            runBlocking {
                seeded.forEach { clip ->
                    library.import(PickedMediaFile(clip.name, clip.bytes) { _, _ -> })
                }
            }
            viewModel = LocalMediaViewModel(library, transport, engine)
        }

        fun content(twoPane: Boolean = false, phoneSoundAvailable: Boolean = true): @Composable () -> Unit = {
            LocalMediaScreen(
                library,
                transport,
                engine,
                twoPane = twoPane,
                phoneSoundAvailable = phoneSoundAvailable,
                providedViewModel = viewModel,
            )
        }
    }

    private class Clip(val name: String, val probe: VideoProbe, val bytes: Long)

    private val clips = listOf(
        Clip("Worship loop - violet.mp4", VideoProbe(30_000L, 1920, 1080), 38L * MB),
        Clip("Missions report - Kenya.mov", VideoProbe(134_000L, 3840, 2160), 840L * MB),
        Clip("Countdown - 5 min.mp4", VideoProbe(300_000L, 1920, 1080), 96L * MB),
        Clip("Welcome video.mp4", VideoProbe(180_000L, 1920, 1080), 212L * MB),
    )

    private companion object {
        const val MB = 1024L * 1024L
    }

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
    fun soundFollowsTheTv() {
        // An iPad with a TV on AirPlay: "This phone" is withdrawn and the note says why.
        val f = Fixture(clips)
        f.viewModel.select(f.library.videos.value.first())
        f.viewModel.onAction(VideoAction.ToggleLive)
        screenshot("local-media__sound-follows-tv", content = f.content(phoneSoundAvailable = false))
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
