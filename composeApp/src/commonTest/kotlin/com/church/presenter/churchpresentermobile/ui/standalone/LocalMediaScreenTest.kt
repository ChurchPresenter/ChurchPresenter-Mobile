package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.model.VideoEndAction
import com.church.presenter.churchpresentermobile.model.VideoSound
import com.church.presenter.churchpresentermobile.model.LocalVideo
import com.church.presenter.churchpresentermobile.present.FakeVideoFiles
import com.church.presenter.churchpresentermobile.present.SinkRegistry
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.present.VIDEO_INDEX_FILE
import com.church.presenter.churchpresentermobile.present.VideoLibrary
import com.church.presenter.churchpresentermobile.present.VideoTransport
import com.church.presenter.churchpresentermobile.present.cueFor
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import com.church.presenter.churchpresentermobile.viewmodel.LocalMediaViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The whole standalone Media tab over a real ViewModel, transport and engine,
 * with the videos held in memory. Asserts the effect — what the engine projects,
 * what the transport is set to — rather than that a button was drawn.
 */
@OptIn(ExperimentalTestApi::class)
class LocalMediaScreenTest {

    private class Harness {
        val engine = StandaloneEngine(MutableStateFlow(AppMode.STANDALONE), SinkRegistry()) { }
        private val files = FakeVideoFiles()
        private val store = InMemoryFileStorage()

        // Seeded as a saved list plus its files, then loaded — the way the app opens
        // its library at launch, and with nothing to suspend on (runBlocking is not
        // available on every target this test compiles for).
        val library: VideoLibrary = run {
            val seeded = listOf(
                LocalVideo(id = "v2", title = "Countdown", fileName = "v2.mp4", durationMs = 300_000L),
                LocalVideo(id = "v1", title = "Welcome", fileName = "v1.mp4", durationMs = 180_000L),
            )
            seeded.forEach { files.files[it.fileName] = ByteArray(8) }
            store.write(VIDEO_INDEX_FILE, Json.encodeToString(ListSerializer(LocalVideo.serializer()), seeded))
            VideoLibrary(files, store, newId = { "new" }, now = { 0L }).also {
                it.load()
                it.serveFrom("http://192.168.1.20:8766")
            }
        }
        val transport = VideoTransport(clock = { 0L }).also {
            engine.videoCue = it::cueFor
            it.onChange = engine::refreshVideo
        }
        val viewModel = LocalMediaViewModel(library, transport, engine)
    }

    private fun ComposeUiTest.show(h: Harness, twoPane: Boolean = false) = setContent {
        AppTheme(themeMode = ThemeMode.DARK) {
            LocalMediaScreen(h.library, h.transport, h.engine, twoPane = twoPane, providedViewModel = h.viewModel)
        }
    }

    private fun ComposeUiTest.press(tag: String) {
        onNodeWithTag(tag, useUnmergedTree = true).performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()
    }

    private fun ComposeUiTest.exists(tag: String) =
        onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun tappingAVideoLoadsItWithoutPuttingItUp() = runComposeUiTest {
        val h = Harness()
        show(h)
        val countdown = h.library.videos.value.first { it.title == "Countdown" }

        press(VideoTags.row(countdown.id))

        onNodeWithTag(VideoTags.TITLE).assertTextEquals("Countdown")
        assertEquals(SlideKind.VIDEO, h.engine.deck.value.kind)
        assertEquals(SlideKind.BLANK, h.engine.currentSlide.value.kind, "loading is browsing, not projecting")
    }

    @Test
    fun liveThenClearPutsTheVideoUpAndTakesItDown() = runComposeUiTest {
        val h = Harness()
        show(h)
        press(VideoTags.row(h.library.videos.value.first().id))

        press(VideoTags.LIVE)
        assertEquals(SlideKind.VIDEO, h.engine.currentSlide.value.kind)

        press(VideoTags.CLEAR)
        assertTrue(h.engine.deck.value.isEmpty)
    }

    @Test
    fun choosingFromTheOptionsCardReachesTheTransport() = runComposeUiTest {
        val h = Harness()
        show(h)
        press(VideoTags.row(h.library.videos.value.first().id))

        press(VideoTags.END_ACTION)
        press(VideoTags.option(VideoTags.END_ACTION, VideoEndAction.HOLD.name))
        press(VideoTags.SOUND)
        press(VideoTags.option(VideoTags.SOUND, VideoSound.PHONE.name))

        assertEquals(VideoEndAction.HOLD, h.transport.state.value.endAction)
        assertEquals(VideoSound.PHONE, h.transport.state.value.sound)
    }

    @Test
    fun aMessageIsShownToTheOperator() = runComposeUiTest {
        val h = Harness()
        show(h)

        h.viewModel.showMessage("Could not add the video: disk full")

        waitUntil(timeoutMillis = 5_000L) {
            onAllNodes(hasText("Could not add the video: disk full"), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun theTabletPutsTheListBesideThePlayer() = runComposeUiTest {
        val h = Harness()
        show(h, twoPane = true)

        assertTrue(exists(VideoTags.PREVIEW))
        assertTrue(exists(VideoTags.ADD))
        press(VideoTags.row(h.library.videos.value.last().id))
        onNodeWithTag(VideoTags.TITLE).assertTextEquals("Welcome")
    }
}
