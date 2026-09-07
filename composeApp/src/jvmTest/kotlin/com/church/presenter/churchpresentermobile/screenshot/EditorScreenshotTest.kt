package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.runtime.Composable
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.model.LocalSongSection
import com.church.presenter.churchpresentermobile.model.SectionType
import com.church.presenter.churchpresentermobile.ui.library.AnnouncementEditorScreen
import com.church.presenter.churchpresentermobile.ui.library.SongEditorScreen
import com.church.presenter.churchpresentermobile.ui.library.libraryOf
import kotlin.test.Test

/**
 * The two editors the operator types content into.
 *
 * Both open in two quite different shapes — empty for a new item, filled for an
 * existing one — and the song editor grows a section per verse, so a song with
 * one section and a song with several are not the same screen.
 */
class EditorScreenshotTest {

    private val song = LocalSong(
        id = "s1",
        number = "42",
        title = "Amazing Grace",
        author = "John Newton",
        sections = listOf(
            LocalSongSection(
                SectionType.VERSE,
                "Amazing grace! how sweet the sound\nThat saved a wretch like me!",
            ),
            LocalSongSection(
                SectionType.CHORUS,
                "'Twas grace that taught my heart to fear,\nAnd grace my fears relieved;",
            ),
        ),
    )

    private val notice = LocalAnnouncement(
        id = "a1",
        title = "Working bee",
        body = "Saturday from 9am. Bring gloves, and a plate for morning tea.",
    )

    private fun songEditor(song: LocalSong? = null): @Composable () -> Unit {
        val repository = libraryOf(songs = listOfNotNull(song))
        return { SongEditorScreen(repository = repository, songId = song?.id, onClose = {}) }
    }

    private fun noticeEditor(notice: LocalAnnouncement? = null): @Composable () -> Unit {
        val repository = libraryOf(notices = listOfNotNull(notice))
        return {
            AnnouncementEditorScreen(
                repository = repository,
                announcementId = notice?.id,
                onClose = {},
            )
        }
    }

    @Test
    fun newSong() = screenshot("song-editor__new", content = songEditor())

    @Test
    fun existingSong() = screenshot("song-editor__existing", content = songEditor(song))

    @Test
    fun songWithOneSection() = screenshot(
        "song-editor__single-section",
        content = songEditor(song.copy(sections = song.sections.take(1))),
    )

    @Test
    fun songWithNoWords() = screenshot(
        // A song saved with a number and title but nothing to sing — the editor
        // has to be usable in the state a half-finished entry leaves it in.
        "song-editor__no-sections",
        content = songEditor(song.copy(sections = emptyList())),
    )

    @Test
    fun newNotice() = screenshot("notice-editor__new", content = noticeEditor())

    @Test
    fun existingNotice() = screenshot("notice-editor__existing", content = noticeEditor(notice))

    @Test
    fun noticeWithLongBody() = screenshot(
        "notice-editor__long-body",
        content = noticeEditor(
            notice.copy(
                body = "Saturday from 9am. Bring gloves, secateurs if you have them, " +
                    "and a plate for morning tea. We will be tidying the car park beds " +
                    "and repainting the hall door, so old clothes are a good idea.",
            ),
        ),
    )
}
