package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.calendar_add_named
import churchpresentermobile.composeapp.generated.resources.calendar_add_section
import churchpresentermobile.composeapp.generated.resources.calendar_add_to
import churchpresentermobile.composeapp.generated.resources.calendar_add_to_service
import churchpresentermobile.composeapp.generated.resources.calendar_filter_books_or_reference
import churchpresentermobile.composeapp.generated.resources.calendar_filter_presets
import churchpresentermobile.composeapp.generated.resources.calendar_search_songs_or_reference
import churchpresentermobile.composeapp.generated.resources.calendar_section_name_hint
import churchpresentermobile.composeapp.generated.resources.calendar_tab_bible
import churchpresentermobile.composeapp.generated.resources.calendar_tab_ministry
import churchpresentermobile.composeapp.generated.resources.calendar_tab_presets
import churchpresentermobile.composeapp.generated.resources.calendar_tab_section
import churchpresentermobile.composeapp.generated.resources.calendar_tab_songs
import com.church.presenter.churchpresentermobile.calendar.ParsedReference
import com.church.presenter.churchpresentermobile.calendar.PickerBook
import com.church.presenter.churchpresentermobile.calendar.parseDuration
import com.church.presenter.churchpresentermobile.calendar.parseReference
import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.PresetSummary
import com.church.presenter.churchpresentermobile.model.RowKind
import com.church.presenter.churchpresentermobile.model.RowTiming
import com.church.presenter.churchpresentermobile.model.SectionPalette
import com.church.presenter.churchpresentermobile.model.Song
import com.church.presenter.churchpresentermobile.model.SongDurations
import com.church.presenter.churchpresentermobile.ui.SearchField
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.ChapterPreview
import org.jetbrains.compose.resources.stringResource

private const val PREVIEW_CHARS = 40
private const val SHEET_HEIGHT_FRACTION = 0.92f

/** The five tabs of the picker. */
internal enum class PickerTab { SONGS, BIBLE, SECTION, MINISTRY, PRESETS }

/** Where the picker's lists come from; the sheet never reaches a ViewModel. */
internal class PickerSources(
    val songs: List<Song>,
    val books: List<PickerBook>,
    val presets: List<PresetSummary>,
    val chapterPreview: suspend (PickerBook, Int) -> ChapterPreview?,
    /** The desktop's measured lengths, so a song arrives in the plan with its usual length filled in. */
    val durations: SongDurations = SongDurations.NONE,
)

/** A row the picker has built, with its planned length, ready for [AddToServiceSheet]'s `onAdd`. */
internal class PickedRow(val row: PlanRow, val seconds: Int?)

/**
 * "Add to Sunday Morning": songs, passages, sections, ministry items and the desktop's presets,
 * each with the timing panel under it. Adding does not close the sheet — a planner adds a run
 * of things in one sitting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddToServiceSheet(
    serviceName: String,
    serviceStart: String,
    sources: PickerSources,
    newRowId: () -> String,
    onAdd: (PlanRow, Int?, RowTiming) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.sheetBackground,
    ) {
        AddToServiceContent(
            serviceName = serviceName,
            serviceStart = serviceStart,
            sources = sources,
            newRowId = newRowId,
            onAdd = onAdd,
            onDismiss = onDismiss,
            modifier = Modifier.fillMaxHeight(SHEET_HEIGHT_FRACTION),
        )
    }
}

@Composable
internal fun AddToServiceContent(
    serviceName: String,
    serviceStart: String,
    sources: PickerSources,
    newRowId: () -> String,
    onAdd: (PlanRow, Int?, RowTiming) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    var tab by remember { mutableStateOf(PickerTab.SONGS) }
    var query by remember { mutableStateOf("") }
    var timing by remember { mutableStateOf(TimingDraft()) }
    val picker = remember { PickerState() }
    val reference = remember(query, sources.books) { parseReference(query, sources.books) }

    LaunchedEffect(picker.book, picker.chapter) {
        val book = picker.book
        val chapter = picker.chapter
        picker.preview = if (book != null && chapter != null) sources.chapterPreview(book, chapter) else null
    }

    fun add(picked: PickedRow?) {
        val row = picked ?: return
        onAdd(row.row, row.seconds ?: timing.runSeconds, timing.toTiming(serviceStart))
        picker.clearSelection()
        if (tab == PickerTab.SECTION || tab == PickerTab.MINISTRY) query = ""
    }

    val pending = picker.pendingRow(tab, query, reference, newRowId, sources.durations)

    Column(modifier = modifier.padding(horizontal = PagePadding)) {
        SheetTitle(stringResource(Res.string.calendar_add_to, serviceName), onClose = onDismiss)
        Spacer(Modifier.height(12.dp))
        SearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(
                when (tab) {
                    PickerTab.BIBLE -> Res.string.calendar_filter_books_or_reference
                    PickerTab.SECTION -> Res.string.calendar_section_name_hint
                    PickerTab.PRESETS -> Res.string.calendar_filter_presets
                    else -> Res.string.calendar_search_songs_or_reference
                },
            ),
        )
        Spacer(Modifier.height(10.dp))
        PickerTabRow(tab, onTab = { tab = it; query = ""; picker.clearSelection() })
        Spacer(Modifier.height(10.dp))
        // The timing panel scrolls with the list rather than sitting under it: with the keyboard
        // up, a fixed panel left the list a few pixels tall. Only the Add button stays pinned.
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (reference != null && tab != PickerTab.BIBLE) {
                item { ReferenceCard(reference, onAdd = { add(picker.bibleRow(reference, newRowId)) }) }
            }
            pickerBody(
                tab = tab,
                query = query,
                sources = sources,
                picker = picker,
                onAddSong = { add(picker.songRow(it, newRowId, sources.durations.secondsFor(it))) },
                onAddPreset = { add(picker.presetRow(it, newRowId)) },
                onPickSection = { name, hex ->
                    add(PickedRow(PlanRow.Section(id = newRowId(), title = name, color = hex), seconds = null))
                },
            )
            if (tab != PickerTab.SECTION) {
                item {
                    Column {
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider(color = colors.borderSubtle)
                        Spacer(Modifier.height(10.dp))
                        TimingPanel(draft = timing, onChange = { timing = it }, serviceStart = serviceStart)
                    }
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
        }
        Spacer(Modifier.height(10.dp))
        CalendarPrimaryButton(
            label = addLabel(tab, pending),
            onClick = { add(pending) },
            enabled = pending != null,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun addLabel(tab: PickerTab, pending: PickedRow?): String = when {
    tab == PickerTab.SECTION -> stringResource(Res.string.calendar_add_section)
    pending is PickedRow && tab == PickerTab.MINISTRY -> stringResource(Res.string.calendar_add_named, pending.row.title)
    else -> stringResource(Res.string.calendar_add_to_service)
}

@Composable
private fun PickerTabRow(tab: PickerTab, onTab: (PickerTab) -> Unit) {
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PickerTab.entries.forEach { entry ->
            val label = stringResource(
                when (entry) {
                    PickerTab.SONGS -> Res.string.calendar_tab_songs
                    PickerTab.BIBLE -> Res.string.calendar_tab_bible
                    PickerTab.SECTION -> Res.string.calendar_tab_section
                    PickerTab.MINISTRY -> Res.string.calendar_tab_ministry
                    PickerTab.PRESETS -> Res.string.calendar_tab_presets
                },
            )
            CalendarChip(label, selected = entry == tab, onClick = { onTab(entry) })
        }
    }
}

private fun LazyListScope.pickerBody(
    tab: PickerTab,
    query: String,
    sources: PickerSources,
    picker: PickerState,
    onAddSong: (Song) -> Unit,
    onAddPreset: (PresetSummary) -> Unit,
    onPickSection: (String, String) -> Unit,
) {
    when (tab) {
        PickerTab.SONGS -> songItems(
            songs = sources.songs,
            query = query,
            selected = picker.song,
            onSelect = { picker.song = it },
            onAdd = onAddSong,
        )
        PickerTab.BIBLE -> item {
            BibleTab(
                books = sources.books,
                query = query,
                book = picker.book,
                chapter = picker.chapter,
                verseCount = picker.preview?.verseCount,
                verseFrom = picker.verseFrom,
                verseTo = picker.verseTo,
                onBook = { picker.pickBook(it) },
                onChapter = { picker.pickChapter(it) },
                onVerse = { picker.pickVerse(it) },
            )
        }
        PickerTab.SECTION -> item {
            SectionTab(
                query = query,
                color = picker.sectionColor,
                onPick = onPickSection,
                onColor = { picker.sectionColor = it },
            )
        }
        PickerTab.MINISTRY -> item {
            MinistryTab(
                what = picker.ministryWhat,
                who = picker.ministryWho,
                duration = picker.ministryDuration,
                onWhat = { picker.ministryWhat = it },
                onWho = { picker.ministryWho = it },
                onDuration = { picker.ministryDuration = it },
            )
        }
        PickerTab.PRESETS -> presetItems(
            presets = sources.presets,
            query = query,
            selected = picker.preset,
            onSelect = { picker.preset = it },
            onAdd = onAddPreset,
        )
    }
}

@Composable
private fun ReferenceCard(reference: ParsedReference, onAdd: () -> Unit) {
    PickerCard(kind = RowKind.BIBLE, title = reference.text, subtitle = "", selected = true, onSelect = onAdd, onAdd = onAdd)
}

/** Everything the picker has half-chosen: the lit song, the book/chapter/verses, the fields typed. */
internal class PickerState {
    var song: Song? by mutableStateOf(null)
    var preset: PresetSummary? by mutableStateOf(null)
    var book: PickerBook? by mutableStateOf(null)
    var chapter: Int? by mutableStateOf(null)
    var verseFrom: Int? by mutableStateOf(null)
    var verseTo: Int? by mutableStateOf(null)
    var preview: ChapterPreview? by mutableStateOf(null)
    var sectionColor: String by mutableStateOf(SectionPalette.DEFAULT)
    var ministryWhat: String by mutableStateOf("")
    var ministryWho: String by mutableStateOf("")
    var ministryDuration: String by mutableStateOf("")

    fun pickBook(picked: PickerBook) {
        book = if (book == picked) null else picked
        chapter = null
        verseFrom = null
        verseTo = null
    }

    fun pickChapter(picked: Int) {
        chapter = if (chapter == picked) null else picked
        verseFrom = null
        verseTo = null
    }

    /** First tap picks a verse, second tap after it makes a range, any other tap starts over. */
    fun pickVerse(verse: Int) {
        val from = verseFrom
        when {
            from == null || verseTo != null -> { verseFrom = verse; verseTo = null }
            verse > from -> verseTo = verse
            else -> { verseFrom = verse; verseTo = null }
        }
    }

    fun clearSelection() {
        song = null
        preset = null
        verseFrom = null
        verseTo = null
        ministryWhat = ""
        ministryWho = ""
        ministryDuration = ""
    }

    fun songRow(picked: Song, newId: () -> String, seconds: Int?) = PickedRow(
        PlanRow.Song(
            id = newId(),
            title = songLabel(picked),
            songId = picked.desktopSongId,
            songbook = picked.bookName.orEmpty(),
            number = picked.number,
        ),
        seconds = seconds,
    )

    fun presetRow(picked: PresetSummary, newId: () -> String) =
        PickedRow(PlanRow.Preset(id = newId(), title = picked.name, presetId = picked.id, kind = picked.kind), seconds = null)

    fun bibleRow(reference: ParsedReference, newId: () -> String) =
        PickedRow(PlanRow.Bible(id = newId(), title = reference.text, bookId = reference.book.number), seconds = null)

    /** What the primary button would add right now, or null when nothing is ready. */
    fun pendingRow(
        tab: PickerTab,
        query: String,
        reference: ParsedReference?,
        newId: () -> String,
        durations: SongDurations = SongDurations.NONE,
    ): PickedRow? = when (tab) {
        PickerTab.SONGS -> song?.let { songRow(it, newId, durations.secondsFor(it)) } ?: reference?.let { bibleRow(it, newId) }
        PickerTab.PRESETS -> preset?.let { presetRow(it, newId) }
        PickerTab.BIBLE -> pendingPassage(reference, newId)
        PickerTab.SECTION -> query.trim().takeIf { it.isNotEmpty() }?.let {
            PickedRow(PlanRow.Section(id = newId(), title = it, color = sectionColor), seconds = null)
        }
        PickerTab.MINISTRY -> ministryWhat.trim().takeIf { it.isNotEmpty() }?.let {
            PickedRow(
                PlanRow.Ministry(id = newId(), title = it, detail = ministryWho.trim()),
                seconds = parseDuration(ministryDuration),
            )
        }
    }

    private fun pendingPassage(typed: ParsedReference?, newId: () -> String): PickedRow? {
        val pickedBook = book
        val pickedChapter = chapter
        if (pickedBook != null && pickedChapter != null) {
            val reference = ParsedReference(pickedBook, pickedChapter, verseFrom, verseTo)
            val firstWords = verseFrom?.let { preview?.firstWords?.get(it) }.orEmpty().take(PREVIEW_CHARS)
            return PickedRow(
                PlanRow.Bible(id = newId(), title = reference.text, preview = firstWords, bookId = pickedBook.number),
                seconds = null,
            )
        }
        return typed?.let { bibleRow(it, newId) }
    }
}
