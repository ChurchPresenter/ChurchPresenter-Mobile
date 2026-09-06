package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.editor_cancel
import churchpresentermobile.composeapp.generated.resources.library_clear_action
import churchpresentermobile.composeapp.generated.resources.library_clear_all
import churchpresentermobile.composeapp.generated.resources.library_clear_all_confirm_body
import churchpresentermobile.composeapp.generated.resources.library_clear_all_confirm_title
import churchpresentermobile.composeapp.generated.resources.library_clear_all_detail
import churchpresentermobile.composeapp.generated.resources.library_clear_bibles
import churchpresentermobile.composeapp.generated.resources.library_clear_bibles_confirm_body
import churchpresentermobile.composeapp.generated.resources.library_clear_bibles_confirm_title
import churchpresentermobile.composeapp.generated.resources.library_clear_bibles_detail
import churchpresentermobile.composeapp.generated.resources.library_clear_done_all
import churchpresentermobile.composeapp.generated.resources.library_clear_done_bibles
import churchpresentermobile.composeapp.generated.resources.library_clear_done_songs
import churchpresentermobile.composeapp.generated.resources.library_clear_explain
import churchpresentermobile.composeapp.generated.resources.library_clear_none
import churchpresentermobile.composeapp.generated.resources.library_clear_songs
import churchpresentermobile.composeapp.generated.resources.library_clear_songs_confirm_body
import churchpresentermobile.composeapp.generated.resources.library_clear_songs_confirm_title
import churchpresentermobile.composeapp.generated.resources.library_clear_songs_detail
import churchpresentermobile.composeapp.generated.resources.library_clear_title
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.WsMessageType
import com.church.presenter.churchpresentermobile.network.WsSender
import com.church.presenter.churchpresentermobile.ui.theme.AppDimens
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.ClearOutcome
import com.church.presenter.churchpresentermobile.viewmodel.LibraryClearViewModel
import com.church.presenter.churchpresentermobile.viewmodel.formatBytes
import org.jetbrains.compose.resources.stringResource

/** Which wipe the confirm dialog is standing in front of. */
private enum class PendingClear { SONGS, BIBLES, EVERYTHING }

/**
 * Emptying this phone of content, without emptying it of settings.
 *
 * Three wipes rather than one, because the two libraries fail differently: a
 * songbook copied from the wrong computer is a mistake to undo, five Bible
 * modules are a space problem, and a phone being handed to another church is
 * both. Each states what goes and what stays before it asks, since none of it
 * can be undone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearContentSheet(
    repository: LibraryRepository,
    bibles: LocalBibleRepository,
    settings: AppSettings,
    sender: WsSender,
    onDismiss: () -> Unit,
) {
    val viewModel: LibraryClearViewModel = viewModel(key = "library_clear") {
        LibraryClearViewModel(repository, bibles, settings)
    }
    val colors = LocalAppColors.current
    val content by viewModel.content.collectAsState()
    val outcome by viewModel.outcome.collectAsState()
    val isClearing by viewModel.isClearing.collectAsState()
    var pending by remember { mutableStateOf<PendingClear?>(null) }

    // Whatever was deleted must not still be on the audience screen. Local in
    // standalone, a message to the desktop in remote — the router decides.
    LaunchedEffect(outcome) {
        if (outcome != null) sender.sendAction(WsMessageType.CLEAR, "", fireAndForget = true)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.sheetBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.space20, vertical = AppDimens.space8),
            verticalArrangement = Arrangement.spacedBy(AppDimens.space12),
        ) {
            Text(
                text = stringResource(Res.string.library_clear_title),
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.library_clear_explain),
                color = colors.muted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )

            outcome?.let { result ->
                OutcomeCard(
                    message = when (result) {
                        ClearOutcome.SONGS -> stringResource(Res.string.library_clear_done_songs)
                        ClearOutcome.BIBLES -> stringResource(Res.string.library_clear_done_bibles)
                        ClearOutcome.EVERYTHING -> stringResource(Res.string.library_clear_done_all)
                    },
                    tint = colors.accent,
                )
            }

            if (content.isEmpty) {
                Text(
                    text = stringResource(Res.string.library_clear_none),
                    color = colors.muted,
                    fontSize = 12.sp,
                )
            }

            if (content.hasSongs) {
                ClearRow(
                    label = stringResource(Res.string.library_clear_songs),
                    detail = stringResource(Res.string.library_clear_songs_detail, content.songCount),
                    enabled = !isClearing,
                    onClick = { pending = PendingClear.SONGS },
                )
            }

            if (content.hasBibles) {
                ClearRow(
                    label = stringResource(Res.string.library_clear_bibles),
                    detail = stringResource(
                        Res.string.library_clear_bibles_detail,
                        content.bibleCount,
                        formatBytes(content.bibleBytes),
                    ),
                    enabled = !isClearing,
                    onClick = { pending = PendingClear.BIBLES },
                )
            }

            if (!content.isEmpty) {
                ClearRow(
                    label = stringResource(Res.string.library_clear_all),
                    detail = stringResource(Res.string.library_clear_all_detail),
                    enabled = !isClearing,
                    onClick = { pending = PendingClear.EVERYTHING },
                )
            }

            Box(Modifier.padding(bottom = AppDimens.space16))
        }
    }

    pending?.let { target ->
        ClearConfirmDialog(
            title = when (target) {
                PendingClear.SONGS -> stringResource(Res.string.library_clear_songs_confirm_title)
                PendingClear.BIBLES -> stringResource(Res.string.library_clear_bibles_confirm_title)
                PendingClear.EVERYTHING -> stringResource(Res.string.library_clear_all_confirm_title)
            },
            body = when (target) {
                PendingClear.SONGS ->
                    stringResource(Res.string.library_clear_songs_confirm_body, content.songCount)
                PendingClear.BIBLES ->
                    stringResource(Res.string.library_clear_bibles_confirm_body, content.bibleCount)
                PendingClear.EVERYTHING -> stringResource(Res.string.library_clear_all_confirm_body)
            },
            onConfirm = {
                when (target) {
                    PendingClear.SONGS -> viewModel.clearSongs()
                    PendingClear.BIBLES -> viewModel.clearBibles()
                    PendingClear.EVERYTHING -> viewModel.clearEverything()
                }
                pending = null
            },
            onDismiss = { pending = null },
        )
    }
}

/** One wipe, with the size of it stated underneath. */
@Composable
private fun ClearRow(
    label: String,
    detail: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SheetButton(label = label, isDestructive = true, enabled = enabled, onClick = onClick)
        Text(text = detail, color = colors.muted, fontSize = 11.sp)
    }
}

@Composable
private fun ClearConfirmDialog(
    title: String,
    body: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(Res.string.library_clear_action)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.editor_cancel)) }
        },
    )
}
