package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The first-run screen that points the app at a desktop.
 *
 * Nothing in the app works until this succeeds, and it is used once, by someone
 * who has not seen it before. The failures worth guarding are the ones that
 * leave a wrong address saved: a port outside the valid range accepted, a blank
 * host accepted, or an entry that reports success while writing nothing.
 *
 * There is no camera on this runtime, so [hasCameraAvailable] is false and the
 * screen opens straight onto manual entry — which is the path this covers.
 */
@OptIn(ExperimentalTestApi::class)
class ConnectSetupScreenTest {

    private fun settings() = AppSettings(InMemorySettingsStorage())

    private fun ComposeUiTest.showConnect(
        appSettings: AppSettings = settings(),
        onDone: () -> Unit = {},
        onSkip: () -> Unit = {},
    ) = showScreen {
        ConnectSetupScreen(appSettings = appSettings, onDone = onDone, onSkip = onSkip)
    }

    // ── Manual entry is the path without a camera ────────────────────────

    @Test
    fun manualEntryIsOfferedWhenThereIsNoCamera() = runComposeUiTest {
        showConnect()

        assertTrue(exists(UiTags.CONNECT_HOST))
        assertTrue(exists(UiTags.CONNECT_PORT))
    }

    @Test
    fun theReasonManualEntryIsShowingIsExplained() = runComposeUiTest {
        // Otherwise the missing scan button reads as a broken screen.
        showConnect()

        assertTrue(exists(UiTags.CONNECT_NO_CAMERA))
    }

    @Test
    fun theApiKeyFieldIsOffered() = runComposeUiTest {
        showConnect()

        assertTrue(exists(UiTags.CONNECT_API_KEY))
    }

    @Test
    fun theFieldsStartFromTheSavedSettings() = runComposeUiTest {
        val settings = settings().apply { host = "192.168.1.50"; port = 9001 }

        showConnect(settings)

        assertTrue(isShowing("192.168.1.50"))
        assertTrue(isShowing("9001"))
    }

    // ── Applying a good address ──────────────────────────────────────────

    @Test
    fun applyingAValidAddressSavesTheHost() = runComposeUiTest {
        val settings = settings()
        showConnect(settings)

        type(UiTags.CONNECT_HOST, "10.0.0.7")
        type(UiTags.CONNECT_PORT, "8765")
        click(UiTags.CONNECT_APPLY)

        assertEquals("10.0.0.7", settings.host)
    }

    @Test
    fun applyingAValidAddressSavesThePort() = runComposeUiTest {
        val settings = settings()
        showConnect(settings)

        type(UiTags.CONNECT_HOST, "10.0.0.7")
        type(UiTags.CONNECT_PORT, "9100")
        click(UiTags.CONNECT_APPLY)

        assertEquals(9100, settings.port)
    }

    @Test
    fun applyingSavesTheApiKey() = runComposeUiTest {
        val settings = settings()
        showConnect(settings)

        type(UiTags.CONNECT_HOST, "10.0.0.7")
        type(UiTags.CONNECT_PORT, "8765")
        type(UiTags.CONNECT_API_KEY, "let-me-in")
        click(UiTags.CONNECT_APPLY)

        assertEquals("let-me-in", settings.apiKey)
    }

    @Test
    fun surroundingSpaceIsTrimmedFromTheHost() = runComposeUiTest {
        // Phone keyboards add a trailing space readily, and it makes the
        // address unusable in a way that is invisible on screen.
        val settings = settings()
        showConnect(settings)

        type(UiTags.CONNECT_HOST, "  10.0.0.7  ")
        type(UiTags.CONNECT_PORT, "8765")
        click(UiTags.CONNECT_APPLY)

        assertEquals("10.0.0.7", settings.host)
    }

    @Test
    fun surroundingSpaceIsTrimmedFromTheApiKey() = runComposeUiTest {
        val settings = settings()
        showConnect(settings)

        type(UiTags.CONNECT_HOST, "10.0.0.7")
        type(UiTags.CONNECT_PORT, "8765")
        type(UiTags.CONNECT_API_KEY, "  key  ")
        click(UiTags.CONNECT_APPLY)

        assertEquals("key", settings.apiKey)
    }

    @Test
    fun aSuccessfulApplyIsConfirmedOnScreen() = runComposeUiTest {
        showConnect()

        type(UiTags.CONNECT_HOST, "10.0.0.7")
        type(UiTags.CONNECT_PORT, "8765")
        click(UiTags.CONNECT_APPLY)

        assertTrue(exists(UiTags.CONNECT_CONNECTED))
    }

    @Test
    fun nothingIsConfirmedBeforeAnythingIsApplied() = runComposeUiTest {
        showConnect()

        assertFalse(exists(UiTags.CONNECT_CONNECTED))
    }

    // ── Refusing a bad address ───────────────────────────────────────────

    @Test
    fun aBlankHostIsNotSaved() = runComposeUiTest {
        val settings = settings().apply { host = "10.0.0.1" }
        showConnect(settings)

        type(UiTags.CONNECT_HOST, "   ")
        type(UiTags.CONNECT_PORT, "8765")
        click(UiTags.CONNECT_APPLY)

        assertEquals("10.0.0.1", settings.host)
    }

    @Test
    fun aBlankHostIsNotReportedAsConnected() = runComposeUiTest {
        showConnect()

        type(UiTags.CONNECT_HOST, "")
        type(UiTags.CONNECT_PORT, "8765")
        click(UiTags.CONNECT_APPLY)

        assertFalse(exists(UiTags.CONNECT_CONNECTED))
    }

    @Test
    fun aPortThatIsNotANumberIsNotSaved() = runComposeUiTest {
        val settings = settings().apply { port = 8765 }
        showConnect(settings)

        type(UiTags.CONNECT_HOST, "10.0.0.7")
        type(UiTags.CONNECT_PORT, "not-a-port")
        click(UiTags.CONNECT_APPLY)

        assertEquals(8765, settings.port)
    }

    @Test
    fun aPortAboveTheValidRangeIsNotSaved() = runComposeUiTest {
        // 65535 is the last usable port; anything past it cannot be bound.
        val settings = settings().apply { port = 8765 }
        showConnect(settings)

        type(UiTags.CONNECT_HOST, "10.0.0.7")
        type(UiTags.CONNECT_PORT, "65536")
        click(UiTags.CONNECT_APPLY)

        assertEquals(8765, settings.port)
    }

    @Test
    fun portZeroIsNotSaved() = runComposeUiTest {
        val settings = settings().apply { port = 8765 }
        showConnect(settings)

        type(UiTags.CONNECT_HOST, "10.0.0.7")
        type(UiTags.CONNECT_PORT, "0")
        click(UiTags.CONNECT_APPLY)

        assertEquals(8765, settings.port)
    }

    @Test
    fun aNegativePortIsNotSaved() = runComposeUiTest {
        val settings = settings().apply { port = 8765 }
        showConnect(settings)

        type(UiTags.CONNECT_HOST, "10.0.0.7")
        type(UiTags.CONNECT_PORT, "-1")
        click(UiTags.CONNECT_APPLY)

        assertEquals(8765, settings.port)
    }

    @Test
    fun theHighestValidPortIsAccepted() = runComposeUiTest {
        val settings = settings()
        showConnect(settings)

        type(UiTags.CONNECT_HOST, "10.0.0.7")
        type(UiTags.CONNECT_PORT, "65535")
        click(UiTags.CONNECT_APPLY)

        assertEquals(65535, settings.port)
    }

    @Test
    fun theLowestValidPortIsAccepted() = runComposeUiTest {
        val settings = settings()
        showConnect(settings)

        type(UiTags.CONNECT_HOST, "10.0.0.7")
        type(UiTags.CONNECT_PORT, "1")
        click(UiTags.CONNECT_APPLY)

        assertEquals(1, settings.port)
    }

    @Test
    fun aBadPortLeavesTheHostUnsavedToo() = runComposeUiTest {
        // The address is applied as one thing — half of it is not an address.
        val settings = settings().apply { host = "10.0.0.1" }
        showConnect(settings)

        type(UiTags.CONNECT_HOST, "10.0.0.99")
        type(UiTags.CONNECT_PORT, "99999")
        click(UiTags.CONNECT_APPLY)

        assertEquals("10.0.0.1", settings.host)
    }

    @Test
    fun aBadPortDoesNotSaveTheApiKeyEither() = runComposeUiTest {
        val settings = settings().apply { apiKey = "original" }
        showConnect(settings)

        type(UiTags.CONNECT_HOST, "10.0.0.7")
        type(UiTags.CONNECT_PORT, "abc")
        type(UiTags.CONNECT_API_KEY, "new-key")
        click(UiTags.CONNECT_APPLY)

        assertEquals("original", settings.apiKey)
    }

    // ── Leaving the screen ───────────────────────────────────────────────

    @Test
    fun theDoneButtonReportsDone() = runComposeUiTest {
        var done = false
        showConnect(onDone = { done = true })

        click(UiTags.CONNECT_DONE)

        assertTrue(done)
    }

    @Test
    fun theSkipButtonReportsSkip() = runComposeUiTest {
        var skipped = false
        showConnect(onSkip = { skipped = true })

        click(UiTags.CONNECT_SKIP)

        assertTrue(skipped)
    }

    @Test
    fun theSkipInTheTopBarAlsoSkips() = runComposeUiTest {
        var skipped = false
        showConnect(onSkip = { skipped = true })

        click(UiTags.CONNECT_SKIP_TOP)

        assertTrue(skipped)
    }

    @Test
    fun doneIsNotAlsoASkip() = runComposeUiTest {
        var skipped = false
        showConnect(onDone = {}, onSkip = { skipped = true })

        click(UiTags.CONNECT_DONE)

        assertFalse(skipped)
    }

    @Test
    fun skippingDoesNotSaveAnAddress() = runComposeUiTest {
        // Skip means "carry on without a desktop", not "save what I typed".
        val settings = settings().apply { host = "10.0.0.1" }
        showConnect(settings, onSkip = {})

        type(UiTags.CONNECT_HOST, "10.0.0.99")
        click(UiTags.CONNECT_SKIP)

        assertNotEquals("10.0.0.99", settings.host)
    }

    @Test
    fun finishingDoesNotSaveAnUnappliedAddress() = runComposeUiTest {
        // Typing is not applying — the operator has to press Apply.
        val settings = settings().apply { host = "10.0.0.1" }
        showConnect(settings, onDone = {})

        type(UiTags.CONNECT_HOST, "10.0.0.99")
        click(UiTags.CONNECT_DONE)

        assertEquals("10.0.0.1", settings.host)
    }
}
