package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideBackdrop
import com.church.presenter.churchpresentermobile.model.SlideDeck
import com.church.presenter.churchpresentermobile.model.SlideTextSize
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.SlideTheme
import kotlinx.serialization.json.Json
import com.church.presenter.churchpresentermobile.present.SinkRegistry
import com.church.presenter.churchpresentermobile.present.SinkStatus
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import kotlinx.coroutines.flow.StateFlow

/**
 * Backs the standalone controller screen.
 *
 * Deliberately thin: the projection state itself lives in [StandaloneEngine],
 * which outlives any one screen and is also written to by the Songs and Bible
 * ViewModels when the operator loads content. This ViewModel re-exposes the
 * engine's flows for the UI and turns taps into engine calls, so there is one
 * source of truth for what is on screen.
 *
 * @param engine The process-wide presenter engine.
 * @param registry Attached output sinks, for the "casting to…" chip.
 */
class StandaloneViewModel(
    private val engine: StandaloneEngine,
    private val registry: SinkRegistry,
    private val settings: AppSettings? = null,
) : ViewModel() {

    val deck: StateFlow<SlideDeck> = engine.deck
    val index: StateFlow<Int> = engine.index
    val currentSlide: StateFlow<Slide> = engine.currentSlide
    val isBlank: StateFlow<Boolean> = engine.isBlank
    val isLive: StateFlow<Boolean> = engine.isLive
    val textSize: StateFlow<SlideTextSize> = engine.textSize
    val backdrop: StateFlow<SlideBackdrop> = engine.backdrop
    val theme: StateFlow<SlideTheme> = engine.theme

    init {
        // A church sets its colours once. Without this they would be back to the built-in
        // purple every Sunday morning.
        settings?.let { stored ->
            runCatching { themeJson.decodeFromString<SlideTheme>(stored.slideThemeJson) }
                .getOrNull()
                ?.let { engine.setTheme(it) }
        }
    }

    /** Status of every registered sink, for the outputs chip and sheet. */
    val sinks: StateFlow<List<SinkStatus>> = registry.statuses

    fun showSlide(index: Int) = engine.showSlide(index)

    fun next() = engine.next()

    fun previous() = engine.previous()

    fun toggleBlank() = engine.toggleBlank()

    fun setLive(live: Boolean) = engine.setLive(live)

    fun setTextSize(size: SlideTextSize) = engine.setTextSize(size)

    fun setBackdrop(backdrop: SlideBackdrop) = engine.setBackdrop(backdrop)

    /**
     * Changes one part of the look, leaving the rest as it was.
     *
     * The theme travels inside every slide, so a change here reaches the phone's own output,
     * an attached screen and any browser watching the hosted page, without any of them being
     * told separately.
     */
    fun updateTheme(block: (SlideTheme) -> SlideTheme) {
        val updated = block(engine.theme.value)
        engine.setTheme(updated)
        settings?.let { store ->
            runCatching { store.slideThemeJson = themeJson.encodeToString(SlideTheme.serializer(), updated) }
        }
    }

    /** Unloads the current deck and blanks the screen. */
    fun clear() = engine.clear()
}

/** Lenient so a theme written by a newer build still opens on an older one. */
private val themeJson = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }
