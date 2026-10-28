package com.church.presenter.churchpresentermobile.model

/**
 * Small, fully specified canvas elements for the logic tests, so each test can say
 * exactly what is on the page without repeating every field.
 */
internal object CanvasLogicFixtures {

    fun text(id: String, x: Float = 0f, y: Float = 0f, text: String = "Hello", group: String? = null) =
        CanvasElement.Text(id = id, x = x, y = y, text = text, group = group)

    fun ink(
        id: String,
        pathData: String = "M0 0 L10 0",
        kind: InkKind = InkKind.PEN,
        locked: Boolean = false,
        group: String? = null,
    ) = CanvasElement.Ink(
        id = id,
        x = 0f,
        y = 0f,
        pathData = pathData,
        ink = CanvasInk.RED,
        width = 4f,
        kind = kind,
        locked = locked,
        group = group,
    )

    fun image(
        id: String,
        x: Float = 0f,
        y: Float = 0f,
        width: Float = 300f,
        height: Float = 200f,
        locked: Boolean = false,
    ) = CanvasElement.Image(
        id = id,
        x = x,
        y = y,
        width = width,
        height = height,
        fileName = "$id.jpg",
        locked = locked,
    )

    fun shape(
        id: String,
        x: Float = 0f,
        y: Float = 0f,
        width: Float = 100f,
        height: Float = 50f,
        group: String? = null,
    ) =
        CanvasElement.Shape(
            id = id,
            x = x,
            y = y,
            width = width,
            height = height,
            shape = CanvasShape.ELLIPSE,
            ink = CanvasInk.SKY,
            group = group,
        )

    fun sticky(id: String, x: Float = 0f, y: Float = 0f, text: String = "Note") =
        CanvasElement.Sticky(id = id, x = x, y = y, width = 200f, height = 180f, text = text)

    /** Hands out "id-1", "id-2", … — the shape of id the ViewModel gives a new element. */
    fun ids(prefix: String = "new"): () -> String {
        var next = 0
        return { "$prefix-${++next}" }
    }
}
