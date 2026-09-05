package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.model.SlideDeckBuilder
import com.church.presenter.churchpresentermobile.present.PhotoLibrary
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.present.StoredPhoto
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private const val TAG = "LocalPhotosViewModel"

/**
 * Photos picked from this device and projected by it.
 *
 * The remote Photos screen browses the desktop's picture folders; with no
 * desktop there are no folders, which is why standalone used to open a screen
 * that could never fill. This is the same idea served locally: the operator
 * picks from the phone, and the phone projects.
 *
 * @param library Where picked photos live for the service.
 * @param presenter The local presenter. Null outside standalone, where every
 *   call on it would be a no-op anyway.
 */
class LocalPhotosViewModel(
    private val library: PhotoLibrary,
    private val presenter: StandaloneEngine?,
) : ViewModel() {

    /** The photos picked this session, in the order they were picked. */
    val photos: StateFlow<List<StoredPhoto>> = library.photos

    /**
     * True once photos can actually be shown.
     *
     * They are served by the phone's own presentation server, so until that is
     * running there is nowhere for either display to fetch them from — and a
     * slide pointing at an address that answers nothing is a black screen
     * mid-service.
     */
    val canProject: StateFlow<Boolean> = library.baseUrl
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, library.baseUrl.value != null)

    private val _projectingId = MutableStateFlow<String?>(null)

    /** Which photo is on the screen right now, if any. */
    val projectingId: StateFlow<String?> = _projectingId.asStateFlow()

    /** Adds a picked photo. Returns it so callers can project it straight away. */
    fun add(fileName: String, bytes: ByteArray): StoredPhoto = library.add(fileName, bytes)

    /** Forgets [id]; clears the screen too if that photo was on it. */
    fun remove(id: String) {
        if (_projectingId.value == id) clearDisplay()
        library.remove(id)
    }

    /**
     * Projects [photo].
     *
     * The whole picked set becomes the deck, not just this one, so the operator
     * can step through with the same next/previous they use for a song's
     * sections rather than coming back here between images.
     */
    fun project(photo: StoredPhoto) {
        val engine = presenter ?: return
        val projectable = library.photos.value.mapNotNull { stored ->
            library.urlFor(stored.id)?.let { stored to it }
        }
        val index = projectable.indexOfFirst { it.first.id == photo.id }
        if (index < 0) {
            Logger.e(TAG, "project — no URL for ${photo.fileName}; is the server running?")
            return
        }
        engine.setDeck(SlideDeckBuilder.fromPhotos(projectable.map { it.second }))
        engine.showSlide(index)
        _projectingId.value = photo.id
        Logger.d(TAG, "project — ${photo.fileName} (${index + 1}/${projectable.size})")
    }

    /** Takes the photo off the screen. */
    fun clearDisplay() {
        presenter?.clear()
        _projectingId.value = null
    }
}
