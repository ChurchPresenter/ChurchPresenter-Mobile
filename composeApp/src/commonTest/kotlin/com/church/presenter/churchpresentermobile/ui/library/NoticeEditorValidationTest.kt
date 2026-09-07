package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.LibraryValidation
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What the notice editor will and will not accept.
 *
 * A notice is the one thing in the app written minutes before it goes on the
 * wall, so the gate is deliberately light: the body is the only thing required,
 * and everything else — a title, a body that runs long — is the author's call.
 * The two ends still have to hold: nothing to show is not a notice, and a body
 * too long for a screen would project as a wall of unreadable text.
 */
@OptIn(ExperimentalTestApi::class)
class NoticeEditorValidationTest {

    private fun repository() = LibraryRepository(InMemoryFileStorage()) { 1_000L }

    private val longBody = (1..20).joinToString("\n") { "Line $it" }

    private val oversizedBody = "x".repeat(LibraryValidation.MAX_SECTION_CHARS + 1)

    private fun notice(id: String = "n1", title: String = "Welcome", body: String = "Coffee in the hall") =
        LocalAnnouncement(id = id, title = title, body = body)

    // ── Nothing to show ──────────────────────────────────────────────────

    @Test
    fun anEmptyNoticeCannotBeSaved() = runComposeUiTest {
        showNoticeEditor(repository())

        tagged(LibraryTags.SAVE).assertIsNotEnabled()
    }

    @Test
    fun aTitleWithNoBodyCannotBeSaved() = runComposeUiTest {
        // The body is what goes on the wall; a title alone projects nothing.
        showNoticeEditor(repository())

        type(LibraryTags.FIELD_TITLE, "Welcome")

        tagged(LibraryTags.SAVE).assertIsNotEnabled()
    }

    @Test
    fun aBodyOfSpacesIsNoBody() = runComposeUiTest {
        showNoticeEditor(repository())

        type(LibraryTags.FIELD_BODY, "   ")

        tagged(LibraryTags.SAVE).assertIsNotEnabled()
    }

    @Test
    fun anEmptyNoticeSaysWhatIsMissing() = runComposeUiTest {
        showNoticeEditor(repository())

        type(LibraryTags.FIELD_BODY, "Coffee")
        type(LibraryTags.FIELD_BODY, "")

        awaitThat { isShowing("Add some text") }
    }

    @Test
    fun aBodyIsEnoughOnItsOwn() = runComposeUiTest {
        // A notice written two minutes before the service needs no title.
        showNoticeEditor(repository())

        type(LibraryTags.FIELD_BODY, "Coffee in the hall")

        tagged(LibraryTags.SAVE).assertIsEnabled()
    }

    @Test
    fun aBodyOnItsOwnIsKept() = runComposeUiTest {
        val repo = repository()
        showNoticeEditor(repo)

        type(LibraryTags.FIELD_BODY, "Coffee in the hall")
        tagged(LibraryTags.SAVE).performClick()

        awaitThat { repo.announcements.isNotEmpty() }
        assertEquals("Coffee in the hall", repo.announcements.single().body)
    }

    @Test
    fun aTitleIsKeptWhenGiven() = runComposeUiTest {
        val repo = repository()
        showNoticeEditor(repo)

        type(LibraryTags.FIELD_TITLE, "Welcome")
        type(LibraryTags.FIELD_BODY, "Coffee in the hall")
        tagged(LibraryTags.SAVE).performClick()

        awaitThat { repo.announcements.isNotEmpty() }
        assertEquals("Welcome", repo.announcements.single().title)
    }

    @Test
    fun emptyingTheBodyAgainStopsTheSave() = runComposeUiTest {
        showNoticeEditor(repository())
        type(LibraryTags.FIELD_BODY, "Coffee in the hall")
        tagged(LibraryTags.SAVE).assertIsEnabled()

        type(LibraryTags.FIELD_BODY, "")

        tagged(LibraryTags.SAVE).assertIsNotEnabled()
    }

    // ── Too much to show ─────────────────────────────────────────────────

    @Test
    fun aBodyTooLongForAScreenCannotBeSaved() = runComposeUiTest {
        showNoticeEditor(repository())

        type(LibraryTags.FIELD_BODY, oversizedBody)

        tagged(LibraryTags.SAVE).assertIsNotEnabled()
    }

    @Test
    fun aBodyTooLongForAScreenSaysWhy() = runComposeUiTest {
        showNoticeEditor(repository())

        type(LibraryTags.FIELD_BODY, oversizedBody)

        awaitThat { isShowing("Too long") }
    }

    @Test
    fun anOversizedNoticeIsNotWritten() = runComposeUiTest {
        val repo = repository()
        showNoticeEditor(repo)

        type(LibraryTags.FIELD_BODY, oversizedBody)
        click(LibraryTags.SAVE)

        assertTrue(repo.announcements.isEmpty())
    }

    @Test
    fun shorteningAnOversizedNoticeAllowsTheSave() = runComposeUiTest {
        showNoticeEditor(repository())
        type(LibraryTags.FIELD_BODY, oversizedBody)
        tagged(LibraryTags.SAVE).assertIsNotEnabled()

        type(LibraryTags.FIELD_BODY, "Coffee in the hall")

        tagged(LibraryTags.SAVE).assertIsEnabled()
    }

    // ── Long, but the author's call ──────────────────────────────────────

    @Test
    fun aLongNoticeIsWarnedAbout() = runComposeUiTest {
        showNoticeEditor(repository())

        type(LibraryTags.FIELD_BODY, longBody)

        awaitThat { isShowing("may not fit") }
    }

    @Test
    fun aLongNoticeCanStillBeSaved() = runComposeUiTest {
        showNoticeEditor(repository())

        type(LibraryTags.FIELD_BODY, longBody)

        tagged(LibraryTags.SAVE).assertIsEnabled()
    }

    @Test
    fun aLongNoticeIsKeptAsWritten() = runComposeUiTest {
        val repo = repository()
        showNoticeEditor(repo)

        type(LibraryTags.FIELD_BODY, longBody)
        tagged(LibraryTags.SAVE).performClick()

        awaitThat { repo.announcements.isNotEmpty() }
        assertEquals(longBody, repo.announcements.single().body)
    }

    @Test
    fun shorteningALongNoticeTakesTheWarningAway() = runComposeUiTest {
        showNoticeEditor(repository())
        type(LibraryTags.FIELD_BODY, longBody)
        awaitThat { isShowing("may not fit") }

        type(LibraryTags.FIELD_BODY, "Coffee in the hall")

        awaitThat { !isShowing("may not fit") }
    }

    @Test
    fun anErrorReplacesTheWarningRatherThanStacking() = runComposeUiTest {
        showNoticeEditor(repository())
        type(LibraryTags.FIELD_BODY, longBody)
        awaitThat { isShowing("may not fit") }

        type(LibraryTags.FIELD_BODY, oversizedBody)

        awaitThat { !isShowing("may not fit") }
    }

    @Test
    fun aNewNoticeStartsWithNoComplaints() = runComposeUiTest {
        showNoticeEditor(repository())

        assertTrue(!isShowing("may not fit"))
        assertTrue(!isShowing("Too long"))
    }

    // ── Editing one that exists ──────────────────────────────────────────

    @Test
    fun anExistingNoticeOpensOnItsOwnBody() = runComposeUiTest {
        val repo = repository().apply { upsertAnnouncement(notice()) }

        showNoticeEditor(repo, noticeId = "n1")

        awaitThat { isShowing("Coffee in the hall") }
    }

    @Test
    fun anExistingNoticeOpensOnItsOwnTitle() = runComposeUiTest {
        val repo = repository().apply { upsertAnnouncement(notice()) }

        showNoticeEditor(repo, noticeId = "n1")

        awaitThat { isShowing("Welcome") }
    }

    @Test
    fun anExistingNoticeCanBeSavedStraightBack() = runComposeUiTest {
        val repo = repository().apply { upsertAnnouncement(notice()) }

        showNoticeEditor(repo, noticeId = "n1")

        tagged(LibraryTags.SAVE).assertIsEnabled()
    }

    @Test
    fun rewritingANoticeReplacesItRatherThanAddingAnother() = runComposeUiTest {
        val repo = repository().apply { upsertAnnouncement(notice()) }
        showNoticeEditor(repo, noticeId = "n1")

        type(LibraryTags.FIELD_BODY, "Coffee is cancelled")
        tagged(LibraryTags.SAVE).performClick()

        awaitThat { repo.announcements.single().body == "Coffee is cancelled" }
    }

    @Test
    fun rewritingANoticeKeepsItsTitle() = runComposeUiTest {
        val repo = repository().apply { upsertAnnouncement(notice()) }
        showNoticeEditor(repo, noticeId = "n1")

        type(LibraryTags.FIELD_BODY, "Coffee is cancelled")
        tagged(LibraryTags.SAVE).performClick()

        awaitThat { repo.announcements.single().body == "Coffee is cancelled" }
        assertEquals("Welcome", repo.announcements.single().title)
    }

    @Test
    fun emptyingAnExistingNoticesBodyStopsTheSave() = runComposeUiTest {
        val repo = repository().apply { upsertAnnouncement(notice()) }
        showNoticeEditor(repo, noticeId = "n1")

        type(LibraryTags.FIELD_BODY, "")

        tagged(LibraryTags.SAVE).assertIsNotEnabled()
    }

    @Test
    fun anExistingNoticeSurvivesAFailedEdit() = runComposeUiTest {
        // Emptying the field and giving up must not lose what was there.
        val repo = repository().apply { upsertAnnouncement(notice()) }
        showNoticeEditor(repo, noticeId = "n1")

        type(LibraryTags.FIELD_BODY, "")
        click(LibraryTags.SAVE)

        assertEquals("Coffee in the hall", repo.announcements.single().body)
    }

    @Test
    fun aTitleCanBeAddedToAnExistingNotice() = runComposeUiTest {
        val repo = repository().apply { upsertAnnouncement(notice(title = "")) }
        showNoticeEditor(repo, noticeId = "n1")

        type(LibraryTags.FIELD_TITLE, "Welcome")
        tagged(LibraryTags.SAVE).performClick()

        awaitThat { repo.announcements.single().title == "Welcome" }
    }

    @Test
    fun aTitleCanBeTakenOffAnExistingNotice() = runComposeUiTest {
        val repo = repository().apply { upsertAnnouncement(notice()) }
        showNoticeEditor(repo, noticeId = "n1")

        type(LibraryTags.FIELD_TITLE, "")
        tagged(LibraryTags.SAVE).performClick()

        awaitThat { repo.announcements.single().title == "" }
    }

    @Test
    fun anExistingLongNoticeOpensWithItsWarning() = runComposeUiTest {
        val repo = repository().apply { upsertAnnouncement(notice(body = longBody)) }

        showNoticeEditor(repo, noticeId = "n1")

        awaitThat { isShowing("may not fit") }
    }

    @Test
    fun aNoticeAddedHereJoinsTheOnesAlreadyThere() = runComposeUiTest {
        val repo = repository().apply { upsertAnnouncement(notice()) }
        showNoticeEditor(repo)

        type(LibraryTags.FIELD_BODY, "Second notice")
        tagged(LibraryTags.SAVE).performClick()

        awaitThat { repo.announcements.size == 2 }
    }
}
