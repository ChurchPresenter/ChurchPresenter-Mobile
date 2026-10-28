package com.church.presenter.churchpresentermobile.ui.canvas

import com.church.presenter.churchpresentermobile.model.CanvasDocument
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasInk
import com.church.presenter.churchpresentermobile.model.CanvasPage
import com.church.presenter.churchpresentermobile.model.CanvasShape
import com.church.presenter.churchpresentermobile.model.CanvasTextStyle
import com.church.presenter.churchpresentermobile.model.HighlightColor
import com.church.presenter.churchpresentermobile.model.InkKind
import com.church.presenter.churchpresentermobile.model.ShapeFill

/*
 * The canvas the UI tests start from. Every element sits well inside the
 * top-left of the board, clear of the tool rail and inside the default test
 * window, so a gesture aimed at one lands where it is drawn.
 */

/** Ids of the sample canvas's elements, named for what they are. */
internal object Sample {
    const val TITLE = "title"
    const val INK = "ink"
    const val HIGHLIGHT = "highlight"
    const val IMAGE = "image"
    const val SHAPE = "shape"
    const val NOTE = "note"
}

/** A two-page canvas whose first page holds one of each element. */
internal fun sampleCanvas(): CanvasDocument = CanvasDocument(
    title = "Sunday sermon sketch",
    pages = listOf(
        CanvasPage(
            listOf(
                CanvasElement.Text(
                    Sample.TITLE,
                    x = 150f,
                    y = 40f,
                    text = "The Good Shepherd",
                    style = CanvasTextStyle.TITLE,
                ),
                CanvasElement.Ink(
                    Sample.INK,
                    x = 0f,
                    y = 0f,
                    pathData = "M560 200 L700 200",
                    ink = CanvasInk.RED,
                    width = 5f,
                ),
                CanvasElement.Ink(
                    Sample.HIGHLIGHT,
                    x = 0f,
                    y = 0f,
                    pathData = "M560 260 L700 260",
                    ink = CanvasInk.CONTRAST,
                    width = 18f,
                    kind = InkKind.HIGHLIGHTER,
                    highlight = HighlightColor.YELLOW,
                ),
                CanvasElement.Image(
                    Sample.IMAGE,
                    x = 150f,
                    y = 150f,
                    width = 300f,
                    height = 200f,
                    fileName = "shepherd.jpg",
                ),
                CanvasElement.Shape(
                    Sample.SHAPE,
                    x = 560f,
                    y = 330f,
                    width = 220f,
                    height = 140f,
                    shape = CanvasShape.ELLIPSE,
                    ink = CanvasInk.SKY,
                    text = "He leads",
                    fill = ShapeFill.NONE,
                ),
                CanvasElement.Sticky(
                    Sample.NOTE,
                    x = 150f,
                    y = 400f,
                    width = 200f,
                    height = 180f,
                    text = "Ask: who felt lost?",
                ),
            ),
        ),
        CanvasPage(),
    ),
)
