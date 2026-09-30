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
    /** The module's `##Abbreviation:` — "KJV" — shown beside the title in the translation list. */
    val abbreviation: String = "",
    /** "English", when known: a download names it; a module synced from a desktop does not. */
    val languageName: String = "",
    /** Where it was downloaded from — "eBible.org", "Zefania" — or blank for a desktop sync. */
    val origin: String = "",
    val license: String = "",
    /** The catalogue row it was installed from, which is how the catalogue marks it "Installed". */
    val catalogKey: String = "",
) {
    /** "OT", "NT" or "OT-NT", from the books it actually carries. */
    val coverage: String
        get() {
            val ids = books.map { it.bookId }
            val hasOld = ids.any { it in OLD_TESTAMENT }
            val hasNew = ids.any { it in NEW_TESTAMENT }
            return when {
                hasOld && hasNew -> "OT-NT"
                hasNew -> "NT"
                hasOld -> "OT"
                else -> ""
            }
        }

    private companion object {
        val OLD_TESTAMENT = 1..39
        val NEW_TESTAMENT = 40..66
    }
}

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
