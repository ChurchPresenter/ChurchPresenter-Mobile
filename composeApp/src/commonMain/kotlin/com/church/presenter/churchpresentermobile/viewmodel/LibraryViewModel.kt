package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.library.LibrarySearch
import com.church.presenter.churchpresentermobile.model.LibraryData
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.model.SlideDeckBuilder
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Which slice of the library the list is showing. */
enum class LibraryFilter {
    ALL,
    SONGS,
    ANNOUNCEMENTS,
    SETLISTS,
}

/**
 * Backs the library browser.
 *
 * Search runs in memory over the whole library — the same approach the Songs
 * tab already takes against the desktop catalogue, and fuzzier than the token
 * matching a database index would give.
 *
 * The Library keeps content; it does not project it. Tapping a row here used to
 * put the item straight on the audience screen, so browsing your own library
 * projected it. Going live belongs to the presenting surfaces — the Songs tab
 * for songs, [com.church.presenter.churchpresentermobile.ui.standalone.LocalNoticesScreen]
 * for notices — which is why this holds no presenter.
 */
class LibraryViewModel(
    private val repository: LibraryRepository,
) : ViewModel() {

    val library: StateFlow<LibraryData> = repository.library

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(LibraryFilter.ALL)
    val filter: StateFlow<LibraryFilter> = _filter.asStateFlow()

    init {
        repository.load()
    }

    fun setQuery(value: String) { _query.value = value }

    fun setFilter(value: LibraryFilter) { _filter.value = value }

    /** Songs matching the current query, best match first. */
    fun visibleSongs(): List<LocalSong> =
        if (_filter.value == LibraryFilter.ALL || _filter.value == LibraryFilter.SONGS) {
            LibrarySearch.songs(library.value.songs, _query.value)
        } else {
            emptyList()
        }

    /** Announcements matching the current query, best match first. */
    fun visibleAnnouncements(): List<LocalAnnouncement> =
        if (_filter.value == LibraryFilter.ALL || _filter.value == LibraryFilter.ANNOUNCEMENTS) {
            LibrarySearch.announcements(library.value.announcements, _query.value)
        } else {
            emptyList()
        }

    fun deleteSong(id: String) = repository.deleteSong(id)

    fun deleteAnnouncement(id: String) = repository.deleteAnnouncement(id)
}
