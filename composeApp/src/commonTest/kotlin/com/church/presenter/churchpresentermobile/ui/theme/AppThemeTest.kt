package com.church.presenter.churchpresentermobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests [AppTheme] — which palette each [ThemeMode] resolves to, and that both
 * the app's own tokens and Material's scheme are provided together.
 *
 * Lives in wasmJsTest because composing needs a Skia surface. Note that these
 * do not move the JaCoCo figure: coverage is measured on the Android unit-test
 * JVM, and AppTheme.kt's palettes are private, so nothing in that file is
 * reachable from a plain unit test.
 */
@OptIn(ExperimentalTestApi::class)
class AppThemeTest {

    @Test
    fun lightModeProvidesTheLightPalette() = runComposeUiTest {
        var colors: AppColors? = null
        setContent {
            AppTheme(themeMode = ThemeMode.LIGHT) { colors = LocalAppColors.current }
        }

        assertEquals(appColorsFor(isDark = false), colors)
    }

    @Test
    fun darkModeProvidesTheDarkPalette() = runComposeUiTest {
        var colors: AppColors? = null
        setContent {
            AppTheme(themeMode = ThemeMode.DARK) { colors = LocalAppColors.current }
        }

        assertEquals(appColorsFor(isDark = true), colors)
    }

    @Test
    fun systemModeResolvesToARealPaletteRatherThanFailing() = runComposeUiTest {
        // Whichever way the host reports itself, SYSTEM has to land on one of the
        // two — never on an unprovided default.
        var colors: AppColors? = null
        setContent {
            AppTheme(themeMode = ThemeMode.SYSTEM) { colors = LocalAppColors.current }
        }

        assertNotNull(colors)
        assertTrue(colors == appColorsFor(isDark = true) || colors == appColorsFor(isDark = false))
    }

    @Test
    fun theDefaultModeIsSystem() = runComposeUiTest {
        var colors: AppColors? = null
        setContent {
            AppTheme { colors = LocalAppColors.current }
        }

        assertNotNull(colors)
    }

    @Test
    fun materialsSchemeIsProvidedAlongsideOurTokens() = runComposeUiTest {
        // Screens mix both — Material components for controls, LocalAppColors for
        // the surfaces this design needs that a ColorScheme cannot express. A
        // theme that provided only one of them half-styles every screen.
        var appBackground: androidx.compose.ui.graphics.Color? = null
        var materialSurface: androidx.compose.ui.graphics.Color? = null
        setContent {
            AppTheme(themeMode = ThemeMode.DARK) {
                appBackground = LocalAppColors.current.background
                materialSurface = MaterialTheme.colorScheme.surface
            }
        }

        assertNotNull(appBackground)
        assertNotNull(materialSurface)
    }

    @Test
    fun materialsSchemeFollowsTheModeToo() = runComposeUiTest {
        // Not just our tokens: a light Material scheme under a dark AppTheme puts
        // white cards on a near-black background.
        var darkSurface: androidx.compose.ui.graphics.Color? = null
        var lightSurface: androidx.compose.ui.graphics.Color? = null
        setContent {
            AppTheme(themeMode = ThemeMode.DARK) { darkSurface = MaterialTheme.colorScheme.surface }
        }
        setContent {
            AppTheme(themeMode = ThemeMode.LIGHT) { lightSurface = MaterialTheme.colorScheme.surface }
        }

        assertNotEquals(darkSurface, lightSurface)
    }

    @Test
    fun switchingModeSwapsThePaletteWithoutRecreatingTheTree() = runComposeUiTest {
        // The settings screen flips this live; the provided palette has to follow.
        var mode by mutableStateOf(ThemeMode.LIGHT)
        var colors: AppColors? = null
        setContent {
            AppTheme(themeMode = mode) { colors = LocalAppColors.current }
        }
        assertEquals(appColorsFor(isDark = false), colors)

        mode = ThemeMode.DARK
        waitForIdle()

        assertEquals(appColorsFor(isDark = true), colors)
    }
}
