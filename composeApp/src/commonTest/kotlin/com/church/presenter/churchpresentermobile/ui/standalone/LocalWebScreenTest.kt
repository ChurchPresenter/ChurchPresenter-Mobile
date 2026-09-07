package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.type
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A link put on the audience screen by this phone.
 *
 * Operators paste whatever they have into one box — a bare host, a YouTube
 * link, sometimes something that is not a link at all — so the screen has to
 * sort that out itself rather than asking them to classify it first. The two
 * failures worth catching are a button that silently does nothing for an
 * address it cannot use, and a site that refuses to be framed being reported as
 * if the whole feature were broken.
 */
@OptIn(ExperimentalTestApi::class)
class LocalWebScreenTest {

    // ── What can be projected ────────────────────────────────────────────

    @Test
    fun aLinkGoesOnTheScreen() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalWeb(f.engine)

        type(StandaloneTags.WEB_URL, "https://example.org/notices")
        click(StandaloneTags.WEB_GO_LIVE)

        awaitThat { f.liveMedia == "https://example.org/notices" }
    }

    @Test
    fun aBareHostHasItsSchemeFilledIn() = runComposeUiTest {
        // "youtube.com" is what people type; demanding https:// of them is a
        // question the app can answer itself.
        val f = StandaloneFixture()
        showLocalWeb(f.engine)

        type(StandaloneTags.WEB_URL, "example.org")
        click(StandaloneTags.WEB_GO_LIVE)

        awaitThat { f.liveMedia?.startsWith("https://") == true }
    }

    @Test
    fun aPageIsFramedRatherThanPlayed() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalWeb(f.engine)

        type(StandaloneTags.WEB_URL, "https://example.org/notices")
        click(StandaloneTags.WEB_GO_LIVE)

        awaitThat { f.engine.deck.value.kind == SlideKind.WEB }
    }

    @Test
    fun aVideoLinkIsPlayedRatherThanFramed() = runComposeUiTest {
        // Framing an .mp4 gives a download prompt on the wall.
        val f = StandaloneFixture()
        showLocalWeb(f.engine)

        type(StandaloneTags.WEB_URL, "https://example.org/sermon.mp4")
        click(StandaloneTags.WEB_GO_LIVE)

        awaitThat { f.engine.deck.value.kind == SlideKind.VIDEO }
    }

    @Test
    fun theLiveLinkIsShownBackToTheOperator() = runComposeUiTest {
        // So they can see what is on the wall without looking at the wall.
        val f = StandaloneFixture()
        showLocalWeb(f.engine)

        type(StandaloneTags.WEB_URL, "https://example.org/notices")
        click(StandaloneTags.WEB_GO_LIVE)

        awaitThat { exists(StandaloneTags.WEB_LIVE_URL) }
    }

    @Test
    fun nothingIsLiveBeforeAPress() = runComposeUiTest {
        val f = StandaloneFixture()

        showLocalWeb(f.engine)

        assertFalse(exists(StandaloneTags.WEB_LIVE_URL))
    }

    @Test
    fun typingAloneProjectsNothing() = runComposeUiTest {
        // Going live is a press, not a keystroke.
        val f = StandaloneFixture()
        showLocalWeb(f.engine)

        type(StandaloneTags.WEB_URL, "https://example.org/notices")

        assertTrue(f.engine.deck.value.isEmpty)
    }

    // ── What cannot ──────────────────────────────────────────────────────

    @Test
    fun anAddressTheScreenCannotOpenIsRefusedOutLoud() = runComposeUiTest {
        // The button did nothing at all, which is indistinguishable from the
        // feature being broken.
        val f = StandaloneFixture()
        showLocalWeb(f.engine)

        type(StandaloneTags.WEB_URL, "mailto:pastor@example.org")

        awaitThat { exists(StandaloneTags.WEB_NOT_A_LINK) }
    }

    @Test
    fun anAddressTheScreenCannotOpenIsNotProjected() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalWeb(f.engine)

        type(StandaloneTags.WEB_URL, "mailto:pastor@example.org")
        click(StandaloneTags.WEB_GO_LIVE)

        assertTrue(f.engine.deck.value.isEmpty)
    }

    @Test
    fun aBareWordIsTreatedAsAHostRatherThanRefused() = runComposeUiTest {
        // Operators type "youtube.com"; adding https:// to something schemeless
        // cannot turn it into code or a file path, so it is filled in rather
        // than refused.
        val f = StandaloneFixture()
        showLocalWeb(f.engine)

        type(StandaloneTags.WEB_URL, "example.org")

        awaitThat { !exists(StandaloneTags.WEB_NOT_A_LINK) }
    }

    @Test
    fun anEmptyBoxIsNotAComplaint() = runComposeUiTest {
        // Nothing typed yet is not a mistake.
        val f = StandaloneFixture()

        showLocalWeb(f.engine)

        assertFalse(exists(StandaloneTags.WEB_NOT_A_LINK))
    }

    @Test
    fun clearingTheBoxTakesTheComplaintAway() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalWeb(f.engine)
        type(StandaloneTags.WEB_URL, "mailto:pastor@example.org")
        awaitThat { exists(StandaloneTags.WEB_NOT_A_LINK) }

        type(StandaloneTags.WEB_URL, "")

        awaitThat { !exists(StandaloneTags.WEB_NOT_A_LINK) }
    }

    @Test
    fun fixingTheLinkTakesTheComplaintAway() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalWeb(f.engine)
        type(StandaloneTags.WEB_URL, "mailto:pastor@example.org")
        awaitThat { exists(StandaloneTags.WEB_NOT_A_LINK) }

        type(StandaloneTags.WEB_URL, "https://example.org")

        awaitThat { !exists(StandaloneTags.WEB_NOT_A_LINK) }
    }

    @Test
    fun aFileSchemeIsRefused() = runComposeUiTest {
        // The address reaches an embedded browser, where this is file access
        // rather than a slide.
        val f = StandaloneFixture()
        showLocalWeb(f.engine)

        type(StandaloneTags.WEB_URL, "file:///etc/passwd")
        click(StandaloneTags.WEB_GO_LIVE)

        assertTrue(f.engine.deck.value.isEmpty)
    }

    @Test
    fun aJavascriptSchemeIsRefused() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalWeb(f.engine)

        type(StandaloneTags.WEB_URL, "javascript:alert(1)")
        click(StandaloneTags.WEB_GO_LIVE)

        assertTrue(f.engine.deck.value.isEmpty)
    }

    @Test
    fun aRefusedSchemeIsCalledOut() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalWeb(f.engine)

        type(StandaloneTags.WEB_URL, "javascript:alert(1)")

        awaitThat { exists(StandaloneTags.WEB_NOT_A_LINK) }
    }

    @Test
    fun aPlainHttpLinkIsAllowed() = runComposeUiTest {
        // A church running its own notice board on the local network.
        val f = StandaloneFixture()
        showLocalWeb(f.engine)

        type(StandaloneTags.WEB_URL, "http://192.168.1.10:8080/notices")
        click(StandaloneTags.WEB_GO_LIVE)

        awaitThat { f.liveMedia == "http://192.168.1.10:8080/notices" }
    }

    // ── Taking it off again ──────────────────────────────────────────────

    @Test
    fun aLiveLinkOffersAWayToClearIt() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalWeb(f.engine)

        type(StandaloneTags.WEB_URL, "https://example.org")
        click(StandaloneTags.WEB_GO_LIVE)

        awaitThat { exists(StandaloneTags.WEB_CLEAR) }
    }

    @Test
    fun nothingLiveOffersNoClear() = runComposeUiTest {
        val f = StandaloneFixture()

        showLocalWeb(f.engine)

        assertFalse(exists(StandaloneTags.WEB_CLEAR))
    }

    @Test
    fun clearingTakesThePageOffTheScreen() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalWeb(f.engine)
        type(StandaloneTags.WEB_URL, "https://example.org")
        click(StandaloneTags.WEB_GO_LIVE)
        awaitThat { exists(StandaloneTags.WEB_CLEAR) }

        click(StandaloneTags.WEB_CLEAR)

        awaitThat { f.engine.deck.value.isEmpty }
    }

    @Test
    fun clearingTakesTheLiveLineAway() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalWeb(f.engine)
        type(StandaloneTags.WEB_URL, "https://example.org")
        click(StandaloneTags.WEB_GO_LIVE)
        awaitThat { exists(StandaloneTags.WEB_LIVE_URL) }

        click(StandaloneTags.WEB_CLEAR)

        awaitThat { !exists(StandaloneTags.WEB_LIVE_URL) }
    }

    @Test
    fun clearingLeavesTheTypedLinkAlone() = runComposeUiTest {
        // Clearing the wall is not the same as clearing the box; retyping the
        // address mid-service is exactly what nobody has time for.
        val f = StandaloneFixture()
        showLocalWeb(f.engine)
        type(StandaloneTags.WEB_URL, "https://example.org")
        click(StandaloneTags.WEB_GO_LIVE)
        awaitThat { exists(StandaloneTags.WEB_CLEAR) }

        click(StandaloneTags.WEB_CLEAR)

        awaitThat { exists(StandaloneTags.WEB_GO_LIVE) }
    }

    @Test
    fun aLinkCanBeProjectedAgainAfterClearing() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalWeb(f.engine)
        type(StandaloneTags.WEB_URL, "https://example.org")
        click(StandaloneTags.WEB_GO_LIVE)
        awaitThat { exists(StandaloneTags.WEB_CLEAR) }
        click(StandaloneTags.WEB_CLEAR)
        awaitThat { f.engine.deck.value.isEmpty }

        click(StandaloneTags.WEB_GO_LIVE)

        awaitThat { f.liveMedia == "https://example.org" }
    }

    @Test
    fun projectingASecondLinkReplacesTheFirst() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalWeb(f.engine)
        type(StandaloneTags.WEB_URL, "https://example.org")
        click(StandaloneTags.WEB_GO_LIVE)
        awaitThat { f.liveMedia == "https://example.org" }

        type(StandaloneTags.WEB_URL, "https://example.com/other")
        click(StandaloneTags.WEB_GO_LIVE)

        awaitThat { f.liveMedia == "https://example.com/other" }
    }

    // ── A site that will not be framed ───────────────────────────────────

    @Test
    fun aSiteThatRefusesFramingIsReported() = runComposeUiTest {
        // Nothing here can override it — the browser enforces it on the site's
        // behalf — so the operator is told rather than left with an error on the
        // wall.
        val f = StandaloneFixture()
        showLocalWeb(f.engine, refusesFraming = { true })

        type(StandaloneTags.WEB_URL, "https://example.org/notices")
        click(StandaloneTags.WEB_GO_LIVE)

        awaitThat { exists(StandaloneTags.WEB_REFUSES_FRAMING) }
    }

    @Test
    fun aSiteThatRefusesFramingIsStillProjected() = runComposeUiTest {
        // A screen attached to this phone loads the address as a page in its own
        // right, so the site's rule about framing does not apply to it.
        val f = StandaloneFixture()
        showLocalWeb(f.engine, refusesFraming = { true })

        type(StandaloneTags.WEB_URL, "https://example.org/notices")
        click(StandaloneTags.WEB_GO_LIVE)

        awaitThat { f.liveMedia == "https://example.org/notices" }
    }

    @Test
    fun aSiteThatAllowsFramingIsNotComplainedAbout() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalWeb(f.engine, refusesFraming = { false })

        type(StandaloneTags.WEB_URL, "https://example.org/notices")
        click(StandaloneTags.WEB_GO_LIVE)

        awaitThat { exists(StandaloneTags.WEB_LIVE_URL) }
        assertFalse(exists(StandaloneTags.WEB_REFUSES_FRAMING))
    }

    @Test
    fun aVideoIsNeverAskedAboutFraming() = runComposeUiTest {
        // It is played rather than framed, so the question does not arise.
        val f = StandaloneFixture()
        showLocalWeb(f.engine, refusesFraming = { true })

        type(StandaloneTags.WEB_URL, "https://example.org/sermon.mp4")
        click(StandaloneTags.WEB_GO_LIVE)

        awaitThat { f.engine.deck.value.kind == SlideKind.VIDEO }
        assertFalse(exists(StandaloneTags.WEB_REFUSES_FRAMING))
    }

    @Test
    fun typingANewLinkTakesTheFramingWarningAway() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalWeb(f.engine, refusesFraming = { true })
        type(StandaloneTags.WEB_URL, "https://example.org/notices")
        click(StandaloneTags.WEB_GO_LIVE)
        awaitThat { exists(StandaloneTags.WEB_REFUSES_FRAMING) }

        type(StandaloneTags.WEB_URL, "https://example.com/other")

        awaitThat { !exists(StandaloneTags.WEB_REFUSES_FRAMING) }
    }

    @Test
    fun clearingTakesTheFramingWarningAway() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalWeb(f.engine, refusesFraming = { true })
        type(StandaloneTags.WEB_URL, "https://example.org/notices")
        click(StandaloneTags.WEB_GO_LIVE)
        awaitThat { exists(StandaloneTags.WEB_REFUSES_FRAMING) }

        click(StandaloneTags.WEB_CLEAR)

        awaitThat { !exists(StandaloneTags.WEB_REFUSES_FRAMING) }
    }

    @Test
    fun aFramingRefusalForALinkNoLongerLiveIsNotShown() = runComposeUiTest {
        // The answer comes back after the fact; by then the operator may have
        // moved on, and a warning about a page nobody is showing is noise.
        val f = StandaloneFixture()
        showLocalWeb(f.engine, refusesFraming = { url -> url.contains("slow") })
        type(StandaloneTags.WEB_URL, "https://example.org/slow")
        click(StandaloneTags.WEB_GO_LIVE)

        type(StandaloneTags.WEB_URL, "https://example.com/other")
        click(StandaloneTags.WEB_GO_LIVE)

        awaitThat { f.liveMedia == "https://example.com/other" }
        assertFalse(exists(StandaloneTags.WEB_REFUSES_FRAMING))
    }

    // ── With no output, or no presenter ──────────────────────────────────

    @Test
    fun havingNoOutputIsSaidPlainly() = runComposeUiTest {
        val f = StandaloneFixture()

        showLocalWeb(f.engine, hasOutput = false)

        assertTrue(exists(StandaloneTags.WEB_NO_OUTPUT))
    }

    @Test
    fun havingAnOutputIsNotNaggedAbout() = runComposeUiTest {
        val f = StandaloneFixture()

        showLocalWeb(f.engine, hasOutput = true)

        assertFalse(exists(StandaloneTags.WEB_NO_OUTPUT))
    }

    @Test
    fun havingNoOutputStillAllowsTyping() = runComposeUiTest {
        // The screen may be connected a moment later.
        val f = StandaloneFixture()
        showLocalWeb(f.engine, hasOutput = false)

        type(StandaloneTags.WEB_URL, "https://example.org")

        assertFalse(exists(StandaloneTags.WEB_NOT_A_LINK))
    }

    @Test
    fun withoutAPresenterTheScreenStillOpens() = runComposeUiTest {
        showLocalWeb(engine = null)

        assertTrue(exists(StandaloneTags.WEB_URL))
    }

    @Test
    fun withoutAPresenterGoingLiveIsHarmless() = runComposeUiTest {
        showLocalWeb(engine = null)

        type(StandaloneTags.WEB_URL, "https://example.org")
        click(StandaloneTags.WEB_GO_LIVE)

        assertFalse(exists(StandaloneTags.WEB_LIVE_URL))
    }

    @Test
    fun theTypedLinkIsKeptWhileTheScreenIsOpen() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalWeb(f.engine)

        type(StandaloneTags.WEB_URL, "https://example.org")
        click(StandaloneTags.WEB_GO_LIVE)

        awaitThat { exists(StandaloneTags.WEB_LIVE_URL) }
        assertEquals("https://example.org", f.liveMedia)
    }
}
