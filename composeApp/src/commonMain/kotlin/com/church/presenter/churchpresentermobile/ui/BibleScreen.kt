package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.church.presenter.churchpresentermobile.SyncRequestHandler
import com.church.presenter.churchpresentermobile.TabNavigationHandler
import com.church.presenter.churchpresentermobile.model.AppTab
import com.church.presenter.churchpresentermobile.ui.library.SyncSection
import androidx.compose.material.icons.filled.CloudDownload
import churchpresentermobile.composeapp.generated.resources.empty_action_get_bible
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.bible_chapters_count
import churchpresentermobile.composeapp.generated.resources.bible_loading_error
import churchpresentermobile.composeapp.generated.resources.bible_standalone_empty_body
import churchpresentermobile.composeapp.generated.resources.bible_standalone_empty_title
import churchpresentermobile.composeapp.generated.resources.bible_no_books
import churchpresentermobile.composeapp.generated.resources.bible_no_match
import churchpresentermobile.composeapp.generated.resources.bible_retry
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
import com.church.presenter.churchpresentermobile.network.ServerEventService
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
            BibleViewModel(appSettings, ServerEventService(appSettings), isDemoMode)
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
    val isHolding           by vm.isHolding.collectAsState()
    val selectedVerseIndices by vm.selectedVerseIndices.collectAsState()
    val projectedVerseIndex  by vm.projectedVerseIndex.collectAsState()
    val isMultiSelectMode   by vm.isMultiSelectMode.collectAsState()
    val hasNoLocalBibles    by vm.hasNoLocalBibles.collectAsState()
    val scheduleAdded       by vm.scheduleAdded.collectAsState()
    val scheduleRefreshTrigger by vm.scheduleRefreshTrigger.collectAsState()
    val toastEvent          by vm.toastEvent.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(scheduleRefreshTrigger) {
        if (scheduleRefreshTrigger > 0) onScheduleRefresh()
    }

    // Resolve toast events to localised strings in composable scope
    val toastMessage = toastEvent?.bibleToastMessage()
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

    val colors = LocalAppColors.current

    // Standalone reads a translation copied onto this device. With none copied yet the tab
    // offers the one thing that fixes that, rather than naming a mode the operator would have
    // to switch to.
    if (hasNoLocalBibles) {
        Box(modifier = modifier.fillMaxSize().background(colors.background)) {
            EmptyState(
                title = stringResource(Res.string.bible_standalone_empty_title),
                body = stringResource(Res.string.bible_standalone_empty_body),
                actionLabel = stringResource(Res.string.empty_action_get_bible),
                actionIcon = Icons.Filled.CloudDownload,
                onAction = {
                    // The Library tab owns the sheet; ask it to open on the Bible half.
                    SyncRequestHandler.request(SyncSection.BIBLE)
                    TabNavigationHandler.navigateTo(AppTab.LIBRARY)
                },
            )
        }
        return
    }

    // ── Error banner ──────────────────────────────────────────────────────
    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (error != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.danger.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = error ?: stringResource(Res.string.bible_loading_error),
                        color = colors.danger,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(Res.string.bible_retry),
                        color = colors.danger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 12.dp).clickable { vm.refresh() }
                    )
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
                        isHolding = isHolding,
                        scheduleAdded = scheduleAdded,
                        selectedVerseIndices = selectedVerseIndices,
                        projectedVerseIndex = projectedVerseIndex,
                        isMultiSelectMode = isMultiSelectMode,
                        onToggleMultiSelect = { vm.toggleMultiSelectMode() },
                        onChapterSelect = { vm.selectChapter(it) },
                        onVerseToggleSelection = { vm.toggleVerseSelection(it) },
                        onToggleProjecting = { vm.toggleProjecting() },
                        onToggleHold = { vm.toggleHold() },
                        onClearDisplay = { vm.clearDisplay() },
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

/**
 * Resolves a [ToastEvent] to a localised display string.
 *
 * `internal` and named after the tab it belongs to: three tabs each had a
 * private extension of the same name, and none of them could be tested. The
 * branch that matters is the last one — an event this tab does not handle
 * resolves to an empty string, which shows as an empty snackbar.
 */
@Composable
internal fun ToastEvent.bibleToastMessage(): String = when (this) {
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
    val colors = LocalAppColors.current
    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        SearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = stringResource(Res.string.bible_search_placeholder),
            modifier = Modifier
                .testTag(UiTags.BIBLE_SEARCH)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        when {
            books.isEmpty() && searchQuery.isNotEmpty() -> Box(
                modifier = Modifier.fillMaxSize().testTag(UiTags.BIBLE_NO_MATCH),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.bible_no_match),
                    color = colors.muted,
                    fontSize = 15.sp
                )
            }
            books.isEmpty() && !isLoading -> Box(
                modifier = Modifier.fillMaxSize().testTag(UiTags.BIBLE_NO_BOOKS),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.bible_no_books),
                    color = colors.muted,
                    fontSize = 15.sp
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
                        HorizontalDivider(color = colors.borderSubtle)
                    }
                }
            }
        }
    }
}

@Composable
private fun BibleBookRow(book: BibleBook, onSelect: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(UiTags.bibleBook(book.displayName))
            .background(if (colors.isDark) androidx.compose.ui.graphics.Color.Transparent else colors.surface)
            .clickable { onSelect() }
            .height(60.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = book.displayName,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = colors.text,
            modifier = Modifier.weight(1f)
        )
        if (book.totalChapters > 0) {
            Text(
                text = stringResource(Res.string.bible_chapters_count, book.totalChapters),
                fontSize = 13.sp,
                color = colors.dim
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.dim,
            modifier = Modifier.size(20.dp)
        )
    }
}
