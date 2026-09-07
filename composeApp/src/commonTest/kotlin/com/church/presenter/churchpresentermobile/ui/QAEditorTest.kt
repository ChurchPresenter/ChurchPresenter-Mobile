package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.QuestionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The question editor — where a question is rewritten before it goes on screen.
 *
 * Rewriting is the last thing that happens to a question before a room full of
 * people reads it, so the edits worth pinning are the destructive ones: an empty
 * save that would blank the question, a save that carries the *old* text, and a
 * 200-character limit that has to hold because the desktop's slide cannot show
 * more.
 *
 * Composed directly, without its bottom sheet — see [QuestionEditor].
 */
@OptIn(ExperimentalTestApi::class)
class QAEditorTest {

    private val subject = question(
        "q1",
        text = "Why is the sky blue?",
        submitterName = "Sam",
        status = QuestionStatus.APPROVED,
    )

    // ── What the editor opens with ───────────────────────────────────────

    @Test
    fun theEditorOpensOnTheQuestionsOwnText() = runComposeUiTest {
        showQuestionEditor(subject)

        assertTrue(isShowing("Why is the sky blue?"))
    }

    @Test
    fun theEditorOffersATextField() = runComposeUiTest {
        showQuestionEditor(subject)

        assertTrue(exists(UiTags.QA_EDIT_TEXT))
    }

    @Test
    fun theEditorOffersSave() = runComposeUiTest {
        showQuestionEditor(subject)

        assertTrue(exists(UiTags.QA_EDIT_SAVE))
    }

    @Test
    fun theEditorOffersCancel() = runComposeUiTest {
        showQuestionEditor(subject)

        assertTrue(exists(UiTags.QA_EDIT_CANCEL))
    }

    @Test
    fun theEditorOffersDelete() = runComposeUiTest {
        showQuestionEditor(subject)

        assertTrue(exists(UiTags.QA_EDIT_DELETE))
    }

    @Test
    fun theEditorNamesWhoAsked() = runComposeUiTest {
        showQuestionEditor(subject)

        assertTrue(isShowing("Sam"))
    }

    @Test
    fun anAnonymousQuestionNamesNobody() = runComposeUiTest {
        showQuestionEditor(question("q1", text = "Anonymous ask"))

        assertFalse(isShowing("Sam"))
    }

    @Test
    fun theEditorCountsTheCharacters() = runComposeUiTest {
        showQuestionEditor(subject)

        assertTrue(exists(UiTags.QA_EDIT_COUNTER))
    }

    // ── Editing ──────────────────────────────────────────────────────────

    @Test
    fun typingReplacesTheQuestion() = runComposeUiTest {
        showQuestionEditor(subject)

        type(UiTags.QA_EDIT_TEXT, "Why is the sea blue?")

        assertTrue(isShowing("Why is the sea blue?"))
    }

    @Test
    fun theOldWordsGoWhenTheyAreReplaced() = runComposeUiTest {
        showQuestionEditor(subject)

        type(UiTags.QA_EDIT_TEXT, "Why is the sea blue?")

        assertFalse(isShowing("Why is the sky blue?"))
    }

    @Test
    fun savingCarriesTheEditedText() = runComposeUiTest {
        var saved: String? = null
        showQuestionEditor(subject, onSave = { saved = it })

        type(UiTags.QA_EDIT_TEXT, "Why is the sea blue?")
        click(UiTags.QA_EDIT_SAVE)

        assertEquals("Why is the sea blue?", saved)
    }

    @Test
    fun savingUneditedTextKeepsTheQuestionAsItWas() = runComposeUiTest {
        var saved: String? = null
        showQuestionEditor(subject, onSave = { saved = it })

        click(UiTags.QA_EDIT_SAVE)

        assertEquals("Why is the sky blue?", saved)
    }

    @Test
    fun savingTrimsStrayWhitespace() = runComposeUiTest {
        var saved: String? = null
        showQuestionEditor(subject, onSave = { saved = it })

        type(UiTags.QA_EDIT_TEXT, "  Why is the sea blue?  ")
        click(UiTags.QA_EDIT_SAVE)

        assertEquals("Why is the sea blue?", saved)
    }

    @Test
    fun anEmptyQuestionCannotBeSaved() = runComposeUiTest {
        // Saving nothing would put a blank question in front of the room.
        var saved: String? = null
        showQuestionEditor(subject, onSave = { saved = it })

        type(UiTags.QA_EDIT_TEXT, "")
        click(UiTags.QA_EDIT_SAVE)

        assertNull(saved)
    }

    @Test
    fun aQuestionOfOnlySpacesCannotBeSaved() = runComposeUiTest {
        var saved: String? = null
        showQuestionEditor(subject, onSave = { saved = it })

        type(UiTags.QA_EDIT_TEXT, "    ")
        click(UiTags.QA_EDIT_SAVE)

        assertNull(saved)
    }

    @Test
    fun anEmptyEditorShowsItsHint() = runComposeUiTest {
        showQuestionEditor(subject)

        type(UiTags.QA_EDIT_TEXT, "")

        assertFalse(isShowing("Why is the sky blue?"))
    }

    @Test
    fun theEditorStopsAtTwoHundredCharacters() = runComposeUiTest {
        // The desktop's slide cannot show more, so the field refuses the rest
        // rather than letting the operator write words nobody will see.
        var saved: String? = null
        showQuestionEditor(subject, onSave = { saved = it })

        type(UiTags.QA_EDIT_TEXT, "x".repeat(250))
        click(UiTags.QA_EDIT_SAVE)

        assertEquals("Why is the sky blue?", saved)
    }

    @Test
    fun exactlyTwoHundredCharactersIsAccepted() = runComposeUiTest {
        var saved: String? = null
        showQuestionEditor(subject, onSave = { saved = it })

        type(UiTags.QA_EDIT_TEXT, "y".repeat(200))
        click(UiTags.QA_EDIT_SAVE)

        assertEquals("y".repeat(200), saved)
    }

    @Test
    fun oneCharacterOverTheLimitIsRefused() = runComposeUiTest {
        var saved: String? = null
        showQuestionEditor(subject, onSave = { saved = it })

        type(UiTags.QA_EDIT_TEXT, "z".repeat(201))
        click(UiTags.QA_EDIT_SAVE)

        assertEquals("Why is the sky blue?", saved)
    }

    // ── Leaving without saving ───────────────────────────────────────────

    @Test
    fun cancellingReportsTheDismissal() = runComposeUiTest {
        var dismissed = 0
        showQuestionEditor(subject, onDismiss = { dismissed++ })

        click(UiTags.QA_EDIT_CANCEL)

        assertEquals(1, dismissed)
    }

    @Test
    fun cancellingSavesNothing() = runComposeUiTest {
        var saved: String? = null
        showQuestionEditor(subject, onSave = { saved = it })

        type(UiTags.QA_EDIT_TEXT, "Changed my mind")
        click(UiTags.QA_EDIT_CANCEL)

        assertNull(saved)
    }

    @Test
    fun cancellingDeletesNothing() = runComposeUiTest {
        var deleted = 0
        showQuestionEditor(subject, onDelete = { deleted++ })

        click(UiTags.QA_EDIT_CANCEL)

        assertEquals(0, deleted)
    }

    @Test
    fun savingDoesNotAlsoDelete() = runComposeUiTest {
        var deleted = 0
        showQuestionEditor(subject, onDelete = { deleted++ })

        click(UiTags.QA_EDIT_SAVE)

        assertEquals(0, deleted)
    }

    // ── Deleting ─────────────────────────────────────────────────────────

    @Test
    fun deletingReportsTheDeletion() = runComposeUiTest {
        var deleted = 0
        showQuestionEditor(subject, onDelete = { deleted++ })

        click(UiTags.QA_EDIT_DELETE)

        assertEquals(1, deleted)
    }

    @Test
    fun deletingDoesNotSaveTheEditFirst() = runComposeUiTest {
        var saved: String? = null
        showQuestionEditor(subject, onSave = { saved = it }, onDelete = {})

        type(UiTags.QA_EDIT_TEXT, "Never mind")
        click(UiTags.QA_EDIT_DELETE)

        assertNull(saved)
    }
}
