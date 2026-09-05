package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.model.SlideDeckBuilder
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private const val TAG = "LocalNoticesViewModel"

/**
 * The notices kept on this device, and the press that puts one on the screen.
 *
 * Notices are written in the Library tab, but projecting them does not belong
 * there: the Library is for keeping content, and tapping a row there used to
 * put it live with no second press. This is the presenting surface for notices,
 * the way the Songs tab is for songs.
 *
 * @param presenter The local presenter. Null in remote mode, where this screen
 *   is not reachable — the remote Announcements screen drives a desktop instead.
 */
class LocalNoticesViewModel(
    private val repository: LibraryRepository,
    private val presenter: StandaloneEngine? = null,
) : ViewModel() {

    /** Every notice in the library, following edits made in the Library tab. */
    val notices: StateFlow<List<LocalAnnouncement>> =
        repository.library
            .map { it.announcements }
            .stateIn(viewModelScope, SharingStarted.Eagerly, repository.announcements)

    private val _liveId = MutableStateFlow<String?>(null)

    /** The notice currently on the screen, so the list can show which one it is. */
    val liveId: StateFlow<String?> = _liveId.asStateFlow()

    /**
     * Puts [announcement] on the audience screen.
     *
     * Supplies the deck on every press rather than relying on one loaded
     * earlier, so this works whatever cleared the screen beforehand.
     */
    fun project(announcement: LocalAnnouncement) {
        val presenter = presenter ?: return
        presenter.setDeck(
            SlideDeckBuilder.fromAnnouncement(
                text = announcement.body,
                title = announcement.title.takeIf { it.isNotBlank() },
                id = announcement.id,
            )
        )
        _liveId.value = announcement.id
        Logger.d(TAG, "project — notice ${announcement.id} is live")
    }

    /** Takes the notice off the screen. */
    fun clear() {
        presenter?.clear()
        _liveId.value = null
        Logger.d(TAG, "clear — screen cleared")
    }
}
