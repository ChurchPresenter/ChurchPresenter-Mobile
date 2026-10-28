package com.church.presenter.churchpresentermobile.model

/** How many colors a pen or highlighter row offers, the operator's own mixes included. */
const val PALETTE_SIZE = 10

/**
 * The ten inks a row offers before anything is mixed, in the design's order: white and black,
 * then gray, red, orange, yellow, green, sky, blue and purple.
 *
 * White and black are the board's own contrast and inverse inks — white ink is [CanvasInk.CONTRAST]
 * on the dark board and [CanvasInk.INVERSE] on the light one — so a drawing keeps reading when the
 * theme changes. [isDark] says which board the row is for.
 */
fun inkPalette(isDark: Boolean): List<CanvasInk> {
    val white = if (isDark) CanvasInk.CONTRAST else CanvasInk.INVERSE
    val black = if (isDark) CanvasInk.INVERSE else CanvasInk.CONTRAST
    return listOf(
        white, black, CanvasInk.GRAY, CanvasInk.RED, CanvasInk.ORANGE,
        CanvasInk.YELLOW, CanvasInk.GREEN, CanvasInk.SKY, CanvasInk.BLUE, CanvasInk.PURPLE,
    )
}

/** The ten highlighter colors, in the order the row shows them. */
val HighlightPalette: List<HighlightColor> = listOf(
    HighlightColor.YELLOW, HighlightColor.LIME, HighlightColor.GREEN, HighlightColor.TEAL, HighlightColor.SKY,
    HighlightColor.BLUE, HighlightColor.PURPLE, HighlightColor.PINK, HighlightColor.RED, HighlightColor.ORANGE,
)

/** One swatch in a color row: a named preset, or a "#RRGGBB" the operator mixed. */
sealed interface PaletteSlot<out T> {
    data class Preset<T>(val value: T) : PaletteSlot<T>
    data class Mixed(val hex: String) : PaletteSlot<Nothing>
}

/**
 * A color row: the latest mixes first, then the presets, [PALETTE_SIZE] in all.
 * Each new mix pushes the last preset off the end, so the row keeps its length
 * and the colors the operator actually uses stay one tap away.
 */
fun <T> paletteSlots(mixed: List<String>, presets: List<T>): List<PaletteSlot<T>> =
    (mixed.map { PaletteSlot.Mixed(it) } + presets.map { PaletteSlot.Preset(it) }).take(PALETTE_SIZE)

/** [mixes] with [hex] at the front — moved there if it was already in them — kept to [PALETTE_SIZE]. */
fun mixedInto(mixes: List<String>, hex: String): List<String> =
    (listOf(hex) + mixes.filterNot { it.equals(hex, ignoreCase = true) }).take(PALETTE_SIZE)
