package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.model.ApiException
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.BibleBook
import com.church.presenter.churchpresentermobile.model.BibleVerse
import com.church.presenter.churchpresentermobile.model.DemoData
import com.church.presenter.churchpresentermobile.model.ToastEvent
import com.church.presenter.churchpresentermobile.network.BibleService
import com.church.presenter.churchpresentermobile.network.recordNetworkError
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "BibleViewModel"

/**
 * Manages Bible navigation state: book list → chapter selection → verse display.
 *
 * @param appSettings The shared [AppSettings] instance used to configure the API service.
 * @param isDemoMode  When true, demo content from [DemoData] is used instead of live API calls.
 */
class BibleViewModel(private val appSettings: AppSettings, private val isDemoMode: Boolean = false) : ViewModel() {
    private var bibleService = BibleService(appSettings)

    // ── Books ─────────────────────────────────────────────────────────────────

    private val _allBooks = MutableStateFlow<List<BibleBook>>(emptyList())

    private val _bookSearchQuery = MutableStateFlow("")
    val bookSearchQuery = _bookSearchQuery.asStateFlow()

    /** Books filtered by [bookSearchQuery]. */
    val books = combine(_allBooks, _bookSearchQuery) { books, query ->
        if (query.isBlank()) books
        else books.filter { it.displayName.contains(query.trim(), ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Navigation state ──────────────────────────────────────────────────────

    private val _selectedBook = MutableStateFlow<BibleBook?>(null)
    /** The currently open book, or null when showing the books list. */
    val selectedBook = _selectedBook.asStateFlow()

    /** 1-based position of [_selectedBook] in the full (unfiltered) books list. */
    private val _selectedBookNumber = MutableStateFlow<Int?>(null)

    private val _selectedChapter = MutableStateFlow<Int?>(null)
    /** The currently open chapter number, or null when showing the chapters grid. */
    val selectedChapter = _selectedChapter.asStateFlow()

    // ── Verses ────────────────────────────────────────────────────────────────

    private val _verses = MutableStateFlow<List<BibleVerse>>(emptyList())
    val verses = _verses.asStateFlow()

    // ── Projection / schedule ─────────────────────────────────────────────────

    private val _isProjecting = MutableStateFlow(false)
    val isProjecting = _isProjecting.asStateFlow()

    /** 0-based indices of all verses the user has tapped to select (multi-select). */
    private val _selectedVerseIndices = MutableStateFlow<Set<Int>>(emptySet())
    val selectedVerseIndices = _selectedVerseIndices.asStateFlow()

    /** 0-based index of the verse currently being projected on screen, or null. */
    private val _projectedVerseIndex = MutableStateFlow<Int?>(null)
    val projectedVerseIndex = _projectedVerseIndex.asStateFlow()

    private val _scheduleAdded = MutableStateFlow(false)
    val scheduleAdded = _scheduleAdded.asStateFlow()

    /** Incremented each time a Bible passage is successfully added to the schedule;
     *  triggers a schedule drawer reload in the UI layer. */
    private val _scheduleRefreshTrigger = MutableStateFlow(0)
    val scheduleRefreshTrigger = _scheduleRefreshTrigger.asStateFlow()

    private val _toastEvent = MutableStateFlow<ToastEvent?>(null)
    val toastEvent = _toastEvent.asStateFlow()

    // ── Loading / error ───────────────────────────────────────────────────────

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    // ── Schedule-driven pending navigation ───────────────────────────────────

    /** Set of verse numbers to auto-select once chapter verses finish loading. */
    private var pendingInitialVerseNumbers: Set<Int> = emptySet()

    /** Queued navigation from schedule drawer (processed once books are loaded). */
    private var pendingNavBook: String? = null
    private var pendingNavChapter: Int? = null
    private var pendingNavVerseNumbers: Set<Int> = emptySet()

    init {
        loadBooks()
    }

    // ── Public actions ────────────────────────────────────────────────────────

    /**
     * Token of the last [onSettingsSaved] call that was fully processed.
     * Guards against `LaunchedEffect(settingsSaveToken)` re-firing when the Bible tab
     * re-enters composition after being removed by `beyondViewportPageCount = 0`.
     */
    private var lastSettingsSaveToken = 0

    /**
     * Loads the full list of Bible books.
     * In demo mode, returns hardcoded [DemoData.books] without any API call.
     *
     * @param forceReload When true, bypasses the "already loaded" guard (used by
     *                    pull-to-refresh and settings-save reload).
     */
    fun loadBooks(forceReload: Boolean = false) {
        if (isDemoMode) {
            Logger.d(TAG, "loadBooks — DEMO MODE")
            _allBooks.value = DemoData.books
            _error.value = null
            tryProcessPendingNav()
            return
        }
        // If we already have a successful result, don't throw it away on a tab switch.
        // forceReload bypasses this guard for explicit refreshes and settings-change reloads.
        if (!forceReload && _allBooks.value.isNotEmpty() && !_isLoading.value) {
            Logger.d(TAG, "loadBooks — already loaded (${_allBooks.value.size} books), skipping")
            tryProcessPendingNav()
            return
        }
        Logger.d(TAG, "loadBooks — url=${appSettings.apiBaseUrl}")
        // Set loading state synchronously so no frame can see empty data + isLoading=false
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                bibleService.getBooks()
                    .onSuccess {
                        _allBooks.value = it
                        tryProcessPendingNav()
                    }
                    .onFailure { e ->
                        Logger.e(TAG, "loadBooks — FAILED: ${e.message}", e)
                        _error.value = "Failed to load Bible books: ${e.recordNetworkError(TAG, "loadBooks")}"
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Navigates to a specific book and chapter, optionally pre-selecting a set of verses.
     * Handles any combination of formats (single verse, range, comma-separated list).
     * If books have not finished loading yet the request is queued and processed once they do.
     *
     * @param bookName     Display name of the Bible book (case-insensitive match).
     * @param chapter      1-based chapter number.
     * @param verseNumbers Set of 1-based verse numbers to pre-select (may be empty).
     */
    fun navigateToBookAndChapter(
        bookName: String,
        chapter: Int,
        verseNumbers: Set<Int> = emptySet()
    ) {
        Logger.d(TAG, "navigateToBookAndChapter — $bookName $chapter verses=$verseNumbers")
        pendingNavBook         = bookName
        pendingNavChapter      = chapter
        pendingNavVerseNumbers = verseNumbers
        tryProcessPendingNav()
    }

    /** Processes [pendingNavBook]/[pendingNavChapter] if books are already loaded. */
    private fun tryProcessPendingNav() {
        val bookName = pendingNavBook    ?: return
        val chapter  = pendingNavChapter ?: return
        val books    = _allBooks.value
        if (books.isEmpty()) return  // Will be retried after loadBooks() succeeds

        val book = books.firstOrNull {
            it.displayName.equals(bookName, ignoreCase = true)
        } ?: books.firstOrNull {
            it.bookName?.equals(bookName, ignoreCase = true) == true ||
            it.name?.equals(bookName, ignoreCase = true) == true
        }
        if (book == null) {
            Logger.d(TAG, "tryProcessPendingNav — book '$bookName' not found in ${books.size} books")
            return
        }

        pendingInitialVerseNumbers = pendingNavVerseNumbers
        pendingNavBook         = null
        pendingNavChapter      = null
        pendingNavVerseNumbers = emptySet()

        selectBook(book)
        selectChapter(chapter)
    }

    /**
     * Opens a book and shows its chapter list.
     * Derives the 1-based book number from the book's position in the unfiltered list.
     *
     * @param book The [BibleBook] the user tapped.
     */
    fun selectBook(book: BibleBook) {
        val bookNumber = _allBooks.value.indexOf(book) + 1
        Logger.d(TAG, "selectBook — ${book.displayName} (bookNumber=$bookNumber)")
        _selectedBook.value = book
        _selectedBookNumber.value = bookNumber
        _selectedChapter.value = null
        _verses.value = emptyList()
    }

    /**
     * Opens a chapter and loads its verses.
     * In demo mode, returns hardcoded verses from [DemoData] without any API call.
     *
     * @param chapter The 1-based chapter number the user tapped.
     */
    fun selectChapter(chapter: Int) {
        val book = _selectedBook.value ?: return
        val bookNumber = _selectedBookNumber.value ?: return
        Logger.d(TAG, "selectChapter — ${book.displayName} (bookNumber=$bookNumber) chapter $chapter")
        _selectedChapter.value = chapter
        _verses.value = emptyList()
        _isProjecting.value = false
        _selectedVerseIndices.value = emptySet()
        _projectedVerseIndex.value = null
        _scheduleAdded.value = false

        if (isDemoMode) {
            Logger.d(TAG, "selectChapter — DEMO MODE, serving demo verses")
            val verses = DemoData.getVerses(book.displayName, chapter)
            _verses.value = verses
            val targets = pendingInitialVerseNumbers
            if (targets.isNotEmpty()) {
                pendingInitialVerseNumbers = emptySet()
                val indices = verses.mapIndexedNotNull { idx, v -> if (v.number in targets) idx else null }.toSet()
                if (indices.isNotEmpty()) _selectedVerseIndices.value = indices
            }
            return
        }

        // Set loading state synchronously so no frame sees empty verses + isLoading=false
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                bibleService.getChapter(bookNumber, chapter)
                    .onSuccess { verses ->
                        _verses.value = verses
                        // Auto-select verses requested by schedule navigation, if any
                        val targets = pendingInitialVerseNumbers
                        if (targets.isNotEmpty()) {
                            pendingInitialVerseNumbers = emptySet()
                            val indices = verses
                                .mapIndexedNotNull { idx, v ->
                                    if (v.number in targets) idx else null
                                }
                                .toSet()
                            if (indices.isNotEmpty()) _selectedVerseIndices.value = indices
                        }
                    }
                    .onFailure { e ->
                        Logger.e(TAG, "selectChapter — FAILED: ${e.message}", e)
                        _error.value = "Failed to load chapter: ${e.recordNetworkError(TAG, "selectChapter")}"
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Navigates back one level (verses → chapters, chapters → books). */
    fun navigateBack() {
        when {
            _selectedChapter.value != null -> {
                _selectedChapter.value = null
                _verses.value = emptyList()
                _error.value = null
                _isProjecting.value = false
                _selectedVerseIndices.value = emptySet()
                _projectedVerseIndex.value = null
                _scheduleAdded.value = false
            }
            _selectedBook.value != null -> {
                _selectedBook.value = null
                _selectedBookNumber.value = null
                _error.value = null
            }
        }
    }

    /**
     * Toggles projection mode. Turning ON sends the selected verses to the presenter live.
     * In demo mode, simulates success without any API call.
     * Turning OFF stops projecting and clears the projected verse.
     */
    fun toggleProjecting() {
        val book    = _selectedBook.value    ?: return
        val chapter = _selectedChapter.value ?: return
        if (_isProjecting.value) {
            _isProjecting.value        = false
            _projectedVerseIndex.value = null
            if (isDemoMode) {
                Logger.d(TAG, "toggleProjecting OFF — DEMO MODE, skipping clear API call")
                return
            }
            Logger.d(TAG, "toggleProjecting OFF — firing clearDisplay")
            viewModelScope.launch {
                bibleService.clearDisplay()
                    .onSuccess { Logger.d(TAG, "clearDisplay — success") }
                    .onFailure { e ->
                        Logger.e(TAG, "clearDisplay — FAILED: ${e.message}", e)
                        e.recordNetworkError(TAG, "toggleProjecting/clearDisplay")
                    }
            }
            return
        }
        val selectedVerses = _selectedVerseIndices.value
            .sorted()
            .mapNotNull { _verses.value.getOrNull(it) }
        val firstVerse = selectedVerses.firstOrNull()
        if (firstVerse == null) {
            _toastEvent.value = ToastEvent.FailedToProjectBible("Select at least one verse first")
            return
        }
        if (isDemoMode) {
            Logger.d(TAG, "toggleProjecting — DEMO MODE, simulating success")
            _isProjecting.value        = true
            _projectedVerseIndex.value = _selectedVerseIndices.value.minOrNull()
            _toastEvent.value          = ToastEvent.BibleLive
            return
        }
        Logger.d(TAG, "toggleProjecting ON — firing selectBibleVerse ${book.displayName} $chapter:${firstVerse.number}")
        viewModelScope.launch {
            bibleService.selectBibleVerse(
                bookName    = book.displayName,
                chapter     = chapter,
                verseNumber = firstVerse.number,
                verseText   = selectedVerses.joinToString("\n") { it.displayText }
            ).onSuccess {
                Logger.d(TAG, "selectBibleVerse — success")
                _isProjecting.value        = true
                _projectedVerseIndex.value = _selectedVerseIndices.value.minOrNull()
                _toastEvent.value          = ToastEvent.BibleLive
            }.onFailure { e ->
                Logger.e(TAG, "selectBibleVerse — FAILED: ${e.message}", e)
                _toastEvent.value = ToastEvent.FailedToProjectBible(e.recordNetworkError(TAG, "toggleProjecting/selectBibleVerse"))
            }
        }
    }

    /**
     * Toggles whether the verse at [index] is in the multi-select set.
     * Always available — does not require projecting to be active.
     */
    fun toggleVerseSelection(index: Int) {
        val current = _selectedVerseIndices.value
        _selectedVerseIndices.value =
            if (index in current) current - index else current + index
        // If projecting, immediately project the tapped verse
        if (_isProjecting.value) projectVerseAtIndex(index)
    }

    /**
     * Sends a specific verse to the projector while projecting mode is active.
     * Also adds it to the selection set.
     */
    fun selectVerse(index: Int) {
        if (!_isProjecting.value) return
        _projectedVerseIndex.value = index
        // ensure it is in the selection set
        _selectedVerseIndices.value = _selectedVerseIndices.value + index
        projectVerseAtIndex(index)
    }

    private fun projectVerseAtIndex(index: Int) {
        val book    = _selectedBook.value    ?: return
        val chapter = _selectedChapter.value ?: return
        val verse   = _verses.value.getOrNull(index) ?: return
        _projectedVerseIndex.value = index
        if (isDemoMode) {
            Logger.d(TAG, "projectVerseAtIndex — DEMO MODE, skipping API call")
            return
        }
        Logger.d(TAG, "projectVerseAtIndex — firing selectBibleVerse ${book.displayName} $chapter:${verse.number} index=$index")
        viewModelScope.launch {
            bibleService.selectBibleVerse(
                bookName    = book.displayName,
                chapter     = chapter,
                verseNumber = verse.number,
                verseText   = verse.displayText
            ).onSuccess {
                Logger.d(TAG, "selectBibleVerse — success")
            }.onFailure { e ->
                Logger.e(TAG, "selectBibleVerse — FAILED: ${e.message}", e)
                _toastEvent.value = ToastEvent.FailedToProjectBible(e.recordNetworkError(TAG, "projectVerseAtIndex/selectBibleVerse"))
            }
        }
    }

    /**
     * Adds all currently selected verses to the schedule without going live.
     * In demo mode, simulates success without any API call.
     * Shows an error toast if no verses are selected.
     */
    fun addToSchedule() {
        val book    = _selectedBook.value    ?: return
        val chapter = _selectedChapter.value ?: return
        val indices = _selectedVerseIndices.value.sorted()
        if (indices.isEmpty()) {
            _toastEvent.value = ToastEvent.FailedToAddBibleSchedule("Select at least one verse first")
            return
        }
        val selectedVerses = indices.mapNotNull { _verses.value.getOrNull(it) }
        val nums = selectedVerses.map { it.number }
        val ref = if (nums.size == 1)
            "${book.displayName} $chapter:${nums.first()}"
        else
            "${book.displayName} $chapter:${nums.first()}-${nums.last()}"
        if (isDemoMode) {
            Logger.d(TAG, "addToSchedule — DEMO MODE, simulating success")
            _scheduleAdded.value = true
            _scheduleRefreshTrigger.value++
            _toastEvent.value = ToastEvent.BibleAddedToSchedule(ref)
            return
        }
        Logger.d(TAG, "addToSchedule — firing addBibleToSchedule for $ref (${selectedVerses.size} verses)")
        viewModelScope.launch {
            bibleService.addBibleToSchedule(
                bookName = book.displayName,
                chapter  = chapter,
                verses   = selectedVerses
            ).onSuccess {
                Logger.d(TAG, "addBibleToSchedule — success")
                _scheduleAdded.value = true
                _scheduleRefreshTrigger.value++
                _toastEvent.value = ToastEvent.BibleAddedToSchedule(ref)
            }.onFailure { e ->
                Logger.e(TAG, "addBibleToSchedule — FAILED: ${e.message}", e)
                _toastEvent.value = e.toToastEvent { ToastEvent.FailedToAddBibleSchedule(e.recordNetworkError(TAG, "addToSchedule/addBibleToSchedule")) }
            }
        }
    }

    /** Called after the UI has consumed a toast event. */
    fun toastShown() { _toastEvent.value = null }

    /** Returns true if the back action is currently meaningful (not at the top-level book list). */
    val canNavigateBack: Boolean
        get() = _selectedBook.value != null

    /** Updates the search query used to filter the books list. */
    fun setBookSearchQuery(query: String) {
        _bookSearchQuery.value = query
    }

    /**
     * Refreshes the current navigation level:
     * - Verses view → re-fetches the current chapter
     * - Books list → reloads the full books list
     * - Chapter grid → nothing to reload (chapter list is derived from book data)
     */
    fun refresh() {
        val chapter = _selectedChapter.value
        when {
            chapter != null -> selectChapter(chapter)
            _selectedBook.value == null -> loadBooks(forceReload = true)
        }
    }

    /**
     * Rebuilds the HTTP service with latest settings and reloads books.
     * Call this after the user saves new settings.
     */
    fun onSettingsSaved(settingsSaveToken: Int = 0) {
        if (settingsSaveToken > 0 && settingsSaveToken == lastSettingsSaveToken) {
            Logger.d(TAG, "onSettingsSaved — token $settingsSaveToken already processed, skipping")
            return
        }
        lastSettingsSaveToken = settingsSaveToken
        Logger.d(TAG, "onSettingsSaved — new url=${appSettings.apiBaseUrl}")
        // Mark as loading immediately so the UI never sees empty-data + isLoading=false.
        // _allBooks is intentionally NOT cleared here — old books remain visible while
        // the new server's books load, preventing a "no books" flash.
        _isLoading.value = true
        bibleService.closeClient()
        bibleService = BibleService(appSettings)
        _selectedBook.value = null
        _selectedBookNumber.value = null
        _selectedChapter.value = null
        _verses.value = emptyList()
        _bookSearchQuery.value = ""
        _isProjecting.value = false
        _selectedVerseIndices.value = emptySet()
        _projectedVerseIndex.value = null
        _scheduleAdded.value = false
        pendingNavBook         = null
        pendingNavChapter      = null
        pendingNavVerseNumbers = emptySet()
        pendingInitialVerseNumbers = emptySet()
        loadBooks(forceReload = true)
    }

    override fun onCleared() {
        super.onCleared()
        bibleService.closeClient()
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
