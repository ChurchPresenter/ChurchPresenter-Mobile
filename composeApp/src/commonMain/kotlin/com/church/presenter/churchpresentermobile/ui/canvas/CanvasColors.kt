package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.church.presenter.churchpresentermobile.model.CanvasInk
import com.church.presenter.churchpresentermobile.model.HighlightColor
import com.church.presenter.churchpresentermobile.model.ShapeFill
import com.church.presenter.churchpresentermobile.model.StickyColor
import com.church.presenter.churchpresentermobile.ui.standalone.parseHexColorOrNull
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors

/**
 * The canvas's own palette.
 *
 * Not the app's [com.church.presenter.churchpresentermobile.ui.theme.AppColors]: the
 * design draws the canvas as a drawing surface, with a board a step lighter than
 * its chrome and floating panels a step lighter again, and the app's translucent
 * surfaces have no opaque panel to float over a sketch. The light theme lines up
 * with the app's own light tokens; the dark one is the canvas's.
 */
@Immutable
internal data class CanvasColors(
    val isDark: Boolean,
    /** The top bar. */
    val chrome: Color,
    /** The page drawn on, and the gap inside a selected swatch's ring. */
    val board: Color,
    val boardDots: Color,
    /** The floating rail, dock, page strip and panels. */
    val panel: Color,
    val border: Color,
    val divider: Color,
    val text: Color,
    val secondary: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val accentTint: Color,
    val accentBorder: Color,
    /** A chip, tile or segmented track at rest. */
    val control: Color,
    /** The raised pill in a segmented control. */
    val segmentActive: Color,
    /** A toggle switched off, and the empty part of a slider. */
    val track: Color,
    val shadow: Color,
    val imageStripe: Color,
    val imageStripeAlt: Color,
    val danger: Color,
    val dangerTint: Color,
    val dangerBorder: Color,
)

/** The dark board's palette. `internal`, not private, so tests can check every ink on it. */
internal val DarkCanvasColors = CanvasColors(
    isDark = true,
    chrome = Color(0xFF0F1016),
    board = Color(0xFF1B1D26),
    boardDots = Color(0x12FFFFFF),
    panel = Color(0xFF181A22),
    border = Color(0x14FFFFFF),
    divider = Color(0x0FFFFFFF),
    text = Color(0xFFFAFAFA),
    secondary = Color(0xFFD4D4D8),
    muted = Color(0xFF8B8F99),
    accent = Color(0xFF86EFAC),
    onAccent = Color(0xFF08130C),
    accentTint = Color(0x1F86EFAC),
    accentBorder = Color(0x5986EFAC),
    control = Color(0x0FFFFFFF),
    segmentActive = Color(0x21FFFFFF),
    track = Color(0x1AFFFFFF),
    shadow = Color(0x73000000),
    imageStripe = Color(0xFF262A36),
    imageStripeAlt = Color(0xFF20232E),
    danger = Color(0xFFEF4444),
    dangerTint = Color(0x1FEF4444),
    dangerBorder = Color(0x4DEF4444),
)

/** The light board's palette. `internal`, not private, so tests can check every ink on it. */
internal val LightCanvasColors = CanvasColors(
    isDark = false,
    chrome = Color(0xFFF5F5F3),
    board = Color(0xFFFFFFFF),
    boardDots = Color(0x12000000),
    panel = Color(0xFFFFFFFF),
    border = Color(0x14000000),
    divider = Color(0x0F000000),
    text = Color(0xFF18181B),
    secondary = Color(0xFF3F3F46),
    muted = Color(0xFF71717A),
    accent = Color(0xFF16A34A),
    onAccent = Color(0xFFFFFFFF),
    accentTint = Color(0x1A16A34A),
    accentBorder = Color(0x4D16A34A),
    control = Color(0xFFF0F0EE),
    segmentActive = Color(0xFFFFFFFF),
    track = Color(0x17000000),
    shadow = Color(0x1F000000),
    imageStripe = Color(0xFFE9ECF1),
    imageStripeAlt = Color(0xFFF2F4F7),
    danger = Color(0xFFEF4444),
    dangerTint = Color(0x12EF4444),
    dangerBorder = Color(0x4DEF4444),
)

/** The canvas palette for the theme the app is in. */
internal val canvasColors: CanvasColors
    @Composable @ReadOnlyComposable
    get() = if (LocalAppColors.current.isDark) DarkCanvasColors else LightCanvasColors

/**
 * The color [ink] is drawn in on this theme's board.
 *
 * Contrast ink is white on the dark board and black on the light one; the bright
 * inks drop a shade on white so a thin stroke of them still reads.
 */
internal fun CanvasColors.inkColor(ink: CanvasInk): Color = when (ink) {
    CanvasInk.CONTRAST -> text
    CanvasInk.INVERSE -> if (isDark) Zinc900 else White
    CanvasInk.RED -> Red500
    CanvasInk.AMBER -> if (isDark) Amber500 else Amber600
    CanvasInk.GREEN -> accent
    CanvasInk.SKY -> if (isDark) Sky400 else Sky600
    CanvasInk.VIOLET -> Violet400
    CanvasInk.ORANGE -> if (isDark) Orange500 else Orange600
    CanvasInk.TEAL -> if (isDark) Teal400 else Teal600
    CanvasInk.BLUE -> if (isDark) Blue500 else Blue600
    CanvasInk.PINK -> if (isDark) Pink400 else Pink600
    CanvasInk.BROWN -> if (isDark) BrownLight else BrownDark
    CanvasInk.GRAY -> if (isDark) Gray400 else Gray500
    CanvasInk.YELLOW -> if (isDark) Yellow400 else Yellow600
    CanvasInk.PURPLE -> if (isDark) Purple500 else Purple600
}

/** [ink] on this board, unless the operator mixed a [custom] color, which wins. */
internal fun CanvasColors.inkColor(ink: CanvasInk, custom: String?): Color =
    custom?.let(::parseHexColorOrNull) ?: inkColor(ink)

/** The color a swatch for [ink] shows — the pure color, whichever board it will land on. */
internal fun CanvasColors.inkSwatch(ink: CanvasInk): Color = when (ink) {
    CanvasInk.CONTRAST -> if (isDark) Zinc50 else Zinc900
    CanvasInk.INVERSE -> if (isDark) Zinc900 else Zinc50
    CanvasInk.RED -> Red500
    CanvasInk.AMBER -> Amber500
    CanvasInk.GREEN -> Green300
    CanvasInk.SKY -> Sky400
    CanvasInk.VIOLET -> Violet400
    CanvasInk.ORANGE -> Orange500
    CanvasInk.TEAL -> Teal400
    CanvasInk.BLUE -> Blue500
    CanvasInk.PINK -> Pink400
    CanvasInk.BROWN -> BrownSwatch
    CanvasInk.GRAY -> Gray400
    CanvasInk.YELLOW -> Yellow400
    CanvasInk.PURPLE -> Purple500
}

internal fun highlightColor(color: HighlightColor): Color = when (color) {
    HighlightColor.YELLOW -> Yellow300
    HighlightColor.GREEN -> Green300
    HighlightColor.SKY -> Sky300
    HighlightColor.PINK -> Pink300
    HighlightColor.ORANGE -> Orange300
    HighlightColor.LIME -> Lime300
    HighlightColor.TEAL -> Teal300
    HighlightColor.PURPLE -> Purple300
    HighlightColor.RED -> Rose300
    HighlightColor.BLUE -> Indigo300
}

/** A shape's fill swatch; [ShapeFill.NONE] has none, and is drawn as a slashed circle instead. */
internal fun shapeFillColor(fill: ShapeFill): Color = when (fill) {
    ShapeFill.NONE -> Color.Transparent
    ShapeFill.GREEN -> Green300
    ShapeFill.SKY -> Sky400
    ShapeFill.YELLOW -> Amber200
    ShapeFill.PINK -> Red300
    ShapeFill.VIOLET -> Violet300
    ShapeFill.ORANGE -> Orange300
    ShapeFill.ROSE -> Pink300
    ShapeFill.GRAY -> Zinc300
    ShapeFill.BLACK -> Zinc900
}

internal fun stickyColor(color: StickyColor): Color = when (color) {
    StickyColor.YELLOW -> Amber200
    StickyColor.GREEN -> Green200
    StickyColor.BLUE -> Sky200
    StickyColor.PINK -> Pink200
    StickyColor.ORANGE -> Orange200
    StickyColor.PURPLE -> Purple200
}

/**
 * Words that read on a solid [fill]: near-black or white, whichever stands out more. The
 * outline's color — what the words were drawn in over the old faint tint — would vanish
 * into a solid fill of the same family.
 */
internal fun textOn(fill: Color): Color =
    if (contrast(fill, TextOnLight) >= contrast(fill, Color.White)) TextOnLight else Color.White

/**
 * The WCAG contrast ratio between [a] and [b], 1 to 21. Chosen by ratio rather than a
 * brightness cut-off: sky blue is just under half bright, yet white on it reads at about 2:1
 * and near-black at about 10:1.
 */
private fun contrast(a: Color, b: Color): Float {
    val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
    return (light + CONTRAST_OFFSET) / (dark + CONTRAST_OFFSET)
}

private const val CONTRAST_OFFSET = 0.05f
private val TextOnLight = Color(0xFF18181B)

/** Sticky-note text: a dark brown that reads on every note color, in either theme. */
internal val StickyInk = Color(0xFF4A3410)

/** The picker wheel's spectrum — the design's conic gradient, red at the top and round clockwise. */
internal val CustomSwatchColors = listOf(
    Color(0xFFEF4444),
    Color(0xFFFACC15),
    Color(0xFF22C55E),
    Color(0xFF38BDF8),
    Color(0xFFA855F7),
    Color(0xFFEF4444),
)

/*
 * The named colors the inks, highlighters, fills and notes are drawn from — the
 * Tailwind shades the design is specified in.
 */
private val Zinc900 = Color(0xFF18181B)
private val White = Color(0xFFFFFFFF)
private val Red500 = Color(0xFFEF4444)
private val Amber500 = Color(0xFFF59E0B)
private val Amber600 = Color(0xFFD97706)
private val Sky400 = Color(0xFF38BDF8)
private val Sky600 = Color(0xFF0284C7)
private val Violet400 = Color(0xFFA78BFA)
private val Zinc50 = Color(0xFFFAFAFA)
private val Green300 = Color(0xFF86EFAC)
private val Yellow300 = Color(0xFFFDE047)
private val Sky300 = Color(0xFF7DD3FC)
private val Pink300 = Color(0xFFF9A8D4)
private val Orange300 = Color(0xFFFDBA74)
private val Amber200 = Color(0xFFFDE68A)
private val Red300 = Color(0xFFFCA5A5)
private val Green200 = Color(0xFFBBF7D0)
private val Sky200 = Color(0xFFBAE6FD)
private val Pink200 = Color(0xFFFBCFE8)
private val Orange200 = Color(0xFFFED7AA)
private val Purple200 = Color(0xFFE9D5FF)
private val Orange500 = Color(0xFFF97316)
private val Orange600 = Color(0xFFEA580C)
private val Teal300 = Color(0xFF5EEAD4)
private val Teal400 = Color(0xFF2DD4BF)
private val Teal600 = Color(0xFF0D9488)
private val Indigo300 = Color(0xFFA5B4FC)
private val Rose300 = Color(0xFFFDA4AF)
private val Violet300 = Color(0xFFC4B5FD)
private val Zinc300 = Color(0xFFD4D4D8)
private val Gray400 = Color(0xFF9CA3AF)
private val Gray500 = Color(0xFF6B7280)
private val Yellow400 = Color(0xFFFACC15)
private val Yellow600 = Color(0xFFCA8A04)
private val Purple500 = Color(0xFFA855F7)
private val Purple600 = Color(0xFF9333EA)
private val Blue500 = Color(0xFF3B82F6)
private val Blue600 = Color(0xFF2563EB)
private val Pink400 = Color(0xFFF472B6)
private val Pink600 = Color(0xFFDB2777)
private val Lime300 = Color(0xFFBEF264)
private val Purple300 = Color(0xFFD8B4FE)

/* Brown has no Tailwind shade; these are a warm mid-brown and a lift and a drop of it. */
private val BrownSwatch = Color(0xFFA0672F)
private val BrownLight = Color(0xFFC08A55)
private val BrownDark = Color(0xFF8B5A2B)
