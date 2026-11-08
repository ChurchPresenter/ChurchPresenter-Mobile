package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.MoreDestination
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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

    @Test
    fun theRateTileIsHiddenWhereThereIsNoStore() = runComposeUiTest {
        setContent(themed { MoreScreen(mode = AppMode.REMOTE, onSelect = {}, rateUrl = null) })

        onAllNodesWithTag(UiTags.MORE_RATE).assertCountEquals(0)
    }

    @Test
    fun theRateTileIsShownInEveryModeWithAStore() = runComposeUiTest {
        for (mode in listOf(AppMode.REMOTE, AppMode.STANDALONE)) {
            setContent(themed { MoreScreen(mode = mode, onSelect = {}, rateUrl = STORE_URL) })
            onNodeWithTag(UiTags.MORE_RATE).assertIsDisplayed()
        }
    }

    @Test
    fun tappingTheRateTileOpensTheStoreAndNoDestination() = runComposeUiTest {
        var opened: String? = null
        var picked: MoreDestination? = null
        setContent(
            themed {
                MoreScreen(
                    mode = AppMode.REMOTE,
                    onSelect = { picked = it },
                    rateUrl = STORE_URL,
                    onRate = { opened = it },
                )
            },
        )

        onNodeWithTag(UiTags.MORE_RATE).performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(STORE_URL, opened)
        assertNull(picked)
    }

    @Test
    fun theRateTileAddsExactlyOneRow() = runComposeUiTest {
        setContent(themed { MoreScreen(mode = AppMode.REMOTE, onSelect = {}, rateUrl = STORE_URL) })

        assertEquals(
            MoreDestination.forMode(AppMode.REMOTE).size + 1,
            rows(this).fetchSemanticsNodes().size,
        )
    }

    private companion object {
        const val STORE_URL = "https://apps.apple.com/app/id1?action=write-review"
    }
}
