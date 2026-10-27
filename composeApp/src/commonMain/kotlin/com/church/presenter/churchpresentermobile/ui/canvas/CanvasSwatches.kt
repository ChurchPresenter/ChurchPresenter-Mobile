package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import churchpresentermobile.composeapp.generated.resources.canvas_color_mixed
import com.church.presenter.churchpresentermobile.model.CanvasInk
import com.church.presenter.churchpresentermobile.model.InkPalette
import com.church.presenter.churchpresentermobile.model.PaletteSlot
import com.church.presenter.churchpresentermobile.model.paletteSlots
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.SolidColor
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.canvas_color_custom
import churchpresentermobile.composeapp.generated.resources.canvas_custom_color_title
import com.church.presenter.churchpresentermobile.ui.standalone.ColorPickerDialog
import com.church.presenter.churchpresentermobile.ui.standalone.parseHexColorOrNull
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The selected ring the design puts round a chosen swatch: a gap the color of
 * whatever the swatch sits on, then the accent. Drawn *outside* the swatch, so
 * choosing one does not shift its neighbours.
 *
 * @param gap The color behind the swatch — the panel, or the board under the dock.
 * @param width The gap; the accent ring is as wide again outside it.
 */
internal fun Modifier.selectedRing(selected: Boolean, shape: Shape, gap: Color, accent: Color, width: Dp): Modifier =
    if (!selected) {
        this
    } else {
        drawBehind {
            val step = width.toPx()
            listOf(accent to step * 2, gap to step).forEach { (color, grow) ->
                val outline = shape.createOutline(
                    Size(size.width + grow * 2, size.height + grow * 2),
                    layoutDirection,
                    this,
                )
                translate(-grow, -grow) { drawOutline(outline, color) }
            }
        }
    }

/**
 * One color to pick.
 *
 * @param description What a screen reader calls it — "Red", "Yellow" — since the
 *   color itself is all a sighted operator gets.
 * @param gap See [selectedRing].
 */
@Composable
internal fun ColorSwatch(
    color: Color,
    selected: Boolean,
    description: String,
    onClick: () -> Unit,
    size: Dp,
    shape: Shape = CircleShape,
    gap: Color = canvasColors.panel,
    ringWidth: Dp = 2.dp,
) {
    val colors = canvasColors
    Box(
        modifier = Modifier
            .size(size)
            .selectedRing(selected, shape, gap, colors.accent, ringWidth)
            .clip(shape)
            .background(color)
            .border(1.dp, if (selected) Color.Transparent else SwatchEdge, shape)
            .semantics {
                contentDescription = description
                this.selected = selected
            }
            .clickable(role = Role.RadioButton, onClick = onClick),
    )
}

/**
 * The rainbow swatch at the end of a palette, for a color of the operator's own.
 * It opens the color picker; once a color is mixed the swatch shows it, ringed
 * as the one in use, until a named color is picked instead.
 *
 * @param current The mixed color in use, as "#RRGGBB", or null when a named one is.
 */
@Composable
internal fun CustomColorSwatch(
    current: String?,
    size: Dp,
    /** Where the picker opens; a palette row passes its color in use while [current] stays null. */
    startFrom: String? = current,
    onPick: (String) -> Unit,
) {
    val colors = canvasColors
    var picking by remember { mutableStateOf(false) }
    val title = stringResource(Res.string.canvas_custom_color_title)
    val description = stringResource(Res.string.canvas_color_custom)
    val mixed = current?.let(::parseHexColorOrNull)
    Box(
        modifier = Modifier
            .size(size)
            .selectedRing(mixed != null, CircleShape, colors.panel, colors.accent, 2.dp)
            .clip(CircleShape)
            .background(if (mixed != null) SolidColor(mixed) else Brush.sweepGradient(CustomSwatchColors))
            .semantics {
                contentDescription = description
                selected = mixed != null
            }
            .clickable(role = Role.Button) { picking = true },
    )
    if (picking) {
        ColorPickerDialog(
            title = title,
            initial = startFrom ?: DEFAULT_CUSTOM,
            onPick = {
                onPick(it)
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

/** Where the picker starts when no color has been mixed yet: the accent green. */
private const val DEFAULT_CUSTOM = "#86EFAC"

/** "No fill": a clear circle struck through in red. */
@Composable
internal fun NoFillSwatch(selected: Boolean, description: String, size: Dp, onClick: () -> Unit) {
    val colors = canvasColors
    Box(
        modifier = Modifier
            .size(size)
            .selectedRing(selected, CircleShape, colors.panel, colors.accent, 2.dp)
            .clip(CircleShape)
            .border(1.dp, SwatchEdge, CircleShape)
            .drawBehind {
                val inset = this.size.width * SLASH_INSET
                drawLine(
                    color = colors.danger,
                    start = Offset(this.size.width - inset, inset),
                    end = Offset(inset, this.size.height - inset),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            .semantics {
                contentDescription = description
                this.selected = selected
            }
            .clickable(role = Role.RadioButton, onClick = onClick),
    )
}

/** How far in from the corner the no-fill slash starts, as a share of the swatch. */
private const val SLASH_INSET = 0.15f

/** The faint edge that keeps a white swatch visible on a white panel. */
private val SwatchEdge = Color(0x26000000)

/**
 * An ink row: the board's contrast ink, the [PALETTE_SIZE] colors — the operator's
 * latest [mixes] first, then [InkPalette] — and the picker on the end. Wraps onto a
 * second line in a narrow panel.
 *
 * A mixed [custom] color, when there is one, is the one in use, so it is the mix
 * that is ringed rather than a named ink.
 *
 * @param onMix A color from the picker: put it in use and at the front of [mixes].
 * @param onReuse A mix already in the row, tapped: put it in use as it is.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun InkSwatches(
    selected: CanvasInk,
    custom: String?,
    mixes: List<String>,
    onPick: (CanvasInk) -> Unit,
    onReuse: (String) -> Unit,
    onMix: (String) -> Unit,
    size: Dp,
) {
    val colors = canvasColors
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf(PaletteSlot.Preset(CanvasInk.CONTRAST)).plus(paletteSlots(mixes, InkPalette)).forEach { slot ->
            PaletteSwatch(
                slot = slot,
                custom = custom,
                isPreset = { it == selected },
                presetColor = { colors.inkSwatch(it) },
                presetLabel = { stringResource(it.label(colors.isDark)) },
                onPick = onPick,
                onReuse = onReuse,
                size = size,
            )
        }
        CustomColorSwatch(current = null, size = size, startFrom = custom, onPick = onMix)
    }
}

/**
 * One slot of a color row: a named preset, ringed while it is in use and nothing
 * is mixed; or a color the operator mixed, ringed while it is the one in use.
 */
@Composable
internal fun <T> PaletteSwatch(
    slot: PaletteSlot<T>,
    custom: String?,
    isPreset: (T) -> Boolean,
    presetColor: (T) -> Color,
    presetLabel: @Composable (T) -> String,
    onPick: (T) -> Unit,
    onReuse: (String) -> Unit,
    size: Dp,
    gap: Color = canvasColors.panel,
    ringWidth: Dp = 2.dp,
) {
    when (slot) {
        is PaletteSlot.Preset -> ColorSwatch(
            color = presetColor(slot.value),
            selected = custom == null && isPreset(slot.value),
            description = presetLabel(slot.value),
            onClick = { onPick(slot.value) },
            size = size,
            gap = gap,
            ringWidth = ringWidth,
        )
        is PaletteSlot.Mixed -> ColorSwatch(
            color = parseHexColorOrNull(slot.hex) ?: Color.Transparent,
            selected = custom.equals(slot.hex, ignoreCase = true),
            description = stringResource(Res.string.canvas_color_mixed, slot.hex),
            onClick = { onReuse(slot.hex) },
            size = size,
            gap = gap,
            ringWidth = ringWidth,
        )
    }
}
