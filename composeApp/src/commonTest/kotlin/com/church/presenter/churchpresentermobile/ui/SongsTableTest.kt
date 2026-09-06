package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.viewmodel.SongsViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The Songs tab as a whole — the list and the detail sheet behind one switch.
 *
 * The two halves are covered on their own; what is only reachable here is the
 * wiring between them. The failures that matter are the ones an operator meets
 * mid-service: a song that opens the wrong sheet, a back gesture registered
 * when there is nothing to go back from, and a toolbar title left naming a song
 * that is no longer open.
 *
 * Demo mode serves the canned catalogue, so nothing here touches a network.
 */
@OptIn(ExperimentalTestApi::class)
class SongsTableTest {

    private fun demoVm(): SongsViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        return SongsViewModel(settings, ServerEventService(settings), isDemoMode = true)
    }

    private fun ComposeUiTest.showSongsTab(
        vm: SongsViewModel,
        onDetailChanged: (String?, String?) -> Unit = { _, _ -> },
        onRegisterBackAction: ((() -> Unit)?) -> Unit = {},
        onScheduleRefresh: () -> Unit = {},
        pendingNavSongTitle: String? = null,
        pendingNavSongBook: String? = null,
        onPendingNavHandled: () -> Unit = {},
    ) = showScreen {
        SongsTable(
            appSettings = AppSettings(InMemorySettingsStorage()),
            isDemoMode = true,
            settingsSaveToken = 0,
            onDetailChanged = onDetailChanged,
            onRegisterBackAction = onRegisterBackAction,
            onScheduleRefresh = onScheduleRefresh,
            pendingNavSongTitle = pendingNavSongTitle,
            pendingNavSongBook = pendingNavSongBook,
            onPendingNavHandled = onPendingNavHandled,
            providedViewModel = vm,
        )
    }

    private fun hymn(number: String) = UiTags.songCard(number, "Hymnal")

    // ── The list is what shows first ─────────────────────────────────────

    @Test
    fun theCatalogueIsListedFirst() = runComposeUiTest {
        val vm = demoVm()
        showSongsTab(vm)

        awaitThat { vm.songs.value.isNotEmpty() }
        assertTrue(isShowing("Amazing Grace"))
    }

    @Test
    fun noDetailSheetIsShownBeforeASongIsOpened() = runComposeUiTest {
        val vm = demoVm()
        showSongsTab(vm)

        awaitThat { vm.songs.value.isNotEmpty() }
        assertFalse(exists(UiTags.verseCard(0)))
    }

    @Test
    fun theSearchFieldIsOfferedOnTheList() = runComposeUiTest {
        val vm = demoVm()
        showSongsTab(vm)

        awaitThat { vm.songs.value.isNotEmpty() }
        assertTrue(exists(UiTags.SONGS_SEARCH))
    }

    @Test
    fun everySongInTheCatalogueGetsACard() = runComposeUiTest {
        val vm = demoVm()
        showSongsTab(vm)

        awaitThat { exists(hymn("1")) }
        assertTrue(exists(hymn("1")))
        assertTrue(exists(hymn("2")))
    }

    // ── Opening a song ───────────────────────────────────────────────────

    @Test
    fun tappingASongOpensItsSheet() = runComposeUiTest {
        val vm = demoVm()
        showSongsTab(vm)

        awaitThat { exists(hymn("1")) }
        click(hymn("1"))

        awaitThat { vm.songDetail.value != null }
        assertEquals("Amazing Grace", vm.selectedSong.value?.title)
    }

    @Test
    fun openingASongReplacesTheListWithTheSheet() = runComposeUiTest {
        // One at a time — a sheet drawn over a live list is how the wrong verse
        // gets tapped.
        val vm = demoVm()
        showSongsTab(vm)

        awaitThat { exists(hymn("1")) }
        click(hymn("1"))

        awaitThat { vm.songDetail.value != null }
        assertFalse(exists(UiTags.SONGS_SEARCH))
    }

    @Test
    fun openingReportsTheSongThatWasTapped() = runComposeUiTest {
        val vm = demoVm()
        showSongsTab(vm)

        awaitThat { exists(hymn("2")) }
        click(hymn("2"))

        awaitThat { vm.songDetail.value != null }
        assertEquals("How Great Thou Art", vm.selectedSong.value?.title)
    }

    @Test
    fun theOpenSongsTitleIsReportedToTheToolbar() = runComposeUiTest {
        // The toolbar is outside this composable, so it is told rather than
        // reading the state itself.
        var title: String? = null
        val vm = demoVm()
        showSongsTab(vm, onDetailChanged = { t, _ -> title = t })

        awaitThat { exists(hymn("1")) }
        click(hymn("1"))

        awaitThat { title != null }
        assertTrue(title!!.contains("Amazing Grace"), title!!)
    }

    @Test
    fun theOpenSongsBookIsReportedToTheToolbar() = runComposeUiTest {
        var book: String? = null
        val vm = demoVm()
        showSongsTab(vm, onDetailChanged = { _, b -> book = b })

        awaitThat { exists(hymn("1")) }
        click(hymn("1"))

        awaitThat { book != null }
        assertEquals("Hymnal", book)
    }

    @Test
    fun theToolbarIsToldTheNumberAlongsideTheTitle() = runComposeUiTest {
        // Two books can share a title; the number is what tells them apart.
        var title: String? = null
        val vm = demoVm()
        showSongsTab(vm, onDetailChanged = { t, _ -> title = t })

        awaitThat { exists(hymn("2")) }
        click(hymn("2"))

        awaitThat { title != null }
        assertTrue(title!!.contains("2"), title!!)
    }

    @Test
    fun noTitleIsReportedWhileTheListIsShowing() = runComposeUiTest {
        var reported: String? = "stale"
        val vm = demoVm()
        showSongsTab(vm, onDetailChanged = { t, _ -> reported = t })

        awaitThat { vm.songs.value.isNotEmpty() }

        assertNull(reported)
    }

    // ── The back gesture ─────────────────────────────────────────────────

    @Test
    fun noBackActionIsRegisteredWhileTheListIsShowing() = runComposeUiTest {
        // Registering one here would swallow the gesture that leaves the tab.
        var action: (() -> Unit)? = { }
        val vm = demoVm()
        showSongsTab(vm, onRegisterBackAction = { action = it })

        awaitThat { vm.songs.value.isNotEmpty() }

        assertNull(action)
    }

    @Test
    fun aBackActionIsRegisteredOnceASheetIsOpen() = runComposeUiTest {
        var action: (() -> Unit)? = null
        val vm = demoVm()
        showSongsTab(vm, onRegisterBackAction = { action = it })

        awaitThat { exists(hymn("1")) }
        click(hymn("1"))

        awaitThat { action != null }
        assertTrue(action != null)
    }

    @Test
    fun theRegisteredBackActionClosesTheSheet() = runComposeUiTest {
        var action: (() -> Unit)? = null
        val vm = demoVm()
        showSongsTab(vm, onRegisterBackAction = { action = it })

        awaitThat { exists(hymn("1")) }
        click(hymn("1"))
        awaitThat { action != null }
        action!!.invoke()

        awaitThat { vm.songDetail.value == null }
        assertNull(vm.songDetail.value)
    }

    @Test
    fun closingTheSheetBringsTheListBack() = runComposeUiTest {
        var action: (() -> Unit)? = null
        val vm = demoVm()
        showSongsTab(vm, onRegisterBackAction = { action = it })

        awaitThat { exists(hymn("1")) }
        click(hymn("1"))
        awaitThat { action != null }
        action!!.invoke()

        awaitThat { exists(UiTags.SONGS_SEARCH) }
        assertTrue(exists(UiTags.SONGS_SEARCH))
    }

    @Test
    fun closingTheSheetClearsTheToolbarTitle() = runComposeUiTest {
        // A toolbar still naming a closed song is the bug this guards.
        var title: String? = null
        var action: (() -> Unit)? = null
        val vm = demoVm()
        showSongsTab(
            vm,
            onDetailChanged = { t, _ -> title = t },
            onRegisterBackAction = { action = it },
        )

        awaitThat { exists(hymn("1")) }
        click(hymn("1"))
        awaitThat { action != null }
        action!!.invoke()

        awaitThat { title == null }
        assertNull(title)
    }

    @Test
    fun closingTheSheetUnregistersTheBackAction() = runComposeUiTest {
        val registered = mutableListOf<(() -> Unit)?>()
        val vm = demoVm()
        showSongsTab(vm, onRegisterBackAction = { registered += it })

        awaitThat { exists(hymn("1")) }
        click(hymn("1"))
        awaitThat { registered.lastOrNull() != null }
        registered.last()!!.invoke()

        awaitThat { registered.lastOrNull() == null }
        assertNull(registered.last())
    }

    // ── Being sent here from somewhere else ──────────────────────────────

    @Test
    fun aSongNavigatedToFromTheScheduleIsOpened() = runComposeUiTest {
        val vm = demoVm()
        showSongsTab(vm, pendingNavSongTitle = "Be Thou My Vision", pendingNavSongBook = "Hymnal")

        awaitThat { vm.songDetail.value != null }
        assertEquals("Be Thou My Vision", vm.selectedSong.value?.title)
    }

    @Test
    fun aHandledNavigationIsReportedSoItIsNotRepeated() = runComposeUiTest {
        // Without this the same song reopens every recomposition, and the
        // operator cannot get back to the list.
        var handled = false
        val vm = demoVm()
        showSongsTab(
            vm,
            pendingNavSongTitle = "Be Thou My Vision",
            pendingNavSongBook = "Hymnal",
            onPendingNavHandled = { handled = true },
        )

        awaitThat { handled }
        assertTrue(handled)
    }

    @Test
    fun nothingIsOpenedWhenThereIsNoPendingNavigation() = runComposeUiTest {
        val vm = demoVm()
        showSongsTab(vm, pendingNavSongTitle = null)

        awaitThat { vm.songs.value.isNotEmpty() }
        assertNull(vm.songDetail.value)
    }

    @Test
    fun noNavigationIsReportedHandledWhenThereWasNone() = runComposeUiTest {
        var handled = false
        val vm = demoVm()
        showSongsTab(vm, pendingNavSongTitle = null, onPendingNavHandled = { handled = true })

        awaitThat { vm.songs.value.isNotEmpty() }
        assertFalse(handled)
    }
}
