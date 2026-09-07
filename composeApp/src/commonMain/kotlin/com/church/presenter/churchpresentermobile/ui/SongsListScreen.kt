package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.songs_count
import churchpresentermobile.composeapp.generated.resources.book_filter_all_books
import churchpresentermobile.composeapp.generated.resources.songs_table_no_match
import churchpresentermobile.composeapp.generated.resources.songs_empty_local_library
import churchpresentermobile.composeapp.generated.resources.songs_empty_local_library_body
import churchpresentermobile.composeapp.generated.resources.songs_table_no_songs
import churchpresentermobile.composeapp.generated.resources.songs_table_retry
import churchpresentermobile.composeapp.generated.resources.songs_table_search_placeholder
import com.church.presenter.churchpresentermobile.model.Song
import com.church.presenter.churchpresentermobile.SyncRequestHandler
import com.church.presenter.churchpresentermobile.TabNavigationHandler
import com.church.presenter.churchpresentermobile.model.AppTab
import com.church.presenter.churchpresentermobile.ui.library.SyncSection
import androidx.compose.material.icons.filled.CloudDownload
import churchpresentermobile.composeapp.generated.resources.empty_action_get_songs
import churchpresentermobile.composeapp.generated.resources.empty_action_write_song
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/**
 * Redesigned song list: search field + "ALL SONGS / N songs" overline + song cards
 * (accent number chip + title + book + chevron). Book filtering is preserved by
 * making the overline label a dropdown trigger.
 *
 * Signature is unchanged so [SongsTable] does not need modification.
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
    /** True when the list comes from this device's library rather than a desktop. */
    showsLocalLibrary: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    // Default sort: by song number ascending (design shows a numbered list).
    val sortedSongs = remember(songs) {
        songs.sortedWith(compareBy { it.number.toIntOrNull() ?: Int.MAX_VALUE })
    }

    Column(modifier = modifier.fillMaxWidth().background(colors.background)) {

        // ── Error banner ──────────────────────────────────────────────────
        if (error != null) ErrorBanner(error, onRefresh)

        // ── Search ────────────────────────────────────────────────────────
        SearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = stringResource(Res.string.songs_table_search_placeholder),
            modifier = Modifier
                .testTag(UiTags.SONGS_SEARCH)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // ── Overline: ALL SONGS / book filter  ·  N songs ────────────────
        SongsOverline(
            selectedBook = selectedBook,
            availableBooks = availableBooks,
            count = sortedSongs.size,
            onBookSelected = onBookSelected,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        // ── List body ─────────────────────────────────────────────────────
        val listState = rememberLazyListState()
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            when {
                sortedSongs.isNotEmpty() -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().verticalScrollbar(listState),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        items(sortedSongs) { song ->
                            SongCard(
                                song = song,
                                isSelected = selectedSong?.number == song.number
                                        && selectedSong?.bookName == song.bookName,
                                onClick = { onSongClick(song) }
                            )
                        }
                    }
                }
                // A local library with nothing in it is not an error — it is a phone that
                // has not been given any songs yet, and the tab says how to fix that.
                showsLocalLibrary && !hasActiveFilter -> EmptyState(
                    modifier = Modifier.testTag(UiTags.SONGS_EMPTY_LOCAL_LIBRARY),
                    title = stringResource(Res.string.songs_empty_local_library),
                    body = stringResource(Res.string.songs_empty_local_library_body),
                    actionLabel = stringResource(Res.string.empty_action_get_songs),
                    actionIcon = Icons.Filled.CloudDownload,
                    onAction = {
                        SyncRequestHandler.request(SyncSection.SONGS)
                        TabNavigationHandler.navigateTo(AppTab.LIBRARY)
                    },
                    secondaryLabel = stringResource(Res.string.empty_action_write_song),
                    onSecondary = { TabNavigationHandler.navigateTo(AppTab.LIBRARY) },
                )
                !isLoading -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(
                            if (hasActiveFilter) UiTags.SONGS_EMPTY_NO_MATCH
                            else UiTags.SONGS_EMPTY_NO_SONGS
                        )
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (hasActiveFilter) stringResource(Res.string.songs_table_no_match)
                               else stringResource(Res.string.songs_table_no_songs),
                        color = colors.muted,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}

/**
 * What went wrong, and the one thing that might fix it.
 *
 * Sits above the list rather than replacing it: a blip mid-service must not
 * blank a catalogue the operator is working from.
 */
@Composable
private fun ErrorBanner(error: String, onRefresh: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(UiTags.SONGS_ERROR)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.danger.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = error,
            color = colors.danger,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stringResource(Res.string.songs_table_retry),
            color = colors.danger,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .testTag(UiTags.SONGS_RETRY)
                .padding(start = 12.dp)
                .clickable { onRefresh() }
        )
    }
}

@Composable
private fun SongsOverline(
    selectedBook: String?,
    availableBooks: List<String>,
    count: Int,
    onBookSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    var expanded by remember { mutableStateOf(false) }
    val allBooksLabel = stringResource(Res.string.book_filter_all_books)
    val label = (selectedBook ?: allBooksLabel).uppercase()

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box {
            Row(
                modifier = Modifier
                    .testTag(UiTags.SONGS_BOOK_FILTER)
                    .clickable(enabled = availableBooks.isNotEmpty()) { expanded = true },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    color = if (selectedBook != null) colors.accent else colors.muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.05.em,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(allBooksLabel, fontWeight = if (selectedBook == null) FontWeight.Bold else FontWeight.Normal) },
                    onClick = { onBookSelected(null); expanded = false },
                    modifier = Modifier.testTag(UiTags.bookOption(null)),
                )
                availableBooks.forEach { book ->
                    DropdownMenuItem(
                        text = { Text(book, fontWeight = if (book == selectedBook) FontWeight.Bold else FontWeight.Normal) },
                        onClick = { onBookSelected(book); expanded = false },
                        modifier = Modifier.testTag(UiTags.bookOption(book)),
                    )
                }
            }
        }
        Text(
            text = stringResource(Res.string.songs_count, count),
            color = colors.muted,
            fontSize = 12.sp,
            modifier = Modifier.testTag(UiTags.SONGS_COUNT),
        )
    }
}

@Composable
private fun SongCard(
    song: Song,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(UiTags.songCard(song.number, song.bookName))
            .selectable(selected = isSelected, onClick = onClick)
            .clip(shape)
            .background(if (isSelected) colors.accentTint else colors.surface)
            .border(1.dp, if (isSelected) colors.accent else colors.borderSubtle, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Number chip
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(colors.accentTint)
                .border(1.dp, colors.accent.copy(alpha = 0.5f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = song.number,
                color = colors.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = colors.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.015).em,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!song.bookName.isNullOrBlank()) {
                Text(
                    text = song.bookName!!,
                    color = colors.muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.dim,
            modifier = Modifier.size(20.dp)
        )
    }
}
