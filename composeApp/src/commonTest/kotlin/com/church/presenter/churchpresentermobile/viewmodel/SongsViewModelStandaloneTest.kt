package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.library.ServiceOrder
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.model.LocalSongSection
import com.church.presenter.churchpresentermobile.model.SectionType
import com.church.presenter.churchpresentermobile.model.Song
import com.church.presenter.churchpresentermobile.model.SongDetail
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.network.SongCatalog
import com.church.presenter.churchpresentermobile.network.SongReader
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The Songs tab in standalone: on-device songs, and no trace of the desktop.
 *
 * This is the regression the user reported — a fresh standalone install showed
 * "Failed to load songs" because the tab asked a computer that wasn't there.
 */
class SongsViewModelStandaloneTest {

    private class ForbiddenReader : SongReader {
        override suspend fun getSongs(): Result<List<Song>> =
            throw AssertionError("standalone must not contact the desktop")

        override suspend fun getSongDetail(
            number: String,
            bookName: String?,
            songId: Int,
            title: String?,
        ): Result<SongDetail> =
            throw AssertionError("standalone must not contact the desktop")
    }

    private fun library(vararg songs: LocalSong): LibraryRepository {
        val repository = LibraryRepository(InMemoryFileStorage()) { 0L }
        songs.forEach { repository.upsertSong(it) }
        return repository
    }

    private fun localSong(id: String, number: String, title: String) = LocalSong(
        id = id,
        number = number,
        title = title,
        sections = listOf(LocalSongSection(SectionType.VERSE, "words")),
    )

    private fun vm(
        repository: LibraryRepository,
        sender: FakeWsSender = FakeWsSender(),
        service: ServiceOrder? = ServiceOrder(repository),
    ): SongsViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        return SongsViewModel(
            appSettings = settings,
            eventService = ServerEventService(settings, MutableStateFlow(AppMode.STANDALONE)),
            isDemoMode = false,
            sender = sender,
            presenter = null,
            service = service,
            catalog = SongCatalog(MutableStateFlow(AppMode.STANDALONE), ForbiddenReader(), repository),
        )
    }

    @Test
    fun standaloneListsTheOnDeviceLibrary() = runVmTest {
        val vm = vm(library(localSong("a", "42", "Amazing Grace"), localSong("b", "7", "Be Thou My Vision")))
        advanceUntilIdle()

        assertEquals(setOf("Amazing Grace", "Be Thou My Vision"), vm.songs.value.map { it.title }.toSet())
        assertNull(vm.error.value)
        assertTrue(vm.showsLocalLibrary.value)
    }

    @Test
    fun anEmptyLibraryIsNotAnError() = runVmTest {
        val vm = vm(library())
        advanceUntilIdle()

        assertTrue(vm.songs.value.isEmpty())
        assertNull(vm.error.value, "the empty state must not be dressed up as a failure")
        assertFalse(vm.isLoading.value)
    }

    @Test
    fun addingToTheRunningOrderIsOfferedWhenThereIsOneToAddTo() = runVmTest {
        val vm = vm(library(localSong("a", "42", "Amazing Grace")))
        advanceUntilIdle()

        assertTrue(vm.canAddToSchedule.value)
    }

    @Test
    fun withNoRunningOrderTheActionStaysHidden() = runVmTest {
        // Without one the action would reach the router, which swallows it and
        // reports success — a cheerful "Added to schedule" for something that
        // never happened.
        val vm = vm(library(localSong("a", "42", "Amazing Grace")), service = null)
        advanceUntilIdle()

        assertFalse(vm.canAddToSchedule.value)
    }

    @Test
    fun addingASongWritesItToTheOnDeviceRunningOrder() = runVmTest {
        val repository = library(localSong("a", "42", "Amazing Grace"))
        val order = ServiceOrder(repository)
        val vm = vm(repository, service = order)
        advanceUntilIdle()

        vm.openSongDetail(vm.songs.value.single())
        advanceUntilIdle()
        vm.addSongToSchedule()
        advanceUntilIdle()

        assertEquals(listOf("Amazing Grace"), order.current.map { it.title })
        assertEquals("a", order.current.single().reference, "the library id, so an edit still resolves")
        assertTrue(vm.scheduleAdded.value)
    }

    @Test
    fun addingASongNeverReachesTheDesktop() = runVmTest {
        // ForbiddenReader covers reads; this covers the write path, which used to
        // go out over the router and be swallowed.
        val repository = library(localSong("a", "42", "Amazing Grace"))
        val sender = FakeWsSender()
        val vm = vm(repository, sender = sender, service = ServiceOrder(repository))
        advanceUntilIdle()

        vm.openSongDetail(vm.songs.value.single())
        advanceUntilIdle()
        val callsBefore = sender.calls.size
        vm.addSongToSchedule()
        advanceUntilIdle()

        assertEquals(callsBefore, sender.calls.size, "nothing may be sent for a local add")
    }

    @Test
    fun openingASongUsesTheLibraryLyrics() = runVmTest {
        val vm = vm(library(localSong("a", "42", "Amazing Grace")))
        advanceUntilIdle()

        vm.openSongDetail(vm.songs.value.single())
        advanceUntilIdle()

        assertEquals("Amazing Grace", vm.songDetail.value?.title)
        assertNull(vm.detailError.value)
    }

    // ── Staying level with the Library tab ───────────────────────────────

    @Test
    fun aSongAddedInTheLibraryAppearsWithoutARestart() = runVmTest {
        // Reported: a song saved in the Library tab stayed invisible on the Songs
        // tab through pull-to-refresh and tab switches, and showed up only after
        // a force-stop. The list was read once and never watched again.
        val repository = library()
        val vm = vm(repository)
        advanceUntilIdle()
        assertTrue(vm.songs.value.isEmpty())

        repository.upsertSong(localSong("a", "42", "Adb Standalone Test"))
        advanceUntilIdle()

        assertEquals(listOf("Adb Standalone Test"), vm.songs.value.map { it.title })
    }

    @Test
    fun anEditInTheLibraryReachesTheList() = runVmTest {
        val repository = library(localSong("a", "42", "Amazing Grace"))
        val vm = vm(repository)
        advanceUntilIdle()

        repository.upsertSong(localSong("a", "42", "Amazing Grace (revised)"))
        advanceUntilIdle()

        assertEquals(listOf("Amazing Grace (revised)"), vm.songs.value.map { it.title })
    }

    @Test
    fun aDeletionInTheLibraryLeavesTheList() = runVmTest {
        val repository = library(localSong("a", "42", "Amazing Grace"))
        val vm = vm(repository)
        advanceUntilIdle()

        repository.deleteSong("a")
        advanceUntilIdle()

        assertTrue(vm.songs.value.isEmpty())
    }

    @Test
    fun savingSettingsKeepsActionsOnTheInjectedSender() = runVmTest {
        // Regression: onSettingsSaved rebuilt the service with the raw WebSocket,
        // dropping the ProjectionRouter, so standalone actions went back to
        // dialling the desktop after any settings save.
        val sender = FakeWsSender()
        val vm = vm(library(localSong("a", "42", "Amazing Grace")), sender)
        advanceUntilIdle()

        vm.onSettingsSaved(settingsSaveToken = 1)
        advanceUntilIdle()

        vm.openSongDetail(vm.songs.value.single())
        advanceUntilIdle()

        assertTrue(sender.calls.isNotEmpty(), "actions must still reach the injected sender")
    }

    // ── Switching into standalone ────────────────────────────────────────

    /** A desktop that isn't there — what remote mode meets with no server. */
    private class TimingOutReader : SongReader {
        override suspend fun getSongs(): Result<List<Song>> =
            Result.failure(Exception("Connect timeout has expired [url=http://192.168.1.100:8765/api/songs]"))

        override suspend fun getSongDetail(
            number: String,
            bookName: String?,
            songId: Int,
            title: String?,
        ): Result<SongDetail> = Result.failure(Exception("Connect timeout has expired"))
    }

    @Test
    fun switchingIntoStandaloneClearsTheServerErrorAndShowsTheLibrary() = runVmTest {
        // Reported as a banner on a standalone startup. The tab is built before
        // the mode picker is answered, so on a first launch it has already timed
        // out against a desktop that isn't there by the time the operator picks
        // Standalone — and the error outlived the mode it belonged to.
        val mode = MutableStateFlow(AppMode.REMOTE)
        val settings = AppSettings(InMemorySettingsStorage())
        val vm = SongsViewModel(
            appSettings = settings,
            eventService = ServerEventService(settings, mode),
            isDemoMode = false,
            sender = FakeWsSender(),
            presenter = null,
            catalog = SongCatalog(mode, TimingOutReader(), library(localSong("a", "42", "Amazing Grace"))),
        )
        advanceUntilIdle()
        assertNotNull(vm.error.value, "remote with no desktop is expected to fail here")

        mode.value = AppMode.STANDALONE
        advanceUntilIdle()

        assertNull(vm.error.value, "a server error must not survive into a mode with no server")
        assertEquals(listOf("Amazing Grace"), vm.songs.value.map { it.title })
        assertTrue(vm.showsLocalLibrary.value)
    }
}
