package com.church.presenter.churchpresentermobile.model

import kotlinx.serialization.Serializable

/** A look with a name on it — a built-in preset, or one a church saved. */
@Serializable
data class NamedTheme(val name: String, val theme: SlideTheme)

/**
 * Looks to start from.
 *
 * A church setting up on a Sunday morning should not have to build a readable
 * screen out of six separate color fields. These are whole [SlideTheme]s, so
 * applying one is a single assignment and anything can be adjusted afterwards.
 *
 * The first entry is the app's own defaults, which makes "put it back" a tap
 * rather than a memory test. Each starts the words from a fixed line, top or
 * bottom, so they stay put when one slide has more lines than the next; middled
 * words cannot, because the middle of a block moves with its height.
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
                verticalAlign = SlideVerticalAlign.TOP,
                margin = SlideMargin.THICK,
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
                verticalAlign = SlideVerticalAlign.TOP,
                margin = SlideMargin.THICK,
            ),
        ),
        NamedTheme(
            "Midnight",
            SlideTheme(
                gradientTop = "#0B1F3A",
                gradientBottom = "#02060F",
                textColor = "#FFFFFF",
                accentColor = "#5AA9E6",
                verticalAlign = SlideVerticalAlign.TOP,
                margin = SlideMargin.THICK,
            ),
        ),
        NamedTheme(
            "Warm",
            SlideTheme(
                gradientTop = "#3B1F1F",
                gradientBottom = "#120806",
                textColor = "#FFF3E6",
                accentColor = "#E8A33D",
                verticalAlign = SlideVerticalAlign.TOP,
                margin = SlideMargin.THICK,
            ),
        ),
        NamedTheme(
            "Lower third",
            // Words along the bottom, for a screen that also shows a stage or a camera.
            SlideTheme(
                gradientTop = "#000000",
                gradientBottom = "#1C1C1E",
                textColor = "#FFFFFF",
                accentColor = "#9AA0A6",
                font = SlideFont.SANS,
                verticalAlign = SlideVerticalAlign.BOTTOM,
            ),
        ),
    )
}
