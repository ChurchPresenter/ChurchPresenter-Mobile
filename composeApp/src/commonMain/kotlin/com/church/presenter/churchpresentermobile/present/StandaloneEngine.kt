package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideBackdrop
import com.church.presenter.churchpresentermobile.model.SlideDeck
import com.church.presenter.churchpresentermobile.model.SlideEnvelope
import com.church.presenter.churchpresentermobile.model.SlideMessageType
import com.church.presenter.churchpresentermobile.model.SlideTextSize
import com.church.presenter.churchpresentermobile.model.SlideTheme
import com.church.presenter.churchpresentermobile.network.WsMessageType
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "StandaloneEngine"

/**
 * Owns what the phone is projecting in [AppMode.STANDALONE]: the loaded deck,
 * the position within it, and the operator's blank / live / styling choices.
 *
 * Every mutator is a no-op while the app is in [AppMode.REMOTE]. That is what
 * lets `SongsViewModel` and `BibleViewModel` call [setDeck] and [showSlide]
 * unconditionally — there is no mode branching anywhere but here and in
 * [ProjectionRouter].
 *
 * On each change the engine stamps a monotonically increasing revision,
 * publishes to [SlideBus] (for in-process renderers such as an external
 * display) and broadcasts through [SinkRegistry] (for out-of-process ones such
 * as a browser on the LAN).
 *
 * @param mode The live app mode; read per call so a mode switch takes effect immediately.
 * @param registry Sinks to broadcast to.
 * @param publish Where in-process renderers read from. Injectable for tests.
 */
class StandaloneEngine(
    private val mode: StateFlow<AppMode>,
    private val registry: SinkRegistry,
    private val publish: (SlideEnvelope) -> Unit = SlideBus::publish,
) {
    private var revision = 0L

    private val _deck = MutableStateFlow(SlideDeck.EMPTY)
    val deck: StateFlow<SlideDeck> = _deck.asStateFlow()

    private val _index = MutableStateFlow(0)
    val index: StateFlow<Int> = _index.asStateFlow()

    private val _isBlank = MutableStateFlow(false)
    val isBlank: StateFlow<Boolean> = _isBlank.asStateFlow()

    private val _isLive = MutableStateFlow(true)
    val isLive: StateFlow<Boolean> = _isLive.asStateFlow()

    private val _textSize = MutableStateFlow(SlideTextSize.MEDIUM)
    val textSize: StateFlow<SlideTextSize> = _textSize.asStateFlow()

    private val _backdrop = MutableStateFlow(SlideBackdrop.GRADIENT)
    val backdrop: StateFlow<SlideBackdrop> = _backdrop.asStateFlow()

    private val _backdropUrl = MutableStateFlow<String?>(null)
    val backdropUrl: StateFlow<String?> = _backdropUrl.asStateFlow()

    private val _theme = MutableStateFlow(SlideTheme())
    val theme: StateFlow<SlideTheme> = _theme.asStateFlow()

    private val _currentSlide = MutableStateFlow(Slide.BLANK)

    /** The slide as it is actually being projected, with operator overrides applied. */
    val currentSlide: StateFlow<Slide> = _currentSlide.asStateFlow()

    private val isActive: Boolean get() = mode.value == AppMode.STANDALONE

    /**
     * Loads a deck and projects its first slide immediately.
     *
     * For the actions that mean "put this on the screen now" — presenting from the library,
     * a web page, a set of photos. Browsing is [loadDeck].
     */
    fun setDeck(deck: SlideDeck) {
        if (!isActive) return
        loadDeck(deck)
        emit()
    }

    /**
     * Loads a deck without putting it on the audience screen.
     *
     * Opening a song or a Bible chapter is browsing, not projecting: it fills the section list
     * so the operator can see what they are about to show, and leaves the screen on whatever is
     * already there until they say otherwise. Loading used to project, so tapping a song in the
     * list put it in front of the congregation before anyone pressed anything.
     */
    fun loadDeck(deck: SlideDeck) {
        if (!isActive) return
        _deck.value = deck
        _index.value = 0
        _isBlank.value = false
        Logger.d(TAG, "loadDeck — kind=${deck.kind} slides=${deck.slides.size} title=${deck.title}")
    }

    /** Projects whatever is loaded, at the slide it is sitting on. The operator's "go live". */
    fun goLive() {
        if (!isActive) return
        // Both ways of being off-air have to give way, or "go live" is a button
        // that does not go live: the audience page hides everything while either
        // isBlank or !isLive, so leaving isLive alone here meant an output taken
        // off-air with the Live toggle could only be recovered from that toggle.
        _isLive.value = true
        _isBlank.value = false
        emit()
    }

    /** Projects the slide at [index], clamped into the deck's range. Clears blank. */
    fun showSlide(index: Int) {
        if (!isActive) return
        _index.value = _deck.value.clampIndex(index)
        _isBlank.value = false
        emit()
    }

    /** Advances one slide. Stops at the end of the deck rather than wrapping. */
    fun next() = showSlide(_index.value + 1)

    /** Steps back one slide. Stops at the start of the deck rather than wrapping. */
    fun previous() = showSlide(_index.value - 1)

    /** Blacks the audience screen out, or restores it. */
    fun setBlank(blank: Boolean) {
        if (!isActive) return
        _isBlank.value = blank
        emit()
    }

    fun toggleBlank() = setBlank(!_isBlank.value)

    /** Holds output back from, or releases it to, the audience screen. */
    fun setLive(live: Boolean) {
        if (!isActive) return
        _isLive.value = live
        emit()
    }

    fun setTextSize(size: SlideTextSize) {
        if (!isActive) return
        _textSize.value = size
        emit()
    }

    fun setBackdrop(backdrop: SlideBackdrop, url: String? = null) {
        if (!isActive) return
        _backdrop.value = backdrop
        _backdropUrl.value = url
        emit()
    }

    fun setTheme(theme: SlideTheme) {
        if (!isActive) return
        _theme.value = theme
        emit()
    }

    /** Unloads the deck and shows nothing. */
    fun clear() {
        if (!isActive) return
        _deck.value = SlideDeck.EMPTY
        _index.value = 0
        _isBlank.value = false
        emit(SlideMessageType.CLEAR)
    }

    /**
     * Handles an action that a service sent through [ProjectionRouter] while in
     * standalone mode.
     *
     * Only actions with a local meaning are honoured — [WsMessageType.CLEAR]
     * blanks the screen. Everything else is a desktop-only concept (schedule
     * management, media transport, uploads) or is superseded by the explicit
     * [setDeck]/[showSlide] handoff, so it is swallowed successfully rather
     * than surfaced to the user as a failure.
     */
    fun handleRemoteAction(type: String, payloadJson: String): Result<Unit> {
        when (type) {
            WsMessageType.CLEAR -> clear()
            else -> Logger.d(TAG, "ignoring desktop-only action in standalone mode: $type")
        }
        return Result.success(Unit)
    }

    /** Builds the current slide, stamps a revision, and pushes it everywhere. */
    private fun emit(type: SlideMessageType = SlideMessageType.SLIDE) {
        val base = _deck.value.slideAt(_index.value) ?: Slide.BLANK
        // The operator's backdrop choice is what sits *behind text*. A photo
        // slide is not text on a background — the image is the slide — so a slide
        // carrying its own image keeps it, and stepping through a set of photos
        // changes the picture rather than repainting the same one.
        val carriesOwnImage = base.backdrop == SlideBackdrop.IMAGE && !base.backdropUrl.isNullOrBlank()
        val slide = base.copy(
            textSize = _textSize.value,
            backdrop = if (carriesOwnImage) base.backdrop else _backdrop.value,
            backdropUrl = if (carriesOwnImage) base.backdropUrl else _backdropUrl.value,
            isBlank = _isBlank.value,
            isLive = _isLive.value,
            theme = _theme.value,
            index = _index.value,
            total = _deck.value.slides.size,
        )
        _currentSlide.value = slide
        val envelope = SlideEnvelope(type = type, rev = ++revision, slide = slide)
        publish(envelope)
        registry.broadcast(envelope)
    }
}
