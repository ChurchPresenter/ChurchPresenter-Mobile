package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.SlideDeckBuilder
import com.church.presenter.churchpresentermobile.present.SinkRegistry
import com.church.presenter.churchpresentermobile.present.SinkState
import com.church.presenter.churchpresentermobile.present.SinkStatus
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.isShowing
import com.church.presenter.churchpresentermobile.ui.showScreen
import com.church.presenter.churchpresentermobile.viewmodel.StandaloneViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The chip that answers "is any of this actually reaching a screen?".
 *
 * It is the first thing an operator looks at when the wall is blank, and the
 * only place the app admits that nothing is connected. Saying "casting" over a
 * TV that was unplugged ten minutes ago is the failure worth catching — the
 * operator then spends the sermon looking for a fault in the wrong place.
 */
@OptIn(ExperimentalTestApi::class)
class ControllerOutputsTest {

    /** A controller wired to sinks whose state the test decides. */
    private class Fixture(vararg sinks: TestSink) {
        val registry = SinkRegistry()
        val engine = StandaloneEngine(MutableStateFlow(AppMode.STANDALONE), registry)
        val settings = AppSettings(InMemorySettingsStorage())

        init {
            sinks.forEach { registry.register(it) }
        }

        fun publish(sink: TestSink, status: SinkStatus) {
            sink.publish(status)
            registry.refreshStatuses()
        }
    }

    private fun ComposeUiTest.showController(f: Fixture) = showScreen {
        StandaloneControllerScreen(
            engine = f.engine,
            registry = f.registry,
            settings = f.settings,
            providedViewModel = StandaloneViewModel(f.engine, f.registry, f.settings, null),
        )
    }

    private fun attached(id: String = "tv", name: String = "Sanctuary TV", detail: String? = null) =
        TestSink(id, name, SinkState.ATTACHED, detail)

    private fun waiting(id: String = "tv", name: String = "External display") =
        TestSink(id, name, SinkState.ATTACHING)

    private fun off(id: String = "tv", name: String = "External display") =
        TestSink(id, name, SinkState.DETACHED)

    // ── Nothing connected ────────────────────────────────────────────────

    @Test
    fun withNoSinksTheChipIsStillThere() = runComposeUiTest {
        // It is where the operator goes to find out why the wall is blank.
        showController(Fixture())

        assertTrue(exists(StandaloneTags.OUTPUT_CHIP))
    }

    @Test
    fun withNoSinksTheOutputsListSaysItIsEmpty() = runComposeUiTest {
        showController(Fixture())

        click(StandaloneTags.OUTPUT_CHIP)

        awaitThat { exists(StandaloneTags.OUTPUTS_EMPTY) }
    }

    @Test
    fun withEverythingOffTheChipIsStillThere() = runComposeUiTest {
        showController(Fixture(off()))

        assertTrue(exists(StandaloneTags.OUTPUT_CHIP))
    }

    @Test
    fun withEverythingOffTheListStillNamesTheScreen() = runComposeUiTest {
        showController(Fixture(off()))

        click(StandaloneTags.OUTPUT_CHIP)

        awaitThat { exists(StandaloneTags.sink("tv")) }
    }

    // ── Something connected ──────────────────────────────────────────────

    @Test
    fun anAttachedScreenIsNamedOnTheChip() = runComposeUiTest {
        showController(Fixture(attached(name = "Sanctuary TV")))

        assertTrue(isShowing("Sanctuary TV"))
    }

    @Test
    fun anAttachedScreenIsListed() = runComposeUiTest {
        showController(Fixture(attached()))

        click(StandaloneTags.OUTPUT_CHIP)

        awaitThat { exists(StandaloneTags.sink("tv")) }
    }

    @Test
    fun aScreenThatArrivesLaterReachesTheChip() = runComposeUiTest {
        // A TV plugged in mid-service is the common case, not an edge one.
        val sink = off()
        val f = Fixture(sink)
        showController(f)

        f.publish(sink, SinkStatus(id = "tv", displayName = "Sanctuary TV", state = SinkState.ATTACHED))

        awaitThat { isShowing("Sanctuary TV") }
    }

    @Test
    fun aScreenThatGoesAwayStopsBeingAdvertised() = runComposeUiTest {
        // Saying "casting" over a TV in someone's bag sends the operator hunting
        // for the wrong fault.
        val sink = attached(name = "Sanctuary TV")
        val f = Fixture(sink)
        showController(f)
        awaitThat { isShowing("Sanctuary TV") }

        f.publish(sink, SinkStatus(id = "tv", displayName = "External display", state = SinkState.ATTACHING))

        awaitThat { !isShowing("Sanctuary TV") }
    }

    @Test
    fun aFailedScreenIsStillListed() = runComposeUiTest {
        val sink = TestSink("tv", "Sanctuary TV", SinkState.ERROR)
        showController(Fixture(sink))

        click(StandaloneTags.OUTPUT_CHIP)

        awaitThat { exists(StandaloneTags.sink("tv")) }
    }

    @Test
    fun twoScreensAreBothListed() = runComposeUiTest {
        showController(Fixture(attached(), TestSink("browser", "Browser display", SinkState.ATTACHED)))

        click(StandaloneTags.OUTPUT_CHIP)

        awaitThat { exists(StandaloneTags.sink("tv")) }
        assertTrue(exists(StandaloneTags.sink("browser")))
    }

    @Test
    fun aServingBrowserShowsItsAddressInTheList() = runComposeUiTest {
        val browser = TestSink(
            "browser",
            "Browser display",
            SinkState.ATTACHED,
            "http://192.168.1.50:8080/display",
        )
        showController(Fixture(browser))

        click(StandaloneTags.OUTPUT_CHIP)

        awaitThat { exists(StandaloneTags.sinkUrl("browser")) }
    }

    @Test
    fun theOutputsListOpensOverTheController() = runComposeUiTest {
        showController(Fixture(attached()))

        click(StandaloneTags.OUTPUT_CHIP)

        awaitThat { exists(StandaloneTags.sink("tv")) }
        assertTrue(exists(StandaloneTags.PREVIEW))
    }

    @Test
    fun theListReflectsAScreenLostWhileItIsOpen() = runComposeUiTest {
        val sink = attached(name = "Sanctuary TV")
        val f = Fixture(sink)
        showController(f)
        click(StandaloneTags.OUTPUT_CHIP)
        awaitThat { exists(StandaloneTags.sink("tv")) }

        f.publish(sink, SinkStatus(id = "tv", displayName = "External display", state = SinkState.ERROR))

        awaitThat { exists(StandaloneTags.sink("tv")) }
    }

    // ── What actually reaches the screens ────────────────────────────────

    @Test
    fun aProjectedSlideReachesAnAttachedScreen() = runComposeUiTest {
        val sink = attached()
        val f = Fixture(sink)
        showController(f)

        f.engine.setDeck(SlideDeckBuilder.fromAnnouncements(listOf("First slide"), "Notices"))

        awaitThat { sink.rendered.isNotEmpty() }
    }

    @Test
    fun aProjectedSlideCarriesItsWords() = runComposeUiTest {
        val sink = attached()
        val f = Fixture(sink)
        showController(f)

        f.engine.setDeck(SlideDeckBuilder.fromAnnouncements(listOf("First slide"), "Notices"))

        awaitThat { sink.rendered.any { it.slide?.body?.contains("First slide") == true } }
    }

    @Test
    fun steppingReachesEveryScreen() = runComposeUiTest {
        val tv = attached()
        val browser = TestSink("browser", "Browser display", SinkState.ATTACHED)
        val f = Fixture(tv, browser)
        showController(f)
        f.engine.setDeck(SlideDeckBuilder.fromAnnouncements(listOf("One", "Two"), "Notices"))
        awaitThat { exists(StandaloneTags.section(1)) }

        click(StandaloneTags.NEXT)

        awaitThat { browser.rendered.any { it.slide?.body?.contains("Two") == true } }
    }

    @Test
    fun blankingReachesTheScreen() = runComposeUiTest {
        val sink = attached()
        val f = Fixture(sink)
        showController(f)
        f.engine.setDeck(SlideDeckBuilder.fromAnnouncements(listOf("One"), "Notices"))
        awaitThat { exists(StandaloneTags.section(0)) }

        click(StandaloneTags.BLANK)

        awaitThat { sink.rendered.last().slide?.isHidden == true }
    }

    @Test
    fun clearingReachesTheScreen() = runComposeUiTest {
        val sink = attached()
        val f = Fixture(sink)
        showController(f)
        f.engine.setDeck(SlideDeckBuilder.fromAnnouncements(listOf("One"), "Notices"))
        awaitThat { exists(StandaloneTags.section(0)) }

        click(StandaloneTags.CLEAR)

        awaitThat { sink.rendered.last().slide?.body.isNullOrBlank() }
    }

    @Test
    fun holdingOutputBackReachesTheScreen() = runComposeUiTest {
        // The audience screen has to stop showing it; the operator's preview
        // does not.
        val sink = attached()
        val f = Fixture(sink)
        showController(f)
        f.engine.setDeck(SlideDeckBuilder.fromAnnouncements(listOf("One"), "Notices"))
        awaitThat { exists(StandaloneTags.section(0)) }

        click(StandaloneTags.LIVE)

        awaitThat { sink.rendered.last().slide?.isHidden == true }
    }

    @Test
    fun aBackdropChangeReachesTheScreen() = runComposeUiTest {
        val sink = attached()
        val f = Fixture(sink)
        showController(f)
        f.engine.setDeck(SlideDeckBuilder.fromAnnouncements(listOf("One"), "Notices"))
        awaitThat { exists(StandaloneTags.section(0)) }

        click(StandaloneTags.backdrop(2))

        awaitThat { sink.rendered.last().slide?.backdrop?.name == "BLACK" }
    }

    @Test
    fun aScreenAttachedAfterASlideCanStillBeCaughtUp() = runComposeUiTest {
        // Registration order does not decide who is up to date; the next frame
        // reaches everyone.
        val sink = off()
        val f = Fixture(sink)
        showController(f)
        f.engine.setDeck(SlideDeckBuilder.fromAnnouncements(listOf("One", "Two"), "Notices"))
        awaitThat { exists(StandaloneTags.section(1)) }

        click(StandaloneTags.NEXT)

        awaitThat { sink.rendered.any { it.slide?.body?.contains("Two") == true } }
    }

    @Test
    fun aScreenInErrorStillReceivesFrames() = runComposeUiTest {
        // Whether it can show them is its own business; the registry does not
        // decide to stop talking to it.
        val sink = TestSink("tv", "Sanctuary TV", SinkState.ERROR)
        val f = Fixture(sink)
        showController(f)

        f.engine.setDeck(SlideDeckBuilder.fromAnnouncements(listOf("One"), "Notices"))

        awaitThat { sink.rendered.isNotEmpty() }
    }

    @Test
    fun twoScreensReceiveTheSameSlide() = runComposeUiTest {
        val tv = attached()
        val browser = TestSink("browser", "Browser display", SinkState.ATTACHED)
        val f = Fixture(tv, browser)
        showController(f)

        f.engine.setDeck(SlideDeckBuilder.fromAnnouncements(listOf("One"), "Notices"))

        awaitThat { tv.rendered.isNotEmpty() && browser.rendered.isNotEmpty() }
    }

    @Test
    fun theControllerWorksWithNoScreensAtAll() = runComposeUiTest {
        // Setting up before the TV arrives is normal.
        val f = Fixture()
        showController(f)

        f.engine.setDeck(SlideDeckBuilder.fromAnnouncements(listOf("One", "Two"), "Notices"))
        awaitThat { exists(StandaloneTags.section(1)) }

        click(StandaloneTags.NEXT)

        awaitThat { f.engine.index.value == 1 }
    }

    @Test
    fun theChipStaysThroughADeckChange() = runComposeUiTest {
        val f = Fixture(attached())
        showController(f)

        f.engine.setDeck(SlideDeckBuilder.fromAnnouncements(listOf("One"), "Notices"))

        awaitThat { exists(StandaloneTags.section(0)) }
        assertTrue(exists(StandaloneTags.OUTPUT_CHIP))
    }

    @Test
    fun theChipStaysAfterClearing() = runComposeUiTest {
        val f = Fixture(attached())
        showController(f)
        f.engine.setDeck(SlideDeckBuilder.fromAnnouncements(listOf("One"), "Notices"))
        awaitThat { exists(StandaloneTags.section(0)) }

        click(StandaloneTags.CLEAR)

        awaitThat { exists(StandaloneTags.EMPTY_DECK) }
        assertTrue(exists(StandaloneTags.OUTPUT_CHIP))
    }

    @Test
    fun theOutputsListCanBeOpenedWithADeckLoaded() = runComposeUiTest {
        val f = Fixture(attached())
        showController(f)
        f.engine.setDeck(SlideDeckBuilder.fromAnnouncements(listOf("One"), "Notices"))
        awaitThat { exists(StandaloneTags.section(0)) }

        click(StandaloneTags.OUTPUT_CHIP)

        awaitThat { exists(StandaloneTags.sink("tv")) }
    }

    @Test
    fun anUnattachedScreenReceivesFramesToo() = runComposeUiTest {
        // The registry broadcasts; a sink decides for itself what to do with a
        // frame while it has nowhere to draw.
        val sink = waiting()
        val f = Fixture(sink)
        showController(f)

        f.engine.setDeck(SlideDeckBuilder.fromAnnouncements(listOf("One"), "Notices"))

        awaitThat { sink.rendered.isNotEmpty() }
    }

    @Test
    fun aScreenNameWithNoDetailStillReadsAsConnected() = runComposeUiTest {
        showController(Fixture(attached(name = "Sanctuary TV", detail = null)))

        assertTrue(isShowing("Sanctuary TV"))
    }

    @Test
    fun aScreenDetailIsShownInTheList() = runComposeUiTest {
        showController(Fixture(attached(detail = "1920×1080")))

        click(StandaloneTags.OUTPUT_CHIP)

        awaitThat { isShowing("1920×1080") }
    }

    @Test
    fun theListSurvivesEveryScreenGoingAway() = runComposeUiTest {
        val sink = attached()
        val f = Fixture(sink)
        showController(f)
        click(StandaloneTags.OUTPUT_CHIP)
        awaitThat { exists(StandaloneTags.sink("tv")) }

        f.publish(sink, SinkStatus(id = "tv", displayName = "External display", state = SinkState.DETACHED))

        awaitThat { exists(StandaloneTags.sink("tv")) }
    }

    @Test
    fun theControllerStillStepsWhileTheListIsOpen() = runComposeUiTest {
        val f = Fixture(attached())
        showController(f)
        f.engine.setDeck(SlideDeckBuilder.fromAnnouncements(listOf("One", "Two"), "Notices"))
        awaitThat { exists(StandaloneTags.section(1)) }
        click(StandaloneTags.OUTPUT_CHIP)
        awaitThat { exists(StandaloneTags.sink("tv")) }

        click(StandaloneTags.NEXT)

        awaitThat { f.engine.index.value == 1 }
    }

    @Test
    fun nothingIsSentBeforeADeckIsLoaded() = runComposeUiTest {
        // Opening the tab must not put anything on the wall.
        val sink = attached()
        val f = Fixture(sink)

        showController(f)

        assertFalse(sink.rendered.any { it.slide?.body?.isNotBlank() == true })
    }
}
