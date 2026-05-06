package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.qa_admin_add_question
import churchpresentermobile.composeapp.generated.resources.qa_admin_add_question_hint
import churchpresentermobile.composeapp.generated.resources.qa_admin_cancel
import churchpresentermobile.composeapp.generated.resources.qa_admin_clear_display
import churchpresentermobile.composeapp.generated.resources.qa_admin_edit_hint
import churchpresentermobile.composeapp.generated.resources.qa_admin_error_retry
import churchpresentermobile.composeapp.generated.resources.qa_admin_finished_tab
import churchpresentermobile.composeapp.generated.resources.qa_admin_incoming_tab
import churchpresentermobile.composeapp.generated.resources.qa_admin_no_finished
import churchpresentermobile.composeapp.generated.resources.qa_admin_no_incoming
import churchpresentermobile.composeapp.generated.resources.qa_admin_now_live
import churchpresentermobile.composeapp.generated.resources.qa_admin_save
import churchpresentermobile.composeapp.generated.resources.qa_admin_session_active
import churchpresentermobile.composeapp.generated.resources.qa_admin_session_stopped
import com.church.presenter.churchpresentermobile.model.Question
import com.church.presenter.churchpresentermobile.model.QuestionStatus
import com.church.presenter.churchpresentermobile.viewmodel.QAUiState
import com.church.presenter.churchpresentermobile.viewmodel.QAViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QAAdminScreen(
    viewModel: QAViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionError) {
        if (actionError != null) {
            snackbarHostState.showSnackbar(actionError!!)
            viewModel.clearActionError()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {
                is QAUiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
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
                        Text(state.message, color = MaterialTheme.colorScheme.error)
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
                    onRefresh = { viewModel.loadQuestions() },
                    votingEnabled = state.votingEnabled
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onRefresh: () -> Unit,
    votingEnabled: Boolean = false
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    val incoming = state.questions.filter {
        it.status == QuestionStatus.PENDING || it.status == QuestionStatus.APPROVED
    }.sortedBy { it.timestamp }

    val finished = state.questions.filter {
        it.status == QuestionStatus.DONE || it.status == QuestionStatus.DENIED
    }

    val displayedQuestion = state.questions.firstOrNull { it.id == state.displayedQuestionId }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Session badge + refresh
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val sessionColor = if (state.sessionActive) Color(0xFF43A047) else Color(0xFFE53935)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(sessionColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (state.sessionActive)
                            stringResource(Res.string.qa_admin_session_active)
                        else
                            stringResource(Res.string.qa_admin_session_stopped),
                        style = MaterialTheme.typography.labelSmall,
                        color = sessionColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }

            // Live display banner
            if (displayedQuestion != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF43A047).copy(alpha = 0.12f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = Color(0xFF43A047), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(Res.string.qa_admin_now_live) + " " + displayedQuestion.text.take(60),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(onClick = onClearDisplay) {
                        Text(stringResource(Res.string.qa_admin_clear_display), fontSize = 11.sp)
                    }
                }
            }

            // Tab row
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(Res.string.qa_admin_incoming_tab, incoming.size)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(Res.string.qa_admin_finished_tab, finished.size)) }
                )
            }

            val displayedList = if (selectedTab == 0) incoming else finished

            if (displayedList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (selectedTab == 0) stringResource(Res.string.qa_admin_no_incoming)
                        else stringResource(Res.string.qa_admin_no_finished),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(displayedList, key = { it.id }) { question ->
                        QuestionItem(
                            question = question,
                            isDisplayed = question.id == state.displayedQuestionId,
                            showVotes = votingEnabled,
                            onApprove = { onApprove(question.id) },
                            onDeny = { onDeny(question.id) },
                            onEdit = { newText -> onEdit(question.id, newText) },
                            onMarkDone = { onMarkDone(question.id) },
                            onDisplay = { onDisplay(question.id) },
                            onApproveAndDisplay = { onApproveAndDisplay(question.id) },
                            onDelete = { onDelete(question.id) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.qa_admin_add_question))
        }
    }

    if (showAddDialog) {
        AddQuestionDialog(
            onConfirm = { text -> onAddQuestion(text); showAddDialog = false },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun QuestionItem(
    question: Question,
    isDisplayed: Boolean,
    showVotes: Boolean = false,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    onEdit: (String) -> Unit,
    onMarkDone: () -> Unit,
    onDisplay: () -> Unit,
    onApproveAndDisplay: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (question.status) {
        QuestionStatus.PENDING  -> Color(0xFFFFA726)
        QuestionStatus.APPROVED -> Color(0xFF66BB6A)
        QuestionStatus.DENIED   -> Color(0xFFEF5350)
        QuestionStatus.DONE     -> Color(0xFF42A5F5)
    }
    val bgColor = if (isDisplayed) Color(0xFF43A047).copy(alpha = 0.08f) else Color.Transparent

    var editing by remember { mutableStateOf(false) }
    var editText by remember(question.text) { mutableStateOf(question.text) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(statusColor))
            Spacer(Modifier.width(8.dp))

            if (!editing) {
                Column(modifier = Modifier.weight(1f)) {
                    if (question.submitterName.isNotBlank()) {
                        Text(
                            question.submitterName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(question.text, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    if (showVotes) {
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (question.upvotes > 0) {
                                VoteChip(label = "▲ ${question.upvotes}", color = Color(0xFF43A047))
                            }
                            if (question.downvotes > 0) {
                                VoteChip(label = "▼ ${question.downvotes}", color = Color(0xFFE53935))
                            }
                            if (question.upvotes == 0 && question.downvotes == 0) {
                                Text("no votes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            when {
                editing -> {
                    IconButton(onClick = {
                        if (editText.isNotBlank() && editText.trim() != question.text) onEdit(editText.trim())
                        editing = false
                    }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Check, null, tint = Color(0xFF43A047), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { editText = question.text; editing = false }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
                question.status == QuestionStatus.PENDING -> {
                    IconButton(onClick = { editing = true; editText = question.text }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, null, tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onApprove, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Check, null, tint = Color(0xFF43A047), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDeny, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
                question.status == QuestionStatus.APPROVED -> {
                    IconButton(onClick = { editing = true; editText = question.text }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, null, tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp))
                    }
                    if (!isDisplayed) {
                        IconButton(onClick = onDisplay, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Tv, null, tint = Color(0xFF1E88E5), modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = onMarkDone, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Done, null, tint = Color(0xFF42A5F5), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDeny, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
                else -> { // DONE or DENIED
                    IconButton(onClick = onApprove, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Refresh, null, tint = Color(0xFFFFA726), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onApproveAndDisplay, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Tv, null, tint = Color(0xFF1E88E5).copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        if (editing) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = editText,
                onValueChange = { editText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.qa_admin_edit_hint)) },
                singleLine = false,
                maxLines = 4,
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun VoteChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
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
