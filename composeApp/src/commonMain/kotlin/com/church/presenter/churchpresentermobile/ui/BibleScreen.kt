package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.VerticalDivider
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
import churchpresentermobile.composeapp.generated.resources.bible_standalone_empty_body
import churchpresentermobile.composeapp.generated.resources.bible_standalone_empty_title
import churchpresentermobile.composeapp.generated.resources.bible_no_books
import churchpresentermobile.composeapp.generated.resources.bible_no_match
import churchpresentermobile.composeapp.generated.resources.bible_retry
import churchpresentermobile.composeapp.generated.resources.bible_search_placeholder
import churchpresentermobile.composeapp.generated.resources.bible_chapter_label
import churchpresentermobile.composeapp.generated.resources.bible_chapters_overline
import churchpresentermobile.composeapp.generated.resources.bible_pane_empty_body
import churchpresentermobile.composeapp.generated.resources.bible_pane_empty_title
import churchpresentermobile.composeapp.generated.resources.tab_bible
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
 * On a phone those are alternatives, one level at a time, and the toolbar
 * title, back-arrow and tab visibility are controlled by the parent App scaffold
 * via [onNavigationChanged] and [onRegisterBackAction]. With [twoPane] set all
 * three levels are on screen at once — books, chapters, verses — and this screen
 * draws its own pane headers.
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
    /** Lay the three levels out side by side rather than one at a time. */
    twoPane: Boolean = false,
    /** Opens the schedule drawer. Only used in [twoPane], which owns its header. */
    onMenu: (() -> Unit)? = null,
    /** Opens settings. Only used in [twoPane], which owns its header. */
    onSettings: (() -> Unit)? = null,
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

    LaunchedEffect(settingsSaveToken) { if (settingsSaveToken > 0) vm.onSettingsSaved(settingsSaveToken) }

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

    LaunchedEffect(scheduleRefreshTrigger) { if (scheduleRefreshTrigger > 0) onScheduleRefresh() }
    BibleToasts(toastEvent, snackbarHostState, vm::toastShown)

    ReportBibleNavigation(
        selectedBook, selectedChapter, onNavigationChanged, onRegisterBackAction, vm::navigateBack,
    )

    // Intercept the system back button when a book (or chapter) is open. Not in
    // two panes: every level is already on screen, so there is no previous one
    // for back to return to.
    AppBackHandler(enabled = selectedBook != null && !twoPane, onBack = vm::navigateBack)

    // ── Schedule-driven navigation ────────────────────────────────────────
    LaunchedEffect(pendingNavBookName, pendingNavChapter) {
        if (pendingNavBookName == null || pendingNavChapter == null) return@LaunchedEffect
        vm.navigateToBookAndChapter(pendingNavBookName, pendingNavChapter, pendingNavVerses)
        onPendingNavHandled()
    }

    val colors = LocalAppColors.current

    if (hasNoLocalBibles) {
        NoBibleYet(modifier)
        return
    }

    // The three levels, each named once, because the phone shows one at a time and
    // the tablet shows all three at once.
    val booksPane: @Composable () -> Unit = {
        BibleBooksScreen(
            books, bookSearchQuery, vm::setBookSearchQuery, vm::selectBook, isLoading,
            Modifier.fillMaxSize(),
        )
    }
    val chaptersPane: @Composable (BibleBook, Int) -> Unit = { book, columns ->
        ChaptersGrid(book, vm::selectChapter, Modifier.fillMaxSize(), selectedChapter, columns)
    }
    val versesPane: @Composable (BibleBook, Int) -> Unit = { book, chapter ->
        BibleVersesPane(
            book = book,
            selectedChapter = chapter,
            verses = verses,
            isProjecting = isProjecting,
            scheduleAdded = scheduleAdded,
            selectedVerseIndices = selectedVerseIndices,
            projectedVerseIndex = projectedVerseIndex,
            onChapterSelect = vm::selectChapter,
            onVerseToggleSelection = vm::toggleVerseSelection,
            onToggleProjecting = vm::toggleProjecting,
            onAddToSchedule = vm::addToSchedule,
            modifier = Modifier.fillMaxSize(),
            isLoading = isLoading,
            isHolding = isHolding,
            isMultiSelectMode = isMultiSelectMode,
            onToggleMultiSelect = vm::toggleMultiSelectMode,
            onToggleHold = vm::toggleHold,
            onClearDisplay = vm::clearDisplay,
        )
    }
    val errorBanner: @Composable () -> Unit = { BibleErrorBanner(error, vm::refresh) }

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        if (twoPane) {
            BibleThreePane(
                book = selectedBook,
                chapter = selectedChapter,
                errorBanner = errorBanner,
                booksPane = booksPane,
                chaptersPane = chaptersPane,
                versesPane = versesPane,
                onMenu = onMenu,
                onSettings = onSettings,
            )
        } else {
            BibleOnePane(
                book = selectedBook,
                chapter = selectedChapter,
                isRefreshing = isLoading,
                onRefresh = vm::refresh,
                errorBanner = errorBanner,
                booksPane = booksPane,
                chaptersPane = chaptersPane,
                versesPane = versesPane,
            )
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

/**
 * The phone's arrangement: one level at a time, with the header's back arrow to
 * climb out of it.
 *
 * Separate from [BibleScreen] only so the coordinator above stays about wiring
 * the ViewModel up rather than about layout — the two arrangements sit side by
 * side and read as the alternatives they are.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BibleOnePane(
    book: BibleBook?,
    chapter: Int?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    errorBanner: @Composable () -> Unit,
    booksPane: @Composable () -> Unit,
    chaptersPane: @Composable (BibleBook, Int) -> Unit,
    versesPane: @Composable (BibleBook, Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        errorBanner()
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            when {
                book != null && chapter != null -> versesPane(book, chapter)
                book != null -> chaptersPane(book, PHONE_CHAPTER_COLUMNS)
                else -> booksPane()
            }
        }
    }
}

/**
 * Standalone with no translation copied onto the device yet.
 *
 * Offers the one thing that fixes it rather than naming a mode the operator
 * would have to go and switch to.
 */
@Composable
private fun NoBibleYet(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
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
}

/** Shows each toast the ViewModel raises, once, in this tab's own wording. */
@Composable
private fun BibleToasts(
    toastEvent: ToastEvent?,
    hostState: SnackbarHostState,
    onShown: () -> Unit,
) {
    val message = toastEvent?.bibleToastMessage()
    LaunchedEffect(toastEvent) {
        if (message != null) {
            hostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            onShown()
        }
    }
}

/**
 * Tells the App scaffold how deep into the Bible the operator is, so its header
 * can name the place and its back arrow can climb out of it.
 *
 * The callbacks are held through [rememberUpdatedState] because the effects key
 * on the *position*, not on the lambdas: a recomposition that hands over fresh
 * lambdas must not re-report a navigation that has not changed.
 */
@Composable
private fun ReportBibleNavigation(
    book: BibleBook?,
    chapter: Int?,
    onNavigationChanged: (BibleBook?, Int?) -> Unit,
    onRegisterBackAction: ((() -> Unit)?) -> Unit,
    onBack: () -> Unit,
) {
    val currentOnNavigationChanged by rememberUpdatedState(onNavigationChanged)
    LaunchedEffect(book, chapter) {
        currentOnNavigationChanged(book, chapter)
    }

    val currentOnRegisterBackAction by rememberUpdatedState(onRegisterBackAction)
    val currentOnBack by rememberUpdatedState(onBack)
    DisposableEffect(book) {
        if (book != null) {
            currentOnRegisterBackAction { currentOnBack() }
        } else {
            currentOnRegisterBackAction(null)
        }
        onDispose { currentOnRegisterBackAction(null) }
    }
}

/** The books request having failed, said where the Retry that fixes it is. */
@Composable
private fun BibleErrorBanner(error: String?, onRetry: () -> Unit) {
    if (error == null) return
    val colors = LocalAppColors.current
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
            text = error,
            color = colors.danger,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stringResource(Res.string.bible_retry),
            color = colors.danger,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 12.dp).clickable { onRetry() }
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
 * The tablet's arrangement for the Bible tab: books, chapters and verses in
 * three panes rather than three screens.
 *
 * The phone walks down those levels one at a time and climbs back with the
 * header's back arrow. Here there is nothing to climb back to, so each pane
 * carries its own heading — the tab's over the books, the reference over the
 * verses — and the two panes to the right stand empty until a book, then a
 * chapter, is picked.
 *
 * @param errorBanner Drawn inside the books pane rather than across all three:
 *   it is the books request that failed, and its Retry reloads that list.
 */
@Composable
private fun BibleThreePane(
    book: BibleBook?,
    chapter: Int?,
    errorBanner: @Composable () -> Unit,
    booksPane: @Composable () -> Unit,
    chaptersPane: @Composable (BibleBook, Int) -> Unit,
    versesPane: @Composable (BibleBook, Int) -> Unit,
    modifier: Modifier = Modifier,
    onMenu: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    Row(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.width(BibleBooksPaneWidth).fillMaxHeight()) {
            ScreenHeader(
                title = stringResource(Res.string.tab_bible),
                onMenu = onMenu,
                onSettings = onSettings,
            )
            errorBanner()
            Box(modifier = Modifier.weight(1f)) { booksPane() }
        }

        VerticalDivider(color = colors.borderSubtle)

        // Kept in place, empty, rather than appearing when a book is picked:
        // a pane that materialises mid-tap shifts the verses out from under the
        // finger already reaching for them.
        Column(
            modifier = Modifier
                .width(BibleChaptersPaneWidth)
                .fillMaxHeight(),
        ) {
            // No horizontal padding here: the grid below brings its own, and
            // padding the pane as well left two chapter cells unable to fit.
            OverlineRow(
                label = stringResource(Res.string.bible_chapters_overline),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 10.dp),
            )
            if (book != null) {
                Box(modifier = Modifier.weight(1f)) { chaptersPane(book, PANE_CHAPTER_COLUMNS) }
            }
        }

        VerticalDivider(color = colors.borderSubtle)

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (book != null && chapter != null) {
                ScreenHeader(
                    title = "${book.displayName} · ${stringResource(Res.string.bible_chapter_label)} $chapter",
                )
                HorizontalDivider(color = colors.borderSubtle)
                Box(modifier = Modifier.weight(1f)) { versesPane(book, chapter) }
            } else {
                EmptyState(
                    title = stringResource(Res.string.bible_pane_empty_title),
                    body = stringResource(Res.string.bible_pane_empty_body),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }
    }
}

/**
 * Chapters per row, on a phone and in the tablet's chapter pane.
 *
 * Two numbers rather than one adaptive rule: the phone shows the grid *instead
 * of* the books and gets the width for four, while the pane is a narrow column
 * between the books and the verses and fits two.
 */
private const val PHONE_CHAPTER_COLUMNS = 4
private const val PANE_CHAPTER_COLUMNS = 2

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
