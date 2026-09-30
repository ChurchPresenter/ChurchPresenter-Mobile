package com.church.presenter.churchpresentermobile.ui.bibles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.bibles_all_languages
import churchpresentermobile.composeapp.generated.resources.bibles_done
import churchpresentermobile.composeapp.generated.resources.bibles_language
import churchpresentermobile.composeapp.generated.resources.bibles_on_this_device
import churchpresentermobile.composeapp.generated.resources.bibles_search_languages
import com.church.presenter.churchpresentermobile.ui.verticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import churchpresentermobile.composeapp.generated.resources.Res
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.CatalogView
import com.church.presenter.churchpresentermobile.viewmodel.LanguageOption
import org.jetbrains.compose.resources.stringResource

/** "Language  All languages ........ 1,248 ⌄" */
@Composable
internal fun LanguageButton(
    view: CatalogView,
    language: String?,
    open: Boolean,
    height: Int = 42,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(12.dp)
    val chosen = view.languages.firstOrNull { it.code == language }
    val name = if (language == null) stringResource(Res.string.bibles_all_languages) else chosen?.name ?: language
    val count = chosen?.count ?: view.sourceTotal
    Row(
        Modifier
            .testTag(BiblesTags.LANGUAGE_BUTTON)
            .fillMaxWidth()
            .height(height.dp)
            .clip(shape)
            .background(colors.searchFill)
            .border(1.dp, if (open) colors.accent else colors.cardEdge, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.Language, contentDescription = null, tint = colors.muted, modifier = Modifier.size(16.dp))
        Text(
            stringResource(Res.string.bibles_language),
            color = if (colors.isDark) colors.muted else colors.dim,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            name,
            color = colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            grouped(count),
            color = if (colors.isDark) colors.muted else colors.dim,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Icon(
            if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(14.dp),
        )
    }
}

/** The language picker — design 2e, a tall sheet over the catalogue. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LanguageSheet(view: CatalogView, language: String?, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = LocalAppColors.current.sheetBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        LanguagePicker(
            view = view,
            language = language,
            onPick = { onPick(it); onDismiss() },
            onDone = onDismiss,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 20.dp).navigationBarsPadding(),
        )
    }
}

/**
 * The picker's content, shared by the phone's sheet and the tablet's dropdown (5a).
 *
 * @param onDone Null hides the header — the tablet dropdown has none.
 */
@Composable
internal fun LanguagePicker(
    view: CatalogView,
    language: String?,
    onPick: (String?) -> Unit,
    onDone: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    var filter by remember { mutableStateOf("") }
    val languages = view.languages.drop(1)
    val needle = filter.trim()
    val shown = languages.filter {
        needle.isEmpty() ||
            it.name.contains(needle, ignoreCase = true) ||
            it.code.orEmpty().contains(needle, ignoreCase = true)
    }
    Column(modifier.testTag(BiblesTags.LANGUAGE_SHEET)) {
        if (onDone != null) {
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.bibles_language),
                    color = colors.text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.025).em,
                )
                Text(
                    stringResource(Res.string.bibles_done),
                    color = colors.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag(BiblesTags.LANGUAGE_DONE).clickable(onClick = onDone),
                )
            }
        }
        CatalogSearch(
            filter,
            { filter = it },
            stringResource(Res.string.bibles_search_languages, languages.size),
            height = 38,
        )
        if (onDone != null && view.deviceLanguages.isNotEmpty() && filter.isBlank()) {
            Overline(
                stringResource(Res.string.bibles_on_this_device),
                Modifier.padding(start = 4.dp, top = 16.dp, bottom = 6.dp),
            )
            Row(Modifier.padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                view.deviceLanguages.forEach { option ->
                    val shape = RoundedCornerShape(18.dp)
                    Text(
                        option.name,
                        color = colors.secondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(shape)
                            .background(colors.cardFill)
                            .border(1.dp, colors.cardEdge, shape)
                            .clickable { onPick(option.code) }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
            Overline(
                stringResource(Res.string.bibles_all_languages),
                Modifier.padding(start = 4.dp, top = 14.dp, bottom = 4.dp),
            )
        } else {
            Box(Modifier.height(8.dp))
        }
        val state = rememberLazyListState()
        LazyColumn(state = state, modifier = Modifier.weight(1f, fill = false).verticalScrollbar(state)) {
            if (filter.isBlank()) {
                item(key = "all") {
                    LanguageRow(
                        LanguageOption(null, stringResource(Res.string.bibles_all_languages), view.sourceTotal),
                        language == null,
                    ) { onPick(null) }
                }
            }
            items(
                shown,
                key = { it.code.orEmpty() },
            ) { option -> LanguageRow(option, option.code == language) { onPick(option.code) } }
        }
    }
}

@Composable
private fun LanguageRow(option: LanguageOption, chosen: Boolean, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        Modifier
            .testTag(BiblesTags.language(option.code))
            .semantics { selected = chosen }
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(if (chosen) colors.chosenFill else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.width(16.dp)) {
            if (chosen) Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            option.name,
            color = if (chosen) colors.accent else colors.text,
            fontSize = 15.sp,
            fontWeight = if (chosen) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(grouped(option.count), color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
