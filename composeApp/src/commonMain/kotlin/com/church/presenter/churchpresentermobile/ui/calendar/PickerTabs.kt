package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.calendar_duration
import churchpresentermobile.composeapp.generated.resources.calendar_ministry_hint
import churchpresentermobile.composeapp.generated.resources.calendar_no_matches
import churchpresentermobile.composeapp.generated.resources.calendar_no_presets
import churchpresentermobile.composeapp.generated.resources.calendar_no_songs
import churchpresentermobile.composeapp.generated.resources.calendar_section_closing
import churchpresentermobile.composeapp.generated.resources.calendar_section_communion
import churchpresentermobile.composeapp.generated.resources.calendar_section_hint
import churchpresentermobile.composeapp.generated.resources.calendar_section_pre_service
import churchpresentermobile.composeapp.generated.resources.calendar_section_response
import churchpresentermobile.composeapp.generated.resources.calendar_section_word
import churchpresentermobile.composeapp.generated.resources.calendar_section_worship
import churchpresentermobile.composeapp.generated.resources.calendar_tap_add
import churchpresentermobile.composeapp.generated.resources.calendar_tap_verse_hint
import churchpresentermobile.composeapp.generated.resources.calendar_what_happens
import churchpresentermobile.composeapp.generated.resources.calendar_who_or_note
import com.church.presenter.churchpresentermobile.calendar.PickerBook
import com.church.presenter.churchpresentermobile.calendar.findBook
import com.church.presenter.churchpresentermobile.model.PresetSummary
import com.church.presenter.churchpresentermobile.model.RowKind
import com.church.presenter.churchpresentermobile.model.SectionPalette
import com.church.presenter.churchpresentermobile.model.Song
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val VERSE_COLUMNS = 7
private const val MAX_VERSES_SHOWN = 60

/** One card in a picker list: a kind badge, the title, a line under it, and a `+` on the right. */
@Composable
internal fun PickerCard(
    kind: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    CalendarCard(modifier = modifier, selected = selected, onClick = onSelect) {
        KindBadge(kind, size = 28.dp)
        Column(modifier = Modifier.weight(1f)) {
            TitleText(title, size = 14)
            if (subtitle.isNotEmpty()) MutedText(subtitle)
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.accentTint)
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = stringResource(Res.string.calendar_tap_add),
                tint = colors.accent,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** The song list as lazy items: a library can hold thousands, and composing them all at once ran out of memory. */
internal fun LazyListScope.songItems(
    songs: List<Song>,
    query: String,
    selected: Song?,
    onSelect: (Song) -> Unit,
    onAdd: (Song) -> Unit,
) {
    val filtered = filterSongs(songs, query)
    when {
        songs.isEmpty() -> item { HintText(stringResource(Res.string.calendar_no_songs)) }
        filtered.isEmpty() -> item { HintText(stringResource(Res.string.calendar_no_matches)) }
    }
    items(filtered, key = { it.identity }) { song ->
        PickerCard(
            kind = RowKind.SONG,
            title = songLabel(song),
            subtitle = song.bookName.orEmpty(),
            selected = selected?.identity == song.identity,
            onSelect = { onSelect(song) },
            onAdd = { onAdd(song) },
        )
    }
}

internal fun songLabel(song: Song): String {
    val title = song.secondaryTitle?.takeIf { it.isNotBlank() }?.let { "${song.title} · $it" } ?: song.title
    return if (song.number.isNotBlank() && song.number != "0") "${song.number} - $title" else title
}

internal fun filterSongs(songs: List<Song>, query: String): List<Song> {
    val needle = query.trim().lowercase()
    if (needle.isEmpty()) return songs
    return songs.filter { song ->
        song.title.lowercase().contains(needle) || song.number.lowercase().startsWith(needle) ||
            song.author.orEmpty().lowercase().contains(needle) ||
            song.secondaryTitle.orEmpty().lowercase().contains(needle)
    }
}

/** Book chips, then the chapter grid, then the verses of the picked chapter when they are known. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BibleTab(
    books: List<PickerBook>,
    query: String,
    book: PickerBook?,
    chapter: Int?,
    verseCount: Int?,
    verseFrom: Int?,
    verseTo: Int?,
    onBook: (PickerBook) -> Unit,
    onChapter: (Int) -> Unit,
    onVerse: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (book == null) {
            val shown = books.filter { query.isBlank() || findBook(query, listOf(it)) != null }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                shown.forEach { CalendarChip(it.name, selected = false, onClick = { onBook(it) }, dim = true) }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CalendarChip(book.name, selected = true, onClick = { onBook(book) })
                if (chapter != null) CalendarChip(chapter.toString(), selected = true, onClick = { onChapter(chapter) })
                Text(
                    text = stringResource(Res.string.calendar_tap_verse_hint),
                    color = colors.muted,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f).padding(start = 6.dp),
                    maxLines = 2,
                )
            }
            if (chapter == null) {
                NumberGrid(count = book.chapters, from = null, to = null, onTap = onChapter)
            } else if (verseCount != null) {
                NumberGrid(count = verseCount, from = verseFrom, to = verseTo, onTap = onVerse)
            } else {
                NumberGrid(count = MAX_VERSES_SHOWN, from = verseFrom, to = verseTo, onTap = onVerse)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NumberGrid(count: Int, from: Int?, to: Int?, onTap: (Int) -> Unit) {
    val colors = LocalAppColors.current
    FlowRow(
        maxItemsInEachRow = VERSE_COLUMNS,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        (1..count).forEach { n ->
            val inRange = from != null && n >= from && n <= (to ?: from)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (inRange) colors.accentTint else colors.surface)
                    .border(1.dp, if (inRange) colors.accent else colors.borderSubtle, RoundedCornerShape(9.dp))
                    .clickable { onTap(n) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    n.toString(),
                    color = if (inRange) colors.accent else colors.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private val SECTION_SUGGESTIONS: List<Pair<StringResource, String>> = listOf(
    Res.string.calendar_section_pre_service to SectionPalette.SKY,
    Res.string.calendar_section_worship to SectionPalette.BLUE,
    Res.string.calendar_section_word to SectionPalette.AMBER,
    Res.string.calendar_section_response to SectionPalette.GREEN,
    Res.string.calendar_section_communion to SectionPalette.VIOLET,
    Res.string.calendar_section_closing to SectionPalette.ROSE,
)

/** The usual section names, each with its color; the search field is the name for a new one. */
@Composable
internal fun SectionTab(
    query: String,
    color: String,
    onPick: (name: String, color: String) -> Unit,
    onColor: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SECTION_SUGGESTIONS.forEach { (nameRes, hex) ->
            val name = stringResource(nameRes)
            if (query.isBlank() || name.contains(query.trim(), ignoreCase = true)) {
                CalendarCard(selected = name.equals(query.trim(), ignoreCase = true), onClick = { onPick(name, hex) }) {
                    ColorDot(hex, size = 10.dp)
                    TitleText(name, size = 14, modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = LocalAppColors.current.accent,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        SwatchRow(color = color, onColor = onColor, modifier = Modifier.padding(top = 4.dp))
        HintText(stringResource(Res.string.calendar_section_hint))
    }
}

/** The section palette as round swatches, the chosen one ringed. */
@Composable
internal fun SwatchRow(color: String, onColor: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        SectionPalette.ALL.forEach { hex ->
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorOf(hex))
                    .border(
                        2.dp,
                        if (hex == color) LocalAppColors.current.text else colorOf(hex),
                        RoundedCornerShape(12.dp),
                    )
                    .clickable { onColor(hex) },
            )
        }
    }
}

/** What happens · who · how long, with the row read back as a card. */
@Composable
internal fun MinistryTab(
    what: String,
    who: String,
    duration: String,
    onWhat: (String) -> Unit,
    onWho: (String) -> Unit,
    onDuration: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CompactField(stringResource(Res.string.calendar_what_happens), what, onWhat, highlighted = true)
        CompactField(stringResource(Res.string.calendar_who_or_note), who, onWho)
        CompactField(
            stringResource(Res.string.calendar_duration),
            duration,
            onDuration,
            placeholder = "3:30",
            keyboardType = KeyboardType.Number,
        )
        if (what.isNotBlank()) {
            CalendarCard {
                KindBadge(RowKind.MINISTRY, size = 28.dp)
                Column(modifier = Modifier.weight(1f)) {
                    TitleText(what, size = 14)
                    MutedText(listOf(who, duration).filter { it.isNotBlank() }.joinToString(" · "))
                }
                Text(
                    text = stringResource(Res.string.calendar_tap_add).uppercase(),
                    color = LocalAppColors.current.accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        HintText(stringResource(Res.string.calendar_ministry_hint))
    }
}

internal fun LazyListScope.presetItems(
    presets: List<PresetSummary>,
    query: String,
    selected: PresetSummary?,
    onSelect: (PresetSummary) -> Unit,
    onAdd: (PresetSummary) -> Unit,
) {
    val filtered = presets.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
    when {
        presets.isEmpty() -> item { HintText(stringResource(Res.string.calendar_no_presets)) }
        filtered.isEmpty() -> item { HintText(stringResource(Res.string.calendar_no_matches)) }
    }
    items(filtered, key = { it.id }) { preset ->
        PickerCard(
            kind = preset.kind,
            title = preset.name,
            subtitle = preset.detail,
            selected = selected?.id == preset.id,
            onSelect = { onSelect(preset) },
            onAdd = { onAdd(preset) },
        )
    }
}

@Composable
internal fun HintText(text: String, modifier: Modifier = Modifier) {
    Text(text, color = LocalAppColors.current.muted, fontSize = 12.sp, modifier = modifier.padding(vertical = 4.dp))
}
