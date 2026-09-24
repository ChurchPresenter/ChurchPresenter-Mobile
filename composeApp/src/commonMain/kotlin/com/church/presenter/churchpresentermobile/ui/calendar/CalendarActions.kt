package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.ui.Modifier
import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.RowTiming

/**
 * What the calendar's screens and sheets can ask for.
 *
 * Every one of these is a bundle of callbacks a child is handed: the screens themselves hold no
 * ViewModel, so this is the whole surface between them and the one screen that does.
 */

/**
 * What a row can do from its card: open, or — on a tablet — be moved and removed in place.
 * [handle] goes on the row's drag handle, which moves it without a long press.
 */
internal class RowActions(
    val onOpen: () -> Unit,
    val handle: Modifier = Modifier,
    val onMoveUp: (() -> Unit)? = null,
    val onMoveDown: (() -> Unit)? = null,
    val onRemove: (() -> Unit)? = null,
)

/** The four things that can happen to the rows of a run of show. */
internal class RowListActions(
    val onAdd: (PlanRow, Int?, RowTiming) -> Unit,
    val onUpdate: (RowEdit) -> Unit,
    val onRemove: (String) -> Unit,
    val onMove: (from: Int, to: Int) -> Unit,
)

/** Everything the run of show can do to its service; the screen itself holds no ViewModel. */
internal class RunOfShowActions(
    /** Adding, editing, removing and reordering the rows -- grouped, because they travel together. */
    val rows: RowListActions,
    val onArmed: (Boolean) -> Unit,
    val onCopy: (CopyChoice) -> Unit,
    val onUpdateService: (ServiceDraft) -> Unit,
    val onDelete: () -> Unit,
)

/** What the row editor hands back: the row as edited, its length and its timing. */
internal class RowEdit(val row: PlanRow, val seconds: Int?, val timing: RowTiming)

/** What the sync sheet can ask for; the screen owning the ViewModel answers. */
internal class SyncActions(
    val onEnroll: () -> Unit,
    val onScanned: (String) -> Unit,
    val onReset: () -> Unit,
    val onSyncNow: () -> Unit,
    val onLeave: () -> Unit,
)
