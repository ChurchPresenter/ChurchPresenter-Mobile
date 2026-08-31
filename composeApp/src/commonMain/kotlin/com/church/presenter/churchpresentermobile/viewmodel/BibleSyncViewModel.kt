package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.library.BibleSyncOutcome
import com.church.presenter.churchpresentermobile.library.BibleSyncService
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.ApiException
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.InstalledBible
import com.church.presenter.churchpresentermobile.network.BibleDownloadService
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "BibleSyncViewModel"

/** One translation the desktop is offering. */
data class TranslationChoice(
    val fileName: String,
    val displayName: String,
    val isInstalled: Boolean,
)

/**
 * Choosing translations and copying them across.
 *
 * There is no cap on how many may be picked: a bilingual congregation wanting three is not the
 * app's business to prevent, and the size of each is shown so the choice is an informed one.
 */
class BibleSyncViewModel(
    private val repository: LocalBibleRepository,
    private val settings: AppSettings,
    private val downloads: BibleDownloadService = BibleDownloadService(settings),
) : ViewModel() {

    private val service = BibleSyncService(
        repository = repository,
        listTranslations = { downloads.listTranslations() },
        downloadTranslation = { index -> downloads.downloadTranslation(index) },
        sourceHost = { settings.host },
    )

    /** Translations already on this device. */
    val installed: StateFlow<List<InstalledBible>> = repository.index
        .map { it.bibles }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.index.value.bibles)

    /**
     * Which translation the Bible tab reads.
     *
     * There is no other way to choose: the first translation copied became the one presented
     * and stayed that way, so a second download was visible in the list and unreachable
     * everywhere else.
     */
    val activeId: StateFlow<String> = repository.index
        .map { it.active?.id.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.index.value.active?.id.orEmpty())

    /** Chooses the translation to read and present. */
    fun setActive(id: String) {
        repository.setActive(id)
        Logger.d(TAG, "setActive — reading '$id'")
    }

    private val _choices = MutableStateFlow<List<TranslationChoice>>(emptyList())
    val choices = _choices.asStateFlow()

    private val _selection = MutableStateFlow<Set<String>>(emptySet())
    val selection = _selection.asStateFlow()

    private val _isLoadingChoices = MutableStateFlow(false)
    val isLoadingChoices = _isLoadingChoices.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError = _loadError.asStateFlow()

    /** True once the desktop answered "who are you?", so the sheet can reveal its key field. */
    private val _needsApiKey = MutableStateFlow(false)
    val needsApiKey = _needsApiKey.asStateFlow()

    val progress = service.progress

    private val _outcome = MutableStateFlow<BibleSyncOutcome?>(null)
    val outcome = _outcome.asStateFlow()

    /** Asks the desktop what it has. */
    fun loadChoices() {
        _isLoadingChoices.value = true
        _loadError.value = null
        _outcome.value = null
        viewModelScope.launch {
            service.catalogue()
                .onSuccess { names ->
                    val onDevice = repository.index.value.bibles.map { it.fileName }.toSet()
                    _choices.value = names.map { name ->
                        TranslationChoice(
                            fileName = name,
                            displayName = displayNameFor(name),
                            isInstalled = name in onDevice,
                        )
                    }
                }
                .onFailure { error ->
                    Logger.e(TAG, "loadChoices — FAILED: ${error.message}")
                    // A rejected key is a different problem from an unreachable computer, and
                    // only one of them is fixed by typing a key.
                    if ((error as? ApiException)?.httpStatus == 401) _needsApiKey.value = true
                    _loadError.value = error.message ?: "Could not reach your computer"
                }
            _isLoadingChoices.value = false
        }
    }

    fun toggle(fileName: String) {
        val current = _selection.value
        _selection.value = if (fileName in current) current - fileName else current + fileName
    }

    /** Copies everything currently ticked. */
    fun sync() {
        val wanted = _choices.value.map { it.fileName }.filter { it in _selection.value }
        if (wanted.isEmpty()) return
        _outcome.value = null
        viewModelScope.launch {
            val result = service.sync(wanted)
            _outcome.value = result
            _selection.value = emptySet()
            loadChoicesQuietly()
        }
    }

    fun cancel() = service.requestCancel()

    fun remove(id: String) {
        repository.remove(id)
        loadChoicesQuietly()
    }

    fun dismissOutcome() {
        _outcome.value = null
    }

    /** Refreshes the installed ticks without flashing the whole picker back to a spinner. */
    private fun loadChoicesQuietly() {
        val onDevice = repository.index.value.bibles.map { it.fileName }.toSet()
        _choices.value = _choices.value.map { it.copy(isInstalled = it.fileName in onDevice) }
    }

    override fun onCleared() {
        super.onCleared()
        downloads.closeClient()
    }

    private companion object {
        /** "en_KJV.spb" reads as "en KJV" until the module itself supplies a title. */
        fun displayNameFor(fileName: String): String =
            fileName.substringBeforeLast(".").replace('_', ' ')
    }
}
