package com.church.presenter.churchpresentermobile.present

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Nothing on the web build: it has no embedded server (see LocalWebServer.web), so a
 * canvas frame would have nowhere to be served from and no screen to reach.
 */
actual fun encodeCanvasFrame(bitmap: ImageBitmap, maxEdge: Int): ByteArray? = null
