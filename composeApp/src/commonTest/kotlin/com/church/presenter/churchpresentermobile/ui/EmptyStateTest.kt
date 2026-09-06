package com.church.presenter.churchpresentermobile.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests [EmptyState] — the screen a church meets before any content is synced.
 *
 * Its point is that the action is optional but, when present, reachable: telling
 * someone to "copy it from your computer" is only useful if the screen can also
 * take them there.
 */
@OptIn(ExperimentalTestApi::class)
class EmptyStateTest {

    @Test
    fun titleAndBodyAreShown() = runComposeUiTest {
        setContent {
            EmptyState(title = "No songs yet", body = "Sync your library from the desktop app.")
        }

        onNodeWithText("No songs yet").assertExists()
        onNodeWithText("Sync your library from the desktop app.").assertExists()
    }

    @Test
    fun withNoActionSuppliedNoButtonIsDrawn() = runComposeUiTest {
        setContent { EmptyState(title = "No songs yet", body = "Nothing here.") }

        onNodeWithText("Sync now").assertDoesNotExist()
    }

    @Test
    fun theActionIsDrawnAndClickable() = runComposeUiTest {
        var clicks = 0
        setContent {
            EmptyState(
                title = "No songs yet",
                body = "Nothing here.",
                actionLabel = "Sync now",
                actionIcon = Icons.Filled.Refresh,
                onAction = { clicks++ },
            )
        }

        onNodeWithText("Sync now").assertHasClickAction().performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun anIncompleteActionIsNotDrawnAtAll() = runComposeUiTest {
        // Label, icon and handler are all required — a half-specified action must
        // not render a button that does nothing when tapped.
        setContent {
            EmptyState(
                title = "No songs yet",
                body = "Nothing here.",
                actionLabel = "Sync now",
                onAction = {},
            )
        }

        onNodeWithText("Sync now").assertDoesNotExist()
    }

    @Test
    fun theSecondaryLinkIsClickable() = runComposeUiTest {
        var clicks = 0
        setContent {
            EmptyState(
                title = "No songs yet",
                body = "Nothing here.",
                secondaryLabel = "Learn more",
                onSecondary = { clicks++ },
            )
        }

        onNodeWithText("Learn more").performClick()

        assertEquals(1, clicks)
    }
}
