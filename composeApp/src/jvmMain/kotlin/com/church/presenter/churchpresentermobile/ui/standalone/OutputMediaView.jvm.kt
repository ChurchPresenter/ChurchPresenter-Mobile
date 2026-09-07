package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun OutputWebView(url: String, modifier: Modifier) {
    Box(modifier.fillMaxSize().background(Color.Black))
}

@Composable
actual fun OutputVideoView(url: String, modifier: Modifier) {
    Box(modifier.fillMaxSize().background(Color.Black))
}
