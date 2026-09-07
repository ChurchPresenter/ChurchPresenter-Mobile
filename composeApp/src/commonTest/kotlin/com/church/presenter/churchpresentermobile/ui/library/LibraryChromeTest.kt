package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.SyncRequestHandler
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.ContentOrigin
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import kotlin.time.Clock
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The furniture around the library list: where an item came from, when the
 * phone last copied anything, and the other tabs' way of asking for the copy
 * sheet.
 *
 * The origin badge is not decoration — it is the same distinction that decides
 * whether the next desktop copy may overwrite an item, so an edited song that
 * stops saying it was edited is a warning that has gone missing. The sync
 * request is how an empty Songs or Bible tab offers a way out; landing on the
 * wrong half of the sheet sends the operator to download a Bible when they
 * wanted songs.
 */
@OptIn(ExperimentalTestApi::class)
class LibraryChromeTest {

    @AfterTest
    fun clearRequest() {
        // The handler is a singleton shared by every tab; a request left behind
        // opens a sheet in an unrelated test.
        SyncRequestHandler.consume()
    }

    private fun notice(id: String, title: String, origin: ContentOrigin = ContentOrigin.LOCAL) =
        LocalAnnouncement(id = id, title = title, body = "Coffee in the hall", origin = origin)

    private fun settingsSyncedAt(epochMs: Long): AppSettings {
        val settings = AppSettings(InMemorySettingsStorage())
        settings.librarySyncStateJson = """{"lastSyncEpochMs":$epochMs,"songCount":42}"""
        return settings
    }

    private fun nowMs() = Clock.System.now().toEpochMilliseconds()

    // ── Where an item came from ──────────────────────────────────────────

    @Test
    fun aSongWrittenHereCarriesNoBadge() = runComposeUiTest {
        // Everything would be badged otherwise, which says nothing at all.
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        assertFalse(exists(LibraryTags.rowOrigin("s1")))
    }

    @Test
    fun aSongCopiedFromTheDesktopSaysSo() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace().copy(origin = ContentOrigin.DESKTOP))))

        assertTrue(exists(LibraryTags.rowOrigin("s1")))
    }

    @Test
    fun anEditedSongSaysSo() = runComposeUiTest {
        // This is the badge that matters: it is why the next copy will not
        // overwrite the operator's words.
        showLibrary(libraryOf(songs = listOf(amazingGrace().copy(origin = ContentOrigin.LOCAL_OVERRIDE))))

        assertTrue(exists(LibraryTags.rowOrigin("s1")))
    }

    @Test
    fun aNoticeWrittenHereCarriesNoBadge() = runComposeUiTest {
        showLibrary(libraryOf(notices = listOf(notice("n1", "Welcome"))))

        assertFalse(exists(LibraryTags.rowOrigin("n1")))
    }

    @Test
    fun aNoticeCopiedFromTheDesktopSaysSo() = runComposeUiTest {
        showLibrary(libraryOf(notices = listOf(notice("n1", "Welcome", ContentOrigin.DESKTOP))))

        assertTrue(exists(LibraryTags.rowOrigin("n1")))
    }

    @Test
    fun anEditedNoticeSaysSo() = runComposeUiTest {
        showLibrary(libraryOf(notices = listOf(notice("n1", "Welcome", ContentOrigin.LOCAL_OVERRIDE))))

        assertTrue(exists(LibraryTags.rowOrigin("n1")))
    }

    @Test
    fun eachRowCarriesItsOwnBadge() = runComposeUiTest {
        // Badging the wrong row is the same failure as badging none.
        showLibrary(
            libraryOf(
                songs = listOf(
                    amazingGrace(),
                    song("s2", "43", "How Great").copy(origin = ContentOrigin.DESKTOP),
                )
            )
        )

        assertFalse(exists(LibraryTags.rowOrigin("s1")))
        assertTrue(exists(LibraryTags.rowOrigin("s2")))
    }

    @Test
    fun aBadgedRowIsStillEditable() = runComposeUiTest {
        var edited: String? = null
        showLibrary(
            libraryOf(songs = listOf(amazingGrace().copy(origin = ContentOrigin.DESKTOP))),
            onEditSong = { edited = it },
        )

        click(LibraryTags.rowEdit("s1"))

        assertTrue(edited == "s1")
    }

    @Test
    fun aBadgedRowIsStillDeletable() = runComposeUiTest {
        val repo = libraryOf(songs = listOf(amazingGrace().copy(origin = ContentOrigin.DESKTOP)))
        showLibrary(repo)

        click(LibraryTags.rowDelete("s1"))
        click(LibraryTags.DELETE_CONFIRM)

        awaitThat { repo.library.value.songs.isEmpty() }
    }

    @Test
    fun aBadgedRowStillShowsItsTitle() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace().copy(origin = ContentOrigin.LOCAL_OVERRIDE))))

        assertTrue(exists(LibraryTags.row("s1")))
    }

    @Test
    fun aBadgedNoticeWithNoTitleStillHasARow() = runComposeUiTest {
        // The body's first line stands in for a title; the badge sits beside it.
        showLibrary(
            libraryOf(
                notices = listOf(
                    LocalAnnouncement(
                        id = "n1",
                        title = "",
                        body = "Coffee in the hall",
                        origin = ContentOrigin.DESKTOP,
                    )
                )
            )
        )

        assertTrue(exists(LibraryTags.row("n1")))
        assertTrue(exists(LibraryTags.rowOrigin("n1")))
    }

    // ── Another tab asking for the copy sheet ────────────────────────────

    @Test
    fun aRequestFromAnotherTabOpensTheCopySheet() = runComposeUiTest {
        // An empty Songs tab's only useful action.
        SyncRequestHandler.request(SyncSection.SONGS)

        showLibrary(libraryOf())

        awaitThat { exists(LibraryTags.SYNC_BUTTON) }
    }

    @Test
    fun aRequestForTheBibleHalfOpensOnTheBibleHalf() = runComposeUiTest {
        // Landing on the songs half would send them to copy a songbook they did
        // not ask for.
        SyncRequestHandler.request(SyncSection.BIBLE)

        showLibrary(libraryOf())

        awaitThat { exists(LibraryTags.BIBLE_SYNC_FIND) }
    }

    @Test
    fun aRequestForTheSongsHalfOpensOnTheSongsHalf() = runComposeUiTest {
        SyncRequestHandler.request(SyncSection.SONGS)

        showLibrary(libraryOf())

        awaitThat { exists(LibraryTags.SYNC_BUTTON) }
        tagged(LibraryTags.syncSection(0)).assertIsSelected()
    }

    @Test
    fun aRequestIsConsumedOnceItHasOpenedTheSheet() = runComposeUiTest {
        // Otherwise every later recomposition reopens it.
        SyncRequestHandler.request(SyncSection.SONGS)

        showLibrary(libraryOf())

        awaitThat { SyncRequestHandler.requested.value == null }
    }

    @Test
    fun noRequestLeavesTheSheetClosed() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        assertFalse(exists(LibraryTags.SYNC_BUTTON))
    }

    @Test
    fun aRequestArrivingWhileTheTabIsOpenStillOpensTheSheet() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        SyncRequestHandler.request(SyncSection.BIBLE)

        awaitThat { exists(LibraryTags.BIBLE_SYNC_FIND) }
    }

    @Test
    fun aRequestDoesNotDisturbTheList() = runComposeUiTest {
        SyncRequestHandler.request(SyncSection.SONGS)

        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        awaitThat { exists(LibraryTags.SYNC_BUTTON) }
        assertTrue(exists(LibraryTags.row("s1")))
    }

    @Test
    fun aSecondRequestIsConsumedToo() = runComposeUiTest {
        // A request left standing would reopen the sheet on some later
        // recomposition, over whatever the operator had moved on to.
        SyncRequestHandler.request(SyncSection.SONGS)
        showLibrary(libraryOf())
        awaitThat { SyncRequestHandler.requested.value == null }

        SyncRequestHandler.request(SyncSection.BIBLE)

        awaitThat { SyncRequestHandler.requested.value == null }
    }

    // ── When the phone last copied anything ──────────────────────────────

    @Test
    fun aPhoneThatHasNeverCopiedStillShowsTheChip() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        assertTrue(exists(LibraryTags.SYNC_CHIP))
    }

    @Test
    fun aCopyMomentsAgoShowsTheChip() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())), settings = settingsSyncedAt(nowMs()))

        assertTrue(exists(LibraryTags.SYNC_CHIP))
    }

    @Test
    fun aCopyMinutesAgoShowsTheChip() = runComposeUiTest {
        showLibrary(
            libraryOf(songs = listOf(amazingGrace())),
            settings = settingsSyncedAt(nowMs() - 10 * 60 * 1_000L),
        )

        assertTrue(exists(LibraryTags.SYNC_CHIP))
    }

    @Test
    fun aCopyHoursAgoShowsTheChip() = runComposeUiTest {
        showLibrary(
            libraryOf(songs = listOf(amazingGrace())),
            settings = settingsSyncedAt(nowMs() - 5 * 60 * 60 * 1_000L),
        )

        assertTrue(exists(LibraryTags.SYNC_CHIP))
    }

    @Test
    fun aCopyDaysAgoShowsTheChip() = runComposeUiTest {
        showLibrary(
            libraryOf(songs = listOf(amazingGrace())),
            settings = settingsSyncedAt(nowMs() - 3 * 24 * 60 * 60 * 1_000L),
        )

        assertTrue(exists(LibraryTags.SYNC_CHIP))
    }

    @Test
    fun aSyncRecordedInTheFutureStillShowsTheChip() = runComposeUiTest {
        // A device correcting its clock, or a state copied off another phone.
        showLibrary(
            libraryOf(songs = listOf(amazingGrace())),
            settings = settingsSyncedAt(nowMs() + 60 * 60 * 1_000L),
        )

        assertTrue(exists(LibraryTags.SYNC_CHIP))
    }

    @Test
    fun anOldCopyStillOpensTheCopySheet() = runComposeUiTest {
        showLibrary(
            libraryOf(songs = listOf(amazingGrace())),
            settings = settingsSyncedAt(nowMs() - 30L * 24 * 60 * 60 * 1_000L),
        )

        click(LibraryTags.SYNC_CHIP)

        awaitThat { exists(LibraryTags.SYNC_BUTTON) }
    }

    @Test
    fun aRecentCopyStillOpensTheCopySheet() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())), settings = settingsSyncedAt(nowMs()))

        click(LibraryTags.SYNC_CHIP)

        awaitThat { exists(LibraryTags.SYNC_BUTTON) }
    }

    @Test
    fun aSyncStateWithNoTimestampStillShowsTheChip() = runComposeUiTest {
        val settings = AppSettings(InMemorySettingsStorage())
        settings.librarySyncStateJson = """{"songCount":42}"""
        showLibrary(libraryOf(songs = listOf(amazingGrace())), settings = settings)

        assertTrue(exists(LibraryTags.SYNC_CHIP))
    }

    @Test
    fun aSyncStateWithUnknownFieldsStillShowsTheChip() = runComposeUiTest {
        // Written by a later version of the app than this one.
        val settings = AppSettings(InMemorySettingsStorage())
        settings.librarySyncStateJson =
            """{"lastSyncEpochMs":${nowMs()},"songCount":42,"somethingNew":true}"""
        showLibrary(libraryOf(songs = listOf(amazingGrace())), settings = settings)

        assertTrue(exists(LibraryTags.SYNC_CHIP))
    }

    @Test
    fun theChipSitsBesideTheShareChip() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())), settings = settingsSyncedAt(nowMs()))

        assertTrue(exists(LibraryTags.SYNC_CHIP))
        assertTrue(exists(LibraryTags.SHARE_CHIP))
    }
}
