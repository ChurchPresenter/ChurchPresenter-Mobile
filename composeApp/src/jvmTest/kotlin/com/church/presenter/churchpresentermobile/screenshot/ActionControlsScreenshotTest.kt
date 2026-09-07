package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.ui.ContentActionButtons
import com.church.presenter.churchpresentermobile.ui.FabStack
import com.church.presenter.churchpresentermobile.ui.QrCodeImage
import kotlin.test.Test

/**
 * The controls that put content on the screen — and say what is already on it.
 *
 * [ContentActionButtons] carries the most state of any small component in the
 * app: projecting or not, held or not, added to the schedule or not, a cast
 * badge, multi-select, and a standalone variant with no desktop schedule at
 * all. Each of those changes an icon, a tint or a whole button, which is
 * exactly the kind of thing a behavioural test cannot see.
 */
class ActionControlsScreenshotTest {

    @Test
    fun actionsIdle() = screenshot("content-actions__idle", width = null) {
        ContentActionButtons(
            isProjecting = false,
            scheduleAdded = false,
            onToggleProjecting = {},
            onAddToSchedule = {},
            modifier = Modifier.padding(16.dp),
        )
    }

    @Test
    fun actionsProjecting() = screenshot("content-actions__projecting", width = null) {
        ContentActionButtons(
            isProjecting = true,
            scheduleAdded = false,
            onToggleProjecting = {},
            onAddToSchedule = {},
            modifier = Modifier.padding(16.dp),
        )
    }

    @Test
    fun actionsAlreadyInSchedule() = screenshot("content-actions__in-schedule", width = null) {
        ContentActionButtons(
            isProjecting = false,
            scheduleAdded = true,
            onToggleProjecting = {},
            onAddToSchedule = {},
            modifier = Modifier.padding(16.dp),
        )
    }

    @Test
    fun actionsWithCastBadge() = screenshot("content-actions__cast-badge", width = null) {
        ContentActionButtons(
            isProjecting = true,
            scheduleAdded = false,
            onToggleProjecting = {},
            onAddToSchedule = {},
            castBadgeCount = 3,
            modifier = Modifier.padding(16.dp),
        )
    }

    @Test
    fun actionsHeld() = screenshot("content-actions__held", width = null) {
        ContentActionButtons(
            isProjecting = true,
            scheduleAdded = false,
            onToggleProjecting = {},
            onAddToSchedule = {},
            isHolding = true,
            onToggleHold = {},
            onClearDisplay = {},
            modifier = Modifier.padding(16.dp),
        )
    }

    @Test
    fun actionsMultiSelect() = screenshot("content-actions__multi-select", width = null) {
        ContentActionButtons(
            isProjecting = false,
            scheduleAdded = false,
            onToggleProjecting = {},
            onAddToSchedule = {},
            isMultiSelectMode = true,
            onToggleMultiSelect = {},
            modifier = Modifier.padding(16.dp),
        )
    }

    @Test
    fun actionsStandalone() = screenshot("content-actions__standalone", width = null) {
        // No desktop attached, so no schedule button at all — a null handler,
        // not a disabled control.
        ContentActionButtons(
            isProjecting = false,
            scheduleAdded = false,
            onToggleProjecting = {},
            onAddToSchedule = null,
            modifier = Modifier.padding(16.dp),
        )
    }

    @Test
    fun fabStackAllActions() = screenshot("fab-stack__all-actions", width = null) {
        FabStack(
            modifier = Modifier.padding(16.dp),
            onSelect = {},
            onAddToSchedule = {},
            onCast = {},
        )
    }

    @Test
    fun fabStackSelectOnly() = screenshot("fab-stack__select-only", width = null) {
        FabStack(modifier = Modifier.padding(16.dp), onSelect = {})
    }

    @Test
    fun fabStackWithCastBadge() = screenshot("fab-stack__cast-badge", width = null) {
        FabStack(
            modifier = Modifier.padding(16.dp),
            onSelect = {},
            onCast = {},
            castBadgeCount = 12,
        )
    }

    @Test
    fun qrCode() = screenshot("qr-code__display-url", width = null) {
        QrCodeImage(content = "http://192.168.1.10:8765/display", modifier = Modifier.padding(16.dp))
    }
}
