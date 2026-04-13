package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.DemoData
import com.church.presenter.churchpresentermobile.model.PictureImage
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

    /** The currently-selected [PictureImage]; used for highlight, Cast, and Add-to-Schedule. */
    private val _selectedImage = MutableStateFlow<PictureImage?>(null)
    val selectedImage = _selectedImage.asStateFlow()

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
     * a folder. Loads the folder first, then sets [_pendingScrollIndex] — ensuring
     * the `PicturesScreen` LaunchedEffect only fires once the correct folder is
     * already in [_folder], so [selectPicture] sends the right folderId + fileName
     * to the desktop.
     *
     * Previously [_pendingScrollIndex] was set before the network call, causing the
     * LaunchedEffect to fire immediately with the old folder (wrong image projected)
     * and then never fire again once the correct folder arrived (pendingScrollIndex
     * had already been cleared by [onPendingScrollHandled]).
     */
    fun navigateTo(folderId: String?, imageIndex: Int?) {
        Logger.d(TAG, "navigateTo — folderId=$folderId imageIndex=$imageIndex")
        if (isDemoMode) {
            _folder.value = DemoData.picturesFolder
            _pendingScrollIndex.value = imageIndex
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                picturesService.getPictures(folderId)
                    .onSuccess { newFolder ->
                        // Set folder first, THEN pendingScrollIndex — the LaunchedEffect
                        // in PicturesScreen keys on both, so it will see the correct folder
                        // when it fires.
                        _folder.value = newFolder
                        _pendingScrollIndex.value = imageIndex
                    }
                    .onFailure { e ->
                        Logger.e(TAG, "navigateTo — FAILED: ${e.message}", e)
                        _error.value = "Failed to load pictures: ${e.recordNetworkError(TAG, "navigateTo")}"
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Clears the pending scroll so it fires only once. */
    fun onPendingScrollHandled() {
        _pendingScrollIndex.value = null
    }

    /**
     * Selects a picture and sends it to the display.
     * Uses [PictureImage.fileName] as the primary identifier; [PictureImage.index] is only
     * forwarded to the server as a fallback when the filename is unavailable.
     *
     * @param image The image object from the current folder response.
     */
    fun selectPicture(image: PictureImage) {
        _selectedImage.value = image
        _scheduleAdded.value = false
        val folderId = _folder.value?.folderId ?: run {
            Logger.d(TAG, "selectPicture — no folderId, cannot project (fileName=${image.fileName})")
            return
        }
        _isProjecting.value = true
        if (isDemoMode) {
            Logger.d(TAG, "selectPicture — DEMO MODE folderId=$folderId fileName=${image.fileName}")
            return
        }
        Logger.d(TAG, "selectPicture — folderId=$folderId fileName=${image.fileName} index=${image.index}")
        viewModelScope.launch {
            picturesService.selectPicture(folderId, image.fileName, image.index)
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
        // _selectedImage is intentionally kept so the Cast button can re-project
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
        val folder   = _folder.value ?: return
        val image    = _selectedImage.value ?: return
        val folderId = folder.folderId ?: return
        val label    = image.fileName ?: "Image ${image.index}"
        if (isDemoMode) {
            Logger.d(TAG, "addToSchedule — DEMO MODE folderId=$folderId fileName=${image.fileName}")
            _scheduleAdded.value = true
            return
        }
        Logger.d(TAG, "addToSchedule — folderId=$folderId index=${image.index} label=$label")
        viewModelScope.launch {
            picturesService.addToSchedule(folderId, image.index, label)
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
     * Uploads one or more device photos to the server sequentially and projects
     * the last successful upload immediately on the desktop.
     *
     * Photos land in the server's session-only dated "Device Photos" folder
     * (`device_uploads/yyyy-MM-dd`) and are projected instantly via POST /api/pictures/select.
     * After all uploads complete the folder catalog is fetched inline (awaited), then
     * [_pendingScrollIndex] is set — ensuring the grid switches to the new photos immediately
     * without briefly flashing the old projector folder.
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
                Logger.d(TAG, "uploadDevicePhotos — projecting folderId=${uploaded.folderId} fileName=${uploaded.fileName} index=${uploaded.imageIndex}")
                _isProjecting.value = true
                // Track the selected image so Cast button can re-project it
                _selectedImage.value = PictureImage(index = uploaded.imageIndex, fileName = uploaded.fileName)
                picturesService.selectPicture(uploaded.folderId, uploaded.fileName, uploaded.imageIndex)
                    .onSuccess { Logger.d(TAG, "uploadDevicePhotos — selectPicture success") }
                    .onFailure { e ->
                        Logger.e(TAG, "uploadDevicePhotos — selectPicture FAILED: ${e.message}", e)
                        _error.value = "Uploaded but failed to project: ${e.toFriendlyNetworkMessage()}"
                    }
                // Fetch the device_uploads folder synchronously within this coroutine so that
                // _folder.value is already the new catalog before _pendingScrollIndex is set.
                _selectedImage.value = null
                picturesService.getPictures(uploaded.folderId)
                    .onSuccess { newFolder ->
                        _folder.value = newFolder
                        _pendingScrollIndex.value = uploaded.imageIndex
                    }
                    .onFailure { e ->
                        Logger.e(TAG, "uploadDevicePhotos — reload FAILED: ${e.message}", e)
                        _error.value = "Uploaded but failed to load folder: ${e.recordNetworkError(TAG, "uploadDevicePhotos/reload")}"
                    }
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
        _selectedImage.value = null
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
