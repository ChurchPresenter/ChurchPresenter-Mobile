package com.church.presenter.churchpresentermobile.util

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests [ReviewPrompt] — when the in-app review prompt is due, counted in
 * go-lives rather than launches.
 */
class ReviewPromptTest {

    private fun settings() = AppSettings(InMemorySettingsStorage())

    private fun AppSettings.goLive(times: Int) = repeat(times) { ReviewPrompt.recordProjection(this) }

    @Test
    fun thresholdsAreFiveThenTwentyFiveThenEveryFifty() {
        assertEquals(listOf(5, 25, 75, 125, 175), (0..4).map(ReviewPrompt::threshold))
    }

    @Test
    fun aNegativeCountIsTreatedAsNoPromptsYet() {
        assertEquals(ReviewPrompt.FIRST_THRESHOLD, ReviewPrompt.threshold(-1))
    }

    @Test
    fun recordProjectionCountsEachGoLive() {
        val s = settings()
        s.goLive(3)
        assertEquals(3, s.reviewProjectionCount)
    }

    @Test
    fun aFreshInstallIsNotDue() {
        assertFalse(ReviewPrompt.consumeDue(settings()))
    }

    @Test
    fun notDueOneGoLiveShortOfTheFirstThreshold() {
        val s = settings()
        s.goLive(ReviewPrompt.FIRST_THRESHOLD - 1)
        assertFalse(ReviewPrompt.consumeDue(s))
        assertEquals(0, s.reviewPromptsShown)
    }

    @Test
    fun dueAtTheFirstThresholdAndMarksThePromptUsed() {
        val s = settings()
        s.goLive(ReviewPrompt.FIRST_THRESHOLD)
        assertTrue(ReviewPrompt.consumeDue(s))
        assertEquals(1, s.reviewPromptsShown)
    }

    @Test
    fun aConsumedPromptIsNotDueAgainUntilTheNextThreshold() {
        val s = settings()
        s.goLive(ReviewPrompt.FIRST_THRESHOLD)
        assertTrue(ReviewPrompt.consumeDue(s))
        // The next launch, with no more use, must not ask again.
        assertFalse(ReviewPrompt.consumeDue(s))

        s.goLive(ReviewPrompt.SECOND_THRESHOLD - ReviewPrompt.FIRST_THRESHOLD - 1)
        assertFalse(ReviewPrompt.consumeDue(s))
        s.goLive(1)
        assertTrue(ReviewPrompt.consumeDue(s))
        assertEquals(2, s.reviewPromptsShown)
    }

    @Test
    fun aBacklogOfUseYieldsOnePromptPerLaunchNotABurst() {
        // An operator who never relaunched can be far past several thresholds;
        // each launch still asks at most once.
        val s = settings()
        s.goLive(200)
        assertTrue(ReviewPrompt.consumeDue(s))
        assertEquals(1, s.reviewPromptsShown)
    }

    @Test
    fun theCountersPersistInStorage() {
        val storage = InMemorySettingsStorage()
        val first = AppSettings(storage)
        first.goLive(ReviewPrompt.FIRST_THRESHOLD)
        assertTrue(ReviewPrompt.consumeDue(first))

        val reopened = AppSettings(storage)
        assertEquals(ReviewPrompt.FIRST_THRESHOLD, reopened.reviewProjectionCount)
        assertEquals(1, reopened.reviewPromptsShown)
        assertFalse(ReviewPrompt.consumeDue(reopened))
    }
}
