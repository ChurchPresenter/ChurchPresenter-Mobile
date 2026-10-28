package com.church.presenter.churchpresentermobile.model

/*
 * Edits to a page's list of elements — the order they stack in, where they sit,
 * which belong together. Each takes the ids it acts on, so one element and a
 * lasso's worth go through the same code.
 */

/** [this] without the elements in [ids]. */
internal fun List<CanvasElement>.without(ids: Collection<String>): List<CanvasElement> = filterNot { it.id in ids }

/** [this] with [ids] moved to the top of the drawing order, keeping their order among themselves. */
internal fun List<CanvasElement>.broughtToFront(ids: Collection<String>): List<CanvasElement> =
    filterNot { it.id in ids } + filter { it.id in ids }

/** [this] with [ids] moved to the bottom of the drawing order. */
internal fun List<CanvasElement>.sentToBack(ids: Collection<String>): List<CanvasElement> =
    filter { it.id in ids } + filterNot { it.id in ids }

/** [this] with each element in [ids] replaced by [transform] of itself. */
internal fun List<CanvasElement>.updated(
    ids: Collection<String>,
    transform: (CanvasElement) -> CanvasElement,
): List<CanvasElement> = map { if (it.id in ids) transform(it) else it }

/**
 * [this] with a copy of every element in [ids] placed just above it, nudged by
 * [DUPLICATE_OFFSET] so each copy is visibly a second item rather than hidden
 * behind the first. Copies of a group become a group of their own.
 *
 * @return The new list and the copies' ids, which become the selection.
 */
internal fun List<CanvasElement>.withDuplicates(
    ids: Collection<String>,
    newId: () -> String,
): Pair<List<CanvasElement>, List<String>> {
    val copies = mutableListOf<String>()
    val groups = mutableMapOf<String, String>()
    val list = flatMap { element ->
        if (element.id !in ids) return@flatMap listOf(element)
        val copy = element.withId(newId())
            .moved(DUPLICATE_OFFSET, DUPLICATE_OFFSET)
            .withGroup(element.group?.let { groups.getOrPut(it, newId) })
        copies += copy.id
        listOf(element, copy)
    }
    return list to copies
}

/** How far a duplicate lands from its original, in dp. */
internal const val DUPLICATE_OFFSET = 24f

/** [ids] moved so their left edges line up with the left-most of them. */
internal fun List<CanvasElement>.aligned(
    ids: Collection<String>,
    bounds: Map<String, CanvasBounds>,
): List<CanvasElement> {
    val left = ids.mapNotNull { bounds[it]?.left }.minOrNull() ?: return this
    return map { element ->
        val box = bounds[element.id]
        if (element.id in ids && box != null) element.moved(left - box.left, 0f) else element
    }
}

/**
 * [ids] spread across, left to right, with equal gaps between them. The outer two
 * stay where they are; it takes three to have anything to spread.
 */
internal fun List<CanvasElement>.distributed(
    ids: Collection<String>,
    bounds: Map<String, CanvasBounds>,
): List<CanvasElement> {
    val ordered = ids.mapNotNull { id -> bounds[id]?.let { id to it } }.sortedBy { it.second.left }
    if (ordered.size < MIN_TO_DISTRIBUTE) return this
    val span = ordered.last().second.right - ordered.first().second.left
    val gap = (span - ordered.sumOf { it.second.width.toDouble() }.toFloat()) / (ordered.size - 1)
    var next = ordered.first().second.left
    val targets = ordered.associate { (id, box) ->
        val target = next
        next += box.width + gap
        id to target - box.left
    }
    return map { element -> targets[element.id]?.let { element.moved(it, 0f) } ?: element }
}

private const val MIN_TO_DISTRIBUTE = 3

/** [ids] tied together under [group], or set loose again for null. */
internal fun List<CanvasElement>.grouped(ids: Collection<String>, group: String?): List<CanvasElement> =
    updated(ids) { it.withGroup(group) }

/**
 * [this] with [id] moved [steps] places up the drawing order (down for a negative
 * number), stopping at either end.
 */
internal fun List<CanvasElement>.movedInStack(id: String, steps: Int): List<CanvasElement> {
    val from = indexOfFirst { it.id == id }
    if (from < 0) return this
    val to = (from + steps).coerceIn(0, lastIndex)
    if (to == from) return this
    val list = toMutableList()
    list.add(to, list.removeAt(from))
    return list
}

/** Every element sharing a group with one in [ids], so a group is always picked whole. */
internal fun List<CanvasElement>.withGroupmates(ids: Collection<String>): List<String> {
    val groups = filter { it.id in ids }.mapNotNull { it.group }.toSet()
    return filter { it.id in ids || (it.group != null && it.group in groups) }.map { it.id }
}
