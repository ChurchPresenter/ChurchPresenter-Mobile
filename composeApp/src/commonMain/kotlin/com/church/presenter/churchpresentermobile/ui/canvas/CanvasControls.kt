package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/*
 * The parts every canvas panel is built from — the design's toggle rows, sliders,
 * segmented controls, chips and tiles, drawn in [CanvasColors] rather than the
 * app's Material theme so they sit on the canvas's floating panels.
 */

/** The look of everything that floats over the board: shadow, fill and a hairline edge. */
internal fun Modifier.floatingPanel(colors: CanvasColors, shape: Shape): Modifier = this
    .shadow(elevation = 14.dp, shape = shape, ambientColor = colors.shadow, spotColor = colors.shadow)
    .clip(shape)
    .background(colors.panel)
    .border(1.dp, colors.border, shape)

/** An overline such as "PEN TYPE" between a panel's groups. [first] sits closer to the panel's title. */
@Composable
internal fun PanelSectionLabel(text: String, first: Boolean = false) {
    val colors = canvasColors
    Text(
        text = text.uppercase(),
        color = colors.muted,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.09.em,
        modifier = Modifier.padding(top = if (first) 2.dp else 14.dp, bottom = 9.dp),
    )
}

/** A labelled switch on its own line, as the design's settings rows are. */
@Composable
internal fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = canvasColors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = label,
                color = colors.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            CanvasSwitch(checked)
        }
        HorizontalDivider(color = colors.divider)
    }
}

/** The design's 40×24 switch: accent when on, a faint track when off, a white knob either way. */
@Composable
private fun CanvasSwitch(checked: Boolean) {
    val colors = canvasColors
    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) colors.accent else colors.track),
    ) {
        Box(
            modifier = Modifier
                .offset(x = if (checked) 19.dp else 3.dp, y = 3.dp)
                .size(18.dp)
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}

/**
 * A labelled slider: the name and its value on one line, the track under them.
 *
 * @param fraction Where the knob sits, 0–1.
 * @param onFraction Called with the new position as the operator taps or drags.
 */
@Composable
internal fun SliderRow(label: String, value: String, fraction: Float, onFraction: (Float) -> Unit) {
    val colors = canvasColors
    Column {
        Column(modifier = Modifier.padding(vertical = 9.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp)) {
                Text(
                    text = label,
                    color = colors.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = value,
                    color = colors.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                )
            }
            CanvasSlider(fraction = fraction, onFraction = onFraction, modifier = Modifier.fillMaxWidth())
        }
        HorizontalDivider(color = colors.divider)
    }
}

/**
 * The design's slider: a 5dp track filled with accent up to an 18dp white knob.
 * Touched anywhere along its height, so the knob is easy to catch on a tablet.
 */
@Composable
internal fun CanvasSlider(fraction: Float, onFraction: (Float) -> Unit, modifier: Modifier = Modifier) {
    val colors = canvasColors
    val clamped = fraction.coerceIn(0f, 1f)
    // The gestures outlive a recomposition, so they read the latest callback rather
    // than the one they started with — which would put back whatever the options
    // were when the slider first appeared.
    val latest by rememberUpdatedState(onFraction)
    BoxWithConstraints(
        modifier = modifier
            .height(18.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f)
                setProgress { latest(it.coerceIn(0f, 1f)); true }
            }
            .pointerInput(Unit) {
                detectTapGestures { latest((it.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { latest((it.x / size.width).coerceIn(0f, 1f)) },
                ) { change, _ ->
                    change.consume()
                    latest((change.position.x / size.width).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val knob: Dp = 18.dp
        Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(colors.track))
        Box(Modifier.width(maxWidth * clamped).height(5.dp).clip(RoundedCornerShape(3.dp)).background(colors.accent))
        Box(
            modifier = Modifier
                .offset(x = maxWidth * clamped - knob / 2)
                .size(knob)
                .shadow(3.dp, CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}

/**
 * A segmented control in the canvas's colours. Not the app's
 * [com.church.presenter.churchpresentermobile.ui.SegmentedControl], whose track
 * and pill are the app's surfaces and disappear on a canvas panel.
 *
 * @param labels One per segment; [content] may draw something richer in its place.
 * @param selected Which segments are on. Usually one, but the text style row is
 *   four independent switches (bold, italic…) drawn as one control.
 */
@Composable
internal fun CanvasSegmented(
    labels: List<String>,
    selected: (Int) -> Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: (@Composable (index: Int, active: Boolean) -> Unit)? = null,
) {
    val colors = canvasColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.control)
            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val active = selected(index)
            val pill = RoundedCornerShape(7.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .then(if (active && !colors.isDark) Modifier.shadow(1.dp, pill) else Modifier)
                    .clip(pill)
                    .background(if (active) colors.segmentActive else Color.Transparent)
                    .semantics {
                        this.selected = active
                        contentDescription = label
                    }
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                if (content != null) {
                    content(index, active)
                } else {
                    Text(
                        text = label,
                        color = if (active) colors.text else colors.muted,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

/**
 * A small rounded button such as "Align" or "Crop"; [active] gives it the accent's
 * tint, and a chip with nothing to act on is faded and not pressable.
 */
@Composable
internal fun CanvasChip(
    text: String?,
    onClick: () -> Unit,
    active: Boolean = false,
    icon: ImageVector? = null,
    iconDescription: String? = null,
    enabled: Boolean = true,
) {
    val colors = canvasColors
    val shape = RoundedCornerShape(9.dp)
    val tint = if (active) colors.accent else colors.secondary
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (active) colors.accentTint else colors.control)
            .border(1.dp, if (active) colors.accentBorder else colors.border, shape)
            .semantics { this.selected = active }
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = iconDescription, tint = tint, modifier = Modifier.size(16.dp))
        if (text != null) Text(text, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * A square-ish choice tile — a pen type, an eraser mode, a shape — with an icon and
 * an optional caption under it. The chosen one wears the accent.
 */
@Composable
internal fun OptionTile(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    iconModifier: Modifier = Modifier,
) {
    val colors = canvasColors
    val shape = RoundedCornerShape(if (showLabel) 11.dp else 10.dp)
    val tint = if (active) colors.accent else if (showLabel) colors.muted else colors.secondary
    Column(
        modifier = modifier
            .height(if (showLabel) 62.dp else 46.dp)
            .clip(shape)
            .background(if (active) colors.accentTint else colors.control)
            .border(1.dp, if (active) colors.accentBorder else colors.border, shape)
            .semantics { this.selected = active }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = if (showLabel) null else label,
            tint = if (active) colors.accent else colors.secondary,
            modifier = iconModifier.size(18.dp),
        )
        if (showLabel) Text(label, color = tint, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
    }
}
