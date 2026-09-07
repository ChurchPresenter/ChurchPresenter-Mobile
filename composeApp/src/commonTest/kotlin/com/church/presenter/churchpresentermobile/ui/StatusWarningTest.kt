package com.church.presenter.churchpresentermobile.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.StatusWarning
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The words each server warning is shown with.
 *
 * Every warning sends the operator somewhere different — to the desktop's
 * device permissions, to its Bible import, to its version number — so a warning
 * resolved to the wrong words sends them to the wrong place, in the ten minutes
 * before a service. Each is checked to say something, and to say something
 * different from its neighbours.
 */
@OptIn(ExperimentalTestApi::class)
class StatusWarningTest {

    private fun details(warning: StatusWarning): WarningDetails {
        lateinit var resolved: WarningDetails
        runComposeUiTest {
            showScreen {
                resolved = warningDetails(warning)
                Text(resolved.title, modifier = Modifier.testTag("title"))
            }
            onNodeWithTag("title").fetchSemanticsNode()
        }
        return resolved
    }

    @Test
    fun aMissingApiKeyIsExplained() {
        val d = details(StatusWarning.NoApiKey)
        assertTrue(d.title.isNotBlank())
        assertTrue(d.body.isNotBlank())
    }

    @Test
    fun aDesktopWithNoBiblesIsExplained() {
        val d = details(StatusWarning.NoBibles)
        assertTrue(d.title.isNotBlank())
        assertTrue(d.body.isNotBlank())
    }

    @Test
    fun aDesktopWithNoSongbooksIsExplained() {
        val d = details(StatusWarning.NoSongbooks)
        assertTrue(d.title.isNotBlank())
        assertTrue(d.body.isNotBlank())
    }

    @Test
    fun aDeviceThatMayNotPresentIsExplained() {
        val d = details(StatusWarning.PresentBlocked)
        assertTrue(d.title.isNotBlank())
        assertTrue(d.body.isNotBlank())
    }

    @Test
    fun aDeviceThatMayNotScheduleIsExplained() {
        val d = details(StatusWarning.ScheduleBlocked)
        assertTrue(d.title.isNotBlank())
        assertTrue(d.body.isNotBlank())
    }

    @Test
    fun aDeviceThatMayNotUploadIsExplained() {
        val d = details(StatusWarning.UploadBlocked)
        assertTrue(d.title.isNotBlank())
        assertTrue(d.body.isNotBlank())
    }

    @Test
    fun anUnknownServerVersionIsExplained() {
        val d = details(StatusWarning.UnknownVersion("0.9"))
        assertTrue(d.title.isNotBlank())
        assertTrue(d.body.isNotBlank())
    }

    @Test
    fun anUnknownServerVersionQuotesTheVersion() {
        assertTrue(details(StatusWarning.UnknownVersion("0.9")).body.contains("0.9"))
    }

    @Test
    fun aVersionTheDesktopDidNotSendStillReadsAsSomething() {
        // Null is the common case for an old desktop, and "unknown" beats a
        // sentence with a hole in it.
        assertTrue(details(StatusWarning.UnknownVersion(null)).body.isNotBlank())
    }

    @Test
    fun aMissingEndpointIsExplained() {
        val d = details(StatusWarning.MissingEndpoint("songs"))
        assertTrue(d.title.isNotBlank())
        assertTrue(d.body.isNotBlank())
    }

    @Test
    fun aMissingEndpointIsNamedInTheTitle() {
        assertTrue(details(StatusWarning.MissingEndpoint("songs")).title.contains("songs"))
    }

    @Test
    fun aMissingEndpointIsNamedInTheBody() {
        assertTrue(details(StatusWarning.MissingEndpoint("bible")).body.contains("bible"))
    }

    @Test
    fun eachEndpointGetsItsOwnWarning() {
        assertNotEquals(
            details(StatusWarning.MissingEndpoint("songs")).title,
            details(StatusWarning.MissingEndpoint("bible")).title,
        )
    }

    @Test
    fun contentWarningsAreNotConfusedWithEachOther() {
        assertNotEquals(details(StatusWarning.NoBibles).title, details(StatusWarning.NoSongbooks).title)
    }

    @Test
    fun permissionWarningsAreNotConfusedWithEachOther() {
        // Three permissions, three different fixes on the desktop.
        val present = details(StatusWarning.PresentBlocked).title
        val schedule = details(StatusWarning.ScheduleBlocked).title
        val upload = details(StatusWarning.UploadBlocked).title
        assertNotEquals(present, schedule)
        assertNotEquals(schedule, upload)
        assertNotEquals(present, upload)
    }

    @Test
    fun aVersionNoteIsNotDressedAsAFailure() {
        // An old-but-working desktop is information, not an alarm.
        assertNotEquals(
            details(StatusWarning.UnknownVersion("0.9")).icon,
            details(StatusWarning.NoBibles).icon,
        )
    }

    @Test
    fun everyOtherWarningCarriesTheSameAlarmIcon() {
        val expected = details(StatusWarning.NoBibles).icon
        listOf(
            StatusWarning.NoApiKey,
            StatusWarning.NoSongbooks,
            StatusWarning.PresentBlocked,
            StatusWarning.ScheduleBlocked,
            StatusWarning.UploadBlocked,
            StatusWarning.MissingEndpoint("songs"),
        ).forEach { assertTrue(details(it).icon == expected, "wrong icon for $it") }
    }
}
