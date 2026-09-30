package com.church.presenter.churchpresentermobile.ui.bibles

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.action_cancel
import churchpresentermobile.composeapp.generated.resources.bibles_done
import churchpresentermobile.composeapp.generated.resources.bibles_failed_title
import churchpresentermobile.composeapp.generated.resources.bibles_failure_checksum
import churchpresentermobile.composeapp.generated.resources.bibles_failure_corrupt
import churchpresentermobile.composeapp.generated.resources.bibles_failure_encoding
import churchpresentermobile.composeapp.generated.resources.bibles_failure_http
import churchpresentermobile.composeapp.generated.resources.bibles_failure_network
import churchpresentermobile.composeapp.generated.resources.bibles_failure_no_verses
import churchpresentermobile.composeapp.generated.resources.bibles_failure_not_a_bible
import churchpresentermobile.composeapp.generated.resources.bibles_failure_storage
import churchpresentermobile.composeapp.generated.resources.bibles_field_books
import churchpresentermobile.composeapp.generated.resources.bibles_field_license
import churchpresentermobile.composeapp.generated.resources.bibles_field_source
import churchpresentermobile.composeapp.generated.resources.bibles_field_verses
import churchpresentermobile.composeapp.generated.resources.bibles_license_not_stated
import churchpresentermobile.composeapp.generated.resources.bibles_open_in_bible
import churchpresentermobile.composeapp.generated.resources.bibles_queued_detail
import churchpresentermobile.composeapp.generated.resources.bibles_ready_body
import churchpresentermobile.composeapp.generated.resources.bibles_ready_title
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import churchpresentermobile.composeapp.generated.resources.Res
import com.church.presenter.churchpresentermobile.bibleimport.catalog.CatalogBible
import com.church.presenter.churchpresentermobile.bibleimport.catalog.InstallFailure
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
            Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, tint = colors.scheduleBibleFg, modifier = Modifier.size(24.dp))
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
            Row(Modifier.padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    listOfNotNull(bible.languageLabel, bible.sizeBytes.takeIf { it > 0 }?.let(::sizeLabel)).joinToString(" · "),
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
            detail = if (state is InstallState.Queued) stringResource(Res.string.bibles_queued_detail) else downloadDetail(progress),
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
                StepState.AHEAD -> Triple(if (colors.isDark) Color(0x0FFFFFFF) else Color.White, colors.cardEdge, 1.dp)
            }
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(fill).border(width, edge, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                when (state) {
                    StepState.DONE -> Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onAccentButton, modifier = Modifier.size(14.dp))
                    else -> Text(
                        "$number",
                        color = when (state) {
                            StepState.CURRENT -> colors.accent
                            StepState.FAILED -> colors.danger
                            else -> if (colors.isDark) Color(0xFF52525B) else colors.dim
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
            Text(detail, color = colors.muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 5.dp))
            if (fraction != null) ProgressTrack(fraction, Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun FailureNote(state: InstallState.Failed) {
    val colors = LocalAppColors.current
    Row(
        Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.danger.copy(alpha = if (colors.isDark) 0.08f else 0.05f))
            .border(1.dp, colors.danger.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = colors.danger, modifier = Modifier.size(18.dp))
        Column {
            Text(
                stringResource(Res.string.bibles_failed_title, state.bible.displayName),
                color = colors.danger,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(failureText(state.failure), color = colors.secondary, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

/** What went wrong, in words that say what to do about it. */
@Composable
internal fun failureText(failure: InstallFailure): String = stringResource(
    when (failure) {
        InstallFailure.NETWORK -> Res.string.bibles_failure_network
        InstallFailure.HTTP -> Res.string.bibles_failure_http
        InstallFailure.CHECKSUM -> Res.string.bibles_failure_checksum
        InstallFailure.CORRUPT -> Res.string.bibles_failure_corrupt
        InstallFailure.NOT_A_BIBLE -> Res.string.bibles_failure_not_a_bible
        InstallFailure.ENCODING -> Res.string.bibles_failure_encoding
        InstallFailure.NO_VERSES -> Res.string.bibles_failure_no_verses
        InstallFailure.STORAGE -> Res.string.bibles_failure_storage
    },
)

@Composable
private fun InstallButtons(state: InstallState, actions: InstallActions) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (state is InstallState.Failed) {
            BiblesButton(stringResource(Res.string.action_cancel), actions.onCancel, Modifier.weight(1f), ButtonKind.QUIET, tag = BiblesTags.INSTALL_CANCEL)
            BiblesButton(stringResource(Res.string.bibles_row_retry), actions.onRetry, Modifier.weight(1.3f), tag = BiblesTags.INSTALL_RETRY)
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

/** Design 3c: the tick, "… is ready", the facts, and where to go next. */
@Composable
private fun ReadyView(state: InstallState.Done, actions: InstallActions) {
    val bible = state.bible
    ReadyHeader(bible.displayName, stringResource(Res.string.bibles_ready_body, bible.source.format.label), state.installed.fileName)
    FactRows(
        rows = listOf(
            stringResource(Res.string.bibles_field_books) to { FactValue(grouped(state.books)) },
            stringResource(Res.string.bibles_field_verses) to { FactValue(grouped(state.verses)) },
            stringResource(Res.string.bibles_field_source) to { FactValue(bible.source.label) },
            stringResource(Res.string.bibles_field_license) to {
                FactValue(state.installed.license.ifBlank { stringResource(Res.string.bibles_license_not_stated) })
            },
        ),
        modifier = Modifier.padding(top = 20.dp),
    )
    ReadyButtons(actions.onDone, actions.onOpen, Modifier.padding(top = 20.dp))
}

@Composable
internal fun ReadyHeader(title: String, body: String, fileName: String) {
    val colors = LocalAppColors.current
    Column(
        Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(colors.chosenFill), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(30.dp))
        }
        Text(
            stringResource(Res.string.bibles_ready_title, title),
            color = colors.text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.025).em,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 18.dp),
        )
        Text(
            buildAnnotatedString {
                append(body)
                append('\n')
                withStyle(SpanStyle(color = colors.secondary, fontFamily = MonoFamily)) { append(fileName) }
            },
            color = colors.muted,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 9.dp),
        )
    }
}

@Composable
internal fun ReadyButtons(onDone: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BiblesButton(stringResource(Res.string.bibles_done), onDone, Modifier.weight(1f), ButtonKind.QUIET, tag = BiblesTags.INSTALL_DONE)
        BiblesButton(stringResource(Res.string.bibles_open_in_bible), onOpen, Modifier.weight(1.3f), tag = BiblesTags.INSTALL_OPEN)
    }
}

