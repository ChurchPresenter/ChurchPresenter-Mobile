package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideBackdrop
import com.church.presenter.churchpresentermobile.model.SlideDeck
import com.church.presenter.churchpresentermobile.model.SlideTextSize
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.NamedTheme
import com.church.presenter.churchpresentermobile.model.SlideThemePresets
import com.church.presenter.churchpresentermobile.util.Logger
import com.church.presenter.churchpresentermobile.model.SlideTheme
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.present.PhotoLibrary
import com.church.presenter.churchpresentermobile.present.StoredPhoto
import com.church.presenter.churchpresentermobile.present.SinkRegistry
import com.church.presenter.churchpresentermobile.present.SinkStatus
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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
 * @param photos The device's photos, so one can be chosen as the backdrop. Null
 *   where the platform has no photo library, which simply hides the choice.
 */
class StandaloneViewModel(
    private val engine: StandaloneEngine,
    private val registry: SinkRegistry,
    private val settings: AppSettings? = null,
    private val photos: PhotoLibrary? = null,
) : ViewModel() {

    val deck: StateFlow<SlideDeck> = engine.deck
    val index: StateFlow<Int> = engine.index
    val currentSlide: StateFlow<Slide> = engine.currentSlide
    val isBlank: StateFlow<Boolean> = engine.isBlank
    val isLive: StateFlow<Boolean> = engine.isLive
    val textSize: StateFlow<SlideTextSize> = engine.textSize
    val backdrop: StateFlow<SlideBackdrop> = engine.backdrop
    val theme: StateFlow<SlideTheme> = engine.theme
    val backdropUrl: StateFlow<String?> = engine.backdropUrl

    /** Photos on this device, offered when the backdrop is an image. */
    val backdropPhotos: StateFlow<List<StoredPhoto>> = photos?.photos ?: MutableStateFlow(emptyList())

    /**
     * Whether a photo can be used as a backdrop yet.
     *
     * A backdrop travels as a URL, and photos are served by the embedded web
     * server, so there is no address to send until that server is running —
     * i.e. until the Browser screen is on. Same precondition the Photos screen
     * reports before it will project.
     */
    val canUsePhotoBackdrop: StateFlow<Boolean> =
        photos?.baseUrl?.map { it != null }
            ?.stateIn(viewModelScope, SharingStarted.Eagerly, photos.baseUrl.value != null)
            ?: MutableStateFlow(false)

    init {
        // A church sets its colours once. Without this they would be back to the built-in
        // purple every Sunday morning.
        settings?.let { stored ->
            runCatching { themeJson.decodeFromString<SlideTheme>(stored.slideThemeJson) }
                .getOrNull()
                ?.let { engine.setTheme(carryOldReferenceSetting(it, stored.slideThemeJson)) }
        }
    }

    /**
     * Whether the words are drawn with their chords.
     *
     * Part of the theme, so one switch reaches the phone, an attached screen and
     * any browser watching the hosted page — the theme rides inside every slide,
     * so none of them has to be told separately.
     */
    val showChords: StateFlow<Boolean> = engine.theme
        .map { it.showChords }
        .stateIn(viewModelScope, SharingStarted.Eagerly, engine.theme.value.showChords)

    fun setShowChords(show: Boolean) = updateTheme { it.copy(showChords = show) }

    /** Status of every registered sink, for the outputs chip and sheet. */
    val sinks: StateFlow<List<SinkStatus>> = registry.statuses

    fun showSlide(index: Int) = engine.showSlide(index)

    fun next() = engine.next()

    fun previous() = engine.previous()

    fun toggleBlank() = engine.toggleBlank()

    fun setLive(live: Boolean) = engine.setLive(live)

    fun setTextSize(size: SlideTextSize) = engine.setTextSize(size)

    /**
     * The photo last chosen as a backdrop.
     *
     * Remembered here because the engine clears its URL whenever the kind
     * changes — switching to gradient and back would otherwise land on IMAGE
     * with nothing to show, which is the state that made this look broken.
     */
    private var lastPhotoUrl: String? = null

    /**
     * Chooses the kind of backdrop.
     *
     * Picking IMAGE alone leaves the screen as it was unless a photo has already
     * been chosen: an image backdrop needs one, which [setImageBackdrop] supplies.
     */
    fun setBackdrop(backdrop: SlideBackdrop) = when (backdrop) {
        SlideBackdrop.IMAGE -> engine.setBackdrop(backdrop, lastPhotoUrl)
        else -> engine.setBackdrop(backdrop)
    }

    /**
     * Puts [photo] behind the words.
     *
     * Does nothing when the photo has no address yet — see [canUsePhotoBackdrop].
     * Silently setting an IMAGE backdrop with no URL is what made the Image
     * option look broken: the audience page falls back to the gradient, so the
     * button appeared to do nothing at all.
     */
    fun setImageBackdrop(photo: StoredPhoto) {
        val url = photos?.urlFor(photo.id) ?: return
        lastPhotoUrl = url
        engine.setBackdrop(SlideBackdrop.IMAGE, url)
    }

    private val _savedThemes = MutableStateFlow(readSavedThemes())

    /** Looks this church has saved, newest last. */
    val savedThemes: StateFlow<List<NamedTheme>> = _savedThemes.asStateFlow()

    /** Built-in looks to start from. */
    val presets: List<NamedTheme> = SlideThemePresets.all

    /**
     * Adopts [named]'s look.
     *
     * Copies the values in rather than remembering which entry they came from,
     * so deleting a saved look never changes what is on the screen.
     */
    fun applyTheme(named: NamedTheme) = updateTheme { named.theme }

    /** Saves the current look under [name], replacing any saved look of that name. */
    fun saveCurrentTheme(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val entry = NamedTheme(trimmed, engine.theme.value)
        writeSavedThemes(_savedThemes.value.filterNot { it.name == trimmed } + entry)
    }

    fun deleteSavedTheme(name: String) {
        writeSavedThemes(_savedThemes.value.filterNot { it.name == name })
    }

    private fun readSavedThemes(): List<NamedTheme> {
        val stored = settings?.savedThemesJson ?: return emptyList()
        return runCatching { themeJson.decodeFromString(ListSerializer(NamedTheme.serializer()), stored) }
            .onFailure { Logger.e(TAG, "could not read saved looks: ${it.message}") }
            .getOrDefault(emptyList())
    }

    private fun writeSavedThemes(themes: List<NamedTheme>) {
        _savedThemes.value = themes
        val store = settings ?: return
        runCatching {
            store.savedThemesJson = themeJson.encodeToString(ListSerializer(NamedTheme.serializer()), themes)
        }.onFailure { Logger.e(TAG, "could not save looks: ${it.message}") }
    }

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
/**
 * Honours a `showReference` saved before the setting was split by kind.
 *
 * The three new flags default to on, so a church that had turned the single one
 * off would find every heading back the next Sunday. Read the old value straight
 * out of the stored JSON — it is no longer a field, so decoding simply drops it —
 * and apply it to all three.
 */
private fun carryOldReferenceSetting(theme: SlideTheme, storedJson: String): SlideTheme {
    val legacy = runCatching {
        themeJson.parseToJsonElement(storedJson).jsonObject["showReference"]?.jsonPrimitive?.boolean
    }.getOrNull() ?: return theme
    if (legacy) return theme
    return theme.copy(
        showSongReference = false,
        showBibleReference = false,
        showOtherReference = false,
    )
}

private const val TAG = "StandaloneViewModel"

private val themeJson = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }
