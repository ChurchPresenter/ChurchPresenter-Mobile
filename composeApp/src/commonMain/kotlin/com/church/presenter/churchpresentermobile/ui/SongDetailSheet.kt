package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.song_detail_no_lyrics
import churchpresentermobile.composeapp.generated.resources.song_detail_projecting_badge
import com.church.presenter.churchpresentermobile.model.SongDetail
import com.church.presenter.churchpresentermobile.model.SongVerse
import org.jetbrains.compose.resources.stringResource

/**
 * Full-screen song detail view with selectable verse cards.
 *
 * @param detail              The loaded [SongDetail], or null while loading.
 * @param isLoading           True while the detail request is in flight.
 * @param error               Non-null if the request failed.
 * @param selectedVerseIndex  The currently projected verse index, or null.
 * @param isProjecting        True when the user has enabled "Project to Screen".
 * @param onVerseSelected     Called with the 0-based verse index when the user taps a verse.
 * @param onToggleProjecting  Called when the user taps the "Project to Screen" mini-FAB.
 * @param onAddToSchedule     Called when the user taps the "Add to Schedule" mini-FAB.
 */
@Composable
fun SongDetailScreen(
    detail: SongDetail?,
    isLoading: Boolean,
    error: String?,
    selectedVerseIndex: Int?,
    isProjecting: Boolean,
    scheduleAdded: Boolean,
    onVerseSelected: (Int) -> Unit,
    onToggleProjecting: () -> Unit,
    onAddToSchedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {

        // ── Main content ──────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(44.dp))
                }

                error != null -> Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                detail != null -> {
                    if (detail.allVerses.isNotEmpty()) {
                        val versesState = rememberLazyListState()
                        LazyColumn(
                            state = versesState,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(if (isProjecting) 1f else 0.6f)
                                .verticalScrollbar(versesState),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp, top = 16.dp, bottom = 200.dp
                            )
                        ) {
                            itemsIndexed(detail.allVerses) { index, verse ->
                                VerseCard(
                                    verse = verse,
                                    index = index,
                                    isSelected = isProjecting && selectedVerseIndex == index,
                                    isProjecting = isProjecting,
                                    onClick = { onVerseSelected(index) }
                                )
                            }
                        }
                    } else if (!detail.plainText.isNullOrBlank()) {
                        val plainState = rememberLazyListState()
                        LazyColumn(
                            state = plainState,
                            modifier = Modifier.fillMaxSize().verticalScrollbar(plainState),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            item {
                                Text(
                                    text = detail.plainText!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = MaterialTheme.typography.bodyMedium.fontSize * 1.6
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(Res.string.song_detail_no_lyrics),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }

        // ── Action buttons (bottom-right, above snackbar) ─────────────────
        ContentActionButtons(
            isProjecting       = isProjecting,
            scheduleAdded      = scheduleAdded,
            onToggleProjecting = onToggleProjecting,
            onAddToSchedule    = onAddToSchedule,
            modifier           = Modifier.align(Alignment.BottomEnd),
        )
    }
}


@Composable
private fun VerseCard(
    verse: SongVerse,
    index: Int,
    isSelected: Boolean,
    isProjecting: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    val borderColor = if (isSelected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outlineVariant

    val textColor = if (isSelected)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = isProjecting) { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                val label = verse.displayLabel ?: (index + 1).toString()
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = stringResource(Res.string.song_detail_projecting_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = verse.displayText,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                lineHeight = MaterialTheme.typography.bodyMedium.fontSize * 1.7
            )
        }
    }
}
