package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.library.CURRENT_SERVICE_SETLIST_ID
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "LibraryClearViewModel"

/** What is on the device, in the terms the clear sheet has to state before deleting it. */
data class ClearableContent(
    val songCount: Int = 0,
    val noticeCount: Int = 0,
    val serviceEntryCount: Int = 0,
    val bibleCount: Int = 0,
    val bibleBytes: Long = 0L,
) {
    val hasSongs: Boolean get() = songCount > 0
    val hasBibles: Boolean get() = bibleCount > 0
    val isEmpty: Boolean get() = songCount == 0 && noticeCount == 0 &&
        serviceEntryCount == 0 && bibleCount == 0
}

/** Which wipe just finished, so the sheet can say so without inventing its own strings. */
enum class ClearOutcome { SONGS, BIBLES, EVERYTHING }

/**
 * Emptying this device's content while keeping it usable.
 *
 * A phone set up against the wrong computer, or handed on to another church,
 * previously had no way back short of deleting the app — which also threw away
 * the server address, the API key and the theme, the parts that are tedious to
 * type back in. So this deletes content only: nothing in [AppSettings] is
 * touched except the record of the last sync, which would otherwise go on
 * claiming a recent copy of songs that are gone.
 */
class LibraryClearViewModel(
    private val repository: LibraryRepository,
    private val bibles: LocalBibleRepository,
    private val settings: AppSettings,
) : ViewModel() {

    /** Live counts, so the confirm text names what is actually about to go. */
    val content: StateFlow<ClearableContent> =
        combine(repository.library, bibles.index) { library, index ->
            ClearableContent(
                songCount = library.songs.size,
                noticeCount = library.announcements.size,
                serviceEntryCount = library.setlists
                    .firstOrNull { it.id == CURRENT_SERVICE_SETLIST_ID }?.entries?.size ?: 0,
                bibleCount = index.bibles.size,
                bibleBytes = index.bibles.sumOf { it.sizeBytes },
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, ClearableContent())

    /** True while a wipe is running, so the sheet cannot start a second one. */
    private val _isClearing = MutableStateFlow(false)
    val isClearing: StateFlow<Boolean> = _isClearing.asStateFlow()

    private val _outcome = MutableStateFlow<ClearOutcome?>(null)
    val outcome: StateFlow<ClearOutcome?> = _outcome.asStateFlow()

    fun clearSongs() = clear(ClearOutcome.SONGS) {
        repository.clearSongs()
        forgetLastSync()
    }

    fun clearBibles() = clear(ClearOutcome.BIBLES) {
        bibles.clearAll()
    }

    fun clearEverything() = clear(ClearOutcome.EVERYTHING) {
        repository.clear()
        bibles.clearAll()
        forgetLastSync()
    }

    /**
     * Runs one wipe off the main dispatcher and reports it.
     *
     * Both repositories write their documents synchronously and a Bible module
     * is megabytes, so on the caller's dispatcher this is a visible freeze —
     * the same reason
     * [com.church.presenter.churchpresentermobile.library.LibrarySyncService.sync]
     * moves off it.
     */
    private fun clear(outcome: ClearOutcome, block: () -> Unit) {
        if (_isClearing.value) return
        _isClearing.value = true
        _outcome.value = null
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) { block() }
                _outcome.value = outcome
                Logger.d(TAG, "cleared $outcome")
            } finally {
                _isClearing.value = false
            }
        }
    }

    /**
     * Drops the "last copied N songs" record.
     *
     * Left alone, the Library tab's sync chip goes on reporting a successful
     * copy of a library that is now empty, which reads as the wipe having
     * failed.
     */
    private fun forgetLastSync() {
        runCatching { settings.librarySyncStateJson = "{}" }
            .onFailure { Logger.e(TAG, "could not reset the sync record: ${it.message}") }
    }
}

// Decimal rather than binary units: a phone's storage settings say MB for a
// million bytes, and a sheet asking permission to delete should agree with the
// figure the operator can go and check.
private const val BYTES_PER_KB = 1_000L
private const val BYTES_PER_MB = 1_000_000L

/** Bible sizes, for a sheet that has to justify deleting them. */
fun formatBytes(bytes: Long): String = when {
    bytes >= BYTES_PER_MB -> "${bytes / BYTES_PER_MB} MB"
    bytes >= BYTES_PER_KB -> "${bytes / BYTES_PER_KB} KB"
    else -> "$bytes B"
}
