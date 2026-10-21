package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.json.Json

/**
 * How a [SlideTheme] is written to and read from settings.
 *
 * One codec for every screen that saves the look — the Present tab's Look sheet and Display
 * text both persist the same blob, and two encoders with different options is how one of them
 * ends up unable to read what the other wrote.
 */
internal object SlideThemeCodec {
    /** Lenient so a theme written by a newer build still opens on an older one. */
    val json: Json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    fun encode(theme: SlideTheme): String = json.encodeToString(SlideTheme.serializer(), theme)

    fun decode(stored: String): SlideTheme? =
        runCatching { json.decodeFromString(SlideTheme.serializer(), stored) }.getOrNull()
}
