package com.church.presenter.churchpresentermobile.screenshot

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.CertSetupScreen
import com.church.presenter.churchpresentermobile.ui.ConnectSetupScreen
import com.church.presenter.churchpresentermobile.ui.ContactScreen
import com.church.presenter.churchpresentermobile.ui.NOT_CHURCH_PRESENTER
import com.church.presenter.churchpresentermobile.ui.StatusScreen
import com.church.presenter.churchpresentermobile.ui.healthyDesktop
import com.church.presenter.churchpresentermobile.ui.restrictedDesktop
import com.church.presenter.churchpresentermobile.ui.statusVm
import io.ktor.http.HttpStatusCode
import kotlin.test.Test

/**
 * Everything between opening the app and being connected to a computer.
 *
 * These are the screens a volunteer meets on a Sunday morning when something is
 * wrong, so their unhappy states matter more than their happy one: a desktop
 * that answers but refuses this device, a desktop that is not ChurchPresenter
 * at all, a certificate nobody has accepted yet.
 */
class SetupScreenshotTest {

    private fun settings() = AppSettings(InMemorySettingsStorage())

    @Test
    fun statusHealthy() = screenshot("status__healthy") {
        val settings = settings()
        StatusScreen(
            viewModel = statusVm(settings, body = healthyDesktop),
            onContinue = {},
            onOpenSettings = {},
        )
    }

    @Test
    fun statusRestricted() = screenshot("status__restricted") {
        // Reachable, but this device may not present or upload — the screen has
        // to say which permissions are missing, not just "connected".
        val settings = settings()
        StatusScreen(
            viewModel = statusVm(settings, body = restrictedDesktop),
            onContinue = {},
            onOpenSettings = {},
        )
    }

    @Test
    fun statusNotChurchPresenter() = screenshot("status__not-churchpresenter") {
        val settings = settings()
        StatusScreen(
            viewModel = statusVm(settings, body = NOT_CHURCH_PRESENTER),
            onContinue = {},
            onOpenSettings = {},
        )
    }

    @Test
    fun statusUnreachable() = screenshot("status__unreachable") {
        val settings = settings()
        StatusScreen(
            viewModel = statusVm(settings, body = "", status = HttpStatusCode.ServiceUnavailable),
            onContinue = {},
            onOpenSettings = {},
        )
    }

    @Test
    fun connectSetup() = screenshot("connect-setup__initial") {
        ConnectSetupScreen(appSettings = settings(), onDone = {}, onSkip = {})
    }

    @Test
    fun certSetupWithFingerprint() = screenshot("cert-setup__with-fingerprint") {
        CertSetupScreen(
            certFingerprint = "A1:B2:C3:D4:E5:F6:07:18:29:3A:4B:5C:6D:7E:8F:90",
            onDone = {},
            onSkip = {},
        )
    }

    @Test
    fun certSetupWithoutFingerprint() = screenshot("cert-setup__no-fingerprint") {
        CertSetupScreen(certFingerprint = null, onDone = {}, onSkip = {})
    }

    @Test
    fun contact() = screenshot("contact__form") {
        ContactScreen()
    }
}
