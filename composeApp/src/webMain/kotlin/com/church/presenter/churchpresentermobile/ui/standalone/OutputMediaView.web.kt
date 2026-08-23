package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Unreachable on web: `supportsStandalone` is false there, so the browser build
 * never drives an output of its own. Present so the shared standalone code
 * compiles for every target.
 */
@Composable
actual fun OutputWebView(url: String, modifier: Modifier) {
    Box(modifier.fillMaxSize().background(Color.Black))
}

@Composable
actual fun OutputVideoView(url: String, modifier: Modifier) {
    Box(modifier.fillMaxSize().background(Color.Black))
}
