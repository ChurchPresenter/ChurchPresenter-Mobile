package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.book_filter_all_books
import churchpresentermobile.composeapp.generated.resources.book_filter_dropdown_arrow
import churchpresentermobile.composeapp.generated.resources.songs_table_clear_search
import churchpresentermobile.composeapp.generated.resources.songs_table_header_book
import churchpresentermobile.composeapp.generated.resources.songs_table_header_number
import churchpresentermobile.composeapp.generated.resources.songs_table_header_title
import churchpresentermobile.composeapp.generated.resources.songs_table_no_match
import churchpresentermobile.composeapp.generated.resources.songs_table_no_songs
import churchpresentermobile.composeapp.generated.resources.songs_table_retry
import churchpresentermobile.composeapp.generated.resources.songs_table_search_placeholder
import com.church.presenter.churchpresentermobile.model.Song
import org.jetbrains.compose.resources.stringResource

// ── Sort state ────────────────────────────────────────────────────────────────
private enum class SortColumn { NUMBER, TITLE, BOOK }

/**
 * Pure UI composable — the searchable, filterable song list table.
 * No ViewModel dependency; all state is passed in as parameters.
 *
 * @param songs          The filtered list of songs to display.
 * @param selectedSong   The currently selected/highlighted song, or null.
 * @param isLoading      True while songs are being fetched (drives pull-to-refresh).
 * @param error          Non-null error message to show in the banner.
 * @param searchQuery    Current text in the search field.
 * @param selectedBook   Currently active book filter, or null for all books.
 * @param availableBooks All distinct book names for the filter dropdown.
 * @param hasActiveFilter True when any search/filter is active.
 * @param onSearchQueryChange Called when the user types in the search field.
 * @param onBookSelected  Called when the user picks a book filter (null = all books).
 * @param onSongClick     Called when the user taps a song row.
 * @param onRefresh       Called when the user pulls to refresh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsListScreen(
    songs: List<Song>,
    selectedSong: Song?,
    isLoading: Boolean,
    error: String?,
    searchQuery: String,
    selectedBook: String?,
    availableBooks: List<String>,
    hasActiveFilter: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onBookSelected: (String?) -> Unit,
    onSongClick: (Song) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ── Sort state ────────────────────────────────────────────────────────
    var sortColumn by remember { mutableStateOf(SortColumn.NUMBER) }
    var sortAscending by remember { mutableStateOf(true) }
    val sortedSongs = remember(songs, sortColumn, sortAscending) {
        val base = when (sortColumn) {
            SortColumn.NUMBER -> songs.sortedWith(
                compareBy { it.number.toIntOrNull() ?: Int.MAX_VALUE }
            )
            SortColumn.TITLE -> songs.sortedBy { it.title.lowercase() }
            SortColumn.BOOK  -> songs.sortedBy { it.bookName?.lowercase() ?: "" }
        }
        if (sortAscending) base else base.reversed()
    }

    val chipMaxWidth = 120.dp

    Column(modifier = modifier.fillMaxWidth()) {

        // ── Error banner ──────────────────────────────────────────────────
        if (error != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onRefresh) {
                        Text(
                            text = stringResource(Res.string.songs_table_retry),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // ── Search + book filter ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = stringResource(Res.string.songs_table_search_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Text("🔍", fontSize = 16.sp, modifier = Modifier.padding(start = 4.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.songs_table_clear_search),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(50.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                textStyle = MaterialTheme.typography.bodyMedium
            )

            if (availableBooks.isNotEmpty()) {
                SongBookFilterDropdown(
                    books = availableBooks,
                    selectedBook = selectedBook,
                    onBookSelected = onBookSelected,
                    maxWidth = chipMaxWidth
                )
            }
        }

        // ── Table header ──────────────────────────────────────────────────
        if (!isLoading && sortedSongs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sortable: Number
                Row(
                    modifier = Modifier
                        .weight(0.2f)
                        .clickable {
                            if (sortColumn == SortColumn.NUMBER) sortAscending = !sortAscending
                            else { sortColumn = SortColumn.NUMBER; sortAscending = true }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.songs_table_header_number),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (sortColumn == SortColumn.NUMBER) {
                        Text(
                            text = if (sortAscending) "↑" else "↓",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                // Sortable: Title
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (sortColumn == SortColumn.TITLE) sortAscending = !sortAscending
                            else { sortColumn = SortColumn.TITLE; sortAscending = true }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.songs_table_header_title),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (sortColumn == SortColumn.TITLE) {
                        Text(
                            text = if (sortAscending) "↑" else "↓",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                // Sortable: Book
                Row(
                    modifier = Modifier
                        .weight(0.4f)
                        .clickable {
                            if (sortColumn == SortColumn.BOOK) sortAscending = !sortAscending
                            else { sortColumn = SortColumn.BOOK; sortAscending = true }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.songs_table_header_book),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (sortColumn == SortColumn.BOOK) {
                        Text(
                            text = if (sortAscending) "↑" else "↓",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        }

        // ── List body ─────────────────────────────────────────────────────
        val listState = rememberLazyListState()
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            when {
                songs.isNotEmpty() -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().verticalScrollbar(listState)
                    ) {
                        items(sortedSongs) { song ->
                            SongRow(
                                song = song,
                                isSelected = selectedSong?.number == song.number
                                        && selectedSong?.bookName == song.bookName,
                                onClick = { onSongClick(song) }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                !isLoading -> Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (hasActiveFilter)
                            stringResource(Res.string.songs_table_no_match)
                        else
                            stringResource(Res.string.songs_table_no_songs),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────────

@Composable
private fun SongBookFilterDropdown(
    books: List<String>,
    selectedBook: String?,
    onBookSelected: (String?) -> Unit,
    maxWidth: Dp = Dp.Unspecified
) {
    var expanded by remember { mutableStateOf(false) }
    val allBooksLabel = stringResource(Res.string.book_filter_all_books)
    val dropdownArrow = stringResource(Res.string.book_filter_dropdown_arrow)

    Box(modifier = if (maxWidth != Dp.Unspecified) Modifier.widthIn(max = maxWidth) else Modifier) {
        FilterChip(
            selected = selectedBook != null,
            onClick = { expanded = true },
            label = {
                Text(
                    text = (selectedBook ?: allBooksLabel) + dropdownArrow,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = {
                    Text(
                        allBooksLabel,
                        fontWeight = if (selectedBook == null) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = { onBookSelected(null); expanded = false }
            )
            HorizontalDivider()
            books.forEach { book ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = book,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (book == selectedBook) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = { onBookSelected(book); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun SongRow(
    song: Song,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = song.number,
            modifier = Modifier.weight(0.2f).padding(end = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = song.title,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = song.bookName ?: "",
            modifier = Modifier.weight(0.4f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
