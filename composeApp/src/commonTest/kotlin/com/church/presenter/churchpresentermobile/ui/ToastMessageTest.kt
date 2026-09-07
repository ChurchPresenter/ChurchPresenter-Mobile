package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.church.presenter.churchpresentermobile.model.ToastEvent
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The words a toast actually shows.
 *
 * Each tab resolves the ViewModel's typed events to its own localised strings,
 * and each ends its `when` with an empty string for the events that belong to
 * another tab. That last branch is the dangerous one: an event a tab forgets to
 * handle does not crash and does not warn — it shows a snackbar with nothing in
 * it, mid-service, and the operator learns only that *something* happened.
 *
 * So every event a tab raises is asserted to produce words, and the events that
 * belong elsewhere are asserted to produce none.
 */
@OptIn(ExperimentalTestApi::class)
class ToastMessageTest {

    /** Renders [message] and reports what it resolved to. */
    private fun resolve(message: @Composable () -> String): String {
        var resolved = ""
        runComposeUiTest {
            showScreen {
                val text = message()
                resolved = text
                Text(text, modifier = Modifier.testTag("toast"))
            }
            onNodeWithTag("toast").fetchSemanticsNode()
        }
        return resolved
    }

    private fun songMessage(event: ToastEvent) = resolve { event.songToastMessage() }
    private fun bibleMessage(event: ToastEvent) = resolve { event.bibleToastMessage() }
    private fun presentationMessage(event: ToastEvent) = resolve { event.presentationToastMessage() }

    // ── The songs tab ────────────────────────────────────────────────────

    @Test
    fun aSongGoingLiveSaysSo() {
        assertTrue(songMessage(ToastEvent.SongLive).isNotBlank())
    }

    @Test
    fun aFailedRequestSaysSo() {
        assertTrue(songMessage(ToastEvent.RequestFailed).isNotBlank())
    }

    @Test
    fun havingNoSongSelectedSaysSo() {
        assertTrue(songMessage(ToastEvent.NoSongSelected).isNotBlank())
    }

    @Test
    fun aDeniedRequestSaysSo() {
        assertTrue(songMessage(ToastEvent.RequestDenied).isNotBlank())
    }

    @Test
    fun aBlockedSessionSaysSo() {
        assertTrue(songMessage(ToastEvent.SessionBlocked).isNotBlank())
    }

    @Test
    fun aSongAddedToTheScheduleSaysSo() {
        assertTrue(songMessage(ToastEvent.SongAddedToSchedule("Amazing Grace")).isNotBlank())
    }

    @Test
    fun aSongAddedToTheScheduleIsNamed() {
        // The operator adds several in a row; which one landed matters.
        assertTrue(songMessage(ToastEvent.SongAddedToSchedule("Amazing Grace")).contains("Amazing Grace"))
    }

    @Test
    fun aFailedProjectionSaysWhy() {
        assertTrue(songMessage(ToastEvent.FailedToProject("timeout")).contains("timeout"))
    }

    @Test
    fun aFailedScheduleAddSaysWhy() {
        assertTrue(songMessage(ToastEvent.FailedToAddSchedule("timeout")).contains("timeout"))
    }

    @Test
    fun aRejectedRequestQuotesTheStatus() {
        assertTrue(songMessage(ToastEvent.RequestRejected(503)).contains("503"))
    }

    @Test
    fun aRejectedRequestWithAReasonQuotesIt() {
        assertTrue(songMessage(ToastEvent.RequestRejectedWithReason("no key")).contains("no key"))
    }

    @Test
    fun theSongsTabSaysNothingAboutABibleEvent() {
        // It belongs to another tab; a snackbar here would be an empty one.
        assertTrue(songMessage(ToastEvent.BibleLive).isEmpty())
    }

    @Test
    fun theSongsTabSaysNothingAboutAnUploadEvent() {
        assertTrue(songMessage(ToastEvent.UploadFileTooLarge).isEmpty())
    }

    // ── The Bible tab ────────────────────────────────────────────────────

    @Test
    fun aVerseGoingLiveSaysSo() {
        assertTrue(bibleMessage(ToastEvent.BibleLive).isNotBlank())
    }

    @Test
    fun aVerseAddedToTheScheduleSaysSo() {
        assertTrue(bibleMessage(ToastEvent.BibleAddedToSchedule("John 3:16")).isNotBlank())
    }

    @Test
    fun aVerseAddedToTheScheduleIsNamed() {
        assertTrue(
            bibleMessage(ToastEvent.BibleAddedToSchedule("John 3:16")).contains("John 3:16")
        )
    }

    @Test
    fun aFailedVerseProjectionSaysWhy() {
        assertTrue(bibleMessage(ToastEvent.FailedToProjectBible("timeout")).contains("timeout"))
    }

    @Test
    fun aFailedVerseScheduleAddSaysWhy() {
        assertTrue(
            bibleMessage(ToastEvent.FailedToAddBibleSchedule("timeout")).contains("timeout")
        )
    }

    @Test
    fun theBibleTabReportsADeniedRequest() {
        // Shared events belong to every tab: a denied request can happen here too.
        assertTrue(bibleMessage(ToastEvent.RequestDenied).isNotBlank())
    }

    @Test
    fun theBibleTabReportsABlockedSession() {
        assertTrue(bibleMessage(ToastEvent.SessionBlocked).isNotBlank())
    }

    @Test
    fun theBibleTabQuotesARejectedStatus() {
        assertTrue(bibleMessage(ToastEvent.RequestRejected(401)).contains("401"))
    }

    @Test
    fun theBibleTabQuotesARejectionReason() {
        assertTrue(bibleMessage(ToastEvent.RequestRejectedWithReason("locked")).contains("locked"))
    }

    @Test
    fun theBibleTabSaysNothingAboutASongEvent() {
        assertTrue(bibleMessage(ToastEvent.SongLive).isEmpty())
    }

    @Test
    fun theBibleTabSaysNothingAboutAnUploadEvent() {
        assertTrue(bibleMessage(ToastEvent.UploadUnsupported).isEmpty())
    }

    // ── The presentations tab ────────────────────────────────────────────

    @Test
    fun aFailedDeckSelectionSaysWhy() {
        assertTrue(
            presentationMessage(ToastEvent.FailedToSelectPresentation("timeout")).contains("timeout")
        )
    }

    @Test
    fun aFailedDeckScheduleAddSaysWhy() {
        assertTrue(
            presentationMessage(ToastEvent.FailedToAddPresentationSchedule("timeout"))
                .contains("timeout")
        )
    }

    @Test
    fun anUnsupportedUploadSaysSo() {
        assertTrue(presentationMessage(ToastEvent.UploadUnsupported).isNotBlank())
    }

    @Test
    fun aFileTooLargeSaysSo() {
        assertTrue(presentationMessage(ToastEvent.UploadFileTooLarge).isNotBlank())
    }

    @Test
    fun aServerErrorDuringUploadSaysWhat() {
        assertTrue(presentationMessage(ToastEvent.UploadServerError("500")).contains("500"))
    }

    @Test
    fun aFailedUploadSaysWhy() {
        assertTrue(presentationMessage(ToastEvent.UploadFailed("no route")).contains("no route"))
    }

    @Test
    fun aFailedReloadAfterUploadSaysWhy() {
        // The file arrived and the list did not come back — a different problem
        // from the upload failing, and the operator needs to know which.
        assertTrue(presentationMessage(ToastEvent.UploadReloadFailed("timeout")).contains("timeout"))
    }

    @Test
    fun thePresentationsTabSaysNothingAboutASongEvent() {
        assertTrue(presentationMessage(ToastEvent.SongLive).isEmpty())
    }

    @Test
    fun thePresentationsTabSaysNothingAboutABibleEvent() {
        assertTrue(presentationMessage(ToastEvent.BibleLive).isEmpty())
    }

    @Test
    fun thePresentationsTabSaysNothingAboutADeniedRequest() {
        // Not handled here today; asserted so the silence is a decision rather
        // than a surprise.
        assertTrue(presentationMessage(ToastEvent.RequestDenied).isEmpty())
    }
}
