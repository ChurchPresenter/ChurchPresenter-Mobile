package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.model.ApiException
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.DemoData
import com.church.presenter.churchpresentermobile.model.Presentation
import com.church.presenter.churchpresentermobile.model.ToastEvent
import com.church.presenter.churchpresentermobile.network.PresentationService
import com.church.presenter.churchpresentermobile.network.recordNetworkError
import com.church.presenter.churchpresentermobile.ui.PickedFile
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "PresentationsViewModel"

/** How many times to retry GET /api/presentations after an upload before giving up. */
private const val UPLOAD_POLL_MAX_RETRIES = 30

/** Milliseconds between each poll attempt after upload. */
private const val UPLOAD_POLL_INTERVAL_MS = 1_000L

/**
 * Manages presentations list state and selection.
 *
 * @param appSettings The shared [AppSettings] instance used to configure the API service.
 * @param isDemoMode  When true, demo content from [DemoData] is used instead of live API calls.
 */
class PresentationsViewModel(private val appSettings: AppSettings, private val isDemoMode: Boolean = false) : ViewModel() {
    private var presentationService = PresentationService(appSettings)

    private val _presentations = MutableStateFlow<List<Presentation>>(emptyList())
    val presentations: StateFlow<List<Presentation>> = _presentations.asStateFlow()

    private val _selectedPresentation = MutableStateFlow<Presentation?>(null)
    val selectedPresentation: StateFlow<Presentation?> = _selectedPresentation.asStateFlow()

    /** Index of the individual slide the user last tapped. */
    private val _selectedSlideIndex = MutableStateFlow<Int?>(null)
    val selectedSlideIndex: StateFlow<Int?> = _selectedSlideIndex.asStateFlow()

    /** Non-null when the screen should scroll to a presentation with this ID. */
    private val _pendingScrollToId = MutableStateFlow<String?>(null)
    val pendingScrollToId: StateFlow<String?> = _pendingScrollToId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** True once a slide has been sent to the display via [selectPresentation]. */
    private val _isProjecting = MutableStateFlow(false)
    val isProjecting: StateFlow<Boolean> = _isProjecting.asStateFlow()

    /** True after the current presentation has been added to the schedule. */
    private val _scheduleAdded = MutableStateFlow(false)
    val scheduleAdded: StateFlow<Boolean> = _scheduleAdded.asStateFlow()

    /** True while a presentation file is being uploaded to the server. */
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    /**
     * Upload progress in the range 0.0..1.0, or null when no upload is in progress.
     * Reflects byte-level progress reported by the HTTP layer via Ktor's onUpload callback.
     */
    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()

    /** Incremented each time a presentation is successfully added to the schedule;
     *  triggers a schedule drawer reload in the UI layer. */
    private val _scheduleRefreshTrigger = MutableStateFlow(0)
    val scheduleRefreshTrigger: StateFlow<Int> = _scheduleRefreshTrigger.asStateFlow()

    /** Transient toast/snackbar event; consumed once by the UI then cleared via [toastShown]. */
    private val _toastEvent = MutableStateFlow<ToastEvent?>(null)
    val toastEvent: StateFlow<ToastEvent?> = _toastEvent.asStateFlow()

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
                    .onFailure { e ->
                        Logger.e(TAG, "loadPresentations — FAILED: ${e.message}", e)
                        _error.value = "Failed to load presentations: ${e.recordNetworkError(TAG, "loadPresentations")}"
                    }
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
                    .onFailure { e ->
                        Logger.e(TAG, "navigateTo — FAILED: ${e.message}", e)
                        _error.value = "Failed to load presentation: ${e.recordNetworkError(TAG, "navigateTo")}"
                    }
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
                    _toastEvent.value = ToastEvent.FailedToSelectPresentation(e.recordNetworkError(TAG, "selectPresentation"))
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
        _isUploading.value = false
        _toastEvent.value = null
        loadPresentations()
    }

    /** Called after the UI has consumed a toast event. */
    fun toastShown() { _toastEvent.value = null }

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
            _scheduleRefreshTrigger.value++
            return
        }
        Logger.d(TAG, "addToSchedule — id=${presentation.displayId}")
        viewModelScope.launch {
            presentationService.addToSchedule(presentation)
                .onSuccess {
                    Logger.d(TAG, "addToSchedule — success")
                    _scheduleAdded.value = true
                    _scheduleRefreshTrigger.value++
                }
                .onFailure { e ->
                    Logger.e(TAG, "addToSchedule — FAILED: ${e.message}", e)
                    _toastEvent.value = ToastEvent.FailedToAddPresentationSchedule(e.recordNetworkError(TAG, "addToSchedule"))
                }
        }
    }

    /**
     * Uploads a presentation file (.pptx / .ppt / .pdf / .key) to the server and reloads
     * the presentations list once the upload completes successfully.
     *
     * The server processes the file asynchronously (slide rendering takes time), so after
     * a successful upload response this method polls [PresentationService.getPresentations]
     * once per second for up to [UPLOAD_POLL_MAX_RETRIES] seconds until the new presentation
     * ID appears in the list, then auto-scrolls to it.
     *
     * @param file The [PickedFile] selected by the user via the native document picker.
     */
    fun uploadPresentationFile(file: PickedFile) {
        if (isDemoMode) return
        viewModelScope.launch {
            _isUploading.value = true
            _uploadProgress.value = null   // indeterminate — byte-level tracking not available cross-platform
            _error.value = null
            presentationService.uploadPresentation(file.bytes, file.fileName)
                .onSuccess { uploaded ->
                    Logger.d(TAG, "uploadPresentationFile — success id=${uploaded.id} name=${uploaded.name}")
                    // Clear the previous list immediately so the UI doesn't show stale entries
                    // while we wait for the server to finish rendering slides.
                    _presentations.value = emptyList()
                    // Poll until the server has finished rendering slides for the new presentation.
                    val uploadedId = uploaded.id
                    var found = false
                    repeat(UPLOAD_POLL_MAX_RETRIES) { attempt ->
                        if (found) return@repeat
                        if (attempt > 0) delay(UPLOAD_POLL_INTERVAL_MS)
                        presentationService.getPresentations()
                            .onSuccess { list ->
                                Logger.d(TAG, "uploadPresentationFile — poll attempt=${attempt + 1} list.size=${list.size} ids=${list.map { it.id }}")
                                if (uploadedId == null || list.any { it.id == uploadedId }) {
                                    _presentations.value = list
                                    if (uploadedId != null) {
                                        _pendingScrollToId.value = uploadedId
                                        val target = list.firstOrNull { it.id == uploadedId }
                                        if (target != null) _selectedPresentation.value = target
                                    }
                                    found = true
                                }
                            }
                            .onFailure { e ->
                                Logger.e(TAG, "uploadPresentationFile — poll FAILED: ${e.message}", e)
                            }
                    }
                    if (!found) {
                        // Timed out — show whatever the server has now and let the user pull-to-refresh
                        Logger.e(TAG, "uploadPresentationFile — timed out waiting for id=$uploadedId")
                        presentationService.getPresentations()
                            .onSuccess { _presentations.value = it }
                            .onFailure { e ->
                                _toastEvent.value = ToastEvent.UploadReloadFailed(e.recordNetworkError(TAG, "uploadPresentationFile/reload"))
                            }
                    }
                }
                .onFailure { e ->
                    Logger.e(TAG, "uploadPresentationFile — FAILED: ${e.message}", e)
                    _toastEvent.value = when ((e as? ApiException)?.httpStatus) {
                        404          -> ToastEvent.UploadUnsupported
                        413          -> ToastEvent.UploadFileTooLarge
                        in 400..599  -> ToastEvent.UploadServerError(e.message ?: "")
                        else         -> ToastEvent.UploadFailed(e.recordNetworkError(TAG, "uploadPresentationFile"))
                    }
                }
            _uploadProgress.value = null
            _isUploading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        presentationService.closeClient()
    }
}
