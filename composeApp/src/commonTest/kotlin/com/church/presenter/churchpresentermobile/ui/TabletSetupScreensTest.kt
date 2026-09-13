package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.AppTab
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.viewmodel.StatusViewModel
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The setup screens in their tablet arrangement: the startup status screen,
 * the status modal, the mode picker and the rail.
 *
 * Each draws the phone's content in a different place rather than different
 * content, so what these tests guard is that the rearrangement lost nothing:
 * every control the phone has is still there and still reaches its callback.
 */
@OptIn(ExperimentalTestApi::class)
class TabletSetupScreensTest {

    private fun settings() = AppSettings(InMemorySettingsStorage())

    // ── Startup status ───────────────────────────────────────────────────

    private fun ComposeUiTest.showStatus(
        vm: StatusViewModel,
        onContinue: () -> Unit = {},
        onOpenSettings: () -> Unit = {},
    ) = showScreen {
        StatusScreen(viewModel = vm, onContinue = onContinue, onOpenSettings = onOpenSettings, twoPane = true)
    }

    @Test
    fun aHealthyDesktopStillReportsAllGoodSideBySide() = runComposeUiTest {
        showStatus(statusVm(settings(), body = healthyDesktop))

        awaitThat { exists(UiTags.STATUS_ALL_GOOD) }
        assertTrue(isShowing("KJV"))
        assertTrue(isShowing("Hymns"))
    }

    @Test
    fun continueStillWorksFromTheSideBySideAllGoodScreen() = runComposeUiTest {
        var continued = false
        showStatus(statusVm(settings(), body = healthyDesktop), onContinue = { continued = true })

        awaitThat { exists(UiTags.STATUS_ALL_GOOD) }
        click(UiTags.STATUS_CONTINUE)

        assertTrue(continued)
    }

    @Test
    fun anOlderDesktopShowsPermissionsWithNoContentCard() = runComposeUiTest {
        // Nothing to list, so the second card would be two empty headings.
        showStatus(olderDesktopStatusVm(settings()))

        awaitThat { exists(UiTags.STATUS_ALL_GOOD) }
        assertFalse(isShowing("KJV"))
        assertTrue(exists(UiTags.STATUS_CONTINUE))
    }

    @Test
    fun aRestrictedDesktopWithContentListsItBesideTheIssues() = runComposeUiTest {
        // Warnings on the right, and the information cards under them.
        showStatus(statusVm(settings(), body = restrictedWithContent))

        awaitThat { exists(UiTags.STATUS_WARNINGS) }
        assertTrue(isShowing("KJV"))
        assertTrue(isShowing("Hymns"))
        assertTrue(isShowing("qa"))
    }

    @Test
    fun aRestrictedDesktopStillWarnsInTheGrid() = runComposeUiTest {
        showStatus(statusVm(settings(), body = restrictedDesktop))

        awaitThat { exists(UiTags.STATUS_WARNINGS) }
        assertFalse(exists(UiTags.STATUS_ALL_GOOD))
    }

    @Test
    fun theButtonsUnderThePermissionsStillWork() = runComposeUiTest {
        // On a tablet Continue and Open Settings move under the permissions
        // card, beside the grid, instead of under everything. Same handlers.
        var continued = false
        var opened = false
        showStatus(
            statusVm(settings(), body = restrictedDesktop),
            onContinue = { continued = true },
            onOpenSettings = { opened = true },
        )

        awaitThat { exists(UiTags.STATUS_WARNINGS) }
        click(UiTags.STATUS_CONTINUE)
        click(UiTags.STATUS_OPEN_SETTINGS)

        assertTrue(continued)
        assertTrue(opened)
    }

    @Test
    fun retryAndSettingsShareARowAndBothStillFire() = runComposeUiTest {
        var opened = false
        val vm = statusVm(settings(), body = "", status = HttpStatusCode.ServiceUnavailable)
        showStatus(vm, onOpenSettings = { opened = true })

        awaitThat { exists(UiTags.STATUS_ERROR) }
        assertTrue(exists(UiTags.STATUS_RETRY))
        click(UiTags.STATUS_OPEN_SETTINGS)

        assertTrue(opened)
    }

    @Test
    fun continueAnywayStillEscapesAnError() = runComposeUiTest {
        var continued = false
        showStatus(statusVm(settings(), body = NOT_CHURCH_PRESENTER), onContinue = { continued = true })

        awaitThat { exists(UiTags.STATUS_ERROR) }
        click(UiTags.STATUS_CONTINUE)

        assertTrue(continued)
    }

    // ── The status modal ─────────────────────────────────────────────────

    private fun ComposeUiTest.showModal(vm: StatusViewModel, onDismiss: () -> Unit = {}) = showScreen {
        ServerStatusDialog(statusViewModel = vm, onDismiss = onDismiss, twoPane = true, address = "http://h:1/api")
    }

    @Test
    fun theModalReportsAHealthyDesktopAcrossTheTablet() = runComposeUiTest {
        showModal(statusVm(settings(), body = healthyDesktop))

        awaitThat { exists(UiTags.STATUS_DIALOG_CONNECTED) }
        assertTrue(exists(UiTags.STATUS_DIALOG_PERMISSIONS))
        assertTrue(isShowing("KJV"))
        assertFalse(exists(UiTags.STATUS_DIALOG_WARNINGS))
    }

    @Test
    fun theModalCountsARestrictedDesktopsIssues() = runComposeUiTest {
        showModal(statusVm(settings(), body = restrictedDesktop))

        awaitThat { exists(UiTags.STATUS_DIALOG_WARNINGS) }
        // Five warnings, named by their codes so a reader can quote one.
        assertTrue(isShowing("NoBibles"))
        assertTrue(isShowing("UploadBlocked"))
    }

    @Test
    fun theModalShowsAnOlderDesktopsContentAsNone() = runComposeUiTest {
        showModal(olderDesktopStatusVm(settings()))

        awaitThat { exists(UiTags.STATUS_DIALOG_CONNECTED) }
        assertTrue(isShowing("none"))
    }

    @Test
    fun theModalNamesTheAddressItTriedOnAFailure() = runComposeUiTest {
        showModal(statusVm(settings(), body = NOT_CHURCH_PRESENTER))

        awaitThat { exists(UiTags.STATUS_DIALOG_NOT_CHURCHPRESENTER) }
        assertTrue(isShowing("http://h:1/api"))
    }

    @Test
    fun theModalStillOffersARecheck() = runComposeUiTest {
        showModal(unreachableStatusVm(settings()))

        awaitThat { exists(UiTags.STATUS_DIALOG_ERROR) }
        assertTrue(exists(UiTags.STATUS_DIALOG_RECHECK))
    }

    // ── The mode picker ──────────────────────────────────────────────────

    @Test
    fun theTabletPickerContinuesWithTheChosenMode() = runComposeUiTest {
        var chosen: AppMode? = null
        showScreen { ModePickerScreen(onModeChosen = { chosen = it }, twoPane = true) }

        click(UiTags.modeCard(AppMode.STANDALONE))
        click(UiTags.MODE_CONTINUE)

        assertEquals(AppMode.STANDALONE, chosen)
    }

    @Test
    fun theTabletPickerDefaultsToRemote() = runComposeUiTest {
        var chosen: AppMode? = null
        showScreen { ModePickerScreen(onModeChosen = { chosen = it }, twoPane = true) }

        click(UiTags.MODE_CONTINUE)

        assertEquals(AppMode.REMOTE, chosen)
    }

    // ── The rail ─────────────────────────────────────────────────────────

    @Test
    fun theRailsHamburgerOpensTheSchedule() = runComposeUiTest {
        var opened = 0
        showScreen { NavRail(selectedTab = AppTab.SONGS, onTabSelected = {}, onMenu = { opened++ }) }

        click(UiTags.HEADER_MENU)

        assertEquals(1, opened)
    }

    @Test
    fun theRailHasNoHamburgerWithoutADrawerToOpen() = runComposeUiTest {
        showScreen { NavRail(selectedTab = AppTab.SONGS, onTabSelected = {}) }

        assertFalse(exists(UiTags.HEADER_MENU))
    }
}
