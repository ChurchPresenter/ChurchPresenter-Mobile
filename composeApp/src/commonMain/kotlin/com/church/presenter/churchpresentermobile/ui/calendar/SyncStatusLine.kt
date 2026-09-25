package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.calendar_sync_line_failed
import churchpresentermobile.composeapp.generated.resources.calendar_sync_line_stopped
import churchpresentermobile.composeapp.generated.resources.calendar_sync_next_in
import churchpresentermobile.composeapp.generated.resources.calendar_sync_next_soon
import churchpresentermobile.composeapp.generated.resources.calendar_sync_retry_in
import churchpresentermobile.composeapp.generated.resources.calendar_sync_retry_soon
import churchpresentermobile.composeapp.generated.resources.calendar_sync_status_syncing
import churchpresentermobile.composeapp.generated.resources.sync_days_ago
import churchpresentermobile.composeapp.generated.resources.sync_hours_ago
import churchpresentermobile.composeapp.generated.resources.sync_just_now
import churchpresentermobile.composeapp.generated.resources.sync_minutes_ago
import churchpresentermobile.composeapp.generated.resources.sync_never
import com.church.presenter.churchpresentermobile.calendar.sync.SyncStatus
import com.church.presenter.churchpresentermobile.model.LibrarySyncState
import com.church.presenter.churchpresentermobile.ui.library.SyncAge
import com.church.presenter.churchpresentermobile.ui.library.syncAgeFor
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Where this phone stands with the relay, when it next checks, and the time to count from: what the
 * headers' sync line shows. [now] is handed in rather than read, so a test or a screenshot is not
 * racing a real clock.
 */
internal data class CalendarSyncView(val status: SyncStatus, val nextAt: Instant?, val now: Instant)

/**
 * The wall clock, re-read every [TICK_MS] while [active] so the sync line's countdown moves.
 *
 * Idle when the phone is not syncing: an endless `delay` loop never lets a Compose test's clock
 * settle, so it runs only where there is something to count down.
 */
@Composable
internal fun rememberSyncClock(active: Boolean): Instant {
    val now by produceState(Clock.System.now(), active) {
        value = Clock.System.now()
        while (active) {
            delay(TICK_MS)
            value = Clock.System.now()
        }
    }
    return now
}

/**
 * What the sync line says, as plain values.
 *
 * @param age How long ago the last good round was; null when the line is not about one.
 * @param nextMinutes Whole minutes until the next timed round, rounded up; 0 when it is under a
 *   minute away, null when none is waiting.
 */
internal data class SyncLine(val kind: Kind, val age: SyncAge?, val nextMinutes: Long?) {
    enum class Kind { SYNCING, SYNCED, FAILED, STOPPED }
}

/**
 * The line for [status] as of [now], or null when there is nothing to say — a phone that is not
 * enrolled already shows a crossed-out cloud, and a fresh open has not finished its first round.
 *
 * Split from [SyncStatusLine] because the rounding is the part that can be wrong, and a composable
 * that reads the wall clock cannot be checked without waiting out the interval.
 */
internal fun syncLineFor(status: SyncStatus, nextAt: Instant?, now: Instant): SyncLine? {
    val next = nextAt?.let { at ->
        val leftMs = (at - now).inWholeMilliseconds.coerceAtLeast(0L)
        if (leftMs < MINUTE_MS) 0L else (leftMs + MINUTE_MS - 1) / MINUTE_MS
    }
    return when (status) {
        SyncStatus.NotEnrolled -> null
        SyncStatus.Syncing -> SyncLine(SyncLine.Kind.SYNCING, null, null)
        SyncStatus.Unauthorized -> SyncLine(SyncLine.Kind.STOPPED, null, null)
        is SyncStatus.Failed -> SyncLine(SyncLine.Kind.FAILED, null, next)
        is SyncStatus.Synced -> {
            val at = runCatching { Instant.parse(status.at) }.getOrNull() ?: return null
            val last = LibrarySyncState(lastSyncEpochMs = at.toEpochMilliseconds())
            val age = syncAgeFor(last, now.toEpochMilliseconds())
            SyncLine(SyncLine.Kind.SYNCED, age, next)
        }
    }
}

/** "Synced 2 min ago · next in 3 min", small, in the room above a header's buttons. */
@Composable
internal fun SyncStatusLine(sync: CalendarSyncView, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val colors = LocalAppColors.current
    val line = syncLineFor(sync.status, sync.nextAt, sync.now) ?: return
    val text = listOfNotNull(firstPart(line), nextPart(line)).joinToString(" · ")
    val failing = line.kind == SyncLine.Kind.FAILED || line.kind == SyncLine.Kind.STOPPED
    Text(
        text,
        color = if (failing) colors.danger else colors.muted,
        fontSize = 11.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .testTag(CalendarTags.SYNC_LINE)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    )
}

/**
 * A header's [row], with the sync line right-aligned in a strip over it — above the header's
 * buttons, where nothing else is drawn. On its own strip rather than stacked on the buttons, so it
 * never takes width from the title beside them.
 */
@Composable
internal fun SyncLineAbove(
    sync: CalendarSyncView?,
    onSync: (() -> Unit)?,
    modifier: Modifier = Modifier,
    row: @Composable RowScope.() -> Unit,
) {
    Column(modifier = modifier) {
        if (sync != null) {
            SyncStatusLine(sync, modifier = Modifier.align(Alignment.End).padding(bottom = 2.dp), onClick = onSync)
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, content = row)
    }
}

@Composable
private fun firstPart(line: SyncLine): String = when (line.kind) {
    SyncLine.Kind.SYNCING -> stringResource(Res.string.calendar_sync_status_syncing)
    SyncLine.Kind.FAILED -> stringResource(Res.string.calendar_sync_line_failed)
    SyncLine.Kind.STOPPED -> stringResource(Res.string.calendar_sync_line_stopped)
    SyncLine.Kind.SYNCED -> {
        val age = line.age ?: SyncAge(SyncAge.Bucket.NEVER, 0L)
        when (age.bucket) {
            SyncAge.Bucket.NEVER -> stringResource(Res.string.sync_never)
            SyncAge.Bucket.JUST_NOW -> stringResource(Res.string.sync_just_now)
            SyncAge.Bucket.MINUTES -> stringResource(Res.string.sync_minutes_ago, age.count.toString())
            SyncAge.Bucket.HOURS -> stringResource(Res.string.sync_hours_ago, age.count.toString())
            SyncAge.Bucket.DAYS -> stringResource(Res.string.sync_days_ago, age.count.toString())
        }
    }
}

@Composable
private fun nextPart(line: SyncLine): String? {
    val minutes = line.nextMinutes ?: return null
    val retry = line.kind == SyncLine.Kind.FAILED
    return when {
        minutes == 0L && retry -> stringResource(Res.string.calendar_sync_retry_soon)
        minutes == 0L -> stringResource(Res.string.calendar_sync_next_soon)
        retry -> stringResource(Res.string.calendar_sync_retry_in, minutes.toString())
        else -> stringResource(Res.string.calendar_sync_next_in, minutes.toString())
    }
}

private const val MINUTE_MS = 60_000L
private const val TICK_MS = 15_000L
