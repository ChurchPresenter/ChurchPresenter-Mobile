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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.bible_chapters_title
import churchpresentermobile.composeapp.generated.resources.bible_no_verses
import churchpresentermobile.composeapp.generated.resources.bible_detail_add_to_schedule
import churchpresentermobile.composeapp.generated.resources.bible_detail_project_to_screen
import churchpresentermobile.composeapp.generated.resources.bible_detail_projecting_badge
import churchpresentermobile.composeapp.generated.resources.bible_detail_stop_projecting
import com.church.presenter.churchpresentermobile.model.BibleBook
import com.church.presenter.churchpresentermobile.model.BibleVerse
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
    scheduleAdded: Boolean,
    selectedVerseIndices: Set<Int>,
    projectedVerseIndex: Int?,
    onChapterSelect: (Int) -> Unit,
    onVerseToggleSelection: (Int) -> Unit,
    onToggleProjecting: () -> Unit,
    onAddToSchedule: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                        selectedVerseIndices = selectedVerseIndices,
                        projectedVerseIndex  = projectedVerseIndex,
                        onVerseToggle        = onVerseToggleSelection,
                        modifier             = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier         = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // ── Action buttons (bottom-right, overlaid on the pager) ──────
            val selectionCount = selectedVerseIndices.size

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 72.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Add to Schedule button
                FloatingActionButton(
                    onClick = { if (!scheduleAdded) onAddToSchedule() },
                    containerColor = if (scheduleAdded)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (scheduleAdded)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = stringResource(Res.string.bible_detail_add_to_schedule)
                    )
                }

                // Project to Screen / Stop Projecting button (with selection badge)
                Box {
                    FloatingActionButton(
                        onClick = { onToggleProjecting() },
                        containerColor = if (isProjecting)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isProjecting)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.Cast,
                            contentDescription = if (isProjecting)
                                stringResource(Res.string.bible_detail_stop_projecting)
                            else
                                stringResource(Res.string.bible_detail_project_to_screen)
                        )
                    }
                    // Selection count badge
                    if (selectionCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.TopEnd)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text  = "$selectionCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onError
                                )
                            }
                        }
                    }
                }
            }
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
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(Res.string.bible_chapters_title),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

        val count = if (book.totalChapters > 0) book.totalChapters else 150
        val gridState = rememberLazyGridState()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 64.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize().verticalScrollbar(gridState),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(count) { index ->
                    val chapter = index + 1
                    Surface(
                        modifier = Modifier.clickable { onChapterSelect(chapter) },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 1.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "$chapter",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VersesList(
    verses: List<BibleVerse>,
    isProjecting: Boolean,
    selectedVerseIndices: Set<Int>,
    projectedVerseIndex: Int?,
    onVerseToggle: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (verses.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text  = stringResource(Res.string.bible_no_verses),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val listState = rememberLazyListState()
    LazyColumn(
        state          = listState,
        modifier       = modifier.fillMaxSize().verticalScrollbar(listState),
        contentPadding = PaddingValues(bottom = 200.dp) // clear both action buttons above snackbar
    ) {
        itemsIndexed(verses) { index, verse ->
            val isSelected  = index in selectedVerseIndices
            val isProjected = isProjecting && projectedVerseIndex == index

            val bgColor = when {
                isProjected -> MaterialTheme.colorScheme.primaryContainer
                isSelected  -> MaterialTheme.colorScheme.secondaryContainer
                else        -> MaterialTheme.colorScheme.surface
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor)
                    .then(
                        if (isProjected) Modifier.border(
                            width  = 2.dp,
                            color  = MaterialTheme.colorScheme.primary,
                            shape  = RoundedCornerShape(0.dp)
                        ) else Modifier
                    )
                    .clickable { onVerseToggle(index) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment   = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Verse number column — always show the number; selection shown via row background
                Box(
                    modifier         = Modifier.padding(top = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "${verse.number}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isProjected -> MaterialTheme.colorScheme.primary
                            isSelected  -> MaterialTheme.colorScheme.secondary
                            else        -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        }
                    )
                }

                // Verse text column
                Column {
                    Text(
                        text  = verse.displayText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            isProjected -> MaterialTheme.colorScheme.onPrimaryContainer
                            isSelected  -> MaterialTheme.colorScheme.onSecondaryContainer
                            else        -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (isProjected) {
                        Surface(
                            shape    = RoundedCornerShape(4.dp),
                            color    = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text     = stringResource(Res.string.bible_detail_projecting_badge),
                                style    = MaterialTheme.typography.labelSmall,
                                color    = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        }
    }
}
