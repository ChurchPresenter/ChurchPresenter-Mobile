package com.church.presenter.churchpresentermobile.present

import kotlin.math.max
import kotlin.math.min

/** What a `Range` header asked for, against a file of a known size. */
sealed interface RangeRequest {
    /** No usable header: send the whole file with a 200. */
    data object Whole : RangeRequest

    /** A satisfiable range, inclusive at both ends: send it with a 206. */
    data class Part(val start: Long, val end: Long) : RangeRequest {
        val length: Long get() = end - start + 1
    }

    /** A range wholly outside the file: answer 416. */
    data object Unsatisfiable : RangeRequest
}

/** `bytes=a-b`, `bytes=a-` or `bytes=-n` — one range, nothing else. */
private val SINGLE_RANGE = Regex("""^bytes=(\d*)-(\d*)$""", RegexOption.IGNORE_CASE)

/**
 * Reads an HTTP `Range` header the way a video player sends it.
 *
 * Every player that streams a video — a TV's browser, AVPlayer, Android's
 * MediaPlayer — asks for byte ranges, and some (AVPlayer above all) refuse to
 * play at all from a server that answers a range with the whole file. Only the
 * single-range forms are honoured. Anything else — a multi-range request, a
 * malformed header — is answered with the whole file, which the spec allows and
 * which no player depends on.
 *
 * Pure so it can be tested without a socket; the server is one line of glue.
 */
fun parseByteRange(header: String?, size: Long): RangeRequest {
    val match = header?.trim()?.let(SINGLE_RANGE::matchEntire)
    if (match == null || size <= 0L) return RangeRequest.Whole
    val (first, last) = match.destructured
    return when {
        first.isEmpty() -> last.toLongOrNull()?.let { suffixOf(it, size) } ?: RangeRequest.Whole
        else -> first.toLongOrNull()?.let { spanOf(it, last.toLongOrNull(), last.isEmpty(), size) }
            ?: RangeRequest.Whole
    }
}

/** The final [count] bytes. */
private fun suffixOf(count: Long, size: Long): RangeRequest =
    if (count <= 0L) RangeRequest.Unsatisfiable else RangeRequest.Part(max(0L, size - count), size - 1)

/** [start] to [end], or to the end of the file when [open]. */
private fun spanOf(start: Long, end: Long?, open: Boolean, size: Long): RangeRequest {
    val lastByte = size - 1
    return when {
        start > lastByte -> RangeRequest.Unsatisfiable
        open -> RangeRequest.Part(start, lastByte)
        end == null || end < start -> RangeRequest.Whole
        else -> RangeRequest.Part(start, min(end, lastByte))
    }
}
