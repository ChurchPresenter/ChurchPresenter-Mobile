package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.ContentOrigin
import com.church.presenter.churchpresentermobile.model.LibraryData
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.model.LocalSetlist
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

private const val TAG = "LibraryRepository"

internal const val LIBRARY_FILE = "library.json"
internal const val LIBRARY_BACKUP_FILE = "library.bak"

/**
 * The on-device library: one JSON document, held in memory and written through
 * on every change.
 *
 * Writes keep the previous good copy as a backup and a corrupt read falls back
 * to it, because the failure this guards against is not hypothetical — a phone
 * killed mid-write during a service would otherwise lose a hand-typed songbook.
 *
 * @param now Supplies edit timestamps. Injected so tests are deterministic and
 *   so the models stay free of clock access.
 */
class LibraryRepository(
    private val storage: FileStore = createFileStore(),
    private val now: () -> Long = { 0L },
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val _library = MutableStateFlow(LibraryData.EMPTY)

    /** The whole library. Screens collect this rather than re-reading the file. */
    val library: StateFlow<LibraryData> = _library.asStateFlow()

    val songs: List<LocalSong> get() = _library.value.songs
    val announcements: List<LocalAnnouncement> get() = _library.value.announcements
    val setlists: List<LocalSetlist> get() = _library.value.setlists

    /**
     * Reads the library from disk, falling back to the backup when the main file
     * is unreadable or corrupt. Safe to call more than once.
     */
    fun load(): LibraryData {
        val loaded = parse(storage.read(LIBRARY_FILE))
            ?: parse(storage.read(LIBRARY_BACKUP_FILE))?.also {
                Logger.e(TAG, "main library file was unreadable — recovered from backup")
            }
            ?: LibraryData.EMPTY
        _library.value = loaded
        Logger.d(TAG, "loaded ${loaded.songs.size} songs, ${loaded.announcements.size} notices")
        return loaded
    }

    private fun parse(text: String?): LibraryData? {
        if (text.isNullOrBlank()) return null
        return runCatching { json.decodeFromString<LibraryData>(text) }
            .onFailure { Logger.e(TAG, "could not parse library: ${it.message}") }
            .getOrNull()
    }

    // ── Songs ────────────────────────────────────────────────────────────

    fun song(id: String): LocalSong? = _library.value.songs.firstOrNull { it.id == id }

    /**
     * Inserts or updates [song].
     *
     * Editing an item that came from a desktop marks it [ContentOrigin.LOCAL_OVERRIDE],
     * which is how a later sync knows to preserve the user's version instead of
     * quietly replacing it.
     */
    fun upsertSong(song: LocalSong) {
        val existing = song(song.id)
        val origin = when {
            existing == null -> song.origin
            existing.origin == ContentOrigin.DESKTOP -> ContentOrigin.LOCAL_OVERRIDE
            else -> existing.origin
        }
        val updated = song.copy(origin = origin, updatedAt = now())
        mutate { data ->
            data.copy(songs = data.songs.replaceOrAdd(updated) { it.id == updated.id })
        }
    }

    fun deleteSong(id: String) {
        mutate { data -> data.copy(songs = data.songs.filterNot { it.id == id }) }
    }

    // ── Announcements ────────────────────────────────────────────────────

    fun announcement(id: String): LocalAnnouncement? =
        _library.value.announcements.firstOrNull { it.id == id }

    fun upsertAnnouncement(announcement: LocalAnnouncement) {
        val existing = announcement(announcement.id)
        val origin = when {
            existing == null -> announcement.origin
            existing.origin == ContentOrigin.DESKTOP -> ContentOrigin.LOCAL_OVERRIDE
            else -> existing.origin
        }
        val updated = announcement.copy(origin = origin, updatedAt = now())
        mutate { data ->
            data.copy(announcements = data.announcements.replaceOrAdd(updated) { it.id == updated.id })
        }
    }

    fun deleteAnnouncement(id: String) {
        mutate { data -> data.copy(announcements = data.announcements.filterNot { it.id == id }) }
    }

    // ── Setlists ─────────────────────────────────────────────────────────

    fun setlist(id: String): LocalSetlist? = _library.value.setlists.firstOrNull { it.id == id }

    fun upsertSetlist(setlist: LocalSetlist) {
        val updated = setlist.copy(updatedAt = now())
        mutate { data ->
            data.copy(setlists = data.setlists.replaceOrAdd(updated) { it.id == updated.id })
        }
    }

    fun deleteSetlist(id: String) {
        mutate { data -> data.copy(setlists = data.setlists.filterNot { it.id == id }) }
    }

    // ── Bulk ─────────────────────────────────────────────────────────────

    /** Replaces the whole library — used by import and by a full desktop sync. */
    fun replaceAll(data: LibraryData) {
        mutate { data }
    }

    /** Empties the library on disk and in memory. */
    fun clear() {
        mutate { LibraryData.EMPTY }
    }

    // ── Persistence ──────────────────────────────────────────────────────

    private inline fun mutate(transform: (LibraryData) -> LibraryData) {
        val updated = transform(_library.value)
        _library.value = updated
        persist(updated)
    }

    private fun persist(data: LibraryData) {
        runCatching {
            // Roll the current file to the backup first, so a failure part-way
            // through the new write still leaves a readable library behind.
            storage.read(LIBRARY_FILE)?.let { storage.write(LIBRARY_BACKUP_FILE, it) }
            storage.write(LIBRARY_FILE, json.encodeToString(LibraryData.serializer(), data))
        }.onFailure { Logger.e(TAG, "could not save library: ${it.message}") }
    }
}

/** Replaces the first element matching [predicate], or appends when there is none. */
private inline fun <T> List<T>.replaceOrAdd(item: T, predicate: (T) -> Boolean): List<T> {
    val index = indexOfFirst(predicate)
    return if (index < 0) this + item else toMutableList().also { it[index] = item }
}
