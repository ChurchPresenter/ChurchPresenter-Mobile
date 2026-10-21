package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.display_aspect_standard
import churchpresentermobile.composeapp.generated.resources.display_aspect_wide
import churchpresentermobile.composeapp.generated.resources.display_back_present
import churchpresentermobile.composeapp.generated.resources.display_background_dark
import churchpresentermobile.composeapp.generated.resources.display_background_light
import churchpresentermobile.composeapp.generated.resources.display_background_output
import churchpresentermobile.composeapp.generated.resources.display_full_screen
import churchpresentermobile.composeapp.generated.resources.display_group_bible
import churchpresentermobile.composeapp.generated.resources.display_group_songs
import churchpresentermobile.composeapp.generated.resources.display_live_preview
import churchpresentermobile.composeapp.generated.resources.display_live_slide
import churchpresentermobile.composeapp.generated.resources.display_preview_hint
import churchpresentermobile.composeapp.generated.resources.display_preview_song
import churchpresentermobile.composeapp.generated.resources.display_reset
import churchpresentermobile.composeapp.generated.resources.display_sample_text
import churchpresentermobile.composeapp.generated.resources.display_save
import churchpresentermobile.composeapp.generated.resources.display_screen_options
import churchpresentermobile.composeapp.generated.resources.display_section_screen
import churchpresentermobile.composeapp.generated.resources.display_standalone_only
import churchpresentermobile.composeapp.generated.resources.display_standalone_only_hint
import churchpresentermobile.composeapp.generated.resources.display_text_title
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.model.TextElement
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/**
 * Tablet: every element listed on the left, the selected one's controls in the middle, and a
 * large live preview on the right — so a change and its effect are on screen together.
 */
@Composable
internal fun DisplayTextTablet(frame: DisplayTextFrame) {
    val colors = LocalAppColors.current
    Row(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .testTag(DisplayTextTags.SCREEN),
    ) {
        ElementList(frame, Modifier.width(LIST_WIDTH).fillMaxHeight())
        VerticalRule()
        ControlsColumn(frame, Modifier.width(CONTROLS_WIDTH).fillMaxHeight())
        VerticalRule()
        PreviewColumn(frame, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun VerticalRule() {
    Box(Modifier.width(1.dp).fillMaxHeight().background(LocalAppColors.current.borderSubtle))
}

@Composable
private fun ElementList(frame: DisplayTextFrame, modifier: Modifier) {
    val colors = LocalAppColors.current
    // Language 2 is a page of Lyrics, so Lyrics stays lit while it is open.
    val element = frame.editor.element
    val selected = if (element == TextElement.SONG_LYRICS_SECOND) TextElement.SONG_LYRICS else element
    Column(modifier.padding(horizontal = 14.dp, vertical = 18.dp)) {
        Row(
            Modifier.clickable(onClick = frame.actions.onBack).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackArrow(frame.actions.onBack)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(Res.string.display_back_present),
                color = colors.accent,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            stringResource(Res.string.display_text_title),
            color = colors.text,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.03).em,
            modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 6.dp),
        )
        StandaloneBadge(Modifier.padding(start = 8.dp))
        Spacer(Modifier.height(14.dp))
        DisplayOverline(stringResource(Res.string.display_group_bible), Modifier.padding(start = 4.dp))
        BIBLE_TABS.forEach { NavRow(it, it == selected) { frame.editor.onSelect(it); frame.preview.onKind(it.kind) } }
        Spacer(Modifier.height(10.dp))
        DisplayOverline(stringResource(Res.string.display_group_songs), Modifier.padding(start = 4.dp))
        SONG_TABS.forEach { NavRow(it, it == selected) { frame.editor.onSelect(it); frame.preview.onKind(it.kind) } }
        Spacer(Modifier.height(10.dp))
        DisplayOverline(stringResource(Res.string.display_section_screen), Modifier.padding(start = 4.dp))
        NavLink(
            stringResource(Res.string.display_screen_options),
            DisplayTextTags.SCREEN_OPTIONS,
            frame.actions.onOpenScreenOptions,
        )
        Spacer(Modifier.weight(1f))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
                .padding(14.dp),
        ) {
            Text(
                stringResource(Res.string.display_standalone_only),
                color = colors.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(stringResource(Res.string.display_standalone_only_hint), color = colors.muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun NavRow(element: TextElement, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .testTag(DisplayTextTags.elementTab(element))
            .semantics { this.selected = selected }
            .clip(shape)
            .background(if (selected) colors.accentTint else colors.background)
            .then(if (selected) Modifier.border(1.dp, colors.accent.copy(alpha = 0.5f), shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(element.tabLabel()),
            color = if (selected) colors.accent else colors.text,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        if (!selected) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.dim,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun NavLink(label: String, tag: String, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .testTag(tag)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = colors.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.dim,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ControlsColumn(frame: DisplayTextFrame, modifier: Modifier) {
    val colors = LocalAppColors.current
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(frame.editor.element.heading()),
                color = colors.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.03).em,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(Res.string.display_reset),
                color = colors.secondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .testTag(DisplayTextTags.RESET)
                    .clickable(onClick = frame.actions.onReset)
                    .padding(6.dp),
            )
        }
        ElementControls(frame.editor, inlineLanguage = true)
    }
}

@Composable
private fun PreviewColumn(frame: DisplayTextFrame, modifier: Modifier) {
    val colors = LocalAppColors.current
    val options = frame.preview.options
    Column(modifier.padding(horizontal = 22.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DisplayOverline(stringResource(Res.string.display_live_preview), Modifier.weight(1f))
            DisplaySegments(
                options = listOf(
                    stringResource(Res.string.display_group_bible),
                    stringResource(Res.string.display_preview_song),
                ),
                selectedIndex = if (frame.preview.kind == SlideKind.SONG) 1 else 0,
                onSelect = { frame.preview.onKind(if (it == 1) SlideKind.SONG else SlideKind.BIBLE) },
                tag = { "${DisplayTextTags.PREVIEW}:kind:$it" },
                modifier = Modifier.width(PREVIEW_SWITCH_WIDTH),
                compact = true,
            )
        }
        PreviewCard(
            slide = frame.preview.slideFor(frame.preview.kind),
            element = frame.editor.element,
            onElementTap = frame.preview.onElementTap,
            aspect = options.aspect,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = PreviewOptionSpacing) {
            DisplayDropdown(
                selectedIndex = if (options.useLive) 1 else 0,
                options = listOf(
                    stringResource(Res.string.display_sample_text),
                    stringResource(Res.string.display_live_slide),
                ),
                onSelect = { frame.preview.onOptions(options.copy(useLive = it == 1)) },
                tag = DisplayTextTags.SAMPLE,
                modifier = Modifier.weight(1f),
                fill = true,
            )
            DisplayDropdown(
                selectedIndex = PreviewAspect.entries.indexOf(options.aspect),
                options = listOf(
                    stringResource(Res.string.display_aspect_wide),
                    stringResource(Res.string.display_aspect_standard),
                ),
                onSelect = { frame.preview.onOptions(options.copy(aspect = PreviewAspect.entries[it])) },
                tag = DisplayTextTags.ASPECT,
                modifier = Modifier.weight(1f),
                fill = true,
            )
            DisplayDropdown(
                selectedIndex = PreviewBackground.entries.indexOf(options.background),
                options = listOf(
                    stringResource(Res.string.display_background_output),
                    stringResource(Res.string.display_background_dark),
                    stringResource(Res.string.display_background_light),
                ),
                onSelect = { frame.preview.onOptions(options.copy(background = PreviewBackground.entries[it])) },
                tag = DisplayTextTags.BACKGROUND,
                modifier = Modifier.weight(1f),
                fill = true,
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.accentTint)
                .border(1.dp, colors.accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
            Text(stringResource(Res.string.display_preview_hint), color = colors.text, fontSize = 13.sp)
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            DisplayActions(
                onReset = frame.actions.onReset,
                onSave = frame.actions.onSave,
                resetLabel = stringResource(Res.string.display_reset),
                saveLabel = stringResource(Res.string.display_save),
                modifier = Modifier.width(ACTIONS_WIDTH),
                leading = {
                    ActionButton(
                        stringResource(Res.string.display_full_screen),
                        frame.actions.onExpand,
                        DisplayTextTags.EXPAND,
                        filled = false,
                        Modifier.weight(FULL_SCREEN_WEIGHT),
                    )
                },
            )
        }
    }
}

private val LIST_WIDTH = 250.dp
private val CONTROLS_WIDTH = 370.dp
private val PREVIEW_SWITCH_WIDTH = 220.dp
private val ACTIONS_WIDTH = 380.dp

/** "Full-screen preview" is the longer label of the two buttons. */
private const val FULL_SCREEN_WEIGHT = 1.4f
