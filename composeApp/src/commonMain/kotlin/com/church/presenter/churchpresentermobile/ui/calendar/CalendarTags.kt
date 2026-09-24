package com.church.presenter.churchpresentermobile.ui.calendar

/**
 * Semantics tags for the calendar, so a UI test can name what it is reaching for.
 *
 * The same reasoning as [com.church.presenter.churchpresentermobile.ui.library.LibraryTags]: a
 * `stringResource` renders empty in the test runtime, so a control whose only content is its label
 * has no text to match and no width to tap. Selecting by position instead turns any layout change
 * into a failure somewhere unrelated.
 *
 * `internal`, and referenced from both sides, so a tag cannot be renamed on one side alone.
 */
/** The month, the day under it, and the run of show. */
internal object CalendarTags {

    // ── The month and the day under it ───────────────────────────────────
    const val MONTH_PREVIOUS = "calendar:month:previous"

    const val MONTH_NEXT = "calendar:month:next"

    /** One day of the grid, by its stored `YYYY-MM-DD`. */
    fun day(date: String) = "calendar:day:$date"

    const val DAY_ADD = "calendar:day:add"

    const val DAY_COPY_LAST = "calendar:day:copyLast"

    /** A service's card on the day it is planned for. */
    fun serviceCard(id: String) = "calendar:service:$id"

    // ── The run of show ──────────────────────────────────────────────────
    const val RUN_BACK = "run:back"

    const val RUN_ARMED = "run:armed"

    const val RUN_ADD = "run:add"

    const val RUN_COPY = "run:copy"


    const val RUN_EDIT_SERVICE = "run:editService"

    /** One row of the run of show, by its row id. */
    fun row(id: String) = "run:row:$id"

    fun rowMoveUp(id: String) = "run:row:$id:up"

    fun rowMoveDown(id: String) = "run:row:$id:down"

    fun rowRemove(id: String) = "run:row:$id:remove"

    // ── The row editor ───────────────────────────────────────────────────
    const val ROW_NAME = "row:name"

    const val ROW_DETAIL = "row:detail"

    const val ROW_DURATION = "row:duration"

    const val ROW_SAVE = "row:save"

    const val ROW_UP = "row:up"

    const val ROW_DOWN = "row:down"

    const val ROW_REMOVE = "row:remove"
}

/** The "Add to service" picker and the timing panel inside it. */
internal object PickerTags {

    // ── The picker ───────────────────────────────────────────────────────
    const val PICKER_SEARCH = "picker:search"

    fun tab(tab: PickerTab) = "picker:tab:${tab.name.lowercase()}"

    const val PICKER_ADD = "picker:add"

    const val PICKER_REFERENCE = "picker:reference"

    /** One song in the picker's list, by the label it is listed under. */
    fun song(label: String) = "picker:song:$label"

    fun preset(id: String) = "picker:preset:$id"

    fun book(number: Int) = "picker:book:$number"

    fun chapter(number: Int) = "picker:chapter:$number"

    fun verse(number: Int) = "picker:verse:$number"

    const val MINISTRY_WHAT = "picker:ministry:what"

    const val MINISTRY_WHO = "picker:ministry:who"

    const val MINISTRY_DURATION = "picker:ministry:duration"

    // ── The timing panel ─────────────────────────────────────────────────
    const val TIMING_CUED = "timing:cued"

    const val TIMING_AFTER_PREVIOUS = "timing:afterPrevious"

    const val TIMING_ON_TIME = "timing:onTime"

    const val TIMING_OWN_LENGTH = "timing:ownLength"

    const val TIMING_ONCE = "timing:once"

    const val TIMING_LOOP = "timing:loop"

    const val TIMING_HOLD = "timing:hold"

    const val TIMING_NEXT = "timing:next"

    const val TIMING_BLANK = "timing:blank"

    /** A start offset chip, by how many minutes before the service it is. */
    fun timingEarly(minutes: Int) = "timing:early:$minutes"

    /** A run-length chip, by its minutes. */
    fun timingRuns(minutes: Int) = "timing:runs:$minutes"

    /** A repeat-count chip, by its count. */
    fun timingRepeats(times: Int) = "timing:repeats:$times"
}

/** The sheets: the service form, the copy dialog, and enrolling with the church computer. */
internal object SheetTags {

    // ── The service form ─────────────────────────────────────────────────
    const val SERVICE_NAME = "service:name"

    const val SERVICE_START = "service:start"

    const val SERVICE_DELETE = "service:delete"

    const val SERVICE_CANCEL = "service:cancel"

    const val SERVICE_CONFIRM = "service:confirm"

    const val SERVICE_BLANK = "service:blank"

    fun serviceKind(index: Int) = "service:kind:$index"

    fun template(id: String) = "service:template:$id"

    // ── Copying a service ────────────────────────────────────────────────
    fun repeatRule(index: Int) = "copy:rule:$index"

    const val COPY_COUNT = "copy:count"

    const val COPY_INCLUDE_ROWS = "copy:includeRows"

    const val COPY_INCLUDE_CUES = "copy:includeCues"

    const val COPY_CONFIRM = "copy:confirm"

    const val COPY_CANCEL = "copy:cancel"

    // ── Sync with the church computer ────────────────────────────────────
    const val SYNC_SCAN = "sync:scan"

    const val SYNC_ENROLL = "sync:enroll"

    const val SYNC_CANCEL = "sync:cancel"

    const val SYNC_NOW = "sync:now"

    const val SYNC_LEAVE = "sync:leave"

    const val SYNC_CLOSE = "sync:close"

    /** The close button every sheet's title row carries. */
    const val SHEET_CLOSE = "sheet:close"
}
