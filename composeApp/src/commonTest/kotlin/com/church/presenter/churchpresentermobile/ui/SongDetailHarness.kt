package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import com.church.presenter.churchpresentermobile.model.SongDetail
import com.church.presenter.churchpresentermobile.model.SongVerse

/** Setup for the song detail sheet. */
internal fun verse(label: String?, text: String) = SongVerse(label = label, text = text)

internal fun detailOf(verses: List<SongVerse> = emptyList(), plain: String? = null) =
    SongDetail(verses = verses.ifEmpty { null }, text = plain)

internal val amazingGrace = detailOf(
    listOf(
        verse("Verse", "Amazing grace, how sweet the sound"),
        verse("Chorus", "How sweet the sound"),
        verse("Verse", "'Twas grace that taught my heart to fear"),
    ),
)

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showSongDetail(
    detail: SongDetail? = amazingGrace,
    isLoading: Boolean = false,
    error: String? = null,
    selectedVerseIndex: Int? = null,
    isProjecting: Boolean = false,
    scheduleAdded: Boolean = false,
    showChords: Boolean = false,
    onVerseSelected: (Int) -> Unit = {},
    onToggleProjecting: () -> Unit = {},
    onAddToSchedule: (() -> Unit)? = {},
    onClearDisplay: (() -> Unit)? = {},
) = showScreen {
    SongDetailScreen(
        detail = detail,
        isLoading = isLoading,
        error = error,
        selectedVerseIndex = selectedVerseIndex,
        isProjecting = isProjecting,
        scheduleAdded = scheduleAdded,
        onVerseSelected = onVerseSelected,
        onToggleProjecting = onToggleProjecting,
        onAddToSchedule = onAddToSchedule,
        onClearDisplay = onClearDisplay,
        showChords = showChords,
    )
}
