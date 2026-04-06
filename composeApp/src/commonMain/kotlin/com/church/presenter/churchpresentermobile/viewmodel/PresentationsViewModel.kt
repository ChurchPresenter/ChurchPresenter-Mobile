package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.DemoData
import com.church.presenter.churchpresentermobile.model.Presentation
import com.church.presenter.churchpresentermobile.network.PresentationService
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "PresentationsViewModel"

/**
 * Manages presentations list state and selection.
 *
 * @param appSettings The shared [AppSettings] instance used to configure the API service.
 * @param isDemoMode  When true, demo content from [DemoData] is used instead of live API calls.
 */
class PresentationsViewModel(private val appSettings: AppSettings, private val isDemoMode: Boolean = false) : ViewModel() {
    private var presentationService = PresentationService(appSettings)

    private val _presentations = MutableStateFlow<List<Presentation>>(emptyList())
    val presentations = _presentations.asStateFlow()

    private val _selectedPresentation = MutableStateFlow<Presentation?>(null)
    val selectedPresentation = _selectedPresentation.asStateFlow()

    /** Index of the individual slide the user last tapped. */
    private val _selectedSlideIndex = MutableStateFlow<Int?>(null)
    val selectedSlideIndex = _selectedSlideIndex.asStateFlow()

    /** Non-null when the screen should scroll to a presentation with this ID. */
    private val _pendingScrollToId = MutableStateFlow<String?>(null)
    val pendingScrollToId = _pendingScrollToId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    /** True once a slide has been sent to the display via [selectPresentation]. */
    private val _isProjecting = MutableStateFlow(false)
    val isProjecting = _isProjecting.asStateFlow()

    /** True after the current presentation has been added to the schedule. */
    private val _scheduleAdded = MutableStateFlow(false)
    val scheduleAdded = _scheduleAdded.asStateFlow()

    init {
        loadPresentations()
    }

    /**
     * Fetches the presentations list from the API and updates UI state.
     * In demo mode, serves [DemoData.presentations] without any network call.
     */
    fun loadPresentations() {
        if (isDemoMode) {
            Logger.d(TAG, "loadPresentations — DEMO MODE")
            _presentations.value = DemoData.presentations
            _error.value = null
            return
        }
        Logger.d(TAG, "loadPresentations — url=${appSettings.apiBaseUrl}")
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                presentationService.getPresentations()
                    .onSuccess { _presentations.value = it }
                    .onFailure { _error.value = "Failed to load presentations: ${it.message}" }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Called from the schedule drawer to navigate to a specific presentation.
     * In demo mode, looks up the presentation from [DemoData] without any network call.
     */
    fun navigateTo(presentationId: String) {
        Logger.d(TAG, "navigateTo — presentationId=$presentationId")
        if (isDemoMode) {
            val found = DemoData.presentations.firstOrNull { it.id == presentationId }
                ?: DemoData.presentations.firstOrNull()
            if (found != null) {
                _presentations.value = listOf(found)
                _selectedPresentation.value = found
                _pendingScrollToId.value = found.id
            }
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                presentationService.getPresentationById(presentationId)
                    .onSuccess { fetched ->
                        // Show only the presentation that was tapped from the schedule
                        _presentations.value = listOf(fetched)
                        _selectedPresentation.value = fetched
                        _pendingScrollToId.value = fetched.id
                    }
                    .onFailure { _error.value = "Failed to load presentation: ${it.message}" }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Clears the pending scroll trigger so it fires only once. */
    fun onPendingScrollHandled() {
        _pendingScrollToId.value = null
    }

    /**
     * Selects a presentation and a specific slide, sending the select request to the server.
     * In demo mode, updates local state without any API call.
     *
     * @param presentation The [Presentation] the user tapped.
     * @param slideIndex   The zero-based index of the slide that was tapped.
     */
    fun selectPresentation(presentation: Presentation, slideIndex: Int) {
        _selectedPresentation.value = presentation
        _selectedSlideIndex.value = slideIndex
        _isProjecting.value = true
        _scheduleAdded.value = false
        val id = presentation.displayId
        if (id.isBlank()) {
            Logger.d(TAG, "selectPresentation — skipping API call, no id for '${presentation.displayName}'")
            return
        }
        if (isDemoMode) {
            Logger.d(TAG, "selectPresentation — DEMO MODE id=$id name=${presentation.displayName}")
            return
        }
        Logger.d(TAG, "selectPresentation — id=$id name=${presentation.displayName} slideIndex=$slideIndex")
        viewModelScope.launch {
            presentationService.selectPresentation(id, slideIndex)
                .onSuccess { Logger.d(TAG, "selectPresentation — success") }
                .onFailure { e ->
                    Logger.e(TAG, "selectPresentation — FAILED: ${e.message}", e)
                    _error.value = "Failed to select presentation: ${e.message}"
                }
        }
    }

    /**
     * Rebuilds the HTTP service with latest settings and reloads presentations.
     * Call this after the user saves new settings.
     */
    fun onSettingsSaved() {
        Logger.d(TAG, "onSettingsSaved — new url=${appSettings.apiBaseUrl}")
        presentationService.closeClient()
        presentationService = PresentationService(appSettings)
        _presentations.value = emptyList()
        _selectedPresentation.value = null
        _selectedSlideIndex.value = null
        _pendingScrollToId.value = null
        _isProjecting.value = false
        _scheduleAdded.value = false
        loadPresentations()
    }

    /** POSTs /api/clear and resets projection state. */
    fun clearDisplay() {
        _isProjecting.value = false
        // _selectedPresentation and _selectedSlideIndex are intentionally kept so
        // the Cast button can re-project and Add-to-Schedule still has a target.
        _scheduleAdded.value = false
        if (isDemoMode) {
            Logger.d(TAG, "clearDisplay — DEMO MODE")
            return
        }
        viewModelScope.launch {
            presentationService.clearDisplay()
                .onSuccess { Logger.d(TAG, "clearDisplay — success") }
                .onFailure { e -> Logger.e(TAG, "clearDisplay — FAILED: ${e.message}", e) }
        }
    }

    /** Adds the currently selected presentation to the schedule via POST /api/schedule/add. */
    fun addToSchedule() {
        val presentation = _selectedPresentation.value ?: return
        if (isDemoMode) {
            Logger.d(TAG, "addToSchedule — DEMO MODE id=${presentation.displayId}")
            _scheduleAdded.value = true
            return
        }
        Logger.d(TAG, "addToSchedule — id=${presentation.displayId}")
        viewModelScope.launch {
            presentationService.addToSchedule(presentation)
                .onSuccess {
                    Logger.d(TAG, "addToSchedule — success")
                    _scheduleAdded.value = true
                }
                .onFailure { e ->
                    Logger.e(TAG, "addToSchedule — FAILED: ${e.message}", e)
                    _error.value = "Failed to add to schedule: ${e.message}"
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        presentationService.closeClient()
    }
}
