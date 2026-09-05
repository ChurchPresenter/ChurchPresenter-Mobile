package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.editor_add_section
import churchpresentermobile.composeapp.generated.resources.editor_cancel
import churchpresentermobile.composeapp.generated.resources.editor_discard_body
import churchpresentermobile.composeapp.generated.resources.editor_discard_confirm
import churchpresentermobile.composeapp.generated.resources.editor_discard_title
import churchpresentermobile.composeapp.generated.resources.editor_field_author
import churchpresentermobile.composeapp.generated.resources.editor_field_book
import churchpresentermobile.composeapp.generated.resources.editor_field_copyright
import churchpresentermobile.composeapp.generated.resources.editor_field_number
import churchpresentermobile.composeapp.generated.resources.editor_field_title
import churchpresentermobile.composeapp.generated.resources.editor_keep_editing
import churchpresentermobile.composeapp.generated.resources.editor_move_down
import churchpresentermobile.composeapp.generated.resources.editor_move_up
import churchpresentermobile.composeapp.generated.resources.editor_preview
import churchpresentermobile.composeapp.generated.resources.editor_remove_section
import churchpresentermobile.composeapp.generated.resources.editor_save
import churchpresentermobile.composeapp.generated.resources.editor_section_text_placeholder
import churchpresentermobile.composeapp.generated.resources.editor_sections
import churchpresentermobile.composeapp.generated.resources.editor_song_edit_title
import churchpresentermobile.composeapp.generated.resources.editor_song_new_title
import churchpresentermobile.composeapp.generated.resources.editor_split_section
import churchpresentermobile.composeapp.generated.resources.section_bridge
import churchpresentermobile.composeapp.generated.resources.section_chorus
import churchpresentermobile.composeapp.generated.resources.section_ending
import churchpresentermobile.composeapp.generated.resources.section_tag
import churchpresentermobile.composeapp.generated.resources.section_verse
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.LibraryField
import com.church.presenter.churchpresentermobile.model.SectionType
import com.church.presenter.churchpresentermobile.ui.ScreenHeader
import com.church.presenter.churchpresentermobile.ui.SegmentedControl
import com.church.presenter.churchpresentermobile.ui.standalone.StandaloneOutputScreen
import com.church.presenter.churchpresentermobile.ui.theme.AppDimens
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.LibraryEditorViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Creates and edits a song held on this device.
 *
 * The preview strip is the point of the screen: it renders through the same
 * [StandaloneOutputScreen] the TV uses, so an author sees a section that will
 * not fit *while typing it* rather than discovering it mid-service.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongEditorScreen(
    repository: LibraryRepository,
    songId: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: LibraryEditorViewModel = viewModel(key = "editor_song_$songId") {
        LibraryEditorViewModel(repository)
    }
    val colors = LocalAppColors.current

    LaunchedEffect(songId) { viewModel.editSong(songId) }

    val song by viewModel.song.collectAsState()
    val validation by viewModel.validation.collectAsState()
    val isDirty by viewModel.isDirty.collectAsState()
    val preview by viewModel.previewSlide.collectAsState()

    var showDiscardPrompt by remember { mutableStateOf(false) }
    val close = { if (isDirty) showDiscardPrompt = true else onClose() }

    if (showDiscardPrompt) {
        DiscardDialog(
            onDiscard = { showDiscardPrompt = false; onClose() },
            onKeepEditing = { showDiscardPrompt = false },
        )
    }

    Column(modifier = modifier.fillMaxSize().background(colors.background).imePadding()) {
        ScreenHeader(
            title = stringResource(
                if (songId == null) Res.string.editor_song_new_title else Res.string.editor_song_edit_title
            ),
            largeTitle = false,
            onBack = close,
        )

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = AppDimens.space16),
            verticalArrangement = Arrangement.spacedBy(AppDimens.space12),
        ) {
            item {
                EditorField(
                    label = stringResource(Res.string.editor_field_title),
                    value = song.title,
                    onValueChange = viewModel::setSongTitle,
                    error = validation.errors[LibraryField.TITLE],
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.space8)) {
                    EditorField(
                        label = stringResource(Res.string.editor_field_number),
                        value = song.number,
                        onValueChange = viewModel::setSongNumber,
                        warning = validation.warnings[LibraryField.NUMBER],
                        modifier = Modifier.weight(1f),
                    )
                    EditorField(
                        label = stringResource(Res.string.editor_field_book),
                        value = song.bookName.orEmpty(),
                        onValueChange = viewModel::setSongBook,
                        modifier = Modifier.weight(2f),
                    )
                }
            }
            item {
                EditorField(
                    label = stringResource(Res.string.editor_field_author),
                    value = song.author.orEmpty(),
                    onValueChange = viewModel::setSongAuthor,
                )
            }
            item {
                EditorField(
                    label = stringResource(Res.string.editor_field_copyright),
                    value = song.copyright.orEmpty(),
                    onValueChange = viewModel::setSongCopyright,
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(Res.string.editor_preview),
                        color = colors.muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(AppDimens.radiusChip)),
                    ) {
                        StandaloneOutputScreen(preview)
                    }
                }
            }

            item {
                Text(
                    text = stringResource(Res.string.editor_sections),
                    color = colors.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                validation.errors[LibraryField.SECTIONS]?.let { ProblemText(it, isError = true) }
                validation.warnings[LibraryField.SECTIONS]?.let { ProblemText(it, isError = false) }
            }

            itemsIndexed(song.sections) { index, section ->
                SectionCard(
                    index = index,
                    type = section.type,
                    text = section.text,
                    isFirst = index == 0,
                    isLast = index == song.sections.lastIndex,
                    onTypeChange = { viewModel.setSectionType(index, it) },
                    onTextChange = {
                        viewModel.setSectionText(index, it)
                        viewModel.previewSection(index)
                    },
                    onFocus = { viewModel.previewSection(index) },
                    onMoveUp = { viewModel.moveSectionUp(index) },
                    onMoveDown = { viewModel.moveSectionDown(index) },
                    onRemove = { viewModel.removeSection(index) },
                    onSplit = { viewModel.splitSection(index) },
                )
            }

            item {
                TextButton(onClick = { viewModel.addSection() }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(
                        text = stringResource(Res.string.editor_add_section),
                        modifier = Modifier.padding(start = 6.dp),
                        fontSize = 13.sp,
                    )
                }
            }
            item { Box(Modifier.size(AppDimens.space16)) }
        }

        EditorActions(
            canSave = validation.isValid,
            onCancel = close,
            onSave = { if (viewModel.saveSong()) onClose() },
        )
    }
}

@Composable
private fun SectionCard(
    index: Int,
    type: SectionType,
    text: String,
    isFirst: Boolean,
    isLast: Boolean,
    onTypeChange: (SectionType) -> Unit,
    onTextChange: (String) -> Unit,
    onFocus: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onSplit: () -> Unit,
) {
    val colors = LocalAppColors.current
    // Splitting only makes sense when there is a blank line to split on.
    val canSplit = text.contains(Regex("\\n\\s*\\n"))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.radiusCard))
            .background(colors.surface)
            .padding(AppDimens.space12),
        verticalArrangement = Arrangement.spacedBy(AppDimens.space8),
    ) {
        SegmentedControl(
            options = SECTION_TYPES.map { it.label() },
            selectedIndex = SECTION_TYPES.indexOf(type).coerceAtLeast(0),
            onSelect = { onTypeChange(SECTION_TYPES[it]) },
        )

        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text(stringResource(Res.string.editor_section_text_placeholder), fontSize = 13.sp) },
            minLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onFocus),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${index + 1}",
                color = colors.muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(end = AppDimens.space8),
            )
            if (canSplit) {
                IconAction(Icons.Filled.CallSplit, stringResource(Res.string.editor_split_section), onSplit)
            }
            if (!isFirst) {
                IconAction(Icons.Filled.ArrowUpward, stringResource(Res.string.editor_move_up), onMoveUp)
            }
            if (!isLast) {
                IconAction(Icons.Filled.ArrowDownward, stringResource(Res.string.editor_move_down), onMoveDown)
            }
            Box(Modifier.weight(1f))
            IconAction(
                icon = Icons.Filled.Delete,
                description = stringResource(Res.string.editor_remove_section),
                onClick = onRemove,
                tint = colors.danger,
            )
        }
    }
}

@Composable
private fun IconAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color? = null,
) {
    val colors = LocalAppColors.current
    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = tint ?: colors.muted,
        modifier = Modifier
            .clip(RoundedCornerShape(AppDimens.radiusChip))
            .clickable(onClick = onClick)
            .padding(6.dp)
            .size(16.dp),
    )
}

@Composable
internal fun EditorField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    warning: String? = null,
    minLines: Int = 1,
) {
    val colors = LocalAppColors.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = colors.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            isError = error != null,
            minLines = minLines,
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { ProblemText(it, isError = true) }
        if (error == null) warning?.let { ProblemText(it, isError = false) }
    }
}

@Composable
internal fun ProblemText(message: String, isError: Boolean) {
    val colors = LocalAppColors.current
    Text(
        text = message,
        color = if (isError) colors.danger else colors.amber,
        fontSize = 11.sp,
    )
}

@Composable
internal fun EditorActions(canSave: Boolean, onCancel: () -> Unit, onSave: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(AppDimens.space16),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space8),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(AppDimens.radiusButton))
                .background(colors.surface)
                .clickable(onClick = onCancel)
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(Res.string.editor_cancel), color = colors.text, fontSize = 14.sp)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(AppDimens.radiusButton))
                .background(if (canSave) colors.accent else colors.surface)
                .clickable(enabled = canSave, onClick = onSave)
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.editor_save),
                color = if (canSave) colors.onAccent else colors.muted,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
internal fun DiscardDialog(onDiscard: () -> Unit, onKeepEditing: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text(stringResource(Res.string.editor_discard_title)) },
        text = { Text(stringResource(Res.string.editor_discard_body)) },
        confirmButton = {
            TextButton(onClick = onDiscard) { Text(stringResource(Res.string.editor_discard_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onKeepEditing) { Text(stringResource(Res.string.editor_keep_editing)) }
        },
    )
}

@Composable
private fun SectionType.label(): String = when (this) {
    SectionType.VERSE -> stringResource(Res.string.section_verse)
    SectionType.CHORUS -> stringResource(Res.string.section_chorus)
    SectionType.BRIDGE -> stringResource(Res.string.section_bridge)
    SectionType.TAG -> stringResource(Res.string.section_tag)
    SectionType.ENDING -> stringResource(Res.string.section_ending)
}

private val SECTION_TYPES = SectionType.entries
