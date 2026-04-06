package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.church.presenter.churchpresentermobile.model.ToastEvent
import com.church.presenter.churchpresentermobile.viewmodel.SongsViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Coordinator for the Songs tab.
 *
 * Owns the [SongsViewModel] and switches between:
 * - [SongsListScreen] — searchable, filterable song table (root)
 * - [SongDetailScreen] — verse cards for the selected song
 *
 * Toolbar title, back-arrow, and tab visibility are controlled by the parent
 * App scaffold via [onDetailChanged] and [onRegisterBackAction].
 */
@Composable
fun SongsTable(
    appSettings: AppSettings,
    isDemoMode: Boolean = false,
    settingsSaveToken: Int,
    onDetailChanged: (title: String?, bookName: String?) -> Unit = { _, _ -> },
    onRegisterBackAction: ((() -> Unit)?) -> Unit = {},
    onScheduleRefresh: () -> Unit = {},
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
            SongsViewModel(appSettings, isDemoMode)
        }

    LaunchedEffect(settingsSaveToken) {
        if (settingsSaveToken > 0) vm.onSettingsSaved(settingsSaveToken)
    }

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

    val snackbarHostState = remember { SnackbarHostState() }

    val toastMessage = toastEvent?.toDisplayString()
    LaunchedEffect(toastEvent) {
        if (toastMessage != null) {
            snackbarHostState.showSnackbar(message = toastMessage, duration = SnackbarDuration.Short)
            vm.toastShown()
        }
    }

    LaunchedEffect(scheduleRefreshTrigger) {
        if (scheduleRefreshTrigger > 0) onScheduleRefresh()
    }

    LaunchedEffect(pendingNavSongTitle) {
        if (pendingNavSongTitle != null) {
            vm.openSongByTitle(pendingNavSongTitle, pendingNavSongBook)
            onPendingNavHandled()
        }
    }

    val showDetail = isLoadingDetail || songDetail != null || detailError != null

    AppBackHandler(enabled = showDetail) {
        vm.dismissSongDetail()
    }

    LaunchedEffect(showDetail, selectedSong) {
        if (showDetail) {
            val toolbarTitle = if (selectedSong != null)
                "#${selectedSong!!.number} - ${selectedSong!!.title}"
            else
                null
            onDetailChanged(toolbarTitle, selectedSong?.bookName)
            onRegisterBackAction { vm.dismissSongDetail() }
        } else {
            onDetailChanged(null, null)
            onRegisterBackAction(null)
        }
    }

    Box(modifier = modifier) {
        if (showDetail) {
            SongDetailScreen(
                detail = songDetail,
                isLoading = isLoadingDetail,
                error = detailError,
                selectedVerseIndex = selectedVerseIndex,
                isProjecting = isProjecting,
                scheduleAdded = scheduleAdded,
                onVerseSelected = { vm.selectVerse(it) },
                onToggleProjecting = { vm.toggleProjecting() },
                onAddToSchedule = { vm.addSongToSchedule() },
                modifier = Modifier.fillMaxSize()
            )
        } else {
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
                modifier = Modifier.fillMaxSize()
            )
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

