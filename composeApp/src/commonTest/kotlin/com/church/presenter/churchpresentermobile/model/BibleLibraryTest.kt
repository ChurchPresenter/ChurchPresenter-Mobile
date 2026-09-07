package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests the on-device Bible index — which translation the Bible tab reads, and
 * whether an index written by an older build still loads.
 */
class BibleLibraryTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun bible(id: String, fileName: String = "$id.spb") = InstalledBible(
        id = id,
        fileName = fileName,
        title = id.uppercase(),
        verseCount = 31_102,
        sizeBytes = 4_600_000L,
    )

    @Test
    fun `an empty index reads as empty and has no active translation`() {
        assertTrue(BibleLibraryIndex.EMPTY.isEmpty)
        assertNull(BibleLibraryIndex.EMPTY.active)
        assertEquals(BIBLE_LIBRARY_SCHEMA_VERSION, BibleLibraryIndex.EMPTY.schemaVersion)
    }

    @Test
    fun `active is the chosen translation`() {
        val index = BibleLibraryIndex(bibles = listOf(bible("kjv"), bible("niv")), activeId = "niv")

        assertFalse(index.isEmpty)
        assertEquals("niv", index.active?.id)
    }

    @Test
    fun `active falls back to the first installed when nothing is chosen`() {
        val index = BibleLibraryIndex(bibles = listOf(bible("kjv"), bible("niv")))

        assertEquals("kjv", index.active?.id)
    }

    @Test
    fun `active falls back when the chosen translation is no longer installed`() {
        // The operator deleted the active translation; the tab must still read something.
        val index = BibleLibraryIndex(bibles = listOf(bible("kjv")), activeId = "deleted")

        assertEquals("kjv", index.active?.id)
    }

    @Test
    fun `an index round-trips through JSON`() {
        val index = BibleLibraryIndex(
            bibles = listOf(
                bible("kjv").copy(
                    sourceHost = "office-mac",
                    downloadedAtEpochMs = 1_700_000_000_000L,
                    books = listOf(InstalledBibleBook(bookId = 1, name = "Genesis", chapterCount = 50)),
                ),
            ),
            activeId = "kjv",
        )

        assertEquals(index, json.decodeFromString<BibleLibraryIndex>(json.encodeToString(index)))
    }

    @Test
    fun `an index written before the optional fields existed still loads`() {
        // Only the fields the first release wrote — the rest must default.
        val stored = """
            {"schemaVersion":1,"bibles":[{"id":"kjv","fileName":"en_KJV.spb","title":"KJV",
            "verseCount":31102,"sizeBytes":4600000}],"activeId":"kjv"}
        """.trimIndent()

        val index = json.decodeFromString<BibleLibraryIndex>(stored)

        val kjv = index.active
        assertEquals("kjv", kjv?.id)
        assertEquals("", kjv?.sourceHost)
        assertEquals(0L, kjv?.downloadedAtEpochMs)
        assertEquals(emptyList(), kjv?.books)
    }

    @Test
    fun `an index with no stored version reads as the current schema`() {
        val index = json.decodeFromString<BibleLibraryIndex>("""{"bibles":[],"activeId":""}""")

        assertEquals(BIBLE_LIBRARY_SCHEMA_VERSION, index.schemaVersion)
        assertTrue(index.isEmpty)
    }

    @Test
    fun `the books a translation lists are kept in order`() {
        val books = listOf(
            InstalledBibleBook(1, "Genesis", 50),
            InstalledBibleBook(2, "Exodus", 40),
        )

        assertEquals(books, bible("kjv").copy(books = books).books)
    }
}
