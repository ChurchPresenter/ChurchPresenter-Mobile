package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
import churchpresentermobile.composeapp.generated.resources.qa_upvotes_count
import com.church.presenter.churchpresentermobile.model.Question
import com.church.presenter.churchpresentermobile.model.QuestionStatus
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/** One question on the moderation board, and the pills and badges it is drawn with. */
@Composable
internal fun QuestionCard(
    question: Question,
    isDisplayed: Boolean,
    showVotes: Boolean = false,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    onStartEdit: () -> Unit,
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
                StatusActions(
                    question = question,
                    isLive = isLive,
                    onApprove = onApprove,
                    onDeny = onDeny,
                    onDisplay = onDisplay,
                    onApproveAndDisplay = onApproveAndDisplay,
                    onStop = onStop,
                )
            }
        }
    }
}

/** The actions that depend on where the question is — live, approved, pending or finished. */
@Composable
private fun StatusActions(
    question: Question,
    isLive: Boolean,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    onDisplay: () -> Unit,
    onApproveAndDisplay: () -> Unit,
    onStop: () -> Unit,
) {
    val colors = LocalAppColors.current
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
