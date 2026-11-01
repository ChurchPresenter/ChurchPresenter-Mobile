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

    // Verse 1 is long enough to be split with a manual break ([---]), so it is two
    // slides on the desktop and must be two cards here.
    private val splitDetail = detail.copy(
        verses = listOf(
            SongVerse(
                number = 1,
                label = "Verse 1",
                lines = listOf(
                    "Amazing grace! how sweet the sound",
                    "That saved a wretch like me!",
                    "[---]",
                    "I once was lost, but now am found,",
                    "Was blind, but now I see.",
                ),
            ),
        ) + detail.verses.orEmpty().drop(1),
    )

    // An order of its own that sings the chorus twice and leaves verse 3 out, with the song's notes
    // and a cue on two sections: what the desktop sends once a song has been arranged.
    private val arranged = SongDetail(
        number = "12",
        title = "Amazing Grace",
        songbook = "Hymns",
        sections = listOf(
            SongVerse(
                label = "Verse 1",
                key = "[Verse 1]",
                lines = listOf("Amazing grace! how sweet the sound", "That saved a wretch like me!"),
            ),
            SongVerse(
                label = "Chorus",
                key = "{Chorus}",
                lines = listOf("Praise the Lord, praise the Lord", "Let the earth hear His voice"),
            ),
            SongVerse(
                label = "Verse 2",
                key = "[Verse 2]",
                lines = listOf("'Twas grace that taught my heart to fear,", "And grace my fears relieved;"),
            ),
            SongVerse(
                label = "Verse 3",
                key = "[Verse 3]",
                lines = listOf("Through many dangers, toils and snares,", "I have already come;"),
            ),
        ),
        order = listOf("[Verse 1]", "{Chorus}", "[Verse 2]", "{Chorus}"),
        playOrder = listOf("[Verse 1]", "{Chorus}", "[Verse 2]", "{Chorus}", "[Verse 3]"),
        notes = "Capo 2 for the band. Piano intro, 4 bars. Last verse a cappella, then full band.",
        cues = mapOf("[Verse 1]" to "Piano only", "[Verse 2]" to "Key change"),
    )

    private fun songDetail(
        detail: SongDetail? = this.detail,
        isLoading: Boolean = false,
        error: String? = null,
        selectedVerseIndex: Int? = null,
        isProjecting: Boolean = false,
        showChords: Boolean = false,
        standalone: Boolean = false,
    ): @Composable () -> Unit = {
        SongDetailScreen(
            detail = detail,
            isLoading = isLoading,
            error = error,
            selectedVerseIndex = selectedVerseIndex,
            isProjecting = isProjecting,
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
    fun arranged() = screenshot("song-detail__arranged", content = songDetail(detail = arranged))

    @Test
    fun arrangedChorusLiveAgain() = screenshot(
        "song-detail__arranged-live",
        // Live on the chorus the second time round: the fourth card, not the second.
        content = songDetail(detail = arranged, selectedVerseIndex = 3, isProjecting = true),
    )

    @Test
    fun splitVerse() = screenshot(
        "song-detail__split-verse",
        content = songDetail(detail = splitDetail),
    )

    @Test
    fun splitVerseSecondHalfLive() = screenshot(
        "song-detail__split-verse-live",
        // The second half of verse 1 live: the slide the reported bug landed on by mistake.
        content = songDetail(detail = splitDetail, selectedVerseIndex = 1, isProjecting = true),
    )

    @Test
    fun onATabletWidth() = screenshot(
        "song-detail__tablet",
        width = Screenshots.TABLET_WIDTH,
        content = songDetail(selectedVerseIndex = 0),
    )
}
