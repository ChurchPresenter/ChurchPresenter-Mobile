package com.church.presenter.churchpresentermobile.screenshot

import com.church.presenter.churchpresentermobile.model.Question
import com.church.presenter.churchpresentermobile.model.QuestionStatus
import com.church.presenter.churchpresentermobile.ui.AddQuestionDialog
import com.church.presenter.churchpresentermobile.ui.QAAdminContent
import com.church.presenter.churchpresentermobile.ui.QuestionEditor
import com.church.presenter.churchpresentermobile.viewmodel.QAUiState
import kotlin.test.Test

/**
 * The Q&A board the host moderates from, and the two sheets it opens.
 *
 * A question carries a status, a vote count and a name, and the board treats
 * each differently — pending waits for approval, approved can go on the screen,
 * done is struck off. The one currently displayed is marked as well, because
 * putting a second question up is how the room loses its place.
 */
class QAScreenshotTest {

    private fun question(
        id: String,
        text: String,
        status: QuestionStatus = QuestionStatus.PENDING,
        name: String = "",
        upvotes: Int = 0,
        downvotes: Int = 0,
    ) = Question(
        id = id,
        text = text,
        submitterName = name,
        timestamp = 0L,
        status = status,
        upvotes = upvotes,
        downvotes = downvotes,
    )

    private val questions = listOf(
        question("1", "How do we know the promises still apply to us today?", name = "Ruth"),
        question("2", "What does 'grace' mean in verse 8?", QuestionStatus.APPROVED, "Sam", upvotes = 7),
        question(
            "3",
            "Could you say more about the second point?",
            QuestionStatus.APPROVED,
            upvotes = 2,
            downvotes = 1,
        ),
        question("4", "Where can we read the passage again?", QuestionStatus.DONE, "Ann"),
    )

    private fun board(
        questions: List<Question> = this.questions,
        sessionActive: Boolean = true,
        displayedQuestionId: String = "",
        votingEnabled: Boolean = false,
    ) = QAUiState.Admin(
        sessionActive = sessionActive,
        questions = questions,
        displayedQuestionId = displayedQuestionId,
        votingEnabled = votingEnabled,
    )

    @Test
    fun boardWithEveryStatus() = screenshot("qa-board__mixed-statuses") {
        QAAdminContent(
            state = board(),
            onApprove = {}, onDeny = {}, onEdit = { _, _ -> }, onMarkDone = {},
            onDisplay = {}, onApproveAndDisplay = {}, onDelete = {},
            onAddQuestion = { _, _ -> }, askForName = false, onClearDisplay = {},
            votingEnabled = false,
        )
    }

    @Test
    fun boardWithVoting() = screenshot("qa-board__voting-enabled") {
        // Votes are only drawn when the session has voting on — otherwise the
        // counts would be shown for a room that was never asked.
        QAAdminContent(
            state = board(votingEnabled = true),
            onApprove = {}, onDeny = {}, onEdit = { _, _ -> }, onMarkDone = {},
            onDisplay = {}, onApproveAndDisplay = {}, onDelete = {},
            onAddQuestion = { _, _ -> }, askForName = false, onClearDisplay = {},
            votingEnabled = true,
        )
    }

    @Test
    fun boardWithOneDisplayed() = screenshot("qa-board__question-displayed") {
        QAAdminContent(
            state = board(displayedQuestionId = "2"),
            onApprove = {}, onDeny = {}, onEdit = { _, _ -> }, onMarkDone = {},
            onDisplay = {}, onApproveAndDisplay = {}, onDelete = {},
            onAddQuestion = { _, _ -> }, askForName = false, onClearDisplay = {},
            votingEnabled = false,
        )
    }

    @Test
    fun boardEmpty() = screenshot("qa-board__no-questions") {
        QAAdminContent(
            state = board(questions = emptyList()),
            onApprove = {}, onDeny = {}, onEdit = { _, _ -> }, onMarkDone = {},
            onDisplay = {}, onApproveAndDisplay = {}, onDelete = {},
            onAddQuestion = { _, _ -> }, askForName = false, onClearDisplay = {},
            votingEnabled = false,
        )
    }

    @Test
    fun boardSessionClosed() = screenshot("qa-board__session-closed") {
        QAAdminContent(
            state = board(sessionActive = false, questions = emptyList()),
            onApprove = {}, onDeny = {}, onEdit = { _, _ -> }, onMarkDone = {},
            onDisplay = {}, onApproveAndDisplay = {}, onDelete = {},
            onAddQuestion = { _, _ -> }, askForName = false, onClearDisplay = {},
            votingEnabled = false,
        )
    }

    @Test
    fun questionEditor() = screenshot("qa-editor__existing-question") {
        QuestionEditor(question = questions[0], onSave = {}, onDelete = {}, onDismiss = {})
    }

    @Test
    fun addQuestion() = screenshot("qa-add__anonymous", dialog = true) {
        AddQuestionDialog(askForName = false, onConfirm = { _, _ -> }, onDismiss = {})
    }

    @Test
    fun addQuestionAskingForName() = screenshot("qa-add__with-name", dialog = true) {
        AddQuestionDialog(askForName = true, onConfirm = { _, _ -> }, onDismiss = {})
    }
}
