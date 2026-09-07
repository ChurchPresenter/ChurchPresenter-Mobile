package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import com.church.presenter.churchpresentermobile.ui.EmptyState
import kotlin.test.Test

/**
 * [EmptyState] in each of the shapes its optional parameters can produce.
 *
 * The states are the point: the composable draws its action only when label,
 * icon and handler are all supplied, and the secondary action only when both of
 * its own are — behaviour `EmptyStateTest` asserts, and these show.
 */
class EmptyStateScreenshotTest {

    @Test
    fun titleAndBodyOnly() = screenshot("empty-state__title-and-body") {
        EmptyState(
            title = "No songs yet",
            body = "Sync your library from the desktop app to see it here.",
        )
    }

    @Test
    fun withPrimaryAction() = screenshot("empty-state__with-action") {
        EmptyState(
            title = "No songs yet",
            body = "Sync your library from the desktop app to see it here.",
            actionLabel = "Sync now",
            actionIcon = Icons.Filled.Refresh,
            onAction = {},
        )
    }

    @Test
    fun withBothActions() = screenshot("empty-state__with-both-actions") {
        EmptyState(
            title = "No songs yet",
            body = "Sync your library from the desktop app to see it here.",
            actionLabel = "Sync now",
            actionIcon = Icons.Filled.Refresh,
            onAction = {},
            secondaryLabel = "Set up the connection",
            onSecondary = {},
        )
    }

    @Test
    fun withTextThatWraps() = screenshot("empty-state__long-text") {
        // The body is operator-facing prose and can run long; this is the state
        // where a padding or line-height regression shows up first.
        EmptyState(
            title = "Nothing has been shared with this device yet",
            body = "Open ChurchPresenter on your computer, go to Settings → Server, " +
                "and make sure this phone is on the same Wi-Fi network before syncing.",
            actionLabel = "Try again",
            actionIcon = Icons.Filled.Refresh,
            onAction = {},
        )
    }
}
