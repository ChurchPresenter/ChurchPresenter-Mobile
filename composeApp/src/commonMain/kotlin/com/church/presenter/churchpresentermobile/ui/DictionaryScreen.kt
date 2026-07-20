package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.action_go_live
import churchpresentermobile.composeapp.generated.resources.dictionary_appears_in
import churchpresentermobile.composeapp.generated.resources.dictionary_appears_in_count
import churchpresentermobile.composeapp.generated.resources.dictionary_definition
import churchpresentermobile.composeapp.generated.resources.dictionary_filter_all
import churchpresentermobile.composeapp.generated.resources.dictionary_filter_greek
import churchpresentermobile.composeapp.generated.resources.dictionary_filter_hebrew
import churchpresentermobile.composeapp.generated.resources.dictionary_kjv_usage
import churchpresentermobile.composeapp.generated.resources.dictionary_no_entries
import churchpresentermobile.composeapp.generated.resources.dictionary_occurrences
import churchpresentermobile.composeapp.generated.resources.dictionary_occurrences_count
import churchpresentermobile.composeapp.generated.resources.dictionary_ref_book
import churchpresentermobile.composeapp.generated.resources.dictionary_ref_chapter
import churchpresentermobile.composeapp.generated.resources.dictionary_ref_clear
import churchpresentermobile.composeapp.generated.resources.dictionary_ref_verse
import churchpresentermobile.composeapp.generated.resources.dictionary_search_placeholder
import churchpresentermobile.composeapp.generated.resources.dictionary_uses
import churchpresentermobile.composeapp.generated.resources.label_add_to_schedule
import org.jetbrains.compose.resources.stringResource
import com.church.presenter.churchpresentermobile.model.BibleBook
import com.church.presenter.churchpresentermobile.model.DictionaryVersesResponse
import com.church.presenter.churchpresentermobile.model.StrongsEntry
import com.church.presenter.churchpresentermobile.network.DictionaryFilter
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.DictionaryViewModel

private val strongsRefRegex = Regex("[HG]\\d{1,5}")

/** Groups an integer with commas: 2606 → "2,606". */
private fun Int.grouped(): String {
    val s = toString()
    val sb = StringBuilder()
    for ((i, c) in s.withIndex()) {
        if (i > 0 && (s.length - i) % 3 == 0) sb.append(',')
        sb.append(c)
    }
    return sb.toString()
}

/**
 * Strong's dictionary index (design screens 11a/b) + entry sheet (10a).
 * Search + Hebrew/Greek filter, entry rows with occurrence counts, and a detail
 * bottom sheet with occurrences / root / definition (tappable links) / KJV usage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(
    viewModel: DictionaryViewModel,
    modifier: Modifier = Modifier,
    settingsSaveToken: Int = 0,
) {
    val colors = LocalAppColors.current
    val query by viewModel.searchQuery.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val books by viewModel.books.collectAsState()
    val refBook by viewModel.refBook.collectAsState()
    val refChapter by viewModel.refChapter.collectAsState()
    val refVerse by viewModel.refVerse.collectAsState()
    val refVerseCount by viewModel.refVerseCount.collectAsState()
    val entries by viewModel.entries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedEntry by viewModel.selectedEntry.collectAsState()
    val appearsIn by viewModel.appearsIn.collectAsState()
    val appearsInLoading by viewModel.appearsInLoading.collectAsState()
    val projectedNumber by viewModel.projectedNumber.collectAsState()
    val scheduleAdded by viewModel.scheduleAdded.collectAsState()
    val actionError by viewModel.actionError.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(settingsSaveToken) { if (settingsSaveToken > 0) viewModel.onSettingsSaved() }
    LaunchedEffect(actionError) {
        if (actionError != null) {
            snackbarHostState.showSnackbar(actionError!!, duration = SnackbarDuration.Short)
            viewModel.clearActionError()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.fillMaxSize()) {
            SearchField(
                value = query,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = stringResource(Res.string.dictionary_search_placeholder),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            SegmentedControl(
                options = listOf(
                    stringResource(Res.string.dictionary_filter_all),
                    stringResource(Res.string.dictionary_filter_hebrew),
                    stringResource(Res.string.dictionary_filter_greek),
                ),
                selectedIndex = when (filter) {
                    DictionaryFilter.ALL -> 0
                    DictionaryFilter.HEBREW -> 1
                    DictionaryFilter.GREEK -> 2
                },
                onSelect = {
                    viewModel.setFilter(
                        when (it) {
                            1 -> DictionaryFilter.HEBREW
                            2 -> DictionaryFilter.GREEK
                            else -> DictionaryFilter.ALL
                        }
                    )
                },
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            )
            ReferenceFilterRow(
                books = books,
                selectedBook = refBook,
                selectedChapter = refChapter,
                selectedVerse = refVerse,
                verseCount = refVerseCount,
                onBookSelected = { viewModel.setRefBook(it) },
                onChapterSelected = { viewModel.setRefChapter(it) },
                onVerseSelected = { viewModel.setRefVerse(it) },
                onClear = { viewModel.clearReference() },
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            )

            when {
                error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(error!!, color = colors.danger, fontSize = 14.sp)
                }
                isLoading && entries.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = colors.accent)
                }
                entries.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(stringResource(Res.string.dictionary_no_entries), color = colors.muted, fontSize = 15.sp)
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    items(entries, key = { it.number }) { entry ->
                        EntryRow(
                            entry = entry,
                            isLive = projectedNumber == entry.number,
                            onClick = { viewModel.selectEntry(entry) },
                        )
                    }
                }
            }
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (selectedEntry != null) {
        EntryDetailSheet(
            entry = selectedEntry!!,
            isLive = projectedNumber == selectedEntry!!.number,
            scheduleAdded = scheduleAdded,
            appearsIn = appearsIn,
            appearsInLoading = appearsInLoading,
            onProject = { viewModel.projectSelected() },
            onAddToSchedule = { viewModel.addSelectedToSchedule() },
            onOpenNumber = { viewModel.selectByNumber(it) },
            onDismiss = { viewModel.clearSelection() },
        )
    }
}

/**
 * Cascading Book → Chapter → Verse filter shown under the search bar.
 *
 * The chapter dropdown only appears once a book is chosen, and the verse dropdown
 * only once a chapter is chosen (and its verse count is known). A clear button
 * resets the whole reference filter. Hidden entirely until the book list loads.
 */
@Composable
private fun ReferenceFilterRow(
    books: List<BibleBook>,
    selectedBook: BibleBook?,
    selectedChapter: Int?,
    selectedVerse: Int?,
    verseCount: Int,
    onBookSelected: (BibleBook?) -> Unit,
    onChapterSelected: (Int?) -> Unit,
    onVerseSelected: (Int?) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (books.isEmpty()) return
    val colors = LocalAppColors.current
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RefDropdown(
            placeholder = stringResource(Res.string.dictionary_ref_book),
            selectedLabel = selectedBook?.displayName,
            options = books.map { it.displayName },
            onSelect = { onBookSelected(books[it]) },
        )
        if (selectedBook != null) {
            val chapters = (1..selectedBook.totalChapters).toList()
            RefDropdown(
                placeholder = stringResource(Res.string.dictionary_ref_chapter),
                selectedLabel = selectedChapter?.toString(),
                options = chapters.map { it.toString() },
                onSelect = { onChapterSelected(chapters[it]) },
            )
        }
        if (selectedBook != null && selectedChapter != null && verseCount > 0) {
            Text(
                text = ":",
                color = colors.muted,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            val verses = (1..verseCount).toList()
            RefDropdown(
                placeholder = stringResource(Res.string.dictionary_ref_verse),
                selectedLabel = selectedVerse?.toString(),
                options = verses.map { it.toString() },
                onSelect = { onVerseSelected(verses[it]) },
            )
        }
        if (selectedBook != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onClear() }
                    .padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.dictionary_ref_clear),
                    tint = colors.muted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** A single pill-style dropdown used by [ReferenceFilterRow]. [onSelect] receives the chosen index. */
@Composable
private fun RefDropdown(
    placeholder: String,
    selectedLabel: String?,
    options: List<String>,
    onSelect: (Int) -> Unit,
) {
    val colors = LocalAppColors.current
    var expanded by remember { mutableStateOf(false) }
    val active = selectedLabel != null
    val shape = RoundedCornerShape(10.dp)
    Box {
        Row(
            modifier = Modifier
                .clip(shape)
                .background(if (active) colors.accentTint else colors.surface)
                .border(1.dp, if (active) colors.accent else colors.borderSubtle, shape)
                .clickable { expanded = true }
                .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = selectedLabel ?: placeholder,
                color = if (active) colors.accent else colors.muted,
                fontSize = 13.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = if (active) colors.accent else colors.muted,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { idx, opt ->
                DropdownMenuItem(
                    text = {
                        Text(
                            opt,
                            fontWeight = if (opt == selectedLabel) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = { onSelect(idx); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun NumberPill(number: String, solid: Boolean = false) {
    val colors = LocalAppColors.current
    // Greek numbers (G####) use a blue accent; Hebrew (H####) keeps the green accent.
    val isGreek = number.startsWith("G")
    val fg = if (isGreek) colors.greekAccent else colors.accent
    val tint = if (isGreek) colors.greekAccentTint else colors.accentTint
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (solid) fg else tint)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number,
            color = if (solid) colors.onAccent else fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun EntryRow(entry: StrongsEntry, isLive: Boolean, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isLive) colors.accentTint else colors.surface)
            .border(1.dp, if (isLive) colors.accent else colors.borderSubtle, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NumberPill(entry.number)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = entry.word,
                    color = colors.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry.transliteration.isNotBlank()) {
                    Text(
                        text = entry.transliteration,
                        color = colors.muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (entry.definition.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = entry.definition,
                    color = colors.muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (entry.occurrences > 0) {
            Column(horizontalAlignment = Alignment.End) {
                Text(entry.occurrences.grouped(), color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(stringResource(Res.string.dictionary_uses), color = colors.dim, fontSize = 9.sp, letterSpacing = 0.05.em)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryDetailSheet(
    entry: StrongsEntry,
    isLive: Boolean,
    scheduleAdded: Boolean,
    appearsIn: DictionaryVersesResponse?,
    appearsInLoading: Boolean,
    onProject: () -> Unit,
    onAddToSchedule: () -> Unit,
    onOpenNumber: (String) -> Unit,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            // Header: solid number pill + language + actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                NumberPill(entry.number, solid = true)
                Spacer(Modifier.size(10.dp))
                Text(
                    text = if (entry.isHebrew) stringResource(Res.string.dictionary_filter_hebrew) else if (entry.isGreek) stringResource(Res.string.dictionary_filter_greek) else "",
                    color = colors.muted,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.weight(1f))
                IconTileButton(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = stringResource(Res.string.label_add_to_schedule),
                    tint = if (scheduleAdded) colors.accent else colors.amber,
                    onClick = onAddToSchedule,
                )
                Spacer(Modifier.size(8.dp))
                IconTileButton(
                    icon = Icons.Outlined.DesktopWindows,
                    contentDescription = stringResource(Res.string.action_go_live),
                    tint = colors.accent,
                    onClick = onProject,
                )
            }

            Spacer(Modifier.height(16.dp))
            // Centered word / transliteration / pronunciation
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(entry.word, color = colors.text, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                if (entry.transliteration.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(entry.transliteration, color = colors.secondary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                if (entry.pronunciation.isNotBlank()) {
                    Text(entry.pronunciation, color = colors.muted, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(18.dp))
            // Occurrences card. Part of Speech and Root were removed — neither is
            // available in the Strong's data set (Part of Speech has no source; Root
            // was only a heuristic extracted from the definition text).
            MetaCard(label = stringResource(Res.string.dictionary_occurrences), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.dictionary_occurrences_count, entry.occurrences.grouped()), color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            if (entry.definition.isNotBlank()) {
                SectionLabel(stringResource(Res.string.dictionary_definition))
                DefinitionText(entry.definition, entry.number, onOpenNumber)
            }
            if (entry.kjvUsage.isNotBlank()) {
                SectionLabel(stringResource(Res.string.dictionary_kjv_usage))
                Text(entry.kjvUsage, color = colors.secondary, fontSize = 14.sp, lineHeight = (14 * 1.6).sp)
            }

            // Appears in — verses where this Strong's number occurs. The active
            // reference filter (if any) orders its scope first (see ViewModel).
            val verses = appearsIn?.verses.orEmpty()
            if (appearsInLoading && verses.isEmpty()) {
                SectionLabel(stringResource(Res.string.dictionary_appears_in))
                Spacer(Modifier.height(4.dp))
                CircularProgressIndicator(color = colors.accent, modifier = Modifier.size(20.dp))
            } else if (verses.isNotEmpty()) {
                val shown = verses.size
                val total = appearsIn?.total ?: shown
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.dictionary_appears_in).uppercase(),
                        color = colors.muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.05.em,
                    )
                    if (total > shown) {
                        Spacer(Modifier.weight(1f))
                        Text(
                            stringResource(
                                Res.string.dictionary_appears_in_count,
                                shown.grouped(), total.grouped()
                            ),
                            color = colors.muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                verses.forEach { v ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(
                            v.reference,
                            color = colors.accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (v.text.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                v.text,
                                color = colors.secondary,
                                fontSize = 13.sp,
                                lineHeight = (13 * 1.5).sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaCard(label: String, modifier: Modifier = Modifier, value: @Composable () -> Unit) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.inputBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label.uppercase(), color = colors.muted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.05.em)
        Spacer(Modifier.height(4.dp))
        value()
    }
}

@Composable
private fun SectionLabel(label: String) {
    val colors = LocalAppColors.current
    Spacer(Modifier.height(18.dp))
    Text(label.uppercase(), color = colors.muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.05.em)
    Spacer(Modifier.height(6.dp))
}

/** Definition text with tappable H####/G#### Strong's links (excluding the entry's own number). */
@Composable
private fun DefinitionText(definition: String, ownNumber: String, onOpenNumber: (String) -> Unit) {
    val colors = LocalAppColors.current
    val annotated = buildAnnotatedString {
        var last = 0
        for (m in strongsRefRegex.findAll(definition)) {
            append(definition.substring(last, m.range.first))
            val ref = m.value
            if (ref == ownNumber) {
                append(ref)
            } else {
                withLink(LinkAnnotation.Clickable(tag = ref, linkInteractionListener = { onOpenNumber(ref) })) {
                    withStyle(SpanStyle(color = colors.accent, fontWeight = FontWeight.Medium)) { append(ref) }
                }
            }
            last = m.range.last + 1
        }
        append(definition.substring(last))
    }
    Text(annotated, color = colors.secondary, fontSize = 14.sp, lineHeight = (14 * 1.6).sp)
}
