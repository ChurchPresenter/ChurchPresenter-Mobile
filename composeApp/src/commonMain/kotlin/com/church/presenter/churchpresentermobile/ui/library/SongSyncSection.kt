package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.sync_action
import churchpresentermobile.composeapp.generated.resources.sync_books_all
import churchpresentermobile.composeapp.generated.resources.sync_books_choose
import churchpresentermobile.composeapp.generated.resources.sync_books_finding
import churchpresentermobile.composeapp.generated.resources.sync_books_none
import churchpresentermobile.composeapp.generated.resources.sync_books_some
import churchpresentermobile.composeapp.generated.resources.sync_cancel
import churchpresentermobile.composeapp.generated.resources.sync_cancelled
import churchpresentermobile.composeapp.generated.resources.sync_done
import churchpresentermobile.composeapp.generated.resources.sync_done_with_failures
import churchpresentermobile.composeapp.generated.resources.sync_explain
import churchpresentermobile.composeapp.generated.resources.sync_failed
import churchpresentermobile.composeapp.generated.resources.sync_kept_local
import churchpresentermobile.composeapp.generated.resources.sync_preparing
import churchpresentermobile.composeapp.generated.resources.sync_running
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.SyncOutcome
import com.church.presenter.churchpresentermobile.network.SongService
import com.church.presenter.churchpresentermobile.network.WsSender
import com.church.presenter.churchpresentermobile.ui.theme.AppDimens
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.LibrarySyncViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Copying the desktop's songbook across.
 *
 * The results line names how many of the operator's own edits were preserved. That number is the
 * whole reason the merge rules exist, so it is stated rather than left to be discovered.
 */
@Composable
internal fun SongSyncSection(
    repository: LibraryRepository,
    settings: AppSettings,
    sender: WsSender,
) {
    val viewModel: LibrarySyncViewModel = viewModel(key = "library_sync") {
        LibrarySyncViewModel(repository, settings, SongService(settings, sender))
    }
    val colors = LocalAppColors.current
    val progress by viewModel.progress.collectAsState()
    val outcome by viewModel.outcome.collectAsState()
    val books by viewModel.books.collectAsState()
    val selectedBooks by viewModel.selectedBooks.collectAsState()
    val isLoadingBooks by viewModel.isLoadingBooks.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.space12)) {
        Text(
            text = stringResource(Res.string.sync_explain),
            color = colors.muted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )

        if (progress.isRunning) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (progress.isPreparing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = colors.accent)
                } else {
                    LinearProgressIndicator(
                        progress = { progress.fraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.accent,
                    )
                }
                Text(
                    text = if (progress.isPreparing) {
                        // Before the catalogue answers there is no total, and "Copying 0 of 0…"
                        // at 0% reads as a hung app.
                        stringResource(Res.string.sync_preparing)
                    } else {
                        stringResource(
                            Res.string.sync_running,
                            progress.done.toString(),
                            progress.total.toString(),
                        )
                    },
                    color = colors.muted,
                    fontSize = 11.sp,
                )
                if (progress.currentTitle.isNotBlank()) {
                    Text(
                        text = progress.currentTitle,
                        color = colors.muted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        outcome?.let { result ->
            val (message, tint) = when (result) {
                is SyncOutcome.Success -> {
                    val headline = if (result.failedCount > 0) {
                        stringResource(
                            Res.string.sync_done_with_failures,
                            result.songCount.toString(),
                            result.failedCount.toString(),
                        )
                    } else {
                        stringResource(Res.string.sync_done, result.songCount.toString())
                    }
                    val kept = if (result.keptLocal > 0) {
                        "\n" + stringResource(Res.string.sync_kept_local, result.keptLocal.toString())
                    } else ""
                    (headline + kept) to if (result.failedCount > 0) colors.amber else colors.accent
                }
                is SyncOutcome.Failed ->
                    stringResource(Res.string.sync_failed, result.message) to colors.danger
                is SyncOutcome.Cancelled ->
                    stringResource(Res.string.sync_cancelled, result.songCount.toString()) to colors.muted
            }
            OutcomeCard(message, tint)
        }

        // Which books to take. Absent until asked for, so the common case — copy
        // everything — stays one press, and nobody waits on a list they don't want.
        if (!progress.isRunning) {
            when {
                isLoadingBooks -> Text(
                    text = stringResource(Res.string.sync_books_finding),
                    color = colors.muted,
                    fontSize = 12.sp,
                )
                books.isEmpty() -> Text(
                    text = stringResource(Res.string.sync_books_choose),
                    color = colors.accent,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { viewModel.loadBooks() },
                )
                else -> {
                    Text(
                        text = if (selectedBooks.size == books.size) {
                            stringResource(Res.string.sync_books_all, books.size)
                        } else {
                            stringResource(Res.string.sync_books_some, selectedBooks.size, books.size)
                        },
                        color = if (selectedBooks.isEmpty()) colors.danger else colors.muted,
                        fontSize = 12.sp,
                    )
                    books.forEach { book ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleBook(book) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = book in selectedBooks,
                                onCheckedChange = { viewModel.toggleBook(book) },
                            )
                            Text(
                                text = book,
                                color = colors.text,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (selectedBooks.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.sync_books_none),
                            color = colors.danger,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }

        SheetButton(
            label = if (progress.isRunning) stringResource(Res.string.sync_cancel)
                    else stringResource(Res.string.sync_action),
            isDestructive = progress.isRunning,
            // Every book unticked is not a sync — copying nothing and reporting
            // success would read as the feature being broken.
            enabled = progress.isRunning || viewModel.canSync,
            onClick = { if (progress.isRunning) viewModel.cancel() else viewModel.sync() },
        )
    }
}
