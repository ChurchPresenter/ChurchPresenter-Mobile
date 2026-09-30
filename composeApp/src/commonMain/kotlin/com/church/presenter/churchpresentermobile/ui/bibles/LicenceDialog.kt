package com.church.presenter.churchpresentermobile.ui.bibles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.church.presenter.churchpresentermobile.ui.verticalScrollbar
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.Download
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.action_cancel
import churchpresentermobile.composeapp.generated.resources.bible_catalog_book_names_english
import churchpresentermobile.composeapp.generated.resources.bible_catalog_license_accept
import churchpresentermobile.composeapp.generated.resources.bible_catalog_license_badge_redistributable
import churchpresentermobile.composeapp.generated.resources.bible_catalog_license_badge_unverified
import churchpresentermobile.composeapp.generated.resources.bible_catalog_license_body
import churchpresentermobile.composeapp.generated.resources.bible_catalog_license_field_copyright
import churchpresentermobile.composeapp.generated.resources.bible_catalog_license_field_identifier
import churchpresentermobile.composeapp.generated.resources.bible_catalog_license_field_source
import churchpresentermobile.composeapp.generated.resources.bible_catalog_license_source_beblia
import churchpresentermobile.composeapp.generated.resources.bible_catalog_license_source_ebible
import churchpresentermobile.composeapp.generated.resources.bible_catalog_license_source_zefania
import churchpresentermobile.composeapp.generated.resources.bible_catalog_license_subtitle
import churchpresentermobile.composeapp.generated.resources.bible_catalog_license_title
import churchpresentermobile.composeapp.generated.resources.bible_catalog_license_unknown
import churchpresentermobile.composeapp.generated.resources.bibles_licence_accept_file
import churchpresentermobile.composeapp.generated.resources.bibles_licence_file_badge
import churchpresentermobile.composeapp.generated.resources.bibles_licence_reinstall
import churchpresentermobile.composeapp.generated.resources.bibles_licence_source_file
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import churchpresentermobile.composeapp.generated.resources.Res
import com.church.presenter.churchpresentermobile.bibleimport.catalog.BibleSource
import com.church.presenter.churchpresentermobile.bibleimport.BookNames
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/**
 * The copyright confirmation, before anything is downloaded or installed — the desktop's
 * `LicenceConfirmation`, in this app's look and with the desktop's own words.
 *
 * @param source The archive, or null for a file picked off the device, whose provenance nothing
 *   can vouch for.
 * @param isReinstall Adds the warning that the installed copy will be replaced.
 */
@Composable
internal fun LicenceDialog(
    name: String,
    identifier: String,
    copyright: String,
    source: BibleSource?,
    language: String,
    isReinstall: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    val redistributable = source == BibleSource.EBIBLE
    // Holy Bible XML numbers its books; a language the app has no table for gets English names.
    val englishBookNames = source == BibleSource.BEBLIA && language.uppercase() !in BookNames.LANGUAGE_LOOKUPS
    val scroll = rememberScrollState()
    // Accepting is only possible once the whole text has been on screen: scrolled to the end, or
    // short enough that it never needed scrolling. It stays possible after that, so scrolling back
    // up to re-read the copyright line does not take the button away again.
    var readToEnd by remember { mutableStateOf(false) }
    LaunchedEffect(scroll.value, scroll.maxValue) {
        if (scroll.value >= scroll.maxValue) readToEnd = true
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.sheetBackground,
        modifier = Modifier.testTag(BiblesTags.LICENCE),
        icon = {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(colors.chosenFill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Copyright,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(Res.string.bible_catalog_license_title),
                    color = colors.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(Res.string.bible_catalog_license_subtitle),
                    color = colors.muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        text = {
            Column(Modifier.verticalScrollbar(scroll).verticalScroll(scroll).padding(end = SCROLLBAR_GUTTER)) {
                LicenceFacts(name, identifier, copyright, source, redistributable)
                LicenceNote(
                    when (source) {
                        BibleSource.EBIBLE -> stringResource(Res.string.bible_catalog_license_source_ebible)
                        BibleSource.ZEFANIA -> stringResource(Res.string.bible_catalog_license_source_zefania)
                        BibleSource.BEBLIA -> stringResource(Res.string.bible_catalog_license_source_beblia)
                        null -> stringResource(Res.string.bibles_licence_source_file)
                    },
                )
                if (englishBookNames) {
                    Text(
                        stringResource(Res.string.bible_catalog_book_names_english),
                        color = colors.muted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                if (isReinstall) {
                    Text(
                        stringResource(Res.string.bibles_licence_reinstall, name),
                        color = colors.danger,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                Box(Modifier.testTag(BiblesTags.LICENCE_END).fillMaxWidth().height(1.dp))
            }
        },
        confirmButton = {
            BiblesButton(
                stringResource(
                    if (source == null) {
                        Res.string.bibles_licence_accept_file
                    } else {
                        Res.string.bible_catalog_license_accept
                    },
                ),
                onConfirm,
                icon = Icons.Filled.Download,
                height = 44.dp,
                enabled = readToEnd,
                tag = BiblesTags.LICENCE_ACCEPT,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag(BiblesTags.LICENCE_CANCEL)) {
                Text(stringResource(Res.string.action_cancel), color = colors.muted)
            }
        },
    )
}

@Composable
private fun LicenceFacts(
    name: String,
    identifier: String,
    copyright: String,
    source: BibleSource?,
    redistributable: Boolean,
) {
    val colors = LocalAppColors.current
    BiblesCard(Modifier.fillMaxWidth(), radius = 12.dp) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    name,
                    color = colors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                val badge = when {
                    source == null -> stringResource(Res.string.bibles_licence_file_badge)
                    redistributable -> stringResource(Res.string.bible_catalog_license_badge_redistributable)
                    else -> stringResource(Res.string.bible_catalog_license_badge_unverified)
                }
                val tint = if (redistributable) colors.accent else colors.warning
                Text(
                    badge.uppercase(),
                    color = tint,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(tint.copy(alpha = 0.12f)).padding(
                        horizontal = 6.dp,
                        vertical = 2.dp,
                    ),
                )
            }
            Box(Modifier.height(8.dp))
            if (source != null) FactLine(stringResource(Res.string.bible_catalog_license_field_source), source.label)
            if (identifier.isNotBlank()) FactLine(
                stringResource(Res.string.bible_catalog_license_field_identifier),
                identifier,
            )
            FactLine(
                stringResource(Res.string.bible_catalog_license_field_copyright),
                copyright.ifBlank { stringResource(Res.string.bible_catalog_license_unknown) },
            )
        }
    }
}

@Composable
private fun FactLine(label: String, value: String) {
    val colors = LocalAppColors.current
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, color = colors.muted, fontSize = 11.sp, modifier = Modifier.width(90.dp))
        Text(value, color = colors.text, fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}

/** The source's own note and the general copyright paragraph, behind an accent rule. */
@Composable
private fun LicenceNote(sourceNote: String) {
    val colors = LocalAppColors.current
    Row(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.cardFill)
            .padding(12.dp),
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(colors.accent))
        Column(Modifier.padding(start = 10.dp)) {
            Text(sourceNote, color = colors.secondary, fontSize = 12.sp, lineHeight = 17.sp)
            Text(
                stringResource(Res.string.bible_catalog_license_body),
                color = colors.secondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** Room at the trailing edge for the scrollbar, so it never sits on top of the text. */
private val SCROLLBAR_GUTTER = 8.dp
