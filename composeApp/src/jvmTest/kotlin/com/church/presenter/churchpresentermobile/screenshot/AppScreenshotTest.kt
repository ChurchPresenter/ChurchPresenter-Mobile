package com.church.presenter.churchpresentermobile.screenshot

import com.church.presenter.churchpresentermobile.App
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
 */
class AppScreenshotTest {

    @Test
    fun coldLaunch() = screenshot("app__cold-launch") { App() }

    @Test
    fun coldLaunchOnATablet() = screenshot(
        "app__tablet",
        width = Screenshots.TABLET_WIDTH,
    ) { App() }
}
