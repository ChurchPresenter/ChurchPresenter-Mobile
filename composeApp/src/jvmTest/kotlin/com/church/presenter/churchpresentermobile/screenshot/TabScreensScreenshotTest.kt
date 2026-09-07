package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.hasText
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.AnnouncementsScreen
import com.church.presenter.churchpresentermobile.ui.BibleScreen
import com.church.presenter.churchpresentermobile.ui.ScheduleDrawerContent
import com.church.presenter.churchpresentermobile.ui.SongsTable
import com.church.presenter.churchpresentermobile.ui.WebScreen
import com.church.presenter.churchpresentermobile.viewmodel.AnnouncementsViewModel
import com.church.presenter.churchpresentermobile.viewmodel.BibleViewModel
import com.church.presenter.churchpresentermobile.viewmodel.WebViewModel
import kotlin.test.Test

/**
 * The tabs as whole screens, wired the way the app wires them.
 *
 * Songs, Bible and the schedule drawer are captured in **demo mode** — the
 * sample content the app itself ships for a device with no computer to talk to
 * — so these are pictures of the real screen with real data rather than of a
 * spinner or an error.
 */
class TabScreensScreenshotTest {

    private fun settings() = AppSettings(InMemorySettingsStorage())

    private fun songsTab(): @Composable () -> Unit = {
        SongsTable(appSettings = settings(), isDemoMode = true, settingsSaveToken = 0)
    }

    private fun bibleTab(): @Composable () -> Unit {
        val appSettings = settings()
        val viewModel = BibleViewModel(appSettings, ServerEventService(appSettings), isDemoMode = true)
        return {
            BibleScreen(
                appSettings = appSettings,
                isDemoMode = true,
                settingsSaveToken = 0,
                onNavigationChanged = { _, _ -> },
                onRegisterBackAction = {},
                providedViewModel = viewModel,
            )
        }
    }

    @Test
    fun songs() = screenshot(
        "songs-tab__demo-library",
        until = { onAllNodes(hasText("Amazing", substring = true)).fetchSemanticsNodes().isNotEmpty() },
        content = songsTab(),
    )

    @Test
    fun songsOnATablet() = screenshot(
        "songs-tab__tablet",
        width = Screenshots.TABLET_WIDTH,
        until = { onAllNodes(hasText("Amazing", substring = true)).fetchSemanticsNodes().isNotEmpty() },
        content = songsTab(),
    )

    @Test
    fun bible() = screenshot("bible-tab__demo-library", content = bibleTab())

    @Test
    fun schedule() = screenshot("schedule-drawer__demo") {
        ScheduleDrawerContent(
            appSettings = settings(),
            isDemoMode = true,
            settingsSaveToken = 0,
        )
    }

    @Test
    fun announcements() = screenshot("announcements__initial") {
        AnnouncementsScreen(
            viewModel = AnnouncementsViewModel(settings(), FakeWsSender()),
        )
    }

    @Test
    fun webPage() = screenshot("web__no-bookmarks") {
        WebScreen(viewModel = WebViewModel(settings(), FakeWsSender()))
    }
}
