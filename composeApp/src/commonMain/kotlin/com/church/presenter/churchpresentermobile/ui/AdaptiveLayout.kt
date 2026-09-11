package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Where the phone layout gives way to the tablet one.
 *
 * There are two separate switches, not one, because they buy different things
 * and the width that pays for the first does not pay for the second:
 *
 *  - [NavRailMinWidth] moves the tab strip from the bottom edge to a side rail.
 *    That only needs enough width to spare 216dp, and it is a win on any tablet:
 *    the strip stops eating 72dp of the *short* axis, which is the scarce one in
 *    landscape.
 *  - [TwoPaneMinWidth] puts the list and the detail on screen together. That
 *    needs enough width for both to still be readable — a detail pane narrower
 *    than the phone screen it replaced is a downgrade, not a tablet layout.
 *
 * Collapsing them into one breakpoint means picking between a rail that arrives
 * too late on a 9" tablet and a two-pane split that arrives too early on one.
 */
internal val NavRailMinWidth: Dp = 840.dp

/**
 * 1000dp = the 216dp rail + a 400dp list pane + 384dp of detail, which is about
 * the narrowest a verse card stays readable at. Landscape on a 10"-and-up tablet
 * clears it (iPad 10.9" is 1180pt, a 12.9" 1366pt); portrait on the same device
 * does not, which is deliberate.
 */
internal val TwoPaneMinWidth: Dp = 1000.dp

/**
 * Fixed width of the Songs list pane; the detail pane takes what is left.
 *
 * Each split screen names its own, because the design gives them different
 * widths — a list of song rows, a grid of More tiles and a column of Bible books
 * are not the same shape. Each is sized so that at [TwoPaneMinWidth] the pane
 * beside it is still usable, which is why they are narrower than the design's
 * own figures at 1366dp.
 */
internal val ListPaneWidth: Dp = 380.dp

/**
 * Narrowest a More tile may be before its title and subtitle stop fitting.
 *
 * One column on a phone, two in a tablet's left-hand pane, three across a wide
 * single-pane window. An adaptive minimum suits these — unlike the verse grid,
 * a tile is a fixed-size thing and more of them per row is simply better use of
 * the space.
 */
internal val MoreTileMinWidth: Dp = 210.dp

/** Fixed width of the More launcher pane; the open tool takes what is left. */
internal val MoreTilePaneWidth: Dp = 480.dp

/** Fixed width of the Library list pane; the editor takes what is left. */
internal val LibraryListPaneWidth: Dp = 420.dp

/** Fixed width of the Media tab's send-and-load pane; the player takes the rest. */
internal val MediaSendPaneWidth: Dp = 340.dp

/** Fixed width of the Present tab's deck-and-look pane; the live surface takes the rest. */
internal val PresentSidePaneWidth: Dp = 380.dp

/** Fixed width of the Bible books pane, leftmost of that tab's three. */
internal val BibleBooksPaneWidth: Dp = 280.dp

/**
 * Fixed width of the Bible chapter-number pane, between the books and the verses.
 *
 * Sized from the two cells it has to hold: 2 x 64dp plus the 8dp between them plus
 * the grid's own 16dp of side padding is 168, and the extra 12 keeps it off the
 * dividers rather than exactly against them.
 */
internal val BibleChaptersPaneWidth: Dp = 180.dp


/** Width of the side rail. Matches the design's 216px at iPad-Pro scale. */
internal val NavRailWidth: Dp = 216.dp

/**
 * Narrowest a *detail pane* may be before the verse grid uses two columns.
 *
 * A width rule rather than an adaptive minimum, because the design says two
 * columns and adaptive sizing has no upper bound: a minimum small enough to
 * reach two columns on a 10.9" tablet gave three on a 12.9" one, and one large
 * enough to stop at two there never reached two on the smaller.
 *
 * 520 sits between the two panes it has to tell apart — ~427dp on a 1024dp
 * tablet, where one column is right, and ~583dp on a 1180dp one, where the
 * design shows two.
 */
internal val TwoVerseColumnsMinWidth: Dp = 520.dp

/**
 * Verse cards per row in a pane [paneWidth] wide.
 *
 * Never more than two, whatever the width: the design's grid is two columns, and
 * a fourth and fifth column of four-line verses is not more readable, only
 * smaller.
 */
internal fun verseColumns(paneWidth: Dp): Int =
    if (paneWidth >= TwoVerseColumnsMinWidth) 2 else 1

/**
 * Whether [width] gets the side rail instead of the bottom tab strip.
 *
 * Pulled out of the layout, with the width as a plain value, so the breakpoint
 * is testable without a Skia surface — see AGENT.md, "reach for a seam".
 */
internal fun usesNavRail(width: Dp): Boolean = width >= NavRailMinWidth

/** Whether [width] gets list and detail side by side. Implies [usesNavRail]. */
internal fun usesTwoPaneLayout(width: Dp): Boolean = width >= TwoPaneMinWidth
