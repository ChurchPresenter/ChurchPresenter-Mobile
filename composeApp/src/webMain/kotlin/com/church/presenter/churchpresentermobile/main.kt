package com.church.presenter.churchpresentermobile

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val body = document.body ?: return
    // Hide the static loading spinner once Compose takes over
    document.getElementById("loading")?.let {
        (it as? org.w3c.dom.HTMLElement)?.style?.display = "none"
    }
    ComposeViewport(body) {
        App()
    }
}