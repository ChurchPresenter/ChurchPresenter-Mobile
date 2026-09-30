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
import androidx.compose.runtime.Immutable
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
import com.church.presenter.churchpresentermobile.bibleimport.catalog.BibleSource
import com.church.presenter.churchpresentermobile.bibleimport.catalog.Testament
import com.church.presenter.churchpresentermobile.ui.theme.AppColors
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors

/**
 * The Bible downloads design's own colours: one hue per archive, used on its format badge, its
 * testament mark and its dot in the tablet's source list — so a row says where it came from at a
 * glance. Values are design "Church Presenter Bible Downloads", dark and light.
 */
@Immutable
internal data class SourceHue(val fg: Color, val tint: Color, val edge: Color)

internal fun AppColors.hueOf(format: BibleXmlFormat): SourceHue = when (format) {
    BibleXmlFormat.USFX -> if (isDark) {
        SourceHue(Color(0xFF7DD3FC), Color(0x1F7DD3FC), Color(0x4D7DD3FC))
    } else {
        SourceHue(Color(0xFF0284C7), Color(0x170284C7), Color(0x4D0284C7))
    }
    BibleXmlFormat.ZEFANIA -> if (isDark) {
        SourceHue(Color(0xFFC9A2F0), Color(0x1FC9A2F0), Color(0x4DC9A2F0))
    } else {
        SourceHue(Color(0xFF7C3AED), Color(0x177C3AED), Color(0x4D7C3AED))
    }
    BibleXmlFormat.BEBLIA -> if (isDark) {
        SourceHue(Color(0xFFE0B45C), Color(0x1FE0B45C), Color(0x4DE0B45C))
    } else {
        SourceHue(Color(0xFFB4781E), Color(0x17B4781E), Color(0x4DB4781E))
    }
}

internal fun AppColors.hueOf(source: BibleSource): SourceHue = hueOf(source.format)

/** The testament mark's colour: green for a whole Bible, blue for NT, amber for OT. */
internal fun AppColors.testamentColor(testament: Testament): Color = when (testament) {
    Testament.FULL -> accent
    Testament.NEW -> hueOf(BibleXmlFormat.USFX).fg
    Testament.OLD -> hueOf(BibleXmlFormat.BEBLIA).fg
}

/** The design's secondary grey on the selector (`#8b8f99`) — between [AppColors.muted] and text. */
internal val AppColors.selectorSubtle: Color get() = if (isDark) Color(0xFF8B8F99) else muted

/** A card fill: faint white on dark, plain white on light. */
internal val AppColors.cardFill: Color get() = if (isDark) Color(0x0AFFFFFF) else Color.White

/** A card's hairline: 8% white on dark, 7% black on light. */
internal val AppColors.cardEdge: Color get() = if (isDark) Color(0x14FFFFFF) else Color(0x12000000)

/** The accent-tinted fill of a chosen or in-progress row. */
internal val AppColors.chosenFill: Color get() = if (isDark) Color(0x1486EFAC) else Color(0x1216A34A)

internal val AppColors.chosenEdge: Color get() = if (isDark) Color(0x4786EFAC) else Color(0x4016A34A)

/** The ink on an accent button — the design's near-black green on dark, white on light. */
internal val AppColors.onAccentButton: Color get() = if (isDark) Color(0xFF08130C) else Color.White

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
        ButtonKind.QUIET -> Triple(if (colors.isDark) Color(0x0FFFFFFF) else Color.White, colors.text, colors.cardEdge)
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
            .background(if (colors.isDark) Color(0x0FFFFFFF) else Color(0x0A000000)),
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

/** 1,248 — digits grouped by thousands, which common Kotlin has no formatter for. */
internal fun grouped(value: Int): String {
    val digits = value.toString()
    if (value < GROUP_SIZE_LIMIT) return digits
    return digits.reversed().chunked(GROUP_WIDTH).joinToString(",").reversed()
}

/** 4.6 MB / 212 KB — one decimal for megabytes, whole kilobytes. */
internal fun sizeLabel(bytes: Long): String = when {
    bytes >= BYTES_PER_MB -> {
        val tenths = (bytes * TENTHS + BYTES_PER_MB / 2) / BYTES_PER_MB
        "${tenths / TENTHS}.${tenths % TENTHS} MB"
    }
    else -> "${(bytes + BYTES_PER_KB / 2) / BYTES_PER_KB} KB"
}

private const val GROUP_SIZE_LIMIT = 1000
private const val GROUP_WIDTH = 3
private const val BYTES_PER_KB = 1024L
private const val BYTES_PER_MB = 1024L * 1024L
private const val TENTHS = 10L

/** A narrow vertical gap, for the few places a spacedBy would not read. */
@Composable
internal fun Gap(height: Dp) = Box(Modifier.width(1.dp).height(height))
