package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.StrongsEntry
import com.church.presenter.churchpresentermobile.network.DictionaryFilter
import com.church.presenter.churchpresentermobile.network.DictionaryService
import com.church.presenter.churchpresentermobile.network.ServerEventService
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
    eventService: ServerEventService,
) : ViewModel() {

    private val service = DictionaryService(appSettings, eventService)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filter = MutableStateFlow(DictionaryFilter.ALL)
    val filter = _filter.asStateFlow()

    private val _entries = MutableStateFlow<List<StrongsEntry>>(emptyList())
    val entries = _entries.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _selectedEntry = MutableStateFlow<StrongsEntry?>(null)
    val selectedEntry = _selectedEntry.asStateFlow()

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

    fun search() {
        searchJob?.cancel()
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            service.search(query = _searchQuery.value, filter = _filter.value)
                .onSuccess { _entries.value = it }
                .onFailure { e -> _error.value = "Failed to load dictionary: ${e.message}" }
            _isLoading.value = false
        }
    }

    fun selectEntry(entry: StrongsEntry) {
        _selectedEntry.value = entry
        _scheduleAdded.value = false
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
    }
}
