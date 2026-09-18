package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.model.ReportFixtures
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The play log on disk: what is written, what survives, what is forgotten. */
class PlayLogRepositoryTest {

    private fun repo(storage: InMemoryFileStorage = InMemoryFileStorage(), clock: () -> Long = { 1_000L }) =
        PlayLogRepository(storage, clock)

    @Test
    fun aFreshDeviceHasAnEmptyLog() {
        assertTrue(repo().load().isEmpty)
    }

    @Test
    fun aPlayIsStampedWithTheClockAndWrittenThrough() {
        val storage = InMemoryFileStorage()
        var now = 5_000L
        val repo = repo(storage) { now }

        repo.recordSong(ReportFixtures.amazingGrace)
        now = 6_000L
        repo.recordVerse(ReportFixtures.john316)

        assertEquals(listOf(5_000L), repo.log.value.songs.map { it.at })
        assertEquals(listOf(6_000L), repo.log.value.verses.map { it.at })
        assertTrue(storage.contains(PLAY_LOG_FILE))
        assertEquals(2, storage.writeCount, "one write per play")
    }

    @Test
    fun whatWasWrittenIsReadBackByTheNextLaunch() {
        val storage = InMemoryFileStorage()
        repo(storage).apply {
            recordSong(ReportFixtures.amazingGrace)
            recordVerse(ReportFixtures.john316)
        }

        val reloaded = repo(storage).load()

        assertEquals("Amazing Grace", reloaded.songs.single().credit.title)
        assertEquals("John 3:16", reloaded.verses.single().credit.reference)
    }

    @Test
    fun aCorruptFileIsAnEmptyLogRatherThanACrash() {
        val storage = InMemoryFileStorage()
        storage.corrupt(PLAY_LOG_FILE)

        assertTrue(repo(storage).load().isEmpty)
    }

    @Test
    fun aFailedWriteStillCountsThePlayForThisSession() {
        // The disk being full mid-service must not lose the report on screen;
        // the next successful write carries everything.
        val storage = InMemoryFileStorage().apply { failWrites = true }
        val repo = repo(storage)

        repo.recordSong(ReportFixtures.amazingGrace)

        assertEquals(1, repo.log.value.songs.size)
    }

    @Test
    fun clearingForgetsEverythingOnDiskToo() {
        val storage = InMemoryFileStorage()
        val repo = repo(storage).apply {
            recordSong(ReportFixtures.amazingGrace)
            recordVerse(ReportFixtures.john316)
        }

        repo.clear()

        assertTrue(repo.log.value.isEmpty)
        assertTrue(repo(storage).load().isEmpty, "a relaunch must not bring the plays back")
    }

    @Test
    fun loadingTwiceIsHarmless() {
        val storage = InMemoryFileStorage()
        val repo = repo(storage).apply { recordSong(ReportFixtures.amazingGrace) }

        repo.load()
        repo.load()

        assertEquals(1, repo.log.value.songs.size)
    }
}
