package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.MediaPlaybackState
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.network.WsMessageType
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.viewmodel.MediaSource
import com.church.presenter.churchpresentermobile.viewmodel.MediaViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The media tab — a video or track played out through the desktop.
 *
 * The transport controls are the part worth pinning. They act on whatever the
 * desktop has loaded, so a Stop that fires while nothing is loaded, or a
 * play/pause left enabled on an empty player, sends a command to a desktop with
 * nothing to apply it to. Everything asserts on what reached the sender, or on
 * whether the control could be pressed at all.
 */
@OptIn(ExperimentalTestApi::class)
class MediaScreenTest {

    private fun vmWith(
        sender: FakeWsSender = FakeWsSender(),
        playback: MediaPlaybackState? = null,
    ): MediaViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        return MediaViewModel(
            appSettings = settings,
            eventService = ServerEventService(settings),
            sender = sender,
            playbackState = MutableStateFlow(playback),
        )
    }

    private fun ComposeUiTest.showMedia(
        vm: MediaViewModel,
        canUploadFiles: Boolean = true,
        maxUploadMb: Int = 200,
    ) = showScreen {
        MediaScreen(viewModel = vm, canUploadFiles = canUploadFiles, maxUploadMb = maxUploadMb)
    }

    private val loaded = MediaPlaybackState(
        isLoaded = true,
        isPlaying = false,
        title = "Welcome video",
        durationMs = 60_000,
        positionMs = 5_000,
    )

    // ── The address bar ──────────────────────────────────────────────────

    @Test
    fun theUrlFieldIsOffered() = runComposeUiTest {
        showMedia(vmWith())

        assertTrue(exists(UiTags.MEDIA_URL))
    }

    @Test
    fun typingAUrlIsKept() = runComposeUiTest {
        val vm = vmWith()
        showMedia(vm)

        type(UiTags.MEDIA_URL, "https://example.org/clip.mp4")

        assertEquals("https://example.org/clip.mp4", vm.url.value)
    }

    @Test
    fun theUrlIsShownBack() = runComposeUiTest {
        val vm = vmWith()
        showMedia(vm)

        type(UiTags.MEDIA_URL, "https://example.org/clip.mp4")

        assertTrue(isShowing("https://example.org/clip.mp4"))
    }

    // ── Sending it to the desktop ────────────────────────────────────────

    @Test
    fun goingLiveSendsTheClip() = runComposeUiTest {
        val sender = FakeWsSender()
        val vm = vmWith(sender)
        showMedia(vm)

        type(UiTags.MEDIA_URL, "https://example.org/clip.mp4")
        click(UiTags.MEDIA_GO_LIVE)

        awaitThat { sender.calls.isNotEmpty() }
        assertEquals(WsMessageType.PROJECT, sender.lastType)
    }

    @Test
    fun theTypedAddressReachesTheDesktop() = runComposeUiTest {
        val sender = FakeWsSender()
        val vm = vmWith(sender)
        showMedia(vm)

        type(UiTags.MEDIA_URL, "https://example.org/clip.mp4")
        click(UiTags.MEDIA_GO_LIVE)

        awaitThat { sender.calls.isNotEmpty() }
        assertTrue("https://example.org/clip.mp4" in sender.lastPayload, sender.lastPayload)
    }

    @Test
    fun addingToTheScheduleGoesDownTheScheduleRoute() = runComposeUiTest {
        // Queued rather than projected — a different thing to do with the clip.
        val sender = FakeWsSender()
        val vm = vmWith(sender)
        showMedia(vm)

        type(UiTags.MEDIA_URL, "https://example.org/clip.mp4")
        click(UiTags.MEDIA_ADD_TO_SCHEDULE)

        awaitThat { sender.calls.isNotEmpty() }
        assertEquals(WsMessageType.ADD_TO_SCHEDULE, sender.lastType)
    }

    @Test
    fun clearingTellsTheDesktopToBlank() = runComposeUiTest {
        val sender = FakeWsSender()
        val vm = vmWith(sender)
        showMedia(vm)

        click(UiTags.MEDIA_CLEAR)

        awaitThat { sender.calls.isNotEmpty() }
        assertEquals(WsMessageType.CLEAR, sender.lastType)
    }

    // ── The transport, and what it acts on ───────────────────────────────

    @Test
    fun playPauseIsDeadWhileNothingIsLoaded() = runComposeUiTest {
        // There is nothing to play; the command would reach a desktop with
        // nothing to apply it to.
        showMedia(vmWith(playback = null))

        tagged(UiTags.MEDIA_PLAY_PAUSE).assertIsNotEnabled()
    }

    @Test
    fun playPauseIsLiveOnceSomethingIsLoaded() = runComposeUiTest {
        showMedia(vmWith(playback = loaded))

        tagged(UiTags.MEDIA_PLAY_PAUSE).assertIsEnabled()
    }

    @Test
    fun stopIsDeadWhileNothingIsLoaded() = runComposeUiTest {
        showMedia(vmWith(playback = null))

        tagged(UiTags.MEDIA_STOP).assertIsNotEnabled()
    }

    @Test
    fun stopIsLiveOnceSomethingIsLoaded() = runComposeUiTest {
        showMedia(vmWith(playback = loaded))

        tagged(UiTags.MEDIA_STOP).assertIsEnabled()
    }

    @Test
    fun theSkipControlsAreDeadWhileNothingIsLoaded() = runComposeUiTest {
        showMedia(vmWith(playback = null))

        tagged(UiTags.MEDIA_BACK_10).assertIsNotEnabled()
        tagged(UiTags.MEDIA_FORWARD_10).assertIsNotEnabled()
    }

    @Test
    fun theSkipControlsAreLiveOnceSomethingIsLoaded() = runComposeUiTest {
        showMedia(vmWith(playback = loaded))

        tagged(UiTags.MEDIA_BACK_10).assertIsEnabled()
        tagged(UiTags.MEDIA_FORWARD_10).assertIsEnabled()
    }

    @Test
    fun muteIsDeadWhileNothingIsLoaded() = runComposeUiTest {
        showMedia(vmWith(playback = null))

        tagged(UiTags.MEDIA_MUTE).assertIsNotEnabled()
    }

    @Test
    fun playPauseSendsThePlayPauseCommand() = runComposeUiTest {
        val sender = FakeWsSender()
        val vm = vmWith(sender, playback = loaded)
        showMedia(vm)

        click(UiTags.MEDIA_PLAY_PAUSE)

        awaitThat { sender.calls.isNotEmpty() }
        assertEquals(WsMessageType.MEDIA_PLAY_PAUSE, sender.lastType)
    }

    @Test
    fun stopSendsTheStopCommand() = runComposeUiTest {
        val sender = FakeWsSender()
        val vm = vmWith(sender, playback = loaded)
        showMedia(vm)

        click(UiTags.MEDIA_STOP)

        awaitThat { sender.calls.isNotEmpty() }
        assertEquals(WsMessageType.MEDIA_STOP, sender.lastType)
    }

    @Test
    fun skippingBackSendsTheBackwardCommand() = runComposeUiTest {
        // Forward and back are one transposition apart, and getting them the
        // wrong way round is invisible until a service.
        val sender = FakeWsSender()
        val vm = vmWith(sender, playback = loaded)
        showMedia(vm)

        click(UiTags.MEDIA_BACK_10)

        awaitThat { sender.calls.isNotEmpty() }
        assertEquals(WsMessageType.MEDIA_SEEK_BACKWARD, sender.lastType)
    }

    @Test
    fun skippingForwardSendsTheForwardCommand() = runComposeUiTest {
        val sender = FakeWsSender()
        val vm = vmWith(sender, playback = loaded)
        showMedia(vm)

        click(UiTags.MEDIA_FORWARD_10)

        awaitThat { sender.calls.isNotEmpty() }
        assertEquals(WsMessageType.MEDIA_SEEK_FORWARD, sender.lastType)
    }

    @Test
    fun muteSendsTheMuteCommand() = runComposeUiTest {
        val sender = FakeWsSender()
        val vm = vmWith(sender, playback = loaded)
        showMedia(vm)

        click(UiTags.MEDIA_MUTE)

        awaitThat { sender.calls.isNotEmpty() }
        assertEquals(WsMessageType.MEDIA_MUTE_TOGGLE, sender.lastType)
    }

    // ── What the desktop is playing ──────────────────────────────────────

    @Test
    fun theLoadedTitleIsShown() = runComposeUiTest {
        showMedia(vmWith(playback = loaded))

        assertTrue(isShowing("Welcome video"))
    }

    @Test
    fun theVolumeSliderIsDeadWhileNothingIsLoaded() = runComposeUiTest {
        showMedia(vmWith(playback = null))

        tagged(UiTags.MEDIA_VOLUME).assertIsNotEnabled()
    }

    @Test
    fun theVolumeSliderIsLiveOnceSomethingIsLoaded() = runComposeUiTest {
        showMedia(vmWith(playback = loaded))

        tagged(UiTags.MEDIA_VOLUME).assertIsEnabled()
    }

    // ── Uploading a local file ───────────────────────────────────────────

    @Test
    fun theScreenOpensOnTheUrlSource() = runComposeUiTest {
        // Typing an address is the common case; picking a file is the detour.
        val vm = vmWith()
        showMedia(vm)

        assertEquals(MediaSource.URL, vm.source.value)
        assertTrue(exists(UiTags.MEDIA_URL))
    }

    @Test
    fun switchingToUploadReplacesTheUrlField() = runComposeUiTest {
        // One source at a time — a URL box left on screen beside a chosen file
        // is how the wrong one gets sent.
        val vm = vmWith()
        showMedia(vm)

        vm.setSource(MediaSource.UPLOAD)

        awaitThat { !exists(UiTags.MEDIA_URL) }
        assertFalse(exists(UiTags.MEDIA_URL))
    }

    @Test
    fun uploadingIsOfferedWhenTheDesktopAllowsIt() = runComposeUiTest {
        val vm = vmWith()
        showMedia(vm, canUploadFiles = true)

        vm.setSource(MediaSource.UPLOAD)

        awaitThat { exists(UiTags.MEDIA_UPLOAD) }
        assertTrue(exists(UiTags.MEDIA_UPLOAD))
    }

    @Test
    fun uploadingIsNotOfferedWhenTheDesktopForbidsIt() = runComposeUiTest {
        // Offering it would send a file the desktop rejects, and the operator
        // would have no idea why.
        val vm = vmWith()
        showMedia(vm, canUploadFiles = false)

        vm.setSource(MediaSource.UPLOAD)

        awaitThat { vm.source.value == MediaSource.UPLOAD }
        assertFalse(exists(UiTags.MEDIA_UPLOAD))
    }
}
