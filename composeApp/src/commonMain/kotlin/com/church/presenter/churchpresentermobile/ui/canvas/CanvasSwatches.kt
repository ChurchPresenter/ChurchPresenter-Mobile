package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
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
 * The selected ring the design puts round a chosen swatch: a gap the colour of
 * whatever the swatch sits on, then the accent. Drawn *outside* the swatch, so
 * choosing one does not shift its neighbours.
 *
 * @param gap The colour behind the swatch — the panel, or the board under the dock.
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
 * One colour to pick.
 *
 * @param description What a screen reader calls it — "Red", "Yellow" — since the
 *   colour itself is all a sighted operator gets.
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
 * The rainbow swatch at the end of a palette, for a colour of the operator's own.
 * The picker it opens is not built yet, so it does nothing when pressed.
 */
@Composable
internal fun CustomSwatch(description: String, size: Dp, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.sweepGradient(CustomSwatchColors))
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
    )
}

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
