package com.church.presenter.churchpresentermobile.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests [ToastEvent] — the app's vocabulary of user-facing messages.
 *
 * The UI resolves each of these to a localised string by exhaustive `when`, so
 * what matters is that the variants stay distinguishable: two events that compare
 * equal, or a data class that drops the detail it exists to carry, show the
 * operator the wrong message with nothing failing.
 */
class ToastEventTest {

    /** One of every variant, so a new one added without a test is visible here. */
    private val all: List<ToastEvent> = listOf(
        ToastEvent.SongLive,
        ToastEvent.RequestFailed,
        ToastEvent.NoSongSelected,
        ToastEvent.RequestDenied,
        ToastEvent.SessionBlocked,
        ToastEvent.SongAddedToSchedule("Amazing Grace"),
        ToastEvent.FailedToProject("offline"),
        ToastEvent.FailedToAddSchedule("denied"),
        ToastEvent.RequestRejected(403),
        ToastEvent.RequestRejectedWithReason("no folder loaded"),
        ToastEvent.BibleLive,
        ToastEvent.BibleAddedToSchedule("John 3:16"),
        ToastEvent.FailedToProjectBible("offline"),
        ToastEvent.FailedToAddBibleSchedule("denied"),
        ToastEvent.FailedToSelectPresentation("offline"),
        ToastEvent.FailedToAddPresentationSchedule("denied"),
        ToastEvent.UploadUnsupported,
        ToastEvent.UploadDisabled,
        ToastEvent.UploadFileTooLarge,
        ToastEvent.UploadServerError("500"),
        ToastEvent.UploadFailed("timeout"),
        ToastEvent.UploadReloadFailed("timeout"),
    )

    @Test
    fun everyVariantIsDistinctFromEveryOther() {
        // The UI switches on these; two that collide would render one message for
        // two different outcomes.
        assertEquals(all.size, all.toSet().size, "two ToastEvent variants compare equal")
    }

    @Test
    fun theStaticMessagesAreSingletons() {
        // `data object`, so the same instance every time — safe to compare by
        // identity in a `when` and free to emit repeatedly.
        assertEquals(ToastEvent.SongLive, ToastEvent.SongLive)
        assertEquals(ToastEvent.BibleLive, ToastEvent.BibleLive)
        assertEquals(ToastEvent.UploadDisabled, ToastEvent.UploadDisabled)
        assertNotEquals<ToastEvent>(ToastEvent.SongLive, ToastEvent.BibleLive)
    }

    @Test
    fun aSongAddedCarriesTheTitleTheOperatorWillSee() {
        val event = ToastEvent.SongAddedToSchedule("Amazing Grace")

        assertEquals("Amazing Grace", event.title)
        assertNotEquals(event, ToastEvent.SongAddedToSchedule("Be Thou My Vision"))
    }

    @Test
    fun aBibleAddedCarriesTheReference() {
        assertEquals("John 3:16", ToastEvent.BibleAddedToSchedule("John 3:16").reference)
    }

    @Test
    fun everyFailureCarriesItsOwnReason() {
        // The reason is the only thing that tells the operator what to fix, so a
        // variant that discards it leaves them with "something went wrong".
        assertEquals("offline", ToastEvent.FailedToProject("offline").reason)
        assertEquals("denied", ToastEvent.FailedToAddSchedule("denied").reason)
        assertEquals("offline", ToastEvent.FailedToProjectBible("offline").reason)
        assertEquals("denied", ToastEvent.FailedToAddBibleSchedule("denied").reason)
        assertEquals("offline", ToastEvent.FailedToSelectPresentation("offline").reason)
        assertEquals("denied", ToastEvent.FailedToAddPresentationSchedule("denied").reason)
        assertEquals("no folder", ToastEvent.RequestRejectedWithReason("no folder").reason)
        assertEquals("timeout", ToastEvent.UploadFailed("timeout").reason)
        assertEquals("timeout", ToastEvent.UploadReloadFailed("timeout").reason)
        assertEquals("500", ToastEvent.UploadServerError("500").msg)
    }

    @Test
    fun aRejectionCarriesTheStatusCode() {
        // Distinct from RequestRejectedWithReason: the desktop said nothing useful,
        // so the number is all there is to show.
        assertEquals(403, ToastEvent.RequestRejected(403).httpStatus)
        assertNotEquals(ToastEvent.RequestRejected(403), ToastEvent.RequestRejected(503))
    }

    @Test
    fun theTwoRejectionKindsAreNotInterchangeable() {
        assertNotEquals<ToastEvent>(
            ToastEvent.RequestRejected(403),
            ToastEvent.RequestRejectedWithReason("403"),
        )
    }

    @Test
    fun theThreeUploadRefusalsAreSeparateOutcomes() {
        // Unsupported platform, disabled by the operator, and file-too-large each
        // need a different answer from the user.
        val refusals = listOf<ToastEvent>(
            ToastEvent.UploadUnsupported,
            ToastEvent.UploadDisabled,
            ToastEvent.UploadFileTooLarge,
        )

        assertEquals(refusals.size, refusals.toSet().size)
    }

    @Test
    fun everyVariantIsAToastEvent() {
        assertTrue(all.all { it is ToastEvent })
    }
}
