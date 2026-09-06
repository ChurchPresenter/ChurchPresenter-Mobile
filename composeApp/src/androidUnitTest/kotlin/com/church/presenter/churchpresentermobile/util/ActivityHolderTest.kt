package com.church.presenter.churchpresentermobile.util

import android.app.Activity
import io.mockk.every
import io.mockk.mockk
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Covers the handover [ActivityHolder] exists for.
 *
 * An output sink lives outside the Compose tree and needs a real Activity to
 * open `android.app.Presentation` on — a Dialog shown with an application
 * context throws. The failure mode this guards is the opposite one: holding a
 * finished Activity past its life and handing it out anyway.
 */
class ActivityHolderTest {

    @AfterTest
    fun clear() {
        // A process-wide object; leaving one attached would reach the next test.
        ActivityHolder.current?.let { ActivityHolder.detach(it) }
    }

    @Test
    fun `nothing is attached to begin with`() {
        assertNull(ActivityHolder.current)
    }

    @Test
    fun `an attached activity is handed back`() {
        val activity = Activity()

        ActivityHolder.attach(activity)

        assertSame(activity, ActivityHolder.current)
    }

    @Test
    fun `a second attach replaces the first`() {
        // What a rotation looks like: the new Activity arrives before the old one
        // is torn down, so the newest must win.
        val first = Activity()
        val second = Activity()

        ActivityHolder.attach(first)
        ActivityHolder.attach(second)

        assertSame(second, ActivityHolder.current)
    }

    @Test
    fun `detaching the held activity clears it`() {
        val activity = Activity()
        ActivityHolder.attach(activity)

        ActivityHolder.detach(activity)

        assertNull(ActivityHolder.current)
    }

    @Test
    fun `detaching some other activity leaves the held one alone`() {
        // The ordering that made this check necessary: the outgoing Activity's
        // onDestroy runs after the incoming one has already attached, so an
        // unconditional clear would blank the reference the new screen just set.
        val held = Activity()
        ActivityHolder.attach(held)

        ActivityHolder.detach(Activity())

        assertSame(held, ActivityHolder.current)
    }

    @Test
    fun `detaching twice is not an error`() {
        val activity = Activity()
        ActivityHolder.attach(activity)

        ActivityHolder.detach(activity)
        ActivityHolder.detach(activity)

        assertNull(ActivityHolder.current)
    }

    // ── An Activity on its way out ───────────────────────────────────────
    //
    // The reference is weak, but a weak reference to a *finishing* Activity is
    // still live until the collector runs. Handing that one out is how the
    // presentation window gets a BadTokenException: a Dialog cannot be shown on
    // a Activity that is already going away, and the crash lands mid-service
    // with the TV going dark.

    @Test
    fun `an activity that is finishing is not handed out`() {
        val leaving = mockk<Activity> {
            every { isFinishing } returns true
            every { isDestroyed } returns false
        }

        ActivityHolder.attach(leaving)

        assertNull(ActivityHolder.current)
    }

    @Test
    fun `an activity that is already destroyed is not handed out`() {
        val gone = mockk<Activity> {
            every { isFinishing } returns false
            every { isDestroyed } returns true
        }

        ActivityHolder.attach(gone)

        assertNull(ActivityHolder.current)
    }

    @Test
    fun `an activity that is both finishing and destroyed is not handed out`() {
        val gone = mockk<Activity> {
            every { isFinishing } returns true
            every { isDestroyed } returns true
        }

        ActivityHolder.attach(gone)

        assertNull(ActivityHolder.current)
    }

    @Test
    fun `a live activity is handed out`() {
        val live = mockk<Activity> {
            every { isFinishing } returns false
            every { isDestroyed } returns false
        }

        ActivityHolder.attach(live)

        assertSame(live, ActivityHolder.current)
    }

    @Test
    fun `an activity that starts finishing stops being handed out`() {
        // Nothing calls detach in this order — the holder simply stops offering
        // it, which is what makes the check worth having rather than relying on
        // onDestroy running first.
        var finishing = false
        val activity = mockk<Activity> {
            every { isFinishing } answers { finishing }
            every { isDestroyed } returns false
        }
        ActivityHolder.attach(activity)
        assertSame(activity, ActivityHolder.current)

        finishing = true

        assertNull(ActivityHolder.current)
    }

    @Test
    fun `detaching a finishing activity still clears it`() {
        // The @AfterTest cleanup relies on this: current is null, so detach has
        // to be reachable by identity rather than through current.
        val leaving = mockk<Activity> {
            every { isFinishing } returns true
            every { isDestroyed } returns false
        }
        ActivityHolder.attach(leaving)

        ActivityHolder.detach(leaving)

        assertNull(ActivityHolder.current)
    }
}
