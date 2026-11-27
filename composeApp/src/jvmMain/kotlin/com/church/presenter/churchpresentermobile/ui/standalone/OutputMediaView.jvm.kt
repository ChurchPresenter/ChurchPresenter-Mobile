package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.church.presenter.churchpresentermobile.model.VideoCue

@Composable
actual fun OutputWebView(url: String, modifier: Modifier) {
    Box(modifier.fillMaxSize().background(Color.Black))
}

@Composable
actual fun OutputVideoView(url: String, cue: VideoCue?, modifier: Modifier, muted: Boolean, soundOnDevice: Boolean) {
    Box(modifier.fillMaxSize().background(Color.Black))
}
