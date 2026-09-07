package com.church.presenter.churchpresentermobile.model

// The JVM target is a test host, not a shipping app: standalone presenting is
// off, so nothing downstream of these is ever reached.
actual val supportsStandalone: Boolean = false
actual val supportsEmbeddedServer: Boolean = false
actual val supportsExternalDisplay: Boolean = false
actual val isForegroundOnlyServer: Boolean = false
actual val mirroringRoute: MirroringRoute = MirroringRoute.NONE
actual val canOpenMirroringSettings: Boolean = false
actual fun openMirroringSettings() = Unit
