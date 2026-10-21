package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HighlightAlt
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.Title
import androidx.compose.ui.graphics.vector.ImageVector
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.canvas_tool_eraser
import churchpresentermobile.composeapp.generated.resources.canvas_tool_highlighter
import churchpresentermobile.composeapp.generated.resources.canvas_tool_image
import churchpresentermobile.composeapp.generated.resources.canvas_tool_lasso
import churchpresentermobile.composeapp.generated.resources.canvas_tool_line
import churchpresentermobile.composeapp.generated.resources.canvas_tool_pen
import churchpresentermobile.composeapp.generated.resources.canvas_tool_select
import churchpresentermobile.composeapp.generated.resources.canvas_tool_shapes
import churchpresentermobile.composeapp.generated.resources.canvas_tool_sticky
import churchpresentermobile.composeapp.generated.resources.canvas_tool_text
import com.church.presenter.churchpresentermobile.model.CanvasTool
import org.jetbrains.compose.resources.StringResource

/**
 * A tool's name and icon, shared by the rail, the options panel's title and the
 * Layers panel, so one tool cannot wear two icons on one screen.
 *
 * @param mirrored Draws the icon flipped left-to-right. Material's pointer
 *   ([Icons.Outlined.NearMe]) points up and right; a selection cursor points up and left.
 */
internal data class CanvasToolSpec(
    val label: StringResource,
    val icon: ImageVector,
    val mirrored: Boolean = false,
)

internal fun CanvasTool.spec(): CanvasToolSpec = when (this) {
    CanvasTool.SELECT -> CanvasToolSpec(Res.string.canvas_tool_select, Icons.Outlined.NearMe, mirrored = true)
    CanvasTool.LASSO -> CanvasToolSpec(Res.string.canvas_tool_lasso, Icons.Outlined.HighlightAlt)
    CanvasTool.PEN -> CanvasToolSpec(Res.string.canvas_tool_pen, Icons.Outlined.Edit)
    CanvasTool.HIGHLIGHTER -> CanvasToolSpec(Res.string.canvas_tool_highlighter, Icons.Outlined.BorderColor)
    CanvasTool.ERASER -> CanvasToolSpec(Res.string.canvas_tool_eraser, Icons.Outlined.CleaningServices)
    CanvasTool.SHAPES -> CanvasToolSpec(Res.string.canvas_tool_shapes, Icons.Outlined.Category)
    CanvasTool.LINE -> CanvasToolSpec(Res.string.canvas_tool_line, Icons.Outlined.NorthEast)
    CanvasTool.TEXT -> CanvasToolSpec(Res.string.canvas_tool_text, Icons.Outlined.Title)
    CanvasTool.STICKY -> CanvasToolSpec(Res.string.canvas_tool_sticky, Icons.AutoMirrored.Outlined.StickyNote2)
    CanvasTool.IMAGE -> CanvasToolSpec(Res.string.canvas_tool_image, Icons.Outlined.Image)
}

/**
 * The rail's groups, top to bottom: picking things, drawing freehand, placing
 * things. The design draws a hairline between each.
 */
internal val CanvasToolGroups: List<List<CanvasTool>> = listOf(
    listOf(CanvasTool.SELECT, CanvasTool.LASSO),
    listOf(CanvasTool.PEN, CanvasTool.HIGHLIGHTER, CanvasTool.ERASER),
    listOf(CanvasTool.SHAPES, CanvasTool.LINE, CanvasTool.TEXT, CanvasTool.STICKY, CanvasTool.IMAGE),
)
