package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Input the address fields refuse to write through.
 *
 * Without the guard, every keystroke of a half-typed address reached the HTTP
 * client and the socket's reconnect loop, which then retried it forever.
 */
@OptIn(ExperimentalTestApi::class)
class DesktopAddressValidationTest {

    @Test
    fun aPortThatIsNotANumberIsNotSaved() = runComposeUiTest {
        val settings = settings().apply { port = DEFAULT_PORT }
        showFields(settings)

        type(UiTags.ADDRESS_PORT, "abc")

        assertEquals(DEFAULT_PORT, settings.port)
    }

    @Test
    fun aKeyIsSavedAsItIsTyped() = runComposeUiTest {
        val settings = settings()
        showFields(settings)
        click(UiTags.ADDRESS_REVEAL_KEY)

        type(UiTags.ADDRESS_API_KEY, "secret-key")

        assertEquals("secret-key", settings.apiKey)
    }

    @Test
    fun aKeyIsTrimmedBeforeItIsSaved() = runComposeUiTest {
        // Pasted keys carry trailing whitespace, and the desktop compares the
        // header exactly.
        val settings = settings()
        showFields(settings)
        click(UiTags.ADDRESS_REVEAL_KEY)

        type(UiTags.ADDRESS_API_KEY, "  secret-key  ")

        assertEquals("secret-key", settings.apiKey)
    }

    // ── Bad input ────────────────────────────────────────────────────────

    @Test
    fun anEmptyAddressIsAcceptedAsADraft() = runComposeUiTest {
        // Clearing the field to retype it must not throw an error mid-keystroke.
        showFields()

        type(UiTags.ADDRESS_HOST, "")

        assertTrue(exists(UiTags.ADDRESS_HOST))
    }

    @Test
    fun aPortThatIsNotANumberLeavesTheFieldUsable() = runComposeUiTest {
        showFields()

        type(UiTags.ADDRESS_PORT, "abc")

        assertTrue(exists(UiTags.ADDRESS_PORT))
    }

    @Test
    fun aPortOutsideTheValidRangeLeavesTheFieldUsable() = runComposeUiTest {
        // 70000 is past the top of the range; the field has to stay editable so
        // it can be corrected.
        showFields()

        type(UiTags.ADDRESS_PORT, "70000")

        assertTrue(exists(UiTags.ADDRESS_PORT))
    }

    @Test
    fun bothFieldsSurviveEachOtherBeingWrong() = runComposeUiTest {
        showFields()

        type(UiTags.ADDRESS_HOST, "not a host")
        type(UiTags.ADDRESS_PORT, "abc")

        assertTrue(exists(UiTags.ADDRESS_HOST))
        assertTrue(exists(UiTags.ADDRESS_PORT))
    }

    private companion object {
        /** The companion server's default, and what settings start on. */
        const val DEFAULT_PORT = 8765

        /** Any other valid port — only needs to differ from the default. */
        const val ANOTHER_PORT = 9000
    }
}
