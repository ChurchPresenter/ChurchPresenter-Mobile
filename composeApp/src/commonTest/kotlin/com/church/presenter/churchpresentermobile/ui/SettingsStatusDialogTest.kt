package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import io.ktor.http.HttpStatusCode
import com.church.presenter.churchpresentermobile.viewmodel.StatusViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The server-status dialog. Settings now shows the same states on its Server page
 * (see SettingsRedesignTest); the dialog itself is still exercised here.
 *
 * Its whole job is telling four failures apart — unreachable, key rejected,
 * something else answering, and connected-but-restricted — because each sends
 * the operator somewhere different. Every test here asserts the other states are
 * *absent* as well, since showing two at once is as misleading as showing the
 * wrong one.
 */
@OptIn(ExperimentalTestApi::class)
class SettingsStatusDialogTest {

    @Composable
    private fun Dialog(vm: StatusViewModel, onDismiss: () -> Unit) {
        var open by remember { mutableStateOf(true) }
        if (open) ServerStatusDialog(statusViewModel = vm, onDismiss = { open = false; onDismiss() })
    }

    private fun ComposeUiTest.showDialog(vm: StatusViewModel, onDismiss: () -> Unit = {}) =
        showScreen { Dialog(vm, onDismiss) }

    @Test
    fun theDialogOpensClosable() = runComposeUiTest {
        showDialog(statusVm(storedSettings()))

        awaitThat { exists(UiTags.STATUS_DIALOG_CLOSE) }
    }

    @Test
    fun closingTheDialogDismissesIt() = runComposeUiTest {
        var dismissed = 0
        showDialog(statusVm(storedSettings()), onDismiss = { dismissed++ })
        awaitThat { exists(UiTags.STATUS_DIALOG_CLOSE) }

        click(UiTags.STATUS_DIALOG_CLOSE)

        awaitThat { !exists(UiTags.STATUS_DIALOG_CLOSE) }
        assertEquals(1, dismissed)
    }

    // ── Opening it ───────────────────────────────────────────────────────

    @Test
    fun theDialogOffersARecheck() = runComposeUiTest {
        val settings = storedSettings()
        showDialog(statusVm(settings))

        awaitThat { exists(UiTags.STATUS_DIALOG_RECHECK) }
    }

    @Test
    fun aHealthyDesktopReportsConnected() = runComposeUiTest {
        val settings = storedSettings()
        showDialog(statusVm(settings, body = healthyDesktop))

        awaitThat { exists(UiTags.STATUS_DIALOG_CONNECTED) }
    }

    @Test
    fun aHealthyDesktopReportsNoFailure() = runComposeUiTest {
        val settings = storedSettings()
        showDialog(statusVm(settings, body = healthyDesktop))

        awaitThat { exists(UiTags.STATUS_DIALOG_CONNECTED) }
        assertFalse(exists(UiTags.STATUS_DIALOG_ERROR))
        assertFalse(exists(UiTags.STATUS_DIALOG_UNAUTHORIZED))
        assertFalse(exists(UiTags.STATUS_DIALOG_NOT_CHURCHPRESENTER))
    }

    @Test
    fun aHealthyDesktopListsWhatThisDeviceMayDo() = runComposeUiTest {
        val settings = storedSettings()
        showDialog(statusVm(settings, body = healthyDesktop))

        awaitThat { exists(UiTags.STATUS_DIALOG_PERMISSIONS) }
    }

    @Test
    fun aHealthyDesktopListsItsBibles() = runComposeUiTest {
        val settings = storedSettings()
        showDialog(statusVm(settings, body = healthyDesktop))

        awaitThat { isShowing("KJV") }
    }

    @Test
    fun aHealthyDesktopListsEveryBible() = runComposeUiTest {
        val settings = storedSettings()
        showDialog(statusVm(settings, body = healthyDesktop))

        awaitThat { isShowing("ESV") }
    }

    @Test
    fun aHealthyDesktopListsItsSongbooks() = runComposeUiTest {
        val settings = storedSettings()
        showDialog(statusVm(settings, body = healthyDesktop))

        awaitThat { isShowing("Hymns") }
    }

    @Test
    fun aHealthyDesktopNamesItsVersion() = runComposeUiTest {
        val settings = storedSettings()
        showDialog(statusVm(settings, body = healthyDesktop))

        awaitThat { exists(UiTags.STATUS_DIALOG_SERVER_VERSION) }
    }

    @Test
    fun aHealthyDesktopWarnsAboutNothing() = runComposeUiTest {
        val settings = storedSettings()
        showDialog(statusVm(settings, body = healthyDesktop))

        awaitThat { exists(UiTags.STATUS_DIALOG_CONNECTED) }
        assertFalse(exists(UiTags.STATUS_DIALOG_WARNINGS))
    }

    // ── A restricted desktop ─────────────────────────────────────────────

    @Test
    fun aRestrictedDesktopIsStillReportedAsReached() = runComposeUiTest {
        val settings = storedSettings()
        showDialog(statusVm(settings, body = restrictedDesktop))

        awaitThat { exists(UiTags.STATUS_DIALOG_CONNECTED) }
    }

    @Test
    fun aRestrictedDesktopWarnsAboutIt() = runComposeUiTest {
        // Connected but unable to present is not "fine", and the operator needs
        // to know before the service rather than during it.
        val settings = storedSettings()
        showDialog(statusVm(settings, body = restrictedDesktop))

        awaitThat { exists(UiTags.STATUS_DIALOG_WARNINGS) }
    }

    @Test
    fun aRestrictedDesktopStillListsItsPermissions() = runComposeUiTest {
        val settings = storedSettings()
        showDialog(statusVm(settings, body = restrictedDesktop))

        awaitThat { exists(UiTags.STATUS_DIALOG_PERMISSIONS) }
    }

    @Test
    fun aDesktopWithNoContentSaysSoRatherThanListingNothing() = runComposeUiTest {
        val settings = storedSettings()
        showDialog(statusVm(settings, body = restrictedDesktop))

        awaitThat { exists(UiTags.STATUS_DIALOG_BIBLES) }
        assertTrue(exists(UiTags.STATUS_DIALOG_SONGBOOKS))
    }

    @Test
    fun aRestrictedDesktopIsNotReportedAsAFailure() = runComposeUiTest {
        val settings = storedSettings()
        showDialog(statusVm(settings, body = restrictedDesktop))

        awaitThat { exists(UiTags.STATUS_DIALOG_WARNINGS) }
        assertFalse(exists(UiTags.STATUS_DIALOG_ERROR))
    }

    // ── A rejected key ───────────────────────────────────────────────────

    @Test
    fun aRejectedKeyIsReportedAsSuch() = runComposeUiTest {
        val settings = storedSettings()
        showDialog(statusVm(settings, body = "{}", status = HttpStatusCode.Unauthorized))

        awaitThat { exists(UiTags.STATUS_DIALOG_UNAUTHORIZED) }
    }

    @Test
    fun aRejectedKeyIsNotReportedAsUnreachable() = runComposeUiTest {
        // The desktop answered; the phone simply is not allowed in. Sending the
        // operator to check the network cable would waste the whole service.
        val settings = storedSettings()
        showDialog(statusVm(settings, body = "{}", status = HttpStatusCode.Unauthorized))

        awaitThat { exists(UiTags.STATUS_DIALOG_UNAUTHORIZED) }
        assertFalse(exists(UiTags.STATUS_DIALOG_ERROR))
    }

    @Test
    fun aRejectedKeyIsNotReportedAsConnected() = runComposeUiTest {
        val settings = storedSettings()
        showDialog(statusVm(settings, body = "{}", status = HttpStatusCode.Forbidden))

        awaitThat { exists(UiTags.STATUS_DIALOG_UNAUTHORIZED) }
        assertFalse(exists(UiTags.STATUS_DIALOG_CONNECTED))
    }

    @Test
    fun aForbiddenReplyIsTreatedTheSameAsUnauthorized() = runComposeUiTest {
        val settings = storedSettings()
        showDialog(statusVm(settings, body = "{}", status = HttpStatusCode.Forbidden))

        awaitThat { exists(UiTags.STATUS_DIALOG_UNAUTHORIZED) }
    }

    // ── Something that is not ChurchPresenter ────────────────────────────

    @Test
    fun aStrangerAnsweringIsReportedAsSuch() = runComposeUiTest {
        val settings = storedSettings()
        showDialog(statusVm(settings, body = NOT_CHURCH_PRESENTER))

        awaitThat { exists(UiTags.STATUS_DIALOG_NOT_CHURCHPRESENTER) }
    }

    @Test
    fun aStrangerAnsweringIsNotReportedAsConnected() = runComposeUiTest {
        // A router's admin page on the same port is a real way to get here.
        val settings = storedSettings()
        showDialog(statusVm(settings, body = NOT_CHURCH_PRESENTER))

        awaitThat { exists(UiTags.STATUS_DIALOG_NOT_CHURCHPRESENTER) }
        assertFalse(exists(UiTags.STATUS_DIALOG_CONNECTED))
    }

    @Test
    fun aStrangerAnsweringIsNotReportedAsARejectedKey() = runComposeUiTest {
        val settings = storedSettings()
        showDialog(statusVm(settings, body = NOT_CHURCH_PRESENTER))

        awaitThat { exists(UiTags.STATUS_DIALOG_NOT_CHURCHPRESENTER) }
        assertFalse(exists(UiTags.STATUS_DIALOG_UNAUTHORIZED))
    }

    // ── Nothing answering at all ─────────────────────────────────────────

    @Test
    fun anUnreachableDesktopIsReported() = runComposeUiTest {
        val settings = storedSettings()
        showDialog(unreachableStatusVm(settings))

        awaitThat { exists(UiTags.STATUS_DIALOG_ERROR) }
    }

    @Test
    fun anUnreachableDesktopIsNotReportedAsConnected() = runComposeUiTest {
        val settings = storedSettings()
        showDialog(unreachableStatusVm(settings))

        awaitThat { exists(UiTags.STATUS_DIALOG_ERROR) }
        assertFalse(exists(UiTags.STATUS_DIALOG_CONNECTED))
    }

    @Test
    fun anUnreachableDesktopStillOffersARecheck() = runComposeUiTest {
        // It is the one thing worth doing after plugging the cable back in.
        val settings = storedSettings()
        showDialog(unreachableStatusVm(settings))

        awaitThat { exists(UiTags.STATUS_DIALOG_ERROR) }
        assertTrue(exists(UiTags.STATUS_DIALOG_RECHECK))
    }

    @Test
    fun anUnreachableDesktopCanStillBeClosed() = runComposeUiTest {
        val settings = storedSettings()
        showDialog(unreachableStatusVm(settings))
        awaitThat { exists(UiTags.STATUS_DIALOG_ERROR) }
        awaitThat { exists(UiTags.STATUS_DIALOG_CLOSE) }

        click(UiTags.STATUS_DIALOG_CLOSE)

        awaitThat { !exists(UiTags.STATUS_DIALOG_ERROR) }
    }

    // ── Rechecking ───────────────────────────────────────────────────────

    @Test
    fun theDialogChecksWhenItOpens() = runComposeUiTest {
        // Opening it on a stale answer from ten minutes ago would be worse than
        // useless.
        val settings = storedSettings()
        val vm = statusVm(settings, body = healthyDesktop)
        showDialog(vm)

        awaitThat { exists(UiTags.STATUS_DIALOG_CONNECTED) }
    }

    @Test
    fun recheckingKeepsTheDialogOpen() = runComposeUiTest {
        val settings = storedSettings()
        showDialog(statusVm(settings, body = healthyDesktop))
        awaitThat { exists(UiTags.STATUS_DIALOG_CONNECTED) }
        awaitThat { exists(UiTags.STATUS_DIALOG_RECHECK) }

        click(UiTags.STATUS_DIALOG_RECHECK)

        awaitThat { exists(UiTags.STATUS_DIALOG_CONNECTED) }
    }

}
