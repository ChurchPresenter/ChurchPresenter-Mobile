package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.DemoData
import com.church.presenter.churchpresentermobile.model.PicturesFolder
import com.church.presenter.churchpresentermobile.network.PicturesService
import com.church.presenter.churchpresentermobile.network.recordNetworkError
import com.church.presenter.churchpresentermobile.network.toFriendlyNetworkMessage
import com.church.presenter.churchpresentermobile.ui.PickedPhoto
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "PicturesViewModel"

/**
 * Manages pictures folder state and image selection.
 *
 * @param appSettings The shared [AppSettings] instance.
 * @param isDemoMode  When true, demo content from [DemoData] is used instead of live API calls.
 */
class PicturesViewModel(private val appSettings: AppSettings, private val isDemoMode: Boolean = false) : ViewModel() {
    private var picturesService = PicturesService(appSettings)

    private val _folder = MutableStateFlow<PicturesFolder?>(null)
    val folder = _folder.asStateFlow()

    private val _selectedIndex = MutableStateFlow<Int?>(null)
    val selectedIndex = _selectedIndex.asStateFlow()

    /** Non-null when the screen should scroll to (and highlight) a particular image. */
    private val _pendingScrollIndex = MutableStateFlow<Int?>(null)
    val pendingScrollIndex = _pendingScrollIndex.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    /** True after a picture has been sent to the display via [selectPicture]. */
    private val _isProjecting = MutableStateFlow(false)
    val isProjecting = _isProjecting.asStateFlow()

    /** True after the current picture has been added to the schedule. */
    private val _scheduleAdded = MutableStateFlow(false)
    val scheduleAdded = _scheduleAdded.asStateFlow()

    /** True while a device photo is being uploaded to the server. */
    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    init {
        loadPictures()
    }

    /** Fetches the pictures folder from the API.
     *  In demo mode, serves [DemoData.picturesFolder] without any network call. */
    fun loadPictures(folderId: String? = null) {
        if (isDemoMode) {
            Logger.d(TAG, "loadPictures — DEMO MODE folderId=$folderId")
            _folder.value = DemoData.picturesFolder
            _error.value = null
            return
        }
        Logger.d(TAG, "loadPictures — url=${appSettings.apiBaseUrl} folderId=$folderId")
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                picturesService.getPictures(folderId)
                    .onSuccess { _folder.value = it }
                    .onFailure { e ->
                        Logger.e(TAG, "loadPictures — FAILED: ${e.message}", e)
                        _error.value = "Failed to load pictures: ${e.recordNetworkError(TAG, "loadPictures")}"
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Called from the schedule drawer to navigate to a specific image inside
     * a folder. Reloads the folder (optionally filtering by [folderId]) then
     * pre-selects [imageIndex] so the screen can highlight and scroll to it.
     */
    fun navigateTo(folderId: String?, imageIndex: Int?) {
        Logger.d(TAG, "navigateTo — folderId=$folderId imageIndex=$imageIndex")
        _pendingScrollIndex.value = imageIndex
        loadPictures(folderId = folderId)
    }

    /** Clears the pending scroll so it fires only once. */
    fun onPendingScrollHandled() {
        _pendingScrollIndex.value = null
    }

    /**
     * Selects a picture by index and sends it to the display.
     * In demo mode, updates [_selectedIndex] locally without any API call.
     *
     * @param index Zero-based image index within the current folder.
     */
    fun selectPicture(index: Int) {
        // Always update the selection for visual highlight and Add-to-Schedule support,
        // even if the folderId is missing and we can't call the API.
        _selectedIndex.value = index
        _scheduleAdded.value = false
        val folderId = _folder.value?.folderId ?: run {
            Logger.d(TAG, "selectPicture — no folderId, cannot project (index=$index)")
            return
        }
        _isProjecting.value = true
        if (isDemoMode) {
            Logger.d(TAG, "selectPicture — DEMO MODE folderId=$folderId index=$index")
            return
        }
        Logger.d(TAG, "selectPicture — folderId=$folderId index=$index")
        viewModelScope.launch {
            picturesService.selectPicture(folderId, index)
                .onSuccess { Logger.d(TAG, "selectPicture — success") }
                .onFailure { e ->
                    Logger.e(TAG, "selectPicture — FAILED: ${e.message}", e)
                    _error.value = "Failed to select picture: ${e.recordNetworkError(TAG, "selectPicture")}"
                }
        }
    }

    /** POSTs /api/clear and resets projection state. */
    fun clearDisplay() {
        _isProjecting.value = false
        // _selectedIndex is intentionally kept so the Cast button can re-project
        // the same image and Add-to-Schedule still has a target.
        _scheduleAdded.value = false
        if (isDemoMode) {
            Logger.d(TAG, "clearDisplay — DEMO MODE")
            return
        }
        viewModelScope.launch {
            picturesService.clearDisplay()
                .onSuccess { Logger.d(TAG, "clearDisplay — success") }
                .onFailure { e -> Logger.e(TAG, "clearDisplay — FAILED: ${e.message}", e) }
        }
    }

    /** Adds the currently selected picture to the schedule via POST /api/schedule/add. */
    fun addToSchedule() {
        val folder = _folder.value ?: return
        val index  = _selectedIndex.value ?: return
        val folderId = folder.folderId ?: return
        val image  = folder.allImages.getOrNull(index)
        val label  = image?.fileName ?: "Image $index"
        if (isDemoMode) {
            Logger.d(TAG, "addToSchedule — DEMO MODE folderId=$folderId index=$index")
            _scheduleAdded.value = true
            return
        }
        Logger.d(TAG, "addToSchedule — folderId=$folderId index=$index label=$label")
        viewModelScope.launch {
            picturesService.addToSchedule(folderId, index, label)
                .onSuccess {
                    Logger.d(TAG, "addToSchedule — success")
                    _scheduleAdded.value = true
                }
                .onFailure { e ->
                    Logger.e(TAG, "addToSchedule — FAILED: ${e.message}", e)
                    _error.value = "Failed to add to schedule: ${e.recordNetworkError(TAG, "addToSchedule")}"
                }
        }
    }

    /**
     * Uploads one or more device photos to the server sequentially.
     *
     * All photos land in the server's persistent "Device Photos" folder (`device_uploads`),
     * so every uploaded photo is visible in the Pictures tab grid.
     * After all uploads, the last successfully uploaded image is projected automatically
     * and the grid is reloaded to show the full Device Photos folder.
     *
     * @param photos List of [PickedPhoto] returned by the platform photo picker.
     */
    fun uploadDevicePhotos(photos: List<PickedPhoto>) {
        if (isDemoMode || photos.isEmpty()) return
        viewModelScope.launch {
            _isUploading.value = true
            _error.value = null
            var lastUploaded: com.church.presenter.churchpresentermobile.model.UploadPhotoResponse? = null
            for (photo in photos) {
                picturesService.uploadPhoto(photo.bytes, photo.fileName)
                    .onSuccess { uploaded ->
                        Logger.d(TAG, "uploadDevicePhotos — uploaded ${photo.fileName} folderId=${uploaded.folderId} index=${uploaded.imageIndex}")
                        lastUploaded = uploaded
                    }
                    .onFailure { e ->
                        Logger.e(TAG, "uploadDevicePhotos — FAILED for ${photo.fileName}: ${e.message}", e)
                        _error.value = "Failed to upload ${photo.fileName}: ${e.recordNetworkError(TAG, "uploadDevicePhotos")}"
                    }
            }
            lastUploaded?.let { uploaded ->
                Logger.d(TAG, "uploadDevicePhotos — projecting folderId=${uploaded.folderId} index=${uploaded.imageIndex}")
                _isProjecting.value = true
                picturesService.selectPicture(uploaded.folderId, uploaded.imageIndex)
                    .onSuccess { Logger.d(TAG, "uploadDevicePhotos — selectPicture success") }
                    .onFailure { e ->
                        Logger.e(TAG, "uploadDevicePhotos — selectPicture FAILED: ${e.message}", e)
                        _error.value = "Uploaded but failed to project: ${e.toFriendlyNetworkMessage()}"
                    }
                // Reload the Device Photos folder so all uploaded images appear in the grid
                loadPictures(folderId = uploaded.folderId)
            }
            _isUploading.value = false
        }
    }

    /** Rebuilds the service with updated settings and reloads. */
    fun onSettingsSaved() {
        Logger.d(TAG, "onSettingsSaved — new url=${appSettings.apiBaseUrl}")
        picturesService.closeClient()
        picturesService = PicturesService(appSettings)
        _folder.value = null
        _selectedIndex.value = null
        _pendingScrollIndex.value = null
        _isProjecting.value = false
        _scheduleAdded.value = false
        _isUploading.value = false
        loadPictures()
    }

    override fun onCleared() {
        super.onCleared()
        picturesService.closeClient()
    }
}
