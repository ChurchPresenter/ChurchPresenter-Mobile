package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.MoreDestination
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests [MoreScreen] — the launcher for everything not on the tab strip.
 *
 * Its entries differ by mode: standalone keeps only the destinations that work
 * without a desktop. Offering one that doesn't leads the operator to a screen
 * that can only time out.
 */
@OptIn(ExperimentalTestApi::class)
class MoreScreenTest {

    private fun themed(content: @Composable () -> Unit): @Composable () -> Unit = {
        AppTheme(themeMode = ThemeMode.DARK) { content() }
    }

    private fun rows(test: androidx.compose.ui.test.ComposeUiTest) =
        test.onAllNodes(hasClickAction())

    @Test
    fun remoteOffersOneRowPerAvailableDestination() = runComposeUiTest {
        setContent(themed { MoreScreen(mode = AppMode.REMOTE, onSelect = {}) })

        assertEquals(
            MoreDestination.forMode(AppMode.REMOTE).size,
            rows(this).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun standaloneOffersOnlyItsOwnDestinations() = runComposeUiTest {
        setContent(themed { MoreScreen(mode = AppMode.STANDALONE, onSelect = {}) })

        assertEquals(
            MoreDestination.forMode(AppMode.STANDALONE).size,
            rows(this).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun standaloneOffersFewerThanRemote() = runComposeUiTest {
        // The whole point of the gate: some of these need a computer.
        assertTrue(
            MoreDestination.forMode(AppMode.STANDALONE).size <
                MoreDestination.forMode(AppMode.REMOTE).size,
        )
    }

    @Test
    fun tappingARowReportsItsDestination() = runComposeUiTest {
        var picked: MoreDestination? = null
        setContent(themed { MoreScreen(mode = AppMode.REMOTE, onSelect = { picked = it }) })

        rows(this)[0].performClick()

        assertTrue(picked in MoreDestination.forMode(AppMode.REMOTE), "reported $picked")
    }

    @Test
    fun everyRowIsTappable() = runComposeUiTest {
        // A row rendered without a handler is a dead entry in a list of live ones.
        val picked = mutableListOf<MoreDestination>()
        setContent(themed { MoreScreen(mode = AppMode.REMOTE, onSelect = { picked += it }) })

        val count = rows(this).fetchSemanticsNodes().size
        repeat(count) { rows(this)[it].performClick() }

        assertEquals(count, picked.size)
    }

    @Test
    fun eachRowReportsADifferentDestination() = runComposeUiTest {
        // Two rows wired to the same destination is a silent mis-mapping.
        val picked = mutableListOf<MoreDestination>()
        setContent(themed { MoreScreen(mode = AppMode.REMOTE, onSelect = { picked += it }) })

        val count = rows(this).fetchSemanticsNodes().size
        repeat(count) { rows(this)[it].performClick() }

        assertEquals(picked.size, picked.toSet().size, "duplicate destinations: $picked")
    }

    @Test
    fun everyOfferedDestinationIsOneTheModeAllows() = runComposeUiTest {
        for (mode in AppMode.entries) {
            val picked = mutableListOf<MoreDestination>()
            setContent(themed { MoreScreen(mode = mode, onSelect = { picked += it }) })

            val count = rows(this).fetchSemanticsNodes().size
            repeat(count) { rows(this)[it].performClick() }

            assertTrue(
                picked.all { it in MoreDestination.forMode(mode) },
                "$mode offered ${picked.filterNot { it in MoreDestination.forMode(mode) }}",
            )
        }
    }

    @Test
    fun itRendersInBothThemes() = runComposeUiTest {
        for (theme in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            setContent { AppTheme(themeMode = theme) { MoreScreen(mode = AppMode.REMOTE, onSelect = {}) } }
            assertTrue(rows(this).fetchSemanticsNodes().isNotEmpty())
        }
    }
}
