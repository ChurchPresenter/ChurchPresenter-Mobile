package com.church.presenter.churchpresentermobile.present

/**
 * No-op on the web build.
 *
 * Shrinking an image in a browser means a canvas round-trip, which is only worth
 * writing where it pays for itself. It does not here: the browser build has no
 * embedded server (see LocalWebServer.web) and so never serves a photo to
 * another device — the bytes stay in the tab that picked them.
 */
actual fun downscaleImage(bytes: ByteArray, maxEdge: Int): ByteArray = bytes
