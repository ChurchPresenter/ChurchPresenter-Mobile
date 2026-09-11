package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.songs_pane_empty_body
import churchpresentermobile.composeapp.generated.resources.songs_pane_empty_title
import churchpresentermobile.composeapp.generated.resources.tab_songs
import churchpresentermobile.composeapp.generated.resources.toast_failed_to_add_schedule
import churchpresentermobile.composeapp.generated.resources.toast_failed_to_project
import churchpresentermobile.composeapp.generated.resources.toast_no_song_selected
import churchpresentermobile.composeapp.generated.resources.toast_request_denied
import churchpresentermobile.composeapp.generated.resources.toast_request_failed
import churchpresentermobile.composeapp.generated.resources.toast_request_rejected
import churchpresentermobile.composeapp.generated.resources.toast_request_rejected_reason
import churchpresentermobile.composeapp.generated.resources.toast_session_blocked
import churchpresentermobile.composeapp.generated.resources.toast_song_added_to_schedule
import churchpresentermobile.composeapp.generated.resources.toast_song_live
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.Song
import com.church.presenter.churchpresentermobile.model.ToastEvent
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.SongsViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Coordinator for the Songs tab.
 *
 * Owns the [SongsViewModel] and switches between:
 * - [SongsListScreen] — searchable, filterable song table (root)
 * - [SongDetailScreen] — verse cards for the selected song
 *
 * On a phone the two are alternatives, and the toolbar title, back-arrow and
 * tab visibility are controlled by the parent App scaffold via [onDetailChanged]
 * and [onRegisterBackAction]. With [twoPane] set they are side by side and this
 * screen draws both headers itself — there is nothing to go back *to*, so the
 * App scaffold draws no header of its own for this tab.
 */
@Composable
fun SongsTable(
    appSettings: AppSettings,
    isDemoMode: Boolean = false,
    settingsSaveToken: Int,
    onDetailChanged: (title: String?, bookName: String?) -> Unit = { _, _ -> },
    onRegisterBackAction: ((() -> Unit)?) -> Unit = {},
    onScheduleRefresh: () -> Unit = {},
    /** Draw the list and the detail together, each in its own pane. */
    twoPane: Boolean = false,
    /** Opens the schedule drawer. Only used in [twoPane], which owns its header. */
    onMenu: (() -> Unit)? = null,
    /** Opens settings. Only used in [twoPane], which owns its header. */
    onSettings: (() -> Unit)? = null,
    pendingNavSongTitle: String? = null,
    pendingNavSongBook: String? = null,
    onPendingNavHandled: () -> Unit = {},
    providedViewModel: SongsViewModel? = null,
    modifier: Modifier = Modifier
) {
    // Use the session-scoped ViewModel passed from App.kt when available.
    // The internal fallback is only here so the composable still works in
    // isolation (e.g. Compose Previews or tests).
    val vm: SongsViewModel = providedViewModel
        ?: viewModel(key = isDemoMode.toString()) {
            SongsViewModel(appSettings, ServerEventService(appSettings), isDemoMode)
        }

    LaunchedEffect(settingsSaveToken) { if (settingsSaveToken > 0) vm.onSettingsSaved(settingsSaveToken) }

    val songs              by vm.songs.collectAsState()
    val selectedSong       by vm.selectedSong.collectAsState()
    val isLoading          by vm.isLoading.collectAsState()
    val error              by vm.error.collectAsState()
    val searchQuery        by vm.searchQuery.collectAsState()
    val selectedBook       by vm.selectedBook.collectAsState()
    val availableBooks     by vm.availableBooks.collectAsState()
    val hasActiveFilter    by vm.hasActiveFilter.collectAsState()
    val songDetail         by vm.songDetail.collectAsState()
    val isLoadingDetail    by vm.isLoadingDetail.collectAsState()
    val detailError        by vm.detailError.collectAsState()
    val selectedVerseIndex by vm.selectedVerseIndex.collectAsState()
    val isProjecting       by vm.isProjecting.collectAsState()
    val toastEvent         by vm.toastEvent.collectAsState()
    val scheduleRefreshTrigger by vm.scheduleRefreshTrigger.collectAsState()
    val scheduleAdded      by vm.scheduleAdded.collectAsState()
    val showsLocalLibrary  by vm.showsLocalLibrary.collectAsState()
    val canAddToSchedule   by vm.canAddToSchedule.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    SongToasts(toastEvent, snackbarHostState) { vm.toastShown() }

    LaunchedEffect(scheduleRefreshTrigger) { if (scheduleRefreshTrigger > 0) onScheduleRefresh() }

    LaunchedEffect(pendingNavSongTitle) {
        if (pendingNavSongTitle == null) return@LaunchedEffect
        vm.openSongByTitle(pendingNavSongTitle, pendingNavSongBook)
        onPendingNavHandled()
    }

    val showDetail = isLoadingDetail || songDetail != null || detailError != null

    // Only a phone can be "in" the detail: with both panes on screen, back has
    // no previous screen to return to, and emptying the right-hand pane is not
    // what the gesture means.
    AppBackHandler(enabled = showDetail && !twoPane) {
        vm.dismissSongDetail()
    }

    ReportSongDetail(
        showDetail = showDetail,
        selectedSong = selectedSong,
        onDetailChanged = onDetailChanged,
        onRegisterBackAction = onRegisterBackAction,
        onDismiss = { vm.dismissSongDetail() },
    )

    // The same switch the outputs follow, read from the presenter's theme.
    val showChords by vm.showChords.collectAsState()

    // Named once each, because the two layouts place the same two screens
    // differently — writing the argument lists out twice is how one of the two
    // ends up a release behind the other.
    val listPane: @Composable () -> Unit = {
        SongsListScreen(
            songs = songs,
            selectedSong = selectedSong,
            isLoading = isLoading,
            error = error,
            searchQuery = searchQuery,
            selectedBook = selectedBook,
            availableBooks = availableBooks,
            hasActiveFilter = hasActiveFilter,
            onSearchQueryChange = { vm.setSearchQuery(it) },
            onBookSelected = { vm.setSelectedBook(it) },
            onSongClick = { vm.openSongDetail(it) },
            onRefresh = { vm.refresh() },
            showsLocalLibrary = showsLocalLibrary,
            modifier = Modifier.fillMaxSize()
        )
    }
    val detailPane: @Composable () -> Unit = {
        SongDetailScreen(
            detail = songDetail,
            isLoading = isLoadingDetail,
            error = detailError,
            selectedVerseIndex = selectedVerseIndex,
            isProjecting = isProjecting,
            scheduleAdded = scheduleAdded,
            onVerseSelected = { vm.selectVerse(it) },
            onToggleProjecting = { vm.toggleProjecting() },
            // Null in standalone: there is no desktop schedule to add to,
            // and the swallowed action would report cheerful success.
            onAddToSchedule = if (canAddToSchedule) ({ vm.addSongToSchedule() }) else null,
            onClearDisplay = { vm.clearDisplay() },
            showChords = showChords,
            modifier = Modifier.fillMaxSize()
        )
    }

    Box(modifier = modifier) {
        when {
            twoPane -> SongsTwoPane(
                showDetail = showDetail,
                detailTitle = selectedSong?.title,
                detailSubtitle = selectedSong?.bookName?.takeIf { it.isNotBlank() },
                onMenu = onMenu,
                onSettings = onSettings,
                listPane = listPane,
                detailPane = detailPane,
            )
            showDetail -> detailPane()
            else -> listPane()
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/** Shows each toast the ViewModel raises, once, in this tab's own wording. */
@Composable
private fun SongToasts(
    toastEvent: ToastEvent?,
    hostState: SnackbarHostState,
    onShown: () -> Unit,
) {
    val message = toastEvent?.songToastMessage()
    LaunchedEffect(toastEvent) {
        if (message != null) {
            hostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            onShown()
        }
    }
}

/**
 * Tells the App scaffold which song is open, so its header can name it and its
 * back arrow can close it.
 *
 * Reports nothing on a tablet, where this tab draws its own pane headers and
 * there is no back arrow to register — see [SongsTwoPane].
 */
@Composable
private fun ReportSongDetail(
    showDetail: Boolean,
    selectedSong: Song?,
    onDetailChanged: (title: String?, bookName: String?) -> Unit,
    onRegisterBackAction: ((() -> Unit)?) -> Unit,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(showDetail, selectedSong) {
        if (showDetail && selectedSong != null) {
            onDetailChanged("#${selectedSong.number} - ${selectedSong.title}", selectedSong.bookName)
            onRegisterBackAction(onDismiss)
        } else if (showDetail) {
            onDetailChanged(null, null)
            onRegisterBackAction(onDismiss)
        } else {
            onDetailChanged(null, null)
            onRegisterBackAction(null)
        }
    }
}

/**
 * The tablet's side-by-side arrangement: the song list in a fixed-width pane on
 * the left, the open song's verses filling what is left.
 *
 * Each pane carries its own header, which is why this exists rather than a
 * two-branch `if` at the call site — the list's header is the tab's (schedule,
 * title, settings) and the detail's names the open song, and neither belongs in
 * a bar spanning both.
 *
 * `internal` rather than private so it can be driven with plain data: reaching
 * this arrangement through [SongsTable] would mean standing up a ViewModel, a
 * settings object and a WebSocket to exercise a `Row`. See AGENT.md, "reach for
 * a seam before declaring code untestable".
 */
@Composable
internal fun SongsTwoPane(
    showDetail: Boolean,
    detailTitle: String?,
    detailSubtitle: String?,
    onMenu: (() -> Unit)?,
    onSettings: (() -> Unit)?,
    listPane: @Composable () -> Unit,
    detailPane: @Composable () -> Unit,
) {
    val colors = LocalAppColors.current
    Row(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.width(ListPaneWidth).fillMaxHeight()) {
            ScreenHeader(
                title = stringResource(Res.string.tab_songs),
                onMenu = onMenu,
                onSettings = onSettings,
            )
            Box(modifier = Modifier.weight(1f)) { listPane() }
        }

        VerticalDivider(color = colors.borderSubtle)

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (showDetail && detailTitle != null) {
                ScreenHeader(title = detailTitle, subtitle = detailSubtitle)
                HorizontalDivider(color = colors.borderSubtle)
                Box(modifier = Modifier.weight(1f)) { detailPane() }
            } else {
                // Not an error, and not worth a spinner: the operator simply has
                // not picked a song yet, which on a phone was a screen they had
                // not opened.
                EmptyState(
                    title = stringResource(Res.string.songs_pane_empty_title),
                    body = stringResource(Res.string.songs_pane_empty_body),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }
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
internal fun ToastEvent.songToastMessage(): String = when (this) {
    is ToastEvent.SongLive                  -> stringResource(Res.string.toast_song_live)
    is ToastEvent.RequestFailed             -> stringResource(Res.string.toast_request_failed)
    is ToastEvent.NoSongSelected            -> stringResource(Res.string.toast_no_song_selected)
    is ToastEvent.RequestDenied             -> stringResource(Res.string.toast_request_denied)
    is ToastEvent.SessionBlocked            -> stringResource(Res.string.toast_session_blocked)
    is ToastEvent.SongAddedToSchedule       -> stringResource(Res.string.toast_song_added_to_schedule, title)
    is ToastEvent.FailedToProject           -> stringResource(Res.string.toast_failed_to_project, reason)
    is ToastEvent.FailedToAddSchedule       -> stringResource(Res.string.toast_failed_to_add_schedule, reason)
    is ToastEvent.RequestRejected           -> stringResource(Res.string.toast_request_rejected, httpStatus.toString())
    is ToastEvent.RequestRejectedWithReason -> stringResource(Res.string.toast_request_rejected_reason, reason)
    else                                    -> "" // Bible-specific events handled in BibleScreen
}

