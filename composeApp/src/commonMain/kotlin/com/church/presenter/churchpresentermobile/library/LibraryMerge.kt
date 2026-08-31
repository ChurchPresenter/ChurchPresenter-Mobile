package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.ContentOrigin
import com.church.presenter.churchpresentermobile.model.LocalSong

/**
 * The result of merging a desktop catalogue into the on-device library.
 *
 * @param kept Songs left alone because the user owns them — either authored
 *   locally or edited after being synced.
 * @param removed Songs dropped because the desktop no longer has them.
 */
data class MergeResult(
    val songs: List<LocalSong>,
    val added: Int = 0,
    val updated: Int = 0,
    val kept: Int = 0,
    val removed: Int = 0,
)

/**
 * Decides what a desktop sync is allowed to do to the on-device library.
 *
 * This is the part of sync worth being careful about. A church's operator may
 * have spent an evening fixing a typo in a hymn or adding a verse the desktop
 * lacks; a re-sync that silently reverted that would be worse than no sync at
 * all. So the rules are conservative in the user's favour and every skip is
 * counted so the UI can report it rather than swallow it.
 *
 * Kept pure and separate from the network service so all of it is testable.
 */
object LibraryMerge {

    /**
     * Merges [incoming] desktop songs into [existing].
     *
     * Matching is on `(songbook, number)` rather than id, because ids are not
     * shared between a desktop and a phone.
     *
     * @param referencedIds Song ids used by a setlist. A desktop song that has
     *   disappeared is normally dropped, but not while a saved service still
     *   points at it — losing an item from next Sunday's set is not a tidy-up.
     * @param removeMissing Whether [incoming] is the whole catalogue. False when
     *   it is one batch of a larger sync: a batch says nothing about the songs
     *   the other batches carry, so treating what it omits as deleted wipes
     *   every batch before it. Pass false and prune once at the end with
     *   [pruneMissing].
     */
    fun mergeSongs(
        existing: List<LocalSong>,
        incoming: List<LocalSong>,
        referencedIds: Set<String> = emptySet(),
        removeMissing: Boolean = true,
    ): MergeResult {
        val existingByKey = existing.associateBy { it.matchKey() }
        val incomingKeys = incoming.map { it.matchKey() }.toSet()

        var added = 0
        var updated = 0
        var kept = 0

        val merged = mutableListOf<LocalSong>()

        // Anything the user owns survives untouched, whatever the desktop says.
        existing.forEach { song ->
            when (song.origin) {
                ContentOrigin.LOCAL, ContentOrigin.LOCAL_OVERRIDE -> {
                    merged += song
                    // Only count it as "kept" when the desktop actually tried to
                    // replace it — an unrelated local song is not a conflict.
                    if (song.matchKey() in incomingKeys) kept++
                }
                ContentOrigin.DESKTOP -> Unit // handled below
            }
        }

        incoming.forEach { song ->
            val key = song.matchKey()
            val current = existingByKey[key]
            when {
                // The user owns this slot — do not add a competing duplicate.
                current != null && current.origin != ContentOrigin.DESKTOP -> Unit

                current == null -> {
                    merged += song.copy(origin = ContentOrigin.DESKTOP)
                    added++
                }

                else -> {
                    // Replace wholesale, but keep the id so setlists still resolve.
                    merged += song.copy(id = current.id, origin = ContentOrigin.DESKTOP)
                    updated++
                }
            }
        }

        // Desktop songs this sync did not bring, unless a setlist needs them.
        // Only a whole catalogue is evidence of a deletion — see [removeMissing].
        val missing = existing.filter {
            it.origin == ContentOrigin.DESKTOP && it.matchKey() !in incomingKeys
        }
        val stale = if (removeMissing) missing.filter { it.id !in referencedIds } else emptyList()
        merged += missing - stale.toSet()

        return MergeResult(
            songs = merged.sortedWith(songOrder),
            added = added,
            updated = updated,
            kept = kept,
            removed = stale.size,
        )
    }

    /**
     * Identity across a desktop and a phone: songbook plus number.
     *
     * Falls back to the title when a song has no number, which is how
     * user-authored songs and some imported formats arrive.
     */
    private fun LocalSong.matchKey(): String = matchKey(bookName, number, title)

    /**
     * The same identity, for callers holding a catalogue entry rather than a
     * [LocalSong] — [pruneMissing] needs a key for songs whose lyrics never
     * arrived, and those exist only as the catalogue row that named them.
     */
    fun matchKey(bookName: String?, number: String, title: String): String {
        val book = bookName.orEmpty().trim().lowercase()
        val num = number.trim().lowercase()
        return if (num.isEmpty()) "$book|title:${title.trim().lowercase()}" else "$book|$num"
    }

    /**
     * Drops desktop songs the computer no longer has, once every batch of a sync
     * has been merged.
     *
     * The deletion half of [mergeSongs], split out because a batched sync only
     * knows the whole catalogue up front, never inside one batch.
     *
     * @param catalogueKeys [matchKey] for every song the computer offered this
     *   sync — including ones whose lyrics failed to download, which are still
     *   songs the computer has.
     * @param books When the operator copied only some songbooks, the books that
     *   were copied. Songs from any other book are outside what this sync asked
     *   about and are left alone; without this, copying one book would delete
     *   every other book already on the phone.
     */
    fun pruneMissing(
        existing: List<LocalSong>,
        catalogueKeys: Set<String>,
        referencedIds: Set<String> = emptySet(),
        books: Set<String>? = null,
    ): MergeResult {
        val stale = existing.filter { song ->
            song.origin == ContentOrigin.DESKTOP &&
                song.matchKey() !in catalogueKeys &&
                song.id !in referencedIds &&
                (books == null || song.bookName.orEmpty() in books)
        }.toSet()

        return MergeResult(
            songs = existing.filterNot { it in stale }.sortedWith(songOrder),
            removed = stale.size,
        )
    }

    /** Songbook, then number read as a number where possible, then title. */
    private val songOrder = compareBy<LocalSong>(
        { it.bookName.orEmpty().lowercase() },
        { it.number.filter(Char::isDigit).toIntOrNull() ?: Int.MAX_VALUE },
        { it.number.lowercase() },
        { it.title.lowercase() },
    )
}
