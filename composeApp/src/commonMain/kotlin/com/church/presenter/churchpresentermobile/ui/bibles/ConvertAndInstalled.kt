package com.church.presenter.churchpresentermobile.ui.bibles

import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.action_cancel
import churchpresentermobile.composeapp.generated.resources.bibles_convert_change
import churchpresentermobile.composeapp.generated.resources.bibles_convert_choose
import churchpresentermobile.composeapp.generated.resources.bibles_convert_choose_another
import churchpresentermobile.composeapp.generated.resources.bibles_convert_choose_body
import churchpresentermobile.composeapp.generated.resources.bibles_convert_field_abbreviation
import churchpresentermobile.composeapp.generated.resources.bibles_convert_field_title
import churchpresentermobile.composeapp.generated.resources.bibles_convert_from_abbreviation
import churchpresentermobile.composeapp.generated.resources.bibles_convert_install
import churchpresentermobile.composeapp.generated.resources.bibles_convert_installed_body
import churchpresentermobile.composeapp.generated.resources.bibles_convert_preview
import churchpresentermobile.composeapp.generated.resources.bibles_convert_reading
import churchpresentermobile.composeapp.generated.resources.bibles_convert_replaces
import churchpresentermobile.composeapp.generated.resources.bibles_convert_saved_as
import churchpresentermobile.composeapp.generated.resources.bibles_convert_step_detected
import churchpresentermobile.composeapp.generated.resources.bibles_convert_step_save
import churchpresentermobile.composeapp.generated.resources.bibles_convert_step_source
import churchpresentermobile.composeapp.generated.resources.bibles_convert_unreadable
import churchpresentermobile.composeapp.generated.resources.bibles_field_books
import churchpresentermobile.composeapp.generated.resources.bibles_field_format
import churchpresentermobile.composeapp.generated.resources.bibles_field_language
import churchpresentermobile.composeapp.generated.resources.bibles_field_verses
import churchpresentermobile.composeapp.generated.resources.bibles_in_use
import churchpresentermobile.composeapp.generated.resources.bibles_installed_empty
import churchpresentermobile.composeapp.generated.resources.bibles_origin_desktop
import churchpresentermobile.composeapp.generated.resources.bibles_origin_file
import churchpresentermobile.composeapp.generated.resources.bibles_remove
import churchpresentermobile.composeapp.generated.resources.bibles_remove_body
import churchpresentermobile.composeapp.generated.resources.bibles_remove_title
import churchpresentermobile.composeapp.generated.resources.bibles_use
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import churchpresentermobile.composeapp.generated.resources.Res
import com.church.presenter.churchpresentermobile.bibleimport.BibleXmlFormat
import com.church.presenter.churchpresentermobile.library.InstallDetails
import com.church.presenter.churchpresentermobile.model.InstalledBible
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.ConvertPhase
import com.church.presenter.churchpresentermobile.viewmodel.PickedBible
import org.jetbrains.compose.resources.stringResource

/**
 * Design 4a–4d: source file, what it was detected as, what to save it as, and the button.
 *
 * The form scrolls; the button stays pinned to the bottom, in its own bar.
 */
@Composable
internal fun ConvertForm(ui: ConvertUi, actions: ConvertActions, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Column(modifier.testTag(BiblesTags.CONVERT).fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            when (val phase = ui.phase) {
                ConvertPhase.Empty -> ChooseFile(actions.onPick)
                is ConvertPhase.Installed -> {
                    ReadyHeader(phase.bible.title, stringResource(Res.string.bibles_convert_installed_body), phase.bible.fileName)
                }
                else -> ConvertBody(ui, actions)
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(if (colors.isDark) Color(0xFF111117) else Color.White)
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 22.dp)
                .navigationBarsPadding(),
        ) {
            when (ui.phase) {
                is ConvertPhase.Unreadable -> BiblesButton(
                    stringResource(Res.string.bibles_convert_choose_another),
                    actions.onPick,
                    Modifier.fillMaxWidth(),
                    ButtonKind.QUIET,
                    tag = BiblesTags.CONVERT_PICK,
                )
                is ConvertPhase.Installed -> ReadyButtons(actions.onDone, actions.onOpen)
                ConvertPhase.Empty -> BiblesButton(
                    stringResource(Res.string.bibles_convert_choose),
                    actions.onPick,
                    Modifier.fillMaxWidth(),
                    icon = Icons.Outlined.FileOpen,
                    tag = BiblesTags.CONVERT_PICK,
                )
                else -> BiblesButton(
                    stringResource(Res.string.bibles_convert_install),
                    actions.onConvert,
                    Modifier.fillMaxWidth(),
                    enabled = ui.phase is ConvertPhase.Ready && ui.title.isNotBlank(),
                    tag = BiblesTags.CONVERT_INSTALL,
                )
            }
        }
    }
}

@Composable
private fun ChooseFile(onPick: () -> Unit) {
    val colors = LocalAppColors.current
    Column(
        Modifier.fillMaxWidth().padding(top = 64.dp, start = 16.dp, end = 16.dp).clickable(onClick = onPick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(colors.hueOf(BibleXmlFormat.ZEFANIA).tint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Description, contentDescription = null, tint = colors.hueOf(BibleXmlFormat.ZEFANIA).fg, modifier = Modifier.size(32.dp))
        }
        Text(
            stringResource(Res.string.bibles_convert_choose),
            color = colors.text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 18.dp),
        )
        Text(
            stringResource(Res.string.bibles_convert_choose_body),
            color = colors.muted,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun ConvertBody(ui: ConvertUi, actions: ConvertActions) {
    val colors = LocalAppColors.current
    val phase = ui.phase
    val unreadable = phase is ConvertPhase.Unreadable
    Overline(stringResource(Res.string.bibles_convert_step_source), Modifier.padding(start = 4.dp, bottom = 10.dp))
    SourceFileCard(ui.file, unreadable, actions.onPick)
    when (phase) {
        is ConvertPhase.Unreadable -> ErrorCard(phase)
        ConvertPhase.Reading -> Row(
            Modifier.padding(top = 22.dp, start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircularProgressIndicator(color = colors.accent, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            Text(stringResource(Res.string.bibles_convert_reading), color = colors.muted, fontSize = 13.sp)
        }
        is ConvertPhase.Ready, ConvertPhase.Installing -> {
            val detected = (phase as? ConvertPhase.Ready)?.detected
            if (detected != null) {
                Overline(stringResource(Res.string.bibles_convert_step_detected), Modifier.padding(start = 4.dp, top = 22.dp, bottom = 10.dp))
                FactRows(
                    listOf(
                        stringResource(Res.string.bibles_field_format) to { FormatBadge(detected.format) },
                        stringResource(Res.string.bibles_field_language) to { FactValue(detected.languageName) },
                        stringResource(Res.string.bibles_field_books) to { FactValue(grouped(detected.books)) },
                        stringResource(Res.string.bibles_field_verses) to { FactValue(grouped(detected.verses)) },
                    ),
                )
            }
            SaveAs(ui, actions)
            detected?.preview?.let { (reference, text) -> Preview(reference, text) }
        }
        else -> Unit
    }
}

@Composable
private fun SourceFileCard(file: PickedBible?, unreadable: Boolean, onChange: () -> Unit) {
    val colors = LocalAppColors.current
    val zefania = colors.hueOf(BibleXmlFormat.ZEFANIA)
    BiblesCard(Modifier.fillMaxWidth(), edge = if (unreadable) colors.danger.copy(alpha = 0.4f) else colors.cardEdge) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (unreadable) colors.danger.copy(alpha = if (colors.isDark) 0.12f else 0.08f) else zefania.tint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Description, contentDescription = null, tint = if (unreadable) colors.danger else zefania.fg, modifier = Modifier.size(21.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    file?.fileName.orEmpty(),
                    color = colors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = MonoFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(sizeLabel(file?.sizeBytes ?: 0), color = colors.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
            }
            Text(
                stringResource(Res.string.bibles_convert_change),
                color = colors.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onChange),
            )
        }
    }
}

@Composable
private fun ErrorCard(phase: ConvertPhase.Unreadable) {
    val colors = LocalAppColors.current
    Row(
        Modifier
            .testTag(BiblesTags.CONVERT_ERROR)
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.danger.copy(alpha = if (colors.isDark) 0.08f else 0.05f))
            .border(1.dp, colors.danger.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = colors.danger, modifier = Modifier.size(18.dp))
        Column {
            Text(stringResource(Res.string.bibles_convert_unreadable), color = colors.danger, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(failureText(phase.failure), color = colors.secondary, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun SaveAs(ui: ConvertUi, actions: ConvertActions) {
    val colors = LocalAppColors.current
    Overline(stringResource(Res.string.bibles_convert_step_save), Modifier.padding(start = 4.dp, top = 22.dp, bottom = 10.dp))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FormField(stringResource(Res.string.bibles_convert_field_title), ui.title, actions.onTitle, BiblesTags.CONVERT_TITLE, mono = false)
        FormField(stringResource(Res.string.bibles_convert_field_abbreviation), ui.abbreviation, actions.onAbbreviation, BiblesTags.CONVERT_ABBREVIATION, mono = true)
        Row(
            Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = colors.muted, modifier = Modifier.size(13.dp))
            Text(stringResource(Res.string.bibles_convert_saved_as), color = colors.muted, fontSize = 12.sp)
            Text(ui.savedFileName, color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = MonoFamily)
            Box(Modifier.weight(1f))
            Text(stringResource(Res.string.bibles_convert_from_abbreviation), color = colors.muted, fontSize = 11.sp)
        }
        if (ui.replaces) {
            Text(
                stringResource(Res.string.bibles_convert_replaces, ui.savedFileName),
                color = colors.warning,
                fontSize = 12.sp,
                modifier = Modifier.testTag(BiblesTags.CONVERT_REPLACES).padding(horizontal = 4.dp),
            )
        }
    }
}

/** A labelled text field: overline above, the value in a card, accent outline while focused. */
@Composable
private fun FormField(label: String, value: String, onValue: (String) -> Unit, tag: String, mono: Boolean) {
    val colors = LocalAppColors.current
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.testTag(tag)) {
        Text(
            label.uppercase(),
            color = colors.muted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.09.em,
            modifier = Modifier.padding(start = 4.dp, bottom = 7.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValue,
            singleLine = true,
            textStyle = TextStyle(
                color = colors.text,
                fontSize = if (mono) 14.sp else 15.sp,
                fontWeight = if (mono) FontWeight.SemiBold else FontWeight.Medium,
                fontFamily = if (mono) MonoFamily else null,
            ),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.cardFill)
                .border(1.dp, if (focused) colors.accent else colors.cardEdge, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 13.dp)
                .onFocusChanged { focused = it.isFocused },
        )
    }
}

@Composable
private fun Preview(reference: String, text: String) {
    val colors = LocalAppColors.current
    BiblesCard(Modifier.padding(top = 14.dp).fillMaxWidth(), radius = 12.dp) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                stringResource(Res.string.bibles_convert_preview, reference).uppercase(),
                color = colors.muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.09.em,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(text, color = colors.secondary, fontSize = 13.sp, lineHeight = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * The translations on this device — "Manage" on a phone, "Installed" on a tablet. Choosing one
 * makes it the one read; removing asks first, since a service may be relying on it.
 */
@Composable
internal fun InstalledList(
    installed: List<InstalledBible>,
    activeId: String,
    onUse: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    var confirming by remember { mutableStateOf<InstalledBible?>(null) }
    Column(
        modifier.testTag(BiblesTags.INSTALLED).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (installed.isEmpty()) {
            Text(
                stringResource(Res.string.bibles_installed_empty),
                color = colors.muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
            )
        }
        installed.forEach { bible ->
            InstalledRow(bible, bible.id == activeId, { onUse(bible.id) }, { confirming = bible })
        }
    }
    confirming?.let { bible ->
        AlertDialog(
            onDismissRequest = { confirming = null },
            containerColor = colors.sheetBackground,
            title = { Text(stringResource(Res.string.bibles_remove_title, bible.title), color = colors.text) },
            text = { Text(stringResource(Res.string.bibles_remove_body), color = colors.secondary) },
            confirmButton = {
                TextButton(
                    onClick = { onRemove(bible.id); confirming = null },
                    modifier = Modifier.testTag(BiblesTags.REMOVE_CONFIRM),
                ) { Text(stringResource(Res.string.bibles_remove), color = colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = null }) { Text(stringResource(Res.string.action_cancel), color = colors.muted) }
            },
        )
    }
}

@Composable
private fun InstalledRow(bible: InstalledBible, active: Boolean, onUse: () -> Unit, onRemove: () -> Unit) {
    val colors = LocalAppColors.current
    val origin = when {
        bible.origin == InstallDetails.ORIGIN_FILE -> stringResource(Res.string.bibles_origin_file)
        bible.origin.isNotBlank() -> bible.origin
        else -> stringResource(Res.string.bibles_origin_desktop)
    }
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .testTag(BiblesTags.installedRow(bible.id))
            .semantics { selected = active }
            .fillMaxWidth()
            .clip(shape)
            .background(if (active) colors.chosenFill else colors.cardFill)
            .border(1.dp, if (active) colors.chosenEdge else colors.cardEdge, shape)
            .clickable(onClick = onUse)
            .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, tint = if (active) colors.accent else colors.muted, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f)) {
            Text(bible.title, color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(bible.abbreviation, bible.languageName, bible.coverage, origin, sizeLabel(bible.sizeBytes))
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                color = colors.selectorSubtle,
                fontSize = 11.sp,
                fontFamily = MonoFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
        Text(
            stringResource(if (active) Res.string.bibles_in_use else Res.string.bibles_use),
            color = if (active) colors.accent else colors.muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        IconButton(onClick = onRemove, modifier = Modifier.testTag(BiblesTags.remove(bible.id)).size(36.dp)) {
            Icon(Icons.Outlined.Delete, contentDescription = stringResource(Res.string.bibles_remove), tint = colors.muted, modifier = Modifier.size(18.dp))
        }
    }
}

