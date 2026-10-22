package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.canvas_cancel
import churchpresentermobile.composeapp.generated.resources.canvas_clear
import churchpresentermobile.composeapp.generated.resources.canvas_clear_canvas
import churchpresentermobile.composeapp.generated.resources.canvas_clear_canvas_body
import churchpresentermobile.composeapp.generated.resources.canvas_clear_canvas_title
import churchpresentermobile.composeapp.generated.resources.canvas_clear_page
import churchpresentermobile.composeapp.generated.resources.canvas_delete_page
import churchpresentermobile.composeapp.generated.resources.canvas_duplicate_page
import churchpresentermobile.composeapp.generated.resources.canvas_more_options
import churchpresentermobile.composeapp.generated.resources.canvas_rename
import churchpresentermobile.composeapp.generated.resources.canvas_rename_label
import churchpresentermobile.composeapp.generated.resources.canvas_rename_title
import churchpresentermobile.composeapp.generated.resources.canvas_save
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import com.church.presenter.churchpresentermobile.viewmodel.CanvasUiState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The bar's "more" button and the menu it opens: rename the canvas; duplicate,
 * delete or clear the page on screen; or clear the whole canvas, after asking.
 * Delete is offered only while there is another page to fall back to, and each
 * Clear only while there is something for it to clear.
 */
@Composable
internal fun CanvasMoreMenu(state: CanvasUiState, onIntent: (CanvasIntent) -> Unit) {
    val colors = canvasColors
    var open by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var clearing by remember { mutableStateOf(false) }
    Box {
        BarIcon(
            icon = Icons.Outlined.MoreHoriz,
            description = stringResource(Res.string.canvas_more_options),
            tag = CanvasTags.MORE,
            active = open,
            onClick = { open = true },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = colors.panel) {
            MenuItem(Res.string.canvas_rename, CanvasTags.MENU_RENAME) {
                open = false
                renaming = true
            }
            MenuItem(Res.string.canvas_duplicate_page, CanvasTags.MENU_DUPLICATE_PAGE) {
                open = false
                onIntent(CanvasIntent.DuplicatePage)
            }
            MenuItem(
                Res.string.canvas_delete_page,
                CanvasTags.MENU_DELETE_PAGE,
                enabled = state.document.pages.size > 1,
                danger = true,
            ) {
                open = false
                onIntent(CanvasIntent.DeletePage)
            }
            MenuItem(
                Res.string.canvas_clear_page,
                CanvasTags.MENU_CLEAR_PAGE,
                enabled = state.page.elements.isNotEmpty(),
                danger = true,
            ) {
                open = false
                onIntent(CanvasIntent.ClearPage)
            }
            MenuItem(
                Res.string.canvas_clear_canvas,
                CanvasTags.MENU_CLEAR_CANVAS,
                enabled = state.document.pages.size > 1 || state.page.elements.isNotEmpty() ||
                    state.document.title != null,
                danger = true,
            ) {
                open = false
                clearing = true
            }
        }
    }
    if (clearing) {
        ClearCanvasDialog(
            onClear = {
                onIntent(CanvasIntent.ClearCanvas)
                clearing = false
            },
            onDismiss = { clearing = false },
        )
    }
    if (renaming) {
        RenameDialog(
            current = state.document.title.orEmpty(),
            onSave = {
                onIntent(CanvasIntent.Rename(it))
                renaming = false
            },
            onDismiss = { renaming = false },
        )
    }
}

@Composable
private fun MenuItem(
    label: StringResource,
    tag: String,
    enabled: Boolean = true,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = canvasColors
    DropdownMenuItem(
        text = {
            Text(
                stringResource(label),
                color = when {
                    !enabled -> colors.muted
                    danger -> colors.danger
                    else -> colors.text
                },
            )
        },
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier.testTag(tag),
    )
}

/**
 * Asks before clearing the whole canvas — every page goes, which is too much to
 * lose to a slip of the finger, even though Undo would bring it back.
 */
@Composable
private fun ClearCanvasDialog(onClear: () -> Unit, onDismiss: () -> Unit) {
    val colors = canvasColors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.canvas_clear_canvas_title)) },
        text = { Text(stringResource(Res.string.canvas_clear_canvas_body)) },
        confirmButton = {
            TextButton(onClick = onClear, modifier = Modifier.testTag(CanvasTags.CLEAR_CANVAS_CONFIRM)) {
                Text(stringResource(Res.string.canvas_clear), color = colors.danger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.canvas_cancel)) }
        },
    )
}

/** Names the canvas. Saving an empty name puts it back to "Untitled canvas". */
@Composable
private fun RenameDialog(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.canvas_rename_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.canvas_rename_label)) },
                singleLine = true,
                modifier = Modifier.testTag(CanvasTags.RENAME_FIELD),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, modifier = Modifier.testTag(CanvasTags.RENAME_SAVE)) {
                Text(stringResource(Res.string.canvas_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.canvas_cancel)) }
        },
    )
}
