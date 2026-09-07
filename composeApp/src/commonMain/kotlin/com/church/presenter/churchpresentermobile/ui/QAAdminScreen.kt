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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.action_go_live
import churchpresentermobile.composeapp.generated.resources.cd_delete
import churchpresentermobile.composeapp.generated.resources.cd_deny
import churchpresentermobile.composeapp.generated.resources.cd_edit
import churchpresentermobile.composeapp.generated.resources.qa_action_approve
import churchpresentermobile.composeapp.generated.resources.qa_action_stop
import churchpresentermobile.composeapp.generated.resources.qa_badge_answered
import churchpresentermobile.composeapp.generated.resources.qa_badge_approved
import churchpresentermobile.composeapp.generated.resources.qa_badge_denied
import churchpresentermobile.composeapp.generated.resources.qa_badge_live
import churchpresentermobile.composeapp.generated.resources.qa_char_counter
import churchpresentermobile.composeapp.generated.resources.qa_delete_question
import churchpresentermobile.composeapp.generated.resources.qa_edit_question_title
import churchpresentermobile.composeapp.generated.resources.qa_submitted_by
import churchpresentermobile.composeapp.generated.resources.qa_upvotes_count
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
                onMarkDone = viewModel::markDone,
                onDisplay = viewModel::displayQuestion,
                onApproveAndDisplay = viewModel::approveAndDisplay,
                onDelete = viewModel::deleteQuestion,
                onAddQuestion = viewModel::addQuestion,
                askForName = needsAuthorName,
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
    onMarkDone: (String) -> Unit,
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

@Composable
private fun QuestionCard(
    question: Question,
    isDisplayed: Boolean,
    showVotes: Boolean = false,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    onStartEdit: () -> Unit,
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(UiTags.qaCard(question.id))
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
                Text(
                    stringResource(Res.string.qa_upvotes_count, question.upvotes),
                    fontSize = 12.sp,
                    color = colors.muted,
                    modifier = Modifier.testTag(UiTags.qaVotes(question.id)),
                )
            }
            Spacer(Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconSquareButton(
                    Icons.Outlined.Edit, stringResource(Res.string.cd_edit), colors.muted,
                    onClick = onStartEdit,
                    modifier = Modifier.testTag(UiTags.qaEdit(question.id)),
                )
                IconSquareButton(
                    Icons.Outlined.Delete, stringResource(Res.string.cd_delete), colors.muted,
                    onClick = onDelete,
                    modifier = Modifier.testTag(UiTags.qaDelete(question.id)),
                )
                when {
                    isLive -> ActionPill(
                        stringResource(Res.string.qa_action_stop), PillStyle.RED_TINT, onStop,
                        modifier = Modifier.testTag(UiTags.qaStop(question.id)),
                    )
                    question.status == QuestionStatus.APPROVED -> {
                        IconSquareButton(
                            Icons.Filled.Close, stringResource(Res.string.cd_deny), colors.danger,
                            onClick = onDeny,
                            modifier = Modifier.testTag(UiTags.qaDeny(question.id)),
                        )
                        ActionPill(
                            stringResource(Res.string.action_go_live), PillStyle.ACCENT_FILL, onDisplay,
                            modifier = Modifier.testTag(UiTags.qaGoLive(question.id)),
                        )
                    }
                    question.status == QuestionStatus.PENDING -> {
                        IconSquareButton(
                            Icons.Filled.Close, stringResource(Res.string.cd_deny), colors.danger,
                            onClick = onDeny,
                            modifier = Modifier.testTag(UiTags.qaDeny(question.id)),
                        )
                        ActionPill(
                            stringResource(Res.string.qa_action_approve), PillStyle.ACCENT_TINT, onApprove,
                            modifier = Modifier.testTag(UiTags.qaApprove(question.id)),
                        )
                    }
                    else -> { // DONE / DENIED (finished tab)
                        ActionPill(
                            stringResource(Res.string.qa_action_approve), PillStyle.ACCENT_TINT, onApprove,
                            modifier = Modifier.testTag(UiTags.qaApprove(question.id)),
                        )
                        ActionPill(
                            stringResource(Res.string.action_go_live), PillStyle.ACCENT_FILL, onApproveAndDisplay,
                            modifier = Modifier.testTag(UiTags.qaGoLive(question.id)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(question: Question, isLive: Boolean) {
    val colors = LocalAppColors.current
    val id = question.id
    when {
        isLive -> Badge(
            text = stringResource(Res.string.qa_badge_live),
            fg = colors.accent,
            bg = colors.accentTint,
            dot = true,
            modifier = Modifier.testTag(UiTags.qaBadge(id, QaBadge.LIVE)),
        )
        question.status == QuestionStatus.APPROVED -> Badge(
            text = stringResource(Res.string.qa_badge_approved),
            fg = colors.muted,
            bg = colors.surfaceStrong.copy(alpha = 0f),
            border = true,
            modifier = Modifier.testTag(UiTags.qaBadge(id, QaBadge.APPROVED)),
        )
        question.status == QuestionStatus.DENIED -> Badge(
            text = stringResource(Res.string.qa_badge_denied),
            fg = colors.danger,
            bg = colors.danger.copy(alpha = 0.12f),
            modifier = Modifier.testTag(UiTags.qaBadge(id, QaBadge.DENIED)),
        )
        question.status == QuestionStatus.DONE -> Badge(
            text = stringResource(Res.string.qa_badge_answered),
            fg = colors.muted,
            bg = colors.inputBg,
            modifier = Modifier.testTag(UiTags.qaBadge(id, QaBadge.ANSWERED)),
        )
        else -> {} // pending: no badge (muted text conveys it)
    }
}

/** The badge kinds a card can carry, named in one place for [UiTags.qaBadge]. */
internal object QaBadge {
    const val LIVE = "live"
    const val APPROVED = "approved"
    const val DENIED = "denied"
    const val ANSWERED = "answered"
}

@Composable
private fun Badge(
    text: String,
    fg: Color,
    bg: Color,
    dot: Boolean = false,
    border: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
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
private fun ActionPill(
    label: String,
    style: PillStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val (bg, fg) = when (style) {
        PillStyle.ACCENT_TINT -> colors.accentTint to colors.accent
        PillStyle.ACCENT_FILL -> colors.accent to colors.onAccent
        PillStyle.RED_TINT -> colors.danger.copy(alpha = 0.12f) to colors.danger
    }
    Box(
        modifier = modifier
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
private fun IconSquareButton(
    icon: ImageVector,
    desc: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.inputBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(16.dp))
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

    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
        // Header: Cancel / Edit question / Save
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
                    .clickable { if (text.isNotBlank()) onSave(text.trim()) }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            ) {
                Text(stringResource(Res.string.qa_admin_save), color = colors.onAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

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
                if (question.submitterName.isNotBlank()) stringResource(Res.string.qa_submitted_by, question.submitterName) else "",
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
            Icon(Icons.Outlined.Delete, contentDescription = null, tint = colors.danger, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.qa_delete_question), color = colors.danger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
