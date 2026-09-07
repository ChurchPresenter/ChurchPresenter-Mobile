package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.library_add
import churchpresentermobile.composeapp.generated.resources.library_bible_menu_title
import churchpresentermobile.composeapp.generated.resources.library_bible_none
import churchpresentermobile.composeapp.generated.resources.library_delete
import churchpresentermobile.composeapp.generated.resources.library_delete_confirm_body
import churchpresentermobile.composeapp.generated.resources.library_delete_confirm_title
import churchpresentermobile.composeapp.generated.resources.library_empty_body
import churchpresentermobile.composeapp.generated.resources.library_empty_title
import churchpresentermobile.composeapp.generated.resources.library_filter_all
import churchpresentermobile.composeapp.generated.resources.library_filter_notices
import churchpresentermobile.composeapp.generated.resources.library_filter_songs
import churchpresentermobile.composeapp.generated.resources.library_new_notice
import churchpresentermobile.composeapp.generated.resources.library_new_song
import churchpresentermobile.composeapp.generated.resources.library_no_results
import churchpresentermobile.composeapp.generated.resources.library_origin_desktop
import churchpresentermobile.composeapp.generated.resources.library_origin_edited
import churchpresentermobile.composeapp.generated.resources.library_search_placeholder
import churchpresentermobile.composeapp.generated.resources.library_section_notices
import churchpresentermobile.composeapp.generated.resources.library_section_songs
import churchpresentermobile.composeapp.generated.resources.editor_cancel
import churchpresentermobile.composeapp.generated.resources.editor_song_edit_label
import churchpresentermobile.composeapp.generated.resources.sync_action
import churchpresentermobile.composeapp.generated.resources.sync_days_ago
import churchpresentermobile.composeapp.generated.resources.sync_section_bible
import churchpresentermobile.composeapp.generated.resources.sync_hours_ago
import churchpresentermobile.composeapp.generated.resources.sync_just_now
import churchpresentermobile.composeapp.generated.resources.sync_minutes_ago
import churchpresentermobile.composeapp.generated.resources.sync_never
import churchpresentermobile.composeapp.generated.resources.share_title
import com.church.presenter.churchpresentermobile.SyncRequestHandler
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.viewmodel.BibleChoiceViewModel
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.LibrarySyncState
import com.church.presenter.churchpresentermobile.network.WsSender
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import com.church.presenter.churchpresentermobile.model.ContentOrigin
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.ui.OverlineRow
import com.church.presenter.churchpresentermobile.ui.SearchField
import com.church.presenter.churchpresentermobile.ui.SegmentedControl
import com.church.presenter.churchpresentermobile.ui.theme.AppDimens
import com.church.presenter.churchpresentermobile.ui.EmptyState
import churchpresentermobile.composeapp.generated.resources.empty_action_get_content
import churchpresentermobile.composeapp.generated.resources.empty_action_write_song
import churchpresentermobile.composeapp.generated.resources.library_empty_body_standalone
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.LibraryFilter
import com.church.presenter.churchpresentermobile.viewmodel.LibraryViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Browses the content that lives on this device.
 *
 * Tapping an item loads it into the presenter; the pencil opens the editor.
 * Items carry an origin badge so the operator can tell at a glance what came
 * from the computer and what is theirs — which is the same distinction that
 * decides what a re-sync is allowed to overwrite.
 */
@Composable
fun LibraryScreen(
    repository: LibraryRepository,
    bibles: LocalBibleRepository,
    settings: AppSettings,
    sender: WsSender,
    onEditSong: (String?) -> Unit,
    onEditAnnouncement: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: LibraryViewModel = viewModel(key = "library") {
        LibraryViewModel(repository)
    }
    val colors = LocalAppColors.current

    val library by viewModel.library.collectAsState()
    val query by viewModel.query.collectAsState()
    val filter by viewModel.filter.collectAsState()

    // Recomputed on every change to the query, the filter, or the library itself.
    val songs = viewModel.visibleSongs()
    val announcements = viewModel.visibleAnnouncements()

    var pendingDelete by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var showAddChoices by remember { mutableStateOf(false) }
    var showSync by remember { mutableStateOf(false) }
    var syncSection by remember { mutableStateOf(SyncSection.SONGS) }
    var showShare by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    if (showShare) {
        ShareSheet(
            repository = repository,
            onDismiss = { showShare = false },
            onMessage = { message = it },
        )
    }

    // An empty state on another tab asked for this — open on the half it asked for.
    val requestedSection by SyncRequestHandler.requested.collectAsState()
    LaunchedEffect(requestedSection) {
        requestedSection?.let {
            syncSection = it
            showSync = true
            SyncRequestHandler.consume()
        }
    }

    if (showSync) {
        SyncSheet(
            repository = repository,
            bibles = bibles,
            settings = settings,
            sender = sender,
            initialSection = syncSection,
            onDismiss = { showSync = false },
        )
    }

    pendingDelete?.let { (id, isSong) ->
        DeleteConfirmDialog(
            onConfirm = {
                if (isSong) viewModel.deleteSong(id) else viewModel.deleteAnnouncement(id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = AppDimens.space16),
            verticalArrangement = Arrangement.spacedBy(AppDimens.space12),
        ) {
            SearchField(
                value = query,
                onValueChange = viewModel::setQuery,
                placeholder = stringResource(Res.string.library_search_placeholder),
                modifier = Modifier.testTag(LibraryTags.SEARCH),
            )

            Row(
                // Three chips overflow a narrow phone, and the Bible one carries
                // a translation's full name.
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.space8),
            ) {
                SyncChip(
                    settings = settings,
                    onClick = { showSync = true },
                    modifier = Modifier.testTag(LibraryTags.SYNC_CHIP),
                )
                BibleChip(
                    bibles = bibles,
                    onCopyBible = { syncSection = SyncSection.BIBLE; showSync = true },
                    modifier = Modifier.testTag(LibraryTags.BIBLE_CHIP),
                )
                ShareChip(
                    onClick = { showShare = true },
                    modifier = Modifier.testTag(LibraryTags.SHARE_CHIP),
                )
            }
            message?.let { text ->
                Text(
                    text,
                    color = colors.danger,
                    fontSize = 11.sp,
                    modifier = Modifier.testTag(LibraryTags.MESSAGE),
                )
            }

            SegmentedControl(
                options = listOf(
                    stringResource(Res.string.library_filter_all),
                    stringResource(Res.string.library_filter_songs),
                    stringResource(Res.string.library_filter_notices),
                ),
                selectedIndex = FILTERS.indexOf(filter).coerceAtLeast(0),
                onSelect = { viewModel.setFilter(FILTERS[it]) },
                optionTag = { LibraryTags.filter(it) },
            )

            when {
                library.isEmpty -> EmptyLibraryHint(
                    onCopyFromComputer = { syncSection = SyncSection.SONGS; showSync = true },
                    onWriteSong = { onEditSong(null) },
                    modifier = Modifier.testTag(LibraryTags.EMPTY_LIBRARY),
                )
                songs.isEmpty() && announcements.isEmpty() ->
                    NoResultsHint(Modifier.testTag(LibraryTags.NO_RESULTS))
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.space8),
                ) {
                    if (songs.isNotEmpty()) {
                        item {
                            OverlineRow(
                                stringResource(Res.string.library_section_songs),
                                "${songs.size}",
                                modifier = Modifier.testTag(LibraryTags.SONGS_HEADING),
                            )
                        }
                        items(songs, key = { it.id }) { song ->
                            SongRow(
                                song = song,
                                onEdit = { onEditSong(song.id) },
                                onDelete = { pendingDelete = song.id to true },
                            )
                        }
                    }
                    if (announcements.isNotEmpty()) {
                        item {
                            OverlineRow(
                                stringResource(Res.string.library_section_notices),
                                "${announcements.size}",
                                modifier = Modifier.testTag(LibraryTags.NOTICES_HEADING),
                            )
                        }
                        items(announcements, key = { it.id }) { item ->
                            AnnouncementRow(
                                announcement = item,
                                onEdit = { onEditAnnouncement(item.id) },
                                onDelete = { pendingDelete = item.id to false },
                            )
                        }
                    }
                    // Clears the FAB so the last row is reachable.
                    item { Box(Modifier.size(72.dp)) }
                }
            }
        }

        if (showAddChoices) {
            AddChoiceDialog(
                onSong = { showAddChoices = false; onEditSong(null) },
                onAnnouncement = { showAddChoices = false; onEditAnnouncement(null) },
                onDismiss = { showAddChoices = false },
            )
        }

        ExtendedFloatingActionButton(
            onClick = { showAddChoices = true },
            containerColor = colors.accent,
            contentColor = colors.onAccent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(AppDimens.space16)
                .testTag(LibraryTags.ADD),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = stringResource(Res.string.library_add),
                modifier = Modifier.padding(start = 8.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SongRow(
    song: LocalSong,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    LibraryRow(
        id = song.id,
        icon = { tint -> Icon(Icons.Filled.MusicNote, null, tint = tint, modifier = Modifier.size(18.dp)) },
        title = song.displayTitle,
        subtitle = listOfNotNull(
            song.author,
            song.bookName,
            "${song.sections.size} sections",
        ).joinToString(" · "),
        origin = song.origin,
        onEdit = onEdit,
        onDelete = onDelete,
    )
}

@Composable
private fun AnnouncementRow(
    announcement: LocalAnnouncement,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    LibraryRow(
        id = announcement.id,
        icon = { tint -> Icon(Icons.Filled.Campaign, null, tint = tint, modifier = Modifier.size(18.dp)) },
        title = announcement.title.ifBlank { announcement.body.lineSequence().first() },
        subtitle = announcement.body.lineSequence().firstOrNull().orEmpty(),
        origin = announcement.origin,
        onEdit = onEdit,
        onDelete = onDelete,
    )
}

@Composable
private fun LibraryRow(
    id: String,
    icon: @Composable (Color) -> Unit,
    title: String,
    subtitle: String,
    origin: ContentOrigin,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(LibraryTags.row(id))
            .clip(RoundedCornerShape(AppDimens.radiusCard))
            .background(colors.surface)
            // Deliberately not clickable. The Library is for writing and keeping
            // content; a tap here used to put the item straight on the audience
            // screen, which meant browsing your own library projected it. Going
            // live belongs to the presenting surfaces — the Songs tab for songs,
            // the Notices screen for notices.
            .padding(AppDimens.space14),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space12),
    ) {
        icon(colors.muted)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OriginBadge(origin, Modifier.testTag(LibraryTags.rowOrigin(id)))
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = colors.muted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        RowAction(
            label = stringResource(Res.string.library_delete),
            color = colors.danger,
            onClick = onDelete,
            modifier = Modifier.testTag(LibraryTags.rowDelete(id)),
        )
        RowAction(
            label = stringResource(Res.string.editor_song_edit_label),
            color = colors.accent,
            onClick = onEdit,
            modifier = Modifier.testTag(LibraryTags.rowEdit(id)),
        )
    }
}

@Composable
private fun RowAction(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(AppDimens.radiusChip))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

/**
 * Shows where an item came from — the same distinction that decides whether a
 * later desktop sync may replace it.
 */
@Composable
private fun OriginBadge(origin: ContentOrigin, modifier: Modifier = Modifier) {
    if (origin == ContentOrigin.LOCAL) return
    val colors = LocalAppColors.current
    val (label, tint) = when (origin) {
        ContentOrigin.DESKTOP -> stringResource(Res.string.library_origin_desktop) to colors.muted
        ContentOrigin.LOCAL_OVERRIDE -> stringResource(Res.string.library_origin_edited) to colors.amber
        ContentOrigin.LOCAL -> return
    }
    Text(
        text = label,
        color = tint,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clip(RoundedCornerShape(AppDimens.radiusChip))
            .background(colors.surfaceStrong)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun EmptyLibraryHint(
    onCopyFromComputer: () -> Unit,
    onWriteSong: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Sync and Share sit in a chip row above this card, which an operator looking at an empty
    // list has no reason to read as "how do I get songs onto this phone". Say it here.
    EmptyState(
        modifier = modifier,
        title = stringResource(Res.string.library_empty_title),
        body = stringResource(Res.string.library_empty_body_standalone),
        actionLabel = stringResource(Res.string.empty_action_get_content),
        actionIcon = Icons.Filled.CloudDownload,
        onAction = onCopyFromComputer,
        secondaryLabel = stringResource(Res.string.empty_action_write_song),
        onSecondary = onWriteSong,
    )
}

@Composable
private fun NoResultsHint(modifier: Modifier = Modifier) {
    HintCard(title = stringResource(Res.string.library_no_results), body = "", modifier = modifier)
}

@Composable
private fun HintCard(title: String, body: String, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.radiusCard))
            .background(colors.surface)
            .padding(AppDimens.space16),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        if (body.isNotBlank()) Text(body, color = colors.muted, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun AddChoiceDialog(
    onSong: () -> Unit,
    onAnnouncement: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.library_add)) },
        text = { Text(stringResource(Res.string.library_empty_body)) },
        confirmButton = { TextButton(onClick = onSong, modifier = Modifier.testTag(LibraryTags.ADD_SONG)) { Text(stringResource(Res.string.library_new_song)) } },
        dismissButton = {
            TextButton(onClick = onAnnouncement, modifier = Modifier.testTag(LibraryTags.ADD_NOTICE)) { Text(stringResource(Res.string.library_new_notice)) }
        },
    )
}

@Composable
private fun DeleteConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.library_delete_confirm_title)) },
        text = { Text(stringResource(Res.string.library_delete_confirm_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag(LibraryTags.DELETE_CONFIRM)) {
                Text(stringResource(Res.string.library_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag(LibraryTags.DELETE_DISMISS)) {
                Text(stringResource(Res.string.editor_cancel))
            }
        },
    )
}

/**
 * Which translation is presented, and the switch between the ones downloaded.
 *
 * Here because the Library is where this device's own content is kept, and because the choice
 * was previously reachable only from inside the copy sheet — an operator with two translations
 * had to open "copy from a computer" to change which one their congregation sees.
 *
 * Absent when nothing is downloaded: an empty picker is not a feature, and the Bible tab
 * already says how to get one.
 */
@Composable
private fun BibleChip(
    bibles: LocalBibleRepository,
    onCopyBible: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val viewModel: BibleChoiceViewModel = viewModel(key = "library_bible_choice") {
        BibleChoiceViewModel(bibles)
    }
    val installed by viewModel.installed.collectAsState()
    val active by viewModel.active.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    if (installed.isEmpty()) return

    Box {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(AppDimens.radiusPill))
                .background(colors.surface)
                .clickable { expanded = true }
                .padding(horizontal = AppDimens.space12, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.MenuBook,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = active?.title ?: stringResource(Res.string.library_bible_none),
                color = colors.text,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(14.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = colors.sheetBackground,
        ) {
            Text(
                text = stringResource(Res.string.library_bible_menu_title),
                color = colors.muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = AppDimens.space12, vertical = 6.dp),
            )
            installed.forEach { bible ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = bible.title,
                            color = colors.text,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = {
                        // A tick on the live one rather than a radio: this is a
                        // menu, and the row itself is the press.
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = if (bible.id == active?.id) colors.accent else Color.Transparent,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    onClick = {
                        viewModel.setActive(bible.id)
                        expanded = false
                    },
                    modifier = Modifier.testTag(LibraryTags.bibleMenuItem(bible.id)),
                )
            }
            HorizontalDivider(color = colors.borderSubtle)
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(Res.string.sync_section_bible),
                        color = colors.accent,
                        fontSize = 13.sp,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.CloudDownload,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(16.dp),
                    )
                },
                onClick = {
                    expanded = false
                    onCopyBible()
                },
                modifier = Modifier.testTag(LibraryTags.BIBLE_MENU_COPY),
            )
        }
    }
}

/**
 * "Synced 3 days ago", or an invitation when it never has been.
 *
 * Reads the persisted sync state rather than the network, so it can answer on a
 * Sunday morning with the router unplugged.
 */
@Composable
private fun SyncChip(settings: AppSettings, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val state = remember(settings) { readSyncState(settings.librarySyncStateJson) }

    val age = syncAgeFor(state, Clock.System.now().toEpochMilliseconds())
    val label = when (age.bucket) {
        SyncAge.Bucket.NEVER -> stringResource(Res.string.sync_never)
        SyncAge.Bucket.JUST_NOW -> stringResource(Res.string.sync_just_now)
        SyncAge.Bucket.MINUTES -> stringResource(Res.string.sync_minutes_ago, age.count.toString())
        SyncAge.Bucket.HOURS -> stringResource(Res.string.sync_hours_ago, age.count.toString())
        SyncAge.Bucket.DAYS -> stringResource(Res.string.sync_days_ago, age.count.toString())
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(AppDimens.radiusPill))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = AppDimens.space12, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.CloudDownload,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(14.dp),
        )
        Text(label, color = colors.muted, fontSize = 11.sp)
        Text(
            text = stringResource(Res.string.sync_action),
            color = colors.accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Opens the import/export sheet. */
@Composable
private fun ShareChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(AppDimens.radiusPill))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = AppDimens.space12, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.IosShare,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(Res.string.share_title),
            color = colors.accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Declaration order must match the segmented-control label order above. */
private val FILTERS = listOf(LibraryFilter.ALL, LibraryFilter.SONGS, LibraryFilter.ANNOUNCEMENTS)

// ── What the sync chip says ──────────────────────────────────────────────
//
// Split out of [SyncChip] because the thresholds are the part that can be
// wrong, and a composable that reads the wall clock cannot be checked without
// waiting out an hour. See the seam guidance in AGENT.md.

/** How long ago the library was last synced, in the terms the chip reports. */
internal data class SyncAge(val bucket: Bucket, val count: Long) {
    enum class Bucket { NEVER, JUST_NOW, MINUTES, HOURS, DAYS }
}

/**
 * Reads the stored sync state, falling back to "never synced".
 *
 * The blob is written by a previous version of the app and can be anything —
 * truncated by a kill mid-write, or a shape this build predates. The Library
 * tab has to open either way; a wrong chip is not worth a crash.
 */
internal fun readSyncState(storedJson: String): LibrarySyncState = runCatching {
    Json { ignoreUnknownKeys = true }.decodeFromString<LibrarySyncState>(storedJson)
}.getOrDefault(LibrarySyncState.NEVER)

/**
 * Which bucket [state]'s last sync falls into as of [nowMs], and the number to
 * show with it.
 *
 * Coarse on purpose: the operator wants "this morning" or "last week", not a
 * timestamp. A clock that has gone backwards — a device correcting its time, or
 * a sync recorded on another phone — reads as "just now" rather than as a
 * negative age.
 */
internal fun syncAgeFor(state: LibrarySyncState, nowMs: Long): SyncAge {
    if (!state.hasEverSynced) return SyncAge(SyncAge.Bucket.NEVER, 0L)
    val elapsedMs = (nowMs - state.lastSyncEpochMs).coerceAtLeast(0L)
    return when {
        elapsedMs < MINUTE_MS -> SyncAge(SyncAge.Bucket.JUST_NOW, 0L)
        elapsedMs < HOUR_MS -> SyncAge(SyncAge.Bucket.MINUTES, elapsedMs / MINUTE_MS)
        elapsedMs < DAY_MS -> SyncAge(SyncAge.Bucket.HOURS, elapsedMs / HOUR_MS)
        else -> SyncAge(SyncAge.Bucket.DAYS, elapsedMs / DAY_MS)
    }
}

private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 3_600_000L
private const val DAY_MS = 86_400_000L
