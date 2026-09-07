package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.isShowing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Putting one of this phone's own notices on the audience screen.
 *
 * The Library tab keeps notices; this screen presents them, and the split is
 * deliberate — a tap in the Library used to project whatever was tapped, so
 * browsing your own content put it on the wall. Here going live is an explicit
 * press, and what it must do is reach the output: "the row highlighted" is not
 * the same claim as "the words are on the screen".
 */
@OptIn(ExperimentalTestApi::class)
class LocalNoticesScreenTest {

    // ── Nothing to show ──────────────────────────────────────────────────

    @Test
    fun anEmptyLibrarySaysThereAreNoNotices() = runComposeUiTest {
        val f = StandaloneFixture()

        showLocalNotices(libraryWith(), f.engine)

        assertTrue(exists(StandaloneTags.NOTICES_EMPTY))
    }

    @Test
    fun anEmptyLibraryOffersNoRows() = runComposeUiTest {
        val f = StandaloneFixture()

        showLocalNotices(libraryWith(), f.engine)

        assertFalse(exists(StandaloneTags.notice("n1")))
    }

    @Test
    fun aScreenWithNoOutputSaysSo() = runComposeUiTest {
        // Pressing "go live" with nothing to project onto is the most common
        // first-run confusion.
        val f = StandaloneFixture()

        showLocalNotices(libraryWith(notice("n1")), f.engine, hasOutput = false)

        assertTrue(exists(StandaloneTags.NO_OUTPUT))
    }

    @Test
    fun aScreenWithAnOutputDoesNotNagAboutIt() = runComposeUiTest {
        val f = StandaloneFixture()

        showLocalNotices(libraryWith(notice("n1")), f.engine)

        assertFalse(exists(StandaloneTags.NO_OUTPUT))
    }

    @Test
    fun theWarningSitsAboveTheNoticesRatherThanReplacingThem() = runComposeUiTest {
        val f = StandaloneFixture()

        showLocalNotices(libraryWith(notice("n1")), f.engine, hasOutput = false)

        assertTrue(exists(StandaloneTags.NO_OUTPUT))
        assertTrue(exists(StandaloneTags.notice("n1")))
    }

    @Test
    fun anEmptyLibraryWithNoOutputSaysBoth() = runComposeUiTest {
        val f = StandaloneFixture()

        showLocalNotices(libraryWith(), f.engine, hasOutput = false)

        assertTrue(exists(StandaloneTags.NO_OUTPUT))
        assertTrue(exists(StandaloneTags.NOTICES_EMPTY))
    }

    // ── The list ─────────────────────────────────────────────────────────

    @Test
    fun aNoticeInTheLibraryIsListed() = runComposeUiTest {
        val f = StandaloneFixture()

        showLocalNotices(libraryWith(notice("n1")), f.engine)

        assertTrue(exists(StandaloneTags.notice("n1")))
    }

    @Test
    fun everyNoticeIsListed() = runComposeUiTest {
        val f = StandaloneFixture()

        showLocalNotices(libraryWith(notice("n1"), notice("n2", title = "Offering")), f.engine)

        assertTrue(exists(StandaloneTags.notice("n1")))
        assertTrue(exists(StandaloneTags.notice("n2")))
    }

    @Test
    fun aNoticeShowsItsTitle() = runComposeUiTest {
        val f = StandaloneFixture()

        showLocalNotices(libraryWith(notice("n1", title = "Welcome")), f.engine)

        assertTrue(isShowing("Welcome"))
    }

    @Test
    fun aNoticeShowsItsFirstLine() = runComposeUiTest {
        // Enough to tell two notices apart without opening either.
        val f = StandaloneFixture()

        showLocalNotices(libraryWith(notice("n1", body = "Coffee in the hall")), f.engine)

        assertTrue(isShowing("Coffee in the hall"))
    }

    @Test
    fun aNoticeWithNoTitleIsNamedByItsWords() = runComposeUiTest {
        // An untitled row would otherwise be a blank line in a list.
        val f = StandaloneFixture()

        showLocalNotices(libraryWith(notice("n1", title = "", body = "Coffee in the hall")), f.engine)

        assertTrue(isShowing("Coffee in the hall"))
    }

    @Test
    fun aNoticeAddedToTheLibraryAppearsHere() = runComposeUiTest {
        // The list follows the Library tab rather than a snapshot taken once.
        val f = StandaloneFixture()
        val repository = libraryWith(notice("n1"))
        showLocalNotices(repository, f.engine)

        repository.upsertAnnouncement(notice("n2", title = "Offering"))

        awaitThat { exists(StandaloneTags.notice("n2")) }
    }

    @Test
    fun aNoticeDeletedInTheLibraryLeavesHere() = runComposeUiTest {
        val f = StandaloneFixture()
        val repository = libraryWith(notice("n1"), notice("n2", title = "Offering"))
        showLocalNotices(repository, f.engine)

        repository.deleteAnnouncement("n2")

        awaitThat { !exists(StandaloneTags.notice("n2")) }
    }

    @Test
    fun aNoticeRewrittenInTheLibraryReadsTheNewWords() = runComposeUiTest {
        val f = StandaloneFixture()
        val repository = libraryWith(notice("n1", body = "Coffee in the hall"))
        showLocalNotices(repository, f.engine)

        repository.upsertAnnouncement(notice("n1", body = "Coffee is cancelled"))

        awaitThat { isShowing("Coffee is cancelled") }
    }

    @Test
    fun emptyingTheLibraryLeavesTheEmptyMessage() = runComposeUiTest {
        val f = StandaloneFixture()
        val repository = libraryWith(notice("n1"))
        showLocalNotices(repository, f.engine)

        repository.deleteAnnouncement("n1")

        awaitThat { exists(StandaloneTags.NOTICES_EMPTY) }
    }

    // ── Going live ───────────────────────────────────────────────────────

    @Test
    fun goingLivePutsTheWordsOnTheScreen() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalNotices(libraryWith(notice("n1", body = "Coffee in the hall")), f.engine)

        click(StandaloneTags.noticeProject("n1"))

        awaitThat { f.liveText?.contains("Coffee in the hall") == true }
    }

    @Test
    fun goingLiveSendsTheRightNotice() = runComposeUiTest {
        // Two rows on screen and the other one's words projected is a real bug
        // that a "did it project" test never sees.
        val f = StandaloneFixture()
        showLocalNotices(
            libraryWith(
                notice("n1", title = "Welcome", body = "Coffee in the hall"),
                notice("n2", title = "Offering", body = "Cash or card"),
            ),
            f.engine,
        )

        click(StandaloneTags.noticeProject("n2"))

        awaitThat { f.liveText?.contains("Cash or card") == true }
    }

    @Test
    fun goingLiveLoadsADeck() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalNotices(libraryWith(notice("n1")), f.engine)

        click(StandaloneTags.noticeProject("n1"))

        awaitThat { !f.engine.deck.value.isEmpty }
    }

    @Test
    fun theLiveNoticeOffersAWayToClearIt() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalNotices(libraryWith(notice("n1")), f.engine)

        click(StandaloneTags.noticeProject("n1"))

        awaitThat { exists(StandaloneTags.noticeClear("n1")) }
    }

    @Test
    fun aNoticeThatIsNotLiveOffersNoClear() = runComposeUiTest {
        // Clearing from a row that is not on screen would be a control with no
        // meaning.
        val f = StandaloneFixture()
        showLocalNotices(libraryWith(notice("n1"), notice("n2", title = "Offering")), f.engine)

        click(StandaloneTags.noticeProject("n1"))

        awaitThat { exists(StandaloneTags.noticeClear("n1")) }
        assertFalse(exists(StandaloneTags.noticeClear("n2")))
    }

    @Test
    fun nothingIsLiveBeforeAPress() = runComposeUiTest {
        val f = StandaloneFixture()

        showLocalNotices(libraryWith(notice("n1")), f.engine)

        assertFalse(exists(StandaloneTags.noticeClear("n1")))
    }

    @Test
    fun clearingTakesTheNoticeOffTheScreen() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalNotices(libraryWith(notice("n1")), f.engine)
        click(StandaloneTags.noticeProject("n1"))
        awaitThat { exists(StandaloneTags.noticeClear("n1")) }

        click(StandaloneTags.noticeClear("n1"))

        awaitThat { f.engine.deck.value.isEmpty }
    }

    @Test
    fun clearingTakesTheClearButtonAway() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalNotices(libraryWith(notice("n1")), f.engine)
        click(StandaloneTags.noticeProject("n1"))
        awaitThat { exists(StandaloneTags.noticeClear("n1")) }

        click(StandaloneTags.noticeClear("n1"))

        awaitThat { !exists(StandaloneTags.noticeClear("n1")) }
    }

    @Test
    fun theRowItselfAlsoGoesLive() = runComposeUiTest {
        // The whole card is the press; hunting for a small button in a service
        // is worse than a large target.
        val f = StandaloneFixture()
        showLocalNotices(libraryWith(notice("n1", body = "Coffee in the hall")), f.engine)

        click(StandaloneTags.notice("n1"))

        awaitThat { f.liveText?.contains("Coffee in the hall") == true }
    }

    @Test
    fun projectingASecondNoticeReplacesTheFirst() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalNotices(
            libraryWith(
                notice("n1", body = "Coffee in the hall"),
                notice("n2", title = "Offering", body = "Cash or card"),
            ),
            f.engine,
        )
        click(StandaloneTags.noticeProject("n1"))
        awaitThat { f.liveText?.contains("Coffee in the hall") == true }

        click(StandaloneTags.noticeProject("n2"))

        awaitThat { f.liveText?.contains("Cash or card") == true }
    }

    @Test
    fun projectingASecondNoticeMovesTheClearButton() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalNotices(libraryWith(notice("n1"), notice("n2", title = "Offering")), f.engine)
        click(StandaloneTags.noticeProject("n1"))
        awaitThat { exists(StandaloneTags.noticeClear("n1")) }

        click(StandaloneTags.noticeProject("n2"))

        awaitThat { exists(StandaloneTags.noticeClear("n2")) }
        assertFalse(exists(StandaloneTags.noticeClear("n1")))
    }

    @Test
    fun aNoticeCanBeProjectedAgainAfterClearing() = runComposeUiTest {
        // The deck is supplied on every press rather than relying on one loaded
        // earlier, so whatever cleared the screen cannot leave this stuck.
        val f = StandaloneFixture()
        showLocalNotices(libraryWith(notice("n1", body = "Coffee in the hall")), f.engine)
        click(StandaloneTags.noticeProject("n1"))
        awaitThat { exists(StandaloneTags.noticeClear("n1")) }
        click(StandaloneTags.noticeClear("n1"))
        awaitThat { f.engine.deck.value.isEmpty }

        click(StandaloneTags.noticeProject("n1"))

        awaitThat { f.liveText?.contains("Coffee in the hall") == true }
    }

    @Test
    fun aNoticesTitleRidesWithIt() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalNotices(libraryWith(notice("n1", title = "Welcome")), f.engine)

        click(StandaloneTags.noticeProject("n1"))

        awaitThat { !f.engine.deck.value.isEmpty }
        assertTrue(f.engine.deck.value.slides.isNotEmpty())
    }

    @Test
    fun aMultiLineNoticeKeepsItsLines() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalNotices(
            libraryWith(notice("n1", body = "Coffee in the hall\nAfter the service")),
            f.engine,
        )

        click(StandaloneTags.noticeProject("n1"))

        awaitThat { f.liveText?.contains("After the service") == true }
    }

    @Test
    fun theListStillWorksAfterProjecting() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalNotices(libraryWith(notice("n1"), notice("n2", title = "Offering")), f.engine)

        click(StandaloneTags.noticeProject("n1"))

        awaitThat { exists(StandaloneTags.notice("n2")) }
    }

    // ── With no presenter at all ─────────────────────────────────────────

    @Test
    fun withoutAPresenterTheListStillOpens() = runComposeUiTest {
        // Remote mode has no local presenter; the screen must not fall over.
        showLocalNotices(libraryWith(notice("n1")), engine = null)

        assertTrue(exists(StandaloneTags.notice("n1")))
    }

    @Test
    fun withoutAPresenterPressingGoLiveIsHarmless() = runComposeUiTest {
        showLocalNotices(libraryWith(notice("n1")), engine = null)

        click(StandaloneTags.noticeProject("n1"))

        assertTrue(exists(StandaloneTags.notice("n1")))
    }

    @Test
    fun withoutAPresenterNothingBecomesLive() = runComposeUiTest {
        showLocalNotices(libraryWith(notice("n1")), engine = null)

        click(StandaloneTags.noticeProject("n1"))

        assertFalse(exists(StandaloneTags.noticeClear("n1")))
    }

    @Test
    fun aNoticeWithABlankBodyStillHasARow() = runComposeUiTest {
        val f = StandaloneFixture()

        showLocalNotices(libraryWith(LocalAnnouncement(id = "n1", title = "Welcome", body = "")), f.engine)

        assertTrue(exists(StandaloneTags.notice("n1")))
    }

    @Test
    fun theScreenShowsTheNoticesInLibraryOrder() = runComposeUiTest {
        // The Library is where the order is decided; this screen does not
        // re-sort it under the operator.
        val f = StandaloneFixture()
        val repository = libraryWith(notice("n1"), notice("n2", title = "Offering"))

        showLocalNotices(repository, f.engine)

        assertEquals(listOf("n1", "n2"), repository.announcements.map { it.id })
    }
}
