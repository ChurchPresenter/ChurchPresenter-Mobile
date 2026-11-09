package com.church.presenter.churchpresentermobile.ui

/**
 * Rewrites every paragraph separator other than `\n` as `\n`, so the text can be laid out in a
 * text field without crashing on Android.
 *
 * Compose splits text into paragraphs only at `\n`, but ICU's Bidi also ends a paragraph at a
 * lone `\r`, U+001C–U+001E, U+0085 (NEL) and U+2029. A visual line that spans one of those makes
 * `Bidi.setLine` throw `IllegalArgumentException` (no message). Compose reaches it from
 * `getHorizontalPosition` when it draws the cursor, which happens during the frame and takes the
 * whole app down. Lyrics typed on Windows or old Mac files, and text pasted from other apps,
 * bring these characters in.
 *
 * The rewrite keeps the length, so cursor and selection offsets stay valid. A `\r` that comes
 * before `\n` is left alone, because ICU treats CRLF as a single separator.
 */
internal fun String.layoutSafe(): String {
    if (none { it.isStrayParagraphSeparator() }) return this
    val chars = toCharArray()
    for (i in chars.indices) {
        val c = chars[i]
        val isCrlf = c == '\r' && chars.getOrNull(i + 1) == '\n'
        if (c.isStrayParagraphSeparator() && !isCrlf) chars[i] = '\n'
    }
    return chars.concatToString()
}

private fun Char.isStrayParagraphSeparator(): Boolean =
    this == '\r' || this in '\u001C'..'\u001E' || this == '\u0085' || this == ' '
