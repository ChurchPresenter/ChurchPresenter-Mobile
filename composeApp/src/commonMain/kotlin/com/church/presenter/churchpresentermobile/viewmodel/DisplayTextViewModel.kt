package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.DisplayTextTheme
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.model.SlideTheme
import com.church.presenter.churchpresentermobile.model.SlideThemeCodec
import com.church.presenter.churchpresentermobile.model.TextElement
import com.church.presenter.churchpresentermobile.model.TextElementStyle
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Backs Display text: a draft of the per-element styling, edited until the operator saves it.
 *
 * Unlike the Look sheet, which writes every change straight to the audience screen, this holds
 * a draft. Dragging a shadow slider during a service must not repaint the hall screen on every
 * frame, and twenty controls on six elements is too much to adjust live without a way back —
 * so the preview shows the draft and only [save] reaches the output.
 *
 * @param engine The process-wide presenter engine, whose theme is read once and written on save.
 * @param settings Where the theme and the recent colours are persisted. Null in tests that do not
 *   care about persistence.
 */
class DisplayTextViewModel(
    private val engine: StandaloneEngine,
    private val settings: AppSettings? = null,
) : ViewModel() {

    private val _saved = MutableStateFlow(startingPoint())
    private val _draft = MutableStateFlow(_saved.value)

    /** What the preview shows and the controls edit. */
    val draft: StateFlow<DisplayTextTheme> = _draft.asStateFlow()

    private val _element = MutableStateFlow(TextElement.BIBLE_VERSE)

    /** The element whose controls are showing. */
    val element: StateFlow<TextElement> = _element.asStateFlow()

    private val _hasChanges = MutableStateFlow(false)

    /** Whether the draft differs from what is on the output. */
    val hasChanges: StateFlow<Boolean> = _hasChanges.asStateFlow()

    private val _recentColors = MutableStateFlow(settings?.displayRecentColors.orEmpty())

    /** Colours last picked, newest first. */
    val recentColors: StateFlow<List<String>> = _recentColors.asStateFlow()

    /** The look around the words — backdrop and colours — for the preview to sit on. */
    val baseTheme: StateFlow<SlideTheme> = engine.theme

    /** What the output is showing now, for previewing the real slide rather than a sample. */
    val liveSlide: StateFlow<Slide> = engine.currentSlide

    fun select(element: TextElement) {
        _element.value = element
    }

    /**
     * Switches between the Bible and song elements, landing on the main words of [kind] unless
     * an element of that kind is already selected.
     */
    fun selectGroup(kind: SlideKind) {
        if (_element.value.kind == kind) return
        _element.value = if (kind == SlideKind.SONG) TextElement.SONG_LYRICS else TextElement.BIBLE_VERSE
    }

    /** Changes the selected element's style. */
    fun updateStyle(block: (TextElementStyle) -> TextElementStyle) {
        val element = _element.value
        update { it.withStyle(element, block(it.style(element))) }
    }

    /** Changes a setting that belongs to the whole of Display text rather than one element. */
    fun update(block: (DisplayTextTheme) -> DisplayTextTheme) {
        _draft.value = block(_draft.value)
        _hasChanges.value = _draft.value != _saved.value
    }

    /** Puts the selected element back to its defaults. Still a draft until [save]. */
    fun resetElement() {
        val element = _element.value
        update { it.resetting(element, DisplayTextTheme()) }
    }

    /** Remembers [hex] at the front of the recent colours. */
    fun rememberColor(hex: String) {
        val updated = (listOf(hex) + _recentColors.value.filterNot { it.equals(hex, ignoreCase = true) })
            .take(MAX_RECENT_COLORS)
        _recentColors.value = updated
        settings?.displayRecentColors = updated
    }

    /**
     * Puts the draft on the output and keeps it.
     *
     * Read-modify-write of the engine's theme at the moment of saving, so a backdrop colour
     * changed in the Look sheet while this screen was open is kept rather than overwritten.
     */
    fun save() {
        val theme = engine.theme.value.copy(display = _draft.value)
        engine.setTheme(theme)
        settings?.let { store ->
            runCatching { store.slideThemeJson = SlideThemeCodec.encode(theme) }
                .onFailure { Logger.e(TAG, "could not save display text: ${it.message}") }
        }
        _saved.value = _draft.value
        _hasChanges.value = false
    }

    /**
     * The saved styling, or — the first time — one seeded from the theme's own font and colour,
     * so the screen opens on what the output already looks like.
     */
    private fun startingPoint(): DisplayTextTheme {
        val theme = engine.theme.value
        return theme.display ?: DisplayTextTheme.seededFrom(theme, engine.textSize.value)
    }

    private companion object {
        const val TAG = "DisplayTextViewModel"
        const val MAX_RECENT_COLORS = 6
    }
}
