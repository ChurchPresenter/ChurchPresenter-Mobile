package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.Serializable

/** Bumped when [BibleLibraryIndex] gains a field an older build would misread. */
const val BIBLE_LIBRARY_SCHEMA_VERSION: Int = 1

/**
 * One book of an installed translation, as the index remembers it.
 *
 * The book list lives in the index rather than being read back out of the module, so opening
 * the Bible tab costs a few kilobytes instead of parsing 4.6 MB to draw a list of names.
 */
@Serializable
data class InstalledBibleBook(
    val bookId: Int,
    val name: String,
    val chapterCount: Int,
)

/** A translation copied onto this device. */
@Serializable
data class InstalledBible(
    val id: String,
    /** The desktop's own file name, e.g. "en_KJV.spb" — what a re-sync matches on. */
    val fileName: String,
    val title: String,
    val verseCount: Int,
    val sizeBytes: Long,
    /** Which computer it came from, so "synced from" can name it later. */
    val sourceHost: String = "",
    val downloadedAtEpochMs: Long = 0L,
    val books: List<InstalledBibleBook> = emptyList(),
)

/** Every translation on this device, and which one the Bible tab is reading. */
@Serializable
data class BibleLibraryIndex(
    val schemaVersion: Int = BIBLE_LIBRARY_SCHEMA_VERSION,
    val bibles: List<InstalledBible> = emptyList(),
    val activeId: String = "",
) {
    val isEmpty: Boolean get() = bibles.isEmpty()

    /** The translation being read: the chosen one, else the first installed. */
    val active: InstalledBible?
        get() = bibles.firstOrNull { it.id == activeId } ?: bibles.firstOrNull()

    companion object {
        val EMPTY = BibleLibraryIndex()
    }
}
