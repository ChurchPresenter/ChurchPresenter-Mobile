package com.church.presenter.churchpresentermobile.ui.standalone

import com.church.presenter.churchpresentermobile.model.LocalVideo
import kotlin.math.min
import kotlin.math.roundToLong

private const val KB = 1024.0
private const val TENTHS = 10.0

/** "MP4 · 1080p · 212 MB", with "· loops" when it is set to. */
internal fun videoDetails(video: LocalVideo, looping: Boolean, loopsLabel: String): String =
    listOfNotNull(
        video.format.ifBlank { null },
        resolutionLabel(video.width, video.height),
        formatBytes(video.sizeBytes).takeIf { video.sizeBytes > 0L },
        loopsLabel.takeIf { looping },
    ).joinToString(" · ")

/** "4K", "1080p", "720p" — named by the shorter side, so an upright phone clip reads as what it is. */
internal fun resolutionLabel(width: Int, height: Int): String? {
    val short = min(width, height)
    return when {
        short <= 0 -> null
        short >= UHD_SHORT_SIDE -> "4K"
        else -> "${short}p"
    }
}

private const val UHD_SHORT_SIDE = 2160

/** "212 MB", "1.2 GB" — one decimal only where it says something. */
internal fun formatBytes(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= KB && unit < units.lastIndex) {
        value /= KB
        unit++
    }
    val shown = if (unit >= 3 && value < TENTHS) {
        val tenths = (value * TENTHS).roundToLong()
        "${tenths / 10}.${tenths % 10}"
    } else {
        value.roundToLong().toString()
    }
    // A non-breaking space: "212" and "MB" on separate lines read as two facts.
    return "$shown\u00A0${units[unit]}"
}
