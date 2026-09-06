package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Writing a notice on the phone.
 *
 * Save stays unpressable until there is something to project: a title alone is
 * only the label in the library list, and a notice with no words is a blank
 * slide.
 */
@OptIn(ExperimentalTestApi::class)
class NoticeEditorTest {

    @Test
    fun aNewNoticeOffersATitleAndABody() = runComposeUiTest {
        showNoticeEditor(libraryOf())

        assertTrue(exists(LibraryTags.FIELD_TITLE))
        assertTrue(exists(LibraryTags.FIELD_BODY))
    }

    @Test
    fun anEmptyNoticeCannotBeSaved() = runComposeUiTest {
        // Disabled rather than hidden, so the operator can see it is there and
        // work out what is missing.
        showNoticeEditor(libraryOf())

        tagged(LibraryTags.SAVE).assertIsNotEnabled()
    }

    @Test
    fun writingSomethingMakesSaveAvailable() = runComposeUiTest {
        showNoticeEditor(libraryOf())

        type(LibraryTags.FIELD_BODY, "Shared lunch after the service")

        tagged(LibraryTags.SAVE).assertIsEnabled()
    }

    @Test
    fun aTitleAloneIsNotEnoughToSave() = runComposeUiTest {
        val repository = libraryOf()
        showNoticeEditor(repository)

        type(LibraryTags.FIELD_TITLE, "Bring a dish")

        tagged(LibraryTags.SAVE).assertIsNotEnabled()
        assertTrue(repository.announcements.isEmpty())
    }

    @Test
    fun aNoticeWithABodyIsSaved() = runComposeUiTest {
        val repository = libraryOf()
        showNoticeEditor(repository)

        type(LibraryTags.FIELD_TITLE, "Bring a dish")
        type(LibraryTags.FIELD_BODY, "Shared lunch after the service")
        tagged(LibraryTags.SAVE).performClick()

        val saved = repository.announcements.single()
        assertEquals("Bring a dish", saved.title)
        assertEquals("Shared lunch after the service", saved.body)
    }

    @Test
    fun savingClosesTheEditor() = runComposeUiTest {
        var closed = false
        showNoticeEditor(libraryOf(), onClose = { closed = true })

        type(LibraryTags.FIELD_BODY, "Shared lunch")
        tagged(LibraryTags.SAVE).performClick()

        assertTrue(closed)
    }

    @Test
    fun anExistingNoticeOpensOnItsOwnWords() = runComposeUiTest {
        val notice = LocalAnnouncement(id = "a1", title = "Bring a dish", body = "Shared lunch")
        showNoticeEditor(libraryOf(notices = listOf(notice)), noticeId = "a1")

        assertTrue(isShowing("Shared lunch"))
        assertTrue(isShowing("Bring a dish"))
    }

    @Test
    fun editingAnExistingNoticeReplacesItRatherThanAddingAnother() = runComposeUiTest {
        // Saving under a new id would leave two notices and the operator
        // projecting the stale one.
        val repository = libraryOf(notices = listOf(LocalAnnouncement(id = "a1", title = "Old", body = "Old words")))
        showNoticeEditor(repository, noticeId = "a1")

        type(LibraryTags.FIELD_BODY, "New words")
        tagged(LibraryTags.SAVE).performClick()

        assertEquals(1, repository.announcements.size)
        assertEquals("New words", repository.announcements.single().body)
    }
}
