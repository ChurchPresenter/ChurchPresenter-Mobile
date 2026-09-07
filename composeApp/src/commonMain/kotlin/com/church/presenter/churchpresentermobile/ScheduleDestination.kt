package com.church.presenter.churchpresentermobile

import com.church.presenter.churchpresentermobile.model.ScheduleItem
import com.church.presenter.churchpresentermobile.model.AppTab
import com.church.presenter.churchpresentermobile.model.MoreDestination

/**
 * Where a tap on a schedule-drawer row takes the operator.
 *
 * Plain values rather than the assignments themselves: this is the one place
 * that decides, from a payload the desktop wrote, which tab opens and what it
 * opens *on*, and every field of it can be wrong in a way nobody sees until a
 * Sunday — a Bible row landing on the right chapter but the wrong verses, an
 * image row opening an empty folder, a row whose type nobody recognises quietly
 * doing nothing.
 *
 * A null [tab] means "stay where you are": the item did not carry enough to
 * navigate anywhere useful, and moving the operator off the screen they were on
 * would be worse than ignoring the tap.
 */
internal data class ScheduleDestination(
    val tab: AppTab? = null,
    val moreDestination: MoreDestination? = null,
    val songTitle: String? = null,
    val songBook: String? = null,
    val bibleBookName: String? = null,
    val bibleChapter: Int? = null,
    val bibleVerses: Set<Int> = emptySet(),
    val pictureFolderId: String? = null,
    val pictureImageIndex: Int? = null,
    val presentationId: String? = null,
    val mediaUrl: String? = null,
    val webUrl: String? = null,
    val announcement: ScheduleItem? = null,
    val dictionaryQuery: String? = null,
) {
    /** True when the tap has somewhere to go. */
    val navigates: Boolean get() = tab != null

    companion object {
        /** The tap that does nothing, for an item this app cannot open. */
        val NONE = ScheduleDestination()
    }
}

/**
 * Works out where [item] should open.
 *
 * The desktop writes these rows in several shapes — structured fields on newer
 * builds, a human-readable title on older ones — so a Bible row falls back to
 * parsing "1 Kings 17:3,4,7" out of its own title rather than refusing to open.
 */
internal fun destinationFor(item: ScheduleItem): ScheduleDestination =
    when (item.type?.lowercase()) {
        "song" -> item.title?.let {
            ScheduleDestination(tab = AppTab.SONGS, songTitle = it, songBook = item.bookName)
        } ?: ScheduleDestination.NONE

        "bible" -> bibleDestination(item)

        "image", "picture" -> ScheduleDestination(
            // Pictures live under the More tab.
            tab = AppTab.MORE,
            moreDestination = MoreDestination.PICTURES,
            // The server puts the folder UUID in the generic "id" field.
            pictureFolderId = item.id ?: item.folderId,
            pictureImageIndex = item.imageIndex,
        )

        "presentation" -> item.id?.takeIf { it.isNotBlank() }?.let {
            ScheduleDestination(tab = AppTab.PRESENTATION, presentationId = it)
        } ?: ScheduleDestination.NONE

        "media" -> ScheduleDestination(tab = AppTab.MEDIA, mediaUrl = item.mediaUrl)

        "announcement" -> ScheduleDestination(
            tab = AppTab.MORE,
            moreDestination = MoreDestination.ANNOUNCEMENTS,
            announcement = item,
        )

        "website", "web" -> ScheduleDestination(
            tab = AppTab.MORE,
            moreDestination = MoreDestination.WEB,
            webUrl = item.url ?: item.displayText,
        )

        "dictionary" -> ScheduleDestination(
            tab = AppTab.MORE,
            moreDestination = MoreDestination.DICTIONARY,
            // The schedule sends "word (translit): definition" — search by the word.
            dictionaryQuery = (item.text ?: item.displayText)?.substringBefore(" (")?.trim(),
        )

        else -> ScheduleDestination.NONE
    }

/**
 * A Bible row's destination, preferring structured fields over the title.
 *
 * Without a book *and* a chapter there is nothing to open, so the tap is
 * ignored rather than dropping the operator on an empty Bible tab.
 */
private fun bibleDestination(item: ScheduleItem): ScheduleDestination {
    var bookName = item.bookName
    var chapter = item.chapter

    // Raw verse string from the dedicated field, or a single verse number.
    val rawVerseStr: String? = item.verseRange?.takeIf { it.isNotBlank() }
        ?: item.verseNumber?.toString()

    // Title-parsing fallback for book, chapter and/or verses.
    val titleToParse = item.title?.trim()
    var titleVerseStr: String? = null
    if (titleToParse != null && titleToParse.contains(":")) {
        val colonIdx = titleToParse.lastIndexOf(':')
        titleVerseStr = titleToParse.substring(colonIdx + 1).trim()
        val beforeColon = titleToParse.substring(0, colonIdx).trim()
        val lastSpace = beforeColon.lastIndexOf(' ')
        if (lastSpace >= 0) {
            if (bookName == null) bookName = beforeColon.substring(0, lastSpace).trim().ifBlank { null }
            if (chapter == null) chapter = beforeColon.substring(lastSpace + 1).toIntOrNull()
        }
    }

    val verseStr = rawVerseStr ?: titleVerseStr

    return if (bookName != null && chapter != null) {
        ScheduleDestination(
            tab = AppTab.BIBLE,
            bibleBookName = bookName,
            bibleChapter = chapter,
            bibleVerses = parseVerseString(verseStr),
        )
    } else {
        ScheduleDestination.NONE
    }
}

/**
 * Parses a verse selection into the verse numbers it names.
 *
 * Accepts what the desktop actually sends:
 *
 *   "3"       → {3}
 *   "3-7"     → {3,4,5,6,7}
 *   "3,4,7"   → {3,4,7}
 *   "3-5,7"   → {3,4,5,7}
 *   null/""   → {}
 *
 * Anything unparseable is skipped rather than throwing: the string comes off a
 * schedule row that a person typed, and one stray character must not stop the
 * rest of the reference opening.
 */
internal fun parseVerseString(verseStr: String?): Set<Int> {
    if (verseStr.isNullOrBlank()) return emptySet()
    val result = mutableSetOf<Int>()
    for (token in verseStr.split(",")) {
        val part = token.trim()
        if (part.contains("-")) {
            val sides = part.split("-")
            val start = sides.firstOrNull()?.trim()?.toIntOrNull() ?: continue
            val end = sides.lastOrNull()?.trim()?.toIntOrNull() ?: start
            for (v in start..end) result += v
        } else {
            part.toIntOrNull()?.let { result += it }
        }
    }
    return result
}
