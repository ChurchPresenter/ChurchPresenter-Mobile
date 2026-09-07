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

    // ── What can be a host ───────────────────────────────────────────────
    //
    // The field writes through on every keystroke, so anything accepted here
    // reaches the HTTP client and the socket's reconnect loop. A dictated address
    // that arrived as "high dynamic range" was accepted, saved, and then thrown
    // out by the URL builder on every request, forever.

    @Test
    fun `ordinary hosts are usable`() {
        for (host in listOf("10.0.0.5", "192.168.1.100", "desktop", "desktop.local", "my-mac", "my_mac", "MyMac")) {
            assertTrue(DesktopAddress.isUsableHost(host), host)
        }
    }

    @Test
    fun `anything a url would have to escape is refused`() {
        for (host in listOf("high dynamic range", "my mac", "10.0.0.5:8765", "host/path", "host?q", "a,b", "a@b")) {
            assertFalse(DesktopAddress.isUsableHost(host), host)
        }
    }

    @Test
    fun `a blank host is not usable`() {
        assertFalse(DesktopAddress.isUsableHost(""))
        assertFalse(DesktopAddress.isUsableHost("   "))
    }

    @Test
    fun `a host must start on something alphanumeric`() {
        assertFalse(DesktopAddress.isUsableHost("-desktop"))
        assertFalse(DesktopAddress.isUsableHost(".desktop"))
        assertFalse(DesktopAddress.isUsableHost("_desktop"))
    }

    @Test
    fun `a bracketed IPv6 literal is the one legitimate use of colons`() {
        assertTrue(DesktopAddress.isUsableHost("[fe80::1]"))
        assertTrue(DesktopAddress.isUsableHost("[::1]"))
    }

    @Test
    fun `an unclosed or malformed IPv6 literal is refused`() {
        assertFalse(DesktopAddress.isUsableHost("[fe80::1"))
        assertFalse(DesktopAddress.isUsableHost("[not hex]"))
    }

    @Test
    fun `a well-formed but wrong name is still usable`() {
        // It fails later, where it is correctly reported as unreachable — this
        // check only rejects what cannot be a host at all.
        assertTrue(DesktopAddress.isUsableHost("printer"))
    }

    @Test
    fun `surrounding whitespace is trimmed before the check`() {
        assertTrue(DesktopAddress.isUsableHost("  10.0.0.5  "))
    }

    // ── Normalising a pasted address ─────────────────────────────────────

    @Test
    fun `every scheme is stripped`() {
        for (scheme in listOf("http://", "https://", "ws://", "wss://")) {
            assertEquals("10.0.0.5", DesktopAddress.normalizeHost("$scheme" + "10.0.0.5"))
        }
    }

    @Test
    fun `a scheme is stripped whatever case it is written in`() {
        assertEquals("10.0.0.5", DesktopAddress.normalizeHost("HTTP://10.0.0.5"))
    }

    @Test
    fun `a path, query and fragment are all dropped`() {
        assertEquals("10.0.0.5", DesktopAddress.normalizeHost("http://10.0.0.5/display"))
        assertEquals("10.0.0.5", DesktopAddress.normalizeHost("10.0.0.5?x=1"))
        assertEquals("10.0.0.5", DesktopAddress.normalizeHost("10.0.0.5#top"))
    }

    @Test
    fun `a bare host is left alone`() {
        assertEquals("desktop.local", DesktopAddress.normalizeHost("desktop.local"))
    }

    @Test
    fun `the port survives normalisation and then fails the host check`() {
        // Pasting the whole address from a browser is the obvious thing to try, and
        // it lands as an error rather than being split into host and port.
        val normalized = DesktopAddress.normalizeHost("http://10.0.0.5:8765/")

        assertEquals("10.0.0.5:8765", normalized)
        assertFalse(DesktopAddress.isUsableHost(normalized))
    }
}
