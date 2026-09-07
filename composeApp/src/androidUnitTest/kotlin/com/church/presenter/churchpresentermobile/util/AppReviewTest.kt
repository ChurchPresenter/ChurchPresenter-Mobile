package com.church.presenter.churchpresentermobile.util

import android.app.Activity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.test.AfterTest
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

    // ── Reaching the Play API ────────────────────────────────────────────
    //
    // Play's own quota silently drops surplus requests, so a broken flow here
    // looks exactly like a working one on a device. Mocked so the two steps —
    // asking for a flow, then launching it — can be told apart.

    @AfterTest
    fun unmock() = unmockkAll()

    private val activity = mockk<Activity>(relaxed = true)
    private val manager = mockk<ReviewManager>(relaxed = true)

    /** Play answers the request for a review flow, successfully or not. */
    private fun playAnswers(successful: Boolean) {
        val request = mockk<Task<ReviewInfo>>(relaxed = true) {
            every { isSuccessful } returns successful
            every { result } returns mockk(relaxed = true)
            every { exception } returns if (successful) null else RuntimeException("no flow")
        }
        every { request.addOnCompleteListener(any<OnCompleteListener<ReviewInfo>>()) } answers {
            firstArg<OnCompleteListener<ReviewInfo>>().onComplete(request)
            request
        }
        every { manager.requestReviewFlow() } returns request

        val launch = mockk<Task<Void>>(relaxed = true)
        every { launch.addOnCompleteListener(any<OnCompleteListener<Void>>()) } answers {
            firstArg<OnCompleteListener<Void>>().onComplete(launch)
            launch
        }
        every { manager.launchReviewFlow(any(), any()) } returns launch

        mockkStatic(ReviewManagerFactory::class)
        every { ReviewManagerFactory.create(any()) } returns manager
    }

    @Test
    fun `a milestone open asks Play for a review flow`() {
        playAnswers(successful = true)

        AppReview.maybeRequest(activity, MILESTONE_OPEN)

        verify { manager.requestReviewFlow() }
    }

    @Test
    fun `a flow Play grants is shown to the operator`() {
        playAnswers(successful = true)

        AppReview.maybeRequest(activity, MILESTONE_OPEN)

        verify { manager.launchReviewFlow(activity, any()) }
    }

    @Test
    fun `a flow Play refuses is not launched`() {
        // Quota exhausted, already reviewed, or no Play Store at all. There is
        // nothing to show and nothing the operator could do about it.
        playAnswers(successful = false)

        AppReview.maybeRequest(activity, MILESTONE_OPEN)

        verify(exactly = 0) { manager.launchReviewFlow(any(), any()) }
    }

    @Test
    fun `a refused flow does not crash the launch it was called from`() {
        // maybeRequest runs from MainActivity.onCreate; a throw here would take
        // the app down on startup for every install with no Play Store.
        playAnswers(successful = false)

        AppReview.maybeRequest(activity, MILESTONE_OPEN)
    }

    @Test
    fun `a non-milestone open never asks Play at all`() {
        // The early return is what lets this be called unconditionally on every
        // launch without burning the quota.
        playAnswers(successful = true)

        AppReview.maybeRequest(activity, 2)

        verify(exactly = 0) { manager.requestReviewFlow() }
    }

    @Test
    fun `every milestone asks, and nothing between them does`() {
        playAnswers(successful = true)

        for (open in listOf(3, 10, 20, 40)) {
            AppReview.maybeRequest(activity, open)
        }
        for (open in listOf(1, 2, 5, 11, 19, 21)) {
            AppReview.maybeRequest(activity, open)
        }

        verify(exactly = 4) { manager.requestReviewFlow() }
    }

    private companion object {
        /** The third open — the first milestone. */
        const val MILESTONE_OPEN = 3
    }
}
