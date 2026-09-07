package com.church.presenter.churchpresentermobile.screenshot

import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.SlideTextSize
import com.church.presenter.churchpresentermobile.model.SlideTheme
import com.church.presenter.churchpresentermobile.model.SlideThemePresets
import com.church.presenter.churchpresentermobile.present.SinkState
import com.church.presenter.churchpresentermobile.present.SinkStatus
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.library.ClearContentSheet
import com.church.presenter.churchpresentermobile.ui.library.ShareSheet
import com.church.presenter.churchpresentermobile.ui.library.SyncSheet
import com.church.presenter.churchpresentermobile.ui.library.libraryOf
import com.church.presenter.churchpresentermobile.ui.library.song
import com.church.presenter.churchpresentermobile.ui.standalone.LookSheet
import com.church.presenter.churchpresentermobile.ui.standalone.OutputTargetsSheet
import kotlin.test.Test

/**
 * The sheets themselves — the bottom-sheet scaffold around content the other
 * tests capture on its own.
 *
 * Worth a golden of their own because the scaffold is where the handle, the
 * insets and the sheet's own height live, and a sheet that opens half off the
 * bottom of the screen looks nothing like its content does in isolation.
 */
class SheetsScreenshotTest {

    private fun settings() = AppSettings(InMemorySettingsStorage())
    private fun repository(): LibraryRepository =
        libraryOf(songs = listOf(song("s1", "1", "Amazing Grace"), song("s2", "104", "How Great Thou Art")))

    private fun bibles() = LocalBibleRepository(InMemoryFileStorage())

    @Test
    fun syncSheet() = screenshot("sync-sheet__songs", dialog = true) {
        SyncSheet(
            repository = repository(),
            bibles = bibles(),
            settings = settings(),
            sender = FakeWsSender(),
            onDismiss = {},
        )
    }

    @Test
    fun shareSheet() = screenshot("share-sheet__opened", dialog = true) {
        ShareSheet(repository = repository(), onDismiss = {}, onMessage = {})
    }

    @Test
    fun clearContentSheet() = screenshot("clear-content-sheet__opened", dialog = true) {
        ClearContentSheet(
            repository = repository(),
            bibles = bibles(),
            settings = settings(),
            sender = FakeWsSender(),
            onDismiss = {},
        )
    }

    @Test
    fun lookSheet() = screenshot("look-sheet__opened", dialog = true) {
        LookSheet(
            theme = SlideTheme(),
            onThemeChange = {},
            showChords = false,
            onShowChordsChange = {},
            textSize = SlideTextSize.MEDIUM,
            onTextSizeChange = {},
            presets = SlideThemePresets.all,
            savedThemes = emptyList(),
            onApplyTheme = {},
            onSaveTheme = {},
            onDeleteTheme = {},
            onDismiss = {},
        )
    }

    @Test
    fun outputTargetsSheet() = screenshot("output-targets-sheet__opened", dialog = true) {
        OutputTargetsSheet(
            sinks = listOf(
                SinkStatus("web", "Web page", SinkState.ATTACHED, detail = "2 viewers", clientCount = 2),
                SinkStatus("hdmi", "HDMI display", SinkState.DETACHED),
            ),
            onDismiss = {},
        )
    }
}
