package com.church.presenter.churchpresentermobile.model

actual val supportsStandalone: Boolean = true

actual val supportsEmbeddedServer: Boolean = true

actual val supportsExternalDisplay: Boolean = true

actual val isForegroundOnlyServer: Boolean = true

actual val mirroringRoute: MirroringRoute = MirroringRoute.AIRPLAY

// There is no public API to open Control Centre, and the `App-Prefs:` scheme is
// private — reaching for it risks an App Store rejection for a shortcut to a
// panel the operator can already swipe to. iOS gets the instructions only.
actual val canOpenMirroringSettings: Boolean = false

actual fun openMirroringSettings() = Unit
