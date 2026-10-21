package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.display_size_decrease
import churchpresentermobile.composeapp.generated.resources.display_size_increase
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/** Minus, the value, plus. */
@Composable
internal fun DisplayStepper(
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    tag: String,
    canDecrease: Boolean = true,
    canIncrease: Boolean = true,
) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StepButton(
            Icons.Filled.Remove,
            stringResource(Res.string.display_size_decrease),
            canDecrease,
            onDecrease,
            "$tag:down",
        )
        Text(
            text = value,
            color = colors.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.widthIn(min = 44.dp).testTag("$tag:value"),
            textAlign = TextAlign.Center,
        )
        StepButton(
            Icons.Filled.Add,
            stringResource(Res.string.display_size_increase),
            canIncrease,
            onIncrease,
            "$tag:up",
        )
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit, tag: String) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .testTag(tag)
            .size(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(colors.inputBg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) colors.text else colors.dim,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * A row of equal tiles, each an icon or a short label, showing which are on. Bold / italic /
 * underline, where any may be on, and the alignments, where one is.
 */
@Composable
internal fun TileRow(
    count: Int,
    isOn: (Int) -> Boolean,
    onClick: (Int) -> Unit,
    tag: (Int) -> String,
    tile: @Composable (Int, Boolean) -> Unit,
) {
    val colors = LocalAppColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { index ->
            val on = isOn(index)
            val shape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag(tag(index))
                    .semantics { selected = on }
                    .clip(shape)
                    .background(if (on) colors.accentTint else colors.inputBg)
                    .border(1.dp, if (on) colors.accent.copy(alpha = 0.55f) else colors.borderSubtle, shape)
                    .clickable { onClick(index) },
                contentAlignment = Alignment.Center,
            ) { tile(index, on) }
        }
    }
}

/** An icon in a [TileRow], tinted as the tile is on or off. */
@Composable
internal fun TileIcon(icon: ImageVector, description: String, on: Boolean) {
    val colors = LocalAppColors.current
    Icon(
        icon,
        contentDescription = description,
        tint = if (on) colors.accent else colors.secondary,
        modifier = Modifier.size(20.dp),
    )
}

/** A switch, styled the app's way. */
@Composable
internal fun DisplaySwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, tag: String) {
    val colors = LocalAppColors.current
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.testTag(tag),
        colors = SwitchDefaults.colors(
            checkedTrackColor = colors.accent,
            checkedThumbColor = Color.White,
            checkedBorderColor = colors.accent,
            uncheckedTrackColor = colors.inputBg,
            uncheckedThumbColor = Color.White,
            uncheckedBorderColor = colors.border,
        ),
    )
}

/** A label, its value in the accent colour, and a slider under both. */
@Composable
internal fun SliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    tag: String,
) {
    val colors = LocalAppColors.current
    Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                valueText,
                color = colors.accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.testTag("$tag:value"),
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth().height(32.dp).testTag(tag),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.inputBg,
            ),
        )
    }
}
