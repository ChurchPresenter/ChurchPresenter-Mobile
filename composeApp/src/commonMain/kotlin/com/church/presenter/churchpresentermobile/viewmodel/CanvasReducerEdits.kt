package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.CanvasBounds
import com.church.presenter.churchpresentermobile.model.CanvasElement
import com.church.presenter.churchpresentermobile.model.CanvasInk
import com.church.presenter.churchpresentermobile.model.CanvasPoint
import com.church.presenter.churchpresentermobile.model.CanvasTool
import com.church.presenter.churchpresentermobile.model.CanvasToolOptions
import com.church.presenter.churchpresentermobile.model.EraserMode
import com.church.presenter.churchpresentermobile.model.ImageAdjust
import com.church.presenter.churchpresentermobile.model.InkKind
import com.church.presenter.churchpresentermobile.model.PenType
import com.church.presenter.churchpresentermobile.model.adjusted
import com.church.presenter.churchpresentermobile.model.aligned
import com.church.presenter.churchpresentermobile.model.broughtToFront
import com.church.presenter.churchpresentermobile.model.distributed
import com.church.presenter.churchpresentermobile.model.grouped
import com.church.presenter.churchpresentermobile.model.imageElement
import com.church.presenter.churchpresentermobile.model.outlineFor
import com.church.presenter.churchpresentermobile.model.sentToBack
import com.church.presenter.churchpresentermobile.model.smoothed
import com.church.presenter.churchpresentermobile.model.strokePathData
import com.church.presenter.churchpresentermobile.model.updated
import com.church.presenter.churchpresentermobile.model.withDuplicates
import com.church.presenter.churchpresentermobile.model.without

/*
 * The reducer's larger edits: strokes, pictures, and what the selection bar and
 * the Arrange row do to the selection.
 */

/**
 * A stroke through [points]: ink for the pen and highlighter, and for the eraser
 * in Pixel mode a stroke of the board's own color over whatever is under it.
 * Other tools place things rather than draw, and ignore a stroke.
 */
internal fun CanvasUiState.stroke(
    points: List<CanvasPoint>,
    pressure: Float,
    newId: () -> String,
): CanvasUiState {
    if (points.isEmpty()) return this
    val ink = when (tool) {
        CanvasTool.PEN -> options.pen.let { pen ->
            CanvasElement.Ink(
                id = newId(),
                x = 0f,
                y = 0f,
                pathData = strokePathData(smoothed(points, pen.smoothing)),
                ink = pen.ink,
                custom = pen.custom,
                width = pen.width * penWidthScale(pen.type) *
                    if (pen.pressure) pressure.coerceIn(MIN_PRESSURE, MAX_PRESSURE) else 1f,
                opacity = pen.opacity * if (pen.type == PenType.PENCIL) PENCIL_OPACITY else 1f,
            )
        }
        CanvasTool.HIGHLIGHTER -> options.highlighter.let { marker ->
            val line = if (marker.straightOnly) listOf(points.first(), points.last()) else points
            CanvasElement.Ink(
                id = newId(),
                x = 0f,
                y = 0f,
                pathData = strokePathData(line),
                ink = options.pen.ink,
                custom = marker.custom,
                width = marker.width,
                opacity = marker.opacity,
                kind = InkKind.HIGHLIGHTER,
                highlight = marker.color,
            )
        }
        CanvasTool.ERASER -> if (options.eraser.mode == EraserMode.PIXEL) {
            CanvasElement.Ink(
                id = newId(),
                x = 0f,
                y = 0f,
                pathData = strokePathData(points),
                ink = CanvasInk.CONTRAST,
                width = options.eraser.size,
                kind = InkKind.ERASER,
            )
        } else {
            return this
        }
        else -> return this
    }
    // A highlighter set to draw behind ink goes under the first stroke on the page,
    // so it marks the writing instead of painting over it.
    val behind = ink.kind == InkKind.HIGHLIGHTER && options.highlighter.behindInk
    return editPage { elements ->
        val firstInk = elements.indexOfFirst { it is CanvasElement.Ink && it.kind == InkKind.PEN }
        if (behind && firstInk >= 0) elements.take(firstInk) + ink + elements.drop(firstInk) else elements + ink
    }
}

/** A brush lays down a broader line than a ballpoint; a fountain pen a little broader. */
private fun penWidthScale(type: PenType): Float = when (type) {
    PenType.BALLPOINT, PenType.PENCIL -> 1f
    PenType.FOUNTAIN -> FOUNTAIN_SCALE
    PenType.BRUSH -> BRUSH_SCALE
}

private const val FOUNTAIN_SCALE = 1.25f
private const val BRUSH_SCALE = 1.75f

/** How far stylus pressure may thin or thicken a stroke: from half to one and a half times. */
private const val MIN_PRESSURE = 0.5f
private const val MAX_PRESSURE = 1.5f

/** A pencil's line is greyer than a pen's. */
private const val PENCIL_OPACITY = 0.7f

/**
 * A picture from the Insert panel or a tap with the image tool. While Replace is
 * pending it takes the place of that picture instead, keeping its size and spot.
 */
internal fun CanvasUiState.insertImage(intent: CanvasIntent.InsertImage, newId: () -> String): CanvasUiState {
    val target = replaceTarget?.let { id -> page.elements.firstOrNull { it.id == id } as? CanvasElement.Image }
    if (target != null) {
        return editPage { elements ->
            elements.updated(listOf(target.id)) {
                target.copy(fileName = "", source = intent.source, photo = intent.picture?.key, cutOut = false)
            }
        }.copy(replaceTarget = null, panel = CanvasPanel.NONE, selectedIds = listOf(target.id))
    }
    val image = imageElement(newId(), intent.at ?: DEFAULT_IMAGE_CENTRE, intent.source, options.image, intent.picture)
    return finishEditing().editPage { it + image }.copy(selectedIds = listOf(image.id))
}

/** Where a picture lands when it is inserted from the panel rather than tapped onto the page. */
private val DEFAULT_IMAGE_CENTRE = CanvasPoint(480f, 340f)

/**
 * [action] applied to the selection. Locked elements are left alone by everything
 * but Lock itself, which is how they are unlocked.
 */
internal fun CanvasUiState.onSelection(action: SelectionAction, newId: () -> String): CanvasUiState {
    val loose = selection.filterNot { it.locked }.map { it.id }
    return when (action) {
        SelectionAction.CROP -> adjustImages(ImageAdjust.CROP)
        SelectionAction.REPLACE ->
            selected?.let { copy(panel = CanvasPanel.INSERT_IMAGE, replaceTarget = it.id) } ?: this
        SelectionAction.BRING_TO_FRONT -> editPage { it.broughtToFront(loose) }
        SelectionAction.DUPLICATE -> {
            val (elements, copies) = page.elements.withDuplicates(loose, newId)
            editPage { elements }.copy(selectedIds = copies)
        }
        SelectionAction.LOCK -> {
            val lock = !selection.all { it.locked }
            editPage { it.updated(selectedIds) { e -> e.withFlags(locked = lock) } }
        }
        SelectionAction.DELETE -> editPage { it.without(loose) }.copy(selectedIds = selectedIds - loose.toSet())
    }
}

/**
 * The select tool's Arrange row. Group ties the selection together, or — if it is
 * already one group — sets it loose again.
 */
internal fun CanvasUiState.arrange(
    action: ArrangeAction,
    bounds: Map<String, CanvasBounds>,
    newId: () -> String,
): CanvasUiState {
    val loose = selection.filterNot { it.locked }.map { it.id }
    return when (action) {
        ArrangeAction.ALIGN -> editPage { it.aligned(loose, bounds) }
        ArrangeAction.DISTRIBUTE -> editPage { it.distributed(loose, bounds) }
        ArrangeAction.GROUP -> {
            val groups = selection.map { it.group }.toSet()
            val ungroup = groups.size == 1 && groups.single() != null
            editPage { it.grouped(selectedIds, if (ungroup) null else newId()) }
        }
        ArrangeAction.FRONT -> editPage { it.broughtToFront(loose) }
        ArrangeAction.BACK -> editPage { it.sentToBack(loose) }
    }
}

/** [adjust] applied to every selected picture that is not locked. */
internal fun CanvasUiState.adjustImages(adjust: ImageAdjust): CanvasUiState {
    val images = selection.filter { it is CanvasElement.Image && !it.locked }.map { it.id }
    return editPage { elements -> elements.updated(images) { adjusted(it as CanvasElement.Image, adjust) } }
}

/**
 * New tool options, applied to what is selected too: changing the text tool's
 * bold, a note's color or a shape's fill while one is selected changes it, as
 * the operator expects. Only the tool whose options changed touches anything.
 */
internal fun CanvasUiState.withOptions(new: CanvasToolOptions): CanvasUiState {
    val old = options
    val targets = (selectedIds + listOfNotNull(editingId)).toSet()
    val changed = copy(options = new)
    if (targets.isEmpty()) return changed
    return changed.editPage { elements ->
        elements.updated(targets) { element ->
            if (element.locked) element else element.restyled(old, new)
        }
    }
}

private fun CanvasElement.restyled(old: CanvasToolOptions, new: CanvasToolOptions): CanvasElement = when {
    this is CanvasElement.Text && new.text != old.text -> copy(format = new.text)
    this is CanvasElement.Sticky && new.sticky != old.sticky -> copy(
        color = new.sticky.color,
        font = new.sticky.font,
        rotation = if (new.sticky.tilt) STICKY_TILT else 0f,
    )
    this is CanvasElement.Shape && new.shapes != old.shapes -> copy(
        shape = new.shapes.shape,
        fill = new.shapes.fill,
        fillCustom = new.shapes.fillCustom,
        border = new.shapes.border,
        borderWidth = new.shapes.borderWidth,
        ink = outlineFor(new.shapes.fill),
        custom = new.shapes.fillCustom,
        skewX = new.shapes.skewX,
        skewY = new.shapes.skewY,
        opacity = new.shapes.opacity,
    )
    this is CanvasElement.Image && new.image != old.image ->
        copy(rounded = new.image.rounded, shadow = new.image.shadow, opacity = new.image.opacity)
    this is CanvasElement.Ink && kind == InkKind.ARROW && new.line != old.line ->
        copy(ink = new.line.ink, custom = new.line.custom, width = new.line.width, dashed = new.line.dashed)
    else -> this
}

private const val STICKY_TILT = -2f

/**
 * The options panel brought in line with a single selected element, so the panel
 * shows the selected text's font, the note's color or the shape's fill — and
 * changing one starts from what is there.
 */
internal fun CanvasUiState.withOptionsFrom(): CanvasUiState {
    val element = selected ?: return this
    val o = options
    return copy(
        options = when (element) {
            is CanvasElement.Text -> element.format?.let { o.copy(text = it) } ?: o
            is CanvasElement.Sticky -> o.copy(
                sticky = o.sticky.copy(color = element.color, font = element.font, tilt = element.rotation != 0f),
            )
            is CanvasElement.Shape -> o.copy(
                shapes = o.shapes.copy(
                    shape = element.shape,
                    fill = element.fill,
                    fillCustom = element.fillCustom,
                    border = element.border,
                    borderWidth = element.borderWidth,
                    skewX = element.skewX,
                    skewY = element.skewY,
                    opacity = element.opacity,
                ),
            )
            is CanvasElement.Image ->
                o.copy(
                    image = o.image.copy(rounded = element.rounded, shadow = element.shadow, opacity = element.opacity),
                )
            is CanvasElement.Ink -> if (element.kind == InkKind.ARROW) {
                o.copy(
                    line = o.line.copy(
                        ink = element.ink,
                        custom = element.custom,
                        width = element.width,
                        dashed = element.dashed,
                    ),
                )
            } else {
                o
            }
        },
    )
}
