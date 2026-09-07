package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import com.church.presenter.churchpresentermobile.model.Question
import com.church.presenter.churchpresentermobile.model.QuestionStatus
import com.church.presenter.churchpresentermobile.viewmodel.QAUiState

/**
 * Setup for the Q&A admin board.
 *
 * The board is composed directly rather than through [QAAdminScreen]: every
 * decision it makes — which tab a question belongs in, which actions its status
 * earns it, what the badge says — is taken from the state it is handed and
 * reported through plain callbacks. Driving that through a ViewModel and a
 * mocked desktop would test the same decisions more slowly and less precisely.
 *
 * [QAAdminScreenTest] covers the part that does need a ViewModel: the loading,
 * error and retry states around it.
 */
internal fun question(
    id: String,
    text: String = "Why is the sky blue?",
    status: QuestionStatus = QuestionStatus.PENDING,
    submitterName: String = "",
    upvotes: Int = 0,
    timestamp: Long = 1_000L,
) = Question(
    id = id,
    text = text,
    submitterName = submitterName,
    timestamp = timestamp,
    status = status,
    upvotes = upvotes,
)

internal fun adminState(
    questions: List<Question>,
    displayedQuestionId: String = "",
    votingEnabled: Boolean = false,
    sessionActive: Boolean = true,
) = QAUiState.Admin(
    sessionActive = sessionActive,
    questions = questions,
    displayedQuestionId = displayedQuestionId,
    votingEnabled = votingEnabled,
)

/** Records what the board asked the ViewModel to do. */
internal class QaActions {
    val approved = mutableListOf<String>()
    val denied = mutableListOf<String>()
    val edited = mutableListOf<Pair<String, String>>()
    val markedDone = mutableListOf<String>()
    val displayed = mutableListOf<String>()
    val approvedAndDisplayed = mutableListOf<String>()
    val deleted = mutableListOf<String>()
    val added = mutableListOf<Pair<String, String>>()
    var displayCleared = 0
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showQaBoard(
    state: QAUiState.Admin,
    actions: QaActions = QaActions(),
    askForName: Boolean = false,
) = showScreen {
    QAAdminContent(
        state = state,
        onApprove = { actions.approved += it },
        onDeny = { actions.denied += it },
        onEdit = { id, text -> actions.edited += id to text },
        onMarkDone = { actions.markedDone += it },
        onDisplay = { actions.displayed += it },
        onApproveAndDisplay = { actions.approvedAndDisplayed += it },
        onDelete = { actions.deleted += it },
        onAddQuestion = { text, name -> actions.added += text to name },
        askForName = askForName,
        onClearDisplay = { actions.displayCleared++ },
        votingEnabled = state.votingEnabled,
    )
}

/** Renders the question editor's body on its own — no sheet. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showQuestionEditor(
    question: Question,
    onSave: (String) -> Unit = {},
    onDelete: () -> Unit = {},
    onDismiss: () -> Unit = {},
) = showScreen {
    QuestionEditor(
        question = question,
        onSave = onSave,
        onDelete = onDelete,
        onDismiss = onDismiss,
    )
}

/** Renders the add-question dialog on its own. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showAddQuestion(
    askForName: Boolean = false,
    onConfirm: (String, String) -> Unit = { _, _ -> },
    onDismiss: () -> Unit = {},
) = showScreen {
    AddQuestionDialog(
        askForName = askForName,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
