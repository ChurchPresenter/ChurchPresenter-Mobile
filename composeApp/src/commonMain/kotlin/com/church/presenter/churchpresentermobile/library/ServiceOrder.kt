package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.model.LocalSetlist
import com.church.presenter.churchpresentermobile.model.LocalSetlistEntry
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.model.SetlistEntryType
import com.church.presenter.churchpresentermobile.model.Song
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val TAG = "ServiceOrder"

/**
 * Id of the reserved setlist that holds the running order for the service in
 * progress. Reserved rather than user-created: there is exactly one "what we
 * are presenting today", and every screen has to mean the same one.
 */
const val CURRENT_SERVICE_SETLIST_ID = "current-service"

/**
 * What "add to schedule" means with no desktop to add to.
 *
 * Standalone had the action wired to the desktop's schedule, so
 * [com.church.presenter.churchpresentermobile.present.StandaloneEngine]
 * swallowed it and reported success for something that never happened; the UI
 * dealt with that by hiding the button. This is the honest version of the same
 * feature — an ordered list, on this device, that the drawer shows and the
 * operator works through.
 *
 * Backed by a [LocalSetlist] in the library rather than a store of its own, so
 * it persists with everything else, survives a restart mid-service, and is
 * already carried by the `.cpset` export format.
 *
 * A song may legitimately appear twice in one service, so nothing here
 * de-duplicates.
 */
class ServiceOrder(private val repository: LibraryRepository) {

    /** The running order, re-read whenever the library changes. */
    val entries: Flow<List<LocalSetlistEntry>> =
        repository.library.map { data -> data.setlists.firstOrNull { it.id == CURRENT_SERVICE_SETLIST_ID }?.entries.orEmpty() }

    /** The running order as it stands right now. */
    val current: List<LocalSetlistEntry>
        get() = repository.setlist(CURRENT_SERVICE_SETLIST_ID)?.entries.orEmpty()

    /**
     * Adds a song from the Songs tab.
     *
     * [Song.localId] is what the library knows it by; the number is a fallback
     * for a row that predates it, mirroring how SongCatalog resolves a song
     * whose list entry was built before an edit.
     */
    fun add(song: Song) = append(
        LocalSetlistEntry(
            type = SetlistEntryType.SONG,
            reference = song.localId ?: song.number,
            title = song.title,
        )
    )

    /** Adds a song straight from the Library tab. */
    fun add(song: LocalSong) = append(
        LocalSetlistEntry(type = SetlistEntryType.SONG, reference = song.id, title = song.title)
    )

    /** Adds an announcement straight from the Library tab. */
    fun add(announcement: LocalAnnouncement) = append(
        LocalSetlistEntry(
            type = SetlistEntryType.ANNOUNCEMENT,
            reference = announcement.id,
            title = announcement.title.ifBlank { announcement.body.lineSequence().first() },
        )
    )

    /**
     * Adds a passage.
     *
     * @param reference Machine-readable, as [LocalSetlistEntry] documents: `John:3:16-18`.
     * @param title What the operator sees: `John 3:16-18`.
     */
    fun addPassage(reference: String, title: String) = append(
        LocalSetlistEntry(type = SetlistEntryType.BIBLE, reference = reference, title = title)
    )

    /** Removes the entry at [index]; out-of-range indices are ignored. */
    fun removeAt(index: Int) {
        val entries = current
        if (index !in entries.indices) return
        write(entries.filterIndexed { i, _ -> i != index })
    }

    /**
     * Moves the entry at [from] to [to] — the running order is rehearsed, so it
     * gets reordered. Out-of-range indices are ignored rather than clamped: a
     * drag that ends off the list should leave the order alone, not move the
     * item somewhere the operator didn't drop it.
     */
    fun move(from: Int, to: Int) {
        val entries = current.toMutableList()
        if (from !in entries.indices || to !in entries.indices || from == to) return
        entries.add(to, entries.removeAt(from))
        write(entries)
    }

    /** Empties the running order, ready for the next service. */
    fun clear() = write(emptyList())

    private fun append(entry: LocalSetlistEntry) {
        Logger.d(TAG, "add — ${entry.type} ${entry.title}")
        write(current + entry)
    }

    private fun write(entries: List<LocalSetlistEntry>) {
        val existing = repository.setlist(CURRENT_SERVICE_SETLIST_ID)
            ?: LocalSetlist(id = CURRENT_SERVICE_SETLIST_ID)
        repository.upsertSetlist(existing.copy(entries = entries))
    }
}
