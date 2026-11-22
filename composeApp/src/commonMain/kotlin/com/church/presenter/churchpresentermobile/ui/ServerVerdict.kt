package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.layout.size
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.settings_server_checking
import churchpresentermobile.composeapp.generated.resources.settings_server_connected
import churchpresentermobile.composeapp.generated.resources.settings_server_limit_all
import churchpresentermobile.composeapp.generated.resources.settings_server_limit_bibles
import churchpresentermobile.composeapp.generated.resources.settings_server_limit_library
import churchpresentermobile.composeapp.generated.resources.settings_server_limit_present
import churchpresentermobile.composeapp.generated.resources.settings_server_limit_schedule
import churchpresentermobile.composeapp.generated.resources.settings_server_limit_songbooks
import churchpresentermobile.composeapp.generated.resources.settings_server_limit_update
import churchpresentermobile.composeapp.generated.resources.settings_server_limit_upload
import churchpresentermobile.composeapp.generated.resources.settings_server_limited
import churchpresentermobile.composeapp.generated.resources.settings_server_rejected
import churchpresentermobile.composeapp.generated.resources.settings_server_rejected_clear
import churchpresentermobile.composeapp.generated.resources.settings_server_rejected_copy
import churchpresentermobile.composeapp.generated.resources.settings_server_unreachable
import churchpresentermobile.composeapp.generated.resources.settings_server_unreachable_address
import churchpresentermobile.composeapp.generated.resources.settings_server_unreachable_open
import churchpresentermobile.composeapp.generated.resources.settings_server_unreachable_wifi
import churchpresentermobile.composeapp.generated.resources.settings_server_wrong
import churchpresentermobile.composeapp.generated.resources.settings_server_wrong_answered
import churchpresentermobile.composeapp.generated.resources.settings_server_wrong_check
import com.church.presenter.churchpresentermobile.model.StatusWarning
import com.church.presenter.churchpresentermobile.viewmodel.StatusUiState
import org.jetbrains.compose.resources.StringResource

internal enum class FieldBlame { NONE, HOST, KEY }

internal class ServerVerdict(
    val tone: ConnectionTone,
    val title: StringResource,
    val reasons: List<StringResource>,
    val retry: Boolean,
    val blame: FieldBlame,
)

internal fun serverVerdict(state: StatusUiState): ServerVerdict = when (state) {
    is StatusUiState.Loading ->
        ServerVerdict(ConnectionTone.PENDING, Res.string.settings_server_checking, emptyList(), false, FieldBlame.NONE)
    is StatusUiState.Error -> ServerVerdict(
        ConnectionTone.BAD,
        Res.string.settings_server_unreachable,
        listOf(
            Res.string.settings_server_unreachable_open,
            Res.string.settings_server_unreachable_wifi,
            Res.string.settings_server_unreachable_address,
        ),
        retry = true,
        blame = FieldBlame.HOST,
    )
    is StatusUiState.Unauthorized -> ServerVerdict(
        ConnectionTone.BAD,
        Res.string.settings_server_rejected,
        listOf(Res.string.settings_server_rejected_copy, Res.string.settings_server_rejected_clear),
        retry = false,
        blame = FieldBlame.KEY,
    )
    is StatusUiState.NotChurchPresenter -> ServerVerdict(
        ConnectionTone.BAD,
        Res.string.settings_server_wrong,
        listOf(Res.string.settings_server_wrong_answered, Res.string.settings_server_wrong_check),
        retry = false,
        blame = FieldBlame.NONE,
    )
    is StatusUiState.Success -> if (state.warnings.isEmpty()) {
        ServerVerdict(ConnectionTone.GOOD, Res.string.settings_server_connected, emptyList(), false, FieldBlame.NONE)
    } else {
        ServerVerdict(
            ConnectionTone.WARNING,
            Res.string.settings_server_limited,
            limitReasons(state.warnings),
            retry = false,
            blame = FieldBlame.NONE,
        )
    }
}

internal fun limitReasons(warnings: List<StatusWarning>): List<StringResource> = buildList {
    val blocked = listOf(
        StatusWarning.PresentBlocked to Res.string.settings_server_limit_present,
        StatusWarning.ScheduleBlocked to Res.string.settings_server_limit_schedule,
        StatusWarning.UploadBlocked to Res.string.settings_server_limit_upload,
    ).filter { it.first in warnings }
    if (blocked.size == BLOCKABLE_PERMISSIONS) {
        add(Res.string.settings_server_limit_all)
    } else {
        blocked.forEach { add(it.second) }
    }
    val noBibles = StatusWarning.NoBibles in warnings
    val noSongbooks = StatusWarning.NoSongbooks in warnings
    when {
        noBibles && noSongbooks -> add(Res.string.settings_server_limit_library)
        noBibles -> add(Res.string.settings_server_limit_bibles)
        noSongbooks -> add(Res.string.settings_server_limit_songbooks)
    }
    if (warnings.any { it is StatusWarning.UnknownVersion || it is StatusWarning.MissingEndpoint }) {
        add(Res.string.settings_server_limit_update)
    }
}

private const val BLOCKABLE_PERMISSIONS = 3
