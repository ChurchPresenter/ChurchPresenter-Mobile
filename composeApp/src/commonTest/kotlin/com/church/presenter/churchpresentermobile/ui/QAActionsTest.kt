package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.QuestionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What each question on the Q&A board can have done to it.
 *
 * The actions on a card are decided entirely by its status and by whether it is
 * the one currently on the screen in the room. Getting that wrong is not a
 * cosmetic problem: an Approve button on a live question would re-approve
 * something already showing, and a missing Stop leaves the operator with no way
 * to take a question down.
 *
 * Every action asserts the id it carried. Two cards on screen and the callback
 * naming the first one's id is a real bug that "the button was there" misses.
 */
@OptIn(ExperimentalTestApi::class)
class QAActionsTest {

    private val pending = question("q1", text = "Why is the sky blue?")
    private val approved = question("q2", text = "How long is the sermon?", status = QuestionStatus.APPROVED)
    private val denied = question("q3", text = "Off topic", status = QuestionStatus.DENIED)
    private val done = question("q4", text = "Already answered", status = QuestionStatus.DONE)

    // ── A pending question ───────────────────────────────────────────────

    @Test
    fun aPendingQuestionCanBeApproved() = runComposeUiTest {
        showQaBoard(adminState(listOf(pending)))

        assertTrue(exists(UiTags.qaApprove("q1")))
    }

    @Test
    fun aPendingQuestionCanBeDenied() = runComposeUiTest {
        showQaBoard(adminState(listOf(pending)))

        assertTrue(exists(UiTags.qaDeny("q1")))
    }

    @Test
    fun aPendingQuestionCannotBeSentStraightToTheScreen() = runComposeUiTest {
        // It has to be approved first; a Go live here would skip the decision
        // the board exists to make.
        showQaBoard(adminState(listOf(pending)))

        assertFalse(exists(UiTags.qaGoLive("q1")))
    }

    @Test
    fun approvingReportsTheQuestion() = runComposeUiTest {
        val actions = QaActions()
        showQaBoard(adminState(listOf(pending)), actions)

        click(UiTags.qaApprove("q1"))

        assertEquals(listOf("q1"), actions.approved)
    }

    @Test
    fun denyingReportsTheQuestion() = runComposeUiTest {
        val actions = QaActions()
        showQaBoard(adminState(listOf(pending)), actions)

        click(UiTags.qaDeny("q1"))

        assertEquals(listOf("q1"), actions.denied)
    }

    @Test
    fun approvingDoesNotAlsoDeny() = runComposeUiTest {
        val actions = QaActions()
        showQaBoard(adminState(listOf(pending)), actions)

        click(UiTags.qaApprove("q1"))

        assertTrue(actions.denied.isEmpty())
    }

    @Test
    fun approvingDoesNotAlsoDisplay() = runComposeUiTest {
        val actions = QaActions()
        showQaBoard(adminState(listOf(pending)), actions)

        click(UiTags.qaApprove("q1"))

        assertTrue(actions.displayed.isEmpty())
    }

    @Test
    fun approvingTheSecondCardReportsTheSecondQuestion() = runComposeUiTest {
        val actions = QaActions()
        showQaBoard(adminState(listOf(pending, question("q9", text = "Second"))), actions)

        click(UiTags.qaApprove("q9"))

        assertEquals(listOf("q9"), actions.approved)
    }

    @Test
    fun denyingTheSecondCardReportsTheSecondQuestion() = runComposeUiTest {
        val actions = QaActions()
        showQaBoard(adminState(listOf(pending, question("q9", text = "Second"))), actions)

        click(UiTags.qaDeny("q9"))

        assertEquals(listOf("q9"), actions.denied)
    }

    // ── An approved question ─────────────────────────────────────────────

    @Test
    fun anApprovedQuestionCanGoLive() = runComposeUiTest {
        showQaBoard(adminState(listOf(approved)))

        assertTrue(exists(UiTags.qaGoLive("q2")))
    }

    @Test
    fun anApprovedQuestionCanStillBeDenied() = runComposeUiTest {
        // Second thoughts before it goes up are the whole point of the queue.
        showQaBoard(adminState(listOf(approved)))

        assertTrue(exists(UiTags.qaDeny("q2")))
    }

    @Test
    fun anApprovedQuestionIsNotOfferedApproveAgain() = runComposeUiTest {
        showQaBoard(adminState(listOf(approved)))

        assertFalse(exists(UiTags.qaApprove("q2")))
    }

    @Test
    fun sendingAnApprovedQuestionLiveReportsIt() = runComposeUiTest {
        val actions = QaActions()
        showQaBoard(adminState(listOf(approved)), actions)

        click(UiTags.qaGoLive("q2"))

        assertEquals(listOf("q2"), actions.displayed)
    }

    @Test
    fun sendingAnApprovedQuestionLiveDoesNotReApproveIt() = runComposeUiTest {
        val actions = QaActions()
        showQaBoard(adminState(listOf(approved)), actions)

        click(UiTags.qaGoLive("q2"))

        assertTrue(actions.approvedAndDisplayed.isEmpty())
    }

    @Test
    fun denyingAnApprovedQuestionReportsIt() = runComposeUiTest {
        val actions = QaActions()
        showQaBoard(adminState(listOf(approved)), actions)

        click(UiTags.qaDeny("q2"))

        assertEquals(listOf("q2"), actions.denied)
    }

    // ── The question currently on screen ─────────────────────────────────

    @Test
    fun aLiveQuestionCanBeStopped() = runComposeUiTest {
        showQaBoard(adminState(listOf(approved), displayedQuestionId = "q2"))

        assertTrue(exists(UiTags.qaStop("q2")))
    }

    @Test
    fun aLiveQuestionIsNotOfferedGoLiveAgain() = runComposeUiTest {
        showQaBoard(adminState(listOf(approved), displayedQuestionId = "q2"))

        assertFalse(exists(UiTags.qaGoLive("q2")))
    }

    @Test
    fun aLiveQuestionIsNotOfferedDeny() = runComposeUiTest {
        // Taking it down is Stop; a Deny here would leave it on screen while
        // marking it refused.
        showQaBoard(adminState(listOf(approved), displayedQuestionId = "q2"))

        assertFalse(exists(UiTags.qaDeny("q2")))
    }

    @Test
    fun stoppingClearsTheDisplay() = runComposeUiTest {
        val actions = QaActions()
        showQaBoard(adminState(listOf(approved), displayedQuestionId = "q2"), actions)

        click(UiTags.qaStop("q2"))

        assertEquals(1, actions.displayCleared)
    }

    @Test
    fun stoppingDoesNotDenyTheQuestion() = runComposeUiTest {
        val actions = QaActions()
        showQaBoard(adminState(listOf(approved), displayedQuestionId = "q2"), actions)

        click(UiTags.qaStop("q2"))

        assertTrue(actions.denied.isEmpty())
    }

    @Test
    fun stoppingDoesNotMarkTheQuestionAnswered() = runComposeUiTest {
        // Taking a question off the screen is not the same as answering it —
        // it must stay in the queue for the speaker to come back to.
        val actions = QaActions()
        showQaBoard(adminState(listOf(approved), displayedQuestionId = "q2"), actions)

        click(UiTags.qaStop("q2"))

        assertTrue(actions.markedDone.isEmpty())
    }

    @Test
    fun anotherCardKeepsItsOwnActionsWhileOneIsLive() = runComposeUiTest {
        showQaBoard(adminState(listOf(pending, approved), displayedQuestionId = "q2"))

        assertTrue(exists(UiTags.qaApprove("q1")))
        assertTrue(exists(UiTags.qaStop("q2")))
    }

    // ── A finished question ──────────────────────────────────────────────

    @Test
    fun aDeniedQuestionCanBeApprovedAfterAll() = runComposeUiTest {
        showQaBoard(adminState(listOf(denied)))
        click(UiTags.qaTab(1))

        assertTrue(exists(UiTags.qaApprove("q3")))
    }

    @Test
    fun aDeniedQuestionCanBeSentStraightLive() = runComposeUiTest {
        showQaBoard(adminState(listOf(denied)))
        click(UiTags.qaTab(1))

        assertTrue(exists(UiTags.qaGoLive("q3")))
    }

    @Test
    fun aFinishedQuestionIsNotOfferedDeny() = runComposeUiTest {
        // It is already out of the queue; denying it again says nothing.
        showQaBoard(adminState(listOf(done)))
        click(UiTags.qaTab(1))

        assertFalse(exists(UiTags.qaDeny("q4")))
    }

    @Test
    fun rescuingADeniedQuestionReportsIt() = runComposeUiTest {
        val actions = QaActions()
        showQaBoard(adminState(listOf(denied)), actions)
        click(UiTags.qaTab(1))

        click(UiTags.qaApprove("q3"))

        assertEquals(listOf("q3"), actions.approved)
    }

    @Test
    fun sendingAFinishedQuestionLiveApprovesItFirst() = runComposeUiTest {
        // It is not approved any more, so "go live" has to do both — otherwise
        // the desktop would refuse a question that is still marked denied.
        val actions = QaActions()
        showQaBoard(adminState(listOf(done)), actions)
        click(UiTags.qaTab(1))

        click(UiTags.qaGoLive("q4"))

        assertEquals(listOf("q4"), actions.approvedAndDisplayed)
    }

    @Test
    fun sendingAFinishedQuestionLiveDoesNotUseThePlainDisplayAction() = runComposeUiTest {
        val actions = QaActions()
        showQaBoard(adminState(listOf(done)), actions)
        click(UiTags.qaTab(1))

        click(UiTags.qaGoLive("q4"))

        assertTrue(actions.displayed.isEmpty())
    }

    @Test
    fun aFinishedQuestionThatIsLiveIsOfferedStop() = runComposeUiTest {
        showQaBoard(adminState(listOf(done), displayedQuestionId = "q4"))
        click(UiTags.qaTab(1))

        assertTrue(exists(UiTags.qaStop("q4")))
    }

    // ── Edit and delete, on every card ───────────────────────────────────

    @Test
    fun aPendingQuestionCanBeEdited() = runComposeUiTest {
        showQaBoard(adminState(listOf(pending)))

        assertTrue(exists(UiTags.qaEdit("q1")))
    }

    @Test
    fun aPendingQuestionCanBeDeleted() = runComposeUiTest {
        showQaBoard(adminState(listOf(pending)))

        assertTrue(exists(UiTags.qaDelete("q1")))
    }

    @Test
    fun aLiveQuestionCanStillBeEdited() = runComposeUiTest {
        // Fixing a typo while it is on screen is exactly when it is needed.
        showQaBoard(adminState(listOf(approved), displayedQuestionId = "q2"))

        assertTrue(exists(UiTags.qaEdit("q2")))
    }

    @Test
    fun aFinishedQuestionCanStillBeDeleted() = runComposeUiTest {
        showQaBoard(adminState(listOf(done)))
        click(UiTags.qaTab(1))

        assertTrue(exists(UiTags.qaDelete("q4")))
    }

    @Test
    fun deletingReportsTheQuestion() = runComposeUiTest {
        val actions = QaActions()
        showQaBoard(adminState(listOf(pending)), actions)

        click(UiTags.qaDelete("q1"))

        assertEquals(listOf("q1"), actions.deleted)
    }

    @Test
    fun deletingTheSecondCardReportsTheSecondQuestion() = runComposeUiTest {
        val actions = QaActions()
        showQaBoard(adminState(listOf(pending, question("q9", text = "Second"))), actions)

        click(UiTags.qaDelete("q9"))

        assertEquals(listOf("q9"), actions.deleted)
    }

    @Test
    fun deletingDoesNotAlsoDeny() = runComposeUiTest {
        val actions = QaActions()
        showQaBoard(adminState(listOf(pending)), actions)

        click(UiTags.qaDelete("q1"))

        assertTrue(actions.denied.isEmpty())
    }

    @Test
    fun editingOpensNothingByItself() = runComposeUiTest {
        // Tapping edit opens the editor; it must not send an edit of its own.
        val actions = QaActions()
        showQaBoard(adminState(listOf(pending)), actions)

        click(UiTags.qaEdit("q1"))

        assertTrue(actions.edited.isEmpty())
    }

    @Test
    fun editingDoesNotDeleteTheQuestion() = runComposeUiTest {
        val actions = QaActions()
        showQaBoard(adminState(listOf(pending)), actions)

        click(UiTags.qaEdit("q1"))

        assertTrue(actions.deleted.isEmpty())
    }
}
