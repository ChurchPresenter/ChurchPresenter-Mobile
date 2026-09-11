package com.church.presenter.churchpresentermobile

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.ui.BibleBooksPaneWidth
import com.church.presenter.churchpresentermobile.ui.BibleChaptersPaneWidth
import com.church.presenter.churchpresentermobile.ui.LibraryListPaneWidth
import com.church.presenter.churchpresentermobile.ui.ListPaneWidth
import com.church.presenter.churchpresentermobile.ui.MediaSendPaneWidth
import com.church.presenter.churchpresentermobile.ui.MoreTilePaneWidth
import com.church.presenter.churchpresentermobile.ui.NavRailMinWidth
import com.church.presenter.churchpresentermobile.ui.NavRailWidth
import com.church.presenter.churchpresentermobile.ui.PresentSidePaneWidth
import com.church.presenter.churchpresentermobile.ui.TwoPaneMinWidth
import com.church.presenter.churchpresentermobile.ui.TwoVerseColumnsMinWidth
import com.church.presenter.churchpresentermobile.ui.usesNavRail
import com.church.presenter.churchpresentermobile.ui.usesTwoPaneLayout
import com.church.presenter.churchpresentermobile.ui.verseColumns
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The widths at which the app changes shape.
 *
 * All of this sits inside `App()`, which cannot be run in a test — it builds a
 * WebSocket, a server and a dozen ViewModels on the way past — while being
 * exactly the part that goes wrong quietly. A rail that arrives at a width it
 * does not fit leaves the tabs over the content; a split that arrives too early
 * hands the operator a detail pane narrower than the phone screen it replaced,
 * mid-service, on the device driving the hall screen.
 *
 * The widths named below are the real devices they sort, so changing a constant
 * has to say which device it is moving.
 *
 * Lives outside the `ui` package deliberately: that package is excluded from
 * `jsBrowserTest` and from the Android run JaCoCo measures, because the Compose
 * tests in it need a Skia surface. These are arithmetic over `Dp` and need
 * nothing of the sort, so filing them there would hide them from the gate and
 * from the coverage figure both.
 */
class AdaptiveLayoutTest {

    // ── The rail ─────────────────────────────────────────────────────────

    @Test
    fun `a phone keeps the bottom strip`() {
        assertFalse(usesNavRail(360.dp))
        assertFalse(usesNavRail(430.dp))   // the largest phones, in portrait
    }

    @Test
    fun `a phone in landscape keeps the bottom strip`() {
        // ~744dp wide but only ~390dp tall: a rail would eat the axis that is
        // already short, which is the whole reason the rail exists.
        assertFalse(usesNavRail(744.dp))
    }

    @Test
    fun `a tablet gets the rail`() {
        assertTrue(usesNavRail(1024.dp))   // 9.7" landscape
        assertTrue(usesNavRail(1180.dp))   // iPad 10.9" landscape
        assertTrue(usesNavRail(1366.dp))   // iPad Pro 12.9" landscape
    }

    @Test
    fun `the rail starts exactly at its breakpoint`() {
        assertFalse(usesNavRail(NavRailMinWidth - 1.dp))
        assertTrue(usesNavRail(NavRailMinWidth))
    }

    // ── The split ────────────────────────────────────────────────────────

    @Test
    fun `a tablet in portrait is not split`() {
        // 820dp: room for the rail, not for two readable panes beside it.
        assertFalse(usesTwoPaneLayout(820.dp))
        assertFalse(usesTwoPaneLayout(834.dp))
    }

    @Test
    fun `a tablet in landscape is split`() {
        assertTrue(usesTwoPaneLayout(1024.dp))
        assertTrue(usesTwoPaneLayout(1366.dp))
    }

    @Test
    fun `the split starts exactly at its breakpoint`() {
        assertFalse(usesTwoPaneLayout(TwoPaneMinWidth - 1.dp))
        assertTrue(usesTwoPaneLayout(TwoPaneMinWidth))
    }

    @Test
    fun `a split layout always has the rail too`() {
        // The panes are laid out beside the rail. A width that split without
        // railing would put a bottom strip under two panes, and give those panes
        // 216dp of width that nothing is drawing in.
        listOf(TwoPaneMinWidth, 1024.dp, 1180.dp, 1366.dp, 2000.dp).forEach { width ->
            assertTrue(usesNavRail(width), "$width splits but has no rail")
        }
    }

    @Test
    fun `there is a width with a rail and no split`() {
        // The point of two breakpoints rather than one: a 9" tablet gets the
        // rail without being asked to fit two panes it has no room for.
        assertTrue(usesNavRail(900.dp))
        assertFalse(usesTwoPaneLayout(900.dp))
    }

    // ── The verse grid ───────────────────────────────────────────────────

    @Test
    fun `a phone shows one verse column`() {
        assertEquals(1, verseColumns(360.dp))
        assertEquals(1, verseColumns(430.dp))
    }

    @Test
    fun `a roomy detail pane shows two`() {
        assertEquals(2, verseColumns(detailPaneAt(1180.dp)))
        assertEquals(2, verseColumns(detailPaneAt(1366.dp)))
    }

    @Test
    fun `a cramped detail pane drops back to one`() {
        // 1024dp splits, but its detail pane is ~427dp — narrower than the phone
        // screen two columns would have to beat.
        assertEquals(1, verseColumns(detailPaneAt(1024.dp)))
    }

    @Test
    fun `the verse grid never goes past two columns`() {
        // An adaptive minimum small enough to reach two columns on a 10.9" tablet
        // gave THREE on a 12.9" one, which the design does not have. Whatever the
        // width, the answer is capped.
        listOf(1366.dp, 1600.dp, 2000.dp, 3840.dp).forEach { window ->
            assertEquals(2, verseColumns(detailPaneAt(window)), "at $window")
        }
    }

    @Test
    fun `two verse columns start exactly at their breakpoint`() {
        assertEquals(1, verseColumns(TwoVerseColumnsMinWidth - 1.dp))
        assertEquals(2, verseColumns(TwoVerseColumnsMinWidth))
    }

    // ── The panes fit ────────────────────────────────────────────────────

    @Test
    fun `every split screen leaves its second pane something to draw in`() {
        // At the very first width that splits. A fixed pane wider than this
        // leaves the other side at zero or negative, which Compose clamps
        // silently rather than failing.
        val remaining = TwoPaneMinWidth - NavRailWidth
        listOf(
            "songs" to ListPaneWidth,
            "more" to MoreTilePaneWidth,
            "library" to LibraryListPaneWidth,
            "media" to MediaSendPaneWidth,
            "present" to PresentSidePaneWidth,
            "bible" to BibleBooksPaneWidth + BibleChaptersPaneWidth,
        ).forEach { (name, fixed) ->
            val flexible = remaining - fixed
            assertTrue(flexible >= 250.dp, "$name leaves only $flexible for its other pane")
        }
    }

    @Test
    fun `the chapter pane fits two chapter cells`() {
        // 2 cells of 64dp, the 8dp between them, and the grid's own 16dp of side
        // padding. The pane was 168dp and produced ONE column, because the pane
        // padded the grid a second time.
        val gridPadding = 32.dp
        val twoCells = 64.dp * 2 + 8.dp
        assertTrue(
            BibleChaptersPaneWidth - gridPadding >= twoCells,
            "chapter pane is $BibleChaptersPaneWidth — too narrow for two cells",
        )
    }

    /** The detail pane's width in a window [window] wide: less the rail, the list and a divider. */
    private fun detailPaneAt(window: Dp): Dp = window - NavRailWidth - ListPaneWidth - 1.dp
}
