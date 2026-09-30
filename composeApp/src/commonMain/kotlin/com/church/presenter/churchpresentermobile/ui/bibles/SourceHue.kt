package com.church.presenter.churchpresentermobile.ui.bibles

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.church.presenter.churchpresentermobile.bibleimport.BibleXmlFormat
import com.church.presenter.churchpresentermobile.bibleimport.catalog.BibleSource
import com.church.presenter.churchpresentermobile.bibleimport.catalog.Testament
import com.church.presenter.churchpresentermobile.ui.theme.AppColors

/**
 * The Bible downloads design's own colours: one hue per archive, used on its format badge, its
 * testament mark and its dot in the tablet's source list — so a row says where it came from at a
 * glance. Values are design "Church Presenter Bible Downloads", dark and light.
 */
@Immutable
internal data class SourceHue(val fg: Color, val tint: Color, val edge: Color)

// Each archive's hue at full strength, as a 12%/9% tint, and as a 30% edge — design values.
private val USFX_DARK = SourceHue(Color(0xFF7DD3FC), Color(0x1F7DD3FC), Color(0x4D7DD3FC))
private val USFX_LIGHT = SourceHue(Color(0xFF0284C7), Color(0x170284C7), Color(0x4D0284C7))
private val ZEFANIA_DARK = SourceHue(Color(0xFFC9A2F0), Color(0x1FC9A2F0), Color(0x4DC9A2F0))
private val ZEFANIA_LIGHT = SourceHue(Color(0xFF7C3AED), Color(0x177C3AED), Color(0x4D7C3AED))
private val BEBLIA_DARK = SourceHue(Color(0xFFE0B45C), Color(0x1FE0B45C), Color(0x4DE0B45C))
private val BEBLIA_LIGHT = SourceHue(Color(0xFFB4781E), Color(0x17B4781E), Color(0x4DB4781E))

internal fun AppColors.hueOf(format: BibleXmlFormat): SourceHue = when (format) {
    BibleXmlFormat.USFX -> if (isDark) USFX_DARK else USFX_LIGHT
    BibleXmlFormat.ZEFANIA -> if (isDark) ZEFANIA_DARK else ZEFANIA_LIGHT
    BibleXmlFormat.BEBLIA -> if (isDark) BEBLIA_DARK else BEBLIA_LIGHT
}

/** A quiet button or pill: 6% white on dark, plain white on light. */
internal val AppColors.quietFill: Color get() = if (isDark) Color(0x0FFFFFFF) else Color.White

/** A small tile behind a mark — the testament tile: 6% white on dark, 4% black on light. */
internal val AppColors.tileFill: Color get() = if (isDark) Color(0x0FFFFFFF) else Color(0x0A000000)

/** The source tabs' track: 5% white on dark, 4% black on light. */
internal val AppColors.trackFill: Color get() = if (isDark) Color(0x0DFFFFFF) else Color(0x0A000000)

/** The chosen source tab's pill: 12% white on dark, white on light. */
internal val AppColors.segmentActiveFill: Color get() = if (isDark) Color(0x1FFFFFFF) else Color.White

/** The convert screen's pinned button bar. */
internal val AppColors.bottomBarFill: Color get() = if (isDark) Color(0xFF111117) else Color.White

/** The number in a step not reached yet. */
internal val AppColors.aheadInk: Color get() = if (isDark) Color(0xFF52525B) else dim

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

/** Behind an error's icon: the danger colour at 12% on dark, 8% on light. */
internal val AppColors.errorTint: Color get() = danger.copy(alpha = if (isDark) 0.12f else 0.08f)
