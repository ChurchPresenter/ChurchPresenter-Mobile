package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.App
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.AppTab
import com.church.presenter.churchpresentermobile.model.MoreDestination
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The app shell — `App()`, composed the way the platform entry points compose it.
 *
 * Every other UI test drives one screen with its arguments set by hand. This is
 * the only one that exercises the wiring *between* them: the splash handing over
 * to the right first screen, the setup flow, and the tab strip swapping the
 * screen behind it. That wiring is the whole of `App.kt`, and until this test
 * nothing executed a line of it.
 *
 * Both of `App`'s seams are load-bearing here:
 *
 * - **settings**, because what follows the splash is decided by persisted state
 *   — with the real store the test would show a mode picker on one machine and
 *   the tab strip on another.
 * - **the launch ping**, because rendering the app fires a request at
 *   churchpresenter.org. Without the seam every run of this test — and every CI
 *   run — would book a phantom install on the live map.
 */
@OptIn(ExperimentalTestApi::class)
class AppShellTest {

    /**
     * Settings for a device that has been through setup already.
     *
     * The address is a closed local port on purpose. The status check the splash
     * hands over to makes a REAL request, so left at its default this test sat on
     * "Connecting to server…" until the connect timeout — slow, and reaching out
     * of the test machine. A refused connection to 127.0.0.1 comes back
     * immediately and never leaves the loopback.
     */
    private fun settledSettings() = AppSettings(InMemorySettingsStorage()).apply {
        isModeChosen = true
        isConnectSetupDone = true
        host = "127.0.0.1"
        port = 1
    }

    private fun ComposeUiTest.exists(tag: String) =
        onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    private fun ComposeUiTest.tap(tag: String) {
        onNode(hasTestTag(tag)).performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()
    }

    /** The splash animates for a couple of seconds before handing over. */
    private fun ComposeUiTest.passTheSplash() {
        mainClock.advanceTimeBy(6_000)
        waitForIdle()
    }

    /**
     * Gets past the status check to the app itself.
     *
     * The check runs on real threads, so the virtual clock cannot skip it — the
     * wait is on the screen settling into a state that has a way forward.
     */
    private fun ComposeUiTest.reachTheApp() {
        passTheSplash()
        waitUntil(timeoutMillis = 10_000) {
            exists(UiTags.STATUS_CONTINUE) || offeredTabs().isNotEmpty()
        }
        if (exists(UiTags.STATUS_CONTINUE)) tap(UiTags.STATUS_CONTINUE)
    }

    private fun ComposeUiTest.offeredTabs() = AppTab.entries.filter { exists(UiTags.tab(it)) }

    @Test
    fun a_settled_device_reaches_a_working_tab_strip() = runComposeUiTest {
        setContent { App(appSettings = settledSettings(), onLaunchPing = {}) }
        reachTheApp()

        assertTrue(offeredTabs().isNotEmpty(), "no tab strip after the status screen")
    }

    @Test
    fun every_tab_the_strip_offers_can_be_opened() = runComposeUiTest {
        // Not "does the tap work" — whether each tab's screen composes at all.
        // A screen that throws on first composition takes the app down, and
        // nothing else renders them through App().
        setContent { App(appSettings = settledSettings(), onLaunchPing = {}) }
        reachTheApp()

        val tabs = offeredTabs()
        assertTrue(tabs.isNotEmpty(), "no tab strip to drive")
        tabs.forEach { tab ->
            tap(UiTags.tab(tab))
            assertTrue(exists(UiTags.tab(tab)), "the strip vanished after opening ${tab.name}")
        }
    }

    @Test
    fun a_fresh_device_is_asked_to_set_up_before_it_is_shown_the_app() = runComposeUiTest {
        // First launch must not drop someone into a Songs tab that can only time
        // out against a computer they have not told it about yet.
        setContent { App(appSettings = AppSettings(InMemorySettingsStorage()), onLaunchPing = {}) }
        passTheSplash()

        assertTrue(
            exists(UiTags.CONNECT_HOST) || exists(UiTags.CONNECT_SKIP) || offeredTabs().isEmpty(),
            "a device that has never been set up went straight to the app",
        )
    }

    @Test
    fun the_launch_ping_fires_once_per_launch() = runComposeUiTest {
        // Once per launch is what the live map counts; a ping per recomposition
        // would inflate one install into dozens.
        val pings = mutableListOf<String>()
        setContent { App(appSettings = settledSettings(), onLaunchPing = { pings += it }) }
        passTheSplash()

        assertEquals(1, pings.size, "expected one launch ping, got ${pings.size}")
    }

    @Test
    fun every_destination_behind_More_opens_and_comes_back() = runComposeUiTest {
        // The More launcher is where half the app lives — pictures, notices,
        // Q&A, the dictionary, the web page, settings, contact. Each is a branch
        // of one `when` in App.kt that nothing else executes, and any of them
        // throwing on first composition takes the whole app down.
        setContent { App(appSettings = settledSettings(), onLaunchPing = {}) }
        reachTheApp()
        tap(UiTags.tab(AppTab.MORE))

        val offered = MoreDestination.entries.filter { exists(UiTags.moreRow(it)) }
        assertTrue(offered.isNotEmpty(), "the More launcher offered nothing")

        offered.forEach { destination ->
            tap(UiTags.moreRow(destination))
            // Back to the launcher for the next one: re-tapping More is the
            // gesture that returns it, and it has to work from every screen.
            tap(UiTags.tab(AppTab.MORE))
            assertTrue(
                exists(UiTags.moreRow(destination)),
                "could not get back to the More launcher after opening ${destination.name}",
            )
        }
    }

    @Test
    fun reopening_the_active_tab_keeps_the_shell_up() = runComposeUiTest {
        // Re-tapping the active tab is a real gesture — it returns More to its
        // launcher grid — and must not tear the strip down.
        setContent { App(appSettings = settledSettings(), onLaunchPing = {}) }
        reachTheApp()
        val first = offeredTabs().firstOrNull() ?: return@runComposeUiTest

        tap(UiTags.tab(first))
        tap(UiTags.tab(first))

        assertTrue(exists(UiTags.tab(first)))
    }
}
