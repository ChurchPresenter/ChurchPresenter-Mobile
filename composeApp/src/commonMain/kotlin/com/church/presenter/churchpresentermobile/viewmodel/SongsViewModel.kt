package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.model.ApiException
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.DemoData
import com.church.presenter.churchpresentermobile.model.Song
import com.church.presenter.churchpresentermobile.model.SongDetail
import com.church.presenter.churchpresentermobile.model.ToastEvent
import com.church.presenter.churchpresentermobile.network.SongService
import com.church.presenter.churchpresentermobile.network.recordNetworkError
import com.church.presenter.churchpresentermobile.util.Analytics
import com.church.presenter.churchpresentermobile.util.AnalyticsEvent
import com.church.presenter.churchpresentermobile.util.AnalyticsParam
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "SongsViewModel"

class SongsViewModel(private val appSettings: AppSettings, private val isDemoMode: Boolean = false) : ViewModel() {
    private var songService = SongService(appSettings)

    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())

    private val _selectedSong = MutableStateFlow<Song?>(null)
    val selectedSong = _selectedSong.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedBook = MutableStateFlow<String?>(null)
    val selectedBook = _selectedBook.asStateFlow()

    // ── Song detail (bottom sheet) ────────────────────────────────────────
    private val _songDetail = MutableStateFlow<SongDetail?>(null)
    val songDetail = _songDetail.asStateFlow()

    private val _isLoadingDetail = MutableStateFlow(false)
    val isLoadingDetail = _isLoadingDetail.asStateFlow()

    private val _detailError = MutableStateFlow<String?>(null)
    val detailError = _detailError.asStateFlow()

    private val _selectedVerseIndex = MutableStateFlow<Int?>(null)
    val selectedVerseIndex = _selectedVerseIndex.asStateFlow()

    private val _isProjecting = MutableStateFlow(false)
    val isProjecting = _isProjecting.asStateFlow()

    private val _toastEvent = MutableStateFlow<ToastEvent?>(null)
    val toastEvent = _toastEvent.asStateFlow()

    private val _scheduleRefreshTrigger = MutableStateFlow(0)
    val scheduleRefreshTrigger = _scheduleRefreshTrigger.asStateFlow()

    private val _scheduleAdded = MutableStateFlow(false)
    val scheduleAdded = _scheduleAdded.asStateFlow()
    // ─────────────────────────────────────────────────────────────────────

    val availableBooks = _allSongs
        .combine(MutableStateFlow(Unit)) { songs, _ ->
            songs.mapNotNull { it.bookName }.distinct().sorted()
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val songs = combine(
        _allSongs, _searchQuery, _selectedBook
    ) { songs, query, book ->
        songs
            .filter { song -> book == null || song.bookName == book }
            .filter { song ->
                if (query.isBlank()) true
                else song.number.startsWith(query.trim(), ignoreCase = true) ||
                        song.title.contains(query.trim(), ignoreCase = true)
            }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val hasActiveFilter = combine(_searchQuery, _selectedBook) { query, book ->
        query.isNotBlank() || book != null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ── Schedule-driven pending navigation ───────────────────────────────────

    private var pendingOpenTitle: String? = null
    private var pendingOpenBook: String? = null

    init { loadSongs() }

    /**
     * Token of the last [onSettingsSaved] call that was fully processed.
     * Guards against `LaunchedEffect(settingsSaveToken)` re-firing when the Songs tab
     * re-enters composition after being removed by `beyondViewportPageCount = 0`.
     */
    private var lastSettingsSaveToken = 0

    fun loadSongs(forceReload: Boolean = false) {
        if (isDemoMode) {
            Logger.d(TAG, "loadSongs — DEMO MODE")
            _allSongs.value = DemoData.songs
            _error.value = null
            tryOpenPendingSong()
            return
        }
        // If we already have a successful result, don't throw it away on a tab switch.
        // forceReload bypasses this guard for explicit refreshes and settings-change reloads.
        if (!forceReload && _allSongs.value.isNotEmpty() && !_isLoading.value) {
            Logger.d(TAG, "loadSongs — already loaded (${_allSongs.value.size} songs), skipping")
            tryOpenPendingSong()
            return
        }
        Logger.d(TAG, "loadSongs — url=${appSettings.apiBaseUrl}")
        // Set loading state synchronously so no frame can see empty data + isLoading=false
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                songService.getSongs()
                    .onSuccess {
                        _allSongs.value = it
                        tryOpenPendingSong()
                    }
                    .onFailure { e ->
                        Logger.e(TAG, "loadSongs — FAILED: ${e.message}", e)
                        _error.value = "Failed to load songs: ${e.recordNetworkError(TAG, "loadSongs")}"
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Explicit pull-to-refresh: always reloads even if songs are cached. */
    fun refresh() {
        loadSongs(forceReload = true)
    }

    /**
     * Opens the detail screen for the song whose title matches [title].
     * If songs haven't loaded yet the request is queued and processed once they do.
     *
     * @param title    Song title to match (case-insensitive).
     * @param bookName Optional songbook name to narrow the match.
     */
    fun openSongByTitle(title: String, bookName: String?) {
        Logger.d(TAG, "openSongByTitle — title='$title' book='$bookName'")
        pendingOpenTitle = title
        pendingOpenBook  = bookName
        tryOpenPendingSong()
    }

    /** Processes [pendingOpenTitle] if songs are already loaded. */
    private fun tryOpenPendingSong() {
        val title = pendingOpenTitle ?: return
        val book  = pendingOpenBook
        val songs = _allSongs.value
        if (songs.isEmpty()) return  // Will be retried after loadSongs() succeeds

        val song = songs.firstOrNull { s ->
            s.title.equals(title, ignoreCase = true) &&
            (book == null || s.bookName?.equals(book, ignoreCase = true) == true)
        } ?: songs.firstOrNull { s ->
            s.title.equals(title, ignoreCase = true)
        }

        if (song != null) {
            pendingOpenTitle = null
            pendingOpenBook  = null
            openSongDetail(song)
        } else {
            Logger.d(TAG, "tryOpenPendingSong — '$title' not found in ${songs.size} songs")
        }
    }

    /**
     * Fetches full details (lyrics) for [song] and sends the select request to the server.
     * In demo mode, serves [DemoData] lyrics without any API call.
     * Opens the detail bottom sheet.
     */
    fun openSongDetail(song: Song) {
        _selectedSong.value = song
        _songDetail.value = null
        _detailError.value = null
        Analytics.logEvent(AnalyticsEvent.SONG_OPENED)
        if (isDemoMode) {
            Logger.d(TAG, "openSongDetail — DEMO MODE for ${song.number}")
            _songDetail.value = DemoData.getSongDetail(song.number)
            return
        }
        viewModelScope.launch {
            _isLoadingDetail.value = true
            songService.getSongDetail(song.number, song.bookName)
                .onSuccess { detail ->
                    _songDetail.value = detail
                    Logger.d(TAG, "openSongDetail — loaded detail for ${song.number}, verses=${detail.allVerses.size}")
                }
                .onFailure { e ->
                    _detailError.value = "Failed to load song details: ${e.recordNetworkError(TAG, "openSongDetail")}"
                    Logger.e(TAG, "openSongDetail — FAILED: ${e.message}", e)
                }
            _isLoadingDetail.value = false

            // Also send the select notification
            Logger.d(TAG, "openSongDetail — firing selectSong for ${song.number}")
            songService.selectSong(song)
                .onSuccess { Logger.d(TAG, "selectSong — success") }
                .onFailure { e ->
                    Logger.e(TAG, "selectSong — FAILED: ${e.message}", e)
                    _error.value = "Failed to select song: ${e.recordNetworkError(TAG, "selectSong")}"
                }
        }
    }

    /** Closes the detail bottom sheet and clears the row highlight. */
    fun dismissSongDetail() {
        _songDetail.value = null
        _detailError.value = null
        _isLoadingDetail.value = false
        _selectedVerseIndex.value = null
        _selectedSong.value = null
        _isProjecting.value = false
        _toastEvent.value = null
        _scheduleAdded.value = false
    }

    /** Called after the UI has consumed a toast event. */
    fun toastShown() { _toastEvent.value = null }

    /** Toggles projection mode.
     *  In demo mode, simulates success without any API call.
     *  Turning ON fires POST /api/project to push the song live immediately.
     *  Turning OFF fires POST /api/clear to blank the display. */
    fun toggleProjecting() {
        val nowProjecting = !_isProjecting.value
        _isProjecting.value = nowProjecting
        if (!nowProjecting) {
            _selectedVerseIndex.value = null
            Analytics.logEvent(AnalyticsEvent.SONG_DISPLAY_CLEARED)
            if (isDemoMode) {
                Logger.d(TAG, "toggleProjecting OFF — DEMO MODE, skipping clear API call")
                return
            }
            Logger.d(TAG, "toggleProjecting OFF — firing clearDisplay")
            viewModelScope.launch {
                songService.clearDisplay()
                    .onSuccess { Logger.d(TAG, "clearDisplay — success") }
                    .onFailure { e ->
                        Logger.e(TAG, "clearDisplay — FAILED: ${e.message}", e)
                        e.recordNetworkError(TAG, "toggleProjecting/clearDisplay")
                    }
            }
            return
        }
        val song = _selectedSong.value
        if (song == null) {
            Logger.e(TAG, "toggleProjecting — _selectedSong is null, aborting")
            _isProjecting.value = false
            return
        }
        if (isDemoMode) {
            Logger.d(TAG, "toggleProjecting — DEMO MODE, simulating success for ${song.number}")
            _toastEvent.value = ToastEvent.SongLive
            return
        }
        Logger.d(TAG, "toggleProjecting — firing projectSong for ${song.number}")
        viewModelScope.launch {
            songService.projectSong(song)
                .onSuccess {
                    Logger.d(TAG, "projectSong — success")
                    Analytics.logEvent(AnalyticsEvent.SONG_PROJECTED)
                    _toastEvent.value = ToastEvent.SongLive
                }
                .onFailure { e ->
                    Logger.e(TAG, "projectSong — FAILED: ${e.message}", e)
                    _isProjecting.value = false
                    _toastEvent.value = e.toToastEvent { ToastEvent.FailedToProject(e.recordNetworkError(TAG, "toggleProjecting/projectSong")) }
                }
        }
    }

    /** Adds the current song to the schedule via POST /api/schedule/add.
     *  In demo mode, simulates success without any API call. */
    fun addSongToSchedule() {
        val song = _selectedSong.value
        if (song == null) {
            Logger.e(TAG, "addSongToSchedule — _selectedSong is null, aborting")
            _toastEvent.value = ToastEvent.NoSongSelected
            return
        }
        if (isDemoMode) {
            Logger.d(TAG, "addSongToSchedule — DEMO MODE, simulating success for ${song.number} / ${song.title}")
            _toastEvent.value = ToastEvent.SongAddedToSchedule(song.title)
            _scheduleAdded.value = true
            _scheduleRefreshTrigger.value++
            return
        }
        Logger.d(TAG, "addSongToSchedule — firing for ${song.number} / ${song.title}")
        viewModelScope.launch {
            songService.addSongToSchedule(song)
                .onSuccess {
                    Logger.d(TAG, "addSongToSchedule — success")
                    Analytics.logEvent(AnalyticsEvent.SONG_ADDED_TO_SCHEDULE)
                    _toastEvent.value = ToastEvent.SongAddedToSchedule(song.title)
                    _scheduleAdded.value = true
                    _scheduleRefreshTrigger.value++
                }
                .onFailure { e ->
                    Logger.e(TAG, "addSongToSchedule — FAILED: ${e.message}", e)
                    _toastEvent.value = e.toToastEvent { ToastEvent.FailedToAddSchedule(e.recordNetworkError(TAG, "addSongToSchedule")) }
                }
        }
    }

    fun onSettingsSaved(settingsSaveToken: Int = 0) {
        if (settingsSaveToken > 0 && settingsSaveToken == lastSettingsSaveToken) {
            Logger.d(TAG, "onSettingsSaved — token $settingsSaveToken already processed, skipping")
            return
        }
        lastSettingsSaveToken = settingsSaveToken
        Logger.d(TAG, "onSettingsSaved — new url=${appSettings.apiBaseUrl}")
        // Mark as loading immediately so the UI never sees empty-data + isLoading=false.
        // _allSongs is intentionally NOT cleared here — old songs remain visible while
        // the new server's songs load, preventing a "no songs" flash.
        _isLoading.value = true
        songService.closeClient()
        songService = SongService(appSettings)
        _selectedSong.value = null
        _selectedBook.value = null
        _searchQuery.value = ""
        _songDetail.value = null
        pendingOpenTitle = null
        pendingOpenBook  = null
        loadSongs(forceReload = true)
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSelectedBook(bookName: String?) { _selectedBook.value = bookName }

    /** Highlights [index] locally and notifies the server to project that verse.
     *  No-op when projection mode is off.
     *  In demo mode, skips the server notification. */
    fun selectVerse(index: Int) {
        if (!_isProjecting.value) return
        _selectedVerseIndex.value = index
        Analytics.logEvent(
            AnalyticsEvent.SONG_VERSE_SELECTED,
            mapOf(AnalyticsParam.VERSE_INDEX to index.toString())
        )
        if (isDemoMode) {
            Logger.d(TAG, "selectVerse — DEMO MODE, skipping API call")
            return
        }
        val song = _selectedSong.value ?: return
        Logger.d(TAG, "selectVerse — firing for ${song.number} section=$index bookName=${song.bookName}")
        viewModelScope.launch {
            songService.selectVerse(song.number, song.bookName, index)
                .onSuccess { Logger.d(TAG, "selectVerse — success") }
                .onFailure { e ->
                    Logger.e(TAG, "selectVerse — FAILED: ${e.message}", e)
                    _error.value = "Failed to select verse: ${e.recordNetworkError(TAG, "selectVerse")}"
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        songService.closeClient()
    }
}

/** Maps a network [Throwable] to a [ToastEvent], routing [ApiException]s to typed denial events. */
private fun Throwable.toToastEvent(fallback: () -> ToastEvent): ToastEvent =
    when (this) {
        is ApiException -> when (reason?.lowercase()) {
            "denied"  -> ToastEvent.RequestDenied
            "blocked" -> ToastEvent.SessionBlocked
            null      -> ToastEvent.RequestRejected(httpStatus)
            else      -> ToastEvent.RequestRejectedWithReason(reason)
        }
        else -> fallback()
    }


