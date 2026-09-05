package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.Serializable

/** Marker identifying a file as one of ours, so a stray `.json` is rejected clearly. */
const val CPSET_FORMAT: String = "churchpresenter.set"

/** Schema version of [CpsetDocument]. Bump on a breaking change. */
const val CPSET_VERSION: Int = 1

/** File extension for an exported library or service set. */
const val CPSET_EXTENSION: String = "cpset"

/**
 * A shareable slice of a library — a whole songbook, or one Sunday's set.
 *
 * A single UTF-8 JSON document rather than a zip: no portable zip writer exists
 * across JVM and Kotlin/Native, and hand-rolling deflate is not worth it for
 * text. Binary attachments have a reserved [assets] array, guarded by [version],
 * for when backdrop images need to travel.
 *
 * This is deliberately the same shape as the on-disk library, which is what
 * makes export nearly free.
 */
@Serializable
data class CpsetDocument(
    val format: String = CPSET_FORMAT,
    val version: Int = CPSET_VERSION,
    val exportedAt: Long = 0L,
    val appVersion: String = "",
    val name: String = "",
    val songs: List<LocalSong> = emptyList(),
    val announcements: List<LocalAnnouncement> = emptyList(),
    val setlists: List<LocalSetlist> = emptyList(),
    val assets: List<CpsetAsset> = emptyList(),
) {
    val itemCount: Int get() = songs.size + announcements.size + setlists.size

    val isEmpty: Boolean get() = itemCount == 0
}

/** A binary attachment carried inside a [CpsetDocument], base64-encoded. */
@Serializable
data class CpsetAsset(
    val id: String,
    val mime: String,
    val base64: String,
)

/** Why a document could not be read. */
enum class CpsetError {
    /** Not JSON, or not JSON we can parse at all. */
    UNREADABLE,

    /** Valid JSON, but not one of our files. */
    WRONG_FORMAT,

    /** Written by a newer app version than this one understands. */
    TOO_NEW,

    /** Parsed fine but contains nothing to import. */
    EMPTY,
}

/** The outcome of reading a `.cpset` file. */
sealed interface CpsetReadResult {
    data class Success(val document: CpsetDocument) : CpsetReadResult
    data class Failure(val error: CpsetError, val detail: String? = null) : CpsetReadResult
}

/**
 * What an import would do, shown before anything is written.
 *
 * @param newSongs Songs not currently in the library.
 * @param conflictingSongs Songs whose slot is already taken — the user chooses
 *   whether to keep theirs or take the file's.
 */
data class ImportPreview(
    val document: CpsetDocument,
    val newSongs: List<LocalSong> = emptyList(),
    val conflictingSongs: List<LocalSong> = emptyList(),
    val newAnnouncements: List<LocalAnnouncement> = emptyList(),
    val conflictingAnnouncements: List<LocalAnnouncement> = emptyList(),
    val newSetlists: List<LocalSetlist> = emptyList(),
) {
    val newCount: Int get() = newSongs.size + newAnnouncements.size + newSetlists.size
    val conflictCount: Int get() = conflictingSongs.size + conflictingAnnouncements.size
    val isEmpty: Boolean get() = newCount == 0 && conflictCount == 0
}

/** How an import should treat an item that is already present. */
enum class ConflictResolution {
    /** Leave the library's version alone. */
    KEEP_MINE,

    /** Take the file's version. */
    REPLACE,
}
