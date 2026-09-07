package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.runtime.Composable
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.library.LibraryScreen
import com.church.presenter.churchpresentermobile.ui.library.amazingGrace
import com.church.presenter.churchpresentermobile.ui.library.biblesWith
import com.church.presenter.churchpresentermobile.ui.library.libraryOf
import com.church.presenter.churchpresentermobile.ui.library.song
import kotlin.test.Test

/**
 * The on-device library — the content a standalone phone presents from.
 *
 * Built on the same in-memory repository the behavioural library tests use, so
 * each state here is a real repository with real content in it rather than a
 * screen handed a list.
 *
 * The states worth a picture are the ones about what the phone HAS: nothing at
 * all, songs but no translation (the Bible chip is not offered), notices, and a
 * library with everything.
 */
class LibraryScreenshotTest {

    private val songs = listOf(
        amazingGrace(),
        song("s2", "104", "How Great Thou Art"),
        song("s3", "7", "In Christ Alone"),
    )

    private val notices = listOf(
        LocalAnnouncement(id = "a1", title = "Welcome", body = "Tea and coffee after the service."),
        LocalAnnouncement(id = "a2", title = "Working bee", body = "Saturday 9am — bring gloves."),
    )

    private fun library(
        songs: List<LocalSong> = emptyList(),
        notices: List<LocalAnnouncement> = emptyList(),
        bibles: LocalBibleRepository = LocalBibleRepository(InMemoryFileStorage()),
    ): @Composable () -> Unit {
        val repository = libraryOf(songs = songs, notices = notices)
        val settings = AppSettings(InMemorySettingsStorage())
        return {
            LibraryScreen(
                repository = repository,
                bibles = bibles,
                settings = settings,
                sender = FakeWsSender(),
                onEditSong = {},
                onEditAnnouncement = {},
            )
        }
    }

    @Test
    fun empty() = screenshot("library__empty", content = library())

    @Test
    fun songsOnly() = screenshot("library__songs", content = library(songs = songs))

    @Test
    fun noticesOnly() = screenshot("library__notices", content = library(notices = notices))

    @Test
    fun everything() = screenshot(
        "library__songs-notices-and-bible",
        content = library(songs = songs, notices = notices, bibles = biblesWith("King James Version")),
    )

    @Test
    fun withTwoTranslations() = screenshot(
        // Two installed translations means the library has to offer a choice
        // rather than just say a Bible is present.
        "library__two-bibles",
        content = library(
            songs = songs,
            bibles = biblesWith("King James Version", "World English Bible"),
        ),
    )

    @Test
    fun tablet() = screenshot(
        "library__tablet",
        width = Screenshots.TABLET_WIDTH,
        content = library(songs = songs, notices = notices),
    )
}
