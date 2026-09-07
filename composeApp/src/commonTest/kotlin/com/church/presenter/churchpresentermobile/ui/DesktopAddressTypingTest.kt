package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Typing into the desktop-address fields, and what is shown back.
 */
@OptIn(ExperimentalTestApi::class)
class DesktopAddressTypingTest {

    @Test
    fun theSavedAddressIsShown() = runComposeUiTest {
        val settings = settings().apply { host = "192.168.1.5" }

        showFields(settings)

        assertTrue(isShowing("192.168.1.5"))
    }

    @Test
    fun theSavedPortIsShown() = runComposeUiTest {
        val settings = settings().apply { port = ANOTHER_PORT }

        showFields(settings)

        assertTrue(isShowing("9000"))
    }

    @Test
    fun theSavedKeyIsShown() = runComposeUiTest {
        val settings = settings().apply { apiKey = "secret-key" }

        showFields(settings)

        assertTrue(isShowing("secret-key"))
    }

    // ── Typing ───────────────────────────────────────────────────────────

    @Test
    fun typingAnAddressIsKept() = runComposeUiTest {
        showFields()

        type(UiTags.ADDRESS_HOST, "192.168.1.9")

        assertTrue(isShowing("192.168.1.9"))
    }

    @Test
    fun typingAPortIsKept() = runComposeUiTest {
        showFields()

        type(UiTags.ADDRESS_PORT, "8765")

        assertTrue(isShowing("8765"))
    }

    @Test
    fun typingAKeyIsKept() = runComposeUiTest {
        showFields()
        click(UiTags.ADDRESS_REVEAL_KEY)

        type(UiTags.ADDRESS_API_KEY, "secret-key")

        assertTrue(isShowing("secret-key"))
    }

    private companion object {
        /** The companion server's default, and what settings start on. */
        const val DEFAULT_PORT = 8765

        /** Any other valid port — only needs to differ from the default. */
        const val ANOTHER_PORT = 9000
    }
}
