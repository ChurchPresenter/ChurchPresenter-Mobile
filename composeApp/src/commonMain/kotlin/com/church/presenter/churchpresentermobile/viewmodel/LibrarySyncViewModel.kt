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

    fun sync() {
        if (job?.isActive == true) return
        _outcome.value = null
        job = viewModelScope.launch {
            val result = service.sync()
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

    /** Stops a sync in flight. Whatever was already written stays. */
    fun cancel() {
        job?.cancel()
        job = null
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
