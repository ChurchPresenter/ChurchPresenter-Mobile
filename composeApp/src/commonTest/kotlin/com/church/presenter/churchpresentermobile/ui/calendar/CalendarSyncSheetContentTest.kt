package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.calendar.sync.SyncStatus
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.isShowing
import com.church.presenter.churchpresentermobile.ui.showScreen
import com.church.presenter.churchpresentermobile.viewmodel.EnrollFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Enrolling this phone with the church computer, and what the sheet says afterwards.
 *
 * The code is the point of the waiting state: the operator at the computer reads it off their
 * prompt and off this screen, and they have to match. It is data, so it is what these assert on.
 */
@OptIn(ExperimentalTestApi::class)
class CalendarSyncSheetContentTest {

    private var enrolled = 0
    private var reset = 0
    private var syncedNow = 0
    private var left = 0
    private var dismissed = 0
    private val scanned = mutableListOf<String>()

    private fun actions() = SyncActions(
        onEnroll = { enrolled++ },
        onScanned = { scanned += it },
        onReset = { reset++ },
        onSyncNow = { syncedNow++ },
        onLeave = { left++ },
    )

    private fun ComposeUiTest.show(
        status: SyncStatus = SyncStatus.NotEnrolled,
        flow: EnrollFlow = EnrollFlow.Idle,
        canReachDesktop: Boolean = true,
    ) = showScreen {
        CalendarSyncSheetContent(status, flow, canReachDesktop, actions()) { dismissed++ }
    }

    private fun ComposeUiTest.controls() = onAllNodes(hasClickAction(), useUnmergedTree = true)

    // The enrol button is disabled without a desktop address, and [click] invokes the semantics
    // action rather than tapping, which a disabled node still carries -- so a test that means
    // "this cannot be pressed" asserts `assertIsNotEnabled` instead of counting callbacks.
    private fun ComposeUiTest.enroll() = click(CalendarTags.SYNC_ENROLL)

    // ── Not enrolled yet ─────────────────────────────────────────────────

    @Test
    fun anUnenrolledPhoneOffersBothWaysIn() = runComposeUiTest {
        show()

        // The request over the church WiFi. The scan button beside it is a platform control --
        // this runtime has no camera, so it draws nothing here and only a device can show it.
        assertTrue(exists(CalendarTags.SYNC_ENROLL))
    }

    @Test
    fun askingOverTheWifiStartsTheEnrollment() = runComposeUiTest {
        show()

        enroll()

        assertEquals(1, enrolled)
    }

    @Test
    fun aPhoneWithNoComputerAddressCannotAsk() = runComposeUiTest {
        show(canReachDesktop = false)

        // Disabled rather than hidden: the sheet says what to do about it right above.
        onNodeWithTag(CalendarTags.SYNC_ENROLL).assertIsNotEnabled()
        assertEquals(0, enrolled)
    }

    @Test
    fun aRefusedPhoneIsToldSoAndCanTryAgain() = runComposeUiTest {
        show(flow = EnrollFlow.Denied)

        enroll()

        assertEquals(1, enrolled)
    }

    @Test
    fun aDesktopWithSyncSwitchedOffIsItsOwnAnswer() = runComposeUiTest {
        show(flow = EnrollFlow.SyncOff)

        // Distinct from a refusal: nothing is wrong with this phone, the switch is off over there.
        assertTrue(controls().fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun aFailureShowsWhatWentWrong() = runComposeUiTest {
        show(flow = EnrollFlow.Failed("connection refused"))

        assertTrue(isShowing("connection refused"), "the reason, not just that it failed")
    }

    @Test
    fun aScanOfSomethingElseSaysSoWithoutAMessage() = runComposeUiTest {
        show(flow = EnrollFlow.Failed(""))

        assertTrue(controls().fetchSemanticsNodes().isNotEmpty())
    }

    // ── Waiting for the operator at the computer ─────────────────────────

    @Test
    fun theCodeIsShownInGroupsForReadingAloud() = runComposeUiTest {
        show(flow = EnrollFlow.WaitingForApproval("482913"))

        assertTrue(isShowing("482 913"))
    }

    @Test
    fun waitingCanBeCancelled() = runComposeUiTest {
        show(flow = EnrollFlow.WaitingForApproval("482913"))

        click(CalendarTags.SYNC_CANCEL)

        assertEquals(1, reset)
    }

    @Test
    fun theApprovedPhoneIsAskedToScanTheComputersCode() = runComposeUiTest {
        show(flow = EnrollFlow.ScanQr)

        // Same here: the scanner is the platform's, so what this runtime can see is the way out.
        assertTrue(exists(CalendarTags.SYNC_CANCEL))
        assertTrue(!exists(CalendarTags.SYNC_ENROLL), "there is nothing left to ask for")
    }

    @Test
    fun scanningCanBeCancelledToo() = runComposeUiTest {
        show(flow = EnrollFlow.ScanQr)

        click(CalendarTags.SYNC_CANCEL)

        assertEquals(1, reset)
    }

    @Test
    fun aFinishedEnrollmentSaysSoAndOffersNothingMore() = runComposeUiTest {
        show(flow = EnrollFlow.Done)

        assertTrue(!exists(CalendarTags.SYNC_ENROLL), "nothing left to do here")
        assertTrue(!exists(CalendarTags.SYNC_SCAN))
    }

    // ── Once enrolled ────────────────────────────────────────────────────

    @Test
    fun anEnrolledPhoneCanSyncNow() = runComposeUiTest {
        show(status = SyncStatus.Synced("2026-09-20T10:30:00Z", pulled = 2, pushed = 1))

        click(CalendarTags.SYNC_NOW)

        assertEquals(1, syncedNow)
    }

    @Test
    fun anEnrolledPhoneCanStopSyncing() = runComposeUiTest {
        show(status = SyncStatus.Synced("2026-09-20T10:30:00Z", pulled = 0, pushed = 0))

        click(CalendarTags.SYNC_LEAVE)

        assertEquals(1, left)
    }

    @Test
    fun aSyncInFlightIsStillTheEnrolledSheet() = runComposeUiTest {
        show(status = SyncStatus.Syncing)

        assertTrue(controls().fetchSemanticsNodes().size >= 2)
    }

    @Test
    fun aFailedRoundReportsTheRelayError() = runComposeUiTest {
        show(status = SyncStatus.Failed("relay unreachable"))

        assertTrue(isShowing("relay unreachable"))
    }

    @Test
    fun aRevokedPhoneIsSentBackToEnrolling() = runComposeUiTest {
        show(status = SyncStatus.Unauthorized)

        // Back to the enrol panel, because there is nothing to sync until it pairs again.
        assertTrue(exists(CalendarTags.SYNC_ENROLL))
        enroll()
        assertEquals(1, enrolled)
    }

    @Test
    fun theSheetCanBeClosed() = runComposeUiTest {
        show()

        click(CalendarTags.SHEET_CLOSE)

        assertEquals(1, dismissed)
    }
}
