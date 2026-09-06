package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Where the operator types the desktop's address.
 *
 * The single most consequential form in the app: get it wrong and every tab
 * times out with nothing to say. It appears on both the Settings screen and the
 * first-run setup, so it is a shared component rather than part of either.
 *
 * The API-key field is deliberately hidden until asked for — most churches do
 * not set one, and an empty password box invites someone to invent a value that
 * then fails every request.
 */
/**
 * What reaches settings as the address is typed.
 *
 * There is no Save button here — the component is embedded in Settings and in
 * first-run setup, both of which have their own — so a usable value is written
 * through immediately, and an unusable one deliberately is not: without that
 * guard every keystroke of a half-typed address reached the socket's reconnect
 * loop, which then retried it forever.
 */
@OptIn(ExperimentalTestApi::class)
class DesktopAddressSavingTest {
    @Test
    fun aUsableAddressIsSavedAsItIsTyped() = runComposeUiTest {
        // There is no Save button on this surface — it is embedded in Settings
        // and in first-run setup, both of which have their own. A usable
        // address is written through the moment it becomes one.
        val settings = settings()
        showFields(settings)

        type(UiTags.ADDRESS_HOST, "10.0.0.1")

        assertEquals("10.0.0.1", settings.host)
    }

    @Test
    fun anAddressThatCouldNotBeAHostIsNotSaved() = runComposeUiTest {
        // The guard that matters: without it every keystroke of a half-typed
        // address reached the HTTP client and the socket's reconnect loop,
        // which then retried it forever.
        val settings = settings().apply { host = "192.168.1.5" }
        showFields(settings)

        type(UiTags.ADDRESS_HOST, "not a host at all")

        assertEquals("192.168.1.5", settings.host)
    }

    @Test
    fun aColonTypedIntoTheAddressIsNotSavedAsOne() = runComposeUiTest {
        // Someone pasting "192.168.1.5:8765" would otherwise save a host no
        // request can resolve. The port has its own field.
        val settings = settings()
        showFields(settings)

        type(UiTags.ADDRESS_HOST, "192.168.1.5:8765")

        assertFalse(settings.host.contains(':'), "saved as ${settings.host}")
    }

    @Test
    fun aPortInRangeIsSavedAsItIsTyped() = runComposeUiTest {
        val settings = settings()
        showFields(settings)

        type(UiTags.ADDRESS_PORT, ANOTHER_PORT.toString())

        assertEquals(ANOTHER_PORT, settings.port)
    }

    @Test
    fun aPortOutsideTheRangeIsNotSaved() = runComposeUiTest {
        // 70000 is past the top of the range; keeping the previous one means
        // the app stays pointed somewhere that works.
        val settings = settings().apply { port = DEFAULT_PORT }
        showFields(settings)

        type(UiTags.ADDRESS_PORT, "70000")

        assertEquals(DEFAULT_PORT, settings.port)
    }

    private companion object {
        /** The companion server's default, and what settings start on. */
        const val DEFAULT_PORT = 8765

        /** Any other valid port — only needs to differ from the default. */
        const val ANOTHER_PORT = 9000
    }
}
