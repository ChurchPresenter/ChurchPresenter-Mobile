package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.CPSET_FORMAT
import com.church.presenter.churchpresentermobile.model.CPSET_VERSION
import com.church.presenter.churchpresentermobile.model.ConflictResolution
import com.church.presenter.churchpresentermobile.model.ContentOrigin
import com.church.presenter.churchpresentermobile.model.CpsetDocument
import com.church.presenter.churchpresentermobile.model.CpsetError
import com.church.presenter.churchpresentermobile.model.CpsetReadResult
import com.church.presenter.churchpresentermobile.model.ImportPreview
import com.church.presenter.churchpresentermobile.model.LibraryData
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.serialization.json.Json

private const val TAG = "CpsetSerializer"

/**
 * Reads and writes `.cpset` files — the way a library or a Sunday set moves
 * between phones, or from a worship leader to an operator.
 *
 * Import never writes blind: [preview] reports what would change, the user
 * decides what to do about conflicts, and only then does [apply] produce the
 * merged library.
 */
object CpsetSerializer {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = true
    }

    // ── Writing ──────────────────────────────────────────────────────────

    /** Serializes [document] to the text written to a `.cpset` file. */
    fun write(document: CpsetDocument): String =
        json.encodeToString(CpsetDocument.serializer(), document)

    /** Builds a document containing the whole library. */
    fun exportLibrary(
        library: LibraryData,
        name: String,
        exportedAt: Long,
        appVersion: String = "",
    ): CpsetDocument = CpsetDocument(
        exportedAt = exportedAt,
        appVersion = appVersion,
        name = name,
        songs = library.songs,
        announcements = library.announcements,
        setlists = library.setlists,
    )

    /**
     * Builds a document containing just the songs and notices a setlist uses.
     *
     * This is the common case for sharing: a leader sends one Sunday, not their
     * entire hymnal.
     */
    fun exportSetlist(
        library: LibraryData,
        setlistId: String,
        exportedAt: Long,
        appVersion: String = "",
    ): CpsetDocument? {
        val setlist = library.setlists.firstOrNull { it.id == setlistId } ?: return null
        val referenced = setlist.entries.map { it.reference }.toSet()
        return CpsetDocument(
            exportedAt = exportedAt,
            appVersion = appVersion,
            name = setlist.name,
            songs = library.songs.filter { it.id in referenced },
            announcements = library.announcements.filter { it.id in referenced },
            setlists = listOf(setlist),
        )
    }

    // ── Reading ──────────────────────────────────────────────────────────

    /**
     * Parses [text] into a document.
     *
     * A version newer than [CPSET_VERSION] is refused outright rather than
     * partially read — silently dropping fields the sender considered important
     * is worse than saying "this needs a newer app".
     */
    fun read(text: String): CpsetReadResult {
        val document = runCatching { json.decodeFromString<CpsetDocument>(text) }
            .onFailure { Logger.e(TAG, "could not parse cpset: ${it.message}") }
            .getOrNull()
            ?: return CpsetReadResult.Failure(CpsetError.UNREADABLE)

        if (document.format != CPSET_FORMAT) {
            return CpsetReadResult.Failure(CpsetError.WRONG_FORMAT, document.format)
        }
        if (document.version > CPSET_VERSION) {
            return CpsetReadResult.Failure(CpsetError.TOO_NEW, document.version.toString())
        }
        if (document.isEmpty) {
            return CpsetReadResult.Failure(CpsetError.EMPTY)
        }
        return CpsetReadResult.Success(document)
    }

    // ── Merging ──────────────────────────────────────────────────────────

    /** Works out what importing [document] into [library] would change. */
    fun preview(document: CpsetDocument, library: LibraryData): ImportPreview {
        val existingSongKeys = library.songs.associateBy { it.importKey() }
        val existingNoticeKeys = library.announcements.associateBy { it.importKey() }

        val (conflictingSongs, newSongs) = document.songs
            .partition { it.importKey() in existingSongKeys }
        val (conflictingNotices, newNotices) = document.announcements
            .partition { it.importKey() in existingNoticeKeys }

        val existingSetlistIds = library.setlists.map { it.id }.toSet()

        return ImportPreview(
            document = document,
            newSongs = newSongs,
            conflictingSongs = conflictingSongs,
            newAnnouncements = newNotices,
            conflictingAnnouncements = conflictingNotices,
            newSetlists = document.setlists.filter { it.id !in existingSetlistIds },
        )
    }

    /**
     * Produces the library that results from accepting [preview].
     *
     * Imported items are marked [ContentOrigin.LOCAL]: they came from a person,
     * not from a desktop, so a later sync must not treat them as replaceable.
     */
    fun apply(
        preview: ImportPreview,
        library: LibraryData,
        resolution: ConflictResolution,
        importedAt: Long = 0L,
    ): LibraryData {
        val songs = library.songs.toMutableList()
        val announcements = library.announcements.toMutableList()

        preview.newSongs.forEach {
            songs += it.copy(origin = ContentOrigin.LOCAL, updatedAt = importedAt)
        }
        preview.newAnnouncements.forEach {
            announcements += it.copy(origin = ContentOrigin.LOCAL, updatedAt = importedAt)
        }

        if (resolution == ConflictResolution.REPLACE) {
            preview.conflictingSongs.forEach { incoming ->
                val index = songs.indexOfFirst { it.importKey() == incoming.importKey() }
                // Keep the existing id so any setlist pointing at it still resolves.
                if (index >= 0) {
                    songs[index] = incoming.copy(
                        id = songs[index].id,
                        origin = ContentOrigin.LOCAL,
                        updatedAt = importedAt,
                    )
                }
            }
            preview.conflictingAnnouncements.forEach { incoming ->
                val index = announcements.indexOfFirst { it.importKey() == incoming.importKey() }
                if (index >= 0) {
                    announcements[index] = incoming.copy(
                        id = announcements[index].id,
                        origin = ContentOrigin.LOCAL,
                        updatedAt = importedAt,
                    )
                }
            }
        }

        return library.copy(
            songs = songs,
            announcements = announcements,
            setlists = library.setlists + preview.newSetlists,
        )
    }

    /**
     * Identity for import purposes: songbook plus number, falling back to title.
     *
     * Matches [LibraryMerge]'s rule, so "already present" means the same thing
     * whether content arrives from a desktop or from a file.
     */
    private fun LocalSong.importKey(): String {
        val book = bookName.orEmpty().trim().lowercase()
        val num = number.trim().lowercase()
        return if (num.isEmpty()) "$book|title:${title.trim().lowercase()}" else "$book|$num"
    }

    private fun LocalAnnouncement.importKey(): String =
        title.trim().lowercase().ifEmpty { body.trim().lowercase().take(80) }
}
