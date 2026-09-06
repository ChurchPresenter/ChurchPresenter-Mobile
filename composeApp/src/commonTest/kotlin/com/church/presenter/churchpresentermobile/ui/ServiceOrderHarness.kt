package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import com.church.presenter.churchpresentermobile.model.LocalSetlistEntry
import com.church.presenter.churchpresentermobile.model.SetlistEntryType

/** Setup for the standalone running-order drawer. */
internal fun entry(
    title: String,
    type: SetlistEntryType = SetlistEntryType.SONG,
    reference: String = title.lowercase(),
) = LocalSetlistEntry(type = type, reference = reference, title = title)

internal val runningOrder = listOf(
    entry("Amazing Grace"),
    entry("John 3:16", SetlistEntryType.BIBLE),
    entry("Welcome notice", SetlistEntryType.ANNOUNCEMENT),
)

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showServiceOrder(
    entries: List<LocalSetlistEntry> = runningOrder,
    onItemClick: (LocalSetlistEntry) -> Unit = {},
    onMove: (from: Int, to: Int) -> Unit = { _, _ -> },
    onRemove: (index: Int) -> Unit = {},
    onClear: () -> Unit = {},
    onClose: () -> Unit = {},
) = showScreen {
    ServiceOrderDrawerContent(
        entries = entries,
        onItemClick = onItemClick,
        onMove = onMove,
        onRemove = onRemove,
        onClear = onClear,
        onClose = onClose,
    )
}
