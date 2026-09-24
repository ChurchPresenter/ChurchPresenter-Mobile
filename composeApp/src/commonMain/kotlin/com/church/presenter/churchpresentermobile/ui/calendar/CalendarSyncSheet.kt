package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.calendar_cancel
import churchpresentermobile.composeapp.generated.resources.calendar_sync_denied
import churchpresentermobile.composeapp.generated.resources.calendar_sync_desktop_off
import churchpresentermobile.composeapp.generated.resources.calendar_sync_done
import churchpresentermobile.composeapp.generated.resources.calendar_sync_enroll
import churchpresentermobile.composeapp.generated.resources.calendar_sync_enroll_needs_desktop
import churchpresentermobile.composeapp.generated.resources.calendar_sync_or_ask
import churchpresentermobile.composeapp.generated.resources.calendar_sync_failed
import churchpresentermobile.composeapp.generated.resources.calendar_sync_intro
import churchpresentermobile.composeapp.generated.resources.calendar_sync_leave
import churchpresentermobile.composeapp.generated.resources.calendar_sync_now
import churchpresentermobile.composeapp.generated.resources.calendar_sync_qr_invalid
import churchpresentermobile.composeapp.generated.resources.calendar_sync_scan_body
import churchpresentermobile.composeapp.generated.resources.calendar_sync_scan_title
import churchpresentermobile.composeapp.generated.resources.calendar_sync_status_failed
import churchpresentermobile.composeapp.generated.resources.calendar_sync_status_synced
import churchpresentermobile.composeapp.generated.resources.calendar_sync_status_syncing
import churchpresentermobile.composeapp.generated.resources.calendar_sync_status_unauthorized
import churchpresentermobile.composeapp.generated.resources.calendar_sync_title
import churchpresentermobile.composeapp.generated.resources.calendar_sync_waiting_body
import churchpresentermobile.composeapp.generated.resources.calendar_sync_waiting_title
import churchpresentermobile.composeapp.generated.resources.calendar_try_again
import com.church.presenter.churchpresentermobile.calendar.sync.SyncStatus
import com.church.presenter.churchpresentermobile.ui.QrScanButton
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.EnrollFlow
import org.jetbrains.compose.resources.stringResource

/**
 * Enrolling with the church computer and, once enrolled, how the sync is doing: the code the
 * operator compares with their prompt, then the scanner for the QR the computer shows.
 */
@Composable
internal fun CalendarSyncSheet(
    status: SyncStatus,
    flow: EnrollFlow,
    canReachDesktop: Boolean,
    actions: SyncActions,
    onDismiss: () -> Unit,
) {
    CalendarSheet(onDismiss) {
        CalendarSyncSheetContent(status, flow, canReachDesktop, actions, onDismiss)
    }
}

/**
 * What the sheet says, without the sheet around it.
 *
 * Separate so that each state can be photographed: a modal sheet is its own window and a capture
 * of the screen behind it shows nothing of what it is covering.
 */
@Composable
internal fun CalendarSyncSheetContent(
    status: SyncStatus,
    flow: EnrollFlow,
    canReachDesktop: Boolean,
    actions: SyncActions,
    onDismiss: () -> Unit,
) {
    SheetTitle(stringResource(Res.string.calendar_sync_title), onClose = onDismiss)
    Spacer(Modifier.height(12.dp))
    when {
        status is SyncStatus.NotEnrolled || status is SyncStatus.Unauthorized ->
            EnrollPanel(status, flow, canReachDesktop, actions)
        else -> EnrolledPanel(status, actions)
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun EnrollPanel(status: SyncStatus, flow: EnrollFlow, canReachDesktop: Boolean, actions: SyncActions) {
    val colors = LocalAppColors.current
    when (flow) {
        EnrollFlow.Idle, EnrollFlow.Denied, EnrollFlow.SyncOff, is EnrollFlow.Failed -> {
            if (status is SyncStatus.Unauthorized) {
                Text(
                    stringResource(Res.string.calendar_sync_status_unauthorized),
                    color = colors.danger,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(stringResource(Res.string.calendar_sync_intro), color = colors.secondary, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            when (flow) {
                EnrollFlow.Denied -> HintText(stringResource(Res.string.calendar_sync_denied))
                EnrollFlow.SyncOff -> HintText(stringResource(Res.string.calendar_sync_desktop_off))
                is EnrollFlow.Failed -> HintText(
                    if (flow.message.isEmpty()) {
                        stringResource(Res.string.calendar_sync_qr_invalid)
                    } else {
                        stringResource(Res.string.calendar_sync_failed, flow.message)
                    },
                )
                else -> Unit
            }
            Spacer(Modifier.height(8.dp))
            // The usual way in: an invite the church computer shows, scanned from anywhere.
            QrScanButton(
                onScanned = actions.onScanned,
                modifier = Modifier.fillMaxWidth().testTag(SheetTags.SYNC_SCAN),
            )
            Spacer(Modifier.height(10.dp))
            HintText(stringResource(Res.string.calendar_sync_or_ask))
            if (!canReachDesktop) HintText(stringResource(Res.string.calendar_sync_enroll_needs_desktop))
            Spacer(Modifier.height(6.dp))
            CalendarSecondaryButton(
                label = stringResource(
                    if (flow == EnrollFlow.Idle) Res.string.calendar_sync_enroll else Res.string.calendar_try_again,
                ),
                onClick = actions.onEnroll,
                enabled = canReachDesktop,
                modifier = Modifier.fillMaxWidth().testTag(SheetTags.SYNC_ENROLL),
            )
        }
        is EnrollFlow.WaitingForApproval -> {
            TitleText(stringResource(Res.string.calendar_sync_waiting_title), size = 16)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(Res.string.calendar_sync_waiting_body), color = colors.secondary, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = flow.code.chunked(CODE_GROUP).joinToString(" "),
                color = colors.text,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.08.em,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            CalendarSecondaryButton(
                stringResource(Res.string.calendar_cancel),
                onClick = actions.onReset,
                modifier = Modifier.fillMaxWidth().testTag(SheetTags.SYNC_CANCEL),
            )
        }
        EnrollFlow.ScanQr -> {
            TitleText(stringResource(Res.string.calendar_sync_scan_title), size = 16)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(Res.string.calendar_sync_scan_body), color = colors.secondary, fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))
            QrScanButton(
                onScanned = actions.onScanned,
                modifier = Modifier.fillMaxWidth().testTag(SheetTags.SYNC_SCAN),
            )
            Spacer(Modifier.height(8.dp))
            CalendarSecondaryButton(
                stringResource(Res.string.calendar_cancel),
                onClick = actions.onReset,
                modifier = Modifier.fillMaxWidth().testTag(SheetTags.SYNC_CANCEL),
            )
        }
        EnrollFlow.Done -> {
            Text(
                stringResource(Res.string.calendar_sync_done),
                color = colors.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun EnrolledPanel(status: SyncStatus, actions: SyncActions) {
    val colors = LocalAppColors.current
    val (line, color) = when (status) {
        is SyncStatus.Synced -> stringResource(
            Res.string.calendar_sync_status_synced,
            status.at.take(TIME_CHARS).replace('T', ' '),
        ) to colors.accent
        SyncStatus.Syncing -> stringResource(Res.string.calendar_sync_status_syncing) to colors.muted
        is SyncStatus.Failed -> stringResource(Res.string.calendar_sync_status_failed, status.message) to colors.danger
        else -> "" to colors.muted
    }
    Text(line, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        CalendarPrimaryButton(
            stringResource(Res.string.calendar_sync_now),
            onClick = actions.onSyncNow,
            modifier = Modifier.weight(1f).testTag(SheetTags.SYNC_NOW),
        )
        CalendarSecondaryButton(
            stringResource(Res.string.calendar_sync_leave),
            onClick = actions.onLeave,
            modifier = Modifier.testTag(SheetTags.SYNC_LEAVE),
        )
    }
}

private const val CODE_GROUP = 3
private const val TIME_CHARS = 16
