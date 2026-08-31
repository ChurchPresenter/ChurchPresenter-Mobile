package com.church.presenter.churchpresentermobile.model

/**
 * Per-platform gate for standalone presenter features.
 *
 * Standalone mode needs an output sink, and every sink we can build needs
 * platform APIs a browser does not have (a second display, or a listening
 * socket). So the js/wasmJs targets compile all of the standalone code but
 * report `false` here, and [AppSettings.appMode] coerces to [AppMode.REMOTE]
 * whenever [supportsStandalone] is false. That keeps a settings blob written on
 * a phone from putting the web build into a mode it cannot honour.
 */
expect val supportsStandalone: Boolean

/** True when the platform can bind a local HTTP/WebSocket server socket. */
expect val supportsEmbeddedServer: Boolean

/** True when the platform can render onto a second, physically separate display. */
expect val supportsExternalDisplay: Boolean

/**
 * True where the embedded server only runs while the app is in the foreground.
 *
 * iOS gives no supported way to keep a listening socket alive once the app is
 * suspended, so the UI has to tell the operator plainly to leave the app open
 * rather than let them discover it when the screen goes blank mid-service.
 * Android can genuinely serve from the background via a foreground service.
 */
expect val isForegroundOnlyServer: Boolean

/**
 * How the operator starts system screen mirroring on this platform.
 *
 * Both AirPlay and Chromecast reach [present.sink.ExternalDisplaySink] the same
 * way — the OS mirrors, then hands the app a second display — but the operator
 * gets there through completely different system UI, and neither can be started
 * from inside the app. So the Screens sheet has to name the right one.
 */
enum class MirroringRoute {
    /** Control Centre → Screen Mirroring → Apple TV. */
    AIRPLAY,

    /** Quick settings → Cast / Screen cast → Chromecast. */
    CAST,

    /** No system mirroring worth pointing the operator at. */
    NONE,
}

expect val mirroringRoute: MirroringRoute

/**
 * True where the app can deep-link the operator to the OS mirroring picker.
 *
 * Android has a public settings intent for this; iOS has no supported way to
 * open Control Centre, so it gets the instructions without the shortcut.
 */
expect val canOpenMirroringSettings: Boolean

/** Opens the OS mirroring picker. No-op where [canOpenMirroringSettings] is false. */
expect fun openMirroringSettings()
