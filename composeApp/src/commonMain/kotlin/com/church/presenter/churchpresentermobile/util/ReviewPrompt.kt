package com.church.presenter.churchpresentermobile.util

import com.church.presenter.churchpresentermobile.model.AppSettings

/**
 * Decides when the in-app review prompt is due, from real use rather than
 * launches.
 *
 * Every successful go-live (a song or Bible passage put on screen) is counted
 * with [recordProjection]. The prompt is due once the count reaches the next
 * threshold — 5 go-lives, then 25, then every 50 after that — and
 * [consumeDue] answers that question once per threshold.
 *
 * The prompt itself is shown by the platform at launch (`iOSApp.swift`), never
 * at the moment of the go-live: the operator is mid-service then, and a system
 * sheet over the controls is the last thing they need.
 */
object ReviewPrompt {

    internal const val FIRST_THRESHOLD = 5
    internal const val SECOND_THRESHOLD = 25
    internal const val REPEAT_EVERY = 50

    /** Go-lives needed before prompt number [promptsShown] + 1. */
    internal fun threshold(promptsShown: Int): Int = when {
        promptsShown <= 0 -> FIRST_THRESHOLD
        else -> SECOND_THRESHOLD + REPEAT_EVERY * (promptsShown - 1)
    }

    /** Counts one successful go-live toward the next prompt. */
    fun recordProjection(settings: AppSettings) {
        settings.reviewProjectionCount = settings.reviewProjectionCount + 1
    }

    /**
     * True when enough go-lives have been counted for the next prompt, and marks
     * that prompt as used — so a caller that gets true should ask the OS now.
     * Called from Swift at launch.
     */
    fun consumeDue(): Boolean = consumeDue(AppSettings())

    internal fun consumeDue(settings: AppSettings): Boolean {
        val shown = settings.reviewPromptsShown
        if (settings.reviewProjectionCount < threshold(shown)) return false
        settings.reviewPromptsShown = shown + 1
        return true
    }
}
