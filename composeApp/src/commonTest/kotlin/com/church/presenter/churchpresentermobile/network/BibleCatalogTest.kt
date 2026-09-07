package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.BibleBook
import com.church.presenter.churchpresentermobile.model.BibleVerse
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Which source the Bible tab reads, and what happens when the desktop goes quiet. */
class BibleCatalogTest {

    private val module = """
        ##Title: King James Version
        1 Genesis 50
        -----
        B001C001V001 1 1 1 In the beginning God created the heavens and the earth.
        B001C001V002 1 1 2 Now the earth was formless and void.
    """.trimIndent()

    private class FakeReader(
        var books: Result<List<BibleBook>> = Result.success(listOf(BibleBook(name = "Desktop Genesis", bookId = 1))),
        var verses: Result<List<BibleVerse>> = Result.success(listOf(BibleVerse(verse = 1, text = "from the desktop"))),
    ) : BibleReader {
        var bookCalls = 0
        override suspend fun getBooks(): Result<List<BibleBook>> { bookCalls++; return books }
        override suspend fun getChapter(bookNumber: Int, chapter: Int) = verses
    }

    private fun installed(): LocalBibleRepository =
        LocalBibleRepository(InMemoryFileStorage(), now = { 0L })
            .also { it.install("en_KJV.spb", module) }

    private fun empty(): LocalBibleRepository = LocalBibleRepository(InMemoryFileStorage())

    @Test
    fun remoteModeAsksTheDesktopEvenWithATranslationInstalled() = runTest {
        // The desktop is authoritative whenever it answers — a downloaded copy must not quietly
        // replace the translation the operator is presenting from.
        val reader = FakeReader()
        val catalog = BibleCatalog(MutableStateFlow(AppMode.REMOTE), reader, installed())

        assertEquals("Desktop Genesis", catalog.books().getOrThrow().first().displayName)
        assertFalse(catalog.isLocal)
    }

    @Test
    fun standaloneReadsTheDeviceAndNeverAsks() = runTest {
        val reader = FakeReader()
        val catalog = BibleCatalog(MutableStateFlow(AppMode.STANDALONE), reader, installed())

        assertEquals("Genesis", catalog.books().getOrThrow().first().displayName)
        assertEquals(0, reader.bookCalls)
        assertTrue(catalog.isLocal)
    }

    @Test
    fun standaloneServesAChapterWithTheModulesOwnNumbering() = runTest {
        val catalog = BibleCatalog(MutableStateFlow(AppMode.STANDALONE), FakeReader(), installed())

        val verses = catalog.chapter(bookNumber = 1, chapter = 1).getOrThrow()

        assertEquals(2, verses.size)
        assertEquals("In the beginning God created the heavens and the earth.", verses.first().displayText)
    }

    @Test
    fun anUnreachableDesktopFallsBackToTheDownloadedTranslation() = runTest {
        // The reason a downloaded Bible is read in remote at all: a dropped connection mid-service
        // must not blank the tab when the same text is sitting on the phone.
        val reader = FakeReader(books = Result.failure(Exception("Connect timeout")))
        val catalog = BibleCatalog(MutableStateFlow(AppMode.REMOTE), reader, installed())

        assertEquals("Genesis", catalog.books().getOrThrow().first().displayName)
    }

    @Test
    fun anUnreachableDesktopWithNothingDownloadedStillReportsTheFailure() = runTest {
        // Inventing a success out of an empty library would hide a broken address behind an
        // empty book list.
        val reader = FakeReader(books = Result.failure(Exception("Connect timeout")))
        val catalog = BibleCatalog(MutableStateFlow(AppMode.REMOTE), reader, empty())

        assertTrue(catalog.books().isFailure)
    }

    @Test
    fun standaloneWithNothingDownloadedIsAnEmptyLibraryNotAFailure() = runTest {
        val catalog = BibleCatalog(MutableStateFlow(AppMode.STANDALONE), FakeReader(), empty())

        assertEquals(emptyList(), catalog.books().getOrThrow())
        assertTrue(catalog.hasNoBible.first())
    }

    @Test
    fun downloadingATranslationOpensTheTabWithoutARestart() = runTest {
        // hasNoBible is a stream for exactly this: the operator syncs and the empty state goes.
        val bibles = empty()
        val catalog = BibleCatalog(MutableStateFlow(AppMode.STANDALONE), FakeReader(), bibles)
        assertTrue(catalog.hasNoBible.first())

        bibles.install("en_KJV.spb", module)

        assertFalse(catalog.hasNoBible.first())
    }

    @Test
    fun aModeSwitchIsFollowedWithoutRebuildingTheCatalog() = runTest {
        val mode = MutableStateFlow(AppMode.REMOTE)
        val catalog = BibleCatalog(mode, FakeReader(), empty())
        assertFalse(catalog.hasNoBible.first())

        mode.value = AppMode.STANDALONE

        assertTrue(catalog.hasNoBible.first())
    }

    // ── Falling back to a downloaded translation ─────────────────────────

    @Test
    fun aQuietDesktopIsAnsweredFromTheDownloadedTranslation() = runTest {
        // The operator has a copy on the phone; a dropped Wi-Fi mid-service should
        // read from it rather than showing an error over a chapter they can see.
        val bibles = LocalBibleRepository(InMemoryFileStorage()) { 1L }
        bibles.install("en_KJV.spb", module)
        val reader = FakeReader(verses = Result.failure(Exception("Connect timeout has expired")))
        val catalog = BibleCatalog(MutableStateFlow(AppMode.REMOTE), reader, bibles)

        val verses = catalog.chapter(1, 1).getOrThrow()

        assertTrue(verses.isNotEmpty())
        assertTrue(verses.first().text!!.contains("In the beginning"), "${verses.first().text}")
    }

    @Test
    fun withNothingDownloadedTheDesktopsFailureIsReported() = runTest {
        // No local copy to fall back on, so the timeout is the honest answer.
        val bibles = LocalBibleRepository(InMemoryFileStorage()) { 1L }
        val reader = FakeReader(verses = Result.failure(Exception("Connect timeout has expired")))
        val catalog = BibleCatalog(MutableStateFlow(AppMode.REMOTE), reader, bibles)

        assertTrue(catalog.chapter(1, 1).isFailure)
    }

    @Test
    fun aReachableDesktopIsPreferredOverTheLocalCopy() = runTest {
        // The desktop's translation is the one the congregation is seeing.
        val bibles = LocalBibleRepository(InMemoryFileStorage()) { 1L }
        bibles.install("en_KJV.spb", module)
        val catalog = BibleCatalog(MutableStateFlow(AppMode.REMOTE), FakeReader(), bibles)

        val verses = catalog.chapter(1, 1).getOrThrow()

        assertEquals("from the desktop", verses.first().text)
    }
}
