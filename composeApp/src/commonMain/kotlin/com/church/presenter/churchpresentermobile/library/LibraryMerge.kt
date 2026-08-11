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
     */
    fun mergeSongs(
        existing: List<LocalSong>,
        incoming: List<LocalSong>,
        referencedIds: Set<String> = emptySet(),
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

        // Desktop songs the desktop no longer has, unless a setlist needs them.
        val stale = existing.filter {
            it.origin == ContentOrigin.DESKTOP &&
                it.matchKey() !in incomingKeys &&
                it.id !in referencedIds
        }
        val rescued = existing.filter {
            it.origin == ContentOrigin.DESKTOP &&
                it.matchKey() !in incomingKeys &&
                it.id in referencedIds
        }
        merged += rescued

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
    private fun LocalSong.matchKey(): String {
        val book = bookName.orEmpty().trim().lowercase()
        val num = number.trim().lowercase()
        return if (num.isEmpty()) "$book|title:${title.trim().lowercase()}" else "$book|$num"
    }

    /** Songbook, then number read as a number where possible, then title. */
    private val songOrder = compareBy<LocalSong>(
        { it.bookName.orEmpty().lowercase() },
        { it.number.filter(Char::isDigit).toIntOrNull() ?: Int.MAX_VALUE },
        { it.number.lowercase() },
        { it.title.lowercase() },
    )
}
