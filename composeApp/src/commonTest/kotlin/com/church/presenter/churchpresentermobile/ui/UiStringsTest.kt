package com.church.presenter.churchpresentermobile.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests [UiStrings] — the messages the operator reads mid-service.
 *
 * The interpolating ones are the point: each embeds runtime data, and a wrong
 * placeholder produces a message that is grammatical, plausible, and about the
 * wrong thing. The constants are checked for the properties a screen depends on
 * rather than restated word for word.
 */
class UiStringsTest {

    @Test
    fun everyConstantSaysSomething() {
        val all = listOf(
            UiStrings.ADD_TO_SCHEDULE,
            UiStrings.SCHEDULE_REQUEST_SENT,
            UiStrings.PROJECT_TO_SCREEN,
            UiStrings.STOP_PROJECTING,
            UiStrings.FAB_ACTIONS,
            UiStrings.FAB_CLOSE_ACTIONS,
            UiStrings.PROJECTING_BADGE,
            UiStrings.NO_LYRICS_AVAILABLE,
            UiStrings.TOAST_SONG_LIVE,
            UiStrings.TOAST_REQUEST_FAILED,
            UiStrings.TOAST_NO_SONG_SELECTED,
        )

        assertTrue(all.all { it.isNotBlank() }, "a blank label renders as an empty control")
    }

    @Test
    fun theProjectAndStopLabelsAreOpposites() {
        // The same button toggles between them; identical text would leave the
        // operator unable to tell whether anything is live.
        assertTrue(UiStrings.PROJECT_TO_SCREEN != UiStrings.STOP_PROJECTING)
    }

    @Test
    fun theScheduleLabelAndItsConfirmationDiffer() {
        assertTrue(UiStrings.ADD_TO_SCHEDULE != UiStrings.SCHEDULE_REQUEST_SENT)
        assertTrue(UiStrings.SCHEDULE_REQUEST_SENT.contains("✓"), UiStrings.SCHEDULE_REQUEST_SENT)
    }

    @Test
    fun theFabLabelsDifferSoTheOpenStateIsReadable() {
        assertTrue(UiStrings.FAB_ACTIONS != UiStrings.FAB_CLOSE_ACTIONS)
    }

    @Test
    fun aSongAddedMessageQuotesTheTitle() {
        // Quoted because song titles are ordinary words — "Amazing Grace added to
        // schedule" reads as a sentence rather than as a name.
        val message = UiStrings.toastSongAddedToSchedule("Amazing Grace")

        assertEquals("\"Amazing Grace\" added to schedule", message)
    }

    @Test
    fun aSongAddedMessageSurvivesAnAwkwardTitle() {
        val message = UiStrings.toastSongAddedToSchedule("")

        assertTrue(message.contains("added to schedule"), message)
        assertFalse(message.contains("null"), message)
    }

    @Test
    fun aTitleWithItsOwnQuotesIsStillEmbedded() {
        val message = UiStrings.toastSongAddedToSchedule("""He Said "Come"""")

        assertTrue(message.contains("Come"), message)
    }

    @Test
    fun aProjectionFailureNamesTheReason() {
        // Without the reason the operator is told only that it failed, which is
        // the message this replaced.
        val message = UiStrings.toastFailedToProject("Server not reachable")

        assertTrue(message.contains("Server not reachable"), message)
        assertTrue(message.startsWith("Failed to project"), message)
    }

    @Test
    fun aScheduleFailureNamesTheReason() {
        val message = UiStrings.toastFailedToAddSchedule("denied")

        assertTrue(message.contains("denied"), message)
        assertTrue(message.contains("schedule"), message)
    }

    @Test
    fun theTwoFailureMessagesAreDistinguishable() {
        // Same reason, different action — the operator has to know which failed.
        val project = UiStrings.toastFailedToProject("offline")
        val schedule = UiStrings.toastFailedToAddSchedule("offline")

        assertTrue(project != schedule)
    }

    @Test
    fun aBlankReasonStillProducesAReadableMessage() {
        assertFalse(UiStrings.toastFailedToProject("").contains("null"))
        assertFalse(UiStrings.toastFailedToAddSchedule("").contains("null"))
    }

    @Test
    fun theProjectingBadgeIsMarkedForTheEye() {
        // Shown over live content; the glyph is what makes it findable at a glance.
        assertTrue(UiStrings.PROJECTING_BADGE.contains("▶"), UiStrings.PROJECTING_BADGE)
    }
}
