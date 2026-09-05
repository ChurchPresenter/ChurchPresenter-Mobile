package com.church.presenter.churchpresentermobile.model

import com.church.presenter.churchpresentermobile.network.ApiConstants
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
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

    // ── Hosts that cannot be hosts ───────────────────────────────────────

    @Test
    fun aDictatedAddressWithSpacesIsRefused() {
        // Reported from the field: the host arrived as "high dynamic range",
        // was saved, and then threw out of the URL builder on every request
        // and every WebSocket reconnect.
        val settings = settings()

        val outcome = DesktopAddress.save(settings, host = "high dynamic range", port = "8765", apiKey = "")

        val invalid = assertIs<DesktopAddress.Outcome.Invalid>(outcome)
        assertTrue(invalid.hostInvalid)
        assertFalse(invalid.hostBlank)
        assertNotEquals("high dynamic range", settings.host)
    }

    @Test
    fun blankAndUnusableAreReportedApart() {
        // The field says different things for each, so they must not collapse.
        val blank = assertIs<DesktopAddress.Outcome.Invalid>(
            DesktopAddress.save(settings(), host = "  ", port = "8765", apiKey = "")
        )
        assertTrue(blank.hostBlank)
        assertFalse(blank.hostInvalid)
    }

    @Test
    fun ordinaryAddressesAreStillAccepted() {
        listOf("192.168.1.5", "10.0.0.5", "my-mac.local", "desktop_2", "localhost", "[::1]")
            .forEach { assertTrue(DesktopAddress.isUsableHost(it), "rejected $it") }
    }

    @Test
    fun thingsThatCannotBeAHostAreRefused() {
        listOf("high dynamic range", "192.168.1.5:8765", "http://x", "a/b", "", "  ", "-leading")
            .forEach { assertFalse(DesktopAddress.isUsableHost(it), "accepted $it") }
    }

    // ── Pasted URLs ──────────────────────────────────────────────────────

    @Test
    fun aPastedUrlIsReducedToItsHost() {
        // Copying the address off the desktop's screen is the ordinary way to
        // get this wrong, and the paste carries the right answer.
        val settings = settings()

        val outcome = DesktopAddress.save(
            settings, host = "http://192.168.1.5/api", port = "8765", apiKey = "",
        )

        assertIs<DesktopAddress.Outcome.Saved>(outcome)
        assertEquals("192.168.1.5", settings.host)
    }

    @Test
    fun aPastedWebSocketUrlIsAlsoReduced() {
        assertEquals("192.168.1.5", DesktopAddress.normalizeHost("ws://192.168.1.5/ws"))
    }

    @Test
    fun aPastedUrlStillCarryingAPortIsRefusedRatherThanGuessedAt() {
        // Silently dropping the :9999 would connect somewhere the operator can
        // see they did not ask for. The field says so instead.
        val settings = settings()

        val outcome = DesktopAddress.save(
            settings, host = "http://192.168.1.5:9999/api", port = "8765", apiKey = "",
        )

        assertTrue(assertIs<DesktopAddress.Outcome.Invalid>(outcome).hostInvalid)
    }
}
