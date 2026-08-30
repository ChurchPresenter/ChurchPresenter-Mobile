package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.model.SlideDeckBuilder
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.network.FramingCheck
import com.church.presenter.churchpresentermobile.network.createHttpClient
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
class LocalWebViewModel(
    private val presenter: StandaloneEngine?,
    /**
     * Asks a site whether it allows being shown inside another page. Injected so
     * the ViewModel is testable without a socket.
     */
    private val refusesFraming: suspend (String) -> Boolean = { url ->
        FramingCheck.refusesFraming(url, framingClient)
    },
) : ViewModel() {

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _projecting = MutableStateFlow<String?>(null)

    /** The link currently on the screen, if any. */
    val projecting: StateFlow<String?> = _projecting.asStateFlow()

    /** True when what is typed could actually be projected. */
    val canProject: StateFlow<Boolean> = _url
        .map { SlideDeckBuilder.isProjectableLink(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _refusedByFraming = MutableStateFlow<String?>(null)

    /**
     * The host of a site that will not appear on the browser screen, once one has
     * been projected.
     *
     * A screen attached to this phone still shows it — that output loads the
     * address as a page in its own right, so the site's rule about being framed
     * does not apply to it. Saying which output is affected is the difference
     * between a useful warning and a scare.
     */
    val refusedByFraming: StateFlow<String?> = _refusedByFraming.asStateFlow()

    fun setUrl(value: String) {
        _url.value = value
        _refusedByFraming.value = null
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
        // "youtube.com" is what people type; the scheme is filled in here rather than demanded
        // of them. Anything carrying a scheme that is not http(s) is still refused.
        val link = SlideDeckBuilder.normaliseLink(_url.value) ?: run {
            Logger.e(TAG, "refusing to project a link that is not http(s)")
            return
        }
        val deck = if (SlideDeckBuilder.looksLikeVideo(link)) {
            SlideDeckBuilder.fromVideo(link)
        } else {
            SlideDeckBuilder.fromWebPage(link)
        }
        engine.setDeck(deck)
        _projecting.value = link
        _refusedByFraming.value = null
        Logger.d(TAG, "project — ${deck.kind} $link")

        // Asked after projecting, not before: a screen attached to this phone
        // shows the page regardless, so the answer must never hold it up. A video
        // is played rather than framed, so the question does not arise.
        if (deck.kind == SlideKind.WEB) {
            viewModelScope.launch {
                if (refusesFraming(link) && _projecting.value == link) {
                    _refusedByFraming.value = hostOf(link)
                }
            }
        }
    }

    /** The bare host, for a warning that reads like something a person would say. */
    private fun hostOf(link: String): String =
        link.substringAfter("://").substringBefore('/').removePrefix("www.")

    /** Takes the page or video off the screen. */
    fun clearDisplay() {
        presenter?.clear()
        _projecting.value = null
        _refusedByFraming.value = null
    }
}

/** One client for the framing question, shared by every instance. */
private val framingClient by lazy { createHttpClient() }
