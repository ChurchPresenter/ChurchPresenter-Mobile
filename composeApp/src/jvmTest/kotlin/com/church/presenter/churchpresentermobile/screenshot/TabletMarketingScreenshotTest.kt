package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performScrollToNode
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.AppTab
import com.church.presenter.churchpresentermobile.model.MediaPlaybackState
import com.church.presenter.churchpresentermobile.model.MoreDestination
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.present.SinkState
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
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
import com.church.presenter.churchpresentermobile.ui.SongDetailScreen
import com.church.presenter.churchpresentermobile.ui.SongsListScreen
import com.church.presenter.churchpresentermobile.ui.SongsTwoPane
import com.church.presenter.churchpresentermobile.ui.library.LibraryScreen
import com.church.presenter.churchpresentermobile.ui.library.LibraryTags
import com.church.presenter.churchpresentermobile.ui.library.LibraryTwoPane
import com.church.presenter.churchpresentermobile.ui.library.SongEditorScreen
import com.church.presenter.churchpresentermobile.ui.library.biblesWith
import com.church.presenter.churchpresentermobile.ui.library.libraryOf
import com.church.presenter.churchpresentermobile.ui.standalone.StandaloneControllerScreen
import com.church.presenter.churchpresentermobile.ui.standalone.StandaloneFixture
import com.church.presenter.churchpresentermobile.ui.standalone.StandaloneOutputScreen
import com.church.presenter.churchpresentermobile.ui.standalone.TestSink
import com.church.presenter.churchpresentermobile.viewmodel.MediaSource
import com.church.presenter.churchpresentermobile.viewmodel.StandaloneViewModel
import kotlin.test.Test

/**
 * Store images for the tablet slots — one per screen, per [StoreTablet], in
 * light and dark — into `composeApp/marketing/tablet/` and `marketing/ipad/`.
 *
 * NOT part of the test gate, like [MarketingScreenshotTest] whose phone set
 * this mirrors: nothing diffs these, and `marketingScreenshots` is the only
 * task that runs them.
 *
 * ```
 * ./gradlew :composeApp:marketingScreenshots
 * ```
 *
 * What a tablet screenshot is on a listing to show is the layout a phone
 * cannot have: the rail, and the list beside the thing it opened. So every
 * one of these is a two-pane screen with something open in the second pane —
 * a hymn with a verse live, a chapter with a verse live, a tool, an editor —
 * built from the same composables `App.kt` puts beside the rail, on the
 * content [MarketingContent] fills a phone with, which fills a tablet too.
 */
@OptIn(ExperimentalTestApi::class)
class TabletMarketingScreenshotTest {

    private fun settings() = AppSettings(InMemorySettingsStorage())

    // ── Remote control: the tabs a volunteer drives a service from ───────

    @Test
    fun songs() = tabletShot("01-songs", tab = AppTab.SONGS) {
        val songs = MarketingContent.songs
        SongsTwoPane(
            showDetail = true,
            detailTitle = "Amazing Grace",
            detailSubtitle = "Hymns",
            onMenu = null,
            onSettings = null,
            listPane = {
                SongsListScreen(
                    songs = songs,
                    selectedSong = songs.first(),
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
                    detail = MarketingContent.amazingGrace,
                    isLoading = false,
                    error = null,
                    selectedVerseIndex = 1,
                    isProjecting = true,
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

    @Test
    fun bible() = tabletShot("02-bible", tab = AppTab.BIBLE) {
        val genesis = MarketingContent.bibleBooks.first()
        BibleThreePane(
            book = genesis,
            chapter = 1,
            errorBanner = {},
            booksPane = {
                BibleBooksScreen(
                    books = MarketingContent.bibleBooks,
                    searchQuery = "",
                    onSearchQueryChange = {},
                    onBookSelect = {},
                    modifier = Modifier.fillMaxSize(),
                )
            },
            chaptersPane = { book, columns ->
                ChaptersGrid(
                    book = book,
                    onChapterSelect = {},
                    modifier = Modifier.fillMaxSize(),
                    selectedChapter = 1,
                    columns = columns,
                )
            },
            versesPane = { book, chapter ->
                BibleVersesPane(
                    book = book,
                    selectedChapter = chapter,
                    verses = MarketingContent.genesisOne,
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
        )
    }

    @Test
    fun media() = tabletShot("03-media", tab = AppTab.MEDIA) {
        val url = "https://stream.church.local/welcome.mp4"
        MediaTwoPane(
            player = {
                MediaPlayerPane(
                    playback = playing,
                    source = MediaSource.URL,
                    composedUrl = url,
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
                    url = url,
                    composedUrl = url,
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
        )
    }

    @Test
    fun more() = tabletShot(
        "04-more",
        tab = AppTab.MORE,
        // The dictionary answers on another dispatcher; without this the image
        // is of an empty tool pane.
        until = { onAllNodes(hasText("bara", substring = true)).fetchSemanticsNodes().isNotEmpty() },
    ) {
        MoreTwoPane(
            mode = AppMode.REMOTE,
            selected = MoreDestination.DICTIONARY,
            onSelect = {},
            tool = {
                DictionaryScreen(
                    viewModel = FakeDesktop(entries = MarketingContent.dictionary).viewModel(),
                    settingsSaveToken = 0,
                    modifier = Modifier.fillMaxSize(),
                )
            },
        )
    }

    // ── Standalone: the tablet driving the room on its own ───────────────

    @Test
    fun library() = tabletShot(
        "05-library",
        tab = AppTab.LIBRARY,
        tabs = AppTab.forMode(AppMode.STANDALONE),
        // The editor's slide preview is blank until a section is edited, and a
        // blank black panel is most of the pane. So edit the first verse and
        // edit it straight back: a text field only reports a change when the
        // words differ, so retyping them as they are would report nothing. The
        // words end up as they were and the preview shows them. Then put the
        // form back at the top for the picture.
        prepare = {
            val firstVerse = MarketingContent.librarySongs.first().sections.first().text
            editorForm().performScrollToNode(hasTestTag(LibraryTags.verse(0)))
            onNode(hasTestTag(LibraryTags.verse(0))).performTextReplacement("$firstVerse ")
            onNode(hasTestTag(LibraryTags.verse(0))).performTextReplacement(firstVerse)
            editorForm().performScrollToNode(hasTestTag(LibraryTags.FIELD_TITLE))
        },
    ) {
        val repository = libraryOf(
            songs = MarketingContent.librarySongs,
            notices = MarketingContent.notices,
        )
        LibraryTwoPane(
            list = {
                LibraryScreen(
                    repository = repository,
                    bibles = biblesWith("King James Version"),
                    settings = settings(),
                    sender = FakeWsSender(),
                    onEditSong = {},
                    onEditAnnouncement = {},
                )
            },
            // A hymn open for editing, not a blank editor: the pane is there
            // to show that the words live on the device and can be changed.
            editor = {
                SongEditorScreen(
                    repository = repository,
                    songId = MarketingContent.librarySongs.first().id,
                    onClose = {},
                    modifier = Modifier.fillMaxSize(),
                )
            },
        )
    }

    @Test
    fun standaloneController() = tabletShot(
        "06-standalone-controller",
        tab = AppTab.PRESENT,
        tabs = AppTab.forMode(AppMode.STANDALONE),
    ) {
        val fixture = StandaloneFixture()
        val appSettings = settings()
        // A screen attached and a hymn live, as the phone image has — the blank
        // "No screen connected" state is the worst frame a listing could show.
        fixture.registry.register(
            TestSink(id = "hall", displayName = "The hall screen", state = SinkState.ATTACHED),
        )
        fixture.engine.setDeck(MarketingContent.amazingGraceDeck)
        fixture.engine.goLive()
        StandaloneControllerScreen(
            engine = fixture.engine,
            registry = fixture.registry,
            settings = appSettings,
            photos = null,
            twoPane = true,
            providedViewModel = StandaloneViewModel(fixture.engine, fixture.registry, appSettings, null),
        )
    }

    // ── What the congregation sees ───────────────────────────────────────

    @Test
    fun outputSlide() = tabletShot(
        // No rail and no gear: this one is the projector, which is landscape
        // like the tablet frame, so it needs no frame of its own.
        "07-output-slide",
        tab = null,
    ) {
        StandaloneOutputScreen(
            slide = Slide(
                kind = SlideKind.BIBLE,
                body = "For God so loved the world, that he gave his only begotten Son, " +
                    "that whosoever believeth in him should not perish, but have everlasting life.",
                reference = "John 3:16",
            ),
        )
    }

    /**
     * The song editor's scrolling form.
     *
     * Found by holding *any* `editor:` field, not a particular one: a scroll
     * re-resolves its node after every step, and a field that has just been
     * scrolled out of the viewport is no longer a descendant. The form always
     * has some tagged field composed, so this selector survives the scroll.
     */
    private fun ComposeUiTest.editorForm() =
        onNode(hasScrollAction() and hasAnyDescendant(editorField))

    private val editorField = SemanticsMatcher("has an editor: tag") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("editor:") == true
    }

    // ── Fixtures ─────────────────────────────────────────────────────────

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
