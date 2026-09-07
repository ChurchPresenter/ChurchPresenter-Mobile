package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.runtime.Composable
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.ApiConstants
import com.church.presenter.churchpresentermobile.network.BibleDownloadService
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.library.BibleSyncSection
import com.church.presenter.churchpresentermobile.viewmodel.BibleChoiceViewModel
import com.church.presenter.churchpresentermobile.viewmodel.BibleSyncViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test

/**
 * Copying a Bible translation onto the phone.
 *
 * Captured before the download starts — the state where the operator sees what
 * the computer is offering — and with the computer refusing the list, which is
 * the failure a volunteer actually meets.
 */
class BibleSyncScreenshotTest {

    private fun module(title: String) = """
        ##Title: $title
        1 Genesis 50
        -----
        B001C001V001 1 1 1 In the beginning.
    """.trimIndent()

    private fun bibleSync(
        manifest: String = """["en_KJV.spb","ru_RST77.spb"]""",
        manifestStatus: HttpStatusCode = HttpStatusCode.OK,
        installed: List<String> = emptyList(),
    ): @Composable () -> Unit {
        val settings = AppSettings(InMemorySettingsStorage())
        val repository = LocalBibleRepository(InMemoryFileStorage(), now = { 0L })
        installed.forEachIndexed { i, title ->
            repository.install(fileName = "installed_$i.spb", text = module(title))
        }
        val client = HttpClient(MockEngine { request ->
            if (request.url.encodedPath.endsWith(ApiConstants.BIBLE_TRANSLATIONS_ENDPOINT)) {
                if (manifestStatus != HttpStatusCode.OK) respond("boom", manifestStatus)
                else respond(manifest, HttpStatusCode.OK)
            } else {
                respond(module("King James Version"))
            }
        })
        val viewModel = BibleSyncViewModel(repository, settings, BibleDownloadService(settings, client))
        val choice = BibleChoiceViewModel(repository)
        return {
            BibleSyncSection(
                bibles = repository,
                settings = settings,
                providedViewModel = viewModel,
                providedChoice = choice,
            )
        }
    }

    @Test
    fun idle() = screenshot("bible-sync__idle", content = bibleSync())

    @Test
    fun withATranslationInstalled() = screenshot(
        // One already on the phone: the section then has something to list, and
        // a choice of which to present from.
        "bible-sync__one-installed",
        content = bibleSync(installed = listOf("King James Version")),
    )

    @Test
    fun withTwoInstalled() = screenshot(
        "bible-sync__two-installed",
        content = bibleSync(installed = listOf("King James Version", "Synodal")),
    )

    @Test
    fun desktopRefusedTheList() = screenshot(
        "bible-sync__unreachable",
        content = bibleSync(manifestStatus = HttpStatusCode.ServiceUnavailable),
    )
}
