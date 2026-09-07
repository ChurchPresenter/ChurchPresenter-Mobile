package com.church.presenter.churchpresentermobile.screenshot

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.LocalSetlistEntry
import com.church.presenter.churchpresentermobile.model.SetlistEntryType
import com.church.presenter.churchpresentermobile.ui.ModePickerScreen
import com.church.presenter.churchpresentermobile.ui.MoreScreen
import com.church.presenter.churchpresentermobile.ui.ServiceOrderDrawerContent
import kotlin.test.Test

/**
 * The screens that decide where the operator goes: the first-run mode picker,
 * the More launcher, and the running order.
 *
 * All three are mode-sensitive — the More list and the mode picker both differ
 * between remote and standalone — and the running order has an empty state
 * nobody sees until the Sunday it matters.
 */
class NavigationScreenshotTest {

    private val entries = listOf(
        LocalSetlistEntry(type = SetlistEntryType.SONG, reference = "1", title = "Amazing Grace"),
        LocalSetlistEntry(type = SetlistEntryType.BIBLE, reference = "John 3:16", title = "John 3:16"),
        LocalSetlistEntry(
            type = SetlistEntryType.ANNOUNCEMENT,
            reference = "welcome",
            title = "Welcome and notices",
        ),
        LocalSetlistEntry(type = SetlistEntryType.SONG, reference = "104", title = "How Great Thou Art"),
    )

    @Test
    fun moreRemote() = screenshot("more__remote") {
        MoreScreen(mode = AppMode.REMOTE, onSelect = {})
    }

    @Test
    fun moreStandalone() = screenshot("more__standalone") {
        // Fewer rows: the destinations that need a desktop are not offered,
        // because reaching one can only end in a timeout.
        MoreScreen(mode = AppMode.STANDALONE, onSelect = {})
    }

    @Test
    fun modePickerRemote() = screenshot("mode-picker__remote-selected") {
        ModePickerScreen(onModeChosen = {}, initialMode = AppMode.REMOTE)
    }

    @Test
    fun modePickerStandalone() = screenshot("mode-picker__standalone-selected") {
        ModePickerScreen(onModeChosen = {}, initialMode = AppMode.STANDALONE)
    }

    @Test
    fun serviceOrderPopulated() = screenshot("service-order__populated") {
        ServiceOrderDrawerContent(entries = entries)
    }

    @Test
    fun serviceOrderSingleEntry() = screenshot("service-order__single-entry") {
        // One row is both first and last, so both reorder controls are disabled
        // — a state the four-row capture cannot show.
        ServiceOrderDrawerContent(entries = entries.take(1))
    }

    @Test
    fun serviceOrderEmpty() = screenshot("service-order__empty") {
        ServiceOrderDrawerContent(entries = emptyList())
    }
}
