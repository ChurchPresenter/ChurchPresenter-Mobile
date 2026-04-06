package com.church.presenter.churchpresentermobile.util

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Manages the Google Play In-App Review prompt.
 *
 * Trigger rule (same thresholds used on iOS via SKStoreReviewController):
 *   - 3rd  app open
 *   - 10th app open
 *   - Every 20th open after that (30, 50, 70 …)
 *
 * The Play API silently no-ops if the user has already reviewed the app or
 * if the quota for the current period has been exceeded — so it is safe to
 * call [maybeRequest] on every launch that satisfies the rule.
 */
object AppReview {

    private const val TAG = "AppReview"

    /** Returns true when [openCount] matches a review-prompt milestone. */
    fun shouldRequest(openCount: Int): Boolean =
        openCount == 3 || openCount == 10 || (openCount > 10 && openCount % 20 == 0)

    /**
     * Shows the Play in-app review sheet if [openCount] hits a milestone.
     * Must be called from an [Activity] context (e.g. MainActivity.onCreate).
     */
    fun maybeRequest(activity: Activity, openCount: Int) {
        if (!shouldRequest(openCount)) return

        Logger.d(TAG, "Requesting in-app review at open #$openCount")
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                manager.launchReviewFlow(activity, task.result)
                    .addOnCompleteListener {
                        Logger.d(TAG, "Review flow complete")
                    }
            } else {
                Logger.e(TAG, "requestReviewFlow failed", task.exception)
            }
        }
    }
}

