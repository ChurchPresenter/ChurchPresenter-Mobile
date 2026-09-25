package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.calendar.sync.SyncStatus
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.showScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

/** The month header's sync line: above the buttons, and a way into the sync sheet. */
@OptIn(ExperimentalTestApi::class)
class MonthHeaderSyncLineTest {

    private var syncTaps = 0

    private fun ComposeUiTest.show(status: SyncStatus) = showScreen {
        MonthHeader(
            serviceCount = 3,
            monthName = "September 2026",
            onToday = {},
            onBack = null,
            compact = false,
            onSync = { syncTaps++ },
            sync = CalendarSyncView(status, nextAt = null, now = Instant.fromEpochMilliseconds(0)),
            onSettings = {},
        )
    }

    @Test
    fun aSyncingPhoneSaysSoAndTheLineOpensTheSheet() = runComposeUiTest {
        show(SyncStatus.Syncing)

        assertTrue(exists(CalendarTags.SYNC_LINE))
        click(CalendarTags.SYNC_LINE)
        assertEquals(1, syncTaps)
    }

    @Test
    fun aPhoneThatIsNotEnrolledHasNoLine() = runComposeUiTest {
        show(SyncStatus.NotEnrolled)

        assertFalse(exists(CalendarTags.SYNC_LINE))
    }
}
