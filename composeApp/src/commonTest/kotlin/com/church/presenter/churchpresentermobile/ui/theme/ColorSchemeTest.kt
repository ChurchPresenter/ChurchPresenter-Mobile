package com.church.presenter.churchpresentermobile.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests the Material schemes [AppTheme] hands to `MaterialTheme`.
 *
 * Screens mix these with [LocalAppColors]: Material components for controls, the
 * app's own tokens for the surfaces a `ColorScheme` cannot express. That only
 * works if the two agree about which mode they are in, so what is checked here is
 * coherence — every role filled, the pairs readable, and the dark scheme actually
 * darker — rather than the individual hex values, which would just restate the file.
 */
class ColorSchemeTest {

    /** Every role a screen actually reads, and the surface it is drawn on. */
    private fun pairsOf(s: ColorScheme): List<Triple<String, Color, Color>> = listOf(
        Triple("primary", s.onPrimary, s.primary),
        Triple("primaryContainer", s.onPrimaryContainer, s.primaryContainer),
        Triple("secondary", s.onSecondary, s.secondary),
        Triple("secondaryContainer", s.onSecondaryContainer, s.secondaryContainer),
        Triple("tertiary", s.onTertiary, s.tertiary),
        Triple("tertiaryContainer", s.onTertiaryContainer, s.tertiaryContainer),
        Triple("background", s.onBackground, s.background),
        Triple("surface", s.onSurface, s.surface),
        Triple("surfaceVariant", s.onSurfaceVariant, s.surfaceVariant),
        Triple("error", s.onError, s.error),
        Triple("errorContainer", s.onErrorContainer, s.errorContainer),
    )

    @Test
    fun everyRoleIsFilledInBothSchemes() {
        // An unset role falls back to Color.Unspecified, which paints nothing —
        // an invisible label rather than a build error.
        for ((label, scheme) in listOf("light" to LightColorScheme, "dark" to DarkColorScheme)) {
            for ((role, on, container) in pairsOf(scheme)) {
                assertNotEquals(Color.Unspecified, on, "$label on$role")
                assertNotEquals(Color.Unspecified, container, "$label $role")
            }
        }
    }

    @Test
    fun nothingIsDrawnOnItsOwnColour() {
        for ((label, scheme) in listOf("light" to LightColorScheme, "dark" to DarkColorScheme)) {
            for ((role, on, container) in pairsOf(scheme)) {
                assertNotEquals(container, on, "$label on$role is the same as $role")
            }
        }
    }

    @Test
    fun everyForegroundContrastsWithItsBackground() {
        // Not a full WCAG check — a coarse guard against a pairing that reads as
        // one flat block, which is how a copy-paste slip between the two schemes
        // usually shows up.
        for ((label, scheme) in listOf("light" to LightColorScheme, "dark" to DarkColorScheme)) {
            for ((role, on, container) in pairsOf(scheme)) {
                val gap = kotlin.math.abs(on.luminance() - container.luminance())
                assertTrue(gap > 0.15f, "$label on$role contrast is only $gap")
            }
        }
    }

    @Test
    fun theDarkSchemeIsDarkerThanTheLightOne() {
        // Guards the two being swapped, which shows as a white flash on a phone
        // in a dark hall rather than as anything the compiler would catch.
        assertTrue(DarkColorScheme.background.luminance() < LightColorScheme.background.luminance())
        assertTrue(DarkColorScheme.surface.luminance() < LightColorScheme.surface.luminance())
        assertTrue(DarkColorScheme.onBackground.luminance() > LightColorScheme.onBackground.luminance())
    }

    @Test
    fun theTwoSchemesAreDistinct() {
        assertNotEquals(LightColorScheme.primary, DarkColorScheme.primary)
        assertNotEquals(LightColorScheme.background, DarkColorScheme.background)
    }

    @Test
    fun eachSchemeAgreesWithTheAppPaletteForItsMode() {
        // The two are provided together by AppTheme; if Material said "light"
        // while LocalAppColors said "dark", every screen would be half-styled.
        assertTrue(
            DarkColorScheme.background.luminance() < 0.5f &&
                appColorsFor(isDark = true).background.luminance() < 0.5f,
        )
        assertTrue(
            LightColorScheme.background.luminance() > 0.5f &&
                appColorsFor(isDark = false).background.luminance() > 0.5f,
        )
    }

    @Test
    fun errorIsTellableFromPrimaryInBothSchemes() {
        // Destructive actions are distinguished by colour alone in several places.
        assertNotEquals(LightColorScheme.error, LightColorScheme.primary)
        assertNotEquals(DarkColorScheme.error, DarkColorScheme.primary)
    }

    @Test
    fun everySchemeColourIsFullyOpaque() {
        // Material draws these as solid fills; a translucent one lets whatever is
        // behind a card bleed through it.
        for ((label, scheme) in listOf("light" to LightColorScheme, "dark" to DarkColorScheme)) {
            for ((role, on, container) in pairsOf(scheme)) {
                assertEquals(1f, on.alpha, "$label on$role")
                assertEquals(1f, container.alpha, "$label $role")
            }
        }
    }
}

/** Perceived brightness, for asserting a scheme is the one it claims to be. */
private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
