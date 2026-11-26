package com.church.presenter.churchpresentermobile.present

/**
 * Maps a video's served address back to the file on this phone, for the players
 * that run on the phone itself — its preview and a screen on its HDMI port or
 * mirrored to a TV.
 *
 * A video slide carries the server's http address so a TV or a browser can
 * reach it. A player on the phone reading that address streams the file to
 * itself over Wi-Fi: every seek stalls to rebuffer, and on a Galaxy S24 the
 * preview fell behind, seeked, stalled and seeked again every two seconds. Read
 * from the file, a seek is instant.
 *
 * A process-wide hook, like [SlideBus], because the in-process display renders
 * outside `App()`'s Compose tree. Left at its default on a device with no such
 * file — the TV app — so the address is used as it came.
 */
object LocalVideoFiles {
    /** The playable local address for [url], or null to play [url] itself. */
    var resolve: (url: String) -> String? = { null }

    /** [url], or this phone's own copy of it. */
    fun playable(url: String): String = resolve(url) ?: url
}
