package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.runtime.Composable
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.SongService
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.mockClient
import com.church.presenter.churchpresentermobile.ui.library.ShareSheetContent
import com.church.presenter.churchpresentermobile.ui.library.SongSyncSection
import com.church.presenter.churchpresentermobile.ui.library.libraryOf
import com.church.presenter.churchpresentermobile.ui.library.song
import com.church.presenter.churchpresentermobile.viewmodel.LibraryShareViewModel
import com.church.presenter.churchpresentermobile.viewmodel.LibrarySyncViewModel
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test

/**
 * Getting content on and off the phone: the songbook copy, and the share sheet.
 *
 * The sync section is captured before anything runs — the state where the
 * operator picks what to copy — and with the desktop refusing, which is what a
 * volunteer actually sees when the laptop is on the wrong network.
 */
class SyncSheetScreenshotTest {

    private val catalogue = """
        {"song-book":[
          {"book-name":"Hymns","song-total":2,"songs":[
            {"id":1,"number":"1","title":"Amazing Grace"},
            {"id":2,"number":"2","title":"How Great Thou Art"}
          ]},
          {"book-name":"Chorus","song-total":1,"songs":[
            {"id":3,"number":"10","title":"Shout to the Lord"}
          ]}
        ]}
    """.trimIndent()

    private fun songSync(
        body: String = catalogue,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): @Composable () -> Unit {
        val settings = AppSettings(InMemorySettingsStorage())
        val repository = LibraryRepository(InMemoryFileStorage()) { 1_000L }
        val service = SongService(settings, FakeWsSender(), mockClient { respond(body, status) })
        val viewModel = LibrarySyncViewModel(repository, settings, service)
        return {
            SongSyncSection(
                repository = LibraryRepository(InMemoryFileStorage()) { 1_000L },
                settings = settings,
                sender = FakeWsSender(),
                onDone = {},
                providedViewModel = viewModel,
            )
        }
    }

    private fun share(songCount: Int): @Composable () -> Unit {
        val repository = libraryOf(
            songs = (1..songCount).map { song("s$it", "$it", "Song number $it") },
        )
        val viewModel = LibraryShareViewModel(repository)
        return { ShareSheetContent(viewModel = viewModel, onMessage = {}) }
    }

    @Test
    fun songSyncIdle() = screenshot("song-sync__idle", content = songSync())

    @Test
    fun songSyncDesktopUnreachable() = screenshot(
        "song-sync__unreachable",
        content = songSync(body = "", status = HttpStatusCode.ServiceUnavailable),
    )

    @Test
    fun shareWithContent() = screenshot("share-sheet__with-songs", content = share(songCount = 12))

    @Test
    fun shareEmptyLibrary() = screenshot("share-sheet__empty-library", content = share(songCount = 0))
}
