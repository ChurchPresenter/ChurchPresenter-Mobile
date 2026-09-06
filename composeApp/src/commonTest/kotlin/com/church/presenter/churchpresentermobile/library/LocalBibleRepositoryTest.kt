package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Translations kept on the device: what is written, what survives, and what is refused. */
class LocalBibleRepositoryTest {

    private fun module(title: String, book: String = "Genesis") = """
        ##Title: $title
        1 $book 50
        -----
        B001C001V001 1 1 1 In the beginning God created the heavens and the earth.
        B001C001V002 1 1 2 Now the earth was formless and void.
    """.trimIndent()

    private fun repo(storage: InMemoryFileStorage = InMemoryFileStorage()) =
        LocalBibleRepository(storage, now = { 1_700_000_000_000 })

    @Test
    fun installingATranslationWritesTheModuleAndIndexesIt() {
        val storage = InMemoryFileStorage()
        val installed = repo(storage).install("en_KJV.spb", module("King James Version"), sourceHost = "10.0.0.5")

        assertNotNull(installed)
        assertEquals("en_KJV", installed.id)
        assertEquals("King James Version", installed.title)
        assertEquals(2, installed.verseCount)
        assertEquals("10.0.0.5", installed.sourceHost)
        assertTrue(storage.contains("bible_en_KJV.spb"))
        assertTrue(storage.contains("bibles.json"))
    }

    @Test
    fun theIndexCarriesTheBookListSoTheTabOpensWithoutParsing() {
        // The whole point of keeping books in the index: drawing the book list must not cost a
        // 4.6 MB parse every time the Bible tab is opened.
        val installed = repo().install("en_KJV.spb", module("KJV"))

        assertNotNull(installed)
        assertEquals(1, installed.books.size)
        assertEquals("Genesis", installed.books.first().name)
        assertEquals(50, installed.books.first().chapterCount)
    }

    @Test
    fun aTranslationSurvivesAReload() {
        val storage = InMemoryFileStorage()
        repo(storage).install("en_KJV.spb", module("KJV"))

        val reloaded = repo(storage).load()

        assertEquals(1, reloaded.bibles.size)
        assertEquals("en_KJV", reloaded.activeId)
    }

    @Test
    fun installingTheSameTranslationAgainReplacesItRatherThanDuplicating() {
        val storage = InMemoryFileStorage()
        val repository = repo(storage)
        repository.install("en_KJV.spb", module("Old title"))
        repository.install("en_KJV.spb", module("New title"))

        assertEquals(1, repository.index.value.bibles.size)
        assertEquals("New title", repository.index.value.bibles.first().title)
    }

    @Test
    fun aSecondTranslationLeavesTheFirstAlone() {
        val repository = repo()
        repository.install("en_KJV.spb", module("KJV"))
        repository.install("ru_RST77.spb", module("Синодальный", book = "Бытие"))

        assertEquals(2, repository.index.value.bibles.size)
        // The first stays the one being read — a second download must not silently switch it.
        assertEquals("en_KJV", repository.index.value.activeId)
    }

    @Test
    fun removingOneLeavesTheOther() {
        val storage = InMemoryFileStorage()
        val repository = repo(storage)
        repository.install("en_KJV.spb", module("KJV"))
        repository.install("ru_RST77.spb", module("Синодальный"))

        repository.remove("en_KJV")

        assertEquals(listOf("ru_RST77"), repository.index.value.bibles.map { it.id })
        assertFalse(storage.contains("bible_en_KJV.spb"))
        assertTrue(storage.contains("bible_ru_RST77.spb"))
    }

    @Test
    fun clearingRemovesEveryTranslationAndItsFile() {
        val storage = InMemoryFileStorage()
        val repository = repo(storage)
        repository.install("en_KJV.spb", module("KJV"))
        repository.install("ru_RST77.spb", module("Синодальный"))
        repository.setActive("ru_RST77")

        repository.clearAll()

        assertTrue(repository.index.value.bibles.isEmpty())
        assertEquals("", repository.index.value.activeId)
        assertFalse(storage.contains("bible_en_KJV.spb"))
        assertFalse(storage.contains("bible_ru_RST77.spb"))
        assertTrue(repo(storage).load().bibles.isEmpty())
    }

    @Test
    fun clearingAlsoSweepsAModuleTheIndexLostTrackOf() {
        // The point of clearing is the space back. A module left behind by an index
        // write that failed after the download is megabytes nothing else deletes.
        val storage = InMemoryFileStorage(mapOf("bible_orphan.spb" to module("Orphan")))
        val repository = repo(storage)
        repository.install("en_KJV.spb", module("KJV"))

        repository.clearAll()

        assertFalse(storage.contains("bible_orphan.spb"))
        assertFalse(storage.contains("bible_en_KJV.spb"))
    }

    @Test
    fun removingTheActiveTranslationPromotesAnother() {
        // Otherwise the tab reads nothing while a perfectly good translation sits installed.
        val repository = repo()
        repository.install("en_KJV.spb", module("KJV"))
        repository.install("ru_RST77.spb", module("Синодальный"))
        repository.setActive("en_KJV")

        repository.remove("en_KJV")

        assertEquals("ru_RST77", repository.index.value.activeId)
    }

    @Test
    fun aDownloadThatIsNotABibleIsRefusedRatherThanStored() {
        // A captive portal or a 404 body arrives as text and would otherwise be indexed as a
        // translation with no verses in it.
        val storage = InMemoryFileStorage()

        val installed = repo(storage).install("en_KJV.spb", "<!doctype html><html>Sign in to Wi-Fi</html>")

        assertNull(installed)
        assertFalse(storage.contains("bible_en_KJV.spb"))
    }

    @Test
    fun aCorruptIndexFallsBackToAnEmptyLibraryRatherThanThrowing() {
        val storage = InMemoryFileStorage()
        repo(storage).install("en_KJV.spb", module("KJV"))
        storage.corrupt("bibles.json")

        assertTrue(repo(storage).load().isEmpty)
    }

    @Test
    fun openingATranslationReturnsItsVerses() {
        val storage = InMemoryFileStorage()
        repo(storage).install("en_KJV.spb", module("KJV"))

        // A fresh repository, so this reads and parses from storage rather than the install cache.
        val bible = repo(storage).also { it.load() }.open("en_KJV")

        assertNotNull(bible)
        assertEquals(2, bible.chapter(bookNumber = 1, chapter = 1).size)
    }

    @Test
    fun openingSomethingNotInstalledReturnsNothing() {
        assertNull(repo().open("not_here"))
    }

    @Test
    fun aMissingModuleFileDoesNotCrashTheTab() {
        // The index and the files can disagree — an OS clearing caches, a half-finished delete.
        val storage = InMemoryFileStorage()
        repo(storage).install("en_KJV.spb", module("KJV"))
        storage.delete("bible_en_KJV.spb")

        val repository = repo(storage).also { it.load() }

        assertNull(repository.open("en_KJV"))
    }

    @Test
    fun theSongLibraryIsLeftCompletelyAlone() {
        // Guards the decision not to fold Bibles into library.json, whose whole document is
        // rewritten on every song edit.
        val storage = InMemoryFileStorage(mapOf("library.json" to """{"songs":[]}"""))

        repo(storage).install("en_KJV.spb", module("KJV"))

        assertEquals("""{"songs":[]}""", storage.read("library.json"))
    }
}
