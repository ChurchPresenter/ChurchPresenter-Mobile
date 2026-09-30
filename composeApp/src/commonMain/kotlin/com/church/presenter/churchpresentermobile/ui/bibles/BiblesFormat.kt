package com.church.presenter.churchpresentermobile.ui.bibles


/** 1,248 — digits grouped by thousands, which common Kotlin has no formatter for. */
internal fun grouped(value: Int): String {
    val digits = value.toString()
    if (value < GROUP_SIZE_LIMIT) return digits
    return digits.reversed().chunked(GROUP_WIDTH).joinToString(",").reversed()
}

/** 4.6 MB / 212 KB — decimal units, as the phone's own file browser counts them. */
internal fun sizeLabel(bytes: Long): String = when {
    bytes >= BYTES_PER_MB -> {
        val tenths = (bytes * TENTHS + BYTES_PER_MB / 2) / BYTES_PER_MB
        "${tenths / TENTHS}.${tenths % TENTHS} MB"
    }
    else -> "${(bytes + BYTES_PER_KB / 2) / BYTES_PER_KB} KB"
}

private const val GROUP_SIZE_LIMIT = 1000

private const val GROUP_WIDTH = 3

private const val BYTES_PER_KB = 1000L

private const val BYTES_PER_MB = 1000L * 1000L

private const val TENTHS = 10L
