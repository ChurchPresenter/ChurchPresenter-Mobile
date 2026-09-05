package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import com.church.presenter.churchpresentermobile.generateUUID
import com.church.presenter.churchpresentermobile.library.ChordProParser
import com.church.presenter.churchpresentermobile.library.CpsetSerializer
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.CPSET_EXTENSION
import com.church.presenter.churchpresentermobile.model.ConflictResolution
import com.church.presenter.churchpresentermobile.model.CpsetDocument
import com.church.presenter.churchpresentermobile.model.CpsetError
import com.church.presenter.churchpresentermobile.model.CpsetReadResult
import com.church.presenter.churchpresentermobile.model.ImportPreview
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock

private const val TAG = "LibraryShareViewModel"

/** What the share screen is currently showing. */
sealed interface ShareUiState {
    data object Idle : ShareUiState

    /** A file has been read and is waiting for the user to confirm. */
    data class Previewing(val preview: ImportPreview) : ShareUiState

    data class Imported(val count: Int) : ShareUiState

    data class Error(val error: CpsetError) : ShareUiState
}

/**
 * Imports and exports library files.
 *
 * A picked file is classified by content rather than by extension: Android
 * routinely reports an emailed `.cho` as `application/octet-stream`, so
 * trusting the MIME type would reject files that are perfectly readable.
 */
class LibraryShareViewModel(
    private val repository: LibraryRepository,
    private val appVersion: String = "",
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShareUiState>(ShareUiState.Idle)
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    /** Serialized library, ready to hand to the platform share sheet. */
    fun exportText(): String = CpsetSerializer.write(
        CpsetSerializer.exportLibrary(
            library = repository.library.value,
            name = "ChurchPresenter library",
            exportedAt = now(),
            appVersion = appVersion,
        )
    )

    /** Suggested file name for an export. */
    fun exportFileName(): String = "ChurchPresenter-library.$CPSET_EXTENSION"

    /**
     * Reads a picked file and moves to the preview step.
     *
     * A `.cpset` is a whole library; anything else is treated as a single song
     * in ChordPro or plain text, which is how songs arrive from other apps.
     */
    fun onFilePicked(text: String, fileName: String) {
        val document = if (looksLikeCpset(text)) {
            when (val result = CpsetSerializer.read(text)) {
                is CpsetReadResult.Success -> result.document
                is CpsetReadResult.Failure -> {
                    Logger.e(TAG, "import failed: ${result.error} ${result.detail.orEmpty()}")
                    _uiState.value = ShareUiState.Error(result.error)
                    return
                }
            }
        } else {
            val song = ChordProParser.parse(
                text = text,
                id = generateUUID(),
                fallbackTitle = fileName.substringBeforeLast('.'),
            )
            if (song.sections.isEmpty()) {
                _uiState.value = ShareUiState.Error(CpsetError.EMPTY)
                return
            }
            CpsetDocument(exportedAt = now(), name = song.title, songs = listOf(song))
        }

        _uiState.value = ShareUiState.Previewing(
            CpsetSerializer.preview(document, repository.library.value)
        )
    }

    /** Applies the pending import with the chosen conflict [resolution]. */
    fun confirmImport(resolution: ConflictResolution) {
        val preview = (_uiState.value as? ShareUiState.Previewing)?.preview ?: return
        val merged = CpsetSerializer.apply(
            preview = preview,
            library = repository.library.value,
            resolution = resolution,
            importedAt = now(),
        )
        repository.replaceAll(merged)

        val imported = preview.newCount +
            if (resolution == ConflictResolution.REPLACE) preview.conflictCount else 0
        _uiState.value = ShareUiState.Imported(imported)
        Logger.d(TAG, "imported $imported items ($resolution)")
    }

    fun dismiss() { _uiState.value = ShareUiState.Idle }

    /** True when [text] is one of our documents rather than a song file. */
    internal fun looksLikeCpset(text: String): Boolean =
        text.trimStart().startsWith("{") && text.contains("\"format\"")
}
