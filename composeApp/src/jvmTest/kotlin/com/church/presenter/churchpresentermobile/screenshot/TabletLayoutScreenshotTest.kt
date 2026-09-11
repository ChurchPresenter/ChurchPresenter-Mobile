package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.AppTab
import com.church.presenter.churchpresentermobile.model.BibleBook
import com.church.presenter.churchpresentermobile.model.BibleVerse
import com.church.presenter.churchpresentermobile.model.MediaPlaybackState
import com.church.presenter.churchpresentermobile.model.MoreDestination
import com.church.presenter.churchpresentermobile.model.Song
import com.church.presenter.churchpresentermobile.model.SongDetail
import com.church.presenter.churchpresentermobile.model.SongVerse
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.BibleBooksScreen
import com.church.presenter.churchpresentermobile.ui.BibleThreePane
import com.church.presenter.churchpresentermobile.ui.BibleVersesPane
import com.church.presenter.churchpresentermobile.ui.ChaptersGrid
import com.church.presenter.churchpresentermobile.ui.DictionaryScreen
import com.church.presenter.churchpresentermobile.ui.FakeDesktop
import com.church.presenter.churchpresentermobile.ui.MediaPlayerPane
import com.church.presenter.churchpresentermobile.ui.MediaSendPane
import com.church.presenter.churchpresentermobile.ui.MediaTwoPane
import com.church.presenter.churchpresentermobile.ui.MoreTwoPane
import com.church.presenter.churchpresentermobile.ui.NavRail
import com.church.presenter.churchpresentermobile.ui.SongDetailScreen
import com.church.presenter.churchpresentermobile.ui.SongsListScreen
import com.church.presenter.churchpresentermobile.ui.SongsTwoPane
import com.church.presenter.churchpresentermobile.ui.library.LibraryScreen
import com.church.presenter.churchpresentermobile.ui.library.LibraryTwoPane
import com.church.presenter.churchpresentermobile.ui.library.SongEditorScreen
import com.church.presenter.churchpresentermobile.ui.library.amazingGrace
import com.church.presenter.churchpresentermobile.ui.library.libraryOf
import com.church.presenter.churchpresentermobile.ui.library.song
import com.church.presenter.churchpresentermobile.ui.standalone.StandaloneControllerScreen
import com.church.presenter.churchpresentermobile.ui.standalone.StandaloneFixture
import com.church.presenter.churchpresentermobile.viewmodel.MediaSource
import com.church.presenter.churchpresentermobile.viewmodel.StandaloneViewModel
import kotlin.test.Test

/**
 * The tablet layouts — the shapes a phone screenshot cannot show.
 *
 * What each of these is protecting is the *arrangement*: that the rail keeps its
 * width and sits on the right edge of the content, that each pane's header hangs
 * over the pane it belongs to rather than spanning both, that a pane with
 * nothing in it reads as "nothing open yet" rather than as a failure, and that
 * the verse grid comes out two columns rather than one or three. The panes'
 * contents are already covered by `songs-list`, `song-detail`, `library` and the
 * rest, and are not re-asserted here.
 *
 * Every one of these needs [Screenshots.TABLET_SURFACE]: the default test window
 * is 1024x768 and a capture is clipped to it, so a two-pane screen shot without
 * it records a cropped golden that looks plausible and hides the pane it cut.
 */
@OptIn(ExperimentalTestApi::class)
class TabletLayoutScreenshotTest {

    // ── The rail ─────────────────────────────────────────────────────────

    /** The rail fills the height it is given, so the frame has to give it one. */
    private fun rail(selected: AppTab, tabs: List<AppTab>): @Composable () -> Unit = {
        Box(Modifier.height(420.dp)) {
            NavRail(selectedTab = selected, onTabSelected = {}, tabs = tabs)
        }
    }

    @Test
    fun railRemote() = screenshot(
        "nav-rail__remote",
        width = null,
        content = rail(AppTab.SONGS, AppTab.forMode(AppMode.REMOTE)),
    )

    @Test
    fun railStandalone() = screenshot(
        "nav-rail__standalone",
        width = null,
        // A different tab set entirely — Present and Library replace Media and
        // the desktop's decks, exactly as in the bottom strip.
        content = rail(AppTab.PRESENT, AppTab.forMode(AppMode.STANDALONE)),
    )

    // ── Songs ────────────────────────────────────────────────────────────

    @Test
    fun songsNothingOpen() = tablet("songs-two-pane__nothing-open") {
        songsPane(showDetail = false, projecting = false)
    }

    @Test
    fun songsWithASongOpen() = tablet("songs-two-pane__song-open") {
        songsPane(showDetail = true, projecting = false)
    }

    @Test
    fun songsWithAVerseLive() = tablet("songs-two-pane__verse-live") {
        songsPane(showDetail = true, projecting = true)
    }

    @Test
    fun songsRightToLeft() = tablet(
        "songs-two-pane__right-to-left",
        direction = LayoutDirection.Rtl,
    ) {
        // The rail belongs on the right, the panes mirror, the chevrons turn and
        // the verse grid flows right-to-left. Captured with the rail in frame
        // because which edge it lands on is the whole point.
        Row(Modifier.fillMaxSize()) {
            NavRail(selectedTab = AppTab.SONGS, onTabSelected = {}, tabs = AppTab.forMode(AppMode.REMOTE))
            songsPane(showDetail = true, projecting = true)
        }
    }

    @Composable
    private fun songsPane(showDetail: Boolean, projecting: Boolean) {
        SongsTwoPane(
            showDetail = showDetail,
            detailTitle = "Amazing Grace".takeIf { showDetail },
            detailSubtitle = "Hymns",
            onMenu = {},
            onSettings = {},
            listPane = {
                SongsListScreen(
                    songs = songs,
                    selectedSong = songs.first().takeIf { showDetail },
                    isLoading = false,
                    error = null,
                    searchQuery = "",
                    selectedBook = null,
                    availableBooks = listOf("Hymns", "Modern"),
                    hasActiveFilter = false,
                    onSearchQueryChange = {},
                    onBookSelected = {},
                    onSongClick = {},
                    onRefresh = {},
                    showsLocalLibrary = false,
                    modifier = Modifier.fillMaxSize(),
                )
            },
            detailPane = {
                SongDetailScreen(
                    detail = amazingGraceDetail,
                    isLoading = false,
                    error = null,
                    selectedVerseIndex = 1.takeIf { projecting },
                    isProjecting = projecting,
                    scheduleAdded = false,
                    onVerseSelected = {},
                    onToggleProjecting = {},
                    onAddToSchedule = {},
                    onClearDisplay = {},
                    modifier = Modifier.fillMaxSize(),
                )
            },
        )
    }

    // ── Bible ────────────────────────────────────────────────────────────

    @Test
    fun bibleNothingOpen() = tablet("bible-three-pane__nothing-open") { biblePanes(book = null) }

    @Test
    fun bibleWithAChapterOpen() = tablet("bible-three-pane__chapter-open") { biblePanes(book = genesis) }

    @Composable
    private fun biblePanes(book: BibleBook?) {
        BibleThreePane(
            book = book,
            chapter = 1.takeIf { book != null },
            errorBanner = {},
            booksPane = {
                BibleBooksScreen(
                    books = listOf(genesis, BibleBook(name = "Exodus", chapterTotal = 40)),
                    searchQuery = "",
                    onSearchQueryChange = {},
                    onBookSelect = {},
                    modifier = Modifier.fillMaxSize(),
                )
            },
            chaptersPane = { b, columns ->
                ChaptersGrid(
                    book = b,
                    onChapterSelect = {},
                    modifier = Modifier.fillMaxSize(),
                    selectedChapter = 1,
                    columns = columns,
                )
            },
            versesPane = { b, chapter ->
                BibleVersesPane(
                    book = b,
                    selectedChapter = chapter,
                    verses = verses,
                    isProjecting = true,
                    scheduleAdded = false,
                    selectedVerseIndices = setOf(2),
                    projectedVerseIndex = 2,
                    onChapterSelect = {},
                    onVerseToggleSelection = {},
                    onToggleProjecting = {},
                    onAddToSchedule = {},
                    modifier = Modifier.fillMaxSize(),
                )
            },
            onMenu = {},
            onSettings = {},
        )
    }

    // ── More ─────────────────────────────────────────────────────────────

    @Test
    fun moreNothingOpen() = tablet("more-two-pane__nothing-open") {
        MoreTwoPane(
            mode = AppMode.REMOTE,
            selected = null,
            onSelect = {},
            tool = {},
            onMenu = {},
            onSettings = {},
        )
    }

    @Test
    fun moreWithAToolOpen() = tablet(
        "more-two-pane__tool-open",
        // The dictionary answers on another dispatcher; without this the golden
        // is of an empty tool pane, recorded as the picture of an open one.
        until = { onAllNodes(hasText("bara", substring = true)).fetchSemanticsNodes().isNotEmpty() },
    ) {
        MoreTwoPane(
            mode = AppMode.REMOTE,
            selected = MoreDestination.DICTIONARY,
            onSelect = {},
            tool = {
                DictionaryScreen(
                    viewModel = FakeDesktop().viewModel(),
                    settingsSaveToken = 0,
                    modifier = Modifier.fillMaxSize(),
                )
            },
            onMenu = {},
            onSettings = {},
        )
    }

    // ── Library ──────────────────────────────────────────────────────────

    @Test
    fun libraryNothingOpen() = tablet("library-two-pane__nothing-open") {
        val repository = libraryOf(
            songs = listOf(amazingGrace(), song("s2", "7", "In Christ Alone")),
        )
        LibraryTwoPane(
            list = {
                LibraryScreen(
                    repository = repository,
                    bibles = LocalBibleRepository(InMemoryFileStorage()),
                    settings = AppSettings(InMemorySettingsStorage()),
                    sender = FakeWsSender(),
                    onEditSong = {},
                    onEditAnnouncement = {},
                )
            },
            editor = null,
            onMenu = {},
            onSettings = {},
        )
    }

    @Test
    fun libraryWithTheEditorOpen() = tablet("library-two-pane__editing") {
        val repository = libraryOf(
            songs = listOf(amazingGrace(), song("s2", "7", "In Christ Alone")),
        )
        LibraryTwoPane(
            list = {
                LibraryScreen(
                    repository = repository,
                    bibles = LocalBibleRepository(InMemoryFileStorage()),
                    settings = AppSettings(InMemorySettingsStorage()),
                    sender = FakeWsSender(),
                    onEditSong = {},
                    onEditAnnouncement = {},
                )
            },
            editor = {
                SongEditorScreen(
                    repository = repository,
                    songId = null,
                    onClose = {},
                    modifier = Modifier.fillMaxSize(),
                )
            },
            onMenu = {},
            onSettings = {},
        )
    }

    // ── Media ────────────────────────────────────────────────────────────

    @Test
    fun mediaPlaying() = tablet("media-two-pane__playing") {
        MediaTwoPane(
            player = {
                MediaPlayerPane(
                    playback = playing,
                    source = MediaSource.URL,
                    composedUrl = "https://stream.church.local/welcome.mp4",
                    uploaded = null,
                    progress = 0.42f,
                    scrubbing = false,
                    scrubValue = 0f,
                    onScrub = {},
                    onScrubFinished = {},
                    onStop = {},
                    onBack10 = {},
                    onPlayPause = {},
                    onForward10 = {},
                    onMute = {},
                    onVolume = {},
                )
            },
            send = {
                MediaSendPane(
                    playback = playing,
                    source = MediaSource.URL,
                    url = "https://stream.church.local/welcome.mp4",
                    composedUrl = "https://stream.church.local/welcome.mp4",
                    uploaded = null,
                    uploading = false,
                    uploadProgress = 0f,
                    canUploadFiles = true,
                    maxUploadMb = 200,
                    onAddToSchedule = {},
                    onGoLive = {},
                    onClearScreen = {},
                    onSourceChange = {},
                    onUrlChange = {},
                    onFilePicked = {},
                    onPickError = {},
                )
            },
            snackbarHostState = SnackbarHostState(),
            onMenu = {},
            onSettings = {},
        )
    }

    // ── Present ──────────────────────────────────────────────────────────

    @Test
    fun presentController() = tablet("present-two-pane__idle") {
        // The real screen with `twoPane` set, on the same in-memory engine the
        // phone golden uses — so this captures the arrangement the app actually
        // builds rather than one assembled by hand.
        val fixture = StandaloneFixture()
        val settings = AppSettings(InMemorySettingsStorage())
        StandaloneControllerScreen(
            engine = fixture.engine,
            registry = fixture.registry,
            settings = settings,
            twoPane = true,
            onMenu = {},
            onSettings = {},
            providedViewModel = StandaloneViewModel(fixture.engine, fixture.registry, settings, null),
        )
    }

    // ── Frame ────────────────────────────────────────────────────────────

    /** A two-pane subject, laid out on a tablet-sized window rather than cropped to the default one. */
    private fun tablet(
        name: String,
        direction: LayoutDirection = LayoutDirection.Ltr,
        until: (ComposeUiTest.() -> Boolean)? = null,
        content: @Composable () -> Unit,
    ) = screenshot(
        name = name,
        width = null,
        surface = Screenshots.TABLET_SURFACE,
        layoutDirection = direction,
        until = until,
        content = content,
    )

    // ── Fixtures ─────────────────────────────────────────────────────────

    private val songs = listOf(
        Song(id = 1, number = "1", title = "Amazing Grace", author = "John Newton", bookName = "Hymns"),
        Song(id = 2, number = "7", title = "In Christ Alone", author = "Keith Getty", bookName = "Modern"),
        Song(id = 3, number = "23", title = "Be Thou My Vision", bookName = "Hymns"),
        Song(id = 4, number = "88", title = "Great Is Thy Faithfulness", bookName = "Hymns"),
        Song(id = 5, number = "104", title = "How Great Thou Art", bookName = "Hymns"),
    )

    private val amazingGraceDetail = SongDetail(
        title = "Amazing Grace",
        verses = listOf(
            verse(
                "Amazing grace! how sweet the sound",
                "That saved a wretch like me!",
                "I once was lost, but now am found,",
                "Was blind, but now I see.",
            ),
            verse(
                "'Twas grace that taught my heart to fear,",
                "And grace my fears relieved;",
                "How precious did that grace appear",
                "The hour I first believed!",
            ),
            verse(
                "Through many dangers, toils and snares,",
                "I have already come;",
                "'Tis grace hath brought me safe thus far,",
                "And grace will lead me home.",
            ),
            verse(
                "When we've been there ten thousand years,",
                "Bright shining as the sun,",
                "We've no less days to sing God's praise",
                "Than when we first begun.",
            ),
        ),
    )

    private fun verse(vararg lines: String) =
        SongVerse(type = "verse", text = lines.joinToString("\n"))

    private val genesis = BibleBook(name = "Genesis", chapterTotal = 50)

    private val verses = listOf(
        BibleVerse(verse = 1, text = "In the beginning God created the heaven and the earth."),
        BibleVerse(
            verse = 2,
            text = "And the earth was without form, and void; and darkness was upon " +
                "the face of the deep.",
        ),
        BibleVerse(verse = 3, text = "And God said, Let there be light: and there was light."),
        BibleVerse(
            verse = 4,
            text = "And God saw the light, that it was good: and God divided the " +
                "light from the darkness.",
        ),
    )

    private val playing = MediaPlaybackState(
        isLive = true,
        isLoaded = true,
        isPlaying = true,
        title = "Welcome video",
        durationMs = 180_000,
        positionMs = 42_000,
        mediaType = "video",
    )

}
