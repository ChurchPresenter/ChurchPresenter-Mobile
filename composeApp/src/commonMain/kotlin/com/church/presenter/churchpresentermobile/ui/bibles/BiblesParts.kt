package com.church.presenter.churchpresentermobile.ui.bibles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.church.presenter.churchpresentermobile.bibleimport.BibleXmlFormat
import com.church.presenter.churchpresentermobile.bibleimport.catalog.Testament
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors

internal val MonoFamily: FontFamily = FontFamily.Monospace

/** "USFX", "Zefania XML", "Beblia XML" in their archive's hue. */
@Composable
internal fun FormatBadge(format: BibleXmlFormat, modifier: Modifier = Modifier) {
    val hue = LocalAppColors.current.hueOf(format)
    Text(
        text = format.label,
        color = hue.fg,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(hue.tint)
            .border(1.dp, hue.edge, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

/** The small caps overline the design uses above every group: "ON THIS DEVICE", "1 · SOURCE FILE". */
@Composable
internal fun Overline(text: String, modifier: Modifier = Modifier, color: Color = LocalAppColors.current.muted) {
    Text(
        text = text.uppercase(),
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.09.em,
        modifier = modifier,
    )
}

/**
 * The design's full-width buttons: accent (primary), quiet (neutral card) and danger.
 * 13 radius, bold 14, a leading icon where one is given.
 */
internal enum class ButtonKind { PRIMARY, QUIET, DANGER }

@Composable
internal fun BiblesButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.PRIMARY,
    icon: ImageVector? = null,
    height: Dp = 50.dp,
    enabled: Boolean = true,
    tag: String? = null,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(13.dp)
    val (fill, ink, edge) = when (kind) {
        ButtonKind.PRIMARY -> Triple(colors.accent, colors.onAccentButton, Color.Transparent)
        ButtonKind.QUIET -> Triple(colors.quietFill, colors.text, colors.cardEdge)
        ButtonKind.DANGER -> Triple(
            colors.danger.copy(alpha = if (colors.isDark) 0.12f else 0.06f),
            colors.danger,
            colors.danger.copy(alpha = 0.3f),
        )
    }
    Row(
        modifier = modifier
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .height(height)
            .clip(shape)
            .background(if (enabled) fill else fill.copy(alpha = fill.alpha * DISABLED_ALPHA))
            .border(1.dp, edge, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(16.dp))
        Text(label, color = ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

private const val DISABLED_ALPHA = 0.4f

/** A card: faint fill, hairline, 14 radius — every grouped block in the design. */
@Composable
internal fun BiblesCard(
    modifier: Modifier = Modifier,
    fill: Color = LocalAppColors.current.cardFill,
    edge: Color = LocalAppColors.current.cardEdge,
    radius: Dp = 14.dp,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Box(modifier = modifier.clip(shape).background(fill).border(1.dp, edge, shape)) { content() }
}

/** "Books ....... 66" — the label/value rows of the ready sheet and the convert screen. */
@Composable
internal fun FactRows(rows: List<Pair<String, @Composable RowScope.() -> Unit>>, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    BiblesCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            rows.forEachIndexed { index, (label, value) ->
                if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSubtle))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, color = colors.muted, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) { value() }
                }
            }
        }
    }
}

/** The value half of a [FactRows] row, as plain semibold text. */
@Composable
internal fun FactValue(text: String) {
    Text(
        text = text,
        color = LocalAppColors.current.text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 16.dp),
    )
}

/** A thin rounded progress track: [fraction] of it filled in the accent. */
@Composable
internal fun ProgressTrack(fraction: Float, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(height / 2)
    Box(modifier.fillMaxWidth().height(height).clip(shape).background(colors.cardEdge)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(height).clip(shape).background(colors.accent))
    }
}

/** The testament mark in a 36×36 tile, as each catalogue row leads with. */
@Composable
internal fun TestamentTile(testament: Testament, size: Dp = 36.dp) {
    val colors = LocalAppColors.current
    val fontSize: TextUnit = if (testament == Testament.FULL) 9.sp else 11.sp
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.tileFill),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = testament.label,
            color = colors.testamentColor(testament),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = MonoFamily,
            letterSpacing = (-0.02).em,
        )
    }
}

/** A narrow vertical gap, for the few places a spacedBy would not read. */
@Composable
internal fun Gap(height: Dp) = Box(Modifier.width(1.dp).height(height))

/** In a pair of buttons the primary one is the wider — design 3c's `flex: 1.3` against `1`. */
internal const val PRIMARY_WEIGHT = 1.3f
