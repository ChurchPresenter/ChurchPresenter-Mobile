package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRightAlt
import androidx.compose.material.icons.outlined.ChangeHistory
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Commit
import androidx.compose.material.icons.outlined.CropDin
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.Hexagon
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.PanoramaFishEye
import androidx.compose.material.icons.outlined.Polyline
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.canvas_align_center
import churchpresentermobile.composeapp.generated.resources.canvas_align_left
import churchpresentermobile.composeapp.generated.resources.canvas_align_right
import churchpresentermobile.composeapp.generated.resources.canvas_background_highlight
import churchpresentermobile.composeapp.generated.resources.canvas_bold
import churchpresentermobile.composeapp.generated.resources.canvas_border_dashed
import churchpresentermobile.composeapp.generated.resources.canvas_border_none
import churchpresentermobile.composeapp.generated.resources.canvas_border_solid
import churchpresentermobile.composeapp.generated.resources.canvas_border_width
import churchpresentermobile.composeapp.generated.resources.canvas_bulleted_list
import churchpresentermobile.composeapp.generated.resources.canvas_crop
import churchpresentermobile.composeapp.generated.resources.canvas_dashed
import churchpresentermobile.composeapp.generated.resources.canvas_drop_shadow
import churchpresentermobile.composeapp.generated.resources.canvas_end_arrow
import churchpresentermobile.composeapp.generated.resources.canvas_end_dot
import churchpresentermobile.composeapp.generated.resources.canvas_end_none
import churchpresentermobile.composeapp.generated.resources.canvas_flip
import churchpresentermobile.composeapp.generated.resources.canvas_font_marker
import churchpresentermobile.composeapp.generated.resources.canvas_font_neat
import churchpresentermobile.composeapp.generated.resources.canvas_font_plain
import churchpresentermobile.composeapp.generated.resources.canvas_italic
import churchpresentermobile.composeapp.generated.resources.canvas_line_curve
import churchpresentermobile.composeapp.generated.resources.canvas_line_elbow
import churchpresentermobile.composeapp.generated.resources.canvas_line_end
import churchpresentermobile.composeapp.generated.resources.canvas_line_start
import churchpresentermobile.composeapp.generated.resources.canvas_line_straight
import churchpresentermobile.composeapp.generated.resources.canvas_opacity
import churchpresentermobile.composeapp.generated.resources.canvas_place_fill
import churchpresentermobile.composeapp.generated.resources.canvas_place_fit
import churchpresentermobile.composeapp.generated.resources.canvas_place_original
import churchpresentermobile.composeapp.generated.resources.canvas_px
import churchpresentermobile.composeapp.generated.resources.canvas_remove_background
import churchpresentermobile.composeapp.generated.resources.canvas_rotate
import churchpresentermobile.composeapp.generated.resources.canvas_rounded_corners
import churchpresentermobile.composeapp.generated.resources.canvas_section_adjust
import churchpresentermobile.composeapp.generated.resources.canvas_section_align
import churchpresentermobile.composeapp.generated.resources.canvas_section_border
import churchpresentermobile.composeapp.generated.resources.canvas_section_color
import churchpresentermobile.composeapp.generated.resources.canvas_section_ends
import churchpresentermobile.composeapp.generated.resources.canvas_section_fill
import churchpresentermobile.composeapp.generated.resources.canvas_section_font
import churchpresentermobile.composeapp.generated.resources.canvas_section_handwriting
import churchpresentermobile.composeapp.generated.resources.canvas_section_insert_from
import churchpresentermobile.composeapp.generated.resources.canvas_section_shape
import churchpresentermobile.composeapp.generated.resources.canvas_section_size
import churchpresentermobile.composeapp.generated.resources.canvas_section_style
import churchpresentermobile.composeapp.generated.resources.canvas_section_when_placed
import churchpresentermobile.composeapp.generated.resources.canvas_snap_shapes
import churchpresentermobile.composeapp.generated.resources.canvas_source_camera
import churchpresentermobile.composeapp.generated.resources.canvas_source_clipboard
import churchpresentermobile.composeapp.generated.resources.canvas_source_files
import churchpresentermobile.composeapp.generated.resources.canvas_source_photos
import churchpresentermobile.composeapp.generated.resources.canvas_sticky_large
import churchpresentermobile.composeapp.generated.resources.canvas_sticky_medium
import churchpresentermobile.composeapp.generated.resources.canvas_sticky_small
import churchpresentermobile.composeapp.generated.resources.canvas_strikethrough
import churchpresentermobile.composeapp.generated.resources.canvas_tilt_notes
import churchpresentermobile.composeapp.generated.resources.canvas_underline
import churchpresentermobile.composeapp.generated.resources.canvas_width
import com.church.presenter.churchpresentermobile.model.BorderStyle
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.ImageAdjust
import com.church.presenter.churchpresentermobile.model.CanvasShape
import com.church.presenter.churchpresentermobile.model.CanvasTextAlign
import com.church.presenter.churchpresentermobile.model.CanvasToolOptions
import com.church.presenter.churchpresentermobile.model.ImagePlacement
import com.church.presenter.churchpresentermobile.model.ImageSource
import com.church.presenter.churchpresentermobile.model.LineEnd
import com.church.presenter.churchpresentermobile.model.LineStyle
import com.church.presenter.churchpresentermobile.model.ShapeFill
import com.church.presenter.churchpresentermobile.model.StickyColor
import com.church.presenter.churchpresentermobile.model.StickyFont
import com.church.presenter.churchpresentermobile.model.StickySize
import com.church.presenter.churchpresentermobile.model.TextOptions
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import com.church.presenter.churchpresentermobile.viewmodel.CanvasUiState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/*
 * The options panels for the tools that place things on the page — shapes, lines,
 * text, sticky notes and pictures.
 */

@Composable
internal fun ShapesPanel(options: CanvasToolOptions, onOptions: (CanvasToolOptions) -> Unit) {
    val shapes = options.shapes
    PanelSectionLabel(stringResource(Res.string.canvas_section_shape), first = true)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CanvasShape.entries.chunked(SHAPES_PER_ROW).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { shape ->
                    OptionTile(
                        icon = shape.icon(),
                        label = stringResource(shape.label()),
                        active = shapes.shape == shape,
                        onClick = { onOptions(options.copy(shapes = shapes.copy(shape = shape))) },
                        showLabel = false,
                        modifier = Modifier.weight(1f),
                        iconModifier = if (shape == CanvasShape.DIAMOND) Modifier.rotate(DIAMOND_TURN) else Modifier,
                    )
                }
            }
        }
    }
    PanelSectionLabel(stringResource(Res.string.canvas_section_fill))
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        ShapeFill.entries.forEach { fill ->
            val pick = { onOptions(options.copy(shapes = shapes.copy(fill = fill, fillCustom = null))) }
            val chosen = shapes.fill == fill && shapes.fillCustom == null
            if (fill == ShapeFill.NONE) {
                NoFillSwatch(chosen, stringResource(fill.label()), 24.dp, pick)
            } else {
                ColorSwatch(shapeFillColor(fill), chosen, stringResource(fill.label()), pick, 24.dp)
            }
        }
        CustomColorSwatch(shapes.fillCustom, 24.dp) { onOptions(options.copy(shapes = shapes.copy(fillCustom = it))) }
    }
    PanelSectionLabel(stringResource(Res.string.canvas_section_border))
    CanvasSegmented(
        labels = listOf(
            stringResource(Res.string.canvas_border_solid),
            stringResource(Res.string.canvas_border_dashed),
            stringResource(Res.string.canvas_border_none),
        ),
        selected = { BorderStyle.entries[it] == shapes.border },
        onSelect = { onOptions(options.copy(shapes = shapes.copy(border = BorderStyle.entries[it]))) },
    )
    Box(Modifier.height(4.dp))
    SliderRow(
        stringResource(Res.string.canvas_border_width),
        stringResource(Res.string.canvas_px, shapes.borderWidth.roundToInt()),
        rangeFraction(shapes.borderWidth, BORDER_WIDTH),
    ) { onOptions(options.copy(shapes = shapes.copy(borderWidth = rangeValue(it, BORDER_WIDTH)))) }
    ToggleRow(stringResource(Res.string.canvas_snap_shapes), shapes.snap) {
        onOptions(options.copy(shapes = shapes.copy(snap = it)))
    }
}

private const val SHAPES_PER_ROW = 4
private const val DIAMOND_TURN = 45f

/** The tile icon for each shape. Material has no diamond outline, so a square is turned on its corner. */
private fun CanvasShape.icon(): ImageVector = when (this) {
    CanvasShape.RECTANGLE -> Icons.Outlined.CropSquare
    CanvasShape.ROUNDED -> Icons.Outlined.CropDin
    CanvasShape.ELLIPSE -> Icons.Outlined.PanoramaFishEye
    CanvasShape.TRIANGLE -> Icons.Outlined.ChangeHistory
    CanvasShape.STAR -> Icons.Outlined.StarBorder
    CanvasShape.DIAMOND -> Icons.Outlined.CropSquare
    CanvasShape.HEXAGON -> Icons.Outlined.Hexagon
    CanvasShape.SPEECH -> Icons.Outlined.ChatBubbleOutline
}

@Composable
internal fun LinePanel(options: CanvasToolOptions, onOptions: (CanvasToolOptions) -> Unit) {
    val line = options.line
    PanelSectionLabel(stringResource(Res.string.canvas_section_style), first = true)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            Triple(LineStyle.STRAIGHT, Icons.Outlined.HorizontalRule, Res.string.canvas_line_straight),
            Triple(LineStyle.CURVE, Icons.Outlined.Timeline, Res.string.canvas_line_curve),
            Triple(LineStyle.ELBOW, Icons.Outlined.Polyline, Res.string.canvas_line_elbow),
        ).forEach { (style, icon, label) ->
            OptionTile(
                icon = icon,
                label = stringResource(label),
                active = line.style == style,
                onClick = { onOptions(options.copy(line = line.copy(style = style))) },
                modifier = Modifier.weight(1f),
                iconModifier = if (style == LineStyle.STRAIGHT) Modifier.rotate(STRAIGHT_TURN) else Modifier,
            )
        }
    }
    PanelSectionLabel(stringResource(Res.string.canvas_section_ends))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LineEndChoice(Res.string.canvas_line_start, line.start, Modifier.weight(1f)) {
            onOptions(options.copy(line = line.copy(start = it)))
        }
        LineEndChoice(Res.string.canvas_line_end, line.end, Modifier.weight(1f)) {
            onOptions(options.copy(line = line.copy(end = it)))
        }
    }
    PanelSectionLabel(stringResource(Res.string.canvas_section_color))
    InkSwatches(
        inks = PanelInks,
        selected = line.ink,
        custom = line.custom,
        onPick = { onOptions(options.copy(line = line.copy(ink = it, custom = null))) },
        onCustom = { onOptions(options.copy(line = line.copy(custom = it))) },
        size = 24.dp,
    )
    Box(Modifier.height(4.dp))
    SliderRow(
        stringResource(Res.string.canvas_width),
        stringResource(Res.string.canvas_px, line.width.roundToInt()),
        rangeFraction(line.width, LINE_WIDTH),
    ) { onOptions(options.copy(line = line.copy(width = rangeValue(it, LINE_WIDTH)))) }
    ToggleRow(stringResource(Res.string.canvas_dashed), line.dashed) {
        onOptions(options.copy(line = line.copy(dashed = it)))
    }
}

/** A diagonal stroke, as the design draws "straight": Material's rule is flat, so it is tipped up. */
private const val STRAIGHT_TURN = -45f

/** One end of a line: plain, arrow or dot, with what it is for underneath. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LineEndChoice(
    caption: StringResource,
    value: LineEnd,
    modifier: Modifier,
    onPick: (LineEnd) -> Unit,
) {
    Column(modifier) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                Triple(LineEnd.NONE, Icons.Outlined.HorizontalRule, Res.string.canvas_end_none),
                Triple(LineEnd.ARROW, Icons.AutoMirrored.Outlined.ArrowRightAlt, Res.string.canvas_end_arrow),
                Triple(LineEnd.DOT, Icons.Outlined.Commit, Res.string.canvas_end_dot),
            ).forEach { (end, icon, label) ->
                CanvasChip(
                    text = null,
                    onClick = { onPick(end) },
                    active = value == end,
                    icon = icon,
                    iconDescription = stringResource(label),
                )
            }
        }
        Text(
            stringResource(caption),
            color = canvasColors.muted,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
internal fun TextPanel(options: CanvasToolOptions, onOptions: (CanvasToolOptions) -> Unit) {
    val text = options.text
    val set: (TextOptions) -> Unit = { onOptions(options.copy(text = it)) }
    PanelSectionLabel(stringResource(Res.string.canvas_section_font), first = true)
    FontRow(text, set)
    PanelSectionLabel(stringResource(Res.string.canvas_section_style))
    val styles = listOf(
        Icons.Outlined.FormatBold to Res.string.canvas_bold,
        Icons.Outlined.FormatItalic to Res.string.canvas_italic,
        Icons.Outlined.FormatUnderlined to Res.string.canvas_underline,
        Icons.Outlined.FormatStrikethrough to Res.string.canvas_strikethrough,
    )
    val on = listOf(text.bold, text.italic, text.underline, text.strikethrough)
    CanvasSegmented(
        labels = styles.map { stringResource(it.second) },
        selected = { on[it] },
        onSelect = { index ->
            set(
                when (index) {
                    0 -> text.copy(bold = !text.bold)
                    1 -> text.copy(italic = !text.italic)
                    2 -> text.copy(underline = !text.underline)
                    else -> text.copy(strikethrough = !text.strikethrough)
                },
            )
        },
    ) { index, active ->
        Icon(
            styles[index].first,
            contentDescription = null,
            tint = if (active) canvasColors.text else canvasColors.muted,
            modifier = Modifier.size(16.dp),
        )
    }
    PanelSectionLabel(stringResource(Res.string.canvas_section_align))
    CanvasSegmented(
        labels = listOf(
            stringResource(Res.string.canvas_align_left),
            stringResource(Res.string.canvas_align_center),
            stringResource(Res.string.canvas_align_right),
        ),
        selected = { CanvasTextAlign.entries[it] == text.align },
        onSelect = { set(text.copy(align = CanvasTextAlign.entries[it])) },
    )
    PanelSectionLabel(stringResource(Res.string.canvas_section_color))
    InkSwatches(
        inks = PanelInks,
        selected = text.ink,
        custom = text.custom,
        onPick = { set(text.copy(ink = it, custom = null)) },
        onCustom = { set(text.copy(custom = it)) },
        size = 24.dp,
    )
    Box(Modifier.height(4.dp))
    ToggleRow(stringResource(Res.string.canvas_background_highlight), text.highlight) {
        set(text.copy(highlight = it))
    }
    ToggleRow(stringResource(Res.string.canvas_bulleted_list), text.bulleted) { set(text.copy(bulleted = it)) }
}

@Composable
internal fun StickyPanel(options: CanvasToolOptions, onOptions: (CanvasToolOptions) -> Unit) {
    val sticky = options.sticky
    val noteShape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomEnd = 12.dp, bottomStart = 6.dp)
    PanelSectionLabel(stringResource(Res.string.canvas_section_color), first = true)
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        StickyColor.entries.forEach { color ->
            Box(Modifier.weight(1f).aspectRatio(1f)) {
                ColorSwatch(
                    color = stickyColor(color),
                    selected = sticky.color == color,
                    description = stringResource(color.label()),
                    onClick = { onOptions(options.copy(sticky = sticky.copy(color = color))) },
                    size = 37.dp,
                    shape = noteShape,
                )
            }
        }
    }
    PanelSectionLabel(stringResource(Res.string.canvas_section_size))
    CanvasSegmented(
        labels = listOf(
            stringResource(Res.string.canvas_sticky_small),
            stringResource(Res.string.canvas_sticky_medium),
            stringResource(Res.string.canvas_sticky_large),
        ),
        selected = { StickySize.entries[it] == sticky.size },
        onSelect = { onOptions(options.copy(sticky = sticky.copy(size = StickySize.entries[it]))) },
    )
    PanelSectionLabel(stringResource(Res.string.canvas_section_handwriting))
    CanvasSegmented(
        labels = listOf(
            stringResource(Res.string.canvas_font_marker),
            stringResource(Res.string.canvas_font_neat),
            stringResource(Res.string.canvas_font_plain),
        ),
        selected = { StickyFont.entries[it] == sticky.font },
        onSelect = { onOptions(options.copy(sticky = sticky.copy(font = StickyFont.entries[it]))) },
    )
    Box(Modifier.height(4.dp))
    ToggleRow(stringResource(Res.string.canvas_tilt_notes), sticky.tilt) {
        onOptions(options.copy(sticky = sticky.copy(tilt = it)))
    }
}

/**
 * Image. "Insert from" brings a picture in from there, and makes it where a tap on
 * the board brings one from too; the
 * Adjust row works on the selected picture — a newly placed one is selected — and
 * is faded while there is none.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ImagePanel(state: CanvasUiState, onIntent: (CanvasIntent) -> Unit) {
    val options = state.options
    val onOptions: (CanvasToolOptions) -> Unit = { onIntent(CanvasIntent.SetOptions(it)) }
    val image = options.image
    val hasImage = state.selection.any { it is CanvasElement.Image && !it.locked }
    PanelSectionLabel(stringResource(Res.string.canvas_section_insert_from), first = true)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            ImageSource.CLIPBOARD to Res.string.canvas_source_clipboard,
            ImageSource.PHOTOS to Res.string.canvas_source_photos,
            ImageSource.CAMERA to Res.string.canvas_source_camera,
            ImageSource.FILES to Res.string.canvas_source_files,
        ).forEach { (source, label) ->
            CanvasChip(
                text = stringResource(label),
                active = image.source == source,
                onClick = {
                    onOptions(options.copy(image = image.copy(source = source)))
                    onIntent(CanvasIntent.InsertImage(source))
                },
            )
        }
    }
    PanelSectionLabel(stringResource(Res.string.canvas_section_when_placed))
    CanvasSegmented(
        labels = listOf(
            stringResource(Res.string.canvas_place_fit),
            stringResource(Res.string.canvas_place_fill),
            stringResource(Res.string.canvas_place_original),
        ),
        selected = { ImagePlacement.entries[it] == image.placement },
        onSelect = { onOptions(options.copy(image = image.copy(placement = ImagePlacement.entries[it]))) },
    )
    Box(Modifier.height(4.dp))
    ToggleRow(stringResource(Res.string.canvas_rounded_corners), image.rounded) {
        onOptions(options.copy(image = image.copy(rounded = it)))
    }
    ToggleRow(stringResource(Res.string.canvas_drop_shadow), image.shadow) {
        onOptions(options.copy(image = image.copy(shadow = it)))
    }
    PercentSlider(Res.string.canvas_opacity, image.opacity) {
        onOptions(options.copy(image = image.copy(opacity = it)))
    }
    PanelSectionLabel(stringResource(Res.string.canvas_section_adjust))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            ImageAdjust.CROP to Res.string.canvas_crop,
            ImageAdjust.FLIP to Res.string.canvas_flip,
            ImageAdjust.ROTATE to Res.string.canvas_rotate,
            ImageAdjust.REMOVE_BACKGROUND to Res.string.canvas_remove_background,
        ).forEach { (adjust, label) ->
            CanvasChip(
                stringResource(label),
                onClick = { onIntent(CanvasIntent.AdjustImage(adjust)) },
                enabled = hasImage,
            )
        }
    }
}
