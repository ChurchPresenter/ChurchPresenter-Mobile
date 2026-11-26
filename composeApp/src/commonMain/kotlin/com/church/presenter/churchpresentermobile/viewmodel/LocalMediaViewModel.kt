package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.model.LocalVideo
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideDeckBuilder
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.model.VideoCue
import com.church.presenter.churchpresentermobile.model.VideoEndAction
import com.church.presenter.churchpresentermobile.model.VideoSound
import com.church.presenter.churchpresentermobile.network.PickedMediaFile
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.present.VIDEO_SKIP_MS
import com.church.presenter.churchpresentermobile.present.VideoLibrary
import com.church.presenter.churchpresentermobile.present.VideoTransport
import com.church.presenter.churchpresentermobile.present.VideoTransportState
import com.church.presenter.churchpresentermobile.present.togglePlay
import com.church.presenter.churchpresentermobile.present.toggleLoop
import com.church.presenter.churchpresentermobile.present.stop
import com.church.presenter.churchpresentermobile.present.skip
import com.church.presenter.churchpresentermobile.present.pause
import com.church.presenter.churchpresentermobile.present.cue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** How often the position under the scrubber is redrawn while playing. */
private const val POSITION_TICK_MS = 250L

/** What the hall screen is doing with the loaded video, for the line under its title. */
enum class LocalVideoStatus {
    /** Nothing loaded. */
    EMPTY,

    /** Loaded, paused, not on screen. */
    READY,

    /** Playing in the phone's preview only. */
    PREVIEWING,

    /** On screen and playing. */
    PLAYING_LIVE,

    /** On screen and paused. */
    PAUSED_LIVE,
}

/**
 * Backs the standalone Media tab: the videos on this device and the player that
 * puts one on the hall screen.
 *
 * Thin on purpose, like [StandaloneViewModel]. The transport and the library
 * outlive this screen — a video keeps playing when the operator switches to the
 * Bible tab — so they are collaborators, and this turns taps into calls on them.
 *
 * Loading a video is browsing, not projecting: it fills the player and leaves
 * the hall screen alone until Live, exactly as opening a song does.
 */
class LocalMediaViewModel(
    private val library: VideoLibrary,
    private val transport: VideoTransport,
    private val engine: StandaloneEngine,
) : ViewModel() {

    val videos: StateFlow<List<LocalVideo>> = library.videos
    val transportState: StateFlow<VideoTransportState> = transport.state
    val isBlank: StateFlow<Boolean> = engine.isBlank
    val isSupported: Boolean get() = library.isSupported

    /** True while the loaded video is what the hall screen is showing (blanked or not). */
    val isOnScreen: StateFlow<Boolean> = combine(engine.currentSlide, transport.state) { slide, state ->
        showsLoadedVideo(slide, state)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * [isOnScreen] as of this instant. The actions read this rather than the flow:
     * a flow collected on the main dispatcher can lag a tap behind, and Clear
     * pressed straight after Live must still know the video is up.
     */
    private fun onScreenNow(): Boolean = showsLoadedVideo(engine.currentSlide.value, transport.state.value)

    val status: StateFlow<LocalVideoStatus> = combine(isOnScreen, transport.state) { onScreen, state ->
        when {
            !state.isLoaded -> LocalVideoStatus.EMPTY
            onScreen && state.playing -> LocalVideoStatus.PLAYING_LIVE
            onScreen -> LocalVideoStatus.PAUSED_LIVE
            state.playing -> LocalVideoStatus.PREVIEWING
            else -> LocalVideoStatus.READY
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LocalVideoStatus.EMPTY)

    /** The cue the phone's own preview follows — the same one the screens get. */
    val previewCue: StateFlow<VideoCue?> = transport.state
        .map { if (it.isLoaded) transport.cue() else null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * The cue as of this instant, for a player being created now. [previewCue]
     * only changes when a button is pressed, so a preview built a minute after
     * Play — coming back to the tab — would otherwise start a minute behind.
     */
    fun currentCue(): VideoCue? = if (transport.state.value.isLoaded) transport.cue() else null

    private val _positionMs = MutableStateFlow(transport.positionMs())

    /** Where the video is, refreshed a few times a second while it plays. */
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _importProgress = MutableStateFlow<Float?>(null)

    /** 0..1 while a video is being copied in, null otherwise. */
    val importProgress: StateFlow<Float?> = _importProgress.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                _positionMs.value = transport.positionMs()
                delay(POSITION_TICK_MS)
            }
        }
    }

    /** Puts [video] in the player without touching the hall screen. */
    fun select(video: LocalVideo) {
        val url = library.playbackUrl(video) ?: run {
            _message.value = "This video cannot be played right now"
            return
        }
        if (transport.state.value.video?.id == video.id && transport.state.value.url == url) return
        // The deck first: the transport tells the engine about the change, and the
        // engine must already know the new video is not the one on screen, or the
        // clip still playing there would be re-sent without its cue.
        engine.loadDeck(SlideDeckBuilder.fromLocalVideo(url, video.title, video.id))
        transport.load(video, url)
        _positionMs.value = 0L
    }

    /** Everything the player's buttons and sliders do. */
    fun onAction(action: VideoAction) {
        when (action) {
            VideoAction.ToggleLive -> toggleLive()
            VideoAction.ToggleBlank -> engine.toggleBlank()
            VideoAction.Clear -> clear()
            VideoAction.TogglePlay -> transport.togglePlay()
            VideoAction.Stop -> transport.stop()
            VideoAction.Back10 -> transport.skip(-VIDEO_SKIP_MS)
            VideoAction.Forward10 -> transport.skip(VIDEO_SKIP_MS)
            VideoAction.ToggleLoop -> transport.toggleLoop()
            is VideoAction.SeekTo -> transport.seekTo(action.positionMs)
            is VideoAction.SetEndAction -> transport.setEndAction(action.endAction)
            is VideoAction.SetVolume -> transport.setVolume(action.volume)
            is VideoAction.SetSound -> transport.setSound(action.sound)
        }
        _positionMs.value = transport.positionMs()
    }

    /**
     * Puts the loaded video on the hall screen — or, when it already is, takes
     * the screen off-air, as the live bar's toggle does.
     */
    private fun toggleLive() {
        val state = transport.state.value
        val url = state.url ?: return
        val video = state.video ?: return
        if (onScreenNow() && !engine.isBlank.value) {
            engine.setLive(false)
            return
        }
        // The operator may have opened a song since: put the video back first, so
        // Live always means "this video", whatever the deck was a moment ago.
        val loaded = engine.deck.value.slideAt(0)
        if (engine.deck.value.kind != SlideKind.VIDEO || loaded?.mediaUrl != url) {
            engine.loadDeck(SlideDeckBuilder.fromLocalVideo(url, video.title, video.id))
        }
        engine.goLive()
    }

    /**
     * Takes the video off every screen and stops it. Clears whenever the engine
     * holds this video — showing, blanked, or taken off-air with "On screen" —
     * not only while it is live: off-air, Clear used to only pause, and the video
     * stayed loaded behind the screen.
     */
    private fun clear() {
        transport.pause()
        val url = transport.state.value.url ?: return
        val holdsVideo = engine.currentSlide.value.mediaUrl == url || engine.deck.value.slideAt(0)?.mediaUrl == url
        if (holdsVideo) engine.clear()
    }

    /** Copies a picked file in, and loads it into the player once it is there. */
    fun import(file: PickedMediaFile) {
        if (_importProgress.value != null) return
        _importProgress.value = 0f
        viewModelScope.launch {
            val total = file.sizeBytes
            library.import(file) { written ->
                if (total > 0L) _importProgress.value = (written.toFloat() / total).coerceIn(0f, 1f)
            }.onSuccess { video ->
                select(video)
            }.onFailure {
                _message.value = "Could not add the video: ${it.message}"
            }
            _importProgress.value = null
        }
    }

    /** Deletes [video]; if it is loaded, unloads it, and if it is up, clears the screen first. */
    fun delete(video: LocalVideo) {
        if (transport.state.value.video?.id == video.id) {
            if (onScreenNow()) engine.clear()
            transport.unload()
        }
        library.remove(video.id)
    }

    fun showMessage(text: String) {
        _message.value = text
    }

    fun clearMessage() {
        _message.value = null
    }
}

private fun showsLoadedVideo(slide: Slide, state: VideoTransportState): Boolean =
    state.isLoaded && slide.kind == SlideKind.VIDEO && slide.mediaUrl == state.url && slide.isLive

/** What the standalone player's controls ask for. One entry point keeps the ViewModel's surface small. */
sealed interface VideoAction {
    data object ToggleLive : VideoAction
    data object ToggleBlank : VideoAction
    data object Clear : VideoAction
    data object TogglePlay : VideoAction
    data object Stop : VideoAction
    data object Back10 : VideoAction
    data object Forward10 : VideoAction
    data object ToggleLoop : VideoAction
    data class SeekTo(val positionMs: Long) : VideoAction
    data class SetEndAction(val endAction: VideoEndAction) : VideoAction
    data class SetVolume(val volume: Float) : VideoAction
    data class SetSound(val sound: VideoSound) : VideoAction
}
