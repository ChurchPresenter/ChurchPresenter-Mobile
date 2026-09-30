package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.bibleimport.BibleNaming
import com.church.presenter.churchpresentermobile.bibleimport.BibleXmlFormat
import com.church.presenter.churchpresentermobile.bibleimport.BibleXmlReader
import com.church.presenter.churchpresentermobile.bibleimport.ImportHints
import com.church.presenter.churchpresentermobile.bibleimport.SourceBible
import com.church.presenter.churchpresentermobile.bibleimport.SpbWriter
import com.church.presenter.churchpresentermobile.bibleimport.XmlText
import com.church.presenter.churchpresentermobile.bibleimport.catalog.BibleLanguageNames
import com.church.presenter.churchpresentermobile.bibleimport.catalog.InstallException
import com.church.presenter.churchpresentermobile.bibleimport.catalog.InstallFailure
import com.church.presenter.churchpresentermobile.bibleimport.catalog.readBible
import com.church.presenter.churchpresentermobile.library.InstallDetails
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.InstalledBible
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ConvertBibleViewModel"

/** The file the operator picked, as the screen names it. */
data class PickedBible(val fileName: String, val sizeBytes: Long)

/** What reading the file found — design 4a's "Detected" card and preview. */
data class DetectedBible(
    val format: BibleXmlFormat,
    val languageName: String,
    val books: Int,
    val verses: Int,
    /** "John 3:16" and its text, or null when the file has neither John nor any verse. */
    val preview: Pair<String, String>?,
)

/** Where the convert screen is. */
sealed interface ConvertPhase {
    /** No file yet — the screen offers the picker. */
    data object Empty : ConvertPhase
    data object Reading : ConvertPhase
    data class Ready(val detected: DetectedBible) : ConvertPhase

    /** The file could not be read as a Bible; design 4c. */
    data class Unreadable(val failure: InstallFailure) : ConvertPhase
    data object Installing : ConvertPhase
    data class Installed(val bible: InstalledBible) : ConvertPhase
}

/**
 * Converting a Bible XML file from the device — design 4.
 *
 * The file is read the moment it is picked, so the screen can say what it is (format, language,
 * books, verses, John 3:16) before the operator commits to a name for it.
 *
 * @param work Where the parse runs. A 21 MB file is not something to read on the frame being drawn.
 */
class ConvertBibleViewModel(
    private val repository: LocalBibleRepository,
    private val work: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _file = MutableStateFlow<PickedBible?>(null)
    val file: StateFlow<PickedBible?> = _file.asStateFlow()

    private val _phase = MutableStateFlow<ConvertPhase>(ConvertPhase.Empty)
    val phase: StateFlow<ConvertPhase> = _phase.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _abbreviation = MutableStateFlow("")
    val abbreviation: StateFlow<String> = _abbreviation.asStateFlow()

    private var parsed: SourceBible? = null

    /** The copyright confirmation is up; nothing is installed until it is accepted. */
    private val _licencePending = MutableStateFlow(false)
    val licencePending: StateFlow<Boolean> = _licencePending.asStateFlow()

    /** The copyright statement the file makes about itself, for the confirmation. */
    val fileRights: String get() = parsed?.rights.orEmpty()

    /** Reads [bytes] as a Bible. Called with the picker's result. */
    fun pick(fileName: String, bytes: ByteArray) {
        _file.value = PickedBible(fileName, bytes.size.toLong())
        _phase.value = ConvertPhase.Reading
        parsed = null
        viewModelScope.launch {
            _phase.value = try {
                val (bible, format) = withContext(work) {
                    val text = XmlText.decode(bytes)
                    val detectedFormat = BibleXmlReader.detect(text)
                    readBible(bytes, ImportHints()) to detectedFormat
                }
                parsed = bible
                _title.value = bible.name.takeUnless { it == "Unknown" } ?: fileName.substringBeforeLast('.')
                _abbreviation.value = BibleNaming.fileStem(
                    bible.language,
                    bible.identifier.ifBlank { BibleNaming.abbreviation(_title.value) },
                )
                ConvertPhase.Ready(
                    DetectedBible(
                        format = format ?: BibleXmlFormat.ZEFANIA,
                        languageName = BibleLanguageNames.nameOf(bible.language),
                        books = bible.books.size,
                        verses = bible.verseCount,
                        preview = bible.previewVerse(),
                    ),
                )
            } catch (e: InstallException) {
                Logger.e(TAG, "could not read '$fileName': ${e.failure} ${e.message}")
                ConvertPhase.Unreadable(e.failure)
            }
        }
    }

    fun setTitle(value: String) {
        _title.value = value
    }

    fun setAbbreviation(value: String) {
        _abbreviation.value = value
    }

    /** True when a translation of that file name is already installed and would be replaced. */
    fun replacesInstalled(abbreviation: String = _abbreviation.value): Boolean =
        repository.index.value.bibles.any { it.fileName.equals(convertedFileName(abbreviation), ignoreCase = true) }

    /** "Convert & install": asks for the copyright confirmation first, as a download does. */
    fun requestConvert() {
        if (parsed != null) _licencePending.value = true
    }

    fun dismissLicence() {
        _licencePending.value = false
    }

    /** Confirmed: writes the module and installs it. */
    fun confirmLicence() {
        _licencePending.value = false
        convertAndInstall()
    }

    private fun convertAndInstall() {
        val bible = parsed ?: return
        val title = _title.value.trim().ifBlank { bible.name }
        val abbreviation = _abbreviation.value.trim()
        _phase.value = ConvertPhase.Installing
        viewModelScope.launch {
            val installed = withContext(work) {
                repository.install(
                    fileName = convertedFileName(abbreviation),
                    text = SpbWriter.write(bible, title = title, abbreviation = abbreviation.ifBlank { BibleNaming.abbreviation(title) }),
                    details = InstallDetails(
                        languageName = BibleLanguageNames.nameOf(bible.language),
                        origin = InstallDetails.ORIGIN_FILE,
                        license = bible.rights,
                    ),
                )
            }
            _phase.value = if (installed != null) {
                ConvertPhase.Installed(installed)
            } else {
                ConvertPhase.Unreadable(InstallFailure.STORAGE)
            }
        }
    }

    /** "Open in Bible" once installed: the converted translation becomes the one read. */
    fun openInBible() {
        (_phase.value as? ConvertPhase.Installed)?.let { repository.setActive(it.bible.id) }
    }

    /** Back to an empty screen, for "Choose another file". */
    fun reset() {
        parsed = null
        _file.value = null
        _phase.value = ConvertPhase.Empty
    }
}

/** The file a converted module is stored as — "Saved as UKR_OGI.spb" — from its abbreviation. */
internal fun convertedFileName(abbreviation: String): String =
    BibleNaming.typedStem(abbreviation).ifBlank { BibleNaming.UNKNOWN_LANGUAGE } + ".spb"
