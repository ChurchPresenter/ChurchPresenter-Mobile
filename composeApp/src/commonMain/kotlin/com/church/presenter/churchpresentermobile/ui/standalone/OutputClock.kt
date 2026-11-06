package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

private const val MINUTE_MS = 60_000L

/** The time a screenshot test draws, so a picture does not change with the hour it was taken. */
internal val LocalFixedClock = staticCompositionLocalOf<String?> { null }

/** The clock's words: 24-hour, as the browser page shows them. */
internal fun clockText(time: LocalDateTime): String =
    "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"

private fun clockTextNow(): String =
    clockText(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))

/**
 * The time, kept current for as long as it is on screen, or null when [show] is
 * false. It waits off the main thread so a UI test's clock does not wait on it.
 */
@Composable
internal fun rememberClockText(show: Boolean): String? {
    val fixed = LocalFixedClock.current
    val current by produceState(initialValue = clockTextNow()) {
        withContext(Dispatchers.Default) {
            while (true) {
                delay(MINUTE_MS - Clock.System.now().toEpochMilliseconds() % MINUTE_MS)
                value = clockTextNow()
            }
        }
    }
    return if (show) fixed ?: current else null
}
