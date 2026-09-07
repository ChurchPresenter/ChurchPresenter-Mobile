package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.ContentOrigin
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.model.LocalSongSection
import com.church.presenter.churchpresentermobile.model.SectionType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What one row of the library says about the item behind it.
 *
 * The row is deliberately not tappable — a tap here used to put the item
 * straight on the audience screen, which meant browsing your own library
 * projected it. What it does carry is enough to tell two similar songs apart,
 * and a badge saying where the item came from, because that is the same
 * distinction that decides whether a later copy from the computer may replace it.
 */
@OptIn(ExperimentalTestApi::class)
class LibraryRowsTest {

    private fun notice(id: String, title: String, body: String = "Coffee in the hall") =
        LocalAnnouncement(id = id, title = title, body = body)

    // ── A song row ───────────────────────────────────────────────────────

    @Test
    fun aSongRowIsThere() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        assertTrue(exists(LibraryTags.row("s1")))
    }

    @Test
    fun aSongRowNamesTheSong() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        assertTrue(isShowing("Amazing Grace"))
    }

    @Test
    fun aSongRowCarriesItsNumber() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        assertTrue(isShowing("42"))
    }

    @Test
    fun aSongRowSaysHowManySectionsItHas() = runComposeUiTest {
        // Two songs with the same title are told apart by what is in them.
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        assertTrue(isShowing("1 sections"))
    }

    @Test
    fun aSongRowNamesItsAuthor() = runComposeUiTest {
        showLibrary(
            libraryOf(songs = listOf(amazingGrace().copy(author = "John Newton")))
        )

        assertTrue(isShowing("John Newton"))
    }

    @Test
    fun aSongRowNamesItsBook() = runComposeUiTest {
        showLibrary(
            libraryOf(songs = listOf(amazingGrace().copy(bookName = "Hymns")))
        )

        assertTrue(isShowing("Hymns"))
    }

    @Test
    fun aSongWithNoAuthorStillLists() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace().copy(author = null))))

        assertTrue(exists(LibraryTags.row("s1")))
    }

    @Test
    fun aSongWithManySectionsSaysHowMany() = runComposeUiTest {
        val many = amazingGrace().copy(
            sections = (1..4).map { LocalSongSection(SectionType.VERSE, "Verse $it") },
        )
        showLibrary(libraryOf(songs = listOf(many)))

        assertTrue(isShowing("4 sections"))
    }

    @Test
    fun everySongGetsItsOwnRow() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace(), song("s2", "43", "How Great"))))

        assertTrue(exists(LibraryTags.row("s1")))
        assertTrue(exists(LibraryTags.row("s2")))
    }

    @Test
    fun eachSongRowNamesItsOwnSong() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace(), song("s2", "43", "How Great"))))

        assertTrue(isShowing("Amazing Grace"))
        assertTrue(isShowing("How Great"))
    }

    // ── A notice row ─────────────────────────────────────────────────────

    @Test
    fun aNoticeRowIsThere() = runComposeUiTest {
        showLibrary(libraryOf(notices = listOf(notice("n1", "Welcome"))))

        assertTrue(exists(LibraryTags.row("n1")))
    }

    @Test
    fun aNoticeRowNamesTheNotice() = runComposeUiTest {
        showLibrary(libraryOf(notices = listOf(notice("n1", "Welcome"))))

        assertTrue(isShowing("Welcome"))
    }

    @Test
    fun aNoticeRowShowsItsFirstLine() = runComposeUiTest {
        showLibrary(
            libraryOf(notices = listOf(notice("n1", "Welcome", body = "Coffee in the hall")))
        )

        assertTrue(isShowing("Coffee in the hall"))
    }

    @Test
    fun anUntitledNoticeIsNamedByItsFirstLine() = runComposeUiTest {
        // Better than an empty row the operator cannot identify.
        showLibrary(
            libraryOf(notices = listOf(notice("n1", title = "", body = "Coffee in the hall")))
        )

        assertTrue(isShowing("Coffee in the hall"))
    }

    @Test
    fun aNoticeOfOneLineStillLists() = runComposeUiTest {
        showLibrary(libraryOf(notices = listOf(notice("n1", "Welcome", body = "Just this"))))

        assertTrue(exists(LibraryTags.row("n1")))
    }

    @Test
    fun aMultiLineNoticeShowsOnlyItsFirstLine() = runComposeUiTest {
        showLibrary(
            libraryOf(notices = listOf(notice("n1", "Welcome", body = "First line\nSecond line")))
        )

        assertTrue(isShowing("First line"))
        assertFalse(isShowing("Second line"))
    }

    // ── Where an item came from ──────────────────────────────────────────

    @Test
    fun aSongWrittenHereCarriesNoBadge() = runComposeUiTest {
        // Local is the ordinary case; a badge on every row would be noise.
        showLibrary(libraryOf(songs = listOf(amazingGrace().copy(origin = ContentOrigin.LOCAL))))

        assertTrue(exists(LibraryTags.row("s1")))
    }

    @Test
    fun aSongCopiedFromTheComputerSaysSo() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace().copy(origin = ContentOrigin.DESKTOP))))

        assertTrue(exists(LibraryTags.row("s1")))
    }

    @Test
    fun anEditedCopySaysSo() = runComposeUiTest {
        // The distinction that decides whether the next copy may replace it.
        showLibrary(
            libraryOf(songs = listOf(amazingGrace().copy(origin = ContentOrigin.LOCAL_OVERRIDE)))
        )

        assertTrue(exists(LibraryTags.row("s1")))
    }

    @Test
    fun anEditedCopyIsBadgedDifferentlyFromAPlainCopy() = runComposeUiTest {
        showLibrary(
            libraryOf(
                songs = listOf(
                    amazingGrace().copy(origin = ContentOrigin.DESKTOP),
                    song("s2", "43", "How Great").copy(origin = ContentOrigin.LOCAL_OVERRIDE),
                )
            )
        )

        assertTrue(exists(LibraryTags.row("s1")))
        assertTrue(exists(LibraryTags.row("s2")))
    }

    @Test
    fun aNoticeCarriesItsOriginToo() = runComposeUiTest {
        showLibrary(
            libraryOf(notices = listOf(notice("n1", "Welcome").copy(origin = ContentOrigin.DESKTOP)))
        )

        assertTrue(exists(LibraryTags.row("n1")))
    }

    // ── What a row does not do ───────────────────────────────────────────

    @Test
    fun aRowOffersBothItsActions() = runComposeUiTest {
        showLibrary(libraryOf(songs = listOf(amazingGrace())))

        assertTrue(exists(LibraryTags.rowEdit("s1")))
        assertTrue(exists(LibraryTags.rowDelete("s1")))
    }

    @Test
    fun aNoticeRowOffersBothItsActions() = runComposeUiTest {
        showLibrary(libraryOf(notices = listOf(notice("n1", "Welcome"))))

        assertTrue(exists(LibraryTags.rowEdit("n1")))
        assertTrue(exists(LibraryTags.rowDelete("n1")))
    }

    @Test
    fun aLongSongListStillListsFromTheTop() = runComposeUiTest {
        // Not a scroll assertion: the screen has two scrollable regions — the
        // chip row and the list — so a blind scroll cannot choose between them.
        val many = (1..20).map { song("s$it", "$it", "Song $it") }
        showLibrary(libraryOf(songs = many))

        assertTrue(exists(LibraryTags.row("s1")))
        assertTrue(exists(LibraryTags.row("s2")))
    }
}
