package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Adding a question from the phone.
 *
 * This is how a question asked out loud gets into the queue, so the operator is
 * typing on someone else's behalf. The name field only appears while nobody has
 * said who they are — once given it is remembered in Settings — and the Save
 * button stays disabled until there is actually a question to add.
 */
@OptIn(ExperimentalTestApi::class)
class QAAddQuestionTest {

    // ── What the dialog offers ───────────────────────────────────────────

    @Test
    fun theDialogAsksForTheQuestion() = runComposeUiTest {
        showAddQuestion()

        assertTrue(exists(UiTags.QA_ADD_TEXT))
    }

    @Test
    fun theDialogOffersSave() = runComposeUiTest {
        showAddQuestion()

        assertTrue(exists(UiTags.QA_ADD_CONFIRM))
    }

    @Test
    fun theDialogOffersCancel() = runComposeUiTest {
        showAddQuestion()

        assertTrue(exists(UiTags.QA_ADD_CANCEL))
    }

    @Test
    fun aNameIsAskedForWhenNobodyHasSaidWhoTheyAre() = runComposeUiTest {
        showAddQuestion(askForName = true)

        assertTrue(exists(UiTags.QA_ADD_NAME))
    }

    @Test
    fun noNameIsAskedForOnceItIsKnown() = runComposeUiTest {
        // It is remembered in Settings; asking again every time would be a
        // field the operator has to skip on every question.
        showAddQuestion(askForName = false)

        assertFalse(exists(UiTags.QA_ADD_NAME))
    }

    // ── Whether Save can be pressed ──────────────────────────────────────

    @Test
    fun anEmptyQuestionCannotBeSaved() = runComposeUiTest {
        showAddQuestion()

        tagged(UiTags.QA_ADD_CONFIRM).assertIsNotEnabled()
    }

    @Test
    fun aTypedQuestionCanBeSaved() = runComposeUiTest {
        showAddQuestion()

        type(UiTags.QA_ADD_TEXT, "Why is the sky blue?")

        tagged(UiTags.QA_ADD_CONFIRM).assertIsEnabled()
    }

    @Test
    fun aQuestionOfOnlySpacesCannotBeSaved() = runComposeUiTest {
        // isNotBlank, not isNotEmpty: a field of spaces is not a question.
        showAddQuestion()

        type(UiTags.QA_ADD_TEXT, "     ")

        tagged(UiTags.QA_ADD_CONFIRM).assertIsNotEnabled()
    }

    @Test
    fun clearingTheQuestionDisablesSaveAgain() = runComposeUiTest {
        showAddQuestion()
        type(UiTags.QA_ADD_TEXT, "Why is the sky blue?")

        type(UiTags.QA_ADD_TEXT, "")

        tagged(UiTags.QA_ADD_CONFIRM).assertIsNotEnabled()
    }

    @Test
    fun aNameAloneDoesNotMakeTheQuestionSavable() = runComposeUiTest {
        showAddQuestion(askForName = true)

        type(UiTags.QA_ADD_NAME, "Sam")

        tagged(UiTags.QA_ADD_CONFIRM).assertIsNotEnabled()
    }

    // ── What gets added ──────────────────────────────────────────────────

    @Test
    fun savingCarriesTheQuestion() = runComposeUiTest {
        var added: Pair<String, String>? = null
        showAddQuestion(onConfirm = { text, name -> added = text to name })

        type(UiTags.QA_ADD_TEXT, "Why is the sky blue?")
        click(UiTags.QA_ADD_CONFIRM)

        assertEquals("Why is the sky blue?", added?.first)
    }

    @Test
    fun savingCarriesTheName() = runComposeUiTest {
        var added: Pair<String, String>? = null
        showAddQuestion(askForName = true, onConfirm = { text, name -> added = text to name })

        type(UiTags.QA_ADD_TEXT, "Why is the sky blue?")
        type(UiTags.QA_ADD_NAME, "Sam")
        click(UiTags.QA_ADD_CONFIRM)

        assertEquals("Sam", added?.second)
    }

    @Test
    fun aQuestionAddedWithoutANameCarriesNone() = runComposeUiTest {
        var added: Pair<String, String>? = null
        showAddQuestion(askForName = false, onConfirm = { text, name -> added = text to name })

        type(UiTags.QA_ADD_TEXT, "Why is the sky blue?")
        click(UiTags.QA_ADD_CONFIRM)

        assertEquals("", added?.second)
    }

    @Test
    fun savingTrimsTheQuestion() = runComposeUiTest {
        var added: Pair<String, String>? = null
        showAddQuestion(onConfirm = { text, name -> added = text to name })

        type(UiTags.QA_ADD_TEXT, "   Why is the sky blue?   ")
        click(UiTags.QA_ADD_CONFIRM)

        assertEquals("Why is the sky blue?", added?.first)
    }

    @Test
    fun savingTrimsTheName() = runComposeUiTest {
        var added: Pair<String, String>? = null
        showAddQuestion(askForName = true, onConfirm = { text, name -> added = text to name })

        type(UiTags.QA_ADD_TEXT, "Why is the sky blue?")
        type(UiTags.QA_ADD_NAME, "  Sam  ")
        click(UiTags.QA_ADD_CONFIRM)

        assertEquals("Sam", added?.second)
    }

    @Test
    fun theTypedQuestionIsShownBack() = runComposeUiTest {
        showAddQuestion()

        type(UiTags.QA_ADD_TEXT, "How long is the sermon?")

        assertTrue(isShowing("How long is the sermon?"))
    }

    @Test
    fun theTypedNameIsShownBack() = runComposeUiTest {
        showAddQuestion(askForName = true)

        type(UiTags.QA_ADD_NAME, "Sam")

        assertTrue(isShowing("Sam"))
    }

    // ── Backing out ──────────────────────────────────────────────────────

    @Test
    fun cancellingReportsTheDismissal() = runComposeUiTest {
        var dismissed = 0
        showAddQuestion(onDismiss = { dismissed++ })

        click(UiTags.QA_ADD_CANCEL)

        assertEquals(1, dismissed)
    }

    @Test
    fun cancellingAddsNothing() = runComposeUiTest {
        var added: Pair<String, String>? = null
        showAddQuestion(onConfirm = { text, name -> added = text to name })

        type(UiTags.QA_ADD_TEXT, "Why is the sky blue?")
        click(UiTags.QA_ADD_CANCEL)

        assertNull(added)
    }

    @Test
    fun savingDoesNotAlsoCancel() = runComposeUiTest {
        var dismissed = 0
        showAddQuestion(onDismiss = { dismissed++ })

        type(UiTags.QA_ADD_TEXT, "Why is the sky blue?")
        click(UiTags.QA_ADD_CONFIRM)

        assertEquals(0, dismissed)
    }

    // ── Opening it from the board ────────────────────────────────────────

    @Test
    fun theBoardOffersAWayToAddAQuestion() = runComposeUiTest {
        showQaBoard(adminState(listOf(question("q1"))))

        assertTrue(exists(UiTags.QA_ADD))
    }

    @Test
    fun noDialogIsOpenBeforeTheAddButtonIsPressed() = runComposeUiTest {
        showQaBoard(adminState(listOf(question("q1"))))

        assertFalse(exists(UiTags.QA_ADD_TEXT))
    }

    @Test
    fun pressingAddOpensTheDialog() = runComposeUiTest {
        showQaBoard(adminState(listOf(question("q1"))))

        click(UiTags.QA_ADD)

        assertTrue(exists(UiTags.QA_ADD_TEXT))
    }

    @Test
    fun addingAQuestionFromTheBoardReportsIt() = runComposeUiTest {
        val actions = QaActions()
        showQaBoard(adminState(listOf(question("q1"))), actions)

        click(UiTags.QA_ADD)
        type(UiTags.QA_ADD_TEXT, "Asked from the floor")
        click(UiTags.QA_ADD_CONFIRM)

        assertEquals(listOf("Asked from the floor" to ""), actions.added)
    }

    @Test
    fun theDialogClosesOnceTheQuestionIsAdded() = runComposeUiTest {
        showQaBoard(adminState(listOf(question("q1"))))
        click(UiTags.QA_ADD)

        type(UiTags.QA_ADD_TEXT, "Asked from the floor")
        click(UiTags.QA_ADD_CONFIRM)

        assertFalse(exists(UiTags.QA_ADD_TEXT))
    }

    @Test
    fun cancellingClosesTheDialog() = runComposeUiTest {
        showQaBoard(adminState(listOf(question("q1"))))
        click(UiTags.QA_ADD)

        click(UiTags.QA_ADD_CANCEL)

        assertFalse(exists(UiTags.QA_ADD_TEXT))
    }

    @Test
    fun theBoardAsksForANameWhenNobodyHasGivenOne() = runComposeUiTest {
        showQaBoard(adminState(listOf(question("q1"))), askForName = true)

        click(UiTags.QA_ADD)

        assertTrue(exists(UiTags.QA_ADD_NAME))
    }

    @Test
    fun theBoardDoesNotAskForANameOnceItIsKnown() = runComposeUiTest {
        showQaBoard(adminState(listOf(question("q1"))), askForName = false)

        click(UiTags.QA_ADD)

        assertFalse(exists(UiTags.QA_ADD_NAME))
    }
}
