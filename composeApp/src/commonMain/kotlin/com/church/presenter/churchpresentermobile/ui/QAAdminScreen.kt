package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.qa_char_counter
import churchpresentermobile.composeapp.generated.resources.qa_delete_question
import churchpresentermobile.composeapp.generated.resources.qa_edit_question_title
import churchpresentermobile.composeapp.generated.resources.qa_submitted_by
import churchpresentermobile.composeapp.generated.resources.qa_admin_add_question
import churchpresentermobile.composeapp.generated.resources.qa_admin_add_question_hint
import churchpresentermobile.composeapp.generated.resources.qa_admin_add_question_name
import churchpresentermobile.composeapp.generated.resources.qa_admin_add_question_name_hint
import churchpresentermobile.composeapp.generated.resources.qa_admin_cancel
import churchpresentermobile.composeapp.generated.resources.qa_admin_edit_hint
import churchpresentermobile.composeapp.generated.resources.qa_admin_error_retry
import churchpresentermobile.composeapp.generated.resources.qa_admin_finished_tab
import churchpresentermobile.composeapp.generated.resources.qa_admin_incoming_tab
import churchpresentermobile.composeapp.generated.resources.qa_admin_no_finished
import churchpresentermobile.composeapp.generated.resources.qa_admin_no_incoming
import churchpresentermobile.composeapp.generated.resources.qa_admin_save
import com.church.presenter.churchpresentermobile.model.Question
import com.church.presenter.churchpresentermobile.model.QuestionStatus
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.QAUiState
import com.church.presenter.churchpresentermobile.viewmodel.QAViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QAAdminScreen(
    viewModel: QAViewModel,
    modifier: Modifier = Modifier,
    settingsSaveToken: Int = 0,
) {
    val colors = LocalAppColors.current
    val uiState by viewModel.uiState.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    val needsAuthorName by viewModel.needsAuthorName.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Re-fetch when the user saves Settings so a newly-entered API key is used
    // immediately (otherwise the tab would stay on the pre-key 401).
    LaunchedEffect(settingsSaveToken) {
        if (settingsSaveToken > 0) viewModel.onSettingsSaved()
    }

    LaunchedEffect(actionError) {
        if (actionError != null) {
            snackbarHostState.showSnackbar(actionError!!)
            viewModel.clearActionError()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        when (val state = uiState) {
            is QAUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = colors.accent,
                    modifier = Modifier.testTag(UiTags.QA_LOADING),
                )
            }
            is QAUiState.Error -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        state.message,
                        color = colors.danger,
                        modifier = Modifier.testTag(UiTags.QA_ERROR),
                    )
                    Button(
                        onClick = { viewModel.loadQuestions() },
                        modifier = Modifier.testTag(UiTags.QA_RETRY),
                    ) {
                        Text(stringResource(Res.string.qa_admin_error_retry))
                    }
                }
            }
            is QAUiState.Admin -> QAAdminContent(
                state = state,
                onApprove = viewModel::approveQuestion,
                onDeny = viewModel::denyQuestion,
                onEdit = viewModel::editQuestion,
                onDisplay = viewModel::displayQuestion,
                onApproveAndDisplay = viewModel::approveAndDisplay,
                onDelete = viewModel::deleteQuestion,
                onAddQuestion = viewModel::addQuestion,
                askForName = needsAuthorName,
                onClearDisplay = viewModel::clearDisplay,
                votingEnabled = state.votingEnabled
            )
        }

        ToastHost(snackbarHostState)
    }
}

/**
 * The board itself: the two tabs, the question cards and their actions.
 *
 * `internal` and separate from [QAAdminScreen] so the whole of it can be
 * exercised without a desktop. Everything it does is decided here from plain
 * values and reported through plain callbacks; the screen above only supplies a
 * ViewModel's state and forwards them.
 */
@Composable
internal fun QAAdminContent(
    state: QAUiState.Admin,
    onApprove: (String) -> Unit,
    onDeny: (String) -> Unit,
    onEdit: (String, String) -> Unit,
    onDisplay: (String) -> Unit,
    onApproveAndDisplay: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAddQuestion: (text: String, name: String) -> Unit,
    askForName: Boolean = false,
    onClearDisplay: () -> Unit,
    votingEnabled: Boolean = false
) {
    val colors = LocalAppColors.current
    var selectedTab by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingQuestion by remember { mutableStateOf<Question?>(null) }

    val incoming = state.questions.filter {
        it.status == QuestionStatus.PENDING || it.status == QuestionStatus.APPROVED
    }.sortedBy { it.timestamp }

    val finished = state.questions.filter {
        it.status == QuestionStatus.DONE || it.status == QuestionStatus.DENIED
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Segmented filter (Incoming / Answered) ────────────────────
            SegmentedControl(
                options = listOf(
                    stringResource(Res.string.qa_admin_incoming_tab, incoming.size),
                    stringResource(Res.string.qa_admin_finished_tab, finished.size),
                ),
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it },
                optionTag = { UiTags.qaTab(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )

            val displayedList = if (selectedTab == 0) incoming else finished

            if (displayedList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (selectedTab == 0) stringResource(Res.string.qa_admin_no_incoming)
                        else stringResource(Res.string.qa_admin_no_finished),
                        color = colors.muted,
                        fontSize = 15.sp,
                        modifier = Modifier.testTag(
                            if (selectedTab == 0) UiTags.QA_EMPTY_INCOMING
                            else UiTags.QA_EMPTY_FINISHED
                        ),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, bottom = 90.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    items(displayedList, key = { it.id }) { question ->
                        QuestionCard(
                            question = question,
                            isDisplayed = question.id == state.displayedQuestionId,
                            showVotes = votingEnabled,
                            onApprove = { onApprove(question.id) },
                            onDeny = { onDeny(question.id) },
                            onStartEdit = { editingQuestion = question },
                            onDisplay = { onDisplay(question.id) },
                            onApproveAndDisplay = { onApproveAndDisplay(question.id) },
                            onDelete = { onDelete(question.id) },
                            onStop = onClearDisplay,
                        )
                    }
                }
            }
        }

        // Add-question FAB (accent)
        SquareFab(
            icon = Icons.Filled.Add,
            contentDescription = stringResource(Res.string.qa_admin_add_question),
            containerColor = colors.accent,
            iconColor = colors.onAccent,
            shadowColor = colors.accent.copy(alpha = if (colors.isDark) 0.35f else 0.3f),
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).testTag(UiTags.QA_ADD),
        )
    }

    if (showAddDialog) {
        AddQuestionDialog(
            askForName = askForName,
            onConfirm = { text, name -> onAddQuestion(text, name); showAddDialog = false },
            onDismiss = { showAddDialog = false }
        )
    }

    editingQuestion?.let { q ->
        EditQuestionSheet(
            question = q,
            onSave = { text -> onEdit(q.id, text); editingQuestion = null },
            onDelete = { onDelete(q.id); editingQuestion = null },
            onDismiss = { editingQuestion = null },
        )
    }
}

/** Bottom-sheet editor for a question (design screens 6c/6d). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditQuestionSheet(
    question: Question,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.sheetBackground,
        scrimColor = colors.scrim,
    ) {
        QuestionEditor(
            question = question,
            onSave = onSave,
            onDelete = onDelete,
            onDismiss = onDismiss,
        )
    }
}

/**
 * The editor's body, without the sheet around it.
 *
 * `internal` for the same reason [EntryDetail] is: a `ModalBottomSheet` brings a
 * scrim and a spring animation that say nothing about editing a question, and
 * none of the fields below are reachable until that animation settles.
 */
@Composable
internal fun QuestionEditor(
    question: Question,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    var text by remember(question.id) { mutableStateOf(question.text) }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScrollWithScrollbar(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
    ) {
        QuestionEditorHeader(onSave = { if (text.isNotBlank()) onSave(text.trim()) }, onDismiss = onDismiss)

        Spacer(Modifier.height(16.dp))
        // Editable text field with accent focus ring
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 110.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.inputBg)
                .border(1.5.dp, colors.accent, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            if (text.isEmpty()) {
                Text(stringResource(Res.string.qa_admin_edit_hint), color = colors.muted, fontSize = 15.sp)
            }
            BasicTextField(
                value = text,
                onValueChange = { if (it.length <= 200) text = it },
                textStyle = TextStyle(color = colors.text, fontSize = 15.sp, lineHeight = (15 * 1.5).sp),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth().testTag(UiTags.QA_EDIT_TEXT),
            )
        }

        Spacer(Modifier.height(8.dp))
        // Meta: submitter + char counter
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                if (question.submitterName.isNotBlank()) {
                    stringResource(Res.string.qa_submitted_by, question.submitterName)
                } else {
                    ""
                },
                color = colors.muted, fontSize = 11.sp,
                modifier = Modifier.testTag(UiTags.QA_EDIT_SUBMITTER),
            )
            Text(
                stringResource(Res.string.qa_char_counter, text.length),
                color = colors.muted, fontSize = 11.sp,
                modifier = Modifier.testTag(UiTags.QA_EDIT_COUNTER),
            )
        }

        Spacer(Modifier.height(20.dp))
        // Full-width delete button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(colors.danger.copy(alpha = 0.12f))
                .border(1.dp, colors.danger.copy(alpha = 0.5f), RoundedCornerShape(13.dp))
                .testTag(UiTags.QA_EDIT_DELETE)
                .clickable { onDelete() },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = null,
                tint = colors.danger,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(Res.string.qa_delete_question),
                color = colors.danger,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** The editor's top bar: Cancel, the title, and Save. */
@Composable
private fun QuestionEditorHeader(onSave: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(Res.string.qa_admin_cancel),
            color = colors.muted, fontSize = 15.sp,
            modifier = Modifier.testTag(UiTags.QA_EDIT_CANCEL).clickable { onDismiss() },
        )
        Text(
            stringResource(Res.string.qa_edit_question_title),
            color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(colors.accent)
                .testTag(UiTags.QA_EDIT_SAVE)
                .clickable(onClick = onSave)
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            Text(
                stringResource(Res.string.qa_admin_save),
                color = colors.onAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
internal fun AddQuestionDialog(
    askForName: Boolean,
    onConfirm: (text: String, name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.qa_admin_add_question)) },
        text = {
            ScrollingDialogBody(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(Res.string.qa_admin_add_question_hint)) },
                    modifier = Modifier.fillMaxWidth().testTag(UiTags.QA_ADD_TEXT),
                    singleLine = false,
                    maxLines = 5
                )
                // Only while nobody has said who they are — once given, the name is
                // remembered in Settings and this field stops appearing.
                if (askForName) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(Res.string.qa_admin_add_question_name)) },
                        placeholder = { Text(stringResource(Res.string.qa_admin_add_question_name_hint)) },
                        modifier = Modifier.fillMaxWidth().testTag(UiTags.QA_ADD_NAME),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text.trim(), name.trim()) },
                enabled = text.isNotBlank(),
                modifier = Modifier.testTag(UiTags.QA_ADD_CONFIRM),
            ) {
                Text(stringResource(Res.string.qa_admin_save))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(UiTags.QA_ADD_CANCEL),
            ) { Text(stringResource(Res.string.qa_admin_cancel)) }
        }
    )
}
