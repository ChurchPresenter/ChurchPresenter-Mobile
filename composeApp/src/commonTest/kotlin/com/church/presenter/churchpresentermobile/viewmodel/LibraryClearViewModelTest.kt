package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.library.CURRENT_SERVICE_SETLIST_ID
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.model.LocalSetlist
import com.church.presenter.churchpresentermobile.model.LocalSetlistEntry
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.model.LocalSongSection
import com.church.presenter.churchpresentermobile.model.SectionType
import com.church.presenter.churchpresentermobile.model.SetlistEntryType
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTestUnconfined
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Emptying the device of content without emptying it of settings. */
class LibraryClearViewModelTest {

    private val module = """
        ##Title: King James Version
        1 Genesis 50
        -----
        B001C001V001 1 1 1 In the beginning.
    """.trimIndent()

    private class Harness {
        val storage = InMemoryFileStorage()
        val library = LibraryRepository(storage) { 1_000L }
        val bibles = LocalBibleRepository(storage) { 1_000L }
        val settings = AppSettings(InMemorySettingsStorage())
        val viewModel = LibraryClearViewModel(library, bibles, settings)
    }

    private fun song(id: String) = LocalSong(
        id = id,
        number = "42",
        title = "Amazing Grace",
        sections = listOf(LocalSongSection(SectionType.VERSE, "Amazing grace")),
    )

    private fun Harness.fill() {
        library.upsertSong(song("s1"))
        library.upsertAnnouncement(LocalAnnouncement(id = "a1", title = "Welcome"))
        library.upsertSetlist(
            LocalSetlist(
                id = CURRENT_SERVICE_SETLIST_ID,
                entries = listOf(LocalSetlistEntry(SetlistEntryType.ANNOUNCEMENT, "a1", "Welcome")),
            )
        )
        bibles.install("en_KJV.spb", module)
        settings.host = "192.168.1.50"
        settings.apiKey = "secret"
        settings.librarySyncStateJson = """{"lastSyncEpochMs":1700000000000,"songCount":240}"""
    }

    @Test
    fun clearingSongsLeavesNoticesAndBiblesAlone() = runVmTestUnconfined {
        val h = Harness()
        h.fill()

        h.viewModel.clearSongs()
        h.viewModel.outcome.first { it != null }

        assertTrue(h.library.songs.isEmpty())
        assertEquals(1, h.library.announcements.size)
        assertEquals(1, h.bibles.index.value.bibles.size)
    }

    @Test
    fun clearingSongsForgetsTheLastSyncRecord() = runVmTestUnconfined {
        // The Library tab's chip reads this: left alone it goes on reporting a
        // successful copy of songs that are gone, which reads as a failed wipe.
        val h = Harness()
        h.fill()

        h.viewModel.clearSongs()
        h.viewModel.outcome.first { it != null }

        assertEquals("{}", h.settings.librarySyncStateJson)
    }

    @Test
    fun clearingBiblesLeavesTheSongsAlone() = runVmTestUnconfined {
        val h = Harness()
        h.fill()

        h.viewModel.clearBibles()
        h.viewModel.outcome.first { it != null }

        assertTrue(h.bibles.index.value.bibles.isEmpty())
        assertEquals(1, h.library.songs.size)
    }

    @Test
    fun clearingEverythingEmptiesBothLibraries() = runVmTestUnconfined {
        val h = Harness()
        h.fill()

        h.viewModel.clearEverything()
        h.viewModel.outcome.first { it != null }

        assertTrue(h.library.library.value.isEmpty)
        assertTrue(h.bibles.index.value.bibles.isEmpty())
    }

    @Test
    fun everyWipeKeepsTheServerSettings() = runVmTestUnconfined {
        // The whole reason this exists rather than "reinstall the app": the
        // address and key are the tedious part to type back in.
        val h = Harness()
        h.fill()

        h.viewModel.clearEverything()
        h.viewModel.outcome.first { it != null }

        assertEquals("192.168.1.50", h.settings.host)
        assertEquals("secret", h.settings.apiKey)
    }

    @Test
    fun theCountsDescribeWhatIsAboutToGo() = runVmTestUnconfined {
        val h = Harness()
        h.fill()

        val content = h.viewModel.content.first { it.songCount > 0 }

        assertEquals(1, content.songCount)
        assertEquals(1, content.noticeCount)
        assertEquals(1, content.serviceEntryCount)
        assertEquals(1, content.bibleCount)
    }

    @Test
    fun aFreshInstallHasNothingToClear() = runVmTestUnconfined {
        val h = Harness()

        assertTrue(h.viewModel.content.value.isEmpty)
    }

    @Test
    fun sizesAreStatedInUnitsAnOperatorReads() {
        assertEquals("9 MB", formatBytes(9_400_000L))
        assertEquals("4 KB", formatBytes(4_100L))
        assertEquals("512 B", formatBytes(512L))
    }
}
