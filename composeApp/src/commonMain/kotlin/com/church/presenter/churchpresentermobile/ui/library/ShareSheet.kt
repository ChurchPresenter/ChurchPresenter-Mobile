package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.import_cancel
import churchpresentermobile.composeapp.generated.resources.import_conflict_count
import churchpresentermobile.composeapp.generated.resources.import_conflict_question
import churchpresentermobile.composeapp.generated.resources.import_confirm
import churchpresentermobile.composeapp.generated.resources.import_done
import churchpresentermobile.composeapp.generated.resources.import_error_empty
import churchpresentermobile.composeapp.generated.resources.import_error_too_new
import churchpresentermobile.composeapp.generated.resources.import_error_unreadable
import churchpresentermobile.composeapp.generated.resources.import_error_wrong_format
import churchpresentermobile.composeapp.generated.resources.import_keep_mine
import churchpresentermobile.composeapp.generated.resources.import_new_count
import churchpresentermobile.composeapp.generated.resources.import_nothing
import churchpresentermobile.composeapp.generated.resources.import_replace
import churchpresentermobile.composeapp.generated.resources.share_explain
import churchpresentermobile.composeapp.generated.resources.share_export
import churchpresentermobile.composeapp.generated.resources.share_import
import churchpresentermobile.composeapp.generated.resources.share_title
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.ConflictResolution
import com.church.presenter.churchpresentermobile.model.CpsetError
import com.church.presenter.churchpresentermobile.model.ImportPreview
import com.church.presenter.churchpresentermobile.ui.TextDocumentExporter
import com.church.presenter.churchpresentermobile.ui.TextDocumentPicker
import com.church.presenter.churchpresentermobile.ui.theme.AppDimens
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.LibraryShareViewModel
import com.church.presenter.churchpresentermobile.viewmodel.ShareUiState
import org.jetbrains.compose.resources.stringResource

/**
 * Moves libraries and service sets between phones.
 *
 * An import always previews first: it names how many items are new and how many
 * the user already has, and makes them choose what happens to the overlap. A
 * silent merge would be quicker and much worse — an operator would have no way
 * to know their version had been replaced.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(
    repository: LibraryRepository,
    onDismiss: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val viewModel: LibraryShareViewModel = viewModel(key = "library_share") {
        LibraryShareViewModel(repository)
    }
    val colors = LocalAppColors.current
    val state by viewModel.uiState.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                text = stringResource(Res.string.share_title),
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.share_explain),
                color = colors.muted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )

            when (val current = state) {
                is ShareUiState.Previewing -> PreviewCard(
                    preview = current.preview,
                    onResolve = viewModel::confirmImport,
                    onCancel = viewModel::dismiss,
                )

                is ShareUiState.Imported -> ResultText(
                    text = stringResource(Res.string.import_done, current.count.toString()),
                    tint = colors.accent,
                )

                is ShareUiState.Error -> ResultText(
                    text = stringResource(current.error.messageResource()),
                    tint = colors.danger,
                )

                ShareUiState.Idle -> {
                    TextDocumentExporter(onError = onMessage) { share ->
                        ActionButton(
                            label = stringResource(Res.string.share_export),
                            isPrimary = true,
                        ) {
                            share(viewModel.exportText(), viewModel.exportFileName())
                        }
                    }
                    TextDocumentPicker(
                        onPicked = { file ->
                            file?.let { viewModel.onFilePicked(it.text, it.fileName) }
                        },
                        onError = onMessage,
                    ) { launch ->
                        ActionButton(
                            label = stringResource(Res.string.share_import),
                            isPrimary = false,
                            onClick = launch,
                        )
                    }
                }
            }

            Box(Modifier.padding(bottom = AppDimens.space16))
        }
    }
}

@Composable
private fun PreviewCard(
    preview: ImportPreview,
    onResolve: (ConflictResolution) -> Unit,
    onCancel: () -> Unit,
) {
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.radiusCard))
            .background(colors.surface)
            .padding(AppDimens.space16),
        verticalArrangement = Arrangement.spacedBy(AppDimens.space8),
    ) {
        if (preview.isEmpty) {
            Text(stringResource(Res.string.import_nothing), color = colors.muted, fontSize = 13.sp)
            ActionButton(stringResource(Res.string.import_cancel), isPrimary = false, onClick = onCancel)
            return@Column
        }

        Text(
            text = stringResource(Res.string.import_new_count, preview.newCount.toString()),
            color = colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (preview.conflictCount > 0) {
            Text(
                text = stringResource(Res.string.import_conflict_count, preview.conflictCount.toString()),
                color = colors.amber,
                fontSize = 12.sp,
            )
            Text(
                text = stringResource(Res.string.import_conflict_question),
                color = colors.muted,
                fontSize = 12.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.space8)) {
                Box(Modifier.weight(1f)) {
                    ActionButton(stringResource(Res.string.import_keep_mine), isPrimary = false) {
                        onResolve(ConflictResolution.KEEP_MINE)
                    }
                }
                Box(Modifier.weight(1f)) {
                    ActionButton(stringResource(Res.string.import_replace), isPrimary = true) {
                        onResolve(ConflictResolution.REPLACE)
                    }
                }
            }
        } else {
            ActionButton(stringResource(Res.string.import_confirm), isPrimary = true) {
                onResolve(ConflictResolution.KEEP_MINE)
            }
        }
    }
}

@Composable
private fun ResultText(text: String, tint: androidx.compose.ui.graphics.Color) {
    val colors = LocalAppColors.current
    Text(
        text = text,
        color = tint,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.radiusCard))
            .background(colors.surface)
            .padding(AppDimens.space14),
    )
}

@Composable
private fun ActionButton(label: String, isPrimary: Boolean, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.radiusButton))
            .background(if (isPrimary) colors.accent else colors.surfaceStrong)
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (isPrimary) colors.onAccent else colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun CpsetError.messageResource() = when (this) {
    CpsetError.UNREADABLE -> Res.string.import_error_unreadable
    CpsetError.WRONG_FORMAT -> Res.string.import_error_wrong_format
    CpsetError.TOO_NEW -> Res.string.import_error_too_new
    CpsetError.EMPTY -> Res.string.import_error_empty
}
