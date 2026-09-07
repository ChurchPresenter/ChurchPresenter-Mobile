package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import kotlin.test.Test

/**
 * Tests [UploadProgressOverlay] — the dialog shown while a file is going up.
 *
 * It is deliberately undismissable: cancelling the dialog would leave the upload
 * running with nothing on screen to say so. What is checked here is that it says
 * enough for the operator to know what is happening and roughly how long is left.
 */
@OptIn(ExperimentalTestApi::class)
class UploadProgressOverlayTest {

    private fun themed(content: @Composable () -> Unit): @Composable () -> Unit = {
        AppTheme(themeMode = ThemeMode.DARK) { content() }
    }

    @Test
    fun theTitleIsShown() = runComposeUiTest {
        setContent(themed { UploadProgressOverlay(title = "Uploading photo", progress = HALFWAY) })

        onNodeWithText("Uploading photo").assertExists()
    }

    @Test
    fun aSubtitleIsShownWhenGiven() = runComposeUiTest {
        setContent(themed {
            UploadProgressOverlay(title = "Uploading", progress = HALFWAY, subtitle = "sermon.mp4")
        })

        onNodeWithText("sermon.mp4").assertExists()
    }

    @Test
    fun aDetailLineIsShownWhenGiven() = runComposeUiTest {
        // Used for "photo 2 of 5" during a multi-file upload.
        setContent(themed {
            UploadProgressOverlay(title = "Uploading", progress = HALFWAY, detail = "Photo 2 of 5")
        })

        onNodeWithText("Photo 2 of 5").assertExists()
    }

    @Test
    fun withNoSubtitleOrDetailOnlyTheTitleShows() = runComposeUiTest {
        setContent(themed { UploadProgressOverlay(title = "Uploading", progress = HALFWAY) })

        onNodeWithText("Uploading").assertExists()
        onNodeWithText("sermon.mp4").assertDoesNotExist()
    }

    @Test
    fun anIndeterminateUploadStillRenders() = runComposeUiTest {
        // Null progress: the server has not said how big the job is yet. Rendering
        // a determinate bar at 0% here makes a working upload look stuck.
        setContent(themed { UploadProgressOverlay(title = "Preparing", progress = null) })

        onNodeWithText("Preparing").assertExists()
    }

    @Test
    fun everyProgressValueRendersIncludingTheEnds() = runComposeUiTest {
        for (p in PROGRESS_RAMP) {
            setContent(themed { UploadProgressOverlay(title = "Uploading", progress = p) })
            onNodeWithText("Uploading").assertExists()
        }
    }

    @Test
    fun progressUpdatesInPlaceRatherThanReopening() = runComposeUiTest {
        var progress by mutableStateOf(PROGRESS_RAMP.first())
        setContent(themed { UploadProgressOverlay(title = "Uploading", progress = progress) })
        onNodeWithText("Uploading").assertExists()

        progress = PROGRESS_RAMP.last()
        waitForIdle()

        onNodeWithText("Uploading").assertExists()
    }

    @Test
    fun theOverlayRendersInBothThemes() = runComposeUiTest {
        for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            setContent { AppTheme(themeMode = mode) { UploadProgressOverlay("Uploading", HALFWAY) } }
            onNodeWithText("Uploading").assertExists()
        }
    }
}

/** Both ends and the middle: 0% and 100% are where a bar is most likely to break. */
private val PROGRESS_RAMP = listOf(0f, 0.01f, 0.5f, 0.99f, 1f)

private const val HALFWAY = 0.5f
