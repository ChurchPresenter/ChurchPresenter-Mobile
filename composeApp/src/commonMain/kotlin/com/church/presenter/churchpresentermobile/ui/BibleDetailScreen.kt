package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.bible_multi_select_count
import churchpresentermobile.composeapp.generated.resources.bible_no_verses
import com.church.presenter.churchpresentermobile.model.BibleBook
import com.church.presenter.churchpresentermobile.model.BibleVerse
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/**
 * Shows either a chapter-number grid (when [selectedChapter] is null)
 * or a scrollable verse list with multi-select and a FAB speed dial.
 *
 * Verses are always tappable — tapping toggles them in/out of [selectedVerseIndices].
 * While [isProjecting] is true, tapping a verse also immediately projects it.
 * The verse currently on screen is marked by [projectedVerseIndex].
 */
@Composable
fun BibleDetailScreen(
    book: BibleBook,
    selectedChapter: Int?,
    verses: List<BibleVerse>,
    isLoading: Boolean = false,
    isProjecting: Boolean,
    isHolding: Boolean = false,
    scheduleAdded: Boolean,
    selectedVerseIndices: Set<Int>,
    projectedVerseIndex: Int?,
    isMultiSelectMode: Boolean = false,
    onToggleMultiSelect: () -> Unit = {},
    onChapterSelect: (Int) -> Unit,
    onVerseToggleSelection: (Int) -> Unit,
    onToggleProjecting: () -> Unit,
    onToggleHold: () -> Unit = {},
    onClearDisplay: () -> Unit = {},
    onAddToSchedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    if (selectedChapter != null) {
        val totalChapters = if (book.totalChapters > 0) book.totalChapters else 150
        val pagerState = rememberPagerState(
            initialPage = (selectedChapter - 1).coerceIn(0, totalChapters - 1)
        ) { totalChapters }

        // Always holds the latest selectedChapter value inside coroutines
        val currentChapterRef = rememberUpdatedState(selectedChapter)

        // Sync pager to the chapter when it changes externally (e.g. schedule nav)
        LaunchedEffect(selectedChapter) {
            val target = (selectedChapter - 1).coerceIn(0, totalChapters - 1)
            if (pagerState.currentPage != target) {
                pagerState.animateScrollToPage(target)
            }
        }

        // Load a new chapter when the user swipes to a different page
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }.collect { page ->
                val newChapter = page + 1
                if (newChapter != currentChapterRef.value) {
                    onChapterSelect(newChapter)
                }
            }
        }

        Box(modifier = modifier.fillMaxSize()) {
            // ── Swipeable chapter pages ────────────────────────────────────
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val isCurrentPage = page + 1 == currentChapterRef.value
                if (isCurrentPage && (!isLoading || verses.isNotEmpty())) {
                    VersesList(
                        verses               = verses,
                        isProjecting         = isProjecting,
                        isMultiSelectMode    = isMultiSelectMode,
                        selectedVerseIndices = selectedVerseIndices,
                        projectedVerseIndex  = projectedVerseIndex,
                        onVerseToggle        = onVerseToggleSelection,
                        modifier             = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier         = Modifier.fillMaxSize().background(colors.background),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colors.accent)
                    }
                }
            }

            // ── Action buttons (bottom-right, overlaid on the pager) ──────
            ContentActionButtons(
                isProjecting       = isProjecting,
                scheduleAdded      = scheduleAdded,
                onToggleProjecting = onToggleProjecting,
                onAddToSchedule    = onAddToSchedule,
                modifier           = Modifier.align(Alignment.BottomEnd),
                castBadgeCount     = selectedVerseIndices.size,
                isHolding          = isHolding,
                onToggleHold       = onToggleHold,
                onClearDisplay     = onClearDisplay,
                isMultiSelectMode  = isMultiSelectMode,
                onToggleMultiSelect = onToggleMultiSelect,
            )
        }
    } else {
        ChaptersGrid(
            book            = book,
            onChapterSelect = onChapterSelect,
            modifier        = modifier
        )
    }
}


@Composable
private fun ChaptersGrid(
    book: BibleBook,
    onChapterSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val count = if (book.totalChapters > 0) book.totalChapters else 150
    val gridState = rememberLazyGridState()
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        state = gridState,
        modifier = modifier.fillMaxSize().background(colors.background).verticalScrollbar(gridState),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(count) { index ->
            val chapter = index + 1
            Box(
                modifier = Modifier
                    .height(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
                    .clickable { onChapterSelect(chapter) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$chapter",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.text
                )
            }
        }
    }
}

@Composable
private fun VersesList(
    verses: List<BibleVerse>,
    isProjecting: Boolean,
    isMultiSelectMode: Boolean,
    selectedVerseIndices: Set<Int>,
    projectedVerseIndex: Int?,
    onVerseToggle: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    if (verses.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().background(colors.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(Res.string.bible_no_verses),
                color = colors.muted,
                fontSize = 15.sp
            )
        }
        return
    }

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        if (isMultiSelectMode && selectedVerseIndices.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.bible_multi_select_count, selectedVerseIndices.size),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.accentTint)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        val listState = rememberLazyListState()
        LazyColumn(
            state          = listState,
            modifier       = Modifier.weight(1f).fillMaxWidth().verticalScrollbar(listState),
            contentPadding = PaddingValues(bottom = 200.dp) // clear FABs above snackbar
        ) {
            itemsIndexed(verses) { index, verse ->
                val isSelected  = index in selectedVerseIndices
                val isProjected = isProjecting && projectedVerseIndex == index
                val highlighted = isProjected || isSelected
                val leftBorderColor = if (highlighted) colors.accent else androidx.compose.ui.graphics.Color.Transparent

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (highlighted) colors.accentTint else colors.background)
                        .drawBehind {
                            drawRect(
                                color = leftBorderColor,
                                size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
                            )
                        }
                        .clickable { onVerseToggle(index) }
                        .padding(horizontal = 20.dp, vertical = 13.dp),
                    verticalAlignment   = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text  = "${verse.number}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = colors.accent,
                        modifier = Modifier.padding(top = 2.dp).widthIn(min = 18.dp)
                    )
                    Text(
                        text  = verse.displayText,
                        fontSize = 14.sp,
                        lineHeight = (14 * 1.7).sp,
                        color = if (highlighted) colors.text else colors.secondary,
                    )
                }
                HorizontalDivider(color = colors.borderSubtle)
            }
        }
    }
}
