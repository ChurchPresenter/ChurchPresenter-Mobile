package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
 * The fields where the operator types the desktop's address.
 *
 * The most consequential form in the app: get it wrong and every tab times out
 * with nothing to say. The API-key field stays hidden until asked for, because
 * most churches do not set one and an empty password box invites a guess.
 */
@OptIn(ExperimentalTestApi::class)
class DesktopAddressFormTest {
    @Test
    fun theAddressAndPortAreBothAskedFor() = runComposeUiTest {
        showFields()

        assertTrue(exists(UiTags.ADDRESS_HOST))
        assertTrue(exists(UiTags.ADDRESS_PORT))
    }

    @Test
    fun theKeyFieldIsHiddenUntilItIsAskedFor() = runComposeUiTest {
        // Most churches run without one; an empty password box invites a guess
        // that then fails every request.
        showFields()

        assertFalse(exists(UiTags.ADDRESS_API_KEY))
        assertTrue(exists(UiTags.ADDRESS_REVEAL_KEY))
    }

    @Test
    fun theKeyFieldAppearsWhenAskedFor() = runComposeUiTest {
        showFields()

        click(UiTags.ADDRESS_REVEAL_KEY)

        assertTrue(exists(UiTags.ADDRESS_API_KEY))
        assertFalse(exists(UiTags.ADDRESS_REVEAL_KEY))
    }

    @Test
    fun theKeyFieldIsAlreadyThereWhenAKeyIsSaved() = runComposeUiTest {
        // Reopening Settings on a church that uses a key must show the key, not
        // a link asking whether they need one.
        val settings = settings().apply { apiKey = "secret-key" }

        showFields(settings)

        assertTrue(exists(UiTags.ADDRESS_API_KEY))
    }

    @Test
    fun theHintIsShownWhenAskedFor() = runComposeUiTest {
        showFields(showHint = true)

        assertTrue(exists(UiTags.ADDRESS_HINT))
    }

    @Test
    fun theHintIsLeftOutWhenTheCallerHasItsOwn() = runComposeUiTest {
        // The first-run screen explains this at length already; repeating it
        // under the fields reads as two different instructions.
        showFields(showHint = false)

        assertFalse(exists(UiTags.ADDRESS_HINT))
    }

    // ── What is already saved ────────────────────────────────────────────
}
