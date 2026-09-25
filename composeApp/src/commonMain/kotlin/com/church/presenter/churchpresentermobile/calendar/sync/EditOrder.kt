package com.church.presenter.churchpresentermobile.calendar.sync

import kotlin.time.Instant

/**
 * Which of two copies of one service is newer -- SYNC.md, *Which copy wins*. More edits win; on a
 * tie, the later edit. Both come from inside the seal, so the relay can move neither.
 */
internal object EditOrder {

    fun outranks(version: Long, editedAt: String, otherVersion: Long, otherEditedAt: String): Boolean =
        version > otherVersion || (version == otherVersion && instant(editedAt) > instant(otherEditedAt))

    /** A sealed edit time as this phone keeps it: parsed, and never later than [now]. */
    fun clamped(editedAt: String, now: String): String {
        val at = parse(editedAt) ?: return ""
        val limit = parse(now) ?: return at.toString()
        return minOf(at, limit).toString()
    }

    private fun instant(text: String): Instant = parse(text) ?: Instant.DISTANT_PAST

    private fun parse(text: String): Instant? = runCatching { Instant.parse(text) }.getOrNull()
}
