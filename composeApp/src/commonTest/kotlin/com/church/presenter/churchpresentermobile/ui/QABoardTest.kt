package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.QuestionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The Q&A admin board — the two tabs and what belongs in each.
 *
 * A question's status decides three separate things: which tab it appears in,
 * which badge it carries, and which actions it earns. Those are the failures
 * worth catching, because each one misleads the operator in a different way — a
 * denied question sitting in Incoming looks like it still needs a decision, and
 * a question missing its Approve button cannot be rescued at all.
 */
@OptIn(ExperimentalTestApi::class)
class QABoardTest {

    private val pending = question("q1", text = "Why is the sky blue?")
    private val approved = question("q2", text = "How long is the sermon?", status = QuestionStatus.APPROVED)
    private val denied = question("q3", text = "Off topic", status = QuestionStatus.DENIED)
    private val done = question("q4", text = "Already answered", status = QuestionStatus.DONE)

    private val everything = listOf(pending, approved, denied, done)

    // ── Which tab a question belongs in ──────────────────────────────────

    @Test
    fun bothTabsAreOffered() = runComposeUiTest {
        showQaBoard(adminState(everything))

        assertTrue(exists(UiTags.qaTab(0)))
        assertTrue(exists(UiTags.qaTab(1)))
    }

    @Test
    fun incomingIsTheTabTheBoardOpensOn() = runComposeUiTest {
        showQaBoard(adminState(everything))

        tagged(UiTags.qaTab(0)).assertIsSelected()
    }

    @Test
    fun theAnsweredTabIsNotSelectedToBeginWith() = runComposeUiTest {
        showQaBoard(adminState(everything))

        tagged(UiTags.qaTab(1)).assertIsNotSelected()
    }

    @Test
    fun aPendingQuestionWaitsInIncoming() = runComposeUiTest {
        showQaBoard(adminState(everything))

        assertTrue(exists(UiTags.qaCard("q1")))
    }

    @Test
    fun anApprovedQuestionWaitsInIncoming() = runComposeUiTest {
        // Approved but not yet shown: still the operator's to act on.
        showQaBoard(adminState(everything))

        assertTrue(exists(UiTags.qaCard("q2")))
    }

    @Test
    fun aDeniedQuestionIsNotInIncoming() = runComposeUiTest {
        showQaBoard(adminState(everything))

        assertFalse(exists(UiTags.qaCard("q3")))
    }

    @Test
    fun anAnsweredQuestionIsNotInIncoming() = runComposeUiTest {
        showQaBoard(adminState(everything))

        assertFalse(exists(UiTags.qaCard("q4")))
    }

    @Test
    fun theAnsweredTabHoldsTheDeniedOnes() = runComposeUiTest {
        showQaBoard(adminState(everything))

        click(UiTags.qaTab(1))

        assertTrue(exists(UiTags.qaCard("q3")))
    }

    @Test
    fun theAnsweredTabHoldsTheDoneOnes() = runComposeUiTest {
        showQaBoard(adminState(everything))

        click(UiTags.qaTab(1))

        assertTrue(exists(UiTags.qaCard("q4")))
    }

    @Test
    fun theAnsweredTabDoesNotHoldPendingOnes() = runComposeUiTest {
        showQaBoard(adminState(everything))

        click(UiTags.qaTab(1))

        assertFalse(exists(UiTags.qaCard("q1")))
    }

    @Test
    fun switchingTabsMarksTheNewOneSelected() = runComposeUiTest {
        showQaBoard(adminState(everything))

        click(UiTags.qaTab(1))

        tagged(UiTags.qaTab(1)).assertIsSelected()
        tagged(UiTags.qaTab(0)).assertIsNotSelected()
    }

    @Test
    fun goingBackToIncomingBringsItsQuestionsBack() = runComposeUiTest {
        showQaBoard(adminState(everything))
        click(UiTags.qaTab(1))

        click(UiTags.qaTab(0))

        assertTrue(exists(UiTags.qaCard("q1")))
    }

    // ── The empty tabs ───────────────────────────────────────────────────

    @Test
    fun anEmptyIncomingTabSaysSo() = runComposeUiTest {
        showQaBoard(adminState(listOf(done)))

        assertTrue(exists(UiTags.QA_EMPTY_INCOMING))
    }

    @Test
    fun anEmptyAnsweredTabSaysSo() = runComposeUiTest {
        showQaBoard(adminState(listOf(pending)))

        click(UiTags.qaTab(1))

        assertTrue(exists(UiTags.QA_EMPTY_FINISHED))
    }

    @Test
    fun theTwoEmptyMessagesAreNotTheSame() = runComposeUiTest {
        // "Nothing waiting" and "nothing answered yet" mean different things;
        // the wrong one would tell the operator the queue was empty when it is
        // the history that is.
        showQaBoard(adminState(listOf(done)))

        assertTrue(exists(UiTags.QA_EMPTY_INCOMING))
        assertFalse(exists(UiTags.QA_EMPTY_FINISHED))
    }

    @Test
    fun aBoardWithNoQuestionsAtAllShowsTheIncomingHint() = runComposeUiTest {
        showQaBoard(adminState(emptyList()))

        assertTrue(exists(UiTags.QA_EMPTY_INCOMING))
    }

    @Test
    fun aPopulatedTabShowsNoEmptyHint() = runComposeUiTest {
        showQaBoard(adminState(listOf(pending)))

        assertFalse(exists(UiTags.QA_EMPTY_INCOMING))
    }

    @Test
    fun theAddButtonIsOfferedEvenWithAnEmptyBoard() = runComposeUiTest {
        // The operator can put a question in themselves — often the only way
        // one asked out loud gets on screen.
        showQaBoard(adminState(emptyList()))

        assertTrue(exists(UiTags.QA_ADD))
    }

    // ── What a card says ─────────────────────────────────────────────────

    @Test
    fun aCardShowsTheQuestion() = runComposeUiTest {
        showQaBoard(adminState(listOf(pending)))

        assertTrue(isShowing("Why is the sky blue?"))
    }

    @Test
    fun aCardNamesWhoAsked() = runComposeUiTest {
        showQaBoard(adminState(listOf(question("q1", submitterName = "Sam"))))

        assertTrue(isShowing("Sam"))
    }

    @Test
    fun anAnonymousQuestionShowsNoName() = runComposeUiTest {
        showQaBoard(adminState(listOf(question("q1", text = "Anonymous ask"))))

        assertTrue(exists(UiTags.qaCard("q1")))
        assertFalse(isShowing("Sam"))
    }

    @Test
    fun everyQuestionInTheTabGetsACard() = runComposeUiTest {
        showQaBoard(adminState(listOf(pending, approved)))

        assertTrue(exists(UiTags.qaCard("q1")))
        assertTrue(exists(UiTags.qaCard("q2")))
    }

    // ── Badges ───────────────────────────────────────────────────────────

    @Test
    fun theQuestionOnScreenIsBadgedLive() = runComposeUiTest {
        showQaBoard(adminState(listOf(approved), displayedQuestionId = "q2"))

        assertTrue(exists(UiTags.qaBadge("q2", QaBadge.LIVE)))
    }

    @Test
    fun anApprovedQuestionIsBadgedApproved() = runComposeUiTest {
        showQaBoard(adminState(listOf(approved)))

        assertTrue(exists(UiTags.qaBadge("q2", QaBadge.APPROVED)))
    }

    @Test
    fun aLiveQuestionIsNotAlsoBadgedApproved() = runComposeUiTest {
        showQaBoard(adminState(listOf(approved), displayedQuestionId = "q2"))

        assertFalse(exists(UiTags.qaBadge("q2", QaBadge.APPROVED)))
    }

    @Test
    fun aDeniedQuestionIsBadgedDenied() = runComposeUiTest {
        showQaBoard(adminState(listOf(denied)))
        click(UiTags.qaTab(1))

        assertTrue(exists(UiTags.qaBadge("q3", QaBadge.DENIED)))
    }

    @Test
    fun anAnsweredQuestionIsBadgedAnswered() = runComposeUiTest {
        showQaBoard(adminState(listOf(done)))
        click(UiTags.qaTab(1))

        assertTrue(exists(UiTags.qaBadge("q4", QaBadge.ANSWERED)))
    }

    @Test
    fun aPendingQuestionCarriesNoBadge() = runComposeUiTest {
        // Pending is the default; a badge for it would be noise on every card.
        showQaBoard(adminState(listOf(pending)))

        assertFalse(exists(UiTags.qaBadge("q1", QaBadge.APPROVED)))
        assertFalse(exists(UiTags.qaBadge("q1", QaBadge.LIVE)))
        assertFalse(exists(UiTags.qaBadge("q1", QaBadge.DENIED)))
        assertFalse(exists(UiTags.qaBadge("q1", QaBadge.ANSWERED)))
    }

    @Test
    fun onlyTheQuestionOnScreenIsBadgedLive() = runComposeUiTest {
        showQaBoard(adminState(listOf(pending, approved), displayedQuestionId = "q2"))

        assertTrue(exists(UiTags.qaBadge("q2", QaBadge.LIVE)))
        assertFalse(exists(UiTags.qaBadge("q1", QaBadge.LIVE)))
    }

    @Test
    fun nothingIsBadgedLiveWhenTheDisplayIsEmpty() = runComposeUiTest {
        showQaBoard(adminState(listOf(pending, approved), displayedQuestionId = ""))

        assertFalse(exists(UiTags.qaBadge("q1", QaBadge.LIVE)))
        assertFalse(exists(UiTags.qaBadge("q2", QaBadge.LIVE)))
    }

    // ── Votes ────────────────────────────────────────────────────────────

    @Test
    fun votesAreShownWhenVotingIsOn() = runComposeUiTest {
        showQaBoard(adminState(listOf(question("q1", upvotes = 12)), votingEnabled = true))

        assertTrue(exists(UiTags.qaVotes("q1")))
    }

    @Test
    fun votesAreHiddenWhenVotingIsOff() = runComposeUiTest {
        // A vote count on a desktop with voting switched off is a number that
        // can only ever be zero.
        showQaBoard(adminState(listOf(question("q1", upvotes = 12)), votingEnabled = false))

        assertFalse(exists(UiTags.qaVotes("q1")))
    }

    @Test
    fun aQuestionWithNoVotesStillShowsItsCounterWhenVotingIsOn() = runComposeUiTest {
        showQaBoard(adminState(listOf(question("q1", upvotes = 0)), votingEnabled = true))

        assertTrue(exists(UiTags.qaVotes("q1")))
    }

    @Test
    fun everyCardGetsItsOwnVoteCount() = runComposeUiTest {
        showQaBoard(
            adminState(
                listOf(question("q1", upvotes = 3), question("q2", upvotes = 9)),
                votingEnabled = true,
            )
        )

        assertTrue(exists(UiTags.qaVotes("q1")))
        assertTrue(exists(UiTags.qaVotes("q2")))
    }

    // ── A long queue ─────────────────────────────────────────────────────

    @Test
    fun aQuestionFarDownTheQueueIsStillReachable() = runComposeUiTest {
        // Thirty questions is an ordinary evening; the last one must be
        // scrollable to, not merely present in the model.
        val many = (1..30).map { question("q$it", text = "Question $it") }
        showQaBoard(adminState(many))

        assertTrue(exists(UiTags.qaCard("q1")))
        // Not `exists`: the thirtieth card has not been composed yet, and the
        // question is whether the list will scroll to it — which is what
        // `tagged` does before naming it.
        tagged(UiTags.qaCard("q30")).assertIsDisplayed()
    }

    @Test
    fun aQuestionFarDownTheQueueKeepsItsOwnActions() = runComposeUiTest {
        val many = (1..30).map { question("q$it", text = "Question $it") }
        val actions = QaActions()
        showQaBoard(adminState(many), actions)

        click(UiTags.qaApprove("q30"))

        assertEquals(listOf("q30"), actions.approved)
    }

    @Test
    fun theAddButtonStaysOnTheAnsweredTab() = runComposeUiTest {
        showQaBoard(adminState(everything))

        click(UiTags.qaTab(1))

        assertTrue(exists(UiTags.QA_ADD))
    }

    @Test
    fun aPendingQuestionThatIsLiveStaysInIncoming() = runComposeUiTest {
        // The desktop can be showing a question the phone still has as pending;
        // moving it out of the queue would hide the only Stop button for it.
        showQaBoard(adminState(listOf(pending), displayedQuestionId = "q1"))

        assertTrue(exists(UiTags.qaCard("q1")))
    }

    @Test
    fun aPendingQuestionThatIsLiveIsBadgedLive() = runComposeUiTest {
        showQaBoard(adminState(listOf(pending), displayedQuestionId = "q1"))

        assertTrue(exists(UiTags.qaBadge("q1", QaBadge.LIVE)))
    }

    @Test
    fun aDisplayedIdThatMatchesNothingBadgesNothing() = runComposeUiTest {
        // The desktop may be showing something that is not a question at all.
        showQaBoard(adminState(listOf(pending, approved), displayedQuestionId = "gone"))

        assertFalse(exists(UiTags.qaBadge("q1", QaBadge.LIVE)))
        assertFalse(exists(UiTags.qaBadge("q2", QaBadge.LIVE)))
    }
}
