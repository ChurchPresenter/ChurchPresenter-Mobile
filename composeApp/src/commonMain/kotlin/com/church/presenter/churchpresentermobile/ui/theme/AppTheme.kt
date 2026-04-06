package com.church.presenter.churchpresentermobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.church.presenter.churchpresentermobile.model.ThemeMode

// ── Light palette ─────────────────────────────────────────────────────────────

private val md_light_primary = Color(0xFF3D5A80)
private val md_light_onPrimary = Color(0xFFFFFFFF)
private val md_light_primaryContainer = Color(0xFFD1E4FF)
private val md_light_onPrimaryContainer = Color(0xFF001D36)
private val md_light_secondary = Color(0xFF7B6F52)
private val md_light_onSecondary = Color(0xFFFFFFFF)
private val md_light_secondaryContainer = Color(0xFFEED9AD)
private val md_light_onSecondaryContainer = Color(0xFF271904)
private val md_light_tertiary = Color(0xFF5E5C71)
private val md_light_onTertiary = Color(0xFFFFFFFF)
private val md_light_tertiaryContainer = Color(0xFFE4DFF9)
private val md_light_onTertiaryContainer = Color(0xFF1B192B)
private val md_light_background = Color(0xFFFAFBFF)
private val md_light_onBackground = Color(0xFF1A1C1E)
private val md_light_surface = Color(0xFFFAFBFF)
private val md_light_onSurface = Color(0xFF1A1C1E)
private val md_light_surfaceVariant = Color(0xFFDFE2EB)
private val md_light_onSurfaceVariant = Color(0xFF43474E)
private val md_light_error = Color(0xFFBA1A1A)
private val md_light_onError = Color(0xFFFFFFFF)
private val md_light_errorContainer = Color(0xFFFFDAD6)
private val md_light_onErrorContainer = Color(0xFF410002)
private val md_light_outline = Color(0xFF73777F)
private val md_light_outlineVariant = Color(0xFFC3C7CF)
private val md_light_inverseSurface = Color(0xFF2F3033)
private val md_light_inverseOnSurface = Color(0xFFF1F0F4)
private val md_light_inversePrimary = Color(0xFF9EC3F5)

// ── Dark palette ──────────────────────────────────────────────────────────────

private val md_dark_primary = Color(0xFF9EC3F5)
private val md_dark_onPrimary = Color(0xFF003259)
private val md_dark_primaryContainer = Color(0xFF1E4976)
private val md_dark_onPrimaryContainer = Color(0xFFD1E4FF)
private val md_dark_secondary = Color(0xFFD4BC8A)
private val md_dark_onSecondary = Color(0xFF3E2E15)
private val md_dark_secondaryContainer = Color(0xFF59432A)
private val md_dark_onSecondaryContainer = Color(0xFFEED9AD)
private val md_dark_tertiary = Color(0xFFC8C3DD)
private val md_dark_onTertiary = Color(0xFF302E42)
private val md_dark_tertiaryContainer = Color(0xFF474459)
private val md_dark_onTertiaryContainer = Color(0xFFE4DFF9)
private val md_dark_background = Color(0xFF1A1C1E)
private val md_dark_onBackground = Color(0xFFE2E2E6)
private val md_dark_surface = Color(0xFF1A1C1E)
private val md_dark_onSurface = Color(0xFFE2E2E6)
private val md_dark_surfaceVariant = Color(0xFF43474E)
private val md_dark_onSurfaceVariant = Color(0xFFC3C7CF)
private val md_dark_error = Color(0xFFFFB4AB)
private val md_dark_onError = Color(0xFF690005)
private val md_dark_errorContainer = Color(0xFF93000A)
private val md_dark_onErrorContainer = Color(0xFFFFDAD6)
private val md_dark_outline = Color(0xFF8D9199)
private val md_dark_outlineVariant = Color(0xFF43474E)
private val md_dark_inverseSurface = Color(0xFFE2E2E6)
private val md_dark_inverseOnSurface = Color(0xFF2F3033)
private val md_dark_inversePrimary = Color(0xFF3D5A80)

// ── Colour schemes ────────────────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary = md_light_primary,
    onPrimary = md_light_onPrimary,
    primaryContainer = md_light_primaryContainer,
    onPrimaryContainer = md_light_onPrimaryContainer,
    secondary = md_light_secondary,
    onSecondary = md_light_onSecondary,
    secondaryContainer = md_light_secondaryContainer,
    onSecondaryContainer = md_light_onSecondaryContainer,
    tertiary = md_light_tertiary,
    onTertiary = md_light_onTertiary,
    tertiaryContainer = md_light_tertiaryContainer,
    onTertiaryContainer = md_light_onTertiaryContainer,
    background = md_light_background,
    onBackground = md_light_onBackground,
    surface = md_light_surface,
    onSurface = md_light_onSurface,
    surfaceVariant = md_light_surfaceVariant,
    onSurfaceVariant = md_light_onSurfaceVariant,
    error = md_light_error,
    onError = md_light_onError,
    errorContainer = md_light_errorContainer,
    onErrorContainer = md_light_onErrorContainer,
    outline = md_light_outline,
    outlineVariant = md_light_outlineVariant,
    inverseSurface = md_light_inverseSurface,
    inverseOnSurface = md_light_inverseOnSurface,
    inversePrimary = md_light_inversePrimary,
)

private val DarkColorScheme = darkColorScheme(
    primary = md_dark_primary,
    onPrimary = md_dark_onPrimary,
    primaryContainer = md_dark_primaryContainer,
    onPrimaryContainer = md_dark_onPrimaryContainer,
    secondary = md_dark_secondary,
    onSecondary = md_dark_onSecondary,
    secondaryContainer = md_dark_secondaryContainer,
    onSecondaryContainer = md_dark_onSecondaryContainer,
    tertiary = md_dark_tertiary,
    onTertiary = md_dark_onTertiary,
    tertiaryContainer = md_dark_tertiaryContainer,
    onTertiaryContainer = md_dark_onTertiaryContainer,
    background = md_dark_background,
    onBackground = md_dark_onBackground,
    surface = md_dark_surface,
    onSurface = md_dark_onSurface,
    surfaceVariant = md_dark_surfaceVariant,
    onSurfaceVariant = md_dark_onSurfaceVariant,
    error = md_dark_error,
    onError = md_dark_onError,
    errorContainer = md_dark_errorContainer,
    onErrorContainer = md_dark_onErrorContainer,
    outline = md_dark_outline,
    outlineVariant = md_dark_outlineVariant,
    inverseSurface = md_dark_inverseSurface,
    inverseOnSurface = md_dark_inverseOnSurface,
    inversePrimary = md_dark_inversePrimary,
)

// ── Theme entry point ─────────────────────────────────────────────────────────

/**
 * Root theme composable for the Church Presenter app.
 *
 * Applies [LightColorScheme] or [DarkColorScheme] based on [themeMode].
 * When [ThemeMode.SYSTEM] is set the device's current setting is used.
 *
 * @param themeMode The user's preferred colour scheme.
 * @param content The composable content to be themed.
 */
@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (useDark) DarkColorScheme else LightColorScheme,
        content = content
    )
}

