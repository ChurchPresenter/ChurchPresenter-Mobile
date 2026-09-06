package com.church.presenter.churchpresentermobile.present

/** No-op: the JVM test host has no embedded server, so it never serves a photo. */
actual fun downscaleImage(bytes: ByteArray, maxEdge: Int): ByteArray = bytes
