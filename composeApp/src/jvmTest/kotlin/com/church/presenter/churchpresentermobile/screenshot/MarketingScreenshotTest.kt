package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.ui.test.hasText
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.AppTab
import com.church.presenter.churchpresentermobile.model.MediaPlaybackState
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.present.SinkState
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.BibleDetailScreen
import com.church.presenter.churchpresentermobile.ui.MediaScreen
import com.church.presenter.churchpresentermobile.ui.MoreScreen
import com.church.presenter.churchpresentermobile.ui.ScreenHeader
import com.church.presenter.churchpresentermobile.ui.SongDetailScreen
import com.church.presenter.churchpresentermobile.ui.SongsTable
import com.church.presenter.churchpresentermobile.ui.genesis
import com.church.presenter.churchpresentermobile.ui.library.LibraryScreen
import com.church.presenter.churchpresentermobile.ui.library.biblesWith
import com.church.presenter.churchpresentermobile.ui.library.libraryOf
import com.church.presenter.churchpresentermobile.ui.standalone.StandaloneControllerScreen
import com.church.presenter.churchpresentermobile.ui.standalone.StandaloneFixture
import com.church.presenter.churchpresentermobile.ui.standalone.StandaloneOutputScreen
import com.church.presenter.churchpresentermobile.ui.standalone.TestSink
import com.church.presenter.churchpresentermobile.viewmodel.MediaViewModel
import com.church.presenter.churchpresentermobile.viewmodel.StandaloneViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test

/**
 * Store and website images — one per screen, in light and dark.
 *
 * NOT part of the test gate. `screenshotTest` never runs this class and nothing
 * diffs what it produces; it fills `composeApp/marketing/` with pictures for an
 * App Store listing, a Play listing and the website.
 *
 * ```
 * ./gradlew :composeApp:marketingScreenshots
 * ```
 *
 * These have to look like screenshots somebody took of a phone, which is a
 * different job from a golden and drives three choices:
 *
 * - **The app's chrome is in the picture** — the real [ScreenHeader] and the
 *   real tab strip, wired as `App.kt` wires them. Without it the image is a
 *   screen's contents floating on a background, and reads as a mock-up.
 * - **Real content, from [MarketingContent]** — a whole chapter, a whole hymn,
 *   a library somebody has used. The test fixtures are three clipped verses,
 *   which is right for a diff and leaves half a listing image empty.
 * - **1080x2340 at 3x**, the shape and resolution of a real phone screenshot.
 */
class MarketingScreenshotTest {

    private fun settings() = AppSettings(InMemorySettingsStorage())

    // ── Remote control: the tabs a volunteer drives a service from ───────

    @Test
    fun songs() = marketingShot(
        "01-songs",
        tab = AppTab.SONGS,
        header = { ScreenHeader(title = "Songs", onMenu = {}, onSettings = {}) },
        until = { onAllNodes(hasText("Amazing", substring = true)).fetchSemanticsNodes().isNotEmpty() },
    ) {
        SongsTable(appSettings = settings(), isDemoMode = true, settingsSaveToken = 0)
    }

    @Test
    fun songDetail() = marketingShot(
        "02-song-detail",
        tab = AppTab.SONGS,
        header = {
            ScreenHeader(title = "Amazing Grace", subtitle = "Hymns", largeTitle = false, onBack = {})
        },
    ) {
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
        )
    }

    @Test
    fun bible() = marketingShot(
        "03-bible",
        tab = AppTab.BIBLE,
        header = { ScreenHeader(title = "Genesis 1", largeTitle = false, onBack = {}) },
    ) {
        BibleDetailScreen(
            book = genesis,
            selectedChapter = 1,
            verses = MarketingContent.genesisOne,
            isLoading = false,
            isProjecting = true,
            isHolding = false,
            scheduleAdded = false,
            selectedVerseIndices = setOf(2),
            projectedVerseIndex = 2,
            isMultiSelectMode = false,
            onToggleMultiSelect = {},
            onChapterSelect = {},
            onVerseToggleSelection = {},
            onToggleProjecting = {},
            onToggleHold = {},
            onClearDisplay = {},
            onAddToSchedule = {},
        )
    }

    @Test
    fun media() = marketingShot(
        "04-media",
        tab = AppTab.MEDIA,
        header = { ScreenHeader(title = "Media", onMenu = {}, onSettings = {}) },
    ) {
        val appSettings = settings()
        MediaScreen(
            viewModel = MediaViewModel(
                appSettings = appSettings,
                eventService = ServerEventService(appSettings),
                sender = FakeWsSender(),
                playbackState = MutableStateFlow(
                    MediaPlaybackState(
                        isLive = true,
                        isLoaded = true,
                        isPlaying = true,
                        title = "Welcome video",
                        durationMs = 180_000,
                        positionMs = 42_000,
                        mediaType = "video",
                    ),
                ),
            ),
            canUploadFiles = true,
            maxUploadMb = 200,
        )
    }

    @Test
    fun more() = marketingShot(
        "05-more",
        tab = AppTab.MORE,
        header = { ScreenHeader(title = "More", onMenu = {}, onSettings = {}) },
    ) {
        MoreScreen(mode = AppMode.REMOTE, onSelect = {})
    }

    // ── Standalone: the phone driving the room on its own ────────────────

    @Test
    fun library() = marketingShot(
        "06-library",
        tab = AppTab.LIBRARY,
        tabs = AppTab.forMode(AppMode.STANDALONE),
        header = { ScreenHeader(title = "Library", onMenu = {}, onSettings = {}) },
    ) {
        LibraryScreen(
            repository = libraryOf(
                songs = MarketingContent.librarySongs,
                notices = MarketingContent.notices,
            ),
            bibles = biblesWith("King James Version"),
            settings = settings(),
            sender = FakeWsSender(),
            onEditSong = {},
            onEditAnnouncement = {},
        )
    }

    @Test
    fun standaloneController() = marketingShot(
        "07-standalone-controller",
        tab = AppTab.PRESENT,
        tabs = AppTab.forMode(AppMode.STANDALONE),
        header = { ScreenHeader(title = "Present", onMenu = {}, onSettings = {}) },
    ) {
        val fixture = StandaloneFixture()
        val appSettings = settings()
        // A screen attached and a hymn loaded and live. Left as the fixture
        // builds it, this captures "No screen connected / Nothing loaded" over
        // an empty preview — the app's blank state, and the worst possible frame
        // for a listing: it advertises the feature doing nothing.
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
            providedViewModel = StandaloneViewModel(fixture.engine, fixture.registry, appSettings, null),
        )
    }

    // ── What the congregation sees ───────────────────────────────────────

    @Test
    fun outputSlide() = marketingShot(
        // No chrome and no tab strip: this one is the projector, not the phone.
        "08-output-slide",
        width = Screenshots.TABLET_WIDTH,
        height = Screenshots.STORE_TABLET_HEIGHT,
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
}
