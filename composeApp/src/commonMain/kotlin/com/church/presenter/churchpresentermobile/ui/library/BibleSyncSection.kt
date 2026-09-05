package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.bible_sync_cancelled
import churchpresentermobile.composeapp.generated.resources.bible_sync_cancelled_none
import churchpresentermobile.composeapp.generated.resources.bible_sync_done
import churchpresentermobile.composeapp.generated.resources.bible_sync_done_with_failures
import churchpresentermobile.composeapp.generated.resources.bible_sync_download
import churchpresentermobile.composeapp.generated.resources.bible_sync_downloading
import churchpresentermobile.composeapp.generated.resources.bible_sync_explain
import churchpresentermobile.composeapp.generated.resources.bible_sync_failed
import churchpresentermobile.composeapp.generated.resources.bible_sync_find
import churchpresentermobile.composeapp.generated.resources.bible_sync_finding
import churchpresentermobile.composeapp.generated.resources.bible_sync_active
import churchpresentermobile.composeapp.generated.resources.bible_sync_choose_hint
import churchpresentermobile.composeapp.generated.resources.bible_sync_installed
import churchpresentermobile.composeapp.generated.resources.bible_sync_none_offered
import churchpresentermobile.composeapp.generated.resources.bible_sync_nothing_copied
import churchpresentermobile.composeapp.generated.resources.bible_sync_remove
import churchpresentermobile.composeapp.generated.resources.bible_sync_remove_confirm_body
import churchpresentermobile.composeapp.generated.resources.bible_sync_remove_confirm_title
import churchpresentermobile.composeapp.generated.resources.bible_sync_stop
import churchpresentermobile.composeapp.generated.resources.bible_sync_verses
import churchpresentermobile.composeapp.generated.resources.qa_admin_cancel
import com.church.presenter.churchpresentermobile.library.BibleSyncOutcome
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.ui.theme.AppDimens
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.BibleChoiceViewModel
import com.church.presenter.churchpresentermobile.viewmodel.BibleSyncViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Choosing translations and copying them onto the device.
 *
 * Nothing is asked of the desktop until the operator taps "See what your computer has": opening
 * the sheet to copy songs should not fire a Bible request at an address that may be wrong.
 */
@Composable
internal fun BibleSyncSection(
    bibles: LocalBibleRepository,
    settings: AppSettings,
) {
    val viewModel: BibleSyncViewModel = viewModel(key = "bible_sync") {
        BibleSyncViewModel(bibles, settings)
    }
    // Choosing is not downloading — the Library tab offers the same choice, so
    // the state behind it lives apart from this sheet.
    val choice: BibleChoiceViewModel = viewModel(key = "bible_choice") {
        BibleChoiceViewModel(bibles)
    }
    val colors = LocalAppColors.current

    val choices by viewModel.choices.collectAsState()
    val selection by viewModel.selection.collectAsState()
    val installed by viewModel.installed.collectAsState()
    val activeId by choice.activeId.collectAsState()
    val isLoading by viewModel.isLoadingChoices.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val outcome by viewModel.outcome.collectAsState()

    var pendingRemoval by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.space12)) {
        Text(
            text = stringResource(Res.string.bible_sync_explain),
            color = colors.muted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )

        if (installed.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.bible_sync_installed),
                color = colors.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            // Which one is read and presented is a choice, not the order they
            // happened to be downloaded in — so the list picks as well as lists.
            if (installed.size > 1) {
                Text(
                    text = stringResource(Res.string.bible_sync_choose_hint),
                    color = colors.muted,
                    fontSize = 11.sp,
                )
            }
            installed.forEach { bible ->
                val isActive = bible.id == activeId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { choice.setActive(bible.id) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = isActive,
                        onClick = { choice.setActive(bible.id) },
                    )
                    Column(Modifier.weight(1f)) {
                        Text(bible.title, color = colors.text, fontSize = 13.sp, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Text(
                            text = if (isActive) {
                                stringResource(Res.string.bible_sync_active) + " · " +
                                    stringResource(Res.string.bible_sync_verses, bible.verseCount.toString())
                            } else {
                                stringResource(Res.string.bible_sync_verses, bible.verseCount.toString())
                            },
                            color = if (isActive) colors.accent else colors.muted,
                            fontSize = 11.sp,
                        )
                    }
                    IconButton(onClick = { pendingRemoval = bible.id }) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = stringResource(Res.string.bible_sync_remove),
                            tint = colors.muted,
                        )
                    }
                }
            }
        }

        when {
            progress.isRunning -> {
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
                        text = stringResource(
                            Res.string.bible_sync_downloading,
                            progress.currentTitle,
                            (progress.done + 1).toString(),
                            progress.total.toString(),
                        ),
                        color = colors.muted,
                        fontSize = 11.sp,
                    )
                }
                // Honest label: a multi-megabyte body already on the wire cannot be stopped
                // without also losing the report of what was copied.
                SheetButton(
                    label = stringResource(Res.string.bible_sync_stop),
                    isDestructive = true,
                    onClick = { viewModel.cancel() },
                )
            }

            isLoading -> Text(
                text = stringResource(Res.string.bible_sync_finding),
                color = colors.muted,
                fontSize = 12.sp,
            )

            choices.isEmpty() -> {
                loadError?.let { OutcomeCard(stringResource(Res.string.bible_sync_failed, it), colors.danger) }
                SheetButton(
                    label = stringResource(Res.string.bible_sync_find),
                    onClick = { viewModel.loadChoices() },
                )
            }

            else -> {
                LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                    items(choices, key = { it.fileName }) { choice ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggle(choice.fileName) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = choice.fileName in selection,
                                onCheckedChange = { viewModel.toggle(choice.fileName) },
                            )
                            Text(
                                text = choice.displayName,
                                color = colors.text,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (choice.isInstalled) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = stringResource(Res.string.bible_sync_installed),
                                    tint = colors.accent,
                                )
                            }
                        }
                    }
                }
                SheetButton(
                    label = stringResource(Res.string.bible_sync_download, selection.size.toString()),
                    enabled = selection.isNotEmpty(),
                    onClick = { viewModel.sync() },
                )
            }
        }

        outcome?.let { result ->
            val (message, tint) = when (result) {
                is BibleSyncOutcome.Success -> when {
                    result.installed.isEmpty() ->
                        stringResource(Res.string.bible_sync_nothing_copied,
                            result.failed.joinToString(", ")) to colors.danger
                    result.failed.isEmpty() ->
                        stringResource(Res.string.bible_sync_done,
                            result.installed.joinToString(", ")) to colors.accent
                    else ->
                        stringResource(Res.string.bible_sync_done_with_failures,
                            result.installed.joinToString(", "),
                            result.failed.joinToString(", ")) to colors.amber
                }
                is BibleSyncOutcome.Failed ->
                    stringResource(Res.string.bible_sync_failed, result.message) to colors.danger
                is BibleSyncOutcome.Cancelled ->
                    if (result.installed.isEmpty()) {
                        stringResource(Res.string.bible_sync_cancelled_none) to colors.muted
                    } else {
                        stringResource(Res.string.bible_sync_cancelled,
                            result.installed.joinToString(", ")) to colors.muted
                    }
            }
            OutcomeCard(message, tint)
        }
    }

    pendingRemoval?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text(stringResource(Res.string.bible_sync_remove_confirm_title)) },
            text = { Text(stringResource(Res.string.bible_sync_remove_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.remove(id); pendingRemoval = null }) {
                    Text(stringResource(Res.string.bible_sync_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) {
                    Text(stringResource(Res.string.qa_admin_cancel))
                }
            },
        )
    }
}
