package com.church.presenter.churchpresentermobile.present.sink

/** How often the slide window is redrawn while a Cast receiver is settling. */
internal const val NUDGE_INTERVAL_MS = 250L

/** How long after the window appears the redraws keep coming. */
internal const val NUDGE_WINDOW_MS = 5_000L

/**
 * Redraws the presentation window a few times a second for a few seconds —
 * after it appears, and again after every slide change.
 *
 * A Cast mirroring session only sends a frame when the virtual display's
 * surface changes, and a slide does not move. The first frame is drawn before
 * the encoder has settled — and a TV that drops part of that key frame (a Sony
 * Bravia logs "Ignoring packet in video frame 0") then shows black until the
 * operator happens to change the slide. A change has the same problem at its
 * other end: the last frame of the fade can be held back until another follows,
 * so the TV ran a verse behind. Redrawing unchanged content still queues a fresh
 * buffer, which the encoder sends; a short burst is enough for one to arrive
 * whole. HDMI and Miracast are unaffected and pay for twenty redraws of a
 * static screen.
 *
 * Kept free of `Handler` and `View` so the schedule can be tested off-device:
 * [schedule] and [cancelAll] are the caller's timer, [repaint] its redraw.
 */
internal class FrameNudger(
    private val repaint: () -> Unit,
    private val schedule: (delayMs: Long, action: () -> Unit) -> Unit,
    private val cancelAll: () -> Unit,
    private val intervalMs: Long = NUDGE_INTERVAL_MS,
    private val windowMs: Long = NUDGE_WINDOW_MS,
) {

    /** Schedules the burst, replacing any burst still pending. */
    fun start() {
        cancelAll()
        var delay = intervalMs
        while (delay <= windowMs) {
            schedule(delay, repaint)
            delay += intervalMs
        }
    }

    /** Drops every redraw still pending. Idempotent. */
    fun stop() = cancelAll()
}
