package com.church.presenter.churchpresentermobile.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests the built-in looks a church picks from on a Sunday morning.
 *
 * Each preset has to be a complete, usable screen on its own — a preset with a
 * malformed colour in it renders as a blank wall in front of a congregation, so
 * the colours are checked rather than assumed.
 */
class SlideThemePresetsTest {

    private val hex = Regex("^#[0-9A-Fa-f]{6}$")

    @Test
    fun `default is first, so putting it back is a tap`() {
        assertEquals("Default", SlideThemePresets.all.first().name)
        assertEquals(SlideTheme(), SlideThemePresets.all.first().theme)
    }

    @Test
    fun `names are unique, so a list of them is unambiguous`() {
        val names = SlideThemePresets.all.map { it.name }

        assertEquals(names.size, names.toSet().size)
        assertTrue(names.none { it.isBlank() })
    }

    @Test
    fun `every preset colour is a six-digit hex`() {
        for ((name, theme) in SlideThemePresets.all.map { it.name to it.theme }) {
            assertTrue(hex.matches(theme.textColor), "$name textColor: ${theme.textColor}")
            assertTrue(hex.matches(theme.accentColor), "$name accentColor: ${theme.accentColor}")
            assertTrue(hex.matches(theme.gradientTop), "$name gradientTop: ${theme.gradientTop}")
            assertTrue(hex.matches(theme.gradientBottom), "$name gradientBottom: ${theme.gradientBottom}")
        }
    }

    @Test
    fun `no preset draws its text in its own background colour`() {
        for ((name, theme) in SlideThemePresets.all.map { it.name to it.theme }) {
            assertNotEquals(theme.gradientTop.uppercase(), theme.textColor.uppercase(), "$name top")
            assertNotEquals(theme.gradientBottom.uppercase(), theme.textColor.uppercase(), "$name bottom")
        }
    }

    @Test
    fun `every preset is a distinct look`() {
        val themes = SlideThemePresets.all.map { it.theme }

        assertEquals(themes.size, themes.toSet().size)
    }

    @Test
    fun `applying a preset is a single assignment that carries its whole look`() {
        val paper = SlideThemePresets.all.first { it.name == "Paper" }.theme

        // Bright-room preset: dark text on a light ground, the inverse of Default.
        assertEquals("#1A1A1A", paper.textColor)
        assertEquals("#FAF7F0", paper.gradientTop)
        assertNotEquals(SlideTheme().textColor, paper.textColor)
    }
}
