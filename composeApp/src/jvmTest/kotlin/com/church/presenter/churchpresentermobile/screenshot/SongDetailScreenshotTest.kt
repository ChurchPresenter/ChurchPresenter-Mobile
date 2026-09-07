package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.runtime.Composable
import com.church.presenter.churchpresentermobile.model.SongDetail
import com.church.presenter.churchpresentermobile.model.SongVerse
import com.church.presenter.churchpresentermobile.ui.SongDetailScreen
import kotlin.test.Test

/**
 * [SongDetailScreen] — the screen an operator drives the service from.
 *
 * It is the busiest state machine in the app: a verse can be selected or not,
 * the song live or not, already in the schedule or not, drawn as words or as a
 * chord chart, and the whole schedule button disappears in standalone mode.
 * Every one of those changes what is on screen.
 */
class SongDetailScreenshotTest {

    private val detail = SongDetail(
        number = "1",
        title = "Amazing Grace",
        author = "John Newton",
        songbook = "Hymns",
        verses = listOf(
            SongVerse(
                number = 1,
                label = "Verse 1",
                lines = listOf(
                    "Amazing grace! how sweet the sound",
                    "That saved a wretch like me!",
                    "I once was lost, but now am found,",
                    "Was blind, but now I see.",
                ),
            ),
            SongVerse(
                number = 2,
                label = "Chorus",
                lines = listOf(
                    "'Twas grace that taught my heart to fear,",
                    "And grace my fears relieved;",
                ),
            ),
            SongVerse(
                number = 3,
                label = "Verse 2",
                lines = listOf(
                    "Through many dangers, toils and snares,",
                    "I have already come;",
                ),
            ),
        ),
    )

    private fun songDetail(
        detail: SongDetail? = this.detail,
        isLoading: Boolean = false,
        error: String? = null,
        selectedVerseIndex: Int? = null,
        isProjecting: Boolean = false,
        scheduleAdded: Boolean = false,
        showChords: Boolean = false,
        standalone: Boolean = false,
    ): @Composable () -> Unit = {
        SongDetailScreen(
            detail = detail,
            isLoading = isLoading,
            error = error,
            selectedVerseIndex = selectedVerseIndex,
            isProjecting = isProjecting,
            scheduleAdded = scheduleAdded,
            onVerseSelected = {},
            onToggleProjecting = {},
            onAddToSchedule = if (standalone) null else ({ }),
            onClearDisplay = {},
            showChords = showChords,
        )
    }

    @Test
    fun loaded() = screenshot("song-detail__loaded", content = songDetail())

    @Test
    fun loading() = screenshot(
        "song-detail__loading",
        content = songDetail(detail = null, isLoading = true),
    )

    @Test
    fun error() = screenshot(
        "song-detail__error",
        content = songDetail(detail = null, error = "Could not load this song from your computer."),
    )

    @Test
    fun verseSelected() = screenshot(
        "song-detail__verse-selected",
        content = songDetail(selectedVerseIndex = 1),
    )

    @Test
    fun projectingSelectedVerse() = screenshot(
        "song-detail__projecting",
        // Live, on the chorus: the state the screen spends a service in.
        content = songDetail(selectedVerseIndex = 1, isProjecting = true),
    )

    @Test
    fun alreadyInSchedule() = screenshot(
        "song-detail__in-schedule",
        content = songDetail(scheduleAdded = true),
    )

    @Test
    fun asChordChart() = screenshot(
        "song-detail__chords",
        content = songDetail(showChords = true),
    )

    @Test
    fun standalone() = screenshot(
        "song-detail__standalone",
        content = songDetail(standalone = true),
    )

    @Test
    fun onATabletWidth() = screenshot(
        "song-detail__tablet",
        width = Screenshots.TABLET_WIDTH,
        content = songDetail(selectedVerseIndex = 0),
    )
}
