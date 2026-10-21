package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.PanoramaFishEye
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.canvas_align
import churchpresentermobile.composeapp.generated.resources.canvas_back
import churchpresentermobile.composeapp.generated.resources.canvas_clear_page
import churchpresentermobile.composeapp.generated.resources.canvas_color_custom
import churchpresentermobile.composeapp.generated.resources.canvas_distribute
import churchpresentermobile.composeapp.generated.resources.canvas_draw_behind
import churchpresentermobile.composeapp.generated.resources.canvas_erase_area
import churchpresentermobile.composeapp.generated.resources.canvas_erase_highlighter_only
import churchpresentermobile.composeapp.generated.resources.canvas_erase_pixel
import churchpresentermobile.composeapp.generated.resources.canvas_erase_stroke
import churchpresentermobile.composeapp.generated.resources.canvas_everything
import churchpresentermobile.composeapp.generated.resources.canvas_freeform
import churchpresentermobile.composeapp.generated.resources.canvas_front
import churchpresentermobile.composeapp.generated.resources.canvas_group
import churchpresentermobile.composeapp.generated.resources.canvas_include_partial
import churchpresentermobile.composeapp.generated.resources.canvas_ink_only
import churchpresentermobile.composeapp.generated.resources.canvas_objects
import churchpresentermobile.composeapp.generated.resources.canvas_opacity
import churchpresentermobile.composeapp.generated.resources.canvas_pen_ballpoint
import churchpresentermobile.composeapp.generated.resources.canvas_pen_brush
import churchpresentermobile.composeapp.generated.resources.canvas_pen_fountain
import churchpresentermobile.composeapp.generated.resources.canvas_pen_pencil
import churchpresentermobile.composeapp.generated.resources.canvas_percent
import churchpresentermobile.composeapp.generated.resources.canvas_pressure
import churchpresentermobile.composeapp.generated.resources.canvas_px
import churchpresentermobile.composeapp.generated.resources.canvas_rectangle
import churchpresentermobile.composeapp.generated.resources.canvas_section_arrange
import churchpresentermobile.composeapp.generated.resources.canvas_section_color
import churchpresentermobile.composeapp.generated.resources.canvas_section_mode
import churchpresentermobile.composeapp.generated.resources.canvas_section_pen_type
import churchpresentermobile.composeapp.generated.resources.canvas_section_picks_up
import churchpresentermobile.composeapp.generated.resources.canvas_section_selection
import churchpresentermobile.composeapp.generated.resources.canvas_section_shape
import churchpresentermobile.composeapp.generated.resources.canvas_select_hint
import churchpresentermobile.composeapp.generated.resources.canvas_select_locked
import churchpresentermobile.composeapp.generated.resources.canvas_size
import churchpresentermobile.composeapp.generated.resources.canvas_smart_guides
import churchpresentermobile.composeapp.generated.resources.canvas_smoothing
import churchpresentermobile.composeapp.generated.resources.canvas_snap_to_grid
import churchpresentermobile.composeapp.generated.resources.canvas_straight_only
import churchpresentermobile.composeapp.generated.resources.canvas_width
import com.church.presenter.churchpresentermobile.model.CanvasInk
import com.church.presenter.churchpresentermobile.model.CanvasTool
import com.church.presenter.churchpresentermobile.model.CanvasToolOptions
import com.church.presenter.churchpresentermobile.model.EraserMode
import com.church.presenter.churchpresentermobile.model.HighlightColor
import com.church.presenter.churchpresentermobile.model.LassoPicks
import com.church.presenter.churchpresentermobile.model.LassoShape
import com.church.presenter.churchpresentermobile.model.PenType
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * The active tool's options, out beside the rail: a titled card with a pointer on
 * its left edge at [pointerY], level with the tool's button.
 *
 * @param pointerY Where the pointer sits, measured from the card's top.
 */
@Composable
internal fun ToolOptionsPanel(
    tool: CanvasTool,
    options: CanvasToolOptions,
    onOptions: (CanvasToolOptions) -> Unit,
    pointerY: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = canvasColors
    val spec = tool.spec()
    Box(modifier = modifier.testTag(CanvasTags.OPTIONS)) {
        Column(
            modifier = Modifier
                .width(290.dp)
                .floatingPanel(colors, RoundedCornerShape(16.dp))
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier.padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = spec.icon,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(18.dp).then(if (spec.mirrored) Modifier.scale(-1f, 1f) else Modifier),
                )
                Text(
                    stringResource(spec.label),
                    color = colors.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.02).em,
                )
            }
            HorizontalDivider(color = colors.divider)
            Column(modifier = Modifier.padding(top = 12.dp)) { ToolOptionsBody(tool, options, onOptions) }
        }
        PanelPointer(Modifier.offset(x = (-7).dp, y = pointerY - 6.dp))
    }
}

/**
 * The design's pointer: a small square turned 45° so its left half pokes out of
 * the card, outlined on the two sides that show.
 */
@Composable
private fun PanelPointer(modifier: Modifier) {
    val colors = canvasColors
    Box(
        modifier = modifier
            .size(12.dp)
            .rotate(POINTER_TURN)
            .background(colors.panel)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(colors.border, Offset(0f, 0f), Offset(0f, size.height), stroke)
                drawLine(colors.border, Offset(0f, size.height), Offset(size.width, size.height), stroke)
            },
    )
}

private const val POINTER_TURN = 45f

@Composable
private fun ToolOptionsBody(tool: CanvasTool, options: CanvasToolOptions, onOptions: (CanvasToolOptions) -> Unit) {
    when (tool) {
        CanvasTool.SELECT -> SelectPanel(options, onOptions)
        CanvasTool.LASSO -> LassoPanel(options, onOptions)
        CanvasTool.PEN -> PenPanel(options, onOptions)
        CanvasTool.HIGHLIGHTER -> HighlighterPanel(options, onOptions)
        CanvasTool.ERASER -> EraserPanel(options, onOptions)
        CanvasTool.SHAPES -> ShapesPanel(options, onOptions)
        CanvasTool.LINE -> LinePanel(options, onOptions)
        CanvasTool.TEXT -> TextPanel(options, onOptions)
        CanvasTool.STICKY -> StickyPanel(options, onOptions)
        CanvasTool.IMAGE -> ImagePanel(options, onOptions)
    }
}

/**
 * Select. Arrange's buttons act on a multi-selection, which arrives with the
 * editing pass; for now they are the design's controls with nothing behind them.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectPanel(options: CanvasToolOptions, onOptions: (CanvasToolOptions) -> Unit) {
    val select = options.select
    PanelSectionLabel(stringResource(Res.string.canvas_section_selection), first = true)
    ToggleRow(stringResource(Res.string.canvas_snap_to_grid), select.snapToGrid) {
        onOptions(options.copy(select = select.copy(snapToGrid = it)))
    }
    ToggleRow(stringResource(Res.string.canvas_smart_guides), select.smartGuides) {
        onOptions(options.copy(select = select.copy(smartGuides = it)))
    }
    ToggleRow(stringResource(Res.string.canvas_select_locked), select.selectLocked) {
        onOptions(options.copy(select = select.copy(selectLocked = it)))
    }
    PanelSectionLabel(stringResource(Res.string.canvas_section_arrange))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            Res.string.canvas_align,
            Res.string.canvas_distribute,
            Res.string.canvas_group,
            Res.string.canvas_front,
            Res.string.canvas_back,
        ).forEach { CanvasChip(stringResource(it), onClick = {}) }
    }
    Text(
        stringResource(Res.string.canvas_select_hint),
        color = canvasColors.muted,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun LassoPanel(options: CanvasToolOptions, onOptions: (CanvasToolOptions) -> Unit) {
    val lasso = options.lasso
    PanelSectionLabel(stringResource(Res.string.canvas_section_shape), first = true)
    CanvasSegmented(
        labels = listOf(stringResource(Res.string.canvas_freeform), stringResource(Res.string.canvas_rectangle)),
        selected = { LassoShape.entries[it] == lasso.shape },
        onSelect = { onOptions(options.copy(lasso = lasso.copy(shape = LassoShape.entries[it]))) },
    )
    PanelSectionLabel(stringResource(Res.string.canvas_section_picks_up))
    CanvasSegmented(
        labels = listOf(
            stringResource(Res.string.canvas_everything),
            stringResource(Res.string.canvas_ink_only),
            stringResource(Res.string.canvas_objects),
        ),
        selected = { LassoPicks.entries[it] == lasso.picks },
        onSelect = { onOptions(options.copy(lasso = lasso.copy(picks = LassoPicks.entries[it]))) },
    )
    Box(Modifier.padding(top = 4.dp)) {
        ToggleRow(stringResource(Res.string.canvas_include_partial), lasso.partial) {
            onOptions(options.copy(lasso = lasso.copy(partial = it)))
        }
    }
}

@Composable
private fun PenPanel(options: CanvasToolOptions, onOptions: (CanvasToolOptions) -> Unit) {
    val pen = options.pen
    PanelSectionLabel(stringResource(Res.string.canvas_section_pen_type), first = true)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            Triple(PenType.BALLPOINT, Icons.Outlined.Edit, Res.string.canvas_pen_ballpoint),
            Triple(PenType.FOUNTAIN, Icons.Outlined.Draw, Res.string.canvas_pen_fountain),
            Triple(PenType.BRUSH, Icons.Outlined.Brush, Res.string.canvas_pen_brush),
            Triple(PenType.PENCIL, Icons.Filled.Create, Res.string.canvas_pen_pencil),
        ).forEach { (type, icon, label) ->
            OptionTile(
                icon = icon,
                label = stringResource(label),
                active = pen.type == type,
                onClick = { onOptions(options.copy(pen = pen.copy(type = type))) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    PanelSectionLabel(stringResource(Res.string.canvas_section_color))
    InkSwatches(
        inks = PanelInks,
        selected = pen.ink,
        onPick = { onOptions(options.copy(pen = pen.copy(ink = it))) },
        size = 26.dp,
    )
    Box(Modifier.height(4.dp))
    SliderRow(
        stringResource(Res.string.canvas_width),
        stringResource(Res.string.canvas_px, pen.width.roundToInt()),
        rangeFraction(pen.width, PEN_WIDTH),
    ) { onOptions(options.copy(pen = pen.copy(width = rangeValue(it, PEN_WIDTH)))) }
    PercentSlider(Res.string.canvas_opacity, pen.opacity) { onOptions(options.copy(pen = pen.copy(opacity = it))) }
    PercentSlider(Res.string.canvas_smoothing, pen.smoothing) {
        onOptions(options.copy(pen = pen.copy(smoothing = it)))
    }
    ToggleRow(stringResource(Res.string.canvas_pressure), pen.pressure) {
        onOptions(options.copy(pen = pen.copy(pressure = it)))
    }
}

@Composable
private fun HighlighterPanel(options: CanvasToolOptions, onOptions: (CanvasToolOptions) -> Unit) {
    val marker = options.highlighter
    PanelSectionLabel(stringResource(Res.string.canvas_section_color), first = true)
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        HighlightColor.entries.forEach { color ->
            ColorSwatch(
                color = highlightColor(color),
                selected = marker.color == color,
                description = stringResource(color.label()),
                onClick = { onOptions(options.copy(highlighter = marker.copy(color = color))) },
                size = 26.dp,
            )
        }
        CustomSwatch(stringResource(Res.string.canvas_color_custom), 26.dp)
    }
    Box(Modifier.height(4.dp))
    SliderRow(
        stringResource(Res.string.canvas_width),
        stringResource(Res.string.canvas_px, marker.width.roundToInt()),
        rangeFraction(marker.width, HIGHLIGHTER_WIDTH),
    ) { onOptions(options.copy(highlighter = marker.copy(width = rangeValue(it, HIGHLIGHTER_WIDTH)))) }
    PercentSlider(Res.string.canvas_opacity, marker.opacity) {
        onOptions(options.copy(highlighter = marker.copy(opacity = it)))
    }
    ToggleRow(stringResource(Res.string.canvas_straight_only), marker.straightOnly) {
        onOptions(options.copy(highlighter = marker.copy(straightOnly = it)))
    }
    ToggleRow(stringResource(Res.string.canvas_draw_behind), marker.behindInk) {
        onOptions(options.copy(highlighter = marker.copy(behindInk = it)))
    }
}

/** Eraser. Erasing itself, and Clear page, arrive with the editing pass; the settings are here now. */
@Composable
private fun EraserPanel(options: CanvasToolOptions, onOptions: (CanvasToolOptions) -> Unit) {
    val eraser = options.eraser
    val colors = canvasColors
    PanelSectionLabel(stringResource(Res.string.canvas_section_mode), first = true)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            Triple(EraserMode.STROKE, Icons.Outlined.Gesture, Res.string.canvas_erase_stroke),
            Triple(EraserMode.PIXEL, Icons.Outlined.PanoramaFishEye, Res.string.canvas_erase_pixel),
            Triple(EraserMode.AREA, Icons.Outlined.CropFree, Res.string.canvas_erase_area),
        ).forEach { (mode, icon, label) ->
            OptionTile(
                icon = icon,
                label = stringResource(label),
                active = eraser.mode == mode,
                onClick = { onOptions(options.copy(eraser = eraser.copy(mode = mode))) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    Box(Modifier.height(4.dp))
    SliderRow(
        stringResource(Res.string.canvas_size),
        stringResource(Res.string.canvas_px, eraser.size.roundToInt()),
        rangeFraction(eraser.size, ERASER_SIZE),
    ) { onOptions(options.copy(eraser = eraser.copy(size = rangeValue(it, ERASER_SIZE)))) }
    ToggleRow(stringResource(Res.string.canvas_erase_highlighter_only), eraser.highlighterOnly) {
        onOptions(options.copy(eraser = eraser.copy(highlighterOnly = it)))
    }
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .height(40.dp)
            .clip(shape)
            .background(colors.dangerTint)
            .border(1.dp, colors.dangerBorder, shape)
            .clickable(role = Role.Button) {},
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(Res.string.canvas_clear_page),
            color = colors.danger,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** A labelled 0–100% slider over a 0–1 value. */
@Composable
internal fun PercentSlider(
    label: StringResource,
    value: Float,
    onValue: (Float) -> Unit,
) {
    SliderRow(
        stringResource(label),
        stringResource(Res.string.canvas_percent, (value * PERCENT).roundToInt()),
        value,
        onValue,
    )
}

private const val PERCENT = 100

/** A row of ink swatches with the custom-colour swatch on the end. */
@Composable
internal fun InkSwatches(inks: List<CanvasInk>, selected: CanvasInk, onPick: (CanvasInk) -> Unit, size: Dp) {
    val colors = canvasColors
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        inks.forEach { ink ->
            ColorSwatch(
                color = colors.inkSwatch(ink),
                selected = ink == selected,
                description = stringResource(ink.label(colors.isDark)),
                onClick = { onPick(ink) },
                size = size,
            )
        }
        CustomSwatch(stringResource(Res.string.canvas_color_custom), size)
    }
}

/** The inks a panel offers: the board's contrast ink and the five colours. */
internal val PanelInks: List<CanvasInk> = CanvasInk.entries - CanvasInk.INVERSE

/** Each slider's range, set so the design's default lands where the design draws its knob. */
internal val PEN_WIDTH = 0f..14f
internal val HIGHLIGHTER_WIDTH = 6f..26f
internal val ERASER_SIZE = 4f..54f
internal val BORDER_WIDTH = 0f..10f
internal val LINE_WIDTH = 1f..11f
