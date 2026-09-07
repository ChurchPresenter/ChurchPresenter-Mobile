package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.present.SinkState
import com.church.presenter.churchpresentermobile.present.SinkStatus
import com.church.presenter.churchpresentermobile.present.sink.EXTERNAL_DISPLAY_SINK_ID
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.isShowing
import com.church.presenter.churchpresentermobile.ui.showScreen
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The list of screens this phone can project onto.
 *
 * Sinks are listed even when nothing is connected: an "external display,
 * waiting for a screen" row says the feature exists and what to do next, while
 * an empty list reads as "not supported on this phone". The two things the list
 * has to place correctly are the browser address — only under the row that is
 * actually serving one — and the mirroring instructions, which belong under the
 * unattached external display and nowhere else.
 */
@OptIn(ExperimentalTestApi::class)
class OutputTargetsSheetTest {

    private fun browser(
        state: SinkState = SinkState.ATTACHED,
        detail: String? = "http://192.168.1.50:8080/display",
    ) = SinkStatus(id = "browser", displayName = "Browser display", state = state, detail = detail)

    private fun external(
        state: SinkState = SinkState.ATTACHING,
        detail: String? = null,
    ) = SinkStatus(
        id = EXTERNAL_DISPLAY_SINK_ID,
        displayName = "External display",
        state = state,
        detail = detail,
    )

    // ── Nothing to project onto ──────────────────────────────────────────

    @Test
    fun noSinksAtAllSaysSo() = runComposeUiTest {
        showScreen { OutputTargetsContent(emptyList()) }

        assertTrue(exists(StandaloneTags.OUTPUTS_EMPTY))
    }

    @Test
    fun noSinksShowsNoRows() = runComposeUiTest {
        showScreen { OutputTargetsContent(emptyList()) }

        assertFalse(exists(StandaloneTags.sink("browser")))
    }

    @Test
    fun noSinksShowsNoAddress() = runComposeUiTest {
        showScreen { OutputTargetsContent(emptyList()) }

        assertFalse(exists(StandaloneTags.sinkUrl("browser")))
    }

    // ── The rows ─────────────────────────────────────────────────────────

    @Test
    fun aSinkIsListed() = runComposeUiTest {
        showScreen { OutputTargetsContent(listOf(browser())) }

        assertTrue(exists(StandaloneTags.sink("browser")))
    }

    @Test
    fun everySinkIsListed() = runComposeUiTest {
        showScreen { OutputTargetsContent(listOf(browser(), external())) }

        assertTrue(exists(StandaloneTags.sink("browser")))
        assertTrue(exists(StandaloneTags.sink(EXTERNAL_DISPLAY_SINK_ID)))
    }

    @Test
    fun aSinkIsNamed() = runComposeUiTest {
        showScreen { OutputTargetsContent(listOf(browser())) }

        assertTrue(isShowing("Browser display"))
    }

    @Test
    fun aDisconnectedSinkIsStillListed() = runComposeUiTest {
        // An empty list reads as "not supported on this phone".
        showScreen { OutputTargetsContent(listOf(external(state = SinkState.DETACHED))) }

        assertTrue(exists(StandaloneTags.sink(EXTERNAL_DISPLAY_SINK_ID)))
    }

    @Test
    fun aSinkThatIsSearchingIsStillListed() = runComposeUiTest {
        showScreen { OutputTargetsContent(listOf(external(state = SinkState.ATTACHING))) }

        assertTrue(exists(StandaloneTags.sink(EXTERNAL_DISPLAY_SINK_ID)))
    }

    @Test
    fun aSinkInErrorIsStillListed() = runComposeUiTest {
        // Hiding a failed screen is how an operator ends up not knowing why the
        // wall is blank.
        showScreen { OutputTargetsContent(listOf(browser(state = SinkState.ERROR))) }

        assertTrue(exists(StandaloneTags.sink("browser")))
    }

    @Test
    fun aSinksDetailIsShownAlongsideIt() = runComposeUiTest {
        showScreen {
            OutputTargetsContent(listOf(external(state = SinkState.ATTACHED, detail = "1920×1080")))
        }

        assertTrue(isShowing("1920×1080"))
    }

    @Test
    fun aSinkWithNoDetailIsStillListed() = runComposeUiTest {
        showScreen { OutputTargetsContent(listOf(external(detail = null))) }

        assertTrue(exists(StandaloneTags.sink(EXTERNAL_DISPLAY_SINK_ID)))
    }

    @Test
    fun aBlankDetailIsNotShownAsAnEmptyLine() = runComposeUiTest {
        showScreen { OutputTargetsContent(listOf(external(detail = "  "))) }

        assertTrue(exists(StandaloneTags.sink(EXTERNAL_DISPLAY_SINK_ID)))
    }

    @Test
    fun severalSinksEachKeepTheirOwnRow() = runComposeUiTest {
        // A row that carries another sink's state is the failure worth catching.
        showScreen {
            OutputTargetsContent(
                listOf(
                    browser(state = SinkState.ATTACHED),
                    external(state = SinkState.DETACHED),
                )
            )
        }

        assertTrue(exists(StandaloneTags.sink("browser")))
        assertTrue(exists(StandaloneTags.sink(EXTERNAL_DISPLAY_SINK_ID)))
    }

    // ── The browser address ──────────────────────────────────────────────

    @Test
    fun anAttachedBrowserSinkShowsItsAddress() = runComposeUiTest {
        // It is the whole point of that sink: someone has to type it in.
        showScreen { OutputTargetsContent(listOf(browser())) }

        assertTrue(exists(StandaloneTags.sinkUrl("browser")))
    }

    @Test
    fun theAddressIsShownInFull() = runComposeUiTest {
        showScreen { OutputTargetsContent(listOf(browser())) }

        assertTrue(isShowing("http://192.168.1.50:8080/display"))
    }

    @Test
    fun anUnattachedBrowserSinkShowsNoAddress() = runComposeUiTest {
        // There is nothing serving it yet, and an address that answers nothing
        // is worse than none.
        showScreen {
            OutputTargetsContent(listOf(browser(state = SinkState.DETACHED)))
        }

        assertFalse(exists(StandaloneTags.sinkUrl("browser")))
    }

    @Test
    fun aSinkStillSearchingShowsNoAddress() = runComposeUiTest {
        showScreen { OutputTargetsContent(listOf(browser(state = SinkState.ATTACHING))) }

        assertFalse(exists(StandaloneTags.sinkUrl("browser")))
    }

    @Test
    fun aFailedSinkShowsNoAddress() = runComposeUiTest {
        showScreen { OutputTargetsContent(listOf(browser(state = SinkState.ERROR))) }

        assertFalse(exists(StandaloneTags.sinkUrl("browser")))
    }

    @Test
    fun aDetailThatIsNotAnAddressIsNotOfferedAsOne() = runComposeUiTest {
        // The external display's detail is a resolution, not something to type
        // into a browser.
        showScreen {
            OutputTargetsContent(listOf(external(state = SinkState.ATTACHED, detail = "1920×1080")))
        }

        assertFalse(exists(StandaloneTags.sinkUrl(EXTERNAL_DISPLAY_SINK_ID)))
    }

    @Test
    fun aSinkWithNoDetailOffersNoAddress() = runComposeUiTest {
        showScreen { OutputTargetsContent(listOf(browser(detail = null))) }

        assertFalse(exists(StandaloneTags.sinkUrl("browser")))
    }

    @Test
    fun theAddressBelongsToItsOwnRow() = runComposeUiTest {
        // Two sinks, one address: it must be the serving one's.
        showScreen {
            OutputTargetsContent(
                listOf(browser(), external(state = SinkState.ATTACHED, detail = "1920×1080"))
            )
        }

        assertTrue(exists(StandaloneTags.sinkUrl("browser")))
        assertFalse(exists(StandaloneTags.sinkUrl(EXTERNAL_DISPLAY_SINK_ID)))
    }

    @Test
    fun anHttpsDetailIsNotTreatedAsTheDisplayAddress() = runComposeUiTest {
        // The phone serves plain http on the local network; anything else is
        // some other kind of detail.
        showScreen {
            OutputTargetsContent(listOf(browser(detail = "https://example.org")))
        }

        assertFalse(exists(StandaloneTags.sinkUrl("browser")))
    }

    // ── Getting a TV attached ────────────────────────────────────────────

    @Test
    fun anUnattachedExternalDisplayIsListedAsWaiting() = runComposeUiTest {
        showScreen { OutputTargetsContent(listOf(external(state = SinkState.ATTACHING))) }

        assertTrue(exists(StandaloneTags.sink(EXTERNAL_DISPLAY_SINK_ID)))
    }

    @Test
    fun anAttachedExternalDisplayNeedsNoInstructions() = runComposeUiTest {
        // Once the TV is showing the slide, instructions for connecting it are
        // noise.
        showScreen {
            OutputTargetsContent(listOf(external(state = SinkState.ATTACHED, detail = "1920×1080")))
        }

        assertFalse(exists(StandaloneTags.OUTPUTS_GUIDANCE))
    }

    @Test
    fun theBrowserSinkNeverCarriesMirroringInstructions() = runComposeUiTest {
        // It is reached by typing an address, not through Control Centre.
        showScreen { OutputTargetsContent(listOf(browser(state = SinkState.DETACHED))) }

        assertFalse(exists(StandaloneTags.OUTPUTS_GUIDANCE))
    }

    @Test
    fun anAttachedListNeverCarriesMirroringInstructions() = runComposeUiTest {
        showScreen {
            OutputTargetsContent(
                listOf(browser(), external(state = SinkState.ATTACHED, detail = "1920×1080"))
            )
        }

        assertFalse(exists(StandaloneTags.OUTPUTS_GUIDANCE))
    }

    @Test
    fun theListSurvivesASinkWithNoName() = runComposeUiTest {
        showScreen {
            OutputTargetsContent(listOf(SinkStatus(id = "odd", displayName = "")))
        }

        assertTrue(exists(StandaloneTags.sink("odd")))
    }

    @Test
    fun aLongListIsStillFullyRendered() = runComposeUiTest {
        val many = (1..6).map { SinkStatus(id = "sink$it", displayName = "Screen $it") }

        showScreen { OutputTargetsContent(many) }

        assertTrue(exists(StandaloneTags.sink("sink1")))
        assertTrue(exists(StandaloneTags.sink("sink6")))
    }

    @Test
    fun anEmptyListIsNotConfusedWithADisconnectedSink() = runComposeUiTest {
        showScreen { OutputTargetsContent(listOf(external(state = SinkState.DETACHED))) }

        assertFalse(exists(StandaloneTags.OUTPUTS_EMPTY))
    }

    @Test
    fun aSinkListWithOnlyTheBrowserSinkIsNotEmpty() = runComposeUiTest {
        showScreen { OutputTargetsContent(listOf(browser())) }

        assertFalse(exists(StandaloneTags.OUTPUTS_EMPTY))
    }

    @Test
    fun theTitleIsAlwaysThere() = runComposeUiTest {
        // Including over an empty list, which is where an operator most needs to
        // know what they are looking at.
        showScreen { OutputTargetsContent(emptyList()) }

        assertTrue(exists(StandaloneTags.OUTPUTS_EMPTY))
    }

    @Test
    fun aSinkThatChangesStateKeepsItsRow() = runComposeUiTest {
        showScreen { OutputTargetsContent(listOf(browser(state = SinkState.ATTACHING))) }

        assertTrue(exists(StandaloneTags.sink("browser")))
    }

    @Test
    fun theSameSinkIdIsNotDuplicated() = runComposeUiTest {
        showScreen { OutputTargetsContent(listOf(browser(), external())) }

        assertTrue(exists(StandaloneTags.sink("browser")))
    }

    @Test
    fun anErrorSinkStillCarriesItsName() = runComposeUiTest {
        showScreen { OutputTargetsContent(listOf(browser(state = SinkState.ERROR))) }

        assertTrue(isShowing("Browser display"))
    }

    @Test
    fun aSearchingSinkStillCarriesItsName() = runComposeUiTest {
        showScreen { OutputTargetsContent(listOf(external(state = SinkState.ATTACHING))) }

        assertTrue(isShowing("External display"))
    }
}
