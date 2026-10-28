package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Draws a thin vertical scroll-thumb on the trailing edge of a [LazyColumn] —
 * the right in a left-to-right layout, the left in a right-to-left one.
 * Uses [drawWithContent] so no extra Box wrapper is needed.
 * Works on all targets (Android, iOS, Web).
 */
fun Modifier.verticalScrollbar(
    state: LazyListState,
    width: Dp = 4.dp,
    minThumbHeight: Dp = 32.dp,
): Modifier = composed {
    val thumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val needsScrollbar by remember(state) {
        derivedStateOf {
            state.layoutInfo.totalItemsCount > state.layoutInfo.visibleItemsInfo.size
        }
    }
    drawWithContent {
        drawContent()
        if (!needsScrollbar) return@drawWithContent

        val info          = state.layoutInfo
        val totalItems    = info.totalItemsCount.toFloat()
        val visible       = info.visibleItemsInfo
        val viewportH     = info.viewportSize.height.toFloat()
        if (viewportH == 0f || visible.isEmpty()) return@drawWithContent

        val avgItemH      = visible.map { it.size }.average().toFloat().coerceAtLeast(1f)
        val firstIndex    = state.firstVisibleItemIndex.toFloat()
        val firstOffset   = state.firstVisibleItemScrollOffset.toFloat()
        val visibleCount  = visible.size.toFloat()

        val thumbFraction = (visibleCount / totalItems).coerceIn(0f, 1f)
        val thumbH        = max(viewportH * thumbFraction, minThumbHeight.toPx())

        val scrollProgress = ((firstIndex + firstOffset / avgItemH) /
                (totalItems - visibleCount).coerceAtLeast(1f)).coerceIn(0f, 1f)
        val thumbTop = scrollProgress * (viewportH - thumbH)

        drawRoundRect(
            color       = thumbColor,
            // Trailing edge, not "the right": in a right-to-left locale the
            // content's own edge is on the left, and a thumb pinned to the right
            // sits over the text instead of beside it.
            topLeft     = Offset(thumbX(layoutDirection, size.width, width.toPx()), thumbTop),
            size        = Size(width.toPx(), thumbH),
            cornerRadius = CornerRadius(width.toPx() / 2)
        )
    }
}

/**
 * Draws a thin vertical scroll-thumb on the trailing edge of a [LazyVerticalGrid].
 * Works on all targets (Android, iOS, Web).
 */
fun Modifier.verticalScrollbar(
    state: LazyGridState,
    width: Dp = 4.dp,
    minThumbHeight: Dp = 32.dp,
): Modifier = composed {
    val thumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val needsScrollbar by remember(state) {
        derivedStateOf {
            state.layoutInfo.totalItemsCount > state.layoutInfo.visibleItemsInfo.size
        }
    }
    drawWithContent {
        drawContent()
        if (!needsScrollbar) return@drawWithContent

        val info       = state.layoutInfo
        val totalItems = info.totalItemsCount.toFloat()
        val visible    = info.visibleItemsInfo
        val viewportH  = info.viewportSize.height.toFloat()
        if (viewportH == 0f || visible.isEmpty()) return@drawWithContent

        // Estimate columns from first-row items
        val firstRow = visible.firstOrNull()?.row ?: 0
        val columns  = visible.count { it.row == firstRow }.toFloat().coerceAtLeast(1f)
        val avgRowH  = visible.firstOrNull()?.size?.height?.toFloat()?.coerceAtLeast(1f) ?: 1f

        val totalRows   = (totalItems / columns).coerceAtLeast(1f)
        val visibleRows = (viewportH / avgRowH).coerceAtLeast(1f)

        val firstIndex  = state.firstVisibleItemIndex.toFloat()
        val firstOffset = state.firstVisibleItemScrollOffset.toFloat()
        val firstRow2   = (firstIndex / columns)

        val thumbFraction  = (visibleRows / totalRows).coerceIn(0f, 1f)
        val thumbH         = max(viewportH * thumbFraction, minThumbHeight.toPx())
        val scrollProgress = ((firstRow2 + firstOffset / avgRowH) /
                (totalRows - visibleRows).coerceAtLeast(1f)).coerceIn(0f, 1f)
        val thumbTop = scrollProgress * (viewportH - thumbH)

        drawRoundRect(
            color        = thumbColor,
            topLeft      = Offset(thumbX(layoutDirection, size.width, width.toPx()), thumbTop),
            size         = Size(width.toPx(), thumbH),
            cornerRadius = CornerRadius(width.toPx() / 2)
        )
    }
}

/**
 * Draws a thin vertical scroll-thumb on the trailing edge of a plain
 * `verticalScroll` column — a form, a status screen, anything that is a
 * `Column` rather than a lazy list.
 *
 * Put it **before** `verticalScroll` in the chain: this modifier draws over
 * the node it is on, and after `verticalScroll` that node is the whole
 * content, so the thumb would scroll away with it instead of staying in the
 * viewport. Nothing is drawn while the content fits.
 */
fun Modifier.verticalScrollbar(
    state: ScrollState,
    width: Dp = 4.dp,
    minThumbHeight: Dp = 32.dp,
): Modifier = composed {
    val thumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    drawWithContent {
        drawContent()
        val thumb = scrollThumb(
            viewportHeight = size.height,
            maxScroll = state.maxValue.toFloat(),
            scrollOffset = state.value.toFloat(),
            minThumbHeight = minThumbHeight.toPx(),
        ) ?: return@drawWithContent

        drawRoundRect(
            color        = thumbColor,
            topLeft      = Offset(thumbX(layoutDirection, size.width, width.toPx()), thumb.top),
            size         = Size(width.toPx(), thumb.height),
            cornerRadius = CornerRadius(width.toPx() / 2)
        )
    }
}

/**
 * Makes a column scroll vertically **and** shows the thumb while it overflows —
 * the pair every dialog or sheet body that can run past the screen wants.
 *
 * Exists so the order cannot be got wrong: the scrollbar has to come before
 * `verticalScroll` (see [verticalScrollbar]), and a caller writing them out by
 * hand is one swap away from a thumb that scrolls off with the content.
 */
fun Modifier.verticalScrollWithScrollbar(state: ScrollState): Modifier =
    verticalScrollbar(state).verticalScroll(state)

/** Where a scroll thumb is drawn, in pixels from the top of the viewport. */
internal data class ScrollThumb(val top: Float, val height: Float)

/**
 * The thumb for a plain scrolling column, or `null` when there is nothing to
 * scroll and so nothing to draw.
 *
 * The thumb's share of the viewport is the viewport's share of the content
 * (never less than [minThumbHeight], so it stays grabbable by eye on a very
 * long body), and it travels the rest of the viewport in proportion to how far
 * the content has scrolled. Plain numbers in, plain numbers out, so it is
 * checkable without a Skia surface.
 */
internal fun scrollThumb(
    viewportHeight: Float,
    maxScroll: Float,
    scrollOffset: Float,
    minThumbHeight: Float,
): ScrollThumb? {
    if (maxScroll <= 0f || viewportHeight <= 0f) return null
    val contentHeight = viewportHeight + maxScroll
    val height = max(viewportHeight * (viewportHeight / contentHeight), minThumbHeight)
        .coerceAtMost(viewportHeight)
    val top = (scrollOffset / maxScroll).coerceIn(0f, 1f) * (viewportHeight - height)
    return ScrollThumb(top = top, height = height)
}

/**
 * Where the scroll thumb sits across the width.
 *
 * A plain function over plain numbers, so the one thing that can be wrong —
 * which edge it lands on — is checkable without a Skia surface.
 */
internal fun thumbX(direction: LayoutDirection, totalWidth: Float, thumbWidth: Float): Float =
    if (direction == LayoutDirection.Rtl) 0f else totalWidth - thumbWidth
