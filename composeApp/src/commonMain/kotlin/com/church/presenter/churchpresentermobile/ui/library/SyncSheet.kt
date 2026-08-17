package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.sync_action
import churchpresentermobile.composeapp.generated.resources.sync_cancel
import churchpresentermobile.composeapp.generated.resources.sync_cancelled
import churchpresentermobile.composeapp.generated.resources.sync_done
import churchpresentermobile.composeapp.generated.resources.sync_done_with_failures
import churchpresentermobile.composeapp.generated.resources.sync_explain
import churchpresentermobile.composeapp.generated.resources.sync_failed
import churchpresentermobile.composeapp.generated.resources.sync_kept_local
import churchpresentermobile.composeapp.generated.resources.sync_preparing
import churchpresentermobile.composeapp.generated.resources.sync_running
import churchpresentermobile.composeapp.generated.resources.sync_title
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
 * Runs a desktop→library sync and reports what it did.
 *
 * The results line names how many of the operator's own edits were preserved.
 * That number is the whole reason the merge rules exist, so it is stated rather
 * than left for them to discover.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSheet(
    repository: LibraryRepository,
    settings: AppSettings,
    sender: WsSender,
    onDismiss: () -> Unit,
) {
    val viewModel: LibrarySyncViewModel = viewModel(key = "library_sync") {
        LibrarySyncViewModel(repository, settings, SongService(settings, sender))
    }
    val colors = LocalAppColors.current

    val progress by viewModel.progress.collectAsState()
    val outcome by viewModel.outcome.collectAsState()

    ModalBottomSheet(
        onDismissRequest = { if (!progress.isRunning) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.sheetBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = AppDimens.space20, vertical = AppDimens.space8),
            verticalArrangement = Arrangement.spacedBy(AppDimens.space12),
        ) {
            Text(
                text = stringResource(Res.string.sync_title),
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.sync_explain),
                color = colors.muted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )

            if (progress.isRunning) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (progress.isPreparing) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.accent,
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { progress.fraction },
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.accent,
                        )
                    }
                    Text(
                        text = if (progress.isPreparing) {
                            // Before the catalogue answers there is no total, and
                            // "Copying 0 of 0…" at 0% reads as a hung app.
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

            outcome?.let { OutcomeCard(it) }

            SheetButton(
                label = if (progress.isRunning) {
                    stringResource(Res.string.sync_cancel)
                } else {
                    stringResource(Res.string.sync_action)
                },
                isDestructive = progress.isRunning,
                onClick = { if (progress.isRunning) viewModel.cancel() else viewModel.sync() },
            )

            Box(Modifier.padding(bottom = AppDimens.space16))
        }
    }
}

@Composable
private fun OutcomeCard(outcome: SyncOutcome) {
    val colors = LocalAppColors.current
    val (message, tint) = when (outcome) {
        is SyncOutcome.Success -> {
            val headline = if (outcome.failedCount > 0) {
                stringResource(
                    Res.string.sync_done_with_failures,
                    outcome.songCount.toString(),
                    outcome.failedCount.toString(),
                )
            } else {
                stringResource(Res.string.sync_done, outcome.songCount.toString())
            }
            // Naming the preserved edits is the point of the merge rules.
            val kept = if (outcome.keptLocal > 0) {
                "\n" + stringResource(Res.string.sync_kept_local, outcome.keptLocal.toString())
            } else ""
            (headline + kept) to if (outcome.failedCount > 0) colors.amber else colors.accent
        }
        is SyncOutcome.Failed ->
            stringResource(Res.string.sync_failed, outcome.message) to colors.danger
        is SyncOutcome.Cancelled ->
            stringResource(Res.string.sync_cancelled, outcome.songCount.toString()) to colors.muted
    }

    Text(
        text = message,
        color = tint,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.radiusCard))
            .background(colors.surface)
            .padding(AppDimens.space14),
    )
}

@Composable
private fun SheetButton(label: String, isDestructive: Boolean, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.radiusButton))
            .background(if (isDestructive) colors.surface else colors.accent)
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (isDestructive) colors.danger else colors.onAccent,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
