package com.church.presenter.churchpresentermobile.ui.canvas

import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.canvas_color_amber
import churchpresentermobile.composeapp.generated.resources.canvas_color_black
import churchpresentermobile.composeapp.generated.resources.canvas_color_blue
import churchpresentermobile.composeapp.generated.resources.canvas_color_green
import churchpresentermobile.composeapp.generated.resources.canvas_color_none
import churchpresentermobile.composeapp.generated.resources.canvas_color_orange
import churchpresentermobile.composeapp.generated.resources.canvas_color_pink
import churchpresentermobile.composeapp.generated.resources.canvas_color_purple
import churchpresentermobile.composeapp.generated.resources.canvas_color_red
import churchpresentermobile.composeapp.generated.resources.canvas_color_white
import churchpresentermobile.composeapp.generated.resources.canvas_color_yellow
import churchpresentermobile.composeapp.generated.resources.canvas_layer_arrow
import churchpresentermobile.composeapp.generated.resources.canvas_layer_eraser
import churchpresentermobile.composeapp.generated.resources.canvas_layer_highlight
import churchpresentermobile.composeapp.generated.resources.canvas_layer_ink
import churchpresentermobile.composeapp.generated.resources.canvas_layer_shape
import churchpresentermobile.composeapp.generated.resources.canvas_layer_sticky
import churchpresentermobile.composeapp.generated.resources.canvas_layer_text
import churchpresentermobile.composeapp.generated.resources.canvas_shape_diamond
import churchpresentermobile.composeapp.generated.resources.canvas_shape_ellipse
import churchpresentermobile.composeapp.generated.resources.canvas_shape_hexagon
import churchpresentermobile.composeapp.generated.resources.canvas_shape_rectangle
import churchpresentermobile.composeapp.generated.resources.canvas_shape_rounded
import churchpresentermobile.composeapp.generated.resources.canvas_shape_speech
import churchpresentermobile.composeapp.generated.resources.canvas_shape_star
import churchpresentermobile.composeapp.generated.resources.canvas_shape_triangle
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasInk
import com.church.presenter.churchpresentermobile.model.CanvasShape
import com.church.presenter.churchpresentermobile.model.HighlightColor
import com.church.presenter.churchpresentermobile.model.InkKind
import com.church.presenter.churchpresentermobile.model.ShapeFill
import com.church.presenter.churchpresentermobile.model.StickyColor
import org.jetbrains.compose.resources.StringResource

/*
 * What a screen reader calls each colour and shape, and what the Layers panel
 * calls an element nobody named. A swatch is only a colour to look at, so its
 * name is the only label a screen-reader user gets.
 */

/** The ink's colour name on this theme — contrast ink is white on the dark board and black on the light one. */
internal fun CanvasInk.label(isDark: Boolean): StringResource = when (this) {
    CanvasInk.CONTRAST -> if (isDark) Res.string.canvas_color_white else Res.string.canvas_color_black
    CanvasInk.INVERSE -> if (isDark) Res.string.canvas_color_black else Res.string.canvas_color_white
    CanvasInk.RED -> Res.string.canvas_color_red
    CanvasInk.AMBER -> Res.string.canvas_color_amber
    CanvasInk.GREEN -> Res.string.canvas_color_green
    CanvasInk.SKY -> Res.string.canvas_color_blue
    CanvasInk.VIOLET -> Res.string.canvas_color_purple
}

internal fun HighlightColor.label(): StringResource = when (this) {
    HighlightColor.YELLOW -> Res.string.canvas_color_yellow
    HighlightColor.GREEN -> Res.string.canvas_color_green
    HighlightColor.SKY -> Res.string.canvas_color_blue
    HighlightColor.PINK -> Res.string.canvas_color_pink
    HighlightColor.ORANGE -> Res.string.canvas_color_orange
}

internal fun ShapeFill.label(): StringResource = when (this) {
    ShapeFill.NONE -> Res.string.canvas_color_none
    ShapeFill.GREEN -> Res.string.canvas_color_green
    ShapeFill.SKY -> Res.string.canvas_color_blue
    ShapeFill.YELLOW -> Res.string.canvas_color_yellow
    ShapeFill.PINK -> Res.string.canvas_color_pink
}

internal fun StickyColor.label(): StringResource = when (this) {
    StickyColor.YELLOW -> Res.string.canvas_color_yellow
    StickyColor.GREEN -> Res.string.canvas_color_green
    StickyColor.BLUE -> Res.string.canvas_color_blue
    StickyColor.PINK -> Res.string.canvas_color_pink
    StickyColor.ORANGE -> Res.string.canvas_color_orange
    StickyColor.PURPLE -> Res.string.canvas_color_purple
}

internal fun CanvasShape.label(): StringResource = when (this) {
    CanvasShape.RECTANGLE -> Res.string.canvas_shape_rectangle
    CanvasShape.ROUNDED -> Res.string.canvas_shape_rounded
    CanvasShape.ELLIPSE -> Res.string.canvas_shape_ellipse
    CanvasShape.TRIANGLE -> Res.string.canvas_shape_triangle
    CanvasShape.STAR -> Res.string.canvas_shape_star
    CanvasShape.DIAMOND -> Res.string.canvas_shape_diamond
    CanvasShape.HEXAGON -> Res.string.canvas_shape_hexagon
    CanvasShape.SPEECH -> Res.string.canvas_shape_speech
}

/**
 * What the Layers panel calls an element the operator never named, or null for
 * a picture, which goes by its file name.
 */
internal fun CanvasElement.kindLabel(): StringResource? = when (this) {
    is CanvasElement.Text -> Res.string.canvas_layer_text
    is CanvasElement.Ink -> when (kind) {
        InkKind.PEN -> Res.string.canvas_layer_ink
        InkKind.HIGHLIGHTER -> Res.string.canvas_layer_highlight
        InkKind.ARROW -> Res.string.canvas_layer_arrow
        InkKind.ERASER -> Res.string.canvas_layer_eraser
    }
    is CanvasElement.Image -> null
    is CanvasElement.Shape -> Res.string.canvas_layer_shape
    is CanvasElement.Sticky -> Res.string.canvas_layer_sticky
}
