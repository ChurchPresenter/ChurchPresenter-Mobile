package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.model.SlideDeckBuilder
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private const val TAG = "LocalWebViewModel"

/**
 * A link on the audience screen, shown by this device.
 *
 * The remote Web viewer asks a desktop to open a page; standalone has no
 * desktop, so the phone puts the page on its own outputs — an embedded browser
 * on the screen it drives over HDMI, an iframe on the browser display.
 *
 * A link ending in a video extension is played rather than framed. Operators
 * paste both kinds into the same box, and asking them to classify their own
 * link before pasting it is a question the app can answer itself.
 */
class LocalWebViewModel(private val presenter: StandaloneEngine?) : ViewModel() {

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _projecting = MutableStateFlow<String?>(null)

    /** The link currently on the screen, if any. */
    val projecting: StateFlow<String?> = _projecting.asStateFlow()

    /** True when what is typed could actually be projected. */
    val canProject: StateFlow<Boolean> = _url
        .map { SlideDeckBuilder.isProjectableLink(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setUrl(value: String) {
        _url.value = value
    }

    /**
     * Puts the link on the screen.
     *
     * Rejects anything that is not http(s): the address reaches an embedded
     * browser and an iframe, where other schemes are code execution and file
     * access rather than a slide.
     */
    fun project() {
        val engine = presenter ?: return
        val link = _url.value.trim()
        if (!SlideDeckBuilder.isProjectableLink(link)) {
            Logger.e(TAG, "refusing to project a non-http(s) link")
            return
        }
        val deck = if (SlideDeckBuilder.looksLikeVideo(link)) {
            SlideDeckBuilder.fromVideo(link)
        } else {
            SlideDeckBuilder.fromWebPage(link)
        }
        engine.setDeck(deck)
        _projecting.value = link
        Logger.d(TAG, "project — ${deck.kind} $link")
    }

    /** Takes the page or video off the screen. */
    fun clearDisplay() {
        presenter?.clear()
        _projecting.value = null
    }
}
