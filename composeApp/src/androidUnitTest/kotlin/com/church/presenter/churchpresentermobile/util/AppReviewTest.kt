package com.church.presenter.churchpresentermobile.util

import android.app.Activity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers when the Play in-app review sheet is offered.
 *
 * Worth pinning down because the cost of getting it wrong is asymmetric: too
 * rare and the app never collects a rating, too often and the operator is
 * interrupted on a Sunday morning. Play's own quota hides a wrong rule in
 * testing — it silently drops surplus requests — so only the arithmetic here
 * says what the app actually intends.
 */
class AppReviewTest {

    @Test
    fun `the third open is a milestone`() {
        assertTrue(AppReview.shouldRequest(3))
    }

    @Test
    fun `the tenth open is a milestone`() {
        assertTrue(AppReview.shouldRequest(10))
    }

    @Test
    fun `the first two opens are too early to ask`() {
        // Someone who has opened the app twice has not yet used it.
        assertFalse(AppReview.shouldRequest(1))
        assertFalse(AppReview.shouldRequest(2))
    }

    @Test
    fun `nothing between the third and tenth open asks again`() {
        assertTrue((4..9).none { AppReview.shouldRequest(it) })
    }

    @Test
    fun `after the tenth it asks every twentieth open`() {
        // Note the doc comment on AppReview says "30, 50, 70"; the rule as written
        // is every multiple of 20 above 10, so the first of these is 20, not 30.
        assertEquals(listOf(20, 40, 60, 80, 100), (11..100).filter { AppReview.shouldRequest(it) })
    }

    @Test
    fun `a count of zero never asks`() {
        // Zero is divisible by twenty; only the "> 10" guard keeps it quiet.
        assertFalse(AppReview.shouldRequest(0))
    }

    @Test
    fun `a negative count never asks`() {
        // Not reachable through the open counter, but a corrupted preference
        // should not be able to summon a review sheet.
        assertTrue((-40..-1).none { AppReview.shouldRequest(it) })
    }

    @Test
    fun `a non-milestone open never reaches the Play API`() {
        // maybeRequest returns before touching ReviewManagerFactory, which is what
        // lets it be called unconditionally on every launch.
        AppReview.maybeRequest(Activity(), 2)
    }
}
