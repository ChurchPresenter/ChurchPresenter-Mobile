package com.church.presenter.churchpresentermobile.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests [FabStack] and [SquareFab] — the floating actions on the content screens.
 *
 * Each of the three actions is optional. A stack that renders a button without a
 * handler puts an inert control in the operator's reach, mid-service.
 */
@OptIn(ExperimentalTestApi::class)
class FabStackTest {

    private fun themed(content: @Composable () -> Unit): @Composable () -> Unit = {
        AppTheme(themeMode = ThemeMode.DARK) { content() }
    }

    private fun fabCount(test: androidx.compose.ui.test.ComposeUiTest): Int =
        test.onAllNodes(hasClickAction()).fetchSemanticsNodes().size

    @Test
    fun anEmptyStackRendersNoButtons() = runComposeUiTest {
        setContent(themed { FabStack() })

        assertEquals(0, fabCount(this))
    }

    @Test
    fun eachHandlerAddsExactlyOneButton() = runComposeUiTest {
        setContent(themed { FabStack(onSelect = {}) })
        assertEquals(1, fabCount(this))

        setContent(themed { FabStack(onSelect = {}, onAddToSchedule = {}) })
        assertEquals(2, fabCount(this))

        setContent(themed { FabStack(onSelect = {}, onAddToSchedule = {}, onCast = {}) })
        assertEquals(ALL_THREE_ACTIONS, fabCount(this))
    }

    @Test
    fun eachButtonReportsItsOwnTap() = runComposeUiTest {
        var selects = 0
        var adds = 0
        var casts = 0
        setContent(themed {
            FabStack(onSelect = { selects++ }, onAddToSchedule = { adds++ }, onCast = { casts++ })
        })

        val nodes = onAllNodes(hasClickAction())
        nodes[0].performClick()
        nodes[1].performClick()
        nodes[2].performClick()

        assertEquals(1, selects)
        assertEquals(1, adds)
        assertEquals(1, casts)
    }

    @Test
    fun aCastBadgeCountDoesNotAddAButton() = runComposeUiTest {
        // The badge decorates the cast button rather than being tappable itself.
        setContent(themed { FabStack(onCast = {}, castBadgeCount = SOME_SCREENS) })

        assertEquals(1, fabCount(this))
    }

    @Test
    fun everyBadgeCountRenders() = runComposeUiTest {
        for (count in BADGE_COUNTS) {
            setContent(themed { FabStack(onCast = {}, castBadgeCount = count) })
            assertEquals(1, fabCount(this))
        }
    }

    // ── SquareFab ────────────────────────────────────────────────────────

    @Test
    fun aSquareFabIsLabelledForScreenReaders() = runComposeUiTest {
        setContent(themed {
            SquareFab(
                icon = Icons.Filled.Refresh,
                contentDescription = "Refresh",
                containerColor = Color.Blue,
                iconColor = Color.White,
                shadowColor = Color.Black,
                onClick = {},
            )
        })

        onNodeWithContentDescription("Refresh", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aSquareFabReportsItsTap() = runComposeUiTest {
        var clicks = 0
        setContent(themed {
            SquareFab(
                icon = Icons.Filled.Refresh,
                contentDescription = "Refresh",
                containerColor = Color.Blue,
                iconColor = Color.White,
                shadowColor = Color.Black,
                onClick = { clicks++ },
            )
        })

        onAllNodes(hasClickAction())[0].performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun theStackRendersInBothThemes() = runComposeUiTest {
        for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            setContent { AppTheme(themeMode = mode) { FabStack(onSelect = {}, onCast = {}) } }
            assertEquals(2, fabCount(this))
        }
    }
}

private const val ALL_THREE_ACTIONS = 3

/** An arbitrary non-zero count, to prove the badge does not add a button. */
private const val SOME_SCREENS = 3

/** None, single digit, and two digit — the three ways the badge is drawn. */
private val BADGE_COUNTS = listOf(0, 1, 9, 10, 99)
