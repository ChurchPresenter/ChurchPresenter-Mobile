package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.library.LibrarySyncService
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.LibrarySyncState
import com.church.presenter.churchpresentermobile.model.SyncOutcome
import com.church.presenter.churchpresentermobile.model.SyncProgress
import com.church.presenter.churchpresentermobile.network.SongService
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.time.Clock

private const val TAG = "LibrarySyncViewModel"

/**
 * Drives the desktop→library sync and remembers how it went.
 *
 * The state is persisted so the Library screen can answer "is this current?"
 * on a Sunday morning without touching the network.
 */
class LibrarySyncViewModel(
    private val repository: LibraryRepository,
    private val settings: AppSettings,
    private val songService: SongService,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val service = LibrarySyncService(
        repository = repository,
        fetchCatalogue = { songService.getSongs() },
        fetchDetail = { song -> songService.getSongDetail(song.number, song.bookName, song.id, song.title) },
        now = { Clock.System.now().toEpochMilliseconds() },
    )

    val progress: StateFlow<SyncProgress> = service.progress

    private val _state = MutableStateFlow(readState())
    val state: StateFlow<LibrarySyncState> = _state.asStateFlow()

    private val _outcome = MutableStateFlow<SyncOutcome?>(null)

    /** The result of the last finished sync, for the results banner. Cleared by [dismissOutcome]. */
    val outcome: StateFlow<SyncOutcome?> = _outcome.asStateFlow()

    private var job: Job? = null

    private val _books = MutableStateFlow<List<String>>(emptyList())

    /** The songbooks the computer offers, once [loadBooks] has asked it. */
    val books: StateFlow<List<String>> = _books.asStateFlow()

    private val _selectedBooks = MutableStateFlow<Set<String>>(emptySet())

    /** Which of [books] will be copied. Every book starts ticked. */
    val selectedBooks: StateFlow<Set<String>> = _selectedBooks.asStateFlow()

    private val _isLoadingBooks = MutableStateFlow(false)
    val isLoadingBooks: StateFlow<Boolean> = _isLoadingBooks.asStateFlow()

    /**
     * Asks the computer which songbooks it has.
     *
     * Reads them off the song catalogue, which already names each song's book,
     * rather than adding a second endpoint. Everything arrives ticked, so a user
     * who ignores this list and presses Sync gets what they got before.
     */
    fun loadBooks() {
        if (_isLoadingBooks.value) return
        _isLoadingBooks.value = true
        viewModelScope.launch {
            songService.getSongs()
                .onSuccess { songs ->
                    val names = songs.mapNotNull { it.bookName?.takeIf(String::isNotBlank) }
                        .distinct()
                        .sorted()
                    _books.value = names
                    _selectedBooks.value = names.toSet()
                    Logger.d(TAG, "loadBooks — ${names.size} songbooks offered")
                }
                .onFailure { Logger.e(TAG, "loadBooks — FAILED: ${it.message}") }
            _isLoadingBooks.value = false
        }
    }

    fun toggleBook(name: String) {
        val current = _selectedBooks.value
        _selectedBooks.value = if (name in current) current - name else current + name
    }

    /** True when there is something to copy — every book unticked is not a sync. */
    val canSync: Boolean
        get() = _books.value.isEmpty() || _selectedBooks.value.isNotEmpty()

    fun sync() {
        if (job?.isActive == true) return
        _outcome.value = null
        // Null while the books are unknown, which keeps the old whole-catalogue
        // behaviour for anyone who never opens the picker.
        val books = _selectedBooks.value.takeIf { _books.value.isNotEmpty() }
        job = viewModelScope.launch {
            val result = service.sync(books)
            _outcome.value = result
            if (result is SyncOutcome.Success) {
                writeState(
                    LibrarySyncState(
                        lastSyncEpochMs = Clock.System.now().toEpochMilliseconds(),
                        sourceHost = settings.host,
                        songCount = result.songCount,
                        failedCount = result.failedCount,
                        keptLocalCount = result.keptLocal,
                    )
                )
            }
        }
    }

    /**
     * Stops a sync in flight. Whatever was already written stays.
     *
     * Asks the service to stop rather than cancelling [job]: cancelling the
     * coroutine kills the very line that publishes the outcome, so the user saw
     * the sheet stop with no "cancelled" result. The job is still held, but only
     * as the re-entrancy guard in [sync].
     */
    fun cancel() {
        service.requestCancel()
    }

    fun dismissOutcome() { _outcome.value = null }

    private fun readState(): LibrarySyncState =
        runCatching { json.decodeFromString<LibrarySyncState>(settings.librarySyncStateJson) }
            .onFailure { Logger.e(TAG, "could not read sync state: ${it.message}") }
            .getOrDefault(LibrarySyncState.NEVER)

    private fun writeState(state: LibrarySyncState) {
        _state.value = state
        runCatching {
            settings.librarySyncStateJson = json.encodeToString(LibrarySyncState.serializer(), state)
        }.onFailure { Logger.e(TAG, "could not save sync state: ${it.message}") }
    }

    override fun onCleared() {
        super.onCleared()
        songService.closeClient()
    }
}
