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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.color_blue
import churchpresentermobile.composeapp.generated.resources.color_green
import churchpresentermobile.composeapp.generated.resources.color_red
import churchpresentermobile.composeapp.generated.resources.qa_admin_cancel
import churchpresentermobile.composeapp.generated.resources.settings_save
import com.church.presenter.churchpresentermobile.ui.theme.AppDimens
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * Picking a colour by dragging rather than by knowing its hex.
 *
 * Red, green and blue as three sliders, each tinted along its own axis so the operator can see
 * where a drag is heading before making it. A wide preview sits above them, and the hex is shown
 * underneath — read-only here, because it is the thing being produced rather than another way to
 * enter one; [ColorField] keeps the typed route for anyone who already knows the value.
 *
 * The colour is only handed back on Save. Dragging a slider must not repaint an audience screen
 * live, which is what writing through on every frame would do.
 */
@Composable
internal fun ColorPickerDialog(
    title: String,
    initial: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    val start = parseHexColorOrNull(initial) ?: Color.Black

    var red by remember(initial) { mutableStateOf((start.red * MAX_CHANNEL).roundToInt()) }
    var green by remember(initial) { mutableStateOf((start.green * MAX_CHANNEL).roundToInt()) }
    var blue by remember(initial) { mutableStateOf((start.blue * MAX_CHANNEL).roundToInt()) }

    val picked = Color(red = red / MAX_CHANNEL, green = green / MAX_CHANNEL, blue = blue / MAX_CHANNEL)
    val hex = rgbToHex(red, green, blue)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.space8)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(AppDimens.radiusCard))
                        .background(picked)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(AppDimens.radiusCard))
                )

                ChannelSlider(
                    label = stringResource(Res.string.color_red),
                    value = red,
                    // Each track runs from the colour with this channel at zero to the same
                    // colour with it full, so the slider previews its own effect.
                    track = listOf(picked.copy(red = 0f), picked.copy(red = 1f)),
                    onValueChange = { red = it },
                )
                ChannelSlider(
                    label = stringResource(Res.string.color_green),
                    value = green,
                    track = listOf(picked.copy(green = 0f), picked.copy(green = 1f)),
                    onValueChange = { green = it },
                )
                ChannelSlider(
                    label = stringResource(Res.string.color_blue),
                    value = blue,
                    track = listOf(picked.copy(blue = 0f), picked.copy(blue = 1f)),
                    onValueChange = { blue = it },
                )

                Text(
                    text = hex,
                    color = colors.muted,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag(COLOR_PICKER_HEX),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onPick(hex); onDismiss() },
                modifier = Modifier.testTag(COLOR_PICKER_SAVE),
            ) {
                Text(stringResource(Res.string.settings_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(COLOR_PICKER_CANCEL),
            ) { Text(stringResource(Res.string.qa_admin_cancel)) }
        },
    )
}

@Composable
private fun ChannelSlider(
    label: String,
    value: Int,
    track: List<Color>,
    onValueChange: (Int) -> Unit,
) {
    val colors = LocalAppColors.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = colors.muted, fontSize = 11.sp)
            Text(
                text = value.toString(),
                color = colors.text,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.horizontalGradient(track))
            )
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.roundToInt()) },
                valueRange = 0f..MAX_CHANNEL,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    // The gradient behind is the track; the slider only contributes its thumb.
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    thumbColor = colors.text,
                ),
            )
        }
    }
}

/** A swatch that opens the picker — the whole point being that it is tappable. */
@Composable
internal fun ColorSwatch(color: Color?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Box(
        modifier
            .size(26.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color ?: Color.Transparent)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
    )
}

internal fun rgbToHex(red: Int, green: Int, blue: Int): String =
    "#" + listOf(red, green, blue).joinToString("") { channel ->
        channel.coerceIn(0, MAX_CHANNEL.toInt()).toString(16).uppercase().padStart(2, '0')
    }

private const val MAX_CHANNEL = 255f

// ── Names a UI test reaches the picker by ────────────────────────────────
//
// The labels come from compose-resources, which renders empty in the wasmJs
// test runtime, so the three buttons cannot be told apart by their words.

internal const val COLOR_PICKER_SAVE = "standaloneColor:save"
internal const val COLOR_PICKER_CANCEL = "standaloneColor:cancel"
internal const val COLOR_PICKER_HEX = "standaloneColor:hex"
