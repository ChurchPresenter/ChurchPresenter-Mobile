package com.church.presenter.churchpresentermobile.present

/**
 * Keeps the phone able to serve while it is presenting.
 *
 * What that means differs sharply per platform, which is the point of the seam:
 * Android can genuinely keep a server alive in the background given a foreground
 * service and the right locks, whereas iOS cannot keep a listening socket alive
 * once the app is suspended at all — there it can only stop the screen locking
 * and rely on the app staying in front.
 */
expect object PresentationKeepAlive {
    /** @param detail Short text for any user-visible indicator, e.g. the display URL. */
    fun start(detail: String?)

    fun stop()
}
