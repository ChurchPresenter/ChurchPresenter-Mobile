package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.song_notes_tag
import churchpresentermobile.composeapp.generated.resources.song_section_cue
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

private const val NOTES_RING_ALPHA = 0.35f
private val NotesShape = RoundedCornerShape(12.dp)
private val TagShape = RoundedCornerShape(5.dp)

/**
 * The song's notes from the desktop, above its verses: one line until tapped, then all of them.
 * For the team only — nothing here reaches the screen.
 */
@Composable
internal fun SongNotesCard(notes: String, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    var expanded by rememberSaveable(notes) { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(UiTags.SONG_DETAIL_NOTES)
            .clip(NotesShape)
            .background(colors.accentTint)
            .border(1.dp, colors.accent.copy(alpha = NOTES_RING_ALPHA), NotesShape)
            .clickable { expanded = !expanded }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = stringResource(Res.string.song_notes_tag).uppercase(),
            color = colors.accent,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.09.em,
            modifier = Modifier
                .padding(top = 2.dp)
                .clip(TagShape)
                .background(colors.accent.copy(alpha = 0.18f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
        Text(
            text = notes,
            color = colors.secondary,
            fontSize = 13.sp,
            lineHeight = (13 * 1.5).sp,
            maxLines = if (expanded) Int.MAX_VALUE else 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** A section's cue from the desktop, beside its label on the verse card. */
@Composable
internal fun SectionCueChip(cue: String, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .widthIn(max = 200.dp)
            .clip(TagShape)
            .background(colors.accentTint)
            .padding(start = 6.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Comment,
            contentDescription = stringResource(Res.string.song_section_cue),
            tint = colors.accent,
            modifier = Modifier.size(11.dp),
        )
        Text(
            text = cue,
            color = colors.accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
