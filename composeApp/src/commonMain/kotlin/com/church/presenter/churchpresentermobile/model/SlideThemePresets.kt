package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.Serializable

/** A look with a name on it — a built-in preset, or one a church saved. */
@Serializable
data class NamedTheme(val name: String, val theme: SlideTheme)

/**
 * Looks to start from.
 *
 * A church setting up on a Sunday morning should not have to build a readable
 * screen out of six separate colour fields. These are whole [SlideTheme]s, so
 * applying one is a single assignment and anything can be adjusted afterwards.
 *
 * The first entry is the app's own defaults, which makes "put it back" a tap
 * rather than a memory test.
 */
object SlideThemePresets {

    val all: List<NamedTheme> = listOf(
        NamedTheme("Default", SlideTheme()),
        NamedTheme(
            "Black",
            SlideTheme(
                gradientTop = "#000000",
                gradientBottom = "#000000",
                textColor = "#FFFFFF",
                accentColor = "#9AA0A6",
            ),
        ),
        NamedTheme(
            "Paper",
            // For a bright room, where white-on-dark washes out.
            SlideTheme(
                gradientTop = "#FAF7F0",
                gradientBottom = "#EDE7DA",
                textColor = "#1A1A1A",
                accentColor = "#7A5C2E",
            ),
        ),
        NamedTheme(
            "Midnight",
            SlideTheme(
                gradientTop = "#0B1F3A",
                gradientBottom = "#02060F",
                textColor = "#FFFFFF",
                accentColor = "#5AA9E6",
            ),
        ),
        NamedTheme(
            "Warm",
            SlideTheme(
                gradientTop = "#3B1F1F",
                gradientBottom = "#120806",
                textColor = "#FFF3E6",
                accentColor = "#E8A33D",
            ),
        ),
    )
}
