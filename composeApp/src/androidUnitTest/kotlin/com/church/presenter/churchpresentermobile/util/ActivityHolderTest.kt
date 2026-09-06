package com.church.presenter.churchpresentermobile.util

import android.app.Activity
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
}
