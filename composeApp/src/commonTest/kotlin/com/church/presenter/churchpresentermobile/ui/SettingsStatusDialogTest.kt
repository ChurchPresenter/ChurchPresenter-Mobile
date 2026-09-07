package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The server-status dialog, reached from Settings.
 *
 * Its whole job is telling four failures apart — unreachable, key rejected,
 * something else answering, and connected-but-restricted — because each sends
 * the operator somewhere different. Every test here asserts the other states are
 * *absent* as well, since showing two at once is as misleading as showing the
 * wrong one.
 */
@OptIn(ExperimentalTestApi::class)
class SettingsStatusDialogTest {

    // ── Opening it ───────────────────────────────────────────────────────

    @Test
    fun checkingTheStatusIsOffered() = runComposeUiTest {
        showSettings(storedSettings())

        assertTrue(exists(UiTags.SETTINGS_CHECK_STATUS))
    }

    @Test
    fun noDialogIsOpenToBeginWith() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings, status = statusVm(settings))

        assertFalse(exists(UiTags.STATUS_DIALOG_CLOSE))
    }

    @Test
    fun checkingTheStatusOpensTheDialog() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings, status = statusVm(settings))

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { exists(UiTags.STATUS_DIALOG_CLOSE) }
    }

    @Test
    fun theDialogOffersARecheck() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings, status = statusVm(settings))

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { exists(UiTags.STATUS_DIALOG_RECHECK) }
    }

    @Test
    fun closingTheDialogLeavesTheSheetOpen() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings, status = statusVm(settings))
        click(UiTags.SETTINGS_CHECK_STATUS)
        awaitThat { exists(UiTags.STATUS_DIALOG_CLOSE) }

        click(UiTags.STATUS_DIALOG_CLOSE)

        awaitThat { !exists(UiTags.STATUS_DIALOG_CLOSE) }
        assertTrue(exists(UiTags.SETTINGS_SAVE))
    }

    @Test
    fun closingTheDialogSavesNothing() = runComposeUiTest {
        var saved = 0
        val settings = storedSettings()
        showSettings(settings, status = statusVm(settings), onSaved = { saved++ })
        click(UiTags.SETTINGS_CHECK_STATUS)
        awaitThat { exists(UiTags.STATUS_DIALOG_CLOSE) }

        click(UiTags.STATUS_DIALOG_CLOSE)

        awaitThat { !exists(UiTags.STATUS_DIALOG_CLOSE) }
        assertFalse(saved > 0)
    }

    // ── A healthy desktop ────────────────────────────────────────────────

    @Test
    fun aHealthyDesktopReportsConnected() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings, status = statusVm(settings, body = healthyDesktop))

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { exists(UiTags.STATUS_DIALOG_CONNECTED) }
    }

    @Test
    fun aHealthyDesktopReportsNoFailure() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings, status = statusVm(settings, body = healthyDesktop))

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { exists(UiTags.STATUS_DIALOG_CONNECTED) }
        assertFalse(exists(UiTags.STATUS_DIALOG_ERROR))
        assertFalse(exists(UiTags.STATUS_DIALOG_UNAUTHORIZED))
        assertFalse(exists(UiTags.STATUS_DIALOG_NOT_CHURCHPRESENTER))
    }

    @Test
    fun aHealthyDesktopListsWhatThisDeviceMayDo() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings, status = statusVm(settings, body = healthyDesktop))

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { exists(UiTags.STATUS_DIALOG_PERMISSIONS) }
    }

    @Test
    fun aHealthyDesktopListsItsBibles() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings, status = statusVm(settings, body = healthyDesktop))

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { isShowing("KJV") }
    }

    @Test
    fun aHealthyDesktopListsEveryBible() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings, status = statusVm(settings, body = healthyDesktop))

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { isShowing("ESV") }
    }

    @Test
    fun aHealthyDesktopListsItsSongbooks() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings, status = statusVm(settings, body = healthyDesktop))

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { isShowing("Hymns") }
    }

    @Test
    fun aHealthyDesktopNamesItsVersion() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings, status = statusVm(settings, body = healthyDesktop))

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { exists(UiTags.STATUS_DIALOG_SERVER_VERSION) }
    }

    @Test
    fun aHealthyDesktopWarnsAboutNothing() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings, status = statusVm(settings, body = healthyDesktop))

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { exists(UiTags.STATUS_DIALOG_CONNECTED) }
        assertFalse(exists(UiTags.STATUS_DIALOG_WARNINGS))
    }

    // ── A restricted desktop ─────────────────────────────────────────────

    @Test
    fun aRestrictedDesktopIsStillReportedAsReached() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings, status = statusVm(settings, body = restrictedDesktop))

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { exists(UiTags.STATUS_DIALOG_CONNECTED) }
    }

    @Test
    fun aRestrictedDesktopWarnsAboutIt() = runComposeUiTest {
        // Connected but unable to present is not "fine", and the operator needs
        // to know before the service rather than during it.
        val settings = storedSettings()
        showSettings(settings, status = statusVm(settings, body = restrictedDesktop))

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { exists(UiTags.STATUS_DIALOG_WARNINGS) }
    }

    @Test
    fun aRestrictedDesktopStillListsItsPermissions() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings, status = statusVm(settings, body = restrictedDesktop))

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { exists(UiTags.STATUS_DIALOG_PERMISSIONS) }
    }

    @Test
    fun aDesktopWithNoContentSaysSoRatherThanListingNothing() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings, status = statusVm(settings, body = restrictedDesktop))

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { exists(UiTags.STATUS_DIALOG_BIBLES) }
        assertTrue(exists(UiTags.STATUS_DIALOG_SONGBOOKS))
    }

    @Test
    fun aRestrictedDesktopIsNotReportedAsAFailure() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings, status = statusVm(settings, body = restrictedDesktop))

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { exists(UiTags.STATUS_DIALOG_WARNINGS) }
        assertFalse(exists(UiTags.STATUS_DIALOG_ERROR))
    }

    // ── A rejected key ───────────────────────────────────────────────────

    @Test
    fun aRejectedKeyIsReportedAsSuch() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(
            settings,
            status = statusVm(settings, body = "{}", status = HttpStatusCode.Unauthorized),
        )

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { exists(UiTags.STATUS_DIALOG_UNAUTHORIZED) }
    }

    @Test
    fun aRejectedKeyIsNotReportedAsUnreachable() = runComposeUiTest {
        // The desktop answered; the phone simply is not allowed in. Sending the
        // operator to check the network cable would waste the whole service.
        val settings = storedSettings()
        showSettings(
            settings,
            status = statusVm(settings, body = "{}", status = HttpStatusCode.Unauthorized),
        )

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { exists(UiTags.STATUS_DIALOG_UNAUTHORIZED) }
        assertFalse(exists(UiTags.STATUS_DIALOG_ERROR))
    }

    @Test
    fun aRejectedKeyIsNotReportedAsConnected() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(
            settings,
            status = statusVm(settings, body = "{}", status = HttpStatusCode.Forbidden),
        )

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { exists(UiTags.STATUS_DIALOG_UNAUTHORIZED) }
        assertFalse(exists(UiTags.STATUS_DIALOG_CONNECTED))
    }

    @Test
    fun aForbiddenReplyIsTreatedTheSameAsUnauthorized() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(
            settings,
            status = statusVm(settings, body = "{}", status = HttpStatusCode.Forbidden),
        )

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { exists(UiTags.STATUS_DIALOG_UNAUTHORIZED) }
    }

    // ── Something that is not ChurchPresenter ────────────────────────────

    @Test
    fun aStrangerAnsweringIsReportedAsSuch() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings, status = statusVm(settings, body = notChurchPresenter))

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { exists(UiTags.STATUS_DIALOG_NOT_CHURCHPRESENTER) }
    }

    @Test
    fun aStrangerAnsweringIsNotReportedAsConnected() = runComposeUiTest {
        // A router's admin page on the same port is a real way to get here.
        val settings = storedSettings()
        showSettings(settings, status = statusVm(settings, body = notChurchPresenter))

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { exists(UiTags.STATUS_DIALOG_NOT_CHURCHPRESENTER) }
        assertFalse(exists(UiTags.STATUS_DIALOG_CONNECTED))
    }

    @Test
    fun aStrangerAnsweringIsNotReportedAsARejectedKey() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings, status = statusVm(settings, body = notChurchPresenter))

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { exists(UiTags.STATUS_DIALOG_NOT_CHURCHPRESENTER) }
        assertFalse(exists(UiTags.STATUS_DIALOG_UNAUTHORIZED))
    }

    // ── Nothing answering at all ─────────────────────────────────────────

    @Test
    fun anUnreachableDesktopIsReported() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings, status = unreachableStatusVm(settings))

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { exists(UiTags.STATUS_DIALOG_ERROR) }
    }

    @Test
    fun anUnreachableDesktopIsNotReportedAsConnected() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings, status = unreachableStatusVm(settings))

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { exists(UiTags.STATUS_DIALOG_ERROR) }
        assertFalse(exists(UiTags.STATUS_DIALOG_CONNECTED))
    }

    @Test
    fun anUnreachableDesktopStillOffersARecheck() = runComposeUiTest {
        // It is the one thing worth doing after plugging the cable back in.
        val settings = storedSettings()
        showSettings(settings, status = unreachableStatusVm(settings))

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { exists(UiTags.STATUS_DIALOG_ERROR) }
        assertTrue(exists(UiTags.STATUS_DIALOG_RECHECK))
    }

    @Test
    fun anUnreachableDesktopCanStillBeClosed() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings, status = unreachableStatusVm(settings))
        click(UiTags.SETTINGS_CHECK_STATUS)
        awaitThat { exists(UiTags.STATUS_DIALOG_ERROR) }

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
        showSettings(settings, status = vm)

        click(UiTags.SETTINGS_CHECK_STATUS)

        awaitThat { exists(UiTags.STATUS_DIALOG_CONNECTED) }
    }

    @Test
    fun recheckingKeepsTheDialogOpen() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings, status = statusVm(settings, body = healthyDesktop))
        click(UiTags.SETTINGS_CHECK_STATUS)
        awaitThat { exists(UiTags.STATUS_DIALOG_CONNECTED) }

        click(UiTags.STATUS_DIALOG_RECHECK)

        awaitThat { exists(UiTags.STATUS_DIALOG_CONNECTED) }
    }

    @Test
    fun recheckingDoesNotCloseTheSettingsSheet() = runComposeUiTest {
        val settings = storedSettings()
        showSettings(settings, status = statusVm(settings, body = healthyDesktop))
        click(UiTags.SETTINGS_CHECK_STATUS)
        awaitThat { exists(UiTags.STATUS_DIALOG_CONNECTED) }

        click(UiTags.STATUS_DIALOG_RECHECK)

        awaitThat { exists(UiTags.SETTINGS_SAVE) }
    }
}
