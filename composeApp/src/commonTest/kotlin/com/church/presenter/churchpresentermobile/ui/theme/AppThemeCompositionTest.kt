package com.church.presenter.churchpresentermobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
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
 * Composes [AppTheme] without a UI toolkit.
 *
 * [AppTheme] emits no layout nodes — it provides composition locals and wraps its
 * content — so it can be composed against a no-op [Applier] on any platform. That
 * matters because the Compose UI tests for it live in `wasmJsTest` (they need a
 * Skia surface), and coverage is measured on the Android unit-test JVM, which
 * never sees them. This exercises the same code where the measurement happens.
 *
 * The technique only works for node-free composables. Anything that lays out or
 * draws still belongs in `wasmJsTest` with `runComposeUiTest`.
 */
class AppThemeCompositionTest {

    /** A frame clock that fires immediately: nothing here animates. */
    private object ImmediateFrameClock : MonotonicFrameClock {
        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(0L)
    }

    /** Composes [content] once and returns, tearing the composition down after. */
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

    /** Accepts and discards every node; [AppTheme] emits none, so nothing is lost. */
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

    @Test
    fun lightModeProvidesTheLightPalette() = runTest {
        var seen: AppColors? = null
        compose { AppTheme(themeMode = ThemeMode.LIGHT) { seen = LocalAppColors.current } }

        assertEquals(appColorsFor(isDark = false), seen)
    }

    @Test
    fun darkModeProvidesTheDarkPalette() = runTest {
        var seen: AppColors? = null
        compose { AppTheme(themeMode = ThemeMode.DARK) { seen = LocalAppColors.current } }

        assertEquals(appColorsFor(isDark = true), seen)
    }

    @Test
    fun materialsSchemeFollowsTheSameMode() = runTest {
        // A light Material scheme under a dark AppTheme would put white cards on
        // a near-black background.
        var dark: androidx.compose.material3.ColorScheme? = null
        var light: androidx.compose.material3.ColorScheme? = null
        compose { AppTheme(themeMode = ThemeMode.DARK) { dark = MaterialTheme.colorScheme } }
        compose { AppTheme(themeMode = ThemeMode.LIGHT) { light = MaterialTheme.colorScheme } }

        assertEquals(DarkColorScheme.background, dark?.background)
        assertEquals(LightColorScheme.background, light?.background)
    }

    @Test
    fun theContentIsActuallyComposed() = runTest {
        // Guards the wrapper silently dropping its content, which would render a
        // blank app rather than fail.
        var ran = 0
        compose { AppTheme(themeMode = ThemeMode.DARK) { ran++ } }

        assertEquals(1, ran)
    }

    // ThemeMode.SYSTEM is deliberately not exercised here: it calls
    // isSystemInDarkTheme(), which needs a host to ask about the device setting
    // and throws without one. That branch is covered by AppThemeTest in
    // wasmJsTest, where the browser can answer.
}
