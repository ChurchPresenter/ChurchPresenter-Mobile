package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.model.AppModeHolder
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.BibleBook
import com.church.presenter.churchpresentermobile.model.DictionaryVersesResponse
import com.church.presenter.churchpresentermobile.model.StrongsEntry
import com.church.presenter.churchpresentermobile.network.BibleService
import com.church.presenter.churchpresentermobile.network.DictionaryFilter
import com.church.presenter.churchpresentermobile.network.DictionaryService
import com.church.presenter.churchpresentermobile.network.WsSender
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "DictionaryViewModel"
private const val SEARCH_DEBOUNCE_MS = 300L

/**
 * Owns Strong's dictionary search state and the project / add-to-schedule actions.
 * Mirrors the structure of [SongsViewModel].
 */
class DictionaryViewModel(
    private val appSettings: AppSettings,
    eventService: WsSender,
) : ViewModel() {

    private val service = DictionaryService(appSettings, eventService)
    private val bibleService = BibleService(appSettings, eventService)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filter = MutableStateFlow(DictionaryFilter.ALL)
    val filter = _filter.asStateFlow()

    // ── Bible-reference filter (Book → Chapter → Verse cascade) ──────────────
    /** All Bible books, used to populate the reference filter's first dropdown. */
    private val _books = MutableStateFlow<List<BibleBook>>(emptyList())
    val books = _books.asStateFlow()

    /** Selected book, or null when the reference filter is inactive. */
    private val _refBook = MutableStateFlow<BibleBook?>(null)
    val refBook = _refBook.asStateFlow()

    /** Selected chapter, or null. Only meaningful when [refBook] is set. */
    private val _refChapter = MutableStateFlow<Int?>(null)
    val refChapter = _refChapter.asStateFlow()

    /** Selected verse, or null. Only meaningful when [refChapter] is set. */
    private val _refVerse = MutableStateFlow<Int?>(null)
    val refVerse = _refVerse.asStateFlow()

    /** Verse count of the selected chapter (0 until known), for the verse dropdown. */
    private val _refVerseCount = MutableStateFlow(0)
    val refVerseCount = _refVerseCount.asStateFlow()

    private val _entries = MutableStateFlow<List<StrongsEntry>>(emptyList())
    val entries = _entries.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _selectedEntry = MutableStateFlow<StrongsEntry?>(null)
    val selectedEntry = _selectedEntry.asStateFlow()

    /** "Appears in" verses for the selected entry (null until loaded). */
    private val _appearsIn = MutableStateFlow<DictionaryVersesResponse?>(null)
    val appearsIn = _appearsIn.asStateFlow()

    private val _appearsInLoading = MutableStateFlow(false)
    val appearsInLoading = _appearsInLoading.asStateFlow()

    /** Strong's number of the entry currently projected live, or null. */
    private val _projectedNumber = MutableStateFlow<String?>(null)
    val projectedNumber = _projectedNumber.asStateFlow()

    private val _scheduleAdded = MutableStateFlow(false)
    val scheduleAdded = _scheduleAdded.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError = _actionError.asStateFlow()

    private var searchJob: Job? = null

    init {
        search()
        loadBooks()
    }

    /** Loads the Bible book list for the reference filter. Silent on failure — the
     *  filter simply stays unavailable while search still works. */
    private fun loadBooks() {
        // Strong's data lives on the desktop; standalone has none and the
        // dictionary is not reachable there.
        if (!AppModeHolder.hasDesktop) return

        viewModelScope.launch {
            bibleService.getBooks()
                .onSuccess { _books.value = it }
                .onFailure { e -> Logger.e(TAG, "loadBooks — FAILED: ${e.message}", e) }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            search()
        }
    }

    fun setFilter(filter: DictionaryFilter) {
        if (_filter.value == filter) return
        _filter.value = filter
        search()
    }

    /** Selects (or clears, with null) the reference book. Resets chapter + verse. */
    fun setRefBook(book: BibleBook?) {
        _refBook.value = book
        _refChapter.value = null
        _refVerse.value = null
        _refVerseCount.value = 0
        search()
    }

    /** Selects (or clears, with null) the reference chapter. Resets verse. */
    fun setRefChapter(chapter: Int?) {
        _refChapter.value = chapter
        _refVerse.value = null
        _refVerseCount.value = 0
        if (chapter != null) resolveVerseCount(_refBook.value, chapter)
        search()
    }

    /** Selects (or clears, with null) the reference verse. */
    fun setRefVerse(verse: Int?) {
        _refVerse.value = verse
        search()
    }

    /** Clears the entire Book → Chapter → Verse reference filter. */
    fun clearReference() {
        _refBook.value = null
        _refChapter.value = null
        _refVerse.value = null
        _refVerseCount.value = 0
        search()
    }

    /** 1-based book number the server expects, mirroring [BibleService.getChapter]. */
    private fun bookNumberOf(book: BibleBook): Int =
        book.bookId ?: (_books.value.indexOf(book) + 1)

    /** Resolves how many verses [chapter] has: prefer the book metadata, else fetch. */
    private fun resolveVerseCount(book: BibleBook?, chapter: Int) {
        if (book == null) return
        val known = book.chapters?.firstOrNull { it.chapter == chapter }?.verseTotal
        if (known != null && known > 0) {
            _refVerseCount.value = known
            return
        }
        viewModelScope.launch {
            bibleService.getChapter(bookNumberOf(book), chapter)
                .onSuccess { verses -> _refVerseCount.value = verses.size }
                .onFailure { e -> Logger.e(TAG, "resolveVerseCount — FAILED: ${e.message}", e) }
        }
    }

    fun search() {
        // Strong's data lives on the desktop; standalone has none and the
        // dictionary is not reachable there.
        if (!AppModeHolder.hasDesktop) return

        searchJob?.cancel()
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val book = _refBook.value
            service.search(
                query = _searchQuery.value,
                filter = _filter.value,
                book = book?.let { bookNumberOf(it) },
                chapter = _refChapter.value,
                verse = _refVerse.value,
            )
                .onSuccess { _entries.value = it }
                .onFailure { e -> _error.value = "Failed to load dictionary: ${e.message}" }
            _isLoading.value = false
        }
    }

    fun selectEntry(entry: StrongsEntry) {
        _selectedEntry.value = entry
        _scheduleAdded.value = false
        loadAppearsIn(entry)
    }

    /**
     * Loads the "Appears in" verses for [entry]. Passes the active reference filter
     * so occurrences within the filtered book/chapter/verse are ordered first.
     * Failure is silent — the section simply stays hidden.
     */
    private fun loadAppearsIn(entry: StrongsEntry) {
        _appearsIn.value = null
        _appearsInLoading.value = true
        val book = _refBook.value
        viewModelScope.launch {
            service.getVerses(
                number = entry.number,
                book = book?.let { bookNumberOf(it) },
                chapter = _refChapter.value,
                verse = _refVerse.value,
            ).onSuccess { _appearsIn.value = it }
            _appearsInLoading.value = false
        }
    }

    /** Opens the entry for [number] (used by tappable H####/G#### links in a definition). */
    fun selectByNumber(number: String) {
        viewModelScope.launch {
            service.lookup(number)
                .onSuccess { selectEntry(it) }
                .onFailure { e -> _actionError.value = "Couldn't open $number: ${e.message}" }
        }
    }

    fun clearSelection() {
        _selectedEntry.value = null
        _appearsIn.value = null
    }

    fun projectSelected() {
        val entry = _selectedEntry.value ?: return
        viewModelScope.launch {
            service.projectEntry(entry).fold(
                onSuccess = { _projectedNumber.value = entry.number },
                onFailure = { e ->
                    Logger.e(TAG, "projectSelected failed: ${e.message}", e)
                    _actionError.value = e.message ?: "Failed to project"
                }
            )
        }
    }

    fun addSelectedToSchedule() {
        val entry = _selectedEntry.value ?: return
        viewModelScope.launch {
            service.addEntryToSchedule(entry).fold(
                onSuccess = { _scheduleAdded.value = true },
                onFailure = { e ->
                    Logger.e(TAG, "addSelectedToSchedule failed: ${e.message}", e)
                    _actionError.value = e.message ?: "Failed to add to schedule"
                }
            )
        }
    }

    fun clearActionError() { _actionError.value = null }

    /** Re-run the current search after settings (host/port/API key) change. */
    fun onSettingsSaved() { search() }

    override fun onCleared() {
        super.onCleared()
        service.closeClient()
        bibleService.closeClient()
    }
}
