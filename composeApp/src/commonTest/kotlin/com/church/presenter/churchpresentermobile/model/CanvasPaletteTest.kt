package com.church.presenter.churchpresentermobile.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CanvasPaletteTest {

    @Test
    fun `before anything is mixed a row is the ten presets in order`() {
        val slots = paletteSlots(emptyList(), InkPalette)

        assertEquals(PALETTE_SIZE, slots.size)
        assertEquals(InkPalette.map { PaletteSlot.Preset(it) }, slots)
    }

    @Test
    fun `the presets are ten distinct colors`() {
        assertEquals(PALETTE_SIZE, InkPalette.toSet().size)
        assertEquals(PALETTE_SIZE, HighlightPalette.toSet().size)
    }

    @Test
    fun `mixes come first and push the last presets off the end`() {
        val slots = paletteSlots(listOf("#111111", "#222222"), InkPalette)

        assertEquals(PALETTE_SIZE, slots.size, "the row keeps its length")
        assertEquals(listOf(PaletteSlot.Mixed("#111111"), PaletteSlot.Mixed("#222222")), slots.take(2))
        assertEquals(InkPalette.take(PALETTE_SIZE - 2).map { PaletteSlot.Preset(it) }, slots.drop(2))
    }

    @Test
    fun `ten mixes fill the row`() {
        val mixes = (0 until PALETTE_SIZE).map { "#00000$it" }

        assertEquals(mixes.map { PaletteSlot.Mixed(it) }, paletteSlots(mixes, InkPalette))
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
