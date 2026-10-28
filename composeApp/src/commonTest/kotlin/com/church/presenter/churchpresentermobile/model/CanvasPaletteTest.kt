package com.church.presenter.churchpresentermobile.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CanvasPaletteTest {

    private val dark = inkPalette(isDark = true)

    @Test
    fun `before anything is mixed a row is the ten presets in order`() {
        val slots = paletteSlots(emptyList(), dark)

        assertEquals(PALETTE_SIZE, slots.size)
        assertEquals(dark.map { PaletteSlot.Preset(it) }, slots)
    }

    @Test
    fun `the presets are ten distinct colors`() {
        assertEquals(PALETTE_SIZE, dark.toSet().size)
        assertEquals(PALETTE_SIZE, HighlightPalette.toSet().size)
    }

    @Test
    fun `the ink row is the design's, white and black then gray to purple`() {
        val colors = listOf(
            CanvasInk.GRAY, CanvasInk.RED, CanvasInk.ORANGE, CanvasInk.YELLOW,
            CanvasInk.GREEN, CanvasInk.SKY, CanvasInk.BLUE, CanvasInk.PURPLE,
        )
        assertEquals(listOf(CanvasInk.CONTRAST, CanvasInk.INVERSE) + colors, inkPalette(isDark = true))
        assertEquals(listOf(CanvasInk.INVERSE, CanvasInk.CONTRAST) + colors, inkPalette(isDark = false))
    }

    @Test
    fun `mixes come first and push the last presets off the end`() {
        val slots = paletteSlots(listOf("#111111", "#222222"), dark)

        assertEquals(PALETTE_SIZE, slots.size, "the row keeps its length")
        assertEquals(listOf(PaletteSlot.Mixed("#111111"), PaletteSlot.Mixed("#222222")), slots.take(2))
        assertEquals(dark.take(PALETTE_SIZE - 2).map { PaletteSlot.Preset(it) }, slots.drop(2))
    }

    @Test
    fun `ten mixes fill the row`() {
        val mixes = (0 until PALETTE_SIZE).map { "#00000$it" }

        assertEquals(mixes.map { PaletteSlot.Mixed(it) }, paletteSlots(mixes, dark))
    }

    @Test
    fun `a new mix goes to the front`() {
        assertEquals(listOf("#333333", "#111111", "#222222"), mixedInto(listOf("#111111", "#222222"), "#333333"))
    }

    @Test
    fun `mixing a color already there moves it to the front instead of repeating it`() {
        assertEquals(listOf("#222222", "#111111"), mixedInto(listOf("#111111", "#222222"), "#222222"))
    }

    @Test
    fun `the same color in another case is the same mix`() {
        assertEquals(listOf("#abcdef", "#111111"), mixedInto(listOf("#111111", "#ABCDEF"), "#abcdef"))
    }

    @Test
    fun `mixes are kept to the row's length, oldest dropped`() {
        val full = (0 until PALETTE_SIZE).map { "#00000$it" }

        val mixed = mixedInto(full, "#FFFFFF")

        assertEquals(PALETTE_SIZE, mixed.size)
        assertEquals("#FFFFFF", mixed.first())
        assertEquals(full.dropLast(1), mixed.drop(1))
    }
}
