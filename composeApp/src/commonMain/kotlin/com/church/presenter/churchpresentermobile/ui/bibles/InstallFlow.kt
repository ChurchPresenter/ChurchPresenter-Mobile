package com.church.presenter.churchpresentermobile.ui.bibles

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.action_cancel
import churchpresentermobile.composeapp.generated.resources.bibles_queued_detail
import churchpresentermobile.composeapp.generated.resources.bibles_row_retry
import churchpresentermobile.composeapp.generated.resources.bibles_run_in_background
import churchpresentermobile.composeapp.generated.resources.bibles_step_convert
import churchpresentermobile.composeapp.generated.resources.bibles_step_convert_progress
import churchpresentermobile.composeapp.generated.resources.bibles_step_convert_progress_of
import churchpresentermobile.composeapp.generated.resources.bibles_step_convert_waiting
import churchpresentermobile.composeapp.generated.resources.bibles_step_download
import churchpresentermobile.composeapp.generated.resources.bibles_step_download_done
import churchpresentermobile.composeapp.generated.resources.bibles_step_download_progress
import churchpresentermobile.composeapp.generated.resources.bibles_step_download_started
import churchpresentermobile.composeapp.generated.resources.bibles_step_install
import churchpresentermobile.composeapp.generated.resources.bibles_step_install_detail
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import churchpresentermobile.composeapp.generated.resources.Res
import com.church.presenter.churchpresentermobile.bibleimport.catalog.CatalogBible
import com.church.presenter.churchpresentermobile.bibleimport.catalog.InstallProgress
import com.church.presenter.churchpresentermobile.bibleimport.catalog.InstallState
import com.church.presenter.churchpresentermobile.bibleimport.catalog.InstallStep
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/** Design 3a–3d on a phone: the install's progress, or its outcome, in a sheet over the catalogue. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InstallSheet(state: InstallState, actions: InstallActions) {
    ModalBottomSheet(
        onDismissRequest = if (state is InstallState.Done) actions.onDone else actions.onBackground,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = LocalAppColors.current.sheetBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        InstallView(
            state,
            actions,
            Modifier.padding(start = 20.dp, end = 20.dp, bottom = 26.dp).navigationBarsPadding(),
        )
    }
}

/**
 * The install itself, without a sheet around it — the tablet shows it in its detail pane.
 */
@Composable
internal fun InstallView(state: InstallState, actions: InstallActions, modifier: Modifier = Modifier) {
    Column(modifier.testTag(BiblesTags.INSTALL_SHEET)) {
        if (state is InstallState.Done) {
            ReadyView(state, actions)
        } else {
            InstallHeader(state.bible)
            Box(Modifier.height(24.dp))
            InstallSteps(state)
            Box(Modifier.height(22.dp))
            InstallButtons(state, actions)
        }
    }
}

@Composable
private fun InstallHeader(bible: CatalogBible) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(colors.scheduleBibleBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.MenuBook,
                contentDescription = null,
                tint = colors.scheduleBibleFg,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                bible.displayName,
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.025).em,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                Modifier.padding(top = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    listOfNotNull(
                        bible.languageLabel,
                        bible.sizeBytes.takeIf { it > 0 }?.let(::sizeLabel),
                    ).joinToString(" · "),
                    color = colors.muted,
                    fontSize = 12.sp,
                )
                FormatBadge(bible.source.format)
            }
        }
    }
}

private enum class StepState { DONE, CURRENT, AHEAD, FAILED }

@Composable
private fun InstallSteps(state: InstallState) {
    val bible = state.bible
    val progress = when (state) {
        is InstallState.Running -> state.progress
        is InstallState.Failed -> state.progress
        else -> InstallProgress()
    }
    val reached = if (state is InstallState.Queued) null else progress.step
    fun stateOf(step: InstallStep): StepState = when {
        reached == null -> StepState.AHEAD
        step.ordinal < reached.ordinal -> StepState.DONE
        step == reached -> if (state is InstallState.Failed) StepState.FAILED else StepState.CURRENT
        else -> StepState.AHEAD
    }
    Column {
        Step(
            number = 1,
            state = stateOf(InstallStep.DOWNLOAD),
            title = stringResource(Res.string.bibles_step_download, bible.source.label),
            detail = if (state is InstallState.Queued) {
                stringResource(Res.string.bibles_queued_detail)
            } else {
                downloadDetail(progress)
            },
            fraction = downloadFraction(progress).takeIf { stateOf(InstallStep.DOWNLOAD) == StepState.CURRENT },
            last = false,
        )
        Step(
            number = 2,
            state = stateOf(InstallStep.CONVERT),
            title = stringResource(Res.string.bibles_step_convert, bible.source.format.label),
            detail = convertDetail(bible, progress),
            fraction = progress.convert?.fraction?.takeIf { stateOf(InstallStep.CONVERT) == StepState.CURRENT },
            last = false,
        )
        Step(
            number = 3,
            state = stateOf(InstallStep.INSTALL),
            title = stringResource(Res.string.bibles_step_install),
            detail = stringResource(Res.string.bibles_step_install_detail, bible.fileName),
            fraction = null,
            last = true,
        )
        if (state is InstallState.Failed) FailureNote(state)
    }
}

@Composable
private fun downloadDetail(progress: InstallProgress): String {
    val name = progress.downloadName
    val total = progress.totalBytes
    return when {
        progress.downloadMs != null -> stringResource(
            Res.string.bibles_step_download_done,
            name,
            sizeLabel(progress.downloadedBytes),
            ((progress.downloadMs + MS_PER_SECOND - 1) / MS_PER_SECOND).toInt(),
        )
        total != null && total > 0 -> stringResource(
            Res.string.bibles_step_download_progress,
            name,
            sizeLabel(progress.downloadedBytes),
            sizeLabel(total),
        )
        else -> stringResource(Res.string.bibles_step_download_started, name, sizeLabel(progress.downloadedBytes))
    }
}

private const val MS_PER_SECOND = 1000L

private fun downloadFraction(progress: InstallProgress): Float {
    val total = progress.totalBytes ?: return 0f
    return if (total > 0) progress.downloadedBytes.toFloat() / total else 0f
}

@Composable
private fun convertDetail(bible: CatalogBible, progress: InstallProgress): String {
    val convert = progress.convert ?: return stringResource(Res.string.bibles_step_convert_waiting)
    val expected = bible.otBookCount + bible.ntBookCount
    return if (expected > 0) {
        stringResource(
            Res.string.bibles_step_convert_progress_of,
            expected,
            grouped(convert.verses),
            convert.books.coerceIn(1, expected),
            expected,
        )
    } else {
        stringResource(Res.string.bibles_step_convert_progress, convert.books, grouped(convert.verses))
    }
}

/** One step: a numbered (or ticked) circle, a connecting rail, a title and a detail line. */
@Composable
private fun Step(number: Int, state: StepState, title: String, detail: String, fraction: Float?, last: Boolean) {
    val colors = LocalAppColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val (fill, edge, width) = when (state) {
                StepState.DONE -> Triple(colors.accent, colors.accent, 1.dp)
                StepState.CURRENT -> Triple(colors.chosenFill, colors.accent, 2.dp)
                StepState.FAILED -> Triple(colors.danger.copy(alpha = 0.12f), colors.danger, 2.dp)
                StepState.AHEAD -> Triple(colors.quietFill, colors.cardEdge, 1.dp)
            }
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(fill).border(width, edge, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                when (state) {
                    StepState.DONE -> Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = colors.onAccentButton,
                        modifier = Modifier.size(14.dp),
                    )
                    else -> Text(
                        "$number",
                        color = when (state) {
                            StepState.CURRENT -> colors.accent
                            StepState.FAILED -> colors.danger
                            else -> colors.aheadInk
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (!last) {
                Box(
                    Modifier
                        .padding(vertical = 4.dp)
                        .width(2.dp)
                        .height(if (fraction != null) 46.dp else 34.dp)
                        .background(if (state == StepState.DONE) colors.accent else colors.cardEdge),
                )
            }
        }
        Column(Modifier.weight(1f).padding(top = 5.dp)) {
            Text(
                title,
                color = if (state == StepState.AHEAD) colors.muted else colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                detail,
                color = colors.muted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 5.dp),
            )
            if (fraction != null) ProgressTrack(fraction, Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun InstallButtons(state: InstallState, actions: InstallActions) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state is InstallState.Failed) {
            BiblesButton(
                stringResource(Res.string.action_cancel),
                actions.onCancel,
                Modifier.weight(1f),
                ButtonKind.QUIET,
                tag = BiblesTags.INSTALL_CANCEL,
            )
            BiblesButton(
                stringResource(Res.string.bibles_row_retry),
                actions.onRetry,
                Modifier.weight(PRIMARY_WEIGHT),
                tag = BiblesTags.INSTALL_RETRY,
            )
        } else {
            BiblesButton(
                stringResource(Res.string.bibles_run_in_background),
                actions.onBackground,
                Modifier.weight(1f),
                ButtonKind.QUIET,
                tag = BiblesTags.INSTALL_BACKGROUND,
            )
            BiblesButton(
                stringResource(Res.string.action_cancel),
                actions.onCancel,
                Modifier.width(110.dp),
                ButtonKind.DANGER,
                tag = BiblesTags.INSTALL_CANCEL,
            )
        }
    }
}
