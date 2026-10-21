package com.church.presenter.churchpresentermobile.ui.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.church.presenter.churchpresentermobile.model.CanvasInk
import com.church.presenter.churchpresentermobile.model.HighlightColor
import com.church.presenter.churchpresentermobile.model.ShapeFill
import com.church.presenter.churchpresentermobile.model.StickyColor
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

private val DarkCanvasColors = CanvasColors(
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

private val LightCanvasColors = CanvasColors(
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
 * The colour [ink] is drawn in on this theme's board.
 *
 * Contrast ink is white on the dark board and black on the light one; the bright
 * inks drop a shade on white so a thin stroke of them still reads.
 */
internal fun CanvasColors.inkColor(ink: CanvasInk): Color = when (ink) {
    CanvasInk.CONTRAST -> text
    CanvasInk.INVERSE -> if (isDark) Color(0xFF18181B) else Color(0xFFFFFFFF)
    CanvasInk.RED -> Color(0xFFEF4444)
    CanvasInk.AMBER -> if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706)
    CanvasInk.GREEN -> accent
    CanvasInk.SKY -> if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    CanvasInk.VIOLET -> Color(0xFFA78BFA)
}

/** The colour a swatch for [ink] shows — the pure colour, whichever board it will land on. */
internal fun CanvasColors.inkSwatch(ink: CanvasInk): Color = when (ink) {
    CanvasInk.CONTRAST -> if (isDark) Color(0xFFFAFAFA) else Color(0xFF18181B)
    CanvasInk.INVERSE -> if (isDark) Color(0xFF18181B) else Color(0xFFFFFFFF)
    CanvasInk.RED -> Color(0xFFEF4444)
    CanvasInk.AMBER -> Color(0xFFF59E0B)
    CanvasInk.GREEN -> Color(0xFF86EFAC)
    CanvasInk.SKY -> Color(0xFF38BDF8)
    CanvasInk.VIOLET -> Color(0xFFA78BFA)
}

/**
 * The inks in the order the dock lists them: the board's contrast ink first, and
 * the inverse — useful only for knocking out — on the end in the light theme,
 * where it is white and least likely to be wanted.
 */
internal fun CanvasColors.dockInks(): List<CanvasInk> =
    if (isDark) CanvasInk.entries else CanvasInk.entries - CanvasInk.INVERSE + CanvasInk.INVERSE

internal fun highlightColor(color: HighlightColor): Color = when (color) {
    HighlightColor.YELLOW -> Color(0xFFFDE047)
    HighlightColor.GREEN -> Color(0xFF86EFAC)
    HighlightColor.SKY -> Color(0xFF7DD3FC)
    HighlightColor.PINK -> Color(0xFFF9A8D4)
    HighlightColor.ORANGE -> Color(0xFFFDBA74)
}

/** A shape's fill swatch; [ShapeFill.NONE] has none, and is drawn as a slashed circle instead. */
internal fun shapeFillColor(fill: ShapeFill): Color = when (fill) {
    ShapeFill.NONE -> Color.Transparent
    ShapeFill.GREEN -> Color(0xFF86EFAC)
    ShapeFill.SKY -> Color(0xFF38BDF8)
    ShapeFill.YELLOW -> Color(0xFFFDE68A)
    ShapeFill.PINK -> Color(0xFFFCA5A5)
}

internal fun stickyColor(color: StickyColor): Color = when (color) {
    StickyColor.YELLOW -> Color(0xFFFDE68A)
    StickyColor.GREEN -> Color(0xFFBBF7D0)
    StickyColor.BLUE -> Color(0xFFBAE6FD)
    StickyColor.PINK -> Color(0xFFFBCFE8)
    StickyColor.ORANGE -> Color(0xFFFED7AA)
    StickyColor.PURPLE -> Color(0xFFE9D5FF)
}

/** Sticky-note text: a dark brown that reads on every note colour, in either theme. */
internal val StickyInk = Color(0xFF4A3410)

/** The spectrum on the "custom colour" swatch. */
internal val CustomSwatchColors = listOf(
    Color(0xFFF87171),
    Color(0xFFFACC15),
    Color(0xFF86EFAC),
    Color(0xFF7DD3FC),
    Color(0xFFA78BFA),
    Color(0xFFF87171),
)
