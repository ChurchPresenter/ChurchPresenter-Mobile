package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.runtime.Composable
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.MediaPlaybackState
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.MediaScreen
import com.church.presenter.churchpresentermobile.viewmodel.MediaViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test

/**
 * The media tab, driven by a real [MediaViewModel] over fake plumbing.
 *
 * The ViewModel is built the way `MediaScreenTest` builds it — in-memory
 * settings, a [FakeWsSender], and the playback state handed straight in — so
 * these captures go through the same code the app runs, not a stand-in of the
 * screen.
 *
 * The transport is what changes: nothing loaded disables it, a loaded track
 * shows a scrubber and a duration, and playing swaps the play control for a
 * pause.
 */
class MediaScreenshotTest {

    private fun media(
        playback: MediaPlaybackState? = null,
        canUploadFiles: Boolean = true,
    ): @Composable () -> Unit {
        val settings = AppSettings(InMemorySettingsStorage())
        val viewModel = MediaViewModel(
            appSettings = settings,
            eventService = ServerEventService(settings),
            sender = FakeWsSender(),
            playbackState = MutableStateFlow(playback),
        )
        return { MediaScreen(viewModel = viewModel, canUploadFiles = canUploadFiles, maxUploadMb = 200) }
    }

    private val loaded = MediaPlaybackState(
        isLive = true,
        isLoaded = true,
        isPlaying = false,
        title = "Welcome video",
        durationMs = 60_000,
        positionMs = 5_000,
        mediaType = "video",
    )

    @Test
    fun nothingLoaded() = screenshot("media__nothing-loaded", content = media())

    @Test
    fun loadedAndPaused() = screenshot("media__paused", content = media(playback = loaded))

    @Test
    fun playing() = screenshot(
        "media__playing",
        content = media(playback = loaded.copy(isPlaying = true, positionMs = 32_000)),
    )

    @Test
    fun muted() = screenshot(
        "media__muted",
        content = media(playback = loaded.copy(isPlaying = true, muted = true, volume = 0f)),
    )

    @Test
    fun uploadsUnavailable() = screenshot(
        // An older desktop with no upload endpoint: the control is not offered
        // rather than offered and failing.
        "media__uploads-unavailable",
        content = media(playback = loaded, canUploadFiles = false),
    )
}
