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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.qa_admin_add_question
import churchpresentermobile.composeapp.generated.resources.qa_admin_add_question_hint
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
                CircularProgressIndicator(color = colors.accent)
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
                    Text(state.message, color = colors.danger)
                    Button(onClick = { viewModel.loadQuestions() }) {
                        Text(stringResource(Res.string.qa_admin_error_retry))
                    }
                }
            }
            is QAUiState.Admin -> QAAdminContent(
                state = state,
                onApprove = viewModel::approveQuestion,
                onDeny = viewModel::denyQuestion,
                onEdit = viewModel::editQuestion,
                onMarkDone = viewModel::markDone,
                onDisplay = viewModel::displayQuestion,
                onApproveAndDisplay = viewModel::approveAndDisplay,
                onDelete = viewModel::deleteQuestion,
                onAddQuestion = viewModel::addQuestion,
                onClearDisplay = viewModel::clearDisplay,
                votingEnabled = state.votingEnabled
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun QAAdminContent(
    state: QAUiState.Admin,
    onApprove: (String) -> Unit,
    onDeny: (String) -> Unit,
    onEdit: (String, String) -> Unit,
    onMarkDone: (String) -> Unit,
    onDisplay: (String) -> Unit,
    onApproveAndDisplay: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAddQuestion: (String) -> Unit,
    onClearDisplay: () -> Unit,
    votingEnabled: Boolean = false
) {
    val colors = LocalAppColors.current
    var selectedTab by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )

            val displayedList = if (selectedTab == 0) incoming else finished

            if (displayedList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (selectedTab == 0) stringResource(Res.string.qa_admin_no_incoming)
                        else stringResource(Res.string.qa_admin_no_finished),
                        color = colors.muted,
                        fontSize = 15.sp
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
                            onEdit = { newText -> onEdit(question.id, newText) },
                            onMarkDone = { onMarkDone(question.id) },
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
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    if (showAddDialog) {
        AddQuestionDialog(
            onConfirm = { text -> onAddQuestion(text); showAddDialog = false },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun QuestionCard(
    question: Question,
    isDisplayed: Boolean,
    showVotes: Boolean = false,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    onEdit: (String) -> Unit,
    onMarkDone: () -> Unit,
    onDisplay: () -> Unit,
    onApproveAndDisplay: () -> Unit,
    onDelete: () -> Unit,
    onStop: () -> Unit,
) {
    val colors = LocalAppColors.current
    val isLive = isDisplayed
    val isPending = question.status == QuestionStatus.PENDING
    val shape = RoundedCornerShape(14.dp)

    var editing by remember { mutableStateOf(false) }
    var editText by remember(question.text) { mutableStateOf(question.text) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isLive) colors.accentTint else colors.surface)
            .border(
                width = if (isLive) 1.5.dp else 1.dp,
                color = if (isLive) colors.accent else colors.borderSubtle,
                shape = shape
            )
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        // Row 1: question text + status badge
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                if (question.submitterName.isNotBlank()) {
                    Text(
                        question.submitterName,
                        fontSize = 11.sp,
                        color = colors.muted,
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    question.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isPending && !isLive) colors.muted else colors.text,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            StatusBadge(question = question, isLive = isLive)
        }

        Spacer(Modifier.height(12.dp))

        // Row 2: votes (left) + actions (right)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showVotes) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = colors.muted, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("${question.upvotes}", fontSize = 12.sp, color = colors.muted)
            }
            Spacer(Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!editing) {
                    IconSquareButton(Icons.Outlined.Edit, "Edit", colors.muted) { editing = true; editText = question.text }
                    IconSquareButton(Icons.Outlined.Delete, "Delete", colors.muted, onClick = onDelete)
                }
                when {
                    editing -> {
                        IconSquareButton(Icons.Filled.Check, "Save", colors.accent) {
                            if (editText.isNotBlank() && editText.trim() != question.text) onEdit(editText.trim())
                            editing = false
                        }
                        IconSquareButton(Icons.Filled.Close, "Cancel", colors.muted) { editText = question.text; editing = false }
                    }
                    isLive -> {
                        ActionPill("Stop", PillStyle.RED_TINT, onStop)
                    }
                    question.status == QuestionStatus.APPROVED -> {
                        IconSquareButton(Icons.Filled.Close, "Deny", colors.danger, onClick = onDeny)
                        ActionPill("Project", PillStyle.ACCENT_FILL, onDisplay)
                    }
                    question.status == QuestionStatus.PENDING -> {
                        IconSquareButton(Icons.Filled.Close, "Deny", colors.danger, onClick = onDeny)
                        ActionPill("Approve", PillStyle.ACCENT_TINT, onApprove)
                    }
                    else -> { // DONE / DENIED (finished tab)
                        ActionPill("Approve", PillStyle.ACCENT_TINT, onApprove)
                        ActionPill("Project", PillStyle.ACCENT_FILL, onApproveAndDisplay)
                    }
                }
            }
        }

        if (editing) {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = editText,
                onValueChange = { editText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.qa_admin_edit_hint)) },
                singleLine = false,
                maxLines = 4,
            )
        }
    }
}

@Composable
private fun StatusBadge(question: Question, isLive: Boolean) {
    val colors = LocalAppColors.current
    when {
        isLive -> Badge(text = "LIVE", fg = colors.accent, bg = colors.accentTint, dot = true)
        question.status == QuestionStatus.APPROVED -> Badge(text = "✓ Approved", fg = colors.muted, bg = colors.surfaceStrong.copy(alpha = 0f), border = true)
        question.status == QuestionStatus.DENIED -> Badge(text = "Denied", fg = colors.danger, bg = colors.danger.copy(alpha = 0.12f))
        question.status == QuestionStatus.DONE -> Badge(text = "Answered", fg = colors.muted, bg = colors.inputBg)
        else -> {} // pending: no badge (muted text conveys it)
    }
}

@Composable
private fun Badge(text: String, fg: Color, bg: Color, dot: Boolean = false, border: Boolean = false) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .then(if (border) Modifier.border(1.dp, colors.border, RoundedCornerShape(20.dp)) else Modifier)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (dot) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(fg))
        }
        Text(text, color = fg, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

private enum class PillStyle { ACCENT_TINT, ACCENT_FILL, RED_TINT }

@Composable
private fun ActionPill(label: String, style: PillStyle, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val (bg, fg) = when (style) {
        PillStyle.ACCENT_TINT -> colors.accentTint to colors.accent
        PillStyle.ACCENT_FILL -> colors.accent to colors.onAccent
        PillStyle.RED_TINT -> colors.danger.copy(alpha = 0.12f) to colors.danger
    }
    Box(
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun IconSquareButton(icon: ImageVector, desc: String, tint: Color, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.inputBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun AddQuestionDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.qa_admin_add_question)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(Res.string.qa_admin_add_question_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 5
            )
        },
        confirmButton = {
            Button(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }, enabled = text.isNotBlank()) {
                Text(stringResource(Res.string.qa_admin_save))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(Res.string.qa_admin_cancel)) }
        }
    )
}
