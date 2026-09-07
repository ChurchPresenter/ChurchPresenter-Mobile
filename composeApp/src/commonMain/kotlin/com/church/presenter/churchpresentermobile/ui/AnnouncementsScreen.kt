package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.announce_anim_fade
import churchpresentermobile.composeapp.generated.resources.announce_anim_none
import churchpresentermobile.composeapp.generated.resources.announce_anim_slide_down
import churchpresentermobile.composeapp.generated.resources.announce_anim_slide_left
import churchpresentermobile.composeapp.generated.resources.announce_anim_slide_right
import churchpresentermobile.composeapp.generated.resources.announce_anim_slide_up
import churchpresentermobile.composeapp.generated.resources.announce_type_clock
import churchpresentermobile.composeapp.generated.resources.announce_type_count_up
import churchpresentermobile.composeapp.generated.resources.announce_type_countdown
import churchpresentermobile.composeapp.generated.resources.announce_type_countdown_to_time
import churchpresentermobile.composeapp.generated.resources.announce_type_text
import churchpresentermobile.composeapp.generated.resources.action_cancel
import churchpresentermobile.composeapp.generated.resources.action_clear
import churchpresentermobile.composeapp.generated.resources.action_go_live
import churchpresentermobile.composeapp.generated.resources.announce_duration_seconds
import churchpresentermobile.composeapp.generated.resources.announcements_add_new
import churchpresentermobile.composeapp.generated.resources.announcements_animation_duration_label
import churchpresentermobile.composeapp.generated.resources.announcements_clock_desc
import churchpresentermobile.composeapp.generated.resources.announcements_count_up_desc
import churchpresentermobile.composeapp.generated.resources.announcements_no_saved
import churchpresentermobile.composeapp.generated.resources.announcements_preview_placeholder
import churchpresentermobile.composeapp.generated.resources.announcements_saved_label
import churchpresentermobile.composeapp.generated.resources.announcements_text_placeholder
import churchpresentermobile.composeapp.generated.resources.cd_custom_color
import churchpresentermobile.composeapp.generated.resources.cd_delete
import churchpresentermobile.composeapp.generated.resources.color_picker_title
import churchpresentermobile.composeapp.generated.resources.color_picker_use
import churchpresentermobile.composeapp.generated.resources.color_slider_brightness
import churchpresentermobile.composeapp.generated.resources.color_slider_hue
import churchpresentermobile.composeapp.generated.resources.color_slider_saturation
import churchpresentermobile.composeapp.generated.resources.label_add_to_schedule
import churchpresentermobile.composeapp.generated.resources.overline_animation
import churchpresentermobile.composeapp.generated.resources.overline_duration
import churchpresentermobile.composeapp.generated.resources.overline_on_screen_preview
import churchpresentermobile.composeapp.generated.resources.overline_target_time
import churchpresentermobile.composeapp.generated.resources.stepper_font_size
import churchpresentermobile.composeapp.generated.resources.stepper_hour
import churchpresentermobile.composeapp.generated.resources.stepper_hrs
import churchpresentermobile.composeapp.generated.resources.stepper_min
import churchpresentermobile.composeapp.generated.resources.stepper_sec
import churchpresentermobile.composeapp.generated.resources.swatch_background
import churchpresentermobile.composeapp.generated.resources.swatch_text
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import com.church.presenter.churchpresentermobile.model.AnnouncementAnimation
import com.church.presenter.churchpresentermobile.model.AnnouncementType
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.AnnouncementForm
import com.church.presenter.churchpresentermobile.viewmodel.AnnouncementsViewModel

/** Widened for the UI tests, which press a swatch by the colour it carries. */
internal val TEXT_SWATCHES = listOf("#FFFFFF", "#F5C518", "#22C55E")
internal val BG_SWATCHES = listOf("#000000", "#1E3A8A", "#7F1D1D")

private fun parseHex(hex: String): Color = try {
    Color(("FF" + hex.removePrefix("#")).toLong(16))
} catch (_: Exception) { Color.White }

private fun p2(n: Int) = n.toString().padStart(2, '0')

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnnouncementsScreen(
    viewModel: AnnouncementsViewModel,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val form by viewModel.form.collectAsState()
    val message by viewModel.message.collectAsState()
    val saved by viewModel.saved.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pickerTarget by remember { mutableStateOf<String?>(null) } // "text" | "background" | null

    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message!!, duration = SnackbarDuration.Short)
            viewModel.clearMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // ── Type (extra — not in the base design) ─────────────────────
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AnnouncementType.entries.forEach { t ->
                    Chip(
                        label = announcementTypeLabel(t),
                        selected = form.type == t,
                        modifier = Modifier.testTag(UiTags.announceType(t.name)),
                    ) { viewModel.update { it.copy(type = t) } }
                }
            }

            Spacer(Modifier.height(18.dp))
            // ── On-screen preview ─────────────────────────────────────────
            Overline(stringResource(Res.string.overline_on_screen_preview))
            PreviewCard(form, Modifier.testTag(UiTags.ANNOUNCE_PREVIEW))

            Spacer(Modifier.height(14.dp))
            // ── Text / timer inputs ───────────────────────────────────────
            when (form.type) {
                AnnouncementType.TEXT -> TextArea(
                    modifier = Modifier.testTag(UiTags.ANNOUNCE_TEXT),
                    value = form.text,
                    onValueChange = { v -> viewModel.update { it.copy(text = v) } },
                    placeholder = stringResource(Res.string.announcements_text_placeholder),
                )
                AnnouncementType.COUNTDOWN -> {
                    Overline(stringResource(Res.string.overline_duration))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.testTag(UiTags.ANNOUNCE_COUNTDOWN_FIELDS),
                    ) {
                        Stepper(
                            stringResource(Res.string.stepper_hrs),
                            form.hours,
                            0,
                            23,
                            Modifier.weight(1f),
                            tag = UiTags.ANNOUNCE_HOURS,
                        ) { v -> viewModel.update { it.copy(hours = v) } }
                        Stepper(
                            stringResource(Res.string.stepper_min),
                            form.minutes,
                            0,
                            59,
                            Modifier.weight(1f),
                            tag = UiTags.ANNOUNCE_MINUTES,
                        ) { v -> viewModel.update { it.copy(minutes = v) } }
                        Stepper(
                            stringResource(Res.string.stepper_sec),
                            form.seconds,
                            0,
                            59,
                            Modifier.weight(1f),
                            tag = UiTags.ANNOUNCE_SECONDS,
                        ) { v -> viewModel.update { it.copy(seconds = v) } }
                    }
                }
                AnnouncementType.COUNTDOWN_TO_TIME -> {
                    Overline(stringResource(Res.string.overline_target_time))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.testTag(UiTags.ANNOUNCE_UNTIL_FIELDS),
                    ) {
                        Stepper(
                            stringResource(Res.string.stepper_hour),
                            form.targetHour,
                            0,
                            23,
                            Modifier.weight(1f),
                            tag = UiTags.ANNOUNCE_TARGET_HOUR,
                        ) { v -> viewModel.update { it.copy(targetHour = v) } }
                        Stepper(
                            stringResource(Res.string.stepper_min),
                            form.targetMinute,
                            0,
                            59,
                            Modifier.weight(1f),
                            tag = UiTags.ANNOUNCE_TARGET_MINUTE,
                        ) { v -> viewModel.update { it.copy(targetMinute = v) } }
                    }
                }
                AnnouncementType.CLOCK, AnnouncementType.COUNT_UP -> Text(
                    if (form.type == AnnouncementType.CLOCK) stringResource(Res.string.announcements_clock_desc)
                    else stringResource(Res.string.announcements_count_up_desc),
                    color = colors.muted, fontSize = 13.sp,
                    modifier = Modifier.testTag(UiTags.ANNOUNCE_TIMER_DESC),
                )
            }

            Spacer(Modifier.height(16.dp))
            // ── Colors: TEXT + BACKGROUND (matches design) ────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                SwatchGroup(stringResource(Res.string.swatch_text), TEXT_SWATCHES, form.textColor,
                    onSelect = { hex -> viewModel.update { it.copy(textColor = hex) } },
                    onOpenPicker = { pickerTarget = "text" }, group = "text")
                SwatchGroup(stringResource(Res.string.swatch_background), BG_SWATCHES, form.backgroundColor,
                    onSelect = { hex -> viewModel.update { it.copy(backgroundColor = hex) } },
                    onOpenPicker = { pickerTarget = "background" }, group = "background")
            }

            Spacer(Modifier.height(18.dp))
            // ── Style extras (font size / animation) ──────────────────────
            Stepper(
                stringResource(Res.string.stepper_font_size),
                form.fontSize,
                16,
                160,
                step = 4,
                tag = UiTags.ANNOUNCE_FONT_SIZE,
            ) { v -> viewModel.update { it.copy(fontSize = v) } }
            Spacer(Modifier.height(14.dp))
            Overline(stringResource(Res.string.overline_animation))
            AnimationDropdown(
                form.animation,
                Modifier.testTag(UiTags.ANNOUNCE_ANIMATION),
            ) { a -> viewModel.update { it.copy(animation = a) } }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.announcements_animation_duration_label), color = colors.muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.05.em, modifier = Modifier.weight(1f))
                // Show the duration in seconds with one decimal (0.5 s steps), e.g. "5.0".
                val halfSecs = form.animationDuration / 500
                val secondsLabel = "${halfSecs / 2}.${if (halfSecs % 2 == 0) "0" else "5"}"
                Text(stringResource(Res.string.announce_duration_seconds, secondsLabel), color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Slider(
                modifier = Modifier.testTag(UiTags.ANNOUNCE_DURATION),
                value = (form.animationDuration / 1000f).coerceIn(0f, 30f),
                onValueChange = { v -> viewModel.update { it.copy(animationDuration = (v * 2).roundToInt() * 500) } },
                valueRange = 0f..30f,
                steps = 59, // 0–30 s in 0.5 s increments
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.inputBg,
                ),
            )

            Spacer(Modifier.height(20.dp))
            // ── Actions (Clear + Show on screen — matches design) ─────────
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(13.dp))
                        .testTag(UiTags.ANNOUNCE_CLEAR)
                        .clickable { viewModel.clearScreen() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(Res.string.action_clear), color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                Row(
                    modifier = Modifier
                        .weight(1.6f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(colors.accent)
                        .testTag(UiTags.ANNOUNCE_GO_LIVE)
                        .clickable { viewModel.showOnScreen() },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(Res.string.action_go_live), color = colors.onAccent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(12.dp))
            // Secondary: add to schedule (queue without going live)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(colors.amber.copy(alpha = 0.16f))
                    .testTag(UiTags.ANNOUNCE_ADD_TO_SCHEDULE)
                    .clickable { viewModel.addToSchedule() },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, tint = colors.amber, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.label_add_to_schedule), color = colors.amber, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(24.dp))
            // ── Saved (matches design) ────────────────────────────────────
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.announcements_saved_label), color = colors.muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.05.em, modifier = Modifier.weight(1f))
                Text(
                    stringResource(Res.string.announcements_add_new),
                    color = colors.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag(UiTags.ANNOUNCE_SAVE).clickable { viewModel.saveCurrent() },
                )
            }
            Spacer(Modifier.height(10.dp))
            if (saved.isEmpty()) {
                Text(
                    stringResource(Res.string.announcements_no_saved),
                    color = colors.muted,
                    fontSize = 13.sp,
                    modifier = Modifier.testTag(UiTags.ANNOUNCE_NO_SAVED),
                )
            } else {
                saved.forEach { item ->
                    SavedRow(
                        label = item.label,
                        onClick = { viewModel.loadSaved(item.id) },
                        onDelete = { viewModel.deleteSaved(item.id) },
                        modifier = Modifier.testTag(UiTags.savedAnnouncement(item.id)),
                        deleteModifier = Modifier.testTag(UiTags.savedAnnouncementDelete(item.id)),
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (pickerTarget != null) {
        val initial = if (pickerTarget == "text") form.textColor else form.backgroundColor
        ColorPickerDialog(
            initialHex = initial,
            onPick = { hex ->
                if (pickerTarget == "text") viewModel.update { it.copy(textColor = hex) }
                else viewModel.update { it.copy(backgroundColor = hex) }
                pickerTarget = null
            },
            onDismiss = { pickerTarget = null },
        )
    }
}

@Composable
private fun Overline(label: String) {
    val colors = LocalAppColors.current
    Text(label.uppercase(), color = colors.muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.05.em)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun Chip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) colors.accentTint else colors.surface)
            .border(1.dp, if (selected) colors.accent else colors.borderSubtle, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, color = if (selected) colors.accent else colors.secondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SwatchGroup(
    label: String,
    swatches: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onOpenPicker: () -> Unit,
    group: String,
) {
    val colors = LocalAppColors.current
    val isCustom = swatches.none { it.equals(selected, ignoreCase = true) }
    Column {
        Overline(label)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            swatches.forEach { hex ->
                val isSel = selected.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(parseHex(hex))
                        .border(
                            width = if (isSel) 2.dp else 1.dp,
                            color = if (isSel) colors.accent else colors.border,
                            shape = RoundedCornerShape(10.dp),
                        )
                        .testTag(UiTags.announceSwatch(group, hex))
                        .clickable { onSelect(hex) },
                )
            }
            // Custom color picker — shows the current custom colour when active
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isCustom) parseHex(selected) else colors.inputBg)
                    .border(
                        width = if (isCustom) 2.dp else 1.dp,
                        color = if (isCustom) colors.accent else colors.border,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .testTag(UiTags.announceCustomSwatch(group))
                    .clickable(onClick = onOpenPicker),
                contentAlignment = Alignment.Center,
            ) {
                if (!isCustom) Icon(Icons.Outlined.Palette, contentDescription = stringResource(Res.string.cd_custom_color), tint = colors.muted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ColorPickerDialog(initialHex: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalAppColors.current
    val initHsv = remember(initialHex) { rgbToHsv(parseHex(initialHex)) }
    var hue by remember { mutableStateOf(initHsv[0]) }
    var sat by remember { mutableStateOf(initHsv[1]) }
    var value by remember { mutableStateOf(initHsv[2]) }
    val current = Color.hsv(hue, sat, value)
    val hex = "#" + listOf(current.red, current.green, current.blue).joinToString("") {
        (it * 255).toInt().coerceIn(0, 255).toString(16).padStart(2, '0').uppercase()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(colors.sheetBackground)
                .padding(20.dp),
        ) {
            Text(
                stringResource(Res.string.color_picker_title),
                color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag(UiTags.COLOR_PICKER),
            )
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(12.dp)).background(current)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    hex,
                    color = if (value > 0.6f && sat < 0.5f) Color.Black else Color.White,
                    fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.testTag(UiTags.COLOR_PICKER_HEX),
                )
            }

            ColorSlider(
                stringResource(Res.string.color_slider_hue),
                hue / 360f,
                colors.accent,
                UiTags.COLOR_PICKER_HUE,
            ) { hue = it * 360f }
            ColorSlider(
                stringResource(Res.string.color_slider_saturation),
                sat,
                colors.accent,
                UiTags.COLOR_PICKER_SATURATION,
            ) { sat = it }
            ColorSlider(
                stringResource(Res.string.color_slider_brightness),
                value,
                colors.accent,
                UiTags.COLOR_PICKER_BRIGHTNESS,
            ) { value = it }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(12.dp)).background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        .testTag(UiTags.COLOR_PICKER_CANCEL)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(Res.string.action_cancel), color = colors.text, fontSize = 14.sp) }
                Box(
                    Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(12.dp)).background(colors.accent)
                        .testTag(UiTags.COLOR_PICKER_USE)
                        .clickable { onPick(hex) },
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(Res.string.color_picker_use), color = colors.onAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun ColorSlider(label: String, value: Float, accent: Color, tag: String, onChange: (Float) -> Unit) {
    val colors = LocalAppColors.current
    Spacer(Modifier.height(6.dp))
    Text(label.uppercase(), color = colors.muted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.05.em)
    Slider(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.testTag(tag),
        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = colors.inputBg),
    )
}

/** Converts a [Color] to [hue(0..360), saturation(0..1), value(0..1)]. */
private fun rgbToHsv(c: Color): FloatArray {
    val r = c.red; val g = c.green; val b = c.blue
    val max = maxOf(r, g, b); val min = minOf(r, g, b); val d = max - min
    var h = when {
        d == 0f -> 0f
        max == r -> 60f * (((g - b) / d) % 6f)
        max == g -> 60f * (((b - r) / d) + 2f)
        else -> 60f * (((r - g) / d) + 4f)
    }
    if (h < 0f) h += 360f
    val s = if (max == 0f) 0f else d / max
    return floatArrayOf(h, s, max)
}

@Composable
private fun SavedRow(
    label: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    deleteModifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(colors.accentTint),
            contentAlignment = Alignment.Center,
        ) {
            Text("Aa", color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Text(label, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1)
        Box(
            modifier = deleteModifier.size(28.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.cd_delete), tint = colors.muted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun PreviewCard(form: AnnouncementForm, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val previewPlaceholder = stringResource(Res.string.announcements_preview_placeholder)
    val previewText = when (form.type) {
        AnnouncementType.TEXT -> form.text.ifBlank { previewPlaceholder }
        AnnouncementType.COUNTDOWN -> "${p2(form.hours)}:${p2(form.minutes)}:${p2(form.seconds)}"
        AnnouncementType.COUNT_UP -> "00:00"
        AnnouncementType.CLOCK -> "12:00:00"
        AnnouncementType.COUNTDOWN_TO_TIME -> "→ ${p2(form.targetHour)}:${p2(form.targetMinute)}"
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .background(parseHex(form.backgroundColor))
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            previewText,
            color = parseHex(form.textColor),
            fontSize = (form.fontSize * 0.32f).coerceIn(14f, 44f).sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TextArea(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 90.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.inputBg)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) Text(placeholder, color = colors.muted, fontSize = 15.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = colors.text, fontSize = 15.sp),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Stepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    modifier: Modifier = Modifier,
    step: Int = 1,
    /** Names this stepper's two buttons and its value for a UI test. */
    tag: String? = null,
    onChange: (Int) -> Unit,
) {
    val colors = LocalAppColors.current
    Column(modifier.then(tag?.let { Modifier.testTag(it) } ?: Modifier)) {
        Overline(label)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.inputBg)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            StepBtn("−", tag?.let { UiTags.stepperDown(it) }) { onChange((value - step).coerceAtLeast(min)) }
            Text(
                value.toString(),
                color = colors.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .then(tag?.let { Modifier.testTag(UiTags.stepperValue(it)) } ?: Modifier),
            )
            StepBtn("+", tag?.let { UiTags.stepperUp(it) }) { onChange((value + step).coerceAtMost(max)) }
        }
    }
}

@Composable
private fun StepBtn(sym: String, tag: String? = null, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .then(tag?.let { Modifier.testTag(it) } ?: Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(sym, color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun AnimationDropdown(
    selected: AnnouncementAnimation,
    modifier: Modifier = Modifier,
    onSelect: (AnnouncementAnimation) -> Unit,
) {
    val colors = LocalAppColors.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.inputBg)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(announcementAnimationLabel(selected), color = colors.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = colors.muted, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AnnouncementAnimation.entries.forEach { a ->
                DropdownMenuItem(
                    text = { Text(announcementAnimationLabel(a)) },
                    onClick = { onSelect(a); expanded = false },
                    modifier = Modifier.testTag(UiTags.announceAnimation(a.name)),
                )
            }
        }
    }
}

@Composable
private fun announcementTypeLabel(type: AnnouncementType): String = stringResource(
    when (type) {
        AnnouncementType.TEXT -> Res.string.announce_type_text
        AnnouncementType.COUNTDOWN -> Res.string.announce_type_countdown
        AnnouncementType.COUNT_UP -> Res.string.announce_type_count_up
        AnnouncementType.CLOCK -> Res.string.announce_type_clock
        AnnouncementType.COUNTDOWN_TO_TIME -> Res.string.announce_type_countdown_to_time
    }
)

@Composable
private fun announcementAnimationLabel(animation: AnnouncementAnimation): String = stringResource(
    when (animation) {
        AnnouncementAnimation.NONE -> Res.string.announce_anim_none
        AnnouncementAnimation.FADE -> Res.string.announce_anim_fade
        AnnouncementAnimation.SLIDE_BOTTOM -> Res.string.announce_anim_slide_up
        AnnouncementAnimation.SLIDE_TOP -> Res.string.announce_anim_slide_down
        AnnouncementAnimation.SLIDE_LEFT -> Res.string.announce_anim_slide_left
        AnnouncementAnimation.SLIDE_RIGHT -> Res.string.announce_anim_slide_right
    }
)
