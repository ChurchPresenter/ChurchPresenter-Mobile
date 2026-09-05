package com.church.presenter.churchpresentermobile

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.present.SlideBus
import com.church.presenter.churchpresentermobile.ui.standalone.StandaloneOutputScreen
import platform.UIKit.UIViewController

/**
 * The Compose content shown on a connected external screen.
 *
 * Reads [SlideBus] rather than taking parameters: this controller is created by
 * platform code outside the app's Compose tree, so there is no composition to
 * pass state down through. Collecting a StateFlow also means a screen connected
 * mid-service immediately shows whatever is already projected.
 */
fun ExternalDisplayViewController(): UIViewController = ComposeUIViewController {
    val envelope by SlideBus.current.collectAsState()
    StandaloneOutputScreen(slide = envelope.slide ?: Slide.BLANK)
}
