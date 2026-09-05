package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.BibleLibraryIndex
import com.church.presenter.churchpresentermobile.model.InstalledBible
import com.church.presenter.churchpresentermobile.model.InstalledBibleBook
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

private const val TAG = "LocalBibleRepository"

internal const val BIBLE_INDEX_FILE = "bibles.json"
internal const val BIBLE_FILE_PREFIX = "bible_"

/**
 * Translations copied onto this device: a small index, and one `.spb` per translation.
 *
 * Deliberately *not* part of [LibraryRepository]. That one rewrites its whole document on every
 * song edit; folding 10 MB of Bible text into it would make each keystroke-saved song rewrite
 * 10 MB. Separate files, separate index.
 *
 * The modules are stored as the raw `.spb` they arrived as — it is the smallest form (re-encoding
 * as JSON would inflate 4.6 MB by half again), it round-trips for a future export, and the
 * desktop owns the format either way.
 *
 * @param now Supplies download timestamps. Injected so tests are deterministic.
 */
class LocalBibleRepository(
    private val storage: FileStore = createFileStore(),
    private val now: () -> Long = { 0L },
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val _index = MutableStateFlow(BibleLibraryIndex.EMPTY)

    /** Screens collect this rather than re-reading the index file. */
    val index: StateFlow<BibleLibraryIndex> = _index.asStateFlow()

    /**
     * Exactly one parsed module is held. A second would double an already large resident cost,
     * and nobody reads two translations at the same instant.
     */
    private var parsedId: String? = null
    private var parsed: ParsedBible? = null

    /** Reads the index. Safe to call more than once. */
    fun load(): BibleLibraryIndex {
        val text = storage.read(BIBLE_INDEX_FILE)
        val loaded = if (text.isNullOrBlank()) {
            BibleLibraryIndex.EMPTY
        } else {
            runCatching { json.decodeFromString<BibleLibraryIndex>(text) }
                .onFailure { Logger.e(TAG, "could not parse bible index: ${it.message}") }
                .getOrDefault(BibleLibraryIndex.EMPTY)
        }
        _index.value = loaded
        Logger.d(TAG, "loaded ${loaded.bibles.size} translations")
        return loaded
    }

    /**
     * Parses [text], stores the module, and records it in the index.
     *
     * Installing a translation already present replaces it rather than adding a second copy —
     * a re-sync after the desktop's text changed is the ordinary case, not an error.
     *
     * Returns null when the download was not a Bible at all, which is what a captive-portal
     * login page or a 404 body looks like by the time it reaches here.
     */
    fun install(fileName: String, text: String, sourceHost: String = ""): InstalledBible? {
        val bible = SpbParser.parse(text, fileName)
        if (bible.isEmpty) {
            Logger.e(TAG, "install '$fileName' — no verses parsed, refusing to store it")
            return null
        }
        val id = idFor(fileName)
        storage.write(moduleFile(id), text)

        val entry = InstalledBible(
            id = id,
            fileName = fileName,
            title = bible.title,
            verseCount = bible.verseCount,
            sizeBytes = storage.sizeBytes(moduleFile(id)),
            sourceHost = sourceHost,
            downloadedAtEpochMs = now(),
            books = bible.books.map {
                InstalledBibleBook(
                    bookId = it.bookId ?: 0,
                    name = it.displayName,
                    chapterCount = it.totalChapters,
                )
            },
        )
        mutate { current ->
            current.copy(
                bibles = current.bibles.filterNot { it.id == id } + entry,
                activeId = current.activeId.ifBlank { id },
            )
        }
        // The freshly parsed copy is almost certainly the next thing read.
        parsedId = id
        parsed = bible
        Logger.d(TAG, "installed '$fileName' — ${entry.verseCount} verses")
        return entry
    }

    /** Removes a translation and its module file. */
    fun remove(id: String) {
        storage.delete(moduleFile(id))
        if (parsedId == id) releaseParsed()
        mutate { current ->
            val remaining = current.bibles.filterNot { it.id == id }
            current.copy(
                bibles = remaining,
                activeId = if (current.activeId == id) remaining.firstOrNull()?.id.orEmpty()
                           else current.activeId,
            )
        }
    }

    /** Chooses which translation the Bible tab reads. */
    fun setActive(id: String) {
        if (_index.value.bibles.none { it.id == id }) return
        mutate { it.copy(activeId = id) }
    }

    /**
     * The parsed module for [id], reading and parsing it on first use.
     *
     * Callers should be off the main dispatcher: [FileStore] reads synchronously and a 4.6 MB
     * parse is not something to do on the frame the operator is looking at.
     */
    fun open(id: String): ParsedBible? {
        if (parsedId == id) return parsed
        val entry = _index.value.bibles.firstOrNull { it.id == id } ?: return null
        val text = storage.read(moduleFile(id)) ?: run {
            Logger.e(TAG, "module for '$id' is missing from storage")
            return null
        }
        val bible = SpbParser.parse(text, entry.fileName)
        parsedId = id
        parsed = bible
        return bible
    }

    /** The active translation, parsed. */
    fun openActive(): ParsedBible? = _index.value.active?.let { open(it.id) }

    /** Drops the parsed copy, which is the largest thing this app holds in memory. */
    fun releaseParsed() {
        parsedId = null
        parsed = null
    }

    private fun mutate(block: (BibleLibraryIndex) -> BibleLibraryIndex) {
        val updated = block(_index.value)
        _index.value = updated
        runCatching {
            storage.write(BIBLE_INDEX_FILE, json.encodeToString(BibleLibraryIndex.serializer(), updated))
        }
            .onFailure { Logger.e(TAG, "could not write bible index: ${it.message}") }
    }

    private fun moduleFile(id: String): String = "$BIBLE_FILE_PREFIX$id.spb"

    private companion object {
        /** A file name reduced to something safe to use as a storage key. */
        fun idFor(fileName: String): String =
            fileName.substringBeforeLast(".").map { if (it.isLetterOrDigit() || it == '_' || it == '-') it else '_' }
                .joinToString("")
    }
}
