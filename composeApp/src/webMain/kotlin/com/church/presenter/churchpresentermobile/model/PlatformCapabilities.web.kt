package com.church.presenter.churchpresentermobile.model

// A browser tab cannot bind a listening socket and cannot drive a second
// display, so the web build stays remote-control-only. Declared once on the
// shared webMain source set — it satisfies the expect for both js and wasmJs.

actual val supportsStandalone: Boolean = false

actual val supportsEmbeddedServer: Boolean = false

actual val supportsExternalDisplay: Boolean = false

actual val isForegroundOnlyServer: Boolean = false
