package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.canvas_drag_hint
import churchpresentermobile.composeapp.generated.resources.canvas_files_hint
import churchpresentermobile.composeapp.generated.resources.canvas_hide
import churchpresentermobile.composeapp.generated.resources.canvas_insert_image
import churchpresentermobile.composeapp.generated.resources.canvas_layer_reorder
import churchpresentermobile.composeapp.generated.resources.canvas_layers
import churchpresentermobile.composeapp.generated.resources.canvas_layers_done
import churchpresentermobile.composeapp.generated.resources.canvas_paste_clipboard
import churchpresentermobile.composeapp.generated.resources.canvas_paste_clipboard_hint
import churchpresentermobile.composeapp.generated.resources.canvas_photo_library
import churchpresentermobile.composeapp.generated.resources.canvas_photo_library_hint
import churchpresentermobile.composeapp.generated.resources.canvas_replace_image
import churchpresentermobile.composeapp.generated.resources.canvas_show
import churchpresentermobile.composeapp.generated.resources.canvas_source_files
import churchpresentermobile.composeapp.generated.resources.canvas_take_photo
import churchpresentermobile.composeapp.generated.resources.canvas_take_photo_hint
import churchpresentermobile.composeapp.generated.resources.canvas_unlock
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasTool
import com.church.presenter.churchpresentermobile.model.ImageSource
import com.church.presenter.churchpresentermobile.model.InkKind
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import com.church.presenter.churchpresentermobile.viewmodel.CanvasPanel
import com.church.presenter.churchpresentermobile.viewmodel.CanvasUiState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The page's elements, top-most first, as the design's Layers panel lists them.
 *
 * Tapping a row selects that element; the lock (shown only on a locked element)
 * unlocks it, the eye hides or shows it, and dragging a row by its handle moves
 * it up or down the drawing order.
 */
@Composable
internal fun LayersPanel(state: CanvasUiState, onIntent: (CanvasIntent) -> Unit, modifier: Modifier = Modifier) {
    val colors = canvasColors
    Column(
        modifier = modifier
            .testTag(CanvasTags.LAYERS_PANEL)
            .width(310.dp)
            .fillMaxHeight()
            .floatingPanel(colors, RoundedCornerShape(18.dp)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(Res.string.canvas_layers),
                color = colors.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(Res.string.canvas_layers_done),
                color = colors.accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .testTag(CanvasTags.LAYERS_DONE)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(role = Role.Button) { onIntent(CanvasIntent.ShowPanel(CanvasPanel.NONE)) },
            )
        }
        HorizontalDivider(color = colors.divider)
        val list = rememberLazyListState()
        val drag = remember { LayerDrag() }
        LazyColumn(
            state = list,
            modifier = Modifier
                .padding(8.dp)
                .reorderLayers(list, HANDLE_ZONE, drag) { id, steps -> onIntent(CanvasIntent.MoveLayer(id, steps)) },
        ) {
            items(state.page.elements.asReversed(), key = { it.id }) { element ->
                val dragged = drag.id == element.id
                LayerRow(
                    element = element,
                    selected = element.id in state.selectedIds,
                    onIntent = onIntent,
                    modifier = Modifier
                        .zIndex(if (dragged) 1f else 0f)
                        .graphicsLayer { translationY = if (dragged) drag.offset else 0f },
                )
            }
        }
    }
}

/** The strip down a layer row's leading edge that drags it: the handle and the row's padding round it. */
private val HANDLE_ZONE = 40.dp

@Composable
private fun LayerRow(
    element: CanvasElement,
    selected: Boolean,
    onIntent: (CanvasIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = canvasColors
    val shape = RoundedCornerShape(11.dp)
    val spec = element.tool().spec()
    Row(
        modifier = modifier
            .testTag(CanvasTags.layer(element.id))
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) colors.accentTint else Color.Transparent)
            .border(1.dp, if (selected) colors.accentBorder else Color.Transparent, shape)
            .semantics { this.selected = selected }
            .clickable { onIntent(CanvasIntent.Select(element.id)) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Outlined.DragIndicator,
            contentDescription = stringResource(Res.string.canvas_layer_reorder),
            tint = colors.muted,
            modifier = Modifier.size(16.dp),
        )
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(colors.control),
            contentAlignment = Alignment.Center,
        ) {
            Icon(spec.icon, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(18.dp))
        }
        Text(
            text = element.layerName(),
            color = if (selected) colors.accent else colors.text,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (element.locked) {
            LayerFlag(Icons.Outlined.Lock, Res.string.canvas_unlock, CanvasTags.layerLock(element.id)) {
                onIntent(CanvasIntent.ToggleLock(element.id))
            }
        }
        LayerFlag(
            icon = if (element.hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
            description = if (element.hidden) Res.string.canvas_show else Res.string.canvas_hide,
            tag = CanvasTags.layerVisibility(element.id),
        ) { onIntent(CanvasIntent.ToggleHidden(element.id)) }
    }
}

@Composable
private fun LayerFlag(icon: ImageVector, description: StringResource, tag: String, onClick: () -> Unit) {
    Icon(
        imageVector = icon,
        contentDescription = stringResource(description),
        tint = canvasColors.muted,
        modifier = Modifier
            .testTag(tag)
            .clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .size(18.dp),
    )
}

/** What a layer row calls [this]: its own name, a picture's file name, or the name of its kind. */
@Composable
private fun CanvasElement.layerName(): String =
    label ?: (this as? CanvasElement.Image)?.fileName ?: kindLabel()?.let { stringResource(it) }.orEmpty()

/** The tool [this] would have been made with, for its icon in the Layers panel. */
private fun CanvasElement.tool(): CanvasTool = when (this) {
    is CanvasElement.Text -> CanvasTool.TEXT
    is CanvasElement.Ink -> when (kind) {
        InkKind.PEN -> CanvasTool.PEN
        InkKind.HIGHLIGHTER -> CanvasTool.HIGHLIGHTER
        InkKind.ARROW -> CanvasTool.LINE
        InkKind.ERASER -> CanvasTool.ERASER
    }
    is CanvasElement.Image -> CanvasTool.IMAGE
    is CanvasElement.Shape -> CanvasTool.SHAPES
    is CanvasElement.Sticky -> CanvasTool.STICKY
}

/**
 * Where a picture can come from, shown while the image tool is out or after
 * Replace. Picking a source brings a picture in from it — or, for Replace, puts it
 * in the selected picture's place — and makes it the image tool's default.
 * Pictures are placeholders named for their source until the picker plumbing arrives.
 */
@Composable
internal fun InsertImagePanel(state: CanvasUiState, onIntent: (CanvasIntent) -> Unit, modifier: Modifier = Modifier) {
    val colors = canvasColors
    val image = state.options.image
    Column(
        modifier = modifier
            .testTag(CanvasTags.INSERT_PANEL)
            .width(330.dp)
            .floatingPanel(colors, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Text(
            stringResource(
                if (state.replaceTarget != null) Res.string.canvas_replace_image else Res.string.canvas_insert_image,
            ).uppercase(),
            color = colors.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.09.em,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 10.dp),
        )
        listOf(
            InsertRow(
                ImageSource.CLIPBOARD,
                Icons.Outlined.ContentPaste,
                Res.string.canvas_paste_clipboard,
                Res.string.canvas_paste_clipboard_hint,
            ),
            InsertRow(
                ImageSource.PHOTOS,
                Icons.Outlined.PhotoLibrary,
                Res.string.canvas_photo_library,
                Res.string.canvas_photo_library_hint,
            ),
            InsertRow(
                ImageSource.CAMERA,
                Icons.Outlined.CameraAlt,
                Res.string.canvas_take_photo,
                Res.string.canvas_take_photo_hint,
            ),
            InsertRow(
                ImageSource.FILES,
                Icons.Outlined.Folder,
                Res.string.canvas_source_files,
                Res.string.canvas_files_hint,
            ),
        ).forEach { row ->
            InsertSource(row, active = image.source == row.source) {
                onIntent(CanvasIntent.SetOptions(state.options.copy(image = image.copy(source = row.source))))
                onIntent(CanvasIntent.InsertImage(row.source))
            }
        }
        Text(
            text = stringResource(Res.string.canvas_drag_hint),
            color = colors.muted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .drawBehind {
                    drawRoundRect(
                        color = colors.accentBorder,
                        cornerRadius = CornerRadius(12.dp.toPx()),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                        ),
                    )
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

private class InsertRow(
    val source: ImageSource,
    val icon: ImageVector,
    val title: StringResource,
    val hint: StringResource,
)

@Composable
private fun InsertSource(row: InsertRow, active: Boolean, onClick: () -> Unit) {
    val colors = canvasColors
    val shape = RoundedCornerShape(13.dp)
    Row(
        modifier = Modifier
            .testTag(CanvasTags.insertSource(row.source))
            .fillMaxWidth()
            .clip(shape)
            .background(if (active) colors.accentTint else Color.Transparent)
            .border(1.dp, if (active) colors.accentBorder else Color.Transparent, shape)
            .semantics { selected = active }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(colors.control),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                row.icon,
                contentDescription = null,
                tint = if (active) colors.accent else colors.secondary,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(stringResource(row.title), color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(row.hint), color = colors.muted, fontSize = 12.sp)
        }
    }
}
