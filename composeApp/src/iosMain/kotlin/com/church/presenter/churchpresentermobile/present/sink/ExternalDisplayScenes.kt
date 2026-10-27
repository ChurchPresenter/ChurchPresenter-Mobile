package com.church.presenter.churchpresentermobile.present.sink

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.UIKit.UIWindowScene

/**
 * The external-display scene iOS hands the app when an AirPlay or wired screen
 * connects. Fed from Swift by `ExternalDisplaySceneDelegate` in `iOSApp.swift`.
 *
 * The app is scene-based (SwiftUI `App` lifecycle), so iOS offers an external
 * screen as a `UIWindowScene` with the `windowExternalDisplayNonInteractive`
 * role, and a window only shows there if it is created on that scene. The scene
 * can connect before standalone mode starts, so it is held here as state for
 * [IosExternalDisplaySink] to pick up whenever it attaches.
 */
object ExternalDisplayScenes {

    private val _scene = MutableStateFlow<UIWindowScene?>(null)

    /** The connected external scene, or null when no screen is attached. */
    internal val scene: StateFlow<UIWindowScene?> = _scene.asStateFlow()

    /** Called from Swift when iOS connects an external-display scene. */
    fun connect(scene: UIWindowScene) {
        _scene.value = scene
    }

    /** Called from Swift when that scene goes away. */
    fun disconnect(scene: UIWindowScene) {
        if (_scene.value == scene) _scene.value = null
    }
}
