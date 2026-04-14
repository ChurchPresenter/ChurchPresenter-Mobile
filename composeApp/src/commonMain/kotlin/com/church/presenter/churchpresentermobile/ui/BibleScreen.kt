package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.bible_loading_error
import churchpresentermobile.composeapp.generated.resources.bible_no_books
import churchpresentermobile.composeapp.generated.resources.bible_no_match
import churchpresentermobile.composeapp.generated.resources.bible_retry
import churchpresentermobile.composeapp.generated.resources.bible_search_clear
import churchpresentermobile.composeapp.generated.resources.bible_search_placeholder
import churchpresentermobile.composeapp.generated.resources.toast_bible_added_to_schedule
import churchpresentermobile.composeapp.generated.resources.toast_bible_live
import churchpresentermobile.composeapp.generated.resources.toast_failed_to_add_bible_schedule
import churchpresentermobile.composeapp.generated.resources.toast_failed_to_project_bible
import churchpresentermobile.composeapp.generated.resources.toast_request_denied
import churchpresentermobile.composeapp.generated.resources.toast_request_rejected
import churchpresentermobile.composeapp.generated.resources.toast_request_rejected_reason
import churchpresentermobile.composeapp.generated.resources.toast_session_blocked
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.BibleBook
import com.church.presenter.churchpresentermobile.model.ToastEvent
import com.church.presenter.churchpresentermobile.viewmodel.BibleViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Coordinator for the Bible tab.
 *
 * Owns the [BibleViewModel] and switches between:
 * - [BibleBooksScreen] — searchable list of all Bible books (root)
 * - [BibleDetailScreen] — chapter grid → verse list (when a book is selected)
 *
 * Toolbar title, back-arrow, and tab visibility are controlled by the parent
 * App scaffold via [onNavigationChanged] and [onRegisterBackAction].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleScreen(
    appSettings: AppSettings,
    isDemoMode: Boolean = false,
    settingsSaveToken: Int,
    onNavigationChanged: (book: BibleBook?, chapter: Int?) -> Unit,
    onRegisterBackAction: (action: (() -> Unit)?) -> Unit,
    pendingNavBookName: String? = null,
    pendingNavChapter: Int? = null,
    pendingNavVerses: Set<Int> = emptySet(),
    onPendingNavHandled: () -> Unit = {},
    onScheduleRefresh: () -> Unit = {},
    providedViewModel: BibleViewModel? = null,
    modifier: Modifier = Modifier
) {
    // Use the session-scoped ViewModel passed from App.kt when available.
    // The internal fallback is only here so the composable still works in
    // isolation (e.g. Compose Previews or tests).
    val vm: BibleViewModel = providedViewModel
        ?: viewModel(key = isDemoMode.toString()) {
            BibleViewModel(appSettings, isDemoMode)
        }

    LaunchedEffect(settingsSaveToken) {
        if (settingsSaveToken > 0) vm.onSettingsSaved(settingsSaveToken)
    }

    val books               by vm.books.collectAsState()
    val bookSearchQuery     by vm.bookSearchQuery.collectAsState()
    val selectedBook        by vm.selectedBook.collectAsState()
    val selectedChapter     by vm.selectedChapter.collectAsState()
    val verses              by vm.verses.collectAsState()
    val isLoading           by vm.isLoading.collectAsState()
    val error               by vm.error.collectAsState()
    val isProjecting        by vm.isProjecting.collectAsState()
    val selectedVerseIndices by vm.selectedVerseIndices.collectAsState()
    val projectedVerseIndex  by vm.projectedVerseIndex.collectAsState()
    val scheduleAdded       by vm.scheduleAdded.collectAsState()
    val scheduleRefreshTrigger by vm.scheduleRefreshTrigger.collectAsState()
    val toastEvent          by vm.toastEvent.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(scheduleRefreshTrigger) {
        if (scheduleRefreshTrigger > 0) onScheduleRefresh()
    }

    // Resolve toast events to localised strings in composable scope
    val toastMessage = toastEvent?.toDisplayString()
    LaunchedEffect(toastEvent) {
        if (toastMessage != null) {
            snackbarHostState.showSnackbar(message = toastMessage, duration = SnackbarDuration.Short)
            vm.toastShown()
        }
    }

    val currentOnNavigationChanged by rememberUpdatedState(onNavigationChanged)
    LaunchedEffect(selectedBook, selectedChapter) {
        currentOnNavigationChanged(selectedBook, selectedChapter)
    }

    val currentOnRegisterBackAction by rememberUpdatedState(onRegisterBackAction)
    DisposableEffect(selectedBook) {
        if (selectedBook != null) {
            currentOnRegisterBackAction { vm.navigateBack() }
        } else {
            currentOnRegisterBackAction(null)
        }
        onDispose { currentOnRegisterBackAction(null) }
    }

    // Intercept the system back button when a book (or chapter) is open
    AppBackHandler(enabled = selectedBook != null) {
        vm.navigateBack()
    }

    // ── Schedule-driven navigation ────────────────────────────────────────
    LaunchedEffect(pendingNavBookName, pendingNavChapter) {
        if (pendingNavBookName != null && pendingNavChapter != null) {
            vm.navigateToBookAndChapter(
                bookName     = pendingNavBookName,
                chapter      = pendingNavChapter,
                verseNumbers = pendingNavVerses
            )
            onPendingNavHandled()
        }
    }

    // ── Error banner ──────────────────────────────────────────────────────
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                            text = error ?: stringResource(Res.string.bible_loading_error),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { vm.refresh() }) {
                            Text(
                                text = stringResource(Res.string.bible_retry),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // ── Switch between books list and detail ──────────────────────
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = { vm.refresh() },
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                if (selectedBook != null) {
                    BibleDetailScreen(
                        book = selectedBook!!,
                        selectedChapter = selectedChapter,
                        verses = verses,
                        isLoading = isLoading,
                        isProjecting = isProjecting,
                        scheduleAdded = scheduleAdded,
                        selectedVerseIndices = selectedVerseIndices,
                        projectedVerseIndex = projectedVerseIndex,
                        onChapterSelect = { vm.selectChapter(it) },
                        onVerseToggleSelection = { vm.toggleVerseSelection(it) },
                        onToggleProjecting = { vm.toggleProjecting() },
                        onAddToSchedule = { vm.addToSchedule() },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    BibleBooksScreen(
                        books = books,
                        isLoading = isLoading,
                        searchQuery = bookSearchQuery,
                        onSearchQueryChange = { vm.setBookSearchQuery(it) },
                        onBookSelect = { vm.selectBook(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/** Resolves a [ToastEvent] to a localised display string using Compose string resources. */
@Composable
private fun ToastEvent.toDisplayString(): String = when (this) {
    is ToastEvent.BibleLive                 -> stringResource(Res.string.toast_bible_live)
    is ToastEvent.BibleAddedToSchedule      -> stringResource(Res.string.toast_bible_added_to_schedule, reference)
    is ToastEvent.FailedToProjectBible      -> stringResource(Res.string.toast_failed_to_project_bible, reason)
    is ToastEvent.FailedToAddBibleSchedule  -> stringResource(Res.string.toast_failed_to_add_bible_schedule, reason)
    is ToastEvent.RequestDenied             -> stringResource(Res.string.toast_request_denied)
    is ToastEvent.SessionBlocked            -> stringResource(Res.string.toast_session_blocked)
    is ToastEvent.RequestRejected           -> stringResource(Res.string.toast_request_rejected, httpStatus.toString())
    is ToastEvent.RequestRejectedWithReason -> stringResource(Res.string.toast_request_rejected_reason, reason)
    else                                    -> ""
}

/**
 * Searchable list of Bible books.
 * Pure UI composable — no ViewModel dependency.
 */
@Composable
fun BibleBooksScreen(
    books: List<BibleBook>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBookSelect: (BibleBook) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            placeholder = {
                Text(
                    text = stringResource(Res.string.bible_search_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            text = stringResource(Res.string.bible_search_clear),
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

        when {
            books.isEmpty() && searchQuery.isNotEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.bible_no_match),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            books.isEmpty() && !isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.bible_no_books),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            books.isNotEmpty() -> {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().verticalScrollbar(listState)
                ) {
                    items(books) { book ->
                        BibleBookRow(book = book, onSelect = { onBookSelect(book) })
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BibleBookRow(book: BibleBook, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = book.displayName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (book.totalChapters > 0) {
            Text(
                text = "${book.totalChapters} ch",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = " ›",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
