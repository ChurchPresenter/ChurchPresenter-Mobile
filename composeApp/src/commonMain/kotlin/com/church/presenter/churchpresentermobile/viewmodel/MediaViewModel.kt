package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.MediaPlaybackState
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.MediaItemPayload
import com.church.presenter.churchpresentermobile.network.MediaCastService
import com.church.presenter.churchpresentermobile.network.PickedMediaFile
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.network.WsSender
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "MediaViewModel"

/** A file uploaded from this device and saved on the desktop, ready to project. */
data class UploadedMedia(val path: String, val title: String, val mediaType: String)

/** Which source the Go Live / Add-to-Schedule actions will send. */
enum class MediaSource { URL, UPLOAD }

/** Guesses a friendly title from a media URL (its file name, sans query/extension). */
fun mediaTitleFrom(url: String): String {
    val clean = url.trim().substringBefore('?').substringBefore('#').trimEnd('/')
    val file = clean.substringAfterLast('/')
    val name = file.substringBeforeLast('.', file)
    return name.replace('_', ' ').replace('-', ' ').trim().ifBlank { domainOf(url).ifBlank { url } }
}

/** Coarse media-kind guess from the URL extension, for the "Video · …" subtitle.
 *  Kept in sync with the desktop player's supported formats. */
fun mediaKindFrom(url: String): String {
    val ext = url.trim().substringBefore('?').substringAfterLast('.', "").lowercase()
    return when (ext) {
        "mp3", "wav", "flac", "aac", "ogg", "wma", "m4a", "aiff", "opus" -> "Audio"
        "mp4", "mov", "avi", "mkv", "wmv", "flv", "webm", "m4v" -> "Video"
        else -> "Media"
    }
}

/**
 * @param eventService The persistent WebSocket, used here for the inbound
 *   `media_state_changed` push event.
 * @param sender Where outbound transport actions go. Defaults to [eventService];
 *   [com.church.presenter.churchpresentermobile.App] passes a
 *   [com.church.presenter.churchpresentermobile.present.ProjectionRouter].
 */
class MediaViewModel(
    private val appSettings: AppSettings,
    private val eventService: ServerEventService,
    sender: WsSender = eventService,
    /**
     * What the desktop reports it is playing. Defaults to the socket's own feed;
     * injectable so tests can drive the fallback in [buildPayload], where an
     * empty composer sends whatever is already loaded over there.
     */
    playbackState: StateFlow<MediaPlaybackState?> = eventService.mediaState,
    /**
     * Builds the service this screen talks to. Injectable for the same reason
     * [playbackState] is: an upload is an HTTP request with a progress callback,
     * and everything this ViewModel does with the result — the title it falls
     * back to, the message it shows, the flags it lowers again — is worth
     * checking without a desktop on the other end.
     */
    serviceFactory: (AppSettings, WsSender) -> MediaCastService = { s, w -> MediaCastService(s, w) },
) : ViewModel() {

    private val service = serviceFactory(appSettings, sender)

    /** Whether Go Live / Add to Schedule will send a URL or an uploaded local file. */
    private val _source = MutableStateFlow(MediaSource.URL)
    val source = _source.asStateFlow()

    private val _url = MutableStateFlow("")
    val url = _url.asStateFlow()

    /** A file uploaded from this device, ready to send as a local media item (null = none). */
    private val _uploaded = MutableStateFlow<UploadedMedia?>(null)
    val uploaded = _uploaded.asStateFlow()

    /** True while an upload is in flight. */
    private val _uploading = MutableStateFlow(false)
    val uploading = _uploading.asStateFlow()

    /** Upload progress 0f‑1f while [uploading] is true. */
    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress = _uploadProgress.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    /** Live desktop media-player state (title, position, playing…) — null until first reported. */
    val playback = playbackState

    /** Full URL of whatever was last sent live — drives the "ON SCREEN" badge. */
    private val _liveUrl = MutableStateFlow<String?>(null)
    val liveUrl = _liveUrl.asStateFlow()

    fun setSource(value: MediaSource) { _source.value = value }

    fun setUrl(value: String) { _url.value = value }

    fun clearMessage() { _message.value = null }

    /** Surfaces a transient message (e.g. a file-picker error) in the UI snackbar. */
    fun showMessage(text: String) { _message.value = text }

    /** Streams a picked file to the desktop, then holds it ready to Go Live / Add to Schedule. */
    fun uploadPicked(picked: PickedMediaFile) {
        viewModelScope.launch {
            _uploading.value = true
            _uploadProgress.value = 0f
            service.uploadMedia(picked) { p -> _uploadProgress.value = p }.fold(
                onSuccess = { resp ->
                    _uploaded.value = UploadedMedia(path = resp.path, title = resp.name.ifBlank { mediaTitleFrom(picked.fileName) }, mediaType = resp.mediaType)
                    _source.value = MediaSource.UPLOAD
                    _message.value = "Uploaded — ready to go live"
                },
                onFailure = { e ->
                    Logger.e(TAG, "uploadMedia failed: ${e.message}", e)
                    _message.value = e.message ?: "Upload failed"
                }
            )
            _uploading.value = false
        }
    }

    private fun buildPayload(): MediaItemPayload? {
        // 1. The explicitly-composed source (uploaded file or typed URL).
        when (_source.value) {
            MediaSource.UPLOAD -> _uploaded.value?.let { up ->
                return MediaItemPayload(id = "", mediaUrl = up.path, mediaTitle = up.title, mediaType = up.mediaType)
            }
            MediaSource.URL -> {
                val full = normalizeUrl(_url.value)
                if (full.isNotBlank()) {
                    return MediaItemPayload(id = "", mediaUrl = full, mediaTitle = mediaTitleFrom(full), mediaType = "url")
                }
            }
        }
        // 2. Nothing composed → fall back to whatever is already loaded on the desktop.
        playback.value?.takeIf { it.isLoaded && it.source.isNotBlank() }?.let { live ->
            return MediaItemPayload(
                id = "",
                mediaUrl = live.source,
                mediaTitle = live.title.ifBlank { mediaTitleFrom(live.source) },
                mediaType = live.mediaType.ifBlank { "url" },
            )
        }
        // 3. Nothing to send.
        _message.value = if (_source.value == MediaSource.UPLOAD) "Upload a file first" else "Enter a media URL first"
        return null
    }

    fun goLive() {
        val payload = buildPayload() ?: return
        viewModelScope.launch {
            service.goLive(payload).fold(
                onSuccess = {
                    _liveUrl.value = payload.mediaUrl
                    _message.value = "Live"
                },
                onFailure = { e ->
                    Logger.e(TAG, "goLive failed: ${e.message}", e)
                    _message.value = e.message ?: "Failed to go live"
                }
            )
        }
    }

    fun addToSchedule() {
        val payload = buildPayload() ?: return
        viewModelScope.launch {
            service.addToSchedule(payload).fold(
                onSuccess = { _message.value = "Added to schedule" },
                onFailure = { e ->
                    Logger.e(TAG, "addToSchedule failed: ${e.message}", e)
                    _message.value = e.message ?: "Failed to add to schedule"
                }
            )
        }
    }

    fun clearScreen() {
        viewModelScope.launch {
            service.clearScreen().fold(
                onSuccess = {
                    _liveUrl.value = null
                    _message.value = "Cleared"
                },
                onFailure = { e ->
                    Logger.e(TAG, "clearScreen failed: ${e.message}", e)
                    _message.value = e.message ?: "Failed to clear"
                }
            )
        }
    }

    // ── Transport controls — the desktop echoes updated state back via [playback] ──

    fun playPause() { viewModelScope.launch { service.playPause() } }
    fun stopPlayback() { viewModelScope.launch { service.stop() } }
    fun seekForward() { viewModelScope.launch { service.seekForward() } }
    fun seekBackward() { viewModelScope.launch { service.seekBackward() } }
    fun seekTo(positionMs: Long) { viewModelScope.launch { service.seekTo(positionMs) } }
    fun setVolume(volume: Float) { viewModelScope.launch { service.setVolume(volume) } }
    fun muteToggle() { viewModelScope.launch { service.muteToggle() } }
}
