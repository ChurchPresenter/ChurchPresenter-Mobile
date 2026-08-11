package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.model.LocalSong

/**
 * Ranked in-memory search over the library.
 *
 * Ranking, not just filtering: an operator typing "23" during a service wants
 * hymn 23, not the forty songs whose lyrics happen to contain "23". Exact
 * number beats title prefix beats title substring beats a match buried in the
 * lyrics — and ties keep their existing order so results do not shuffle as the
 * user types.
 */
object LibrarySearch {

    /** Score bands. Higher is better; [NO_MATCH] means "exclude". */
    private const val EXACT_NUMBER = 100
    private const val NUMBER_PREFIX = 90
    private const val TITLE_EXACT = 80
    private const val TITLE_PREFIX = 70
    private const val TITLE_CONTAINS = 60
    private const val AUTHOR_CONTAINS = 40
    private const val BODY_CONTAINS = 20
    private const val NO_MATCH = 0

    /** Returns the songs matching [query], best first. A blank query returns all. */
    fun songs(songs: List<LocalSong>, query: String): List<LocalSong> {
        val needle = query.trim()
        if (needle.isEmpty()) return songs
        return songs
            .map { it to scoreSong(it, needle) }
            .filter { (_, score) -> score > NO_MATCH }
            // sortedByDescending is stable, so equal scores keep library order.
            .sortedByDescending { (_, score) -> score }
            .map { (song, _) -> song }
    }

    /** Returns the announcements matching [query], best first. A blank query returns all. */
    fun announcements(items: List<LocalAnnouncement>, query: String): List<LocalAnnouncement> {
        val needle = query.trim()
        if (needle.isEmpty()) return items
        return items
            .map { it to scoreAnnouncement(it, needle) }
            .filter { (_, score) -> score > NO_MATCH }
            .sortedByDescending { (_, score) -> score }
            .map { (item, _) -> item }
    }

    internal fun scoreSong(song: LocalSong, query: String): Int {
        val q = query.lowercase()
        val number = song.number.lowercase()
        val title = song.title.lowercase()

        return when {
            number.isNotEmpty() && number == q -> EXACT_NUMBER
            number.isNotEmpty() && number.startsWith(q) -> NUMBER_PREFIX
            title == q -> TITLE_EXACT
            title.startsWith(q) -> TITLE_PREFIX
            title.contains(q) -> TITLE_CONTAINS
            song.author?.lowercase()?.contains(q) == true -> AUTHOR_CONTAINS
            song.bookName?.lowercase()?.contains(q) == true -> AUTHOR_CONTAINS
            song.sections.any { it.text.lowercase().contains(q) } -> BODY_CONTAINS
            else -> NO_MATCH
        }
    }

    internal fun scoreAnnouncement(item: LocalAnnouncement, query: String): Int {
        val q = query.lowercase()
        val title = item.title.lowercase()
        return when {
            title == q -> TITLE_EXACT
            title.startsWith(q) -> TITLE_PREFIX
            title.contains(q) -> TITLE_CONTAINS
            item.body.lowercase().contains(q) -> BODY_CONTAINS
            else -> NO_MATCH
        }
    }
}
