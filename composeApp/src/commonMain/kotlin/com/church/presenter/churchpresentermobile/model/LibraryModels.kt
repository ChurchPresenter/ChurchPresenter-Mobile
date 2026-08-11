package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.Serializable

/** Current on-disk schema version of [LibraryData]. Bump on a breaking change. */
const val LIBRARY_SCHEMA_VERSION: Int = 1

/**
 * Where an item came from, which decides what a desktop sync is allowed to do
 * to it.
 *
 * The distinction exists because a re-sync must never silently discard the
 * operator's own work. [DESKTOP] rows are replaceable; [LOCAL] rows are the
 * user's and are never touched; [LOCAL_OVERRIDE] marks a desktop row the user
 * has since edited, which sync skips and reports rather than overwriting.
 */
@Serializable
enum class ContentOrigin {
    LOCAL,
    DESKTOP,
    LOCAL_OVERRIDE,
}

/** The kind of a song section, used for labelling and stage colour. */
@Serializable
enum class SectionType {
    VERSE,
    CHORUS,
    BRIDGE,
    TAG,
    ENDING,
}

/**
 * One projected block of a [LocalSong] — a stanza, a chorus, a bridge.
 *
 * @param label Optional override for the auto-generated label, e.g. "Verse 1a".
 */
@Serializable
data class LocalSongSection(
    val type: SectionType = SectionType.VERSE,
    val text: String = "",
    val label: String? = null,
)

/**
 * A song the user owns on this device — either authored here or synced down
 * from a desktop and cached.
 *
 * @param number The songbook number. A string because hymnals use "42a" and "10b".
 * @param updatedAt Epoch millis of the last edit. Supplied by the caller rather
 *   than read from a clock so the model stays pure and testable.
 */
@Serializable
data class LocalSong(
    val id: String,
    val number: String = "",
    val title: String = "",
    val author: String? = null,
    val bookName: String? = null,
    val copyright: String? = null,
    val sections: List<LocalSongSection> = emptyList(),
    val origin: ContentOrigin = ContentOrigin.LOCAL,
    val updatedAt: Long = 0L,
) {
    /** Title with its number, as shown in lists: "42 Amazing Grace". */
    val displayTitle: String
        get() = listOf(number, title).filter { it.isNotBlank() }.joinToString(" ")
}

/** A notice the user can project — announcements, welcomes, giving details. */
@Serializable
data class LocalAnnouncement(
    val id: String,
    val title: String = "",
    val body: String = "",
    val origin: ContentOrigin = ContentOrigin.LOCAL,
    val updatedAt: Long = 0L,
)

/** What a [LocalSetlistEntry] points at. */
@Serializable
enum class SetlistEntryType {
    SONG,
    BIBLE,
    ANNOUNCEMENT,
}

/**
 * One item in a service running order.
 *
 * @param reference For [SetlistEntryType.BIBLE], a passage such as `John:3:16-18`.
 *   For the others, the id of the owning [LocalSong] / [LocalAnnouncement].
 */
@Serializable
data class LocalSetlistEntry(
    val type: SetlistEntryType,
    val reference: String,
    val title: String = "",
)

/** An ordered service running order. */
@Serializable
data class LocalSetlist(
    val id: String,
    val name: String = "",
    val entries: List<LocalSetlistEntry> = emptyList(),
    val updatedAt: Long = 0L,
)

/**
 * The whole on-device library — the single document persisted to disk and,
 * later, the basis of the export format.
 */
@Serializable
data class LibraryData(
    val version: Int = LIBRARY_SCHEMA_VERSION,
    val songs: List<LocalSong> = emptyList(),
    val announcements: List<LocalAnnouncement> = emptyList(),
    val setlists: List<LocalSetlist> = emptyList(),
) {
    val isEmpty: Boolean
        get() = songs.isEmpty() && announcements.isEmpty() && setlists.isEmpty()

    val itemCount: Int get() = songs.size + announcements.size + setlists.size

    companion object {
        val EMPTY: LibraryData = LibraryData()
    }
}
