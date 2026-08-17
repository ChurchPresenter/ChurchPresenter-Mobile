package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.library.LibraryRepository
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

    private fun vm(repository: LibraryRepository, sender: FakeWsSender = FakeWsSender()): SongsViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        return SongsViewModel(
            appSettings = settings,
            eventService = ServerEventService(settings, MutableStateFlow(AppMode.STANDALONE)),
            isDemoMode = false,
            sender = sender,
            presenter = null,
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
    fun addingToAScheduleIsOfferedOnlyWhenThereIsADesktop() = runVmTest {
        val vm = vm(library(localSong("a", "42", "Amazing Grace")))
        advanceUntilIdle()

        // The router swallows the action and reports success, so an offered
        // button would tell the operator their song was scheduled when it wasn't.
        assertFalse(vm.canAddToSchedule.value)
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
}
