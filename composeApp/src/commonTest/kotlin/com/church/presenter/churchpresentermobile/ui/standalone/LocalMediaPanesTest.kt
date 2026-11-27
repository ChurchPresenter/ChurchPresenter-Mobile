package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.LocalVideo
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.model.VideoEndAction
import com.church.presenter.churchpresentermobile.model.VideoSound
import com.church.presenter.churchpresentermobile.present.VideoTransportState
import com.church.presenter.churchpresentermobile.ui.UiTags
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import com.church.presenter.churchpresentermobile.viewmodel.LocalVideoStatus
import com.church.presenter.churchpresentermobile.viewmodel.VideoAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The standalone Media tab's two halves, framed from plain values: what the
 * operator can press in each state, and which action each press sends.
 *
 * Pressed through the semantics action rather than a tap: on wasmJs a
 * string-resource label lays out empty, and a tap at the centre of a zero-width
 * button lands outside it (see AGENT.md).
 */
@OptIn(ExperimentalTestApi::class)
class LocalMediaPanesTest {

    private val welcome = LocalVideo(id = "v1", title = "Welcome video", fileName = "v1.mp4", durationMs = 180_000L)
    private val countdown = LocalVideo(id = "v2", title = "Countdown", fileName = "v2.mp4", durationMs = 300_000L)
    private val loaded = VideoTransportState(video = welcome, url = "http://phone/video/v1.mp4")

    private fun themed(content: @Composable () -> Unit): @Composable () -> Unit = {
        AppTheme(themeMode = ThemeMode.DARK) { content() }
    }

    private fun ComposeUiTest.press(tag: String) =
        onNodeWithTag(tag, useUnmergedTree = true).performSemanticsAction(SemanticsActions.OnClick)

    private fun ComposeUiTest.exists(tag: String) =
        onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun ComposeUiTest.player(
        state: VideoTransportState = loaded,
        onScreen: Boolean = false,
        blank: Boolean = false,
        positionMs: Long = 42_000L,
        phoneSoundAvailable: Boolean = true,
        actions: MutableList<VideoAction> = mutableListOf(),
    ): MutableList<VideoAction> {
        setContent(themed {
            Column {
                LocalPlayerPane(
                    state = state,
                    status = LocalVideoStatus.READY,
                    positionMs = positionMs,
                    previewCue = null,
                    thumbnailPath = null,
                    onScreen = onScreen,
                    blank = blank,
                    onAction = { actions += it },
                    phoneSoundAvailable = phoneSoundAvailable,
                )
            }
        })
        return actions
    }

    // ── Player ───────────────────────────────────────────────────────────

    @Test
    fun nothingLoadedLeavesEveryControlInert() = runComposeUiTest {
        player(state = VideoTransportState())
        onNodeWithTag(UiTags.MEDIA_PLAY_PAUSE).assertIsNotEnabled()
        onNodeWithTag(VideoTags.LOOP).assertIsNotEnabled()
        onNodeWithTag(VideoTags.LIVE).assertIsNotEnabled()
        onNodeWithTag(VideoTags.CLEAR).assertIsNotEnabled()
        onNodeWithTag(VideoTags.BLANK).assertIsNotEnabled()
        assertFalse(exists(VideoTags.TIME), "no clock over an empty player")
    }

    @Test
    fun aLoadedVideoShowsItsTitleAndClock() = runComposeUiTest {
        player()
        onNodeWithTag(VideoTags.TITLE).assertTextEquals("Welcome video")
        onNodeWithTag(VideoTags.TIME).assertTextEquals("0:42 / 3:00")
    }

    @Test
    fun blankIsOnlyPressableWhileTheVideoIsUp() = runComposeUiTest {
        player(onScreen = false)
        onNodeWithTag(VideoTags.LIVE).assertIsEnabled()
        onNodeWithTag(VideoTags.BLANK).assertIsNotEnabled()
    }

    @Test
    fun eachButtonSendsItsOwnAction() = runComposeUiTest {
        val actions = player(onScreen = true)
        press(UiTags.MEDIA_PLAY_PAUSE)
        press(UiTags.MEDIA_STOP)
        press(UiTags.MEDIA_BACK_10)
        press(UiTags.MEDIA_FORWARD_10)
        press(VideoTags.LOOP)
        press(VideoTags.BLANK)
        press(VideoTags.CLEAR)
        press(VideoTags.LIVE)
        assertEquals(
            listOf(
                VideoAction.TogglePlay,
                VideoAction.Stop,
                VideoAction.Back10,
                VideoAction.Forward10,
                VideoAction.ToggleLoop,
                VideoAction.ToggleBlank,
                VideoAction.Clear,
                VideoAction.ToggleLive,
            ),
            actions,
        )
    }

    @Test
    fun thePausedPreviewCarriesAPlayButton() = runComposeUiTest {
        val actions = player(positionMs = 0L)
        press(VideoTags.PLAY_OVERLAY)
        assertEquals(listOf<VideoAction>(VideoAction.TogglePlay), actions)
    }

    @Test
    fun aPlayingPreviewHasNoPlayButtonOverIt() = runComposeUiTest {
        player(state = loaded.copy(playing = true))
        assertFalse(exists(VideoTags.PLAY_OVERLAY))
    }

    @Test
    fun phoneSoundFallsBackToTheHallWhenTheDeviceCannotKeepIt() = runComposeUiTest {
        // An iPad with a TV on AirPlay: iOS sends the sound to the TV whatever the
        // app asks, so "This phone" is withdrawn and the reason shown.
        val actions = player(state = loaded.copy(sound = VideoSound.PHONE), phoneSoundAvailable = false)
        waitForIdle()
        assertTrue(VideoAction.SetSound(VideoSound.HALL) in actions)
        assertTrue(exists(VideoTags.SOUND_HINT))
    }

    @Test
    fun phoneSoundIsLeftAloneWhereTheDeviceCanKeepIt() = runComposeUiTest {
        val actions = player(state = loaded.copy(sound = VideoSound.PHONE), phoneSoundAvailable = true)
        waitForIdle()
        assertTrue(actions.none { it is VideoAction.SetSound })
        assertFalse(exists(VideoTags.SOUND_HINT))
    }

    @Test
    fun loopShowsAsOnWhenTheClipLoops() = runComposeUiTest {
        val actions = player(state = loaded.copy(endAction = VideoEndAction.LOOP))
        press(VideoTags.LOOP)
        assertEquals(listOf<VideoAction>(VideoAction.ToggleLoop), actions)
    }

    @Test
    fun everyStatusHasItsLine() = runComposeUiTest {
        // One line per state the hall screen can be in; none may be missing.
        var status by mutableStateOf(LocalVideoStatus.EMPTY)
        setContent(themed { Column { VideoNowPlaying(title = null, status = status) } })
        LocalVideoStatus.entries.forEach {
            status = it
            waitForIdle()
            assertTrue(exists(VideoTags.STATUS), "no status line for $it")
        }
    }

    @Test
    fun aStillIsShownBeforeTheClipStarts() = runComposeUiTest {
        setContent(themed {
            Column {
                LocalPlayerPane(
                    state = loaded,
                    status = LocalVideoStatus.READY,
                    positionMs = 0L,
                    previewCue = null,
                    thumbnailPath = "/data/videos/v1.jpg",
                    onScreen = false,
                    blank = true,
                    onAction = {},
                )
            }
        })
        assertTrue(exists(VideoTags.PLAY_OVERLAY))
    }

    @Test
    fun blankedOnScreenStillOffersLive() = runComposeUiTest {
        // Blanked, the video is up but not visible: Live brings it back.
        val actions = player(onScreen = true, blank = true)
        onNodeWithTag(VideoTags.BLANK).assertIsEnabled()
        press(VideoTags.LIVE)
        assertEquals(listOf<VideoAction>(VideoAction.ToggleLive), actions)
    }

    // ── List ─────────────────────────────────────────────────────────────

    private fun ComposeUiTest.list(
        videos: List<LocalVideo>,
        supported: Boolean = true,
        importProgress: Float? = null,
        loadedId: String? = null,
        loopingId: String? = null,
        selected: MutableList<LocalVideo> = mutableListOf(),
        deleted: MutableList<LocalVideo> = mutableListOf(),
    ) {
        setContent(themed {
            Column {
                LocalVideoListPane(
                    videos = videos,
                    loadedId = loadedId,
                    loopingId = loopingId,
                    supported = supported,
                    importProgress = importProgress,
                    thumbnailPath = { "/data/videos/${it.id}.jpg" },
                    onSelect = { selected += it },
                    onDelete = { deleted += it },
                    onPicked = {},
                    onPickError = {},
                )
            }
        })
    }

    @Test
    fun anEmptyLibrarySaysSoAndOffersAdd() = runComposeUiTest {
        list(emptyList())
        assertTrue(exists(VideoTags.EMPTY))
        assertTrue(exists(VideoTags.ADD))
    }

    @Test
    fun tappingARowLoadsThatVideo() = runComposeUiTest {
        val selected = mutableListOf<LocalVideo>()
        list(listOf(welcome, countdown), selected = selected)
        assertFalse(exists(VideoTags.EMPTY))
        press(VideoTags.row(countdown.id))
        assertEquals(listOf(countdown), selected, "the row tapped, not the first one")
    }

    @Test
    fun deleteAsksFirstAndRemovesTheRightVideo() = runComposeUiTest {
        val deleted = mutableListOf<LocalVideo>()
        list(listOf(welcome, countdown), deleted = deleted)
        press(VideoTags.delete(welcome.id))
        assertTrue(deleted.isEmpty(), "nothing is deleted before the operator confirms")
        press(VideoTags.DELETE_CONFIRM)
        assertEquals(listOf(welcome), deleted)
    }

    @Test
    fun addIsUnavailableWhileAVideoIsBeingCopiedIn() = runComposeUiTest {
        list(listOf(welcome), importProgress = 0.4f)
        onNodeWithTag(VideoTags.ADD).assertIsNotEnabled()
    }

    @Test
    fun theLoadedAndLoopingClipsAreMarked() = runComposeUiTest {
        list(listOf(welcome, countdown), loadedId = welcome.id, loopingId = welcome.id)
        assertTrue(
            onAllNodes(hasText("∞ 3:00"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty(),
            "a looping clip's length carries the loop mark",
        )
        assertTrue(onAllNodes(hasText("5:00"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun cancellingDeleteKeepsTheVideo() = runComposeUiTest {
        val deleted = mutableListOf<LocalVideo>()
        list(listOf(welcome), deleted = deleted)
        press(VideoTags.delete(welcome.id))
        assertTrue(exists(VideoTags.DELETE_CONFIRM))
        press(VideoTags.DELETE_CANCEL)
        waitForIdle()
        assertFalse(exists(VideoTags.DELETE_CONFIRM), "the dialog closes")
        assertTrue(deleted.isEmpty())
    }

    @Test
    fun aDeviceWithNowhereToKeepVideosOffersNoAdd() = runComposeUiTest {
        list(emptyList(), supported = false)
        assertFalse(exists(VideoTags.ADD))
        assertFalse(exists(VideoTags.EMPTY))
    }
}
