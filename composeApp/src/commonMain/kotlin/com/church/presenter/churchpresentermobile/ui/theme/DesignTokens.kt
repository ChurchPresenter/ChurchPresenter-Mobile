package com.church.presenter.churchpresentermobile.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design tokens for the Church Presenter redesign.
 *
 * These mirror the values in the design handoff (README "Design Tokens" section)
 * exactly. They are exposed through [LocalAppColors] so every screen can pull the
 * precise dark/light value without going through Material's [androidx.compose.material3.ColorScheme],
 * which cannot express the semi-transparent surface/border layers this design relies on.
 */
@Immutable
data class AppColors(
    val isDark: Boolean,
    // Backgrounds
    val background: Color,
    val splashBackground: Brush,
    // Surfaces (cards / chips) — two elevations
    val surface: Color,
    val surfaceStrong: Color,
    // Elevated neutral surface (Select FAB, segmented active pill)
    val surfaceElevated: Color,
    // Opaque surface for modals/sheets that float over a scrim (must not be translucent)
    val sheetBackground: Color,
    // Input fill (search field, settings field cards)
    val inputBg: Color,
    // Borders / hairlines — three weights
    val borderSubtle: Color,
    val border: Color,
    val borderStrong: Color,
    // Scrim behind sheets / drawers
    val scrim: Color,
    // Text
    val text: Color,
    val secondary: Color,
    val muted: Color,
    val dim: Color,
    // Accent (primary / project)
    val accent: Color,
    val onAccent: Color,
    val accentTint: Color,
    val accentTintStrong: Color,
    // Add-to-schedule amber + its dark icon-stroke color
    val amber: Color,
    val amberStroke: Color,
    // Danger / deny
    val danger: Color,
    // Splash tagline + cross
    val crossBrush: Brush,
    val taglineWorship: Color,
    val taglinePresent: Color,
    val taglineConnect: Color,
    val taglineDot: Color,
    val splashTitle: Color,
    val splashGlow: Color,
    // Schedule item type colors
    val scheduleSongFg: Color,
    val scheduleSongBg: Color,
    val scheduleBibleFg: Color,
    val scheduleBibleBg: Color,
    val schedulePictureFg: Color,
    val schedulePictureBg: Color,
)

private val DarkAppColors = AppColors(
    isDark = true,
    background = Color(0xFF0E0E17),
    splashBackground = Brush.verticalGradient(listOf(Color(0xFF09090B), Color(0xFF09090B))),
    surface = Color(0x0DFFFFFF),          // rgba(255,255,255,.05)
    surfaceStrong = Color(0x14FFFFFF),    // rgba(255,255,255,.08)
    surfaceElevated = Color(0xFF2B2B3D),
    sheetBackground = Color(0xFF17171F),  // opaque elevated bg for modals/sheets
    inputBg = Color(0x0DFFFFFF),          // rgba(255,255,255,.05)
    borderSubtle = Color(0x0FFFFFFF),     // rgba(255,255,255,.06)
    border = Color(0x14FFFFFF),           // rgba(255,255,255,.08)
    borderStrong = Color(0x21FFFFFF),     // rgba(255,255,255,.13)
    scrim = Color(0x8C000000),            // rgba(0,0,0,.55)
    text = Color(0xFFFAFAFA),
    secondary = Color(0xFFD4D4D8),
    muted = Color(0xFF71717A),
    dim = Color(0xFF3F3F46),
    accent = Color(0xFF86EFAC),
    onAccent = Color(0xFF09090B),
    accentTint = Color(0x1A86EFAC),       // rgba(134,239,172,.10)
    accentTintStrong = Color(0x2686EFAC), // rgba(134,239,172,.15)
    amber = Color(0xFFD4A853),
    amberStroke = Color(0xFF4A3A12),
    danger = Color(0xFFEF4444),
    crossBrush = Brush.verticalGradient(listOf(Color(0xFFD4FCE8), Color(0xFF52C98E))),
    taglineWorship = Color(0xFF86EFAC),
    taglinePresent = Color(0xFF7DD3FC),
    taglineConnect = Color(0xFFE0B968),
    taglineDot = Color(0xFF3F3F46),
    splashTitle = Color(0xFFFAFAFA),
    splashGlow = Color(0x2E86EFAC),       // rgba(134,239,172,.18)
    scheduleSongFg = Color(0xFF818CF8),
    scheduleSongBg = Color(0xFF1E1B4B),
    scheduleBibleFg = Color(0xFF60A5FA),
    scheduleBibleBg = Color(0xFF172554),
    schedulePictureFg = Color(0xFF34D399),
    schedulePictureBg = Color(0xFF052E16),
)

private val LightAppColors = AppColors(
    isDark = false,
    background = Color(0xFFF5F5F3),
    splashBackground = Brush.verticalGradient(
        listOf(Color(0xFFC2D5F5), Color(0xFFDDEAF9), Color(0xFFF0F5FC))
    ),
    surface = Color(0xFFFFFFFF),
    surfaceStrong = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFFFFFFF),
    sheetBackground = Color(0xFFFFFFFF),
    inputBg = Color(0xFFF0F0EE),
    borderSubtle = Color(0x0F000000),     // rgba(0,0,0,.06)
    border = Color(0x14000000),           // rgba(0,0,0,.08)
    borderStrong = Color(0x24000000),     // rgba(0,0,0,.14)
    scrim = Color(0x59000000),            // rgba(0,0,0,.35)
    text = Color(0xFF18181B),
    secondary = Color(0xFF3F3F46),
    muted = Color(0xFF71717A),
    dim = Color(0xFFA1A1AA),
    accent = Color(0xFF16A34A),
    onAccent = Color(0xFFFFFFFF),
    accentTint = Color(0x1416A34A),       // rgba(22,163,74,.08)
    accentTintStrong = Color(0x1F16A34A), // rgba(22,163,74,.12)
    amber = Color(0xFFE0A94F),
    amberStroke = Color(0xFF4A3410),
    danger = Color(0xFFEF4444),
    crossBrush = Brush.verticalGradient(listOf(Color(0xFF5072D8), Color(0xFF17307E))),
    taglineWorship = Color(0xFF1E3A8A),
    taglinePresent = Color(0xFF2563EB),
    taglineConnect = Color(0xFFB4781E),
    taglineDot = Color(0x591E3A8A),       // rgba(30,58,138,.35)
    splashTitle = Color(0xFF1E3A8A),
    splashGlow = Color(0x66FFFFFF),       // soft neutral halo (drop-shadow lift)
    scheduleSongFg = Color(0xFF6366F1),
    scheduleSongBg = Color(0xFFE0E7FF),
    scheduleBibleFg = Color(0xFF2563EB),
    scheduleBibleBg = Color(0xFFDBEAFE),
    schedulePictureFg = Color(0xFF059669),
    schedulePictureBg = Color(0xFFD1FAE5),
)

fun appColorsFor(isDark: Boolean): AppColors = if (isDark) DarkAppColors else LightAppColors

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

/** Shared corner radii and spacing from the design token scale. */
object AppDimens {
    val radiusCard: Dp = 14.dp
    val radiusTile: Dp = 11.dp        // number chip, icon tiles
    val radiusChip: Dp = 10.dp
    val radiusSearch: Dp = 12.dp
    val radiusButton: Dp = 8.dp
    val radiusFab: Dp = 14.dp
    val radiusPill: Dp = 20.dp
    val radiusSheet: Dp = 24.dp

    val space4: Dp = 4.dp
    val space8: Dp = 8.dp
    val space12: Dp = 12.dp
    val space14: Dp = 14.dp
    val space16: Dp = 16.dp
    val space20: Dp = 20.dp
    val space24: Dp = 24.dp

    val tabBarHeight: Dp = 72.dp
    val fabSize: Dp = 50.dp
}
