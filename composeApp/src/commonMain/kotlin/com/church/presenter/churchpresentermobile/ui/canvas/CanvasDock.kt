package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.canvas_add_page
import churchpresentermobile.composeapp.generated.resources.canvas_opacity
import churchpresentermobile.composeapp.generated.resources.canvas_page_number
import churchpresentermobile.composeapp.generated.resources.canvas_percent
import churchpresentermobile.composeapp.generated.resources.canvas_width_medium
import churchpresentermobile.composeapp.generated.resources.canvas_width_thick
import churchpresentermobile.composeapp.generated.resources.canvas_width_thin
import com.church.presenter.churchpresentermobile.model.CanvasTool
import com.church.presenter.churchpresentermobile.model.HighlightPalette
import com.church.presenter.churchpresentermobile.model.HighlighterOptions
import com.church.presenter.churchpresentermobile.model.inkPalette
import com.church.presenter.churchpresentermobile.model.mixedInto
import com.church.presenter.churchpresentermobile.model.PenOptions
import com.church.presenter.churchpresentermobile.model.paletteSlots
import com.church.presenter.churchpresentermobile.viewmodel.CanvasChrome
import com.church.presenter.churchpresentermobile.viewmodel.CanvasIntent
import com.church.presenter.churchpresentermobile.viewmodel.CanvasUiState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * The quick settings under the board while a pen is out: color, three widths and
 * opacity. The pen's options panel edits the same values in more detail.
 *
 * Shown for the pen and the highlighter; every other tool keeps its settings in
 * its options panel only.
 */
@Composable
internal fun PenDock(state: CanvasUiState, onIntent: (CanvasIntent) -> Unit, modifier: Modifier = Modifier) {
    val colors = canvasColors
    val highlighter = state.tool == CanvasTool.HIGHLIGHTER
    val options = state.options
    val width = if (highlighter) options.highlighter.width else options.pen.width
    val opacity = if (highlighter) options.highlighter.opacity else options.pen.opacity
    val presets = if (highlighter) HighlighterWidths else PenWidths
    val setWidth: (Float) -> Unit = { w ->
        onIntent(
            CanvasIntent.SetOptions(
                if (highlighter) {
                    options.copy(highlighter = options.highlighter.copy(width = w))
                } else {
                    options.copy(pen = options.pen.copy(width = w))
                },
            ),
        )
    }
    val setOpacity: (Float) -> Unit = { o ->
        onIntent(
            CanvasIntent.SetOptions(
                if (highlighter) {
                    options.copy(highlighter = options.highlighter.copy(opacity = o))
                } else {
                    options.copy(pen = options.pen.copy(opacity = o))
                },
            ),
        )
    }
    Row(
        modifier = modifier
            .testTag(CanvasTags.DOCK)
            .floatingPanel(colors, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        DockSwatches(state, onIntent)
        DockDivider()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEachIndexed { index, preset ->
                WidthPreset(
                    dot = PresetDots[index],
                    label = PresetLabels[index],
                    active = width == preset,
                    onClick = { setWidth(preset) },
                )
            }
        }
        DockDivider()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(Res.string.canvas_opacity),
                color = colors.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            CanvasSlider(fraction = opacity, onFraction = setOpacity, modifier = Modifier.width(110.dp))
            Text(
                text = stringResource(Res.string.canvas_percent, (opacity * PERCENT).roundToInt()),
                color = colors.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.widthIn(min = 34.dp),
            )
        }
        DockDivider()
        ChromeToggle(
            CanvasChrome.DOCK,
            minimized = false,
            onToggle = { onIntent(CanvasIntent.Minimize(CanvasChrome.DOCK, true)) },
        )
    }
}

/**
 * The dock's colors: the pen's inks, or the highlighter's colors while it is out —
 * the same row as the tool's panel, mixes first. Scrolls rather than pushing the
 * width and opacity controls off a narrow board.
 */
@Composable
private fun RowScope.DockSwatches(state: CanvasUiState, onIntent: (CanvasIntent) -> Unit) {
    val colors = canvasColors
    val options = state.options
    val highlighter = state.tool == CanvasTool.HIGHLIGHTER
    Row(
        modifier = Modifier.weight(1f, fill = false).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(DOCK_SWATCH_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (highlighter) {
            val marker = options.highlighter
            val set: (HighlighterOptions) -> Unit = {
                onIntent(CanvasIntent.SetOptions(options.copy(highlighter = it)))
            }
            paletteSlots(options.mixedHighlights, HighlightPalette).forEach { slot ->
                PaletteSwatch(
                    slot = slot,
                    custom = marker.custom,
                    isPreset = { it == marker.color },
                    presetColor = ::highlightColor,
                    presetLabel = { stringResource(it.label()) },
                    onPick = { set(marker.copy(color = it, custom = null)) },
                    onReuse = { set(marker.copy(custom = it)) },
                    size = DOCK_SWATCH,
                    gap = colors.board,
                    ringWidth = 3.dp,
                )
            }
            // The design ends the dock's row with the picker wheel, as it ends a panel's.
            CustomColorSwatch(current = null, size = DOCK_SWATCH, startFrom = marker.custom) {
                onIntent(
                    CanvasIntent.SetOptions(
                        options.copy(
                            highlighter = marker.copy(custom = it),
                            mixedHighlights = mixedInto(options.mixedHighlights, it),
                        ),
                    ),
                )
            }
        } else {
            val pen = options.pen
            val set: (PenOptions) -> Unit = { onIntent(CanvasIntent.SetOptions(options.copy(pen = it))) }
            val slots = paletteSlots(options.mixedInks, inkPalette(colors.isDark))
            slots.forEach { slot ->
                PaletteSwatch(
                    slot = slot,
                    custom = pen.custom,
                    isPreset = { it == pen.ink },
                    presetColor = { colors.inkSwatch(it) },
                    presetLabel = { stringResource(it.label(colors.isDark)) },
                    onPick = { set(pen.copy(ink = it, custom = null)) },
                    onReuse = { set(pen.copy(custom = it)) },
                    size = DOCK_SWATCH,
                    gap = colors.board,
                    ringWidth = 3.dp,
                )
            }
            CustomColorSwatch(current = null, size = DOCK_SWATCH, startFrom = pen.custom) {
                onIntent(
                    CanvasIntent.SetOptions(
                        options.copy(pen = pen.copy(custom = it), mixedInks = mixedInto(options.mixedInks, it)),
                    ),
                )
            }
        }
    }
}

/** The dock's swatches as the design draws them: 26dp circles, 7dp apart. */
private val DOCK_SWATCH = 26.dp
private val DOCK_SWATCH_GAP = 7.dp

private const val PERCENT = 100

/** The three widths the dock offers, in dp: fine, the default, and broad. */
private val PenWidths = listOf(2f, 4f, 8f)
private val HighlighterWidths = listOf(12f, 18f, 26f)
private val PresetDots: List<Dp> = listOf(7.dp, 10.dp, 14.dp)
private val PresetLabels: List<StringResource> =
    listOf(Res.string.canvas_width_thin, Res.string.canvas_width_medium, Res.string.canvas_width_thick)

@Composable
private fun WidthPreset(dot: Dp, label: StringResource, active: Boolean, onClick: () -> Unit) {
    val colors = canvasColors
    val shape = RoundedCornerShape(10.dp)
    val description = stringResource(label)
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(shape)
            .background(if (active) colors.accentTint else Color.Transparent)
            .border(1.dp, if (active) colors.accentBorder else Color.Transparent, shape)
            .semantics {
                contentDescription = description
                selected = active
            }
            .clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(dot).background(if (active) colors.accent else colors.secondary, CircleShape))
    }
}

@Composable
private fun DockDivider() {
    Box(Modifier.width(1.dp).height(30.dp).background(canvasColors.divider))
}

/** The page thumbnails at the board's bottom-right, with Add page on the end. */
@Composable
internal fun PageStrip(state: CanvasUiState, onIntent: (CanvasIntent) -> Unit, modifier: Modifier = Modifier) {
    val colors = canvasColors
    Row(
        modifier = modifier
            .floatingPanel(colors, RoundedCornerShape(14.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.document.pages.indices.forEach { index ->
            val current = index == state.pageIndex
            val description = stringResource(Res.string.canvas_page_number, index + 1)
            Box(
                modifier = Modifier
                    .testTag(CanvasTags.page(index))
                    .size(width = 62.dp, height = 40.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(colors.board)
                    .border(2.dp, if (current) colors.accent else colors.border, RoundedCornerShape(7.dp))
                    .semantics {
                        contentDescription = description
                        selected = current
                    }
                    .clickable(role = Role.Tab) { onIntent(CanvasIntent.TurnToPage(index)) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${index + 1}",
                    color = if (current) colors.accent else colors.muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
        Box(
            modifier = Modifier
                .testTag(CanvasTags.ADD_PAGE)
                .size(40.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(colors.control)
                .clickable(role = Role.Button) { onIntent(CanvasIntent.AddPage) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = stringResource(Res.string.canvas_add_page),
                tint = colors.secondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

