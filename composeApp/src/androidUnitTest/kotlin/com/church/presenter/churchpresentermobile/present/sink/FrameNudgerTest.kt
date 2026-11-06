package com.church.presenter.churchpresentermobile.present.sink

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The burst of redraws that gets a static slide past a Cast receiver which
 * dropped the first key frame — without it the TV stays black until the
 * operator changes slide.
 */
class FrameNudgerTest {

    /** A hand-cranked timer: records what was scheduled, runs it on demand. */
    private class FakeTimer {
        val pending = mutableListOf<Pair<Long, () -> Unit>>()
        var cancels = 0

        fun schedule(delayMs: Long, action: () -> Unit) {
            pending += delayMs to action
        }

        fun cancelAll() {
            cancels++
            pending.clear()
        }

        fun runAll() = pending.toList().forEach { (_, action) -> action() }
    }

    private val timer = FakeTimer()
    private var repaints = 0

    private fun nudger(intervalMs: Long = NUDGE_INTERVAL_MS, windowMs: Long = NUDGE_WINDOW_MS) =
        FrameNudger(
            repaint = { repaints++ },
            schedule = timer::schedule,
            cancelAll = timer::cancelAll,
            intervalMs = intervalMs,
            windowMs = windowMs,
        )

    @Test
    fun `start schedules a redraw every interval until the window closes`() {
        nudger(intervalMs = 250, windowMs = 1_000).start()

        assertEquals(listOf(250L, 500L, 750L, 1_000L), timer.pending.map { it.first })
    }

    @Test
    fun `each scheduled action redraws the window`() {
        nudger(intervalMs = 250, windowMs = 1_000).start()

        timer.runAll()

        assertEquals(4, repaints)
    }

    @Test
    fun `nothing is redrawn until the timer fires`() {
        nudger().start()

        assertEquals(0, repaints)
    }

    @Test
    fun `the default burst covers the first seconds without running forever`() {
        nudger().start()

        val delays = timer.pending.map { it.first }
        assertEquals(NUDGE_INTERVAL_MS, delays.first())
        assertEquals(NUDGE_WINDOW_MS, delays.last())
        assertEquals((NUDGE_WINDOW_MS / NUDGE_INTERVAL_MS).toInt(), delays.size)
    }

    @Test
    fun `starting again replaces the pending burst instead of stacking a second`() {
        val nudger = nudger(intervalMs = 250, windowMs = 1_000)

        nudger.start()
        nudger.start()

        assertEquals(4, timer.pending.size)
    }

    @Test
    fun `stop drops every pending redraw`() {
        val nudger = nudger()
        nudger.start()

        nudger.stop()
        timer.runAll()

        assertTrue(timer.pending.isEmpty())
        assertEquals(0, repaints)
    }
}
