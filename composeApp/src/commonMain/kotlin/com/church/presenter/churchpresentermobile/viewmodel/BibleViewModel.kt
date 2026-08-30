package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.model.ApiException
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppModeHolder
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.BibleBook
import com.church.presenter.churchpresentermobile.model.BibleVerse
import com.church.presenter.churchpresentermobile.model.DemoData
import com.church.presenter.churchpresentermobile.model.ToastEvent
import com.church.presenter.churchpresentermobile.network.BibleCatalog
import com.church.presenter.churchpresentermobile.network.BibleService
import com.church.presenter.churchpresentermobile.network.WsSender
import com.church.presenter.churchpresentermobile.model.SlideDeck
import com.church.presenter.churchpresentermobile.model.SlideDeckBuilder
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.network.recordNetworkError
import com.church.presenter.churchpresentermobile.util.Analytics
import com.church.presenter.churchpresentermobile.util.AnalyticsEvent
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "BibleViewModel"

/**
 * Formats a sorted list of verse numbers into a range string:
 * a single number → null (no range), a contiguous run → "first-last",
 * otherwise a comma-separated list. Used for both projecting and schedule-add.
 */
internal fun verseRangeString(numbers: List<Int>): String? = when {
    numbers.size <= 1 -> null
    numbers.zipWithNext().all { (a, b) -> b == a + 1 } -> "${numbers.first()}-${numbers.last()}"
    else -> numbers.joinToString(",")
}

/**
 * Manages Bible navigation state: book list → chapter selection → verse display.
 *
 * @param appSettings The shared [AppSettings] instance used to configure the API service.
 * @param isDemoMode  When true, demo content from [DemoData] is used instead of live API calls.
 * @param presenter The local presenter, when the app can present standalone. The
 *   desktop protocol carries only book/chapter/verse ids, so the materialised
 *   verse text has to be handed over separately — this is that handoff. Every
 *   call on it is a no-op in remote mode, so there is no mode branching here.
 */
class BibleViewModel(
    private val appSettings: AppSettings,
    private val eventService: WsSender,
    private val isDemoMode: Boolean = false,
    private val presenter: StandaloneEngine? = null,
    private val mode: StateFlow<AppMode> = AppModeHolder.mode,
    private val catalog: BibleCatalog = BibleCatalog(mode, BibleService(appSettings, eventService)),
) : ViewModel() {
    private var bibleService = BibleService(appSettings, eventService)

    /**
     * The deck built for the open chapter, kept so projecting can re-supply it.
     *
     * Clearing the display unloads the engine's deck, so a later `showSlide`
     * clamped into an empty deck and put nothing on the screen. Holding the
     * deck here makes projecting work from any prior state. Same fix as
     * [SongsViewModel.projectLocally].
     */
    private var loadedDeck: SlideDeck? = null

    /**
     * True when there is no Bible to browse: standalone, with no translation copied onto this
     * device. A stream rather than a snapshot, so finishing a download opens the tab without
     * the app being restarted.
     */
    val hasNoLocalBibles: StateFlow<Boolean> = catalog.hasNoBible
        .stateIn(viewModelScope, SharingStarted.Eagerly, mode.value == AppMode.STANDALONE)

    /** Nothing to read from: standalone with no downloaded translation. */
    private val cannotLoad: Boolean get() = hasNoLocalBibles.value

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

    private val _isHolding = MutableStateFlow(false)
    val isHolding = _isHolding.asStateFlow()

    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode = _isMultiSelectMode.asStateFlow()

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

    /**
     * Which load is the current one. Anything an older load says is discarded.
     *
     * The app starts in remote — the holder's default until settings are read —
     * so a first launch asks the default desktop address before the operator has
     * picked standalone, and that request sits on a ten-second connect timeout.
     * By the time it fails the tab may already be reading the device perfectly
     * well, and the abandoned request would write "make sure the server is
     * running" over the top of it.
     */
    private var loadGeneration = 0

    init {
        loadBooks()
        // Where the books come from can change under this tab, and nothing else
        // re-ran the load. Two things move it:
        //
        //  - the mode: the source changes, and an error from the old source has
        //    to go with it.
        //  - installing a translation: standalone with nothing downloaded is an
        //    empty tab by definition, so finishing a download is exactly the
        //    moment it has something to show.
        //
        // Without these the tab kept its "no Bible books available" and whatever
        // error it had until the operator switched tabs and came back, which is
        // how this was reported.
        viewModelScope.launch {
            catalog.isLocalSource
                .distinctUntilChanged()
                .drop(1)
                .collect { loadBooks(forceReload = true) }
        }
        viewModelScope.launch {
            // Already a StateFlow, so it dedupes itself; drop(1) skips the
            // value loadBooks() above has just acted on.
            hasNoLocalBibles
                .drop(1)
                .collect { loadBooks(forceReload = true) }
        }
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
        if (cannotLoad) {
            // One guard covers every caller: init, pull-to-refresh, settings-save
            // and the server's bible_updated push.
            Logger.d(TAG, "loadBooks — standalone, no Bible source on this device")
            _allBooks.value = emptyList()
            _error.value = null
            _isLoading.value = false
            return
        }
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
        val generation = ++loadGeneration
        viewModelScope.launch {
            try {
                catalog.books()
                    .onSuccess {
                        if (generation != loadGeneration) return@onSuccess
                        _allBooks.value = it
                        tryProcessPendingNav()
                    }
                    .onFailure { e ->
                        Logger.e(TAG, "loadBooks — FAILED: ${e.message}", e)
                        if (generation != loadGeneration) {
                            Logger.d(TAG, "loadBooks — ignoring a superseded load's failure")
                            return@onFailure
                        }
                        _error.value = "Failed to load Bible books: ${e.recordNetworkError(TAG, "loadBooks")}"
                    }
            } finally {
                if (generation == loadGeneration) _isLoading.value = false
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
        val bookNumber = book.bookId ?: (_allBooks.value.indexOf(book) + 1)
        Logger.d(TAG, "selectBook — ${book.displayName} (bookNumber=$bookNumber)")
        _selectedBook.value = book
        _selectedBookNumber.value = bookNumber
        _selectedChapter.value = null
        _verses.value = emptyList()
        Analytics.logEvent(AnalyticsEvent.BIBLE_BOOK_SELECTED)
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
        _isMultiSelectMode.value = false
        _selectedVerseIndices.value = emptySet()
        _projectedVerseIndex.value = null
        _scheduleAdded.value = false
        Analytics.logEvent(AnalyticsEvent.BIBLE_CHAPTER_SELECTED)

        if (isDemoMode) {
            Logger.d(TAG, "selectChapter — DEMO MODE, serving demo verses")
            val verses = DemoData.getVerses(book.displayName, chapter)
            _verses.value = verses
            val deck = SlideDeckBuilder.fromBibleChapter(book, chapter, verses)
            loadedDeck = deck
            presenter?.loadDeck(deck)
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
                catalog.chapter(bookNumber, chapter)
                    .onSuccess { verses ->
                        _verses.value = verses
                        val deck = SlideDeckBuilder.fromBibleChapter(book, chapter, verses)
                        loadedDeck = deck
                        presenter?.loadDeck(deck)
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
                _isMultiSelectMode.value = false
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
        // Standalone: this device is the screen, so cast always means "show the
        // selected verses" — stopping is the Clear Display button. See
        // [SongsViewModel.toggleProjecting] for why a toggle desynchronised here.
        val presenter = presenter
        if (mode.value == AppMode.STANDALONE && presenter != null) {
            projectLocally(presenter)
            return
        }
        if (_isProjecting.value) {
            _isProjecting.value        = false
            _projectedVerseIndex.value = null
            Analytics.logEvent(AnalyticsEvent.BIBLE_DISPLAY_CLEARED)
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
        // The local audience screen, if this device is the presenter. Opening the chapter only
        // loaded it; this is the press that puts the verse in front of anyone.
        val firstSelected = _selectedVerseIndices.value.minOrNull() ?: 0
        presenter?.showSlide(firstSelected)
        if (isDemoMode) {
            Logger.d(TAG, "toggleProjecting — DEMO MODE, simulating success")
            _isProjecting.value        = true
            _projectedVerseIndex.value = _selectedVerseIndices.value.minOrNull()
            _toastEvent.value          = ToastEvent.BibleLive
            return
        }
        val nums = selectedVerses.map { it.number }
        val verseRange: String? = verseRangeString(nums)
        Logger.d(TAG, "toggleProjecting ON — firing selectBibleVerse ${book.displayName} $chapter:${firstVerse.number} range=$verseRange")
        viewModelScope.launch {
            bibleService.selectBibleVerse(
                bookName    = book.displayName,
                chapter     = chapter,
                verseNumber = firstVerse.number,
                verseText   = selectedVerses.joinToString("\n") { it.displayText },
                verseRange  = verseRange
            ).onSuccess {
                Logger.d(TAG, "selectBibleVerse — success")
                _isProjecting.value        = true
                _projectedVerseIndex.value = _selectedVerseIndices.value.minOrNull()
                _toastEvent.value          = ToastEvent.BibleLive
                Analytics.logEvent(AnalyticsEvent.BIBLE_PROJECTED)
            }.onFailure { e ->
                Logger.e(TAG, "selectBibleVerse — FAILED: ${e.message}", e)
                _toastEvent.value = ToastEvent.FailedToProjectBible(e.recordNetworkError(TAG, "toggleProjecting/selectBibleVerse"))
            }
        }
    }

    /**
     * Puts the selected verses on this device's audience screen.
     *
     * Supplies the deck rather than assuming the engine still holds one, so
     * projecting works after a clear, which unloads it.
     */
    private fun projectLocally(presenter: StandaloneEngine) {
        val firstSelected = _selectedVerseIndices.value.minOrNull()
        if (firstSelected == null) {
            _toastEvent.value = ToastEvent.FailedToProjectBible("Select at least one verse first")
            return
        }
        val deck = loadedDeck
        if (deck == null) {
            Logger.d(TAG, "projectLocally — no chapter loaded yet, ignoring")
            return
        }
        presenter.setDeck(deck)
        presenter.showSlide(firstSelected)
        _isProjecting.value        = true
        _projectedVerseIndex.value = firstSelected
        _toastEvent.value          = ToastEvent.BibleLive
        Analytics.logEvent(AnalyticsEvent.BIBLE_PROJECTED)
        Logger.d(TAG, "projectLocally — projected verse index $firstSelected")
    }

    /**
     * Toggles Bible hold mode. When held, the desktop display freezes on the current verse
     * so the user can browse other verses without changing the projection.
     */
    fun toggleHold() {
        val newHold = !_isHolding.value
        _isHolding.value = newHold
        if (isDemoMode) {
            Logger.d(TAG, "toggleHold — DEMO MODE, hold=$newHold")
            return
        }
        Logger.d(TAG, "toggleHold — firing setBibleHold hold=$newHold")
        viewModelScope.launch {
            bibleService.setBibleHold(newHold)
                .onSuccess { Logger.d(TAG, "setBibleHold — success") }
                .onFailure { e ->
                    Logger.e(TAG, "setBibleHold — FAILED: ${e.message}", e)
                    e.recordNetworkError(TAG, "toggleHold/setBibleHold")
                }
        }
    }

    /**
     * Clears the desktop display without toggling projection mode off.
     */
    fun clearDisplay() {
        _isProjecting.value = false
        _projectedVerseIndex.value = null
        _isHolding.value = false
        if (isDemoMode) {
            Logger.d(TAG, "clearDisplay — DEMO MODE")
            return
        }
        Logger.d(TAG, "clearDisplay — firing clear")
        viewModelScope.launch {
            bibleService.clearDisplay()
                .onSuccess { Logger.d(TAG, "clearDisplay — success") }
                .onFailure { e ->
                    Logger.e(TAG, "clearDisplay — FAILED: ${e.message}", e)
                    e.recordNetworkError(TAG, "clearDisplay")
                }
        }
    }

    /**
     * Toggles whether the verse at [index] is in the multi-select set.
     * Always available — does not require projecting to be active.
     */
    fun toggleVerseSelection(index: Int) {
        val current = _selectedVerseIndices.value
        if (_isMultiSelectMode.value) {
            // Multi-select: toggle verse in/out of the set
            _selectedVerseIndices.value =
                if (index in current) current - index else current + index
        } else {
            // Single-select: replace the selection (or deselect if already selected)
            _selectedVerseIndices.value =
                if (current == setOf(index)) emptySet() else setOf(index)
        }
        // If projecting, immediately project the tapped verse
        if (_isProjecting.value) projectVerseAtIndex(index)
    }

    fun toggleMultiSelectMode() {
        val newValue = !_isMultiSelectMode.value
        _isMultiSelectMode.value = newValue
        if (!newValue) _selectedVerseIndices.value = emptySet()
    }

    /**
     * Sends a specific verse to the projector while projecting mode is active.
     * Also adds it to the selection set.
     */
    fun selectVerse(index: Int) {
        if (!_isProjecting.value) return
        // ensure it is in the selection set
        _selectedVerseIndices.value = _selectedVerseIndices.value + index
        projectVerseAtIndex(index)
    }

    private fun projectVerseAtIndex(index: Int) {
        val book    = _selectedBook.value    ?: return
        val chapter = _selectedChapter.value ?: return
        val verse   = _verses.value.getOrNull(index) ?: return
        _projectedVerseIndex.value = index
        // The audience screen this device drives, moved before the desktop is told anything:
        // in standalone there is no desktop, and without this the deck sits on whichever verse
        // loadChapter left it at while the operator taps their way down the chapter.
        presenter?.showSlide(index)
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
        val ref = if (nums.size == 1) {
            "${book.displayName} $chapter:${nums.first()}"
        } else {
            "${book.displayName} $chapter:${verseRangeString(nums)}"
        }
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
                Analytics.logEvent(AnalyticsEvent.BIBLE_ADDED_TO_SCHEDULE)
            }.onFailure { e ->
                Logger.e(TAG, "addBibleToSchedule — FAILED: ${e.message}", e)
                _toastEvent.value = e.toToastEvent { ToastEvent.FailedToAddBibleSchedule(e.recordNetworkError(TAG, "addToSchedule/addBibleToSchedule")) }
            }
        }
    }

    /** Resets projection state when the desktop clears its display. */
    fun onDisplayCleared() {
        _isProjecting.value = false
        _isHolding.value = false
        _projectedVerseIndex.value = null
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
        bibleService = BibleService(appSettings, eventService)
        _selectedBook.value = null
        _selectedBookNumber.value = null
        _selectedChapter.value = null
        _verses.value = emptyList()
        _bookSearchQuery.value = ""
        _isProjecting.value = false
        _isHolding.value = false
        _isMultiSelectMode.value = false
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
