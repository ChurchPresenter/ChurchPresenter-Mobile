package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.MediaPlaybackState
import com.church.presenter.churchpresentermobile.network.ServerEventService
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
 * What the media tab says is happening.
 *
 * The phone renders no video: this panel is the operator's only account of what
 * the desktop is doing, and it has to stay honest in the states nobody plans
 * for — a player with nothing loaded, a track with no duration yet, a file
 * uploaded but not started. Each of those reads differently, and getting them
 * confused means pressing Go live on something that is already playing, or on
 * nothing at all.
 */
@OptIn(ExperimentalTestApi::class)
class MediaNowPlayingTest {

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

    private fun ComposeUiTest.showMedia(vm: MediaViewModel, canUploadFiles: Boolean = true) =
        showScreen { MediaScreen(viewModel = vm, canUploadFiles = canUploadFiles, maxUploadMb = 200) }

    private val playing = MediaPlaybackState(
        isLoaded = true,
        isPlaying = true,
        isLive = true,
        title = "Welcome video",
        durationMs = 125_000,
        positionMs = 65_000,
    )

    // ── The clock ────────────────────────────────────────────────────────

    @Test
    fun aFreshPlayerReadsZero() {
        assertEquals("0:00", formatTime(0))
    }

    @Test
    fun anUnknownPositionReadsZeroRatherThanNegative() {
        // The desktop reports -1 while it works out what it has loaded.
        assertEquals("0:00", formatTime(-1))
    }

    @Test
    fun secondsArePaddedToTwoDigits() {
        assertEquals("0:05", formatTime(5_000))
    }

    @Test
    fun aMinuteReadsAsAMinute() {
        assertEquals("1:00", formatTime(60_000))
    }

    @Test
    fun minutesAndSecondsAreBothShown() {
        assertEquals("2:05", formatTime(125_000))
    }

    @Test
    fun anHourKeepsCountingInMinutes() {
        // No hour field: a 90-minute film reads as 90:00, which is still clear.
        assertEquals("90:00", formatTime(5_400_000))
    }

    @Test
    fun partSecondsRoundDownRatherThanUp() {
        assertEquals("0:01", formatTime(1_999))
    }

    // ── What the panel says with nothing loaded ──────────────────────────

    @Test
    fun anIdlePlayerSaysNothingIsLoaded() = runComposeUiTest {
        showMedia(vmWith())

        assertTrue(exists(UiTags.MEDIA_TITLE))
        assertTrue(exists(UiTags.MEDIA_SUBTITLE))
    }

    @Test
    fun anIdlePlayerShowsNoOnScreenMarker() = runComposeUiTest {
        showMedia(vmWith())

        assertFalse(exists(UiTags.MEDIA_ON_SCREEN))
    }

    @Test
    fun anIdlePlayerReadsZeroOnBothClocks() = runComposeUiTest {
        showMedia(vmWith())

        assertTrue(isShowing("0:00"))
    }

    @Test
    fun anIdlePlayerCannotBeScrubbed() = runComposeUiTest {
        showMedia(vmWith())

        tagged(UiTags.MEDIA_SEEK).assertIsNotEnabled()
    }

    @Test
    fun anIdlePlayerSaysItWouldSendNothing() = runComposeUiTest {
        showMedia(vmWith())

        assertTrue(exists(UiTags.MEDIA_WILL_SEND))
    }

    // ── A loaded, playing track ──────────────────────────────────────────

    @Test
    fun aPlayingTrackIsNamed() = runComposeUiTest {
        showMedia(vmWith(playback = playing))

        assertTrue(isShowing("Welcome video"))
    }

    @Test
    fun aPlayingTrackShowsItsPosition() = runComposeUiTest {
        showMedia(vmWith(playback = playing))

        assertTrue(isShowing("1:05"))
    }

    @Test
    fun aPlayingTrackShowsItsDuration() = runComposeUiTest {
        showMedia(vmWith(playback = playing))

        assertTrue(isShowing("2:05"))
    }

    @Test
    fun aTrackOnScreenSaysSo() = runComposeUiTest {
        showMedia(vmWith(playback = playing))

        assertTrue(exists(UiTags.MEDIA_ON_SCREEN))
    }

    @Test
    fun aTrackNotOnScreenShowsNoMarker() = runComposeUiTest {
        showMedia(vmWith(playback = playing.copy(isLive = false)))

        assertFalse(exists(UiTags.MEDIA_ON_SCREEN))
    }

    @Test
    fun aLoadedTrackCanBeScrubbed() = runComposeUiTest {
        showMedia(vmWith(playback = playing))

        tagged(UiTags.MEDIA_SEEK).assertIsEnabled()
    }

    @Test
    fun aTrackWithNoDurationYetCannotBeScrubbed() = runComposeUiTest {
        // Scrubbing a fraction of an unknown length would seek nowhere.
        showMedia(vmWith(playback = playing.copy(durationMs = 0)))

        tagged(UiTags.MEDIA_SEEK).assertIsNotEnabled()
    }

    @Test
    fun aLoadedTrackWithNoTitleStillNamesSomething() = runComposeUiTest {
        showMedia(vmWith(playback = playing.copy(title = "")))

        assertTrue(exists(UiTags.MEDIA_TITLE))
    }

    @Test
    fun aLoadedTrackSaysItIsPlayingOnTheDesktop() = runComposeUiTest {
        showMedia(vmWith(playback = playing))

        assertTrue(exists(UiTags.MEDIA_SUBTITLE))
    }

    // ── The transport, by state ──────────────────────────────────────────

    @Test
    fun nothingLoadedLeavesTheTransportDisabled() = runComposeUiTest {
        showMedia(vmWith())

        tagged(UiTags.MEDIA_STOP).assertIsNotEnabled()
        tagged(UiTags.MEDIA_BACK_10).assertIsNotEnabled()
        tagged(UiTags.MEDIA_FORWARD_10).assertIsNotEnabled()
        tagged(UiTags.MEDIA_MUTE).assertIsNotEnabled()
    }

    @Test
    fun aLoadedTrackEnablesTheTransport() = runComposeUiTest {
        showMedia(vmWith(playback = playing))

        tagged(UiTags.MEDIA_STOP).assertIsEnabled()
        tagged(UiTags.MEDIA_BACK_10).assertIsEnabled()
        tagged(UiTags.MEDIA_FORWARD_10).assertIsEnabled()
        tagged(UiTags.MEDIA_MUTE).assertIsEnabled()
    }

    @Test
    fun aLoadedTrackEnablesTheVolumeSlider() = runComposeUiTest {
        showMedia(vmWith(playback = playing))

        tagged(UiTags.MEDIA_VOLUME).assertIsEnabled()
    }

    @Test
    fun anIdlePlayerDisablesTheVolumeSlider() = runComposeUiTest {
        showMedia(vmWith())

        tagged(UiTags.MEDIA_VOLUME).assertIsNotEnabled()
    }

    @Test
    fun aPausedTrackStillOffersTheTransport() = runComposeUiTest {
        showMedia(vmWith(playback = playing.copy(isPlaying = false)))

        tagged(UiTags.MEDIA_STOP).assertIsEnabled()
    }

    @Test
    fun aMutedTrackStillOffersTheMuteButton() = runComposeUiTest {
        // It is the way back; disabling it would trap the operator on silence.
        showMedia(vmWith(playback = playing.copy(muted = true)))

        tagged(UiTags.MEDIA_MUTE).assertIsEnabled()
    }

    // ── Where the source comes from ──────────────────────────────────────

    @Test
    fun bothSourcesAreOffered() = runComposeUiTest {
        showMedia(vmWith())

        assertTrue(exists(UiTags.mediaSource(0)))
        assertTrue(exists(UiTags.mediaSource(1)))
    }

    @Test
    fun aNetworkUrlIsTheSourceToBeginWith() = runComposeUiTest {
        showMedia(vmWith())

        tagged(UiTags.mediaSource(0)).assertIsSelected()
    }

    @Test
    fun uploadIsNotTheSourceToBeginWith() = runComposeUiTest {
        showMedia(vmWith())

        tagged(UiTags.mediaSource(1)).assertIsNotSelected()
    }

    @Test
    fun choosingUploadSwitchesTheSource() = runComposeUiTest {
        val vm = vmWith()
        showMedia(vm)

        click(UiTags.mediaSource(1))

        assertEquals(MediaSource.UPLOAD, vm.source.value)
    }

    @Test
    fun choosingUploadMarksItSelected() = runComposeUiTest {
        val vm = vmWith()
        showMedia(vm)

        click(UiTags.mediaSource(1))

        tagged(UiTags.mediaSource(1)).assertIsSelected()
    }

    @Test
    fun choosingUploadTakesTheUrlFieldAway() = runComposeUiTest {
        val vm = vmWith()
        showMedia(vm)

        click(UiTags.mediaSource(1))

        assertFalse(exists(UiTags.MEDIA_URL))
    }

    @Test
    fun goingBackToUrlBringsTheFieldBack() = runComposeUiTest {
        val vm = vmWith()
        showMedia(vm)
        click(UiTags.mediaSource(1))

        click(UiTags.mediaSource(0))

        assertTrue(exists(UiTags.MEDIA_URL))
    }

    @Test
    fun choosingUploadOffersThePicker() = runComposeUiTest {
        val vm = vmWith()
        showMedia(vm, canUploadFiles = true)

        click(UiTags.mediaSource(1))

        assertTrue(exists(UiTags.MEDIA_UPLOAD))
    }

    @Test
    fun aDesktopWithUploadsOffOffersNoPicker() = runComposeUiTest {
        val vm = vmWith()
        showMedia(vm, canUploadFiles = false)

        click(UiTags.mediaSource(1))

        assertFalse(exists(UiTags.MEDIA_UPLOAD))
    }

    // ── What Go live would actually send ─────────────────────────────────

    @Test
    fun anEmptyUrlSaysThereIsNothingToSend() = runComposeUiTest {
        showMedia(vmWith())

        assertTrue(exists(UiTags.MEDIA_WILL_SEND))
    }

    @Test
    fun aTypedUrlBecomesWhatWouldBeSent() = runComposeUiTest {
        val vm = vmWith()
        showMedia(vm)

        type(UiTags.MEDIA_URL, "https://example.org/clip.mp4")

        assertTrue(exists(UiTags.MEDIA_WILL_SEND))
        assertEquals("https://example.org/clip.mp4", vm.url.value)
    }

    @Test
    fun aTypedUrlIsNamedInTheTitle() = runComposeUiTest {
        val vm = vmWith()
        showMedia(vm)

        type(UiTags.MEDIA_URL, "https://example.org/clip.mp4")

        assertTrue(isShowing("clip.mp4"))
    }

    @Test
    fun whatIsAlreadyPlayingIsWhatWouldBeSent() = runComposeUiTest {
        showMedia(vmWith(playback = playing.copy(source = "https://example.org/clip.mp4")))

        assertTrue(exists(UiTags.MEDIA_WILL_SEND))
        assertTrue(isShowing("Welcome video"))
    }

    @Test
    fun aLoadedTrackWithNoSourceCannotBeResent() = runComposeUiTest {
        // The desktop knows what it is playing; the phone does not, so there is
        // nothing it could hand back.
        showMedia(vmWith(playback = playing.copy(source = "")))

        assertTrue(exists(UiTags.MEDIA_WILL_SEND))
    }
}
