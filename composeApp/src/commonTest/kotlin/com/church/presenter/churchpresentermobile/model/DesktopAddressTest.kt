package com.church.presenter.churchpresentermobile.model

import com.church.presenter.churchpresentermobile.network.ApiConstants
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The one validator both address surfaces go through.
 *
 * Two places now write the computer's address — Settings, and the sync sheet — and they must not
 * disagree about what a usable one is.
 */
class DesktopAddressTest {

    private fun settings() = AppSettings(InMemorySettingsStorage())

    @Test
    fun aUsableAddressIsWritten() {
        val settings = settings()

        val outcome = DesktopAddress.save(settings, host = " 10.0.0.5 ", port = "9000", apiKey = " k ")

        assertIs<DesktopAddress.Outcome.Saved>(outcome)
        assertEquals("10.0.0.5", settings.host)
        assertEquals(9000, settings.port)
        assertEquals("k", settings.apiKey)
    }

    @Test
    fun aBlankHostIsRejectedAndNothingIsWritten() {
        val settings = settings()

        val outcome = DesktopAddress.save(settings, host = "   ", port = "9000", apiKey = "")

        assertTrue(assertIs<DesktopAddress.Outcome.Invalid>(outcome).hostBlank)
        assertEquals(ApiConstants.DEFAULT_HOST, settings.host)
    }

    @Test
    fun aPortOutsideTheUsableRangeIsRejectedAndNothingIsWritten() {
        val settings = settings()

        val outcome = DesktopAddress.save(settings, host = "10.0.0.5", port = "70000", apiKey = "")

        assertTrue(assertIs<DesktopAddress.Outcome.Invalid>(outcome).portInvalid)
        assertEquals(ApiConstants.DEFAULT_HOST, settings.host)
    }

    @Test
    fun aPortThatIsNotANumberIsRejected() {
        val outcome = DesktopAddress.save(settings(), host = "10.0.0.5", port = "nonsense", apiKey = "")

        assertTrue(assertIs<DesktopAddress.Outcome.Invalid>(outcome).portInvalid)
    }

    @Test
    fun anApiKeyIsSavedSoAKeyedComputerIsReachable() {
        // Every /api/bible/file route is behind the key on the desktop, and standalone had no
        // way to supply one at all before this.
        val settings = settings()

        DesktopAddress.save(settings, host = "10.0.0.5", port = "8765", apiKey = "secret")

        assertEquals("secret", settings.apiKey)
    }

    @Test
    fun aSuccessfulSaveTellsTheOtherSurfaceToReload() {
        val before = DesktopAddress.changeCount.value

        DesktopAddress.save(settings(), host = "10.0.0.5", port = "8765", apiKey = "")

        assertEquals(before + 1, DesktopAddress.changeCount.value)
    }

    @Test
    fun aRejectedSaveDoesNotTellAnyoneAnythingChanged() {
        val before = DesktopAddress.changeCount.value

        DesktopAddress.save(settings(), host = "", port = "8765", apiKey = "")

        assertEquals(before, DesktopAddress.changeCount.value)
    }
}
