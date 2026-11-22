package com.church.presenter.churchpresentermobile.ui

import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.settings_server_checking
import churchpresentermobile.composeapp.generated.resources.settings_server_connected
import churchpresentermobile.composeapp.generated.resources.settings_server_limit_all
import churchpresentermobile.composeapp.generated.resources.settings_server_limit_bibles
import churchpresentermobile.composeapp.generated.resources.settings_server_limit_library
import churchpresentermobile.composeapp.generated.resources.settings_server_limit_present
import churchpresentermobile.composeapp.generated.resources.settings_server_limit_songbooks
import churchpresentermobile.composeapp.generated.resources.settings_server_limit_update
import churchpresentermobile.composeapp.generated.resources.settings_server_limit_upload
import churchpresentermobile.composeapp.generated.resources.settings_server_limited
import churchpresentermobile.composeapp.generated.resources.settings_server_rejected
import churchpresentermobile.composeapp.generated.resources.settings_server_unreachable
import churchpresentermobile.composeapp.generated.resources.settings_server_wrong
import com.church.presenter.churchpresentermobile.model.ServerStatus
import com.church.presenter.churchpresentermobile.model.StatusWarning
import com.church.presenter.churchpresentermobile.viewmodel.StatusUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerVerdictTest {

    private fun success(vararg warnings: StatusWarning) =
        StatusUiState.Success(ServerStatus(appVersion = "1.4.2"), warnings.toList())

    @Test
    fun aHealthyDesktopIsConnectedWithNothingToFix() {
        val verdict = serverVerdict(success())
        assertEquals(ConnectionTone.GOOD, verdict.tone)
        assertEquals(Res.string.settings_server_connected, verdict.title)
        assertTrue(verdict.reasons.isEmpty())
        assertEquals(FieldBlame.NONE, verdict.blame)
    }

    @Test
    fun warningsMakeItLimited() {
        val verdict = serverVerdict(success(StatusWarning.NoBibles))
        assertEquals(ConnectionTone.WARNING, verdict.tone)
        assertEquals(Res.string.settings_server_limited, verdict.title)
    }

    @Test
    fun anUnreachableDesktopBlamesTheHostAndOffersToTryAgain() {
        val verdict = serverVerdict(StatusUiState.Error("refused"))
        assertEquals(ConnectionTone.BAD, verdict.tone)
        assertEquals(Res.string.settings_server_unreachable, verdict.title)
        assertEquals(FieldBlame.HOST, verdict.blame)
        assertTrue(verdict.retry)
        assertEquals(3, verdict.reasons.size)
    }

    @Test
    fun aRefusedKeyBlamesTheKey() {
        val verdict = serverVerdict(StatusUiState.Unauthorized(401))
        assertEquals(Res.string.settings_server_rejected, verdict.title)
        assertEquals(FieldBlame.KEY, verdict.blame)
        assertFalse(verdict.retry)
    }

    @Test
    fun aStrangerIsNamedAsSuch() {
        val verdict = serverVerdict(StatusUiState.NotChurchPresenter("nginx"))
        assertEquals(Res.string.settings_server_wrong, verdict.title)
        assertEquals(ConnectionTone.BAD, verdict.tone)
    }

    @Test
    fun aCheckInFlightIsPending() {
        val verdict = serverVerdict(StatusUiState.Loading)
        assertEquals(ConnectionTone.PENDING, verdict.tone)
        assertEquals(Res.string.settings_server_checking, verdict.title)
    }

    @Test
    fun everyPermissionBlockedIsOneSentence() {
        val reasons = limitReasons(
            listOf(StatusWarning.PresentBlocked, StatusWarning.ScheduleBlocked, StatusWarning.UploadBlocked),
        )
        assertEquals(listOf(Res.string.settings_server_limit_all), reasons)
    }

    @Test
    fun somePermissionsBlockedAreNamedOneByOne() {
        val reasons = limitReasons(listOf(StatusWarning.PresentBlocked, StatusWarning.UploadBlocked))
        assertEquals(
            listOf(Res.string.settings_server_limit_present, Res.string.settings_server_limit_upload),
            reasons,
        )
    }

    @Test
    fun noBibleAndNoSongbookAreOneSentence() {
        assertEquals(
            listOf(Res.string.settings_server_limit_library),
            limitReasons(listOf(StatusWarning.NoBibles, StatusWarning.NoSongbooks)),
        )
    }

    @Test
    fun aMissingBibleOrSongbookIsNamedAlone() {
        assertEquals(listOf(Res.string.settings_server_limit_bibles), limitReasons(listOf(StatusWarning.NoBibles)))
        assertEquals(
            listOf(Res.string.settings_server_limit_songbooks),
            limitReasons(listOf(StatusWarning.NoSongbooks)),
        )
    }

    @Test
    fun anOldOrIncompleteDesktopAsksForAnUpdateOnce() {
        val reasons = limitReasons(
            listOf(StatusWarning.UnknownVersion("0.9"), StatusWarning.MissingEndpoint("bible")),
        )
        assertEquals(listOf(Res.string.settings_server_limit_update), reasons)
    }

    @Test
    fun aMissingKeyIsNotALimitOnThisDevice() {
        assertTrue(limitReasons(listOf(StatusWarning.NoApiKey)).isEmpty())
    }
}
