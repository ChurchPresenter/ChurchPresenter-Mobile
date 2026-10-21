package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.display_picker_alpha
import churchpresentermobile.composeapp.generated.resources.display_picker_blue
import churchpresentermobile.composeapp.generated.resources.display_picker_custom
import churchpresentermobile.composeapp.generated.resources.display_picker_done
import churchpresentermobile.composeapp.generated.resources.display_picker_green
import churchpresentermobile.composeapp.generated.resources.display_picker_hex
import churchpresentermobile.composeapp.generated.resources.display_picker_presets
import churchpresentermobile.composeapp.generated.resources.display_picker_recent
import churchpresentermobile.composeapp.generated.resources.display_picker_red
import churchpresentermobile.composeapp.generated.resources.display_picker_swatches
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * The colour picker as a sheet over the phone's controls.
 *
 * Every change writes straight to the draft, so the pinned preview above the sheet shows the
 * colour while it is being chosen; Done only closes it and remembers the colour.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ColorPickerSheet(
    request: PickerRequest,
    recent: List<String>,
    onChange: (String) -> Unit,
    onDone: (String) -> Unit,
) {
    var current by remember(request) { mutableStateOf(request.initial) }
    ModalBottomSheet(
        onDismissRequest = { onDone(current) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = LocalAppColors.current.sheetBackground,
    ) {
        ColorPickerContent(
            request = request,
            recent = recent,
            onChange = {
                current = it
                onChange(it)
            },
            onDone = { onDone(current) },
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 18.dp).padding(bottom = 16.dp),
        )
    }
}

/** The same picker as a floating panel, beside the tablet's controls rather than over them. */
@Composable
internal fun ColorPickerPopover(
    request: PickerRequest,
    recent: List<String>,
    onChange: (String) -> Unit,
    onDone: (String) -> Unit,
) {
    var current by remember(request) { mutableStateOf(request.initial) }
    val colors = LocalAppColors.current
    Dialog(onDismissRequest = { onDone(current) }) {
        ColorPickerContent(
            request = request,
            recent = recent,
            onChange = {
                current = it
                onChange(it)
            },
            onDone = { onDone(current) },
            modifier = Modifier
                .width(POPOVER_WIDTH)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.sheetBackground)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(20.dp))
                .padding(18.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColorPickerContent(
    request: PickerRequest,
    recent: List<String>,
    onChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val start = remember(request) { Hsva.of(parseHexColor(request.initial, Color.White)) }
    var hue by remember(request) { mutableFloatStateOf(start.hue) }
    var saturation by remember(request) { mutableFloatStateOf(start.saturation) }
    var value by remember(request) { mutableFloatStateOf(start.value) }
    var alpha by remember(request) { mutableFloatStateOf(start.alpha) }
    var custom by remember { mutableStateOf(true) }

    val color = Color.hsv(hue, saturation, value, alpha)
    fun emit() = onChange(colorToHex(Color.hsv(hue, saturation, value, alpha)))
    fun adopt(hex: String) {
        val picked = Hsva.of(parseHexColor(hex, Color.White))
        // A grey has no hue of its own; keep the slider where it was rather than jumping to red.
        if (picked.saturation > 0f) hue = picked.hue
        saturation = picked.saturation
        value = picked.value
        alpha = picked.alpha
        onChange(hex.uppercase())
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).testTag(DisplayTextTags.PICKER),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PICKER_CHECKER)
                    .background(color)
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(12.dp)),
            )
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(request.title, color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(request.subtitle, color = colors.muted, fontSize = 12.sp)
            }
            Text(
                stringResource(Res.string.display_picker_done),
                color = colors.accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag(DisplayTextTags.PICKER_DONE).clickable(onClick = onDone).padding(6.dp),
            )
        }
        DisplaySegments(
            options = listOf(
                stringResource(Res.string.display_picker_swatches),
                stringResource(Res.string.display_picker_custom),
            ),
            selectedIndex = if (custom) 1 else 0,
            onSelect = { custom = it == 1 },
            tag = { DisplayTextTags.pickerMode(it) },
            compact = true,
        )
        if (custom) {
            SaturationSquare(hue, saturation, value) { s, v ->
                saturation = s
                value = v
                emit()
            }
            HueBar(hue) {
                hue = it
                emit()
            }
            AlphaBar(color.copy(alpha = 1f), alpha) {
                alpha = it
                emit()
            }
            ChannelFields(color, onHex = ::adopt)
        } else {
            SwatchGrid(PRESET_COLORS, colorToHex(color), ::adopt, size = 40.dp)
        }
        if (recent.isNotEmpty()) {
            DisplayOverline(stringResource(Res.string.display_picker_recent))
            SwatchGrid(recent, colorToHex(color), ::adopt, size = 36.dp)
        }
        if (custom) {
            DisplayOverline(stringResource(Res.string.display_picker_presets))
            SwatchGrid(PRESET_COLORS, colorToHex(color), ::adopt, size = 30.dp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SwatchGrid(hexes: List<String>, selected: String, onPick: (String) -> Unit, size: Dp) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        hexes.forEach { hex ->
            ColorChip(
                hex = hex,
                selected = hex.equals(selected, ignoreCase = true),
                onClick = { onPick(hex) },
                tag = DisplayTextTags.pickerSwatch(hex),
                size = size,
            )
        }
    }
}

/** HEX to type into, and the four channels it means. */
@Composable
private fun ChannelFields(color: Color, onHex: (String) -> Unit) {
    val colors = LocalAppColors.current
    val hex = colorToHex(color)
    var typed by remember(hex) { mutableStateOf(hex) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .weight(HEX_FIELD_WEIGHT)
                .height(FIELD_HEIGHT)
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, colors.accent, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(Res.string.display_picker_hex), color = colors.muted, fontSize = 12.sp)
            BasicTextField(
                value = typed,
                onValueChange = { input ->
                    typed = input.uppercase().take(MAX_HEX_LENGTH)
                    if (parseHexColorOrNull(typed) != null) onHex(typed.withHash())
                },
                singleLine = true,
                textStyle = TextStyle(
                    color = colors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                ),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.padding(start = 8.dp).testTag(DisplayTextTags.PICKER_HEX),
            )
        }
        Channel(
            stringResource(Res.string.display_picker_red),
            (color.red * PICKER_MAX_CHANNEL).roundToInt().toString(),
            Modifier.weight(1f),
        )
        Channel(
            stringResource(Res.string.display_picker_green),
            (color.green * PICKER_MAX_CHANNEL).roundToInt().toString(),
            Modifier.weight(1f),
        )
        Channel(
            stringResource(Res.string.display_picker_blue),
            (color.blue * PICKER_MAX_CHANNEL).roundToInt().toString(),
            Modifier.weight(1f),
        )
        Channel(
            stringResource(Res.string.display_picker_alpha),
            "${(color.alpha * PERCENT).roundToInt()}%",
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun Channel(label: String, value: String, modifier: Modifier) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .height(FIELD_HEIGHT)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, color = colors.muted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        Text(
            value,
            color = colors.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

private fun String.withHash(): String = if (startsWith("#")) this else "#$this"

internal const val PICKER_MAX_CHANNEL = 255f
private const val PERCENT = 100f

private const val MAX_HEX_LENGTH = 9

/** The hex field is wider than each channel: it holds eight characters to their three. */
private const val HEX_FIELD_WEIGHT = 2.5f

private val FIELD_HEIGHT = 52.dp
private val POPOVER_WIDTH = 380.dp
internal val PICKER_CHECKER = Color(0xFF8A8A8A)

/** The design's preset row: whites, warm and cool tints, greys and black. */
internal val PRESET_COLORS = listOf(
    "#FFFFFF", "#F5F5F4", "#FDE68A", "#FACC15", "#FDBA74", "#F9A8D4",
    "#C4B5FD", "#BAE6FD", "#86EFAC", "#94A3B8", "#475569", "#000000",
)
