package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The address as edited in the sync sheet, which has no Save button.
 *
 * Writing through as it is typed is the whole point: the next tap on Copy builds its URL from
 * `settings.apiBaseUrl`, so anything still sitting in a draft would be ignored.
 */
class DesktopAddressViewModelTest {

    private fun vm(): Pair<DesktopAddressViewModel, AppSettings> {
        val settings = AppSettings(InMemorySettingsStorage())
        return DesktopAddressViewModel(settings) to settings
    }

    @Test
    fun aTypedHostReachesSettingsWithoutASaveButton() = runVmTest {
        val (viewModel, settings) = vm()

        viewModel.setHost("10.0.0.5")

        assertEquals("10.0.0.5", settings.host)
    }

    @Test
    fun aHalfTypedPortIsNotWrittenButIsStillShown() = runVmTest {
        // "87" on the way to "8765" must not briefly point the app at port 87.
        val (viewModel, settings) = vm()
        val original = settings.port

        viewModel.setPort("70000")

        assertEquals("70000", viewModel.port.value)
        assertEquals(original, settings.port)
        assertTrue(viewModel.portError.value)
    }

    @Test
    fun aUsablePortIsWrittenImmediately() = runVmTest {
        val (viewModel, settings) = vm()

        viewModel.setPort("9000")

        assertEquals(9000, settings.port)
        assertFalse(viewModel.portError.value)
    }

    @Test
    fun theKeyFieldStaysHiddenUntilSomethingAsksForOne() = runVmTest {
        // Most desktops have no key set, and a field asking for a secret reads as a requirement.
        val (viewModel, _) = vm()
        assertFalse(viewModel.keyRequired.value)

        viewModel.revealKeyField()

        assertTrue(viewModel.keyRequired.value)
    }

    @Test
    fun aKeyAlreadySavedIsShownWithoutBeingAskedFor() = runVmTest {
        val settings = AppSettings(InMemorySettingsStorage()).apply { apiKey = "secret" }

        assertTrue(DesktopAddressViewModel(settings).keyRequired.value)
    }

    @Test
    fun aScannedCodeFillsAllThreeAtOnce() = runVmTest {
        val (viewModel, settings) = vm()

        viewModel.applyScannedUrl("churchpresenter://connect?host=10.1.1.9&port=9100&apikey=zzz")

        assertEquals("10.1.1.9", settings.host)
        assertEquals(9100, settings.port)
        assertEquals("zzz", settings.apiKey)
        // And the fields on screen follow, rather than showing what was there before.
        assertEquals("10.1.1.9", viewModel.host.value)
        assertEquals("9100", viewModel.port.value)
        assertTrue(viewModel.keyRequired.value)
    }

    @Test
    fun somethingThatIsNotAConnectCodeChangesNothing() = runVmTest {
        val (viewModel, settings) = vm()
        val original = settings.host

        viewModel.applyScannedUrl("https://example.com")

        assertEquals(original, settings.host)
    }

    @Test
    fun anAddressChangedElsewhereIsPickedUp() = runVmTest {
        // The Settings sheet and this one write the same fields; a stale draft would send the
        // next copy to the old computer.
        val (viewModel, settings) = vm()
        settings.host = "10.9.9.9"

        viewModel.reloadFromStorage()

        assertEquals("10.9.9.9", viewModel.host.value)
    }

    // ── Writing through only what could be a real address ────────────────
    //
    // This surface has no Save button, so every keystroke reaches settings. An
    // unusable address written through gets picked up by the HTTP client and the
    // socket's reconnect loop, which then retries it forever — the failure this
    // validation exists to prevent.

    @Test
    fun `a usable host is written through as you type`() = runVmTest {
        val (vm, settings) = vm()

        vm.setHost("10.0.0.5")

        assertEquals("10.0.0.5", settings.host)
        assertFalse(vm.hostError.value)
    }

    @Test
    fun `a host that cannot be one is flagged and not written`() = runVmTest {
        // Dictated addresses arrive like this; the old build passed them straight
        // to the URL builder, which threw on every retry.
        val (vm, settings) = vm()
        val before = settings.host

        vm.setHost("high dynamic range")

        assertTrue(vm.hostError.value)
        assertEquals(before, settings.host, "an unusable host must not reach the client")
    }

    @Test
    fun `clearing the host is not an error`() = runVmTest {
        // Mid-edit, the field is briefly empty. Flagging that would put a red
        // border under the operator's cursor as they retype.
        val (vm, _) = vm()
        vm.setHost("nonsense address")
        assertTrue(vm.hostError.value)

        vm.setHost("")

        assertFalse(vm.hostError.value)
    }

    @Test
    fun `a pasted url has its scheme and path stripped before storing`() = runVmTest {
        val (vm, settings) = vm()

        vm.setHost("http://10.0.0.5/some/path")

        assertFalse(vm.hostError.value)
        assertEquals("10.0.0.5", settings.host)
    }

    @Test
    fun `a pasted url that still carries its port is refused`() = runVmTest {
        // normalizeHost strips the scheme and path but not the port, and a colon
        // cannot appear in a bare host — so this reaches the field as an error
        // rather than being silently split into host and port. Documented because
        // pasting the whole address from a browser is the obvious thing to try.
        val (vm, settings) = vm()
        val before = settings.host

        vm.setHost("http://10.0.0.5:8765/")

        assertTrue(vm.hostError.value)
        assertEquals(before, settings.host)
    }

    @Test
    fun `a bracketed IPv6 literal passes validation but is mangled on the way in`() = runVmTest {
        // Documents a real inconsistency rather than an intended behaviour:
        // DesktopAddress.isUsableHost accepts a bracketed IPv6 literal as "the one
        // legitimate use of colons in a host", but AppSettings.host replaces every
        // colon with a dot on write. So the field shows no error and stores an
        // address that cannot resolve. Niche — IPv6 on a church LAN is rare — but
        // the two rules disagree, and this pins which one currently wins.
        val (vm, settings) = vm()

        vm.setHost("[fe80::1]")

        assertFalse(vm.hostError.value, "validation accepts it")
        assertEquals("[fe80..1]", settings.host, "but storage rewrites the colons")
    }

    @Test
    fun `a valid port is written through`() = runVmTest {
        val (vm, settings) = vm()

        vm.setPort("9000")

        assertEquals(9000, settings.port)
        assertFalse(vm.portError.value)
    }

    @Test
    fun `a port outside the range is flagged and not written`() = runVmTest {
        val (vm, settings) = vm()
        val before = settings.port

        for (bad in listOf("0", "65536", "70000", "-1")) {
            vm.setPort(bad)

            assertTrue(vm.portError.value, "port '$bad' should be rejected")
            assertEquals(before, settings.port, "port '$bad' must not be stored")
        }
    }

    @Test
    fun `a non-numeric port is flagged and not written`() = runVmTest {
        val (vm, settings) = vm()
        val before = settings.port

        vm.setPort("abc")

        assertTrue(vm.portError.value)
        assertEquals(before, settings.port)
    }

    @Test
    fun `clearing the port is not an error`() = runVmTest {
        val (vm, _) = vm()
        vm.setPort("abc")
        assertTrue(vm.portError.value)

        vm.setPort("")

        assertFalse(vm.portError.value)
    }

    @Test
    fun `a port with surrounding spaces is still accepted`() = runVmTest {
        // Pasted values carry whitespace routinely.
        val (vm, settings) = vm()

        vm.setPort("  8765  ")

        assertFalse(vm.portError.value)
        assertEquals(8765, settings.port)
    }

    @Test
    fun `both ends of the valid port range are accepted`() = runVmTest {
        val (vm, settings) = vm()

        vm.setPort("1")
        assertFalse(vm.portError.value)
        assertEquals(1, settings.port)

        vm.setPort("65535")
        assertFalse(vm.portError.value)
        assertEquals(65535, settings.port)
    }
}
