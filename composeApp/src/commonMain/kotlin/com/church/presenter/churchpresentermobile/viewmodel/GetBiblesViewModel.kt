package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.bibleimport.catalog.BibleDownloads
import com.church.presenter.churchpresentermobile.bibleimport.catalog.BibleSource
import com.church.presenter.churchpresentermobile.bibleimport.catalog.BibleWebCatalog
import com.church.presenter.churchpresentermobile.bibleimport.catalog.CatalogBible
import com.church.presenter.churchpresentermobile.bibleimport.catalog.CatalogSnapshot
import com.church.presenter.churchpresentermobile.bibleimport.catalog.InstallState
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.InstalledBible
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "GetBiblesViewModel"

/** Where a catalogue row stands on this device, which decides what its trailing control shows. */
sealed interface RowStatus {
    data object Available : RowStatus
    data object Installed : RowStatus
    data object Queued : RowStatus

    /** [fraction] spans the whole install — download, then conversion — so the bar never resets. */
    data class Installing(val fraction: Float) : RowStatus
    data object Failed : RowStatus
}

data class CatalogRow(val bible: CatalogBible, val status: RowStatus)

/** One entry in the language picker. [code] is null for "All languages". */
data class LanguageOption(val code: String?, val name: String, val count: Int)

/** What the catalogue screen draws, derived in one place from everything that can change it. */
data class CatalogView(
    val rows: List<CatalogRow> = emptyList(),
    /** Every language the chosen source has, most translations first, "All languages" leading. */
    val languages: List<LanguageOption> = emptyList(),
    /** Languages of the translations already on this device — the picker's quick chips. */
    val deviceLanguages: List<LanguageOption> = emptyList(),
    /** How many translations the chosen source has before the search and language narrow it. */
    val sourceTotal: Int = 0,
)

/**
 * The catalogue screen: one list across eBible.org, Zefania and Holy Bible XML.
 *
 * Installing is handed to [BibleDownloads], which outlives this screen — so leaving mid-download
 * leaves the download running, and coming back finds it where it was.
 */
class GetBiblesViewModel(
    private val catalog: BibleWebCatalog,
    private val downloads: BibleDownloads,
    private val repository: LocalBibleRepository,
) : ViewModel() {

    private val _snapshot = MutableStateFlow(CatalogSnapshot.EMPTY)
    val snapshot: StateFlow<CatalogSnapshot> = _snapshot.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _filter = MutableStateFlow(CatalogFilter())
    val filter: StateFlow<CatalogFilter> = _filter.asStateFlow()

    /**
     * The row waiting on the copyright confirmation. Nothing is downloaded until the operator
     * confirms they are permitted to use the translation — the desktop asks the same question
     * before every catalogue download.
     */
    private val _licenceFor = MutableStateFlow<CatalogBible?>(null)
    val licenceFor: StateFlow<CatalogBible?> = _licenceFor.asStateFlow()

    /** The install sheet: which install it shows, and what its buttons do. */
    val sheet = InstallSheetController(downloads, repository)

    val installs: StateFlow<Map<String, InstallState>> = downloads.states

    val view: StateFlow<CatalogView> =
        combine(_snapshot, _filter, repository.index, downloads.states) { snapshot, filter, index, installs ->
            buildCatalogView(snapshot.bibles, filter, index.bibles, installs)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, CatalogView())

    init {
        load(refresh = false)
    }

    fun load(refresh: Boolean) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                _snapshot.value = catalog.load(refresh)
                Logger.d(TAG, "catalogue loaded — ${_snapshot.value.bibles.size} translations")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Null is "All". */
    fun setSource(source: BibleSource?) = _filter.update { it.copy(source = source) }

    fun setQuery(query: String) = _filter.update { it.copy(query = query) }

    /** Null is "All languages". */
    fun setLanguage(code: String?) = _filter.update { it.copy(language = code) }

    /** Asks for the copyright confirmation; the install starts only once it is given. */
    fun requestInstall(bible: CatalogBible) {
        _licenceFor.value = bible
    }

    /** True when installing [bible] would replace a translation already on this device. */
    fun isReinstall(bible: CatalogBible): Boolean =
        repository.index.value.bibles.any { it.catalogKey == bible.key || it.fileName == bible.fileName }

    /** "I understand — Download": starts the install and opens its sheet — design 3a. */
    fun confirmLicence() {
        val bible = _licenceFor.value ?: return
        _licenceFor.value = null
        downloads.install(bible)
        sheet.show(bible.key)
    }

    fun dismissLicence() {
        _licenceFor.value = null
    }
}

/** What narrows the catalogue: a source (null is "All"), a search, and a language (null is all). */
data class CatalogFilter(
    val source: BibleSource? = null,
    val query: String = "",
    val language: String? = null,
)

/**
 * The install sheet's state and buttons — design 3a–3d.
 *
 * Kept apart from [GetBiblesViewModel] because the sheet is about one install and the screen is
 * about the catalogue: the two share nothing but [BibleDownloads].
 */
class InstallSheetController(
    private val downloads: BibleDownloads,
    private val repository: LocalBibleRepository,
) {
    /** The install the sheet shows, by catalogue key; null when it is closed. */
    private val _key = MutableStateFlow<String?>(null)
    val key: StateFlow<String?> = _key.asStateFlow()

    /** Opens the sheet of an install — one just started, running, or failed. */
    fun show(key: String) {
        _key.value = key
    }

    /** "Run in background": the sheet goes, the install carries on. */
    fun hide() {
        _key.value = null
    }

    fun cancel(key: String) {
        downloads.cancel(key)
        _key.value = null
    }

    /** A retry of a failed install; its copyright was confirmed when it was first started. */
    fun retry(key: String) = downloads.retry(key)

    /** "Done" on the ready sheet: the finished install is forgotten and the sheet closes. */
    fun finish(key: String) {
        downloads.dismiss(key)
        _key.value = null
    }

    /** "Open in Bible": the new translation becomes the one read. */
    fun openInBible(key: String) {
        (downloads.states.value[key] as? InstallState.Done)?.let { repository.setActive(it.installed.id) }
        finish(key)
    }
}

/**
 * The rows, languages and chips the catalogue shows for these filters — pure, so it is tested
 * without a ViewModel.
 *
 * Rows are alphabetical by language, then by name, ignoring a leading apostrophe or bracket so
 * "'Auhelawa" files under A. The languages the device already has are one tap away in the
 * language picker's "On this device" chips.
 */
internal fun buildCatalogView(
    all: List<CatalogBible>,
    filter: CatalogFilter,
    installed: List<InstalledBible>,
    installs: Map<String, InstallState>,
): CatalogView {
    val inSource = if (filter.source == null) all else all.filter { it.source == filter.source }
    val installedKeys = installed.map { it.catalogKey }.filter { it.isNotBlank() }.toSet()
    val installedFiles = installed.map { it.fileName }.toSet()
    val deviceLanguageNames = installed.map { it.languageName }.filter { it.isNotBlank() }.toSet()

    val rows = inSource
        .filter { filter.language == null || it.language == filter.language }
        .filter { it.matches(filter.query) }
        .sortedWith(
            compareBy<CatalogBible> { sortKey(it.languageLabel) }
                .thenBy { sortKey(it.displayName) }
                .thenBy { it.source.ordinal },
        )
        .map { bible ->
            val alreadyInstalled = bible.key in installedKeys || bible.fileName in installedFiles
            CatalogRow(bible, rowStatus(installs[bible.key], alreadyInstalled))
        }

    val byLanguage = inSource.groupBy { it.language }
        .map { (code, bibles) -> LanguageOption(code, bibles.first().languageLabel, bibles.size) }
        .sortedBy { sortKey(it.name) }
    val languages = listOf(LanguageOption(null, "", inSource.size)) + byLanguage
    val deviceLanguages = byLanguage.filter { it.name in deviceLanguageNames }

    return CatalogView(rows, languages, deviceLanguages, inSource.size)
}

private fun rowStatus(install: InstallState?, alreadyInstalled: Boolean): RowStatus = when (install) {
    is InstallState.Queued -> RowStatus.Queued
    is InstallState.Running -> RowStatus.Installing(installFraction(install))
    is InstallState.Failed -> RowStatus.Failed
    is InstallState.Done -> RowStatus.Installed
    null -> if (alreadyInstalled) RowStatus.Installed else RowStatus.Available
}

/** Alphabetical order that ignores case and a leading apostrophe, quote or bracket. */
private fun sortKey(text: String): String = text.trimStart { !it.isLetterOrDigit() }.lowercase()

/** Download is the first half of the bar and conversion the second. */
internal fun installFraction(state: InstallState.Running): Float {
    val progress = state.progress
    val total = progress.totalBytes
    val download = if (total != null && total > 0) {
        (progress.downloadedBytes.toFloat() / total).coerceIn(0f, 1f)
    } else {
        0f
    }
    val convert = progress.convert?.fraction ?: 0f
    return if (progress.convert == null && progress.downloadMs == null) download * HALF else HALF + convert * HALF
}

private const val HALF = 0.5f
