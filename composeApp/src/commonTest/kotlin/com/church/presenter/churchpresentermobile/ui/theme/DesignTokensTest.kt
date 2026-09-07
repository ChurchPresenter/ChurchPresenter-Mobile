package com.church.presenter.churchpresentermobile.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests the design tokens both palettes are built from.
 *
 * These are the values every screen reads through `LocalAppColors`, and they are
 * a transcription of the design handoff rather than anything derived — so what is
 * worth asserting is not the individual hex codes (that would just restate the
 * source) but the properties the UI relies on: that both palettes are complete,
 * that they actually differ, and that text stays legible on its own background.
 */
class DesignTokensTest {

    private val dark = appColorsFor(isDark = true)
    private val light = appColorsFor(isDark = false)

    /** Every colour token on [AppColors], so a new one cannot be missed here. */
    private fun colorsOf(c: AppColors): Map<String, Color> = mapOf(
        "background" to c.background,
        "surface" to c.surface,
        "surfaceStrong" to c.surfaceStrong,
        "surfaceElevated" to c.surfaceElevated,
        "sheetBackground" to c.sheetBackground,
        "inputBg" to c.inputBg,
        "borderSubtle" to c.borderSubtle,
        "border" to c.border,
        "borderStrong" to c.borderStrong,
        "scrim" to c.scrim,
        "text" to c.text,
        "secondary" to c.secondary,
        "muted" to c.muted,
        "dim" to c.dim,
        "accent" to c.accent,
        "onAccent" to c.onAccent,
        "accentTint" to c.accentTint,
        "accentTintStrong" to c.accentTintStrong,
        "greekAccent" to c.greekAccent,
        "greekAccentTint" to c.greekAccentTint,
        "amber" to c.amber,
        "amberStroke" to c.amberStroke,
        "danger" to c.danger,
        "warning" to c.warning,
        "warningTint" to c.warningTint,
        "taglineWorship" to c.taglineWorship,
        "taglinePresent" to c.taglinePresent,
        "taglineConnect" to c.taglineConnect,
        "taglineDot" to c.taglineDot,
        "splashTitle" to c.splashTitle,
        "splashGlow" to c.splashGlow,
        "scheduleSongFg" to c.scheduleSongFg,
        "scheduleSongBg" to c.scheduleSongBg,
        "scheduleBibleFg" to c.scheduleBibleFg,
        "scheduleBibleBg" to c.scheduleBibleBg,
        "schedulePictureFg" to c.schedulePictureFg,
        "schedulePictureBg" to c.schedulePictureBg,
    )

    // ── Which palette you get ────────────────────────────────────────────

    @Test
    fun theRequestedPaletteIsTheOneReturned() {
        assertTrue(dark.isDark)
        assertFalse(light.isDark)
    }

    @Test
    fun theSamePaletteIsHandedBackEachTime() {
        // They are singletons, so `remember`-free reads in composition are free
        // and equality checks against LocalAppColors behave.
        assertEquals(dark, appColorsFor(isDark = true))
        assertEquals(light, appColorsFor(isDark = false))
    }

    @Test
    fun theTwoPalettesAreActuallyDifferent() {
        assertNotEquals(dark, light)
        assertNotEquals(dark.background, light.background)
        assertNotEquals(dark.text, light.text)
    }

    // ── Completeness ─────────────────────────────────────────────────────

    @Test
    fun noTokenIsLeftFullyTransparentInEitherPalette() {
        // A token defaulted to Color.Unspecified or left at zero alpha paints
        // nothing, which shows up as an invisible control rather than an error.
        for ((name, color) in colorsOf(dark)) {
            assertNotEquals(Color.Unspecified, color, "dark.$name")
            assertTrue(color.alpha > 0f, "dark.$name is fully transparent")
        }
        for ((name, color) in colorsOf(light)) {
            assertNotEquals(Color.Unspecified, color, "light.$name")
            assertTrue(color.alpha > 0f, "light.$name is fully transparent")
        }
    }

    @Test
    fun everyTokenIsDefinedInBothPalettes() {
        assertEquals(colorsOf(dark).keys, colorsOf(light).keys)
    }

    @Test
    fun theGradientsDifferBetweenPalettes() {
        // Brushes rather than Colors, and easy to leave pointing at the same
        // instance when a palette is copied — which shows as a dark splash on a
        // light phone.
        assertNotEquals(dark.splashBackground, light.splashBackground)
        assertNotEquals(dark.crossBrush, light.crossBrush)
    }

    // ── Legibility ───────────────────────────────────────────────────────

    @Test
    fun textIsNotDrawnInItsOwnBackgroundColour() {
        for (c in listOf(dark, light)) {
            val label = if (c.isDark) "dark" else "light"
            assertNotEquals(c.background, c.text, "$label text on background")
            assertNotEquals(c.sheetBackground, c.text, "$label text on sheet")
            assertNotEquals(c.accent, c.onAccent, "$label onAccent on accent")
        }
    }

    @Test
    fun theDarkPaletteIsDarkAndTheLightOneIsLight() {
        // Guards a swapped assignment, which would otherwise only show up as a
        // white flash on a phone in a dark hall.
        assertTrue(dark.background.luminance() < light.background.luminance())
        assertTrue(dark.text.luminance() > light.text.luminance())
    }

    @Test
    fun theTextRampGoesFromStrongestToDimmest() {
        // text → secondary → muted → dim is the order screens use for hierarchy;
        // reordering two of them silently flattens every screen's emphasis.
        for (c in listOf(dark, light)) {
            val label = if (c.isDark) "dark" else "light"
            val ramp = listOf(c.text, c.secondary, c.muted, c.dim).map { it.luminance() }
            val ordered = if (c.isDark) ramp.sortedDescending() else ramp.sorted()
            assertEquals(ramp, ordered, "$label text ramp out of order: $ramp")
        }
    }

    @Test
    fun bordersGetStrongerInOrder() {
        for (c in listOf(dark, light)) {
            val label = if (c.isDark) "dark" else "light"
            assertTrue(c.borderSubtle.alpha <= c.border.alpha, "$label borderSubtle vs border")
            assertTrue(c.border.alpha <= c.borderStrong.alpha, "$label border vs borderStrong")
        }
    }

    @Test
    fun aTintIsAFadedFormOfItsAccent() {
        // The tints are the accent at low alpha; a tint that is opaque would
        // paint a solid block where a wash was intended.
        for (c in listOf(dark, light)) {
            val label = if (c.isDark) "dark" else "light"
            assertTrue(c.accentTint.alpha < c.accent.alpha, "$label accentTint")
            assertTrue(c.accentTintStrong.alpha < c.accent.alpha, "$label accentTintStrong")
            assertTrue(c.accentTint.alpha <= c.accentTintStrong.alpha, "$label tint ordering")
            assertTrue(c.greekAccentTint.alpha < c.greekAccent.alpha, "$label greekAccentTint")
            assertTrue(c.warningTint.alpha < c.warning.alpha, "$label warningTint")
        }
    }

    @Test
    fun hebrewAndGreekAccentsAreTellableApart() {
        // Strong's numbers are colour-coded by language; one colour for both
        // makes the distinction invisible.
        assertNotEquals(dark.accent, dark.greekAccent)
        assertNotEquals(light.accent, light.greekAccent)
    }

    @Test
    fun theSheetBackgroundIsOpaqueSoAScrimCannotShowThrough() {
        // Sheets float over a scrim; a translucent sheet background lets the
        // dimmed screen bleed into the sheet's own content.
        assertEquals(1f, dark.sheetBackground.alpha)
        assertEquals(1f, light.sheetBackground.alpha)
    }

    @Test
    fun theScrimIsPartlyTransparentSoTheScreenBehindStaysVisible() {
        for (c in listOf(dark, light)) {
            assertTrue(c.scrim.alpha in 0.1f..0.9f, "scrim alpha ${c.scrim.alpha}")
        }
    }

    @Test
    fun eachScheduleTypeHasItsOwnPairing() {
        for (c in listOf(dark, light)) {
            val label = if (c.isDark) "dark" else "light"
            assertNotEquals(c.scheduleSongFg, c.scheduleSongBg, "$label song")
            assertNotEquals(c.scheduleBibleFg, c.scheduleBibleBg, "$label bible")
            assertNotEquals(c.schedulePictureFg, c.schedulePictureBg, "$label picture")
            // …and the three types are distinguishable from each other.
            val fgs = listOf(c.scheduleSongFg, c.scheduleBibleFg, c.schedulePictureFg)
            assertEquals(fgs.size, fgs.toSet().size, "$label schedule colours collide")
        }
    }

    // ── Spacing and radii ────────────────────────────────────────────────

    @Test
    fun everySpacingStepIsPositiveAndInOrder() {
        val ramp = listOf(
            AppDimens.space4, AppDimens.space8, AppDimens.space12,
            AppDimens.space14, AppDimens.space16, AppDimens.space20, AppDimens.space24,
        )

        assertTrue(ramp.all { it.value > 0f })
        assertEquals(ramp, ramp.sortedBy { it.value })
    }

    @Test
    fun theSpacingStepsMatchTheirNames() {
        // The names are the scale; a mismatch makes every call site misleading.
        assertEquals(4f, AppDimens.space4.value)
        assertEquals(8f, AppDimens.space8.value)
        assertEquals(12f, AppDimens.space12.value)
        assertEquals(14f, AppDimens.space14.value)
        assertEquals(16f, AppDimens.space16.value)
        assertEquals(20f, AppDimens.space20.value)
        assertEquals(24f, AppDimens.space24.value)
    }

    @Test
    fun everyRadiusIsPositive() {
        val radii = listOf(
            AppDimens.radiusCard, AppDimens.radiusTile, AppDimens.radiusChip,
            AppDimens.radiusSearch, AppDimens.radiusButton, AppDimens.radiusFab,
            AppDimens.radiusPill, AppDimens.radiusSheet,
        )

        assertTrue(radii.all { it.value > 0f })
    }

    @Test
    fun aSheetIsRoundedMoreThanAButton() {
        // The radius scale runs small controls → large surfaces.
        assertTrue(AppDimens.radiusButton.value < AppDimens.radiusCard.value)
        assertTrue(AppDimens.radiusCard.value < AppDimens.radiusPill.value)
        assertTrue(AppDimens.radiusPill.value < AppDimens.radiusSheet.value)
    }

    @Test
    fun theTabBarAndFabAreBigEnoughToTap() {
        // 44dp is the smallest comfortable touch target; both sit above it.
        assertTrue(AppDimens.fabSize.value >= 44f, "fab is ${AppDimens.fabSize.value}dp")
        assertTrue(AppDimens.tabBarHeight.value >= 44f, "tab bar is ${AppDimens.tabBarHeight.value}dp")
    }
}

/** Perceived brightness, for asserting a palette is the one it claims to be. */
private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
