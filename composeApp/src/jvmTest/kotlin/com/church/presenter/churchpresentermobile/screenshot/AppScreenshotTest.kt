package com.church.presenter.churchpresentermobile.screenshot

import com.church.presenter.churchpresentermobile.App
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import kotlin.test.Test

/**
 * The whole app, from its own entry point.
 *
 * Everything else in this suite photographs a screen with its arguments set by
 * hand. This one composes [App] itself, which means the real settings, the real
 * mode holder and the real tab strip decide what is on screen — the first frame
 * of a cold launch, as it actually assembles.
 *
 * It is the only capture in the suite that would notice a change to the app's
 * own wiring rather than to a single screen.
 *
 * [App] applies its own [AppTheme][com.church.presenter.churchpresentermobile.ui.theme.AppTheme]
 * from the saved settings, so the harness's theme is overridden — and a fresh
 * install is [ThemeMode.SYSTEM], which reads as light on the test JVM. Each
 * theme is therefore captured on its own, with the settings seeded to match;
 * otherwise the dark golden is a second copy of the light one.
 */
class AppScreenshotTest {

    @Test
    fun coldLaunch() = eachTheme { theme ->
        screenshot("app__cold-launch", themes = listOf(theme)) { App(appSettings = settingsIn(theme)) }
    }

    @Test
    fun coldLaunchOnATablet() = eachTheme { theme ->
        screenshot(
            "app__tablet",
            width = Screenshots.TABLET_WIDTH,
            themes = listOf(theme),
        ) { App(appSettings = settingsIn(theme)) }
    }

    private fun eachTheme(capture: (ThemeMode) -> Unit) =
        listOf(ThemeMode.LIGHT, ThemeMode.DARK).forEach(capture)

    /** A fresh install whose only saved preference is [theme]. */
    private fun settingsIn(theme: ThemeMode) =
        AppSettings(InMemorySettingsStorage()).apply { themeMode = theme }
}
