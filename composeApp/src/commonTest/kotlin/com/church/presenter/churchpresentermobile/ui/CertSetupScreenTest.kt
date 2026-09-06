package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The screen that walks an operator through trusting the desktop's certificate.
 *
 * It is shown once, before anything works, to someone who is usually in a
 * hurry. The failures worth guarding are the ones that strand them: a Skip that
 * does nothing, a Done that also skips, or a fingerprint panel that claims to
 * show a fingerprint it was never given — the one thing on screen an operator
 * is asked to compare against the desktop.
 */
@OptIn(ExperimentalTestApi::class)
class CertSetupScreenTest {

    private fun ComposeUiTest.showCertSetup(
        certFingerprint: String? = null,
        onDone: () -> Unit = {},
        onSkip: () -> Unit = {},
    ) = showScreen {
        CertSetupScreen(certFingerprint = certFingerprint, onDone = onDone, onSkip = onSkip)
    }

    private val fingerprint = "AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89"

    // ── Finishing ────────────────────────────────────────────────────────

    @Test
    fun theDoneButtonReportsDone() = runComposeUiTest {
        var done = false
        showCertSetup(onDone = { done = true })

        click(UiTags.CERT_DONE)

        assertTrue(done)
    }

    @Test
    fun doneIsNotAlsoASkip() = runComposeUiTest {
        // They lead to different places: Done means "I installed it".
        var skipped = false
        showCertSetup(onDone = {}, onSkip = { skipped = true })

        click(UiTags.CERT_DONE)

        assertFalse(skipped)
    }

    @Test
    fun theSkipButtonReportsSkip() = runComposeUiTest {
        var skipped = false
        showCertSetup(onSkip = { skipped = true })

        click(UiTags.CERT_SKIP)

        assertTrue(skipped)
    }

    @Test
    fun skipIsNotAlsoADone() = runComposeUiTest {
        var done = false
        showCertSetup(onDone = { done = true }, onSkip = {})

        click(UiTags.CERT_SKIP)

        assertFalse(done)
    }

    @Test
    fun theSkipInTheTopBarAlsoSkips() = runComposeUiTest {
        // The same escape hatch, reachable without scrolling to the bottom.
        var skipped = false
        showCertSetup(onSkip = { skipped = true })

        click(UiTags.CERT_SKIP_TOP)

        assertTrue(skipped)
    }

    @Test
    fun bothSkipsReachTheSameHandler() = runComposeUiTest {
        var skips = 0
        showCertSetup(onSkip = { skips++ })

        click(UiTags.CERT_SKIP_TOP)
        click(UiTags.CERT_SKIP)

        assertEquals(2, skips)
    }

    @Test
    fun everyExitIsOfferedFromTheStart() = runComposeUiTest {
        // An operator who cannot install a certificate must not be trapped here.
        showCertSetup()

        assertTrue(exists(UiTags.CERT_DONE))
        assertTrue(exists(UiTags.CERT_SKIP))
        assertTrue(exists(UiTags.CERT_SKIP_TOP))
    }

    @Test
    fun nothingIsReportedUntilAButtonIsPressed() = runComposeUiTest {
        var done = false
        var skipped = false
        showCertSetup(onDone = { done = true }, onSkip = { skipped = true })

        assertFalse(done)
        assertFalse(skipped)
    }

    // ── The fingerprint ──────────────────────────────────────────────────

    @Test
    fun theFingerprintIsShownWhenThereIsOne() = runComposeUiTest {
        // The operator compares this against the desktop, character for
        // character, so it is shown verbatim.
        showCertSetup(certFingerprint = fingerprint)

        assertTrue(isShowing(fingerprint))
    }

    @Test
    fun theFingerprintPanelIsShownWhenThereIsOne() = runComposeUiTest {
        showCertSetup(certFingerprint = fingerprint)

        assertTrue(exists(UiTags.CERT_FINGERPRINT))
    }

    @Test
    fun aMissingFingerprintSaysSoRatherThanShowingAnEmptyBox() = runComposeUiTest {
        // An empty panel reads as a fingerprint that is blank, which would send
        // the operator looking for a fault that is not there.
        showCertSetup(certFingerprint = null)

        assertTrue(exists(UiTags.CERT_NO_FINGERPRINT))
    }

    @Test
    fun theTwoFingerprintStatesAreNotShownTogether() = runComposeUiTest {
        showCertSetup(certFingerprint = null)

        assertFalse(exists(UiTags.CERT_FINGERPRINT))
    }

    @Test
    fun theUnavailableNoticeIsGoneOnceAFingerprintArrives() = runComposeUiTest {
        showCertSetup(certFingerprint = fingerprint)

        assertFalse(exists(UiTags.CERT_NO_FINGERPRINT))
    }

    @Test
    fun anEmptyFingerprintIsStillTreatedAsOne() = runComposeUiTest {
        // "" is not null, so the panel is what the screen shows — this pins the
        // current behaviour so a change to it is a deliberate one.
        showCertSetup(certFingerprint = "")

        assertTrue(exists(UiTags.CERT_FINGERPRINT))
        assertFalse(exists(UiTags.CERT_NO_FINGERPRINT))
    }

    @Test
    fun theScreenStillOffersItsExitsWithoutAFingerprint() = runComposeUiTest {
        showCertSetup(certFingerprint = null)

        assertTrue(exists(UiTags.CERT_DONE))
        assertTrue(exists(UiTags.CERT_SKIP))
    }
}
