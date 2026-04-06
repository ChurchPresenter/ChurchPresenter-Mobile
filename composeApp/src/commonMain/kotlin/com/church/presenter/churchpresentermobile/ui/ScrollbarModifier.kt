package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Draws a thin vertical scroll-thumb on the right edge of a [LazyColumn].
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
            topLeft     = Offset(size.width - width.toPx(), thumbTop),
            size        = Size(width.toPx(), thumbH),
            cornerRadius = CornerRadius(width.toPx() / 2)
        )
    }
}

/**
 * Draws a thin vertical scroll-thumb on the right edge of a [LazyVerticalGrid].
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
            topLeft      = Offset(size.width - width.toPx(), thumbTop),
            size         = Size(width.toPx(), thumbH),
            cornerRadius = CornerRadius(width.toPx() / 2)
        )
    }
}

