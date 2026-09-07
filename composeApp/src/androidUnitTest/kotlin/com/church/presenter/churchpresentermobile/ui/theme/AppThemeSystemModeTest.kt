package com.church.presenter.churchpresentermobile.ui.theme

import android.content.res.Configuration
import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.platform.LocalConfiguration
import com.church.presenter.churchpresentermobile.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers the one branch of [AppTheme] the common test cannot reach:
 * [ThemeMode.SYSTEM], which asks the device what it is set to.
 *
 * On Android `isSystemInDarkTheme()` reads `LocalConfiguration`, so the device
 * setting can simply be provided — no emulator, and no Robolectric. Android-only
 * because `LocalConfiguration` is; the rest of AppTheme is tested in commonTest.
 */
class AppThemeSystemModeTest {

    private object ImmediateFrameClock : MonotonicFrameClock {
        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(0L)
    }

    private class NoOpApplier : Applier<Unit> {
        override val current: Unit = Unit
        override fun down(node: Unit) = Unit
        override fun up() = Unit
        override fun insertTopDown(index: Int, instance: Unit) = Unit
        override fun insertBottomUp(index: Int, instance: Unit) = Unit
        override fun remove(index: Int, count: Int) = Unit
        override fun move(from: Int, to: Int, count: Int) = Unit
        override fun clear() = Unit
    }

    private suspend fun compose(content: @Composable () -> Unit) {
        val job = Job(coroutineContext[Job])
        val scope = CoroutineScope(coroutineContext + job + ImmediateFrameClock)
        val recomposer = Recomposer(scope.coroutineContext)
        scope.launch { recomposer.runRecomposeAndApplyChanges() }
        val composition = Composition(NoOpApplier(), recomposer)
        try {
            composition.setContent(content)
        } finally {
            composition.dispose()
            recomposer.close()
            scope.cancel()
        }
    }

    /** A device configuration reporting night mode on or off. */
    private fun configuration(night: Boolean) = Configuration().apply {
        uiMode = if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    }

    private suspend fun paletteUnderSystem(night: Boolean): AppColors? {
        var seen: AppColors? = null
        compose {
            CompositionLocalProvider(LocalConfiguration provides configuration(night)) {
                AppTheme(themeMode = ThemeMode.SYSTEM) { seen = LocalAppColors.current }
            }
        }
        return seen
    }

    @Test
    fun systemFollowsTheDeviceIntoDarkMode() = runTest {
        assertEquals(appColorsFor(isDark = true), paletteUnderSystem(night = true))
    }

    @Test
    fun systemFollowsTheDeviceIntoLightMode() = runTest {
        assertEquals(appColorsFor(isDark = false), paletteUnderSystem(night = false))
    }

    @Test
    fun anExplicitChoiceOverridesTheDeviceSetting() = runTest {
        // The settings screen lets a church pin a mode; the device being in dark
        // mode must not drag a pinned light theme back.
        var seen: AppColors? = null
        compose {
            CompositionLocalProvider(LocalConfiguration provides configuration(night = true)) {
                AppTheme(themeMode = ThemeMode.LIGHT) { seen = LocalAppColors.current }
            }
        }

        assertEquals(appColorsFor(isDark = false), seen)
    }

    @Test
    fun theDefaultModeIsSystem() = runTest {
        // Called with no themeMode at all, as App.kt does before settings load.
        var seen: AppColors? = null
        compose {
            CompositionLocalProvider(LocalConfiguration provides configuration(night = true)) {
                AppTheme { seen = LocalAppColors.current }
            }
        }

        assertEquals(appColorsFor(isDark = true), seen)
    }
}
