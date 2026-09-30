package com.church.presenter.churchpresentermobile.ui.bibles

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
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
import churchpresentermobile.composeapp.generated.resources.bibles_ready_body
import churchpresentermobile.composeapp.generated.resources.bibles_ready_title
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import churchpresentermobile.composeapp.generated.resources.Res
import com.church.presenter.churchpresentermobile.bibleimport.catalog.InstallFailure
import com.church.presenter.churchpresentermobile.bibleimport.catalog.InstallState
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FailureNote(state: InstallState.Failed) {
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
        Icon(
            Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = colors.danger,
            modifier = Modifier.size(18.dp),
        )
        Column {
            Text(
                stringResource(Res.string.bibles_failed_title, state.bible.displayName),
                color = colors.danger,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                failureText(state.failure),
                color = colors.secondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
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

/** Design 3c: the tick, "… is ready", the facts, and where to go next. */
@Composable
internal fun ReadyView(state: InstallState.Done, actions: InstallActions) {
    val bible = state.bible
    ReadyHeader(
        bible.displayName,
        stringResource(Res.string.bibles_ready_body, bible.source.format.label),
        state.installed.fileName,
    )
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
        BiblesButton(
            stringResource(Res.string.bibles_done),
            onDone,
            Modifier.weight(1f),
            ButtonKind.QUIET,
            tag = BiblesTags.INSTALL_DONE,
        )
        BiblesButton(
            stringResource(Res.string.bibles_open_in_bible),
            onOpen,
            Modifier.weight(PRIMARY_WEIGHT),
            tag = BiblesTags.INSTALL_OPEN,
        )
    }
}
