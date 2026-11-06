package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.standalone_theme_delete
import com.church.presenter.churchpresentermobile.model.NamedTheme
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideBackdrop
import com.church.presenter.churchpresentermobile.model.SlideTextSize
import com.church.presenter.churchpresentermobile.model.SlideTheme
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

private val THUMB_WIDTH = 112.dp
private val THUMB_HEIGHT = 63.dp

/** Stands in for a verse: it shows where the words sit, and nobody can read a thumbnail anyway. */
private const val THUMB_WORDS = "\u2014\u2014\u2014\u2014\n\u2014\u2014\u2014\u2014\u2014\u2014\n\u2014\u2014\u2014"

/**
 * Whole looks in one tap, each drawn by the renderer the audience sees, so what
 * is on the thumbnail is where the words will sit. A church's own saved looks
 * come after the built-in ones and can be removed.
 *
 * @param current The look on screen now; the one that matches it is outlined.
 */
@Composable
internal fun LookPresets(
    presets: List<NamedTheme>,
    savedThemes: List<NamedTheme>,
    current: SlideTheme,
    onPreset: (NamedTheme) -> Unit,
    onSaved: (NamedTheme) -> Unit,
    onDeleteSaved: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(presets, key = { "preset:${it.name}" }) { preset ->
            LookThumbnail(
                look = preset,
                selected = sameLook(preset.theme, current),
                onClick = { onPreset(preset) },
                modifier = Modifier.testTag(LookTags.preset(preset.name)),
            )
        }
        items(savedThemes, key = { "saved:${it.name}" }) { saved ->
            LookThumbnail(
                look = saved,
                selected = saved.theme == current,
                onClick = { onSaved(saved) },
                onDelete = { onDeleteSaved(saved.name) },
                modifier = Modifier.testTag(LookTags.savedTheme(saved.name)),
                deleteModifier = Modifier.testTag(LookTags.deleteTheme(saved.name)),
            )
        }
    }
}

/** Whether [current] is [preset], leaving aside what a preset does not change. */
internal fun sameLook(preset: SlideTheme, current: SlideTheme): Boolean =
    preset == current.copy(brandLine = preset.brandLine, showChords = preset.showChords)

@Composable
private fun LookThumbnail(
    look: NamedTheme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
    deleteModifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier.width(THUMB_WIDTH).clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(THUMB_WIDTH, THUMB_HEIGHT)
                .clip(shape)
                .border(if (selected) 2.dp else 1.dp, if (selected) colors.accent else colors.borderSubtle, shape),
        ) {
            StandaloneOutputScreen(
                slide = Slide(
                    body = THUMB_WORDS,
                    theme = look.theme,
                    backdrop = SlideBackdrop.GRADIENT,
                    textSize = SlideTextSize.LARGE,
                ),
                showClock = false,
            )
            if (onDelete != null) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(Res.string.standalone_theme_delete),
                    tint = Color.White,
                    modifier = deleteModifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(16.dp)
                        .clickable(onClick = onDelete),
                )
            }
        }
        Text(
            text = look.name,
            color = colors.text,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
