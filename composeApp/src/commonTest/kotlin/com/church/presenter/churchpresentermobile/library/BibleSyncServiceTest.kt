package com.church.presenter.churchpresentermobile.library

import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Copying translations across: what lands, what is refused, and what a stop leaves behind. */
class BibleSyncServiceTest {

    private fun module(title: String) = """
        ##Title: $title
        1 Genesis 50
        -----
        B001C001V001 1 1 1 In the beginning.
    """.trimIndent()

    private val manifest = listOf("en_KJV.spb", "ru_RST77.spb", "de_LUT.spb")

    private fun service(
        storage: InMemoryFileStorage = InMemoryFileStorage(),
        catalogue: suspend () -> Result<List<String>> = { Result.success(manifest) },
        download: suspend (Int) -> Result<String> = { Result.success(module("Module $it")) },
    ): Pair<BibleSyncService, LocalBibleRepository> {
        val repository = LocalBibleRepository(storage, now = { 1_700_000_000_000 })
        return BibleSyncService(repository, catalogue, download, sourceHost = { "10.0.0.5" }) to repository
    }

    @Test
    fun onlyTheChosenTranslationsAreCopied() = runTest {
        val asked = mutableListOf<Int>()
        val (sync, repository) = service(download = { index ->
            asked += index
            Result.success(module("Module $index"))
        })

        val outcome = sync.sync(listOf("ru_RST77.spb"))

        assertIs<BibleSyncOutcome.Success>(outcome)
        assertEquals(listOf(1), asked)
        assertEquals(1, repository.index.value.bibles.size)
    }

    @Test
    fun aTranslationIsResolvedByNameNotByAStaleIndex() = runTest {
        // The desktop rebuilds its list from its own settings, so a position captured when the
        // picker was drawn can point at a different module by the time Download is tapped.
        val asked = mutableListOf<Int>()
        val (sync, _) = service(
            catalogue = { Result.success(listOf("de_LUT.spb", "en_KJV.spb")) },
            download = { index -> asked += index; Result.success(module("Module $index")) },
        )

        sync.sync(listOf("en_KJV.spb"))

        // Position 1 in the refreshed manifest, not the 0 it held in the original one.
        assertEquals(listOf(1), asked)
    }

    @Test
    fun aTranslationTheDesktopNoLongerOffersIsReportedRatherThanGuessedAt() = runTest {
        val asked = mutableListOf<Int>()
        val (sync, repository) = service(
            catalogue = { Result.success(listOf("de_LUT.spb")) },
            download = { index -> asked += index; Result.success(module("Module $index")) },
        )

        val outcome = assertIs<BibleSyncOutcome.Success>(sync.sync(listOf("en_KJV.spb")))

        assertTrue(asked.isEmpty())
        assertEquals(listOf("en KJV"), outcome.failed)
        assertTrue(repository.index.value.isEmpty)
    }

    @Test
    fun anUnreachableDesktopChangesNothing() = runTest {
        val (sync, repository) = service(catalogue = { Result.failure(Exception("Connect timeout")) })

        val outcome = sync.sync(listOf("en_KJV.spb"))

        assertEquals("Connect timeout", assertIs<BibleSyncOutcome.Failed>(outcome).message)
        assertTrue(repository.index.value.isEmpty)
    }

    @Test
    fun oneUnreadableModuleIsSkippedRatherThanFatal() = runTest {
        val (sync, repository) = service(download = { index ->
            if (index == 0) Result.success("<html>404</html>") else Result.success(module("Good"))
        })

        val outcome = assertIs<BibleSyncOutcome.Success>(sync.sync(listOf("en_KJV.spb", "ru_RST77.spb")))

        assertEquals(listOf("Good"), outcome.installed)
        assertEquals(listOf("en KJV"), outcome.failed)
        assertEquals(1, repository.index.value.bibles.size)
    }

    @Test
    fun oneFailedDownloadDoesNotStopTheRest() = runTest {
        val (sync, repository) = service(download = { index ->
            if (index == 0) Result.failure(Exception("boom")) else Result.success(module("Good"))
        })

        val outcome = assertIs<BibleSyncOutcome.Success>(sync.sync(listOf("en_KJV.spb", "ru_RST77.spb")))

        assertEquals(listOf("Good"), outcome.installed)
        assertEquals(1, repository.index.value.bibles.size)
    }

    @Test
    fun aStopKeepsWhateverAlreadyFinished() = runTest {
        lateinit var sync: BibleSyncService
        val (service, repository) = service(download = { index ->
            // Ask to stop once the first module is in — the check happens between modules.
            sync.requestCancel()
            Result.success(module("Module $index"))
        })
        sync = service

        val outcome = sync.sync(listOf("en_KJV.spb", "ru_RST77.spb"))

        assertEquals(1, assertIs<BibleSyncOutcome.Cancelled>(outcome).installed.size)
        assertEquals(1, repository.index.value.bibles.size)
    }

    @Test
    fun eachTranslationIsWrittenAsItArrivesNotAllAtTheEnd() = runTest {
        // So an interrupted copy leaves a smaller but valid library rather than nothing.
        val storage = InMemoryFileStorage()
        val (sync, _) = service(storage = storage, download = { index ->
            if (index == 1) Result.failure(Exception("dropped")) else Result.success(module("First"))
        })

        sync.sync(listOf("en_KJV.spb", "ru_RST77.spb"))

        assertTrue(storage.contains("bible_en_KJV.spb"))
    }

    @Test
    fun progressCountsTranslationsAndEndsIdle() = runTest {
        val (sync, _) = service()
        assertFalse(sync.progress.value.isRunning)

        val outcome = sync.sync(listOf("en_KJV.spb", "ru_RST77.spb"))

        assertIs<BibleSyncOutcome.Success>(outcome)
        assertFalse(sync.progress.value.isRunning)
        assertEquals(0, sync.progress.value.total)
    }

    @Test
    fun reSyncingAnInstalledTranslationReplacesItInPlace() = runTest {
        val storage = InMemoryFileStorage()
        val (sync, repository) = service(storage = storage)

        sync.sync(listOf("en_KJV.spb"))
        sync.sync(listOf("en_KJV.spb"))

        assertEquals(1, repository.index.value.bibles.size)
    }
}
