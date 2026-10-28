package com.church.presenter.churchpresentermobile.screenshot

import com.church.presenter.churchpresentermobile.model.CanvasDocument
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasInk
import com.church.presenter.churchpresentermobile.model.CanvasPage
import com.church.presenter.churchpresentermobile.model.CanvasShape
import com.church.presenter.churchpresentermobile.model.CanvasTextStyle
import com.church.presenter.churchpresentermobile.model.InkKind
import com.church.presenter.churchpresentermobile.model.ShapeFill

/**
 * The tablet design's "Sunday sermon sketch": the page every canvas mock-up is
 * drawn on, rebuilt as a [CanvasDocument] so a golden can be held against the
 * design picture for picture.
 *
 * Positions and stroke paths are the design markup's own, in board pixels at
 * 100%: the title at (150, 64), the photo card at (150, 240) and so on, and the
 * ink as the same SVG path data the design draws. Listed bottom-most first, so
 * the Layers panel reads top-down as the design's does.
 */
internal object CanvasSketch {
    const val TITLE = "title"
    const val UNDERLINE = "underline"
    const val PSALM = "psalm"
    const val PHOTO = "photo"
    const val ARROW = "arrow"
    const val ELLIPSE = "ellipse"
    const val STICKY = "sticky"
    const val QUOTE = "quote"
    const val INK_A = "ink-a"
    const val INK_QUESTION = "ink-question"

    private val elements: List<CanvasElement> = listOf(
        CanvasElement.Ink(
            id = INK_A,
            x = 760f,
            y = 470f,
            pathData = "M30 190c10-50 20-110 50-150M80 40c10 30 18 70 26 120M40 120h70",
            ink = CanvasInk.RED,
            width = 5f,
            label = "Ink sketch",
        ),
        CanvasElement.Ink(
            id = INK_QUESTION,
            x = 760f,
            y = 470f,
            // The question mark, and its dot as a zero-length stroke drawn by its round cap.
            pathData = "M140 70c22-6 46 2 54 22s-6 40-30 44-36 26-28 46M135 212l0.01 0",
            ink = CanvasInk.AMBER,
            width = 5f,
        ),
        CanvasElement.Shape(
            id = QUOTE,
            x = 420f,
            y = 520f,
            width = 300f,
            height = 150f,
            shape = CanvasShape.ROUNDED,
            ink = CanvasInk.GREEN,
            text = "\"I lack nothing\"",
            fill = ShapeFill.GREEN,
            borderWidth = 3f,
            label = "Quote box",
        ),
        CanvasElement.Sticky(
            id = STICKY,
            x = 150f,
            y = 500f,
            width = 212f,
            height = 196f,
            text = "Ask: who has felt lost this week?",
            rotation = -2f,
        ),
        CanvasElement.Shape(
            id = ELLIPSE,
            x = 680f,
            y = 248f,
            width = 250f,
            height = 170f,
            shape = CanvasShape.ELLIPSE,
            ink = CanvasInk.SKY,
            text = "He leads\nHe restores",
            borderWidth = 4f,
            label = "Ellipse · He leads",
        ),
        CanvasElement.Ink(
            id = ARROW,
            x = 470f,
            y = 300f,
            pathData = "M8 62C50 20 110 12 168 34M150 18l20 16-24 10",
            ink = CanvasInk.CONTRAST,
            width = 4f,
            kind = InkKind.ARROW,
        ),
        CanvasElement.Image(
            id = PHOTO,
            x = 150f,
            y = 240f,
            width = 300f,
            height = 210f,
            fileName = "shepherd-photo.jpg",
        ),
        CanvasElement.Text(
            id = PSALM,
            x = 150f,
            y = 172f,
            text = "Psalm 23 · John 10:11",
            label = "Reading",
        ),
        CanvasElement.Ink(
            id = UNDERLINE,
            x = 146f,
            y = 126f,
            pathData = "M4 16c60-10 140-12 220-6s150 8 232-4",
            ink = CanvasInk.GREEN,
            width = 5f,
            label = "Underline stroke",
        ),
        CanvasElement.Text(
            id = TITLE,
            x = 150f,
            y = 64f,
            text = "The Good Shepherd",
            style = CanvasTextStyle.TITLE,
            label = "Title text",
        ),
    )

    /** The sketch as page 2 of 4, as the design shows it, under the design's title. */
    val document: CanvasDocument = CanvasDocument(
        title = "Sunday sermon sketch",
        pages = listOf(CanvasPage(), CanvasPage(elements), CanvasPage(), CanvasPage()),
    )

    /** [document] with [id]'s element passed through [transform] — locking it, say. */
    fun withElement(id: String, transform: (CanvasElement) -> CanvasElement): CanvasDocument =
        document.copy(
            pages = document.pages.map { page ->
                page.copy(elements = page.elements.map { if (it.id == id) transform(it) else it })
            },
        )

    /** [document] with [element] added on top of the sketch page. */
    fun plus(element: CanvasElement): CanvasDocument =
        document.copy(
            pages = document.pages.mapIndexed { index, page ->
                if (index == SKETCH_PAGE) page.copy(elements = page.elements + element) else page
            },
        )

    /** The page the sketch is on. */
    const val SKETCH_PAGE = 1
}
