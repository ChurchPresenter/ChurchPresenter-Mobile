package com.church.presenter.churchpresentermobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.announcements_title
import churchpresentermobile.composeapp.generated.resources.app_title
import churchpresentermobile.composeapp.generated.resources.bible_chapter_label
import churchpresentermobile.composeapp.generated.resources.deep_link_connected
import churchpresentermobile.composeapp.generated.resources.media_title
import churchpresentermobile.composeapp.generated.resources.contact_us_title
import churchpresentermobile.composeapp.generated.resources.more_notices_title
import churchpresentermobile.composeapp.generated.resources.more_photos_title
import churchpresentermobile.composeapp.generated.resources.strongs_dictionary_title
import churchpresentermobile.composeapp.generated.resources.tab_bible
import churchpresentermobile.composeapp.generated.resources.tab_more
import churchpresentermobile.composeapp.generated.resources.tab_presentation
import churchpresentermobile.composeapp.generated.resources.tab_qa_admin
import churchpresentermobile.composeapp.generated.resources.tab_songs
import churchpresentermobile.composeapp.generated.resources.web_title
import coil3.request.crossfade
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppModeHolder
import com.church.presenter.churchpresentermobile.model.ChordsPreference
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.AppTab
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.library.ServiceOrder
import com.church.presenter.churchpresentermobile.present.PhotoLibrary
import com.church.presenter.churchpresentermobile.present.ProjectionRouter
import com.church.presenter.churchpresentermobile.present.SinkRegistry
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.present.sink.WebPageSink
import com.church.presenter.churchpresentermobile.present.sink.createExternalDisplaySink
import com.church.presenter.churchpresentermobile.model.MoreDestination
import com.church.presenter.churchpresentermobile.model.SetlistEntryType
import com.church.presenter.churchpresentermobile.model.ScheduleItem
import com.church.presenter.churchpresentermobile.model.supportsEmbeddedServer
import com.church.presenter.churchpresentermobile.model.supportsStandalone
import com.church.presenter.churchpresentermobile.model.BibleBook
import com.church.presenter.churchpresentermobile.network.createImageHttpClient
import com.church.presenter.churchpresentermobile.network.PingReporter
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.church.presenter.churchpresentermobile.ui.AnnouncementsScreen
import com.church.presenter.churchpresentermobile.ui.MediaScreen
import com.church.presenter.churchpresentermobile.ui.WebScreen
import com.church.presenter.churchpresentermobile.ui.AppBackHandler
import com.church.presenter.churchpresentermobile.ui.BibleScreen
import com.church.presenter.churchpresentermobile.ui.BottomTabBar
import com.church.presenter.churchpresentermobile.ui.DictionaryScreen
import com.church.presenter.churchpresentermobile.ui.MoreScreen
import com.church.presenter.churchpresentermobile.ui.ScreenHeader
import com.church.presenter.churchpresentermobile.ui.ConnectSetupScreen
import com.church.presenter.churchpresentermobile.ui.PicturesScreen
import com.church.presenter.churchpresentermobile.ui.PresentationScreen
import com.church.presenter.churchpresentermobile.ui.QAAdminScreen
import com.church.presenter.churchpresentermobile.ui.ScheduleDrawerContent
import com.church.presenter.churchpresentermobile.ui.ServiceOrderDrawerContent
import com.church.presenter.churchpresentermobile.ui.SettingsScreen
import com.church.presenter.churchpresentermobile.ui.SongsTable
import com.church.presenter.churchpresentermobile.ui.ModePickerScreen
import com.church.presenter.churchpresentermobile.ui.SplashScreen
import com.church.presenter.churchpresentermobile.ui.standalone.LocalPhotosScreen
import com.church.presenter.churchpresentermobile.ui.ContactScreen
import com.church.presenter.churchpresentermobile.ui.standalone.LocalNoticesScreen
import com.church.presenter.churchpresentermobile.ui.standalone.LocalWebScreen
import com.church.presenter.churchpresentermobile.ui.standalone.StandaloneControllerScreen
import com.church.presenter.churchpresentermobile.ui.library.AnnouncementEditorScreen
import com.church.presenter.churchpresentermobile.ui.library.LibraryScreen
import com.church.presenter.churchpresentermobile.ui.library.SongEditorScreen
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.util.isDebugBuild
import com.church.presenter.churchpresentermobile.util.RemoteConfig
import com.church.presenter.churchpresentermobile.util.RemoteConfigDefaults
import com.church.presenter.churchpresentermobile.util.RemoteConfigKeys
import com.church.presenter.churchpresentermobile.util.Logger
import com.church.presenter.churchpresentermobile.util.CrashReporting
import com.church.presenter.churchpresentermobile.util.Analytics
import com.church.presenter.churchpresentermobile.util.AnalyticsEvent
import com.church.presenter.churchpresentermobile.util.AnalyticsParam
import com.church.presenter.churchpresentermobile.util.AnalyticsScreen
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.network.BibleCatalog
import com.church.presenter.churchpresentermobile.network.BibleService
import com.church.presenter.churchpresentermobile.network.SongCatalog
import com.church.presenter.churchpresentermobile.network.SongService
import com.church.presenter.churchpresentermobile.viewmodel.AnnouncementsViewModel
import com.church.presenter.churchpresentermobile.viewmodel.MediaSource
import com.church.presenter.churchpresentermobile.viewmodel.MediaViewModel
import com.church.presenter.churchpresentermobile.viewmodel.WebViewModel
import com.church.presenter.churchpresentermobile.viewmodel.BibleViewModel
import com.church.presenter.churchpresentermobile.viewmodel.DictionaryViewModel
import com.church.presenter.churchpresentermobile.viewmodel.PicturesViewModel
import com.church.presenter.churchpresentermobile.viewmodel.PresentationsViewModel
import com.church.presenter.churchpresentermobile.viewmodel.QAViewModel
import com.church.presenter.churchpresentermobile.viewmodel.ScheduleViewModel
import com.church.presenter.churchpresentermobile.viewmodel.SongsViewModel
import com.church.presenter.churchpresentermobile.viewmodel.StatusViewModel
import com.church.presenter.churchpresentermobile.viewmodel.StatusUiState
import com.church.presenter.churchpresentermobile.ui.StatusScreen
import kotlin.time.Clock
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    val appSettings = remember { AppSettings() }

    // Seed the process-wide mode holder from persisted settings before anything
    // reads it. AppSettings coerces to REMOTE on platforms without an output
    // sink, so the web build is unaffected by any of this.
    remember(appSettings) { AppModeHolder.init(appSettings) }
    remember(appSettings) { ChordsPreference.init(appSettings) }
    val appMode by AppModeHolder.mode.collectAsState()

    // Anonymous, city-level ping to the live user map — fires once per app launch.
    // Only send the persistent device id when the user has opted into usage
    // analytics — matching the desktop app's equivalent gate (main.kt) and the
    // server's documented privacy intent. Opted-out users still send an
    // anonymous geo ping (PingReporter treats a blank id as "no id").
    LaunchedEffect(Unit) {
        PingReporter.pingOnOpen(if (appSettings.isTelemetryEnabled) appSettings.deviceId else "")
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // ── Deep link / QR code: churchpresenter://connect?host=…&port=…&apikey=… ──
    // DeepLinkHandler.appliedCount increments each time a valid URL is applied.
    // We pick up the fresh values, bump settingsSaveToken so every screen reloads,
    // and show a snackbar so the user knows the settings changed.
    val deepLinkCount by DeepLinkHandler.appliedCount.collectAsState()

    // Demo mode — driven by Firebase Remote Config (default: false).
    // Read eagerly so the app has a value before the async fetch completes,
    // then update the state once fetchAndActivate returns.
    // Debug builds always skip demo mode so developers work against live data.
    var isDemoMode by remember {
        mutableStateOf(
            if (isDebugBuild) false
            else RemoteConfig.getBoolean(RemoteConfigKeys.IS_DEMO_MODE, RemoteConfigDefaults.IS_DEMO_MODE)
        )
    }
    // On debug builds the LaunchedEffect is omitted entirely — we never call
    // fetchAndActivate, so Remote Config cannot activate a demo-mode flag and
    // isDemoMode is guaranteed to stay false for the lifetime of the composition.
    if (!isDebugBuild) {
        LaunchedEffect(Unit) {
            RemoteConfig.fetchAndActivate { _ ->
                isDemoMode = RemoteConfig.getBoolean(RemoteConfigKeys.IS_DEMO_MODE, RemoteConfigDefaults.IS_DEMO_MODE)
            }
        }
    }
    // Log every time isDemoMode flips so we can confirm the recomposition fires
    LaunchedEffect(isDemoMode) {
        Logger.d("App", "isDemoMode = $isDemoMode")
    }

    // ── Shared persistent WebSocket connection ──────────────────────────
    // Single ServerEventService used by all ViewModels for both receiving
    // server-push events and sending action messages.
    val eventService = remember { ServerEventService(appSettings) }

    // ── Projection routing ────────────────────────────────────────────────
    // In REMOTE mode the router delegates verbatim to eventService, so every
    // service below behaves exactly as it always has. In STANDALONE mode the
    // same actions are handled locally by StandaloneEngine and slides are
    // rendered by whatever output sinks are attached. Services are handed the
    // router instead of eventService and are unaware the distinction exists.
    val sinkRegistry = remember { SinkRegistry() }
    // The on-device library. Owned here so the Library tab and the standalone
    // controller see the same content without either owning the other.
    val libraryRepository = remember {
        LibraryRepository(now = { Clock.System.now().toEpochMilliseconds() })
    }
    // Translations copied onto this device. Separate from the song library because a Bible is
    // megabytes and that document is rewritten whole on every song edit.
    val bibleRepository = remember {
        LocalBibleRepository(now = { Clock.System.now().toEpochMilliseconds() })
            .also { it.load() }
    }
    // Photos the operator picks for this service. Held here because two things
    // need the same set: the screen that picks and projects them, and the
    // presentation server that serves the bytes to whatever is displaying.
    val photoLibrary = remember { PhotoLibrary(newId = { generateUUID() }) }
    // Sink state as a stream. SinkRegistry.hasAttachedSink is a plain getter, so a screen
    // attaching or detaching would not have recomposed anything reading it.
    val sinkStatuses by sinkRegistry.statuses.collectAsState()
    val standaloneEngine = remember(sinkRegistry) {
        StandaloneEngine(AppModeHolder.mode, sinkRegistry)
    }
    val projectionRouter = remember(standaloneEngine) {
        ProjectionRouter(AppModeHolder.mode, eventService, standaloneEngine)
    }

    // ── Output sinks ──────────────────────────────────────────────────────
    // Registered where the platform supports them at all; attached only while
    // the app is in standalone mode. Registering up front is what lets the
    // outputs list say "External display — no screen connected" rather than
    // silently omitting the option.
    LaunchedEffect(sinkRegistry) {
        createExternalDisplaySink()?.let(sinkRegistry::register)
        if (supportsEmbeddedServer) {
            sinkRegistry.register(
                WebPageSink(
                    preferredPort = appSettings.standalonePort,
                    // Remember whatever port was actually bound so the URL the
                    // operator gave the TV stays the same next service.
                    onPortBound = { appSettings.standalonePort = it },
                    // The same server that serves the display page serves the
                    // operator's photos, so the browser screen and the in-process
                    // one fetch an image from the identical address.
                    photos = photoLibrary.source,
                    onBaseUrl = { photoLibrary.serveFrom(it) },
                )
            )
        }
        // Sinks report asynchronously — a display can appear seconds after
        // attach, or vanish mid-service — so mirror each sink's own status flow
        // into the registry's aggregate for the outputs UI.
        sinkRegistry.all().forEach { sink ->
            launch { sink.status.collect { sinkRegistry.refreshStatuses() } }
        }
    }
    // Attach on entering standalone, detach on leaving it, so a remote-mode user
    // never has a presentation window opened behind their back.
    LaunchedEffect(appMode) {
        sinkRegistry.all().forEach { sink ->
            if (appMode == AppMode.STANDALONE) sink.attach() else sink.detach()
        }
        sinkRegistry.refreshStatuses()
    }

    // Re-ping the live map once this session actually connects to a desktop, so
    // the server can distinguish paired mobile use from standalone (opened but
    // never connected). `first { it }` suspends until the first successful
    // connect and then completes, so reconnects don't re-fire it. If the app
    // never connects, this just stays suspended until disposed. Same telemetry
    // gate as pingOnOpen above.
    LaunchedEffect(eventService) {
        eventService.connected.first { it }
        PingReporter.pingConnected(if (appSettings.isTelemetryEnabled) appSettings.deviceId else "")
    }

    // Pause the WebSocket reconnect loop while the app is backgrounded so it stops
    // retrying connects to the server (which otherwise keep threads busy and can
    // surface as a background ANR). Resumes when the app returns to the foreground.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, eventService) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP  -> eventService.pause()
                Lifecycle.Event.ON_START -> eventService.resume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── Session-scoped ViewModels ─────────────────────────────────────────
    // Created ONCE here in App() which is never removed from the composition.
    // This guarantees the loaded book/song lists survive tab switches forever —
    // no data is ever discarded just because the user navigated to another tab.
    // Where Bible text is read from, decided per call by the current mode: the desktop in
    // remote, a downloaded translation in standalone — and, when the desktop cannot be
    // reached mid-service, the downloaded one rather than an empty tab.
    val bibleCatalog = remember(appSettings, bibleRepository) {
        BibleCatalog(
            mode = AppModeHolder.mode,
            remote = BibleService(appSettings, projectionRouter),
            bibles = bibleRepository,
        )
    }
    val bibleViewModel: BibleViewModel = viewModel(key = "bible_$isDemoMode") {
        BibleViewModel(
            appSettings, projectionRouter, isDemoMode, standaloneEngine,
            catalog = bibleCatalog,
        )
    }
    // Where song content is read from, decided per call by the current mode —
    // the desktop in remote, this device's library in standalone. The same idea
    // as projectionRouter above, applied to reads instead of actions.
    // What "add to schedule" writes to in standalone: an ordered list on this
    // device, in place of a desktop schedule that isn't there.
    val serviceOrder = remember(libraryRepository) { ServiceOrder(libraryRepository) }
    val songCatalog = remember(appSettings, libraryRepository) {
        SongCatalog(
            mode = AppModeHolder.mode,
            remote = SongService(appSettings, projectionRouter),
            library = libraryRepository,
        )
    }
    val songsViewModel: SongsViewModel = viewModel(key = "songs_$isDemoMode") {
        SongsViewModel(
            appSettings, eventService, isDemoMode, projectionRouter, standaloneEngine,
            service = serviceOrder, catalog = songCatalog,
        )
    }
    val picturesViewModel: PicturesViewModel = viewModel(key = "pictures_$isDemoMode") {
        PicturesViewModel(appSettings, projectionRouter, isDemoMode)
    }
    val presentationsViewModel: PresentationsViewModel = viewModel(key = "presentations_$isDemoMode") {
        PresentationsViewModel(appSettings, projectionRouter, isDemoMode)
    }
    val scheduleViewModel: ScheduleViewModel = viewModel(key = isDemoMode.toString()) {
        ScheduleViewModel(appSettings, eventService, isDemoMode)
    }
    val qaViewModel: QAViewModel = viewModel(key = "qa") {
        QAViewModel(appSettings, eventService)
    }
    val dictionaryViewModel: DictionaryViewModel = viewModel(key = "dictionary") {
        DictionaryViewModel(appSettings, projectionRouter)
    }
    val announcementsViewModel: AnnouncementsViewModel = viewModel(key = "announcements") {
        AnnouncementsViewModel(appSettings, projectionRouter)
    }
    val webViewModel: WebViewModel = viewModel(key = "web") {
        WebViewModel(appSettings, projectionRouter)
    }
    val mediaViewModel: MediaViewModel = viewModel(key = "media") {
        MediaViewModel(appSettings, eventService, projectionRouter)
    }

    // When the desktop clears its display, reset projection state on mobile
    LaunchedEffect(scheduleViewModel) {
        scheduleViewModel.displayCleared.collect {
            bibleViewModel.onDisplayCleared()
            songsViewModel.onDisplayCleared()
        }
    }

    // When the desktop switches Bible translation, reload books on mobile
    LaunchedEffect(scheduleViewModel) {
        scheduleViewModel.bibleUpdated.collect {
            bibleViewModel.loadBooks(forceReload = true)
        }
    }

    // When the server pushes a songs_updated event, reload songs on mobile
    LaunchedEffect(scheduleViewModel) {
        scheduleViewModel.songsUpdated.collect {
            songsViewModel.loadSongs(forceReload = true)
        }
    }

    // When the server pushes a presentation_updated event, reload presentations on mobile
    LaunchedEffect(scheduleViewModel) {
        scheduleViewModel.presentationUpdated.collect {
            presentationsViewModel.loadPresentations()
        }
    }

    // When the server pushes a pictures_updated event, reload pictures on mobile
    LaunchedEffect(scheduleViewModel) {
        scheduleViewModel.picturesUpdated.collect {
            picturesViewModel.loadPictures()
        }
    }

    // Theme mode – updated whenever the user saves settings
    var themeMode by remember { mutableStateOf(appSettings.themeMode) }

    // Bible navigation state lifted up so the shared toolbar can reflect it
    var bibleBook by remember { mutableStateOf<BibleBook?>(null) }
    var bibleChapter by remember { mutableStateOf<Int?>(null) }
    var bibleNavigateBack: (() -> Unit)? by remember { mutableStateOf(null) }

    // Song detail navigation state
    var songDetailTitle by remember { mutableStateOf<String?>(null) }
    var songDetailBookName by remember { mutableStateOf<String?>(null) }
    var songNavigateBack: (() -> Unit)? by remember { mutableStateOf(null) }

    // Incremented each time the user saves settings; screens react via LaunchedEffect
    var settingsSaveToken by remember { mutableStateOf(0) }

    // Pin the server URL to Crashlytics so every non-fatal report shows which server
    // the app was connected to at the time of the failure.
    LaunchedEffect(settingsSaveToken) {
        CrashReporting.setCustomKey("server_url", appSettings.apiBaseUrl)
        CrashReporting.setCustomKey("server_host", appSettings.host)
        CrashReporting.setCustomKey("server_port", appSettings.port.toString())
        if (settingsSaveToken > 0) {
            Analytics.logEvent(AnalyticsEvent.SETTINGS_SAVED)
        }
    }
    // Library editor navigation. A non-null id edits that item; the "creating"
    // flags open the editor on a blank one (the id is null in that case).
    var editingSongId by remember { mutableStateOf<String?>(null) }
    var creatingSong by remember { mutableStateOf(false) }
    var editingAnnouncementId by remember { mutableStateOf<String?>(null) }
    var creatingAnnouncement by remember { mutableStateOf(false) }

    // Secondary destination selected inside the "More" tab (Q&A / Dictionary), or null for the launcher grid.
    var moreDestination by remember { mutableStateOf<MoreDestination?>(null) }

    // Settings are only opened manually by the user — not on first launch
    var showSettings by remember { mutableStateOf(false) }
    // Show connect QR-scan setup screen — set to true by the splash callback on
    // first launch (when !appSettings.isConnectSetupDone) or from SettingsScreen.
    var showConnectSetup by remember { mutableStateOf(false) }

    LaunchedEffect(showSettings) {
        if (showSettings) Analytics.logScreenView(AnalyticsScreen.SETTINGS)
    }
    LaunchedEffect(showConnectSetup) {
        if (showConnectSetup) Analytics.logScreenView(AnalyticsScreen.CONNECT_SETUP)
    }

    // Apply deep-linked settings and notify screens to reload.
    // deepLinkConnectedMsg is evaluated in composable scope so it picks up the
    // fresh host/port values written by DeepLinkHandler before recomposition.
    val deepLinkConnectedMsg = stringResource(Res.string.deep_link_connected,
        appSettings.host, appSettings.port.toString())
    LaunchedEffect(deepLinkCount) {
        if (deepLinkCount > 0) {
            settingsSaveToken++
            // Don't open settings if the connect-setup screen handled the scan
            if (!showConnectSetup) {
                showSettings = true
            }
            snackbarHostState.showSnackbar(
                message  = deepLinkConnectedMsg,
                duration = SnackbarDuration.Short
            )
        }
    }
    // Incremented when a song is added to the schedule; triggers drawer reload
    var scheduleRefreshToken by remember { mutableStateOf(0) }

    // ── Schedule-item navigation (drawer → content screen) ────────────────
    // Bible
    var pendingBibleBookName by remember { mutableStateOf<String?>(null) }
    var pendingBibleChapter  by remember { mutableStateOf<Int?>(null) }
    var pendingBibleVerses   by remember { mutableStateOf<Set<Int>>(emptySet()) }
    // Song
    var pendingSongTitle by remember { mutableStateOf<String?>(null) }
    var pendingSongBook  by remember { mutableStateOf<String?>(null) }
    // Picture
    var pendingPictureFolderId   by remember { mutableStateOf<String?>(null) }
    var pendingPictureImageIndex by remember { mutableStateOf<Int?>(null) }
    // Presentation
    var pendingPresentationId by remember { mutableStateOf<String?>(null) }
    // Media / Web / Announcement / Dictionary (from schedule-drawer taps)
    var pendingMediaUrl by remember { mutableStateOf<String?>(null) }
    var pendingWebUrl by remember { mutableStateOf<String?>(null) }
    var pendingAnnouncement by remember { mutableStateOf<ScheduleItem?>(null) }
    var pendingDictionaryQuery by remember { mutableStateOf<String?>(null) }

    // The tab strip differs per mode — standalone drops the desktop-only tabs
    // and adds the local controller and library.
    val tabs = AppTab.forMode(appMode)

    // Seed the initial tab from any pending shortcut/quick-action so that
    // rememberPagerState starts on the correct page immediately.  Without this,
    // the snapshotFlow's first emission (settledPage == 0) races against the
    // LaunchedEffect(shortcutTab) and can reset navigation back to Songs.
    val initialTab = TabNavigationHandler.requestedTab.value ?: AppTab.SONGS
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }

    // selectedTab is rememberSaveable, so after a mode switch it can still hold a
    // tab that is no longer in the strip. Every index lookup below is therefore
    // written to survive a -1, and this effect settles it on the next frame.
    LaunchedEffect(appMode) {
        if (selectedTab !in tabs) selectedTab = tabs.first()
        // Same for a More destination opened before the switch: standalone cannot
        // fill the desktop-backed screens, so leaving one open would strand the
        // operator on a screen the launcher no longer offers a way back to.
        moreDestination?.let { if (it !in MoreDestination.forMode(appMode)) moreDestination = null }
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // ── Shortcut / Quick-Action tab navigation ────────────────────────────
    // TabNavigationHandler.navigateTo() is called from Android shortcut intents
    // and iOS Quick-Action callbacks. Consume once so it doesn't re-fire.
    val shortcutTab by TabNavigationHandler.requestedTab.collectAsState()
    LaunchedEffect(shortcutTab) {
        shortcutTab?.let { tab ->
            selectedTab = tab
            TabNavigationHandler.consume()
        }
    }

    // ── Pager state — drives swipe-between-tabs ───────────────────────────
    val pagerState = rememberPagerState(
        initialPage = tabs.indexOf(selectedTab).coerceAtLeast(0),
        pageCount = { tabs.size }
    )

    // Swipe → update selected tab (settledPage avoids emitting intermediate pages
    // during programmatic animateScrollToPage, which would fight the animation).
    // drop(1) skips the initial emission whose value always matches the already-
    // correct selectedTab; without it the emission races against a pending
    // shortcut navigation and resets the tab back to Songs.
    // Keyed on `tabs` as well as the pager: the two strips are both five entries
    // long, so switching mode neither recreates pagerState (pageCount is
    // unchanged) nor restarts this effect on its own — and it would go on
    // translating page indices through the *previous* mode's strip. That put the
    // app in a permanent media → songs → bible loop after setup, each hop
    // re-firing every tab's network calls.
    LaunchedEffect(pagerState, tabs) {
        snapshotFlow { pagerState.settledPage }.drop(1).collect { page ->
            selectedTab = tabs.getOrNull(page) ?: tabs.first()
        }
    }

    // Tab click → animate pager to matching page + log screen view.
    // Also keyed on `tabs`, because a tab keeps its identity across a mode switch
    // while changing index (SONGS is 0 in remote, 1 in standalone) — without this
    // the pager would be left showing a different tab than the strip highlights.
    LaunchedEffect(selectedTab, tabs) {
        val targetPage = tabs.indexOf(selectedTab).coerceAtLeast(0)
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
        Analytics.logEvent(
            AnalyticsEvent.TAB_SELECTED,
            mapOf(AnalyticsParam.TAB_NAME to selectedTab.name.lowercase())
        )
        // Breadcrumb only — doesn't count against Sentry's event quota, but gives
        // crash reports a trail of which tabs were visited leading up to the crash.
        CrashReporting.log("Tab selected: ${selectedTab.name.lowercase()}")
        // Only log the tab-level screen when not inside a detail sub-screen
        val tabScreen = when (selectedTab) {
            AppTab.PRESENT       -> AnalyticsScreen.STANDALONE
            AppTab.LIBRARY       -> AnalyticsScreen.LIBRARY
            AppTab.SONGS         -> AnalyticsScreen.SONGS
            AppTab.BIBLE         -> AnalyticsScreen.BIBLE_BOOKS
            AppTab.MEDIA         -> AnalyticsScreen.MEDIA
            AppTab.PRESENTATION  -> AnalyticsScreen.PRESENTATIONS
            AppTab.MORE          -> AnalyticsScreen.MORE
        }
        Analytics.logScreenView(tabScreen)
    }

    // Song detail open/close → update screen name
    LaunchedEffect(songDetailTitle) {
        if (songDetailTitle != null) Analytics.logScreenView(AnalyticsScreen.SONG_DETAIL)
        else if (selectedTab == AppTab.SONGS) Analytics.logScreenView(AnalyticsScreen.SONGS)
    }

    // Bible depth navigation → update screen name
    LaunchedEffect(bibleBook, bibleChapter) {
        if (selectedTab != AppTab.BIBLE) return@LaunchedEffect
        Analytics.logScreenView(
            when {
                bibleChapter != null -> AnalyticsScreen.BIBLE_VERSES
                bibleBook    != null -> AnalyticsScreen.BIBLE_CHAPTERS
                else                 -> AnalyticsScreen.BIBLE_BOOKS
            }
        )
    }

    // Schedule drawer open → log event
    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Open) {
            Analytics.logEvent(AnalyticsEvent.SCHEDULE_DRAWER_OPENED)
        }
    }

    // ── Detail-screen flags (hoisted so both toolbar and pager can read them)
    val inSongDetail = selectedTab == AppTab.SONGS && songDetailTitle != null
    val inBibleDetail = selectedTab == AppTab.BIBLE && bibleBook != null
    val inMoreDetail = selectedTab == AppTab.MORE && moreDestination != null

    // System back inside a More sub-screen returns to the More launcher grid.
    AppBackHandler(enabled = inMoreDetail) { moreDestination = null }

    // Log More sub-screen views
    LaunchedEffect(moreDestination) {
        when (moreDestination) {
            MoreDestination.PICTURES -> Analytics.logScreenView(AnalyticsScreen.PICTURES)
            MoreDestination.QA -> Analytics.logScreenView(AnalyticsScreen.QA_ADMIN)
            MoreDestination.DICTIONARY -> Analytics.logScreenView(AnalyticsScreen.DICTIONARY)
            MoreDestination.ANNOUNCEMENTS -> Analytics.logScreenView(AnalyticsScreen.ANNOUNCEMENTS)
            MoreDestination.WEB -> Analytics.logScreenView(AnalyticsScreen.WEB)
            MoreDestination.CONTACT -> Unit
            null -> {}
        }
    }

    // Dedicated image HTTP client: same SSL bypass but no ContentNegotiation,
    // so Coil can read raw JPEG bytes without Ktor interfering.
    val platformContext = LocalPlatformContext.current
    val httpClient = remember { createImageHttpClient() }
    val imageLoader = remember(platformContext, httpClient) {
        ImageLoader.Builder(platformContext)
            .components { add(KtorNetworkFetcherFactory(httpClient = httpClient)) }
            .crossfade(true)
            .build()
    }
    DisposableEffect(httpClient) { onDispose { httpClient.close() } }

    // Splash screen state — shown once on first composition
    var showSplash by remember { mutableStateOf(true) }
    // Mode picker — shown once, after splash, on platforms that can present.
    var showModePicker by remember { mutableStateOf(false) }
    // Status screen state — shown once after splash; re-shown when settings are saved
    var showStatusScreen by remember { mutableStateOf(false) }
    LaunchedEffect(showStatusScreen) {
        if (showStatusScreen) Analytics.logScreenView(AnalyticsScreen.STATUS)
    }

    val statusViewModel: StatusViewModel =
        viewModel(key = "status_$settingsSaveToken") {
            StatusViewModel(appSettings)
        }

    // Derive per-feature permissions from the last known status response.
    // Defaults to false (deny-by-default) until the server explicitly grants the
    // permission — this prevents upload pickers from opening before/if the status
    // check completes, which was the root cause of the "upload disabled on server
    // but picker still opens" bug.
    val statusUiState by statusViewModel.uiState.collectAsState()
    val canUploadFiles = (statusUiState as? StatusUiState.Success)
        ?.status?.permissions?.canUploadFiles ?: false
    val maxMediaUploadMb = (statusUiState as? StatusUiState.Success)
        ?.status?.permissions?.maxMediaUploadMb ?: 700

    // The Media tab gates its upload UI on the server's live "file uploads allowed" permission —
    // re-fetch on entry so a desktop-side toggle is reflected without restarting the app.
    LaunchedEffect(selectedTab) {
        if (selectedTab == AppTab.MEDIA) statusViewModel.refreshQuietly()
    }

    // Preload the target screen's content when a schedule-drawer tap requests it.
    LaunchedEffect(pendingMediaUrl) {
        pendingMediaUrl?.let { mediaViewModel.setSource(MediaSource.URL); mediaViewModel.setUrl(it); pendingMediaUrl = null }
    }
    LaunchedEffect(pendingWebUrl) {
        pendingWebUrl?.let { webViewModel.setUrl(it); pendingWebUrl = null }
    }
    LaunchedEffect(pendingAnnouncement) {
        pendingAnnouncement?.let { announcementsViewModel.preload(it); pendingAnnouncement = null }
    }
    LaunchedEffect(pendingDictionaryQuery) {
        pendingDictionaryQuery?.let { if (it.isNotBlank()) dictionaryViewModel.setSearchQuery(it); pendingDictionaryQuery = null }
    }

    AppTheme(themeMode = themeMode) {
        if (showSplash) {
            SplashScreen(onComplete = {
                showSplash = false
                when {
                    // First launch on a phone: ask how they want to present before
                    // asking them to find a server they may not need.
                    supportsStandalone && !appSettings.isModeChosen -> showModePicker = true
                    // Standalone has no server to connect to or check.
                    appMode == AppMode.STANDALONE -> Unit
                    // On first launch skip the status check — show the connect-setup
                    // screen directly so the user configures the server first.
                    // On subsequent launches go straight to the status check as usual.
                    !appSettings.isConnectSetupDone -> showConnectSetup = true
                    else -> showStatusScreen = true
                }
            })
            return@AppTheme
        }
        if (showModePicker) {
            ModePickerScreen(
                initialMode = appMode,
                onModeChosen = { chosen ->
                    AppModeHolder.set(appSettings, chosen)
                    appSettings.isModeChosen = true
                    showModePicker = false
                    Analytics.logEvent(
                        AnalyticsEvent.SETTINGS_SAVED,
                        mapOf(AnalyticsParam.TAB_NAME to chosen.name.lowercase()),
                    )
                    // Standalone needs neither the server setup nor the status check.
                    if (chosen == AppMode.REMOTE) {
                        if (!appSettings.isConnectSetupDone) showConnectSetup = true else showStatusScreen = true
                    }
                },
            )
            return@AppTheme
        }
        // Both gates below talk to a desktop, so neither has anything to say in
        // standalone mode — guard on the mode as well as their own flags, in case
        // the user switched mode after one of them was queued.
        if (showStatusScreen && appMode == AppMode.REMOTE) {
            StatusScreen(
                viewModel      = statusViewModel,
                onContinue     = { showStatusScreen = false },
                onOpenSettings = { showStatusScreen = false; showSettings = true },
            )
            return@AppTheme
        }
        // First-launch connect setup — shown full-screen, after splash, before main content.
        // On subsequent launches showConnectSetup is false so this block is skipped entirely.
        if (showConnectSetup && !appSettings.isConnectSetupDone && appMode == AppMode.REMOTE) {
            ConnectSetupScreen(
                appSettings = appSettings,
                onDone = {
                    appSettings.isConnectSetupDone = true
                    appSettings.isSetupComplete = true
                    settingsSaveToken++
                    showConnectSetup = false
                    showStatusScreen = true   // now check server status
                },
                onSkip = {
                    appSettings.isConnectSetupDone = true
                    appSettings.isSetupComplete = true
                    showConnectSetup = false
                    showStatusScreen = true   // still check status even if skipped
                }
            )
            return@AppTheme
        }
        ModalNavigationDrawer(
            drawerState = drawerState,
            scrimColor = LocalAppColors.current.scrim,
            drawerContent = {
                // Standalone's running order is this device's own list, so it gets
                // the same drawer rather than a second idea of "today" somewhere
                // else — and, being local, it is editable in place.
                if (appMode == AppMode.STANDALONE) {
                    val serviceEntries by serviceOrder.entries.collectAsState(initial = serviceOrder.current)
                    ServiceOrderDrawerContent(
                        entries = serviceEntries,
                        onMove = { from, to -> serviceOrder.move(from, to) },
                        onRemove = { index -> serviceOrder.removeAt(index) },
                        onClear = { serviceOrder.clear() },
                        onClose = { coroutineScope.launch { drawerState.close() } },
                        onItemClick = { entry ->
                            coroutineScope.launch {
                                drawerState.close()
                                // Same reset as the remote drawer below: the
                                // destination should show its own header rather
                                // than the previous screen's.
                                songDetailTitle = null
                                songDetailBookName = null
                                bibleBook = null
                                bibleChapter = null
                                when (entry.type) {
                                    SetlistEntryType.SONG -> {
                                        selectedTab = AppTab.SONGS
                                        pendingSongTitle = entry.title
                                        pendingSongBook = null
                                    }
                                    // Announcements live in the Library tab in
                                    // standalone — that is where they are written
                                    // and where projecting one from starts.
                                    SetlistEntryType.ANNOUNCEMENT -> selectedTab = AppTab.LIBRARY
                                    SetlistEntryType.BIBLE -> selectedTab = AppTab.BIBLE
                                }
                            }
                        },
                    )
                    return@ModalNavigationDrawer
                }
                ScheduleDrawerContent(
                    appSettings = appSettings,
                    isDemoMode = isDemoMode,
                    settingsSaveToken = settingsSaveToken,
                    scheduleRefreshToken = scheduleRefreshToken,
                    providedViewModel = scheduleViewModel,
                    onClose = { coroutineScope.launch { drawerState.close() } },
                                    onItemClick = { item ->
                                        coroutineScope.launch {
                                            drawerState.close()
                                            // Clear any leftover song/bible detail state so the
                                            // destination shows its own header (with the right back
                                            // button) rather than the previous screen's.
                                            songDetailTitle = null
                                            songDetailBookName = null
                                            bibleBook = null
                                            bibleChapter = null
                                            when (item.type?.lowercase()) {
                                                "song" -> {
                                                    val title = item.title
                                                    if (title != null) {
                                                        selectedTab     = AppTab.SONGS
                                                        pendingSongTitle = title
                                                        pendingSongBook  = item.bookName
                                                    }
                                                }
                                                "bible" -> {
                                                    // Prefer structured fields; fall back to parsing title
                                                    // (e.g. "1 Kings 17:3,4,7" or "John 3:16-18")
                                                    var bookName = item.bookName
                                                    var chapter  = item.chapter

                                                    // Raw verse string from dedicated field or title suffix
                                                    val rawVerseStr: String? =
                                                        item.verseRange?.takeIf { it.isNotBlank() }
                                                            ?: item.verseNumber?.toString()

                                                    // Title parsing fallback for book, chapter and/or verses
                                                    val titleToParse = item.title?.trim()
                                                    var titleVerseStr: String? = null
                                                    if (titleToParse != null && titleToParse.contains(":")) {
                                                        val colonIdx    = titleToParse.lastIndexOf(':')
                                                        titleVerseStr   = titleToParse.substring(colonIdx + 1).trim()
                                                        val beforeColon = titleToParse.substring(0, colonIdx).trim()
                                                        val lastSpace   = beforeColon.lastIndexOf(' ')
                                                        if (lastSpace >= 0) {
                                                            if (bookName == null) bookName = beforeColon.substring(0, lastSpace).trim().ifBlank { null }
                                                            if (chapter  == null) chapter  = beforeColon.substring(lastSpace + 1).toIntOrNull()
                                                        }
                                                    }

                                                    val verseStr = rawVerseStr ?: titleVerseStr

                                                    if (bookName != null && chapter != null) {
                                                        selectedTab         = AppTab.BIBLE
                                                        pendingBibleBookName = bookName
                                                        pendingBibleChapter  = chapter
                                                        pendingBibleVerses   = parseVerseString(verseStr)
                                                    }
                                                }
                                                "image", "picture" -> {
                                                    // Pictures now live under the More tab.
                                                    selectedTab              = AppTab.MORE
                                                    moreDestination          = MoreDestination.PICTURES
                                                    // Server puts the folder UUID in the generic "id" field
                                                    pendingPictureFolderId   = item.id ?: item.folderId
                                                    pendingPictureImageIndex = item.imageIndex
                                                }
                                                "presentation" -> {
                                                    val id = item.id
                                                    if (!id.isNullOrBlank()) {
                                                        selectedTab           = AppTab.PRESENTATION
                                                        pendingPresentationId = id
                                                    }
                                                }
                                                "media" -> {
                                                    selectedTab = AppTab.MEDIA
                                                    pendingMediaUrl = item.mediaUrl
                                                }
                                                "announcement" -> {
                                                    selectedTab     = AppTab.MORE
                                                    moreDestination = MoreDestination.ANNOUNCEMENTS
                                                    pendingAnnouncement = item
                                                }
                                                "website", "web" -> {
                                                    selectedTab     = AppTab.MORE
                                                    moreDestination = MoreDestination.WEB
                                                    pendingWebUrl = item.url ?: item.displayText
                                                }
                                                "dictionary" -> {
                                                    selectedTab     = AppTab.MORE
                                                    moreDestination = MoreDestination.DICTIONARY
                                                    // Schedule sends "word (translit): definition" — search by the word.
                                                    pendingDictionaryQuery =
                                                        (item.text ?: item.displayText)?.substringBefore(" (")?.trim()
                                                }
                                            }
                                        }
                                    }
                )
            }
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    val chapterLabel = stringResource(Res.string.bible_chapter_label)
                    val appTitle = stringResource(Res.string.app_title)
                    val bibleTitle = stringResource(Res.string.tab_bible)
                    val qaTitle = stringResource(Res.string.tab_qa_admin)
                    when {
                        inSongDetail -> ScreenHeader(
                            title = songDetailTitle!!,
                            subtitle = songDetailBookName?.takeIf { it.isNotBlank() },
                            largeTitle = false,
                            onBack = { songNavigateBack?.invoke() }
                        )
                        inBibleDetail -> ScreenHeader(
                            title = if (bibleChapter != null)
                                "${bibleBook!!.displayName} · $chapterLabel $bibleChapter"
                            else bibleBook!!.displayName,
                            subtitle = if (bibleChapter == null) "Select a chapter" else null,
                            largeTitle = bibleChapter != null,
                            onBack = { bibleNavigateBack?.invoke() }
                        )
                        inMoreDetail -> ScreenHeader(
                            title = when (moreDestination) {
                                MoreDestination.PICTURES -> stringResource(Res.string.more_photos_title)
                                MoreDestination.QA -> qaTitle
                                MoreDestination.DICTIONARY -> stringResource(Res.string.strongs_dictionary_title)
                                MoreDestination.ANNOUNCEMENTS ->
                                    if (appMode == AppMode.STANDALONE) stringResource(Res.string.more_notices_title)
                                    else stringResource(Res.string.announcements_title)
                                MoreDestination.WEB -> stringResource(Res.string.web_title)
                                MoreDestination.CONTACT -> stringResource(Res.string.contact_us_title)
                                null -> ""
                            },
                            onBack = { moreDestination = null },
                            // Q&A keeps the settings gear; the dictionary header (screen 11) has none.
                            onSettings = if (moreDestination == MoreDestination.QA) ({ showSettings = true }) else null
                        )
                        else -> when (selectedTab) {
                            AppTab.SONGS -> ScreenHeader(
                                title = stringResource(Res.string.tab_songs),
                                onMenu = { coroutineScope.launch { drawerState.open() } },
                                onSettings = { showSettings = true }
                            )
                            AppTab.BIBLE -> ScreenHeader(
                                title = bibleTitle,
                                onMenu = { coroutineScope.launch { drawerState.open() } },
                                onSettings = { showSettings = true }
                            )
                            AppTab.MEDIA -> ScreenHeader(
                                title = stringResource(Res.string.media_title),
                                onMenu = { coroutineScope.launch { drawerState.open() } },
                                onSettings = { showSettings = true }
                            )
                            AppTab.PRESENTATION -> ScreenHeader(
                                title = stringResource(Res.string.tab_presentation),
                                onMenu = { coroutineScope.launch { drawerState.open() } },
                                onSettings = { showSettings = true }
                            )
                            AppTab.MORE -> ScreenHeader(
                                title = stringResource(Res.string.tab_more),
                                onMenu = { coroutineScope.launch { drawerState.open() } },
                                onSettings = { showSettings = true }
                            )
                            else -> ScreenHeader(
                                title = appTitle,
                                largeTitle = false,
                                onMenu = { coroutineScope.launch { drawerState.open() } },
                                onSettings = { showSettings = true }
                            )
                        }
                    }
                },
                bottomBar = {
                    BottomTabBar(
                        selectedTab = selectedTab,
                        tabs = tabs,
                        onTabSelected = { tab ->
                            // Re-tapping the active More tab returns to its launcher grid.
                            if (tab == AppTab.MORE && selectedTab == AppTab.MORE) moreDestination = null
                            selectedTab = tab
                        }
                    )
                }
            ) { innerPadding ->
                // Swipe-between-tabs — swiping is locked while inside a detail screen
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !inSongDetail && !inBibleDetail && !inMoreDetail,
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    beyondViewportPageCount = 0  // only compose the visible page
                ) { page ->
                    when (tabs.getOrNull(page) ?: tabs.first()) {
                        // The standalone live controller. Given the engine and the
                        // sink registry directly — they are collaborators, not
                        // ViewModels, so the screen still owns its own ViewModel.
                        AppTab.PRESENT -> StandaloneControllerScreen(
                            engine = standaloneEngine,
                            registry = sinkRegistry,
                            settings = appSettings,
                            // The same library the Photos screen fills, so a photo
                            // added there can be chosen as the backdrop here.
                            photos = photoLibrary,
                            modifier = Modifier.fillMaxSize()
                        )
                        AppTab.SONGS -> SongsTable(
                            providedViewModel = songsViewModel,
                            appSettings = appSettings,
                            isDemoMode = isDemoMode,
                            settingsSaveToken = settingsSaveToken,
                            onDetailChanged = { title, bookName ->
                                songDetailTitle = title
                                songDetailBookName = bookName
                            },
                            onRegisterBackAction = { action ->
                                songNavigateBack = action
                            },
                            onScheduleRefresh = { scheduleRefreshToken++ },
                            pendingNavSongTitle = pendingSongTitle,
                            pendingNavSongBook  = pendingSongBook,
                            onPendingNavHandled = {
                                pendingSongTitle = null
                                pendingSongBook  = null
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        AppTab.BIBLE -> BibleScreen(
                            providedViewModel = bibleViewModel,
                            appSettings = appSettings,
                            isDemoMode = isDemoMode,
                            settingsSaveToken = settingsSaveToken,
                            onNavigationChanged = { book, chapter ->
                                bibleBook = book
                                bibleChapter = chapter
                            },
                            onRegisterBackAction = { action ->
                                bibleNavigateBack = action
                            },
                            pendingNavBookName = pendingBibleBookName,
                            pendingNavChapter  = pendingBibleChapter,
                            pendingNavVerses   = pendingBibleVerses,
                            onPendingNavHandled = {
                                pendingBibleBookName = null
                                pendingBibleChapter  = null
                                pendingBibleVerses   = emptySet()
                            },
                            onScheduleRefresh = { scheduleRefreshToken++ },
                            modifier = Modifier.fillMaxSize()
                        )
                        AppTab.MEDIA -> MediaScreen(
                            viewModel = mediaViewModel,
                            canUploadFiles = canUploadFiles,
                            maxUploadMb = maxMediaUploadMb,
                            modifier = Modifier.fillMaxSize()
                        )
                        AppTab.PRESENTATION -> PresentationScreen(
                            appSettings = appSettings,
                            isDemoMode = isDemoMode,
                            settingsSaveToken = settingsSaveToken,
                            imageLoader = imageLoader,
                            pendingNavPresentationId = pendingPresentationId,
                            onPendingNavHandled = { pendingPresentationId = null },
                            canUploadFiles = canUploadFiles,
                            providedViewModel = presentationsViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                        AppTab.MORE -> when (moreDestination) {
                            // Standalone has no desktop folders to browse, so
                            // Photos means this device's own pictures.
                            MoreDestination.PICTURES if appMode == AppMode.STANDALONE ->
                                LocalPhotosScreen(
                                    library = photoLibrary,
                                    presenter = standaloneEngine,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            MoreDestination.PICTURES -> PicturesScreen(
                                appSettings = appSettings,
                                isDemoMode = isDemoMode,
                                settingsSaveToken = settingsSaveToken,
                                imageLoader = imageLoader,
                                pendingNavFolderId = pendingPictureFolderId,
                                pendingNavImageIndex = pendingPictureImageIndex,
                                onPendingNavHandled = {
                                    pendingPictureFolderId   = null
                                    pendingPictureImageIndex = null
                                },
                                onScheduleRefresh = { scheduleRefreshToken++ },
                                canUploadFiles = canUploadFiles,
                                providedViewModel = picturesViewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                            MoreDestination.QA -> QAAdminScreen(
                                viewModel = qaViewModel,
                                settingsSaveToken = settingsSaveToken,
                                modifier = Modifier.fillMaxSize()
                            )
                            MoreDestination.DICTIONARY -> DictionaryScreen(
                                viewModel = dictionaryViewModel,
                                settingsSaveToken = settingsSaveToken,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Standalone has no desktop schedule to add to, so this
                            // is the local notices list: a notice written in the
                            // Library, put on this device's own outputs.
                            MoreDestination.ANNOUNCEMENTS if appMode == AppMode.STANDALONE ->
                                LocalNoticesScreen(
                                    repository = libraryRepository,
                                    presenter = standaloneEngine,
                                    hasOutput = sinkStatuses.any { it.isAttached },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            MoreDestination.ANNOUNCEMENTS -> AnnouncementsScreen(
                                viewModel = announcementsViewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Standalone has no desktop to hand a link to, so the
                            // page goes on this device's own outputs.
                            MoreDestination.WEB if appMode == AppMode.STANDALONE ->
                                LocalWebScreen(
                                    presenter = standaloneEngine,
                                    // Collected, not read: hasAttachedSink is a plain getter, so
                                    // plugging a screen in never cleared the "no output" warning.
                                    hasOutput = sinkStatuses.any { it.isAttached },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            MoreDestination.CONTACT -> ContactScreen(
                                modifier = Modifier.fillMaxSize()
                            )
                            MoreDestination.WEB -> WebScreen(
                                viewModel = webViewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                            null -> MoreScreen(
                                mode = appMode,
                                onSelect = { moreDestination = it },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        AppTab.LIBRARY -> when {
                            editingSongId != null || creatingSong -> SongEditorScreen(
                                repository = libraryRepository,
                                songId = editingSongId,
                                onClose = { editingSongId = null; creatingSong = false },
                                modifier = Modifier.fillMaxSize()
                            )
                            editingAnnouncementId != null || creatingAnnouncement -> AnnouncementEditorScreen(
                                repository = libraryRepository,
                                announcementId = editingAnnouncementId,
                                onClose = { editingAnnouncementId = null; creatingAnnouncement = false },
                                modifier = Modifier.fillMaxSize()
                            )
                            else -> LibraryScreen(
                                repository = libraryRepository,
                                bibles = bibleRepository,
                                settings = appSettings,
                                sender = projectionRouter,
                                onEditSong = { id ->
                                    editingSongId = id
                                    creatingSong = id == null
                                },
                                onEditAnnouncement = { id ->
                                    editingAnnouncementId = id
                                    creatingAnnouncement = id == null
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            if (showSettings) {
                SettingsScreen(
                    appSettings = appSettings,
                    onContact = {
                        showSettings = false
                        selectedTab = AppTab.MORE
                        moreDestination = MoreDestination.CONTACT
                    },
                    onDismiss = {
                        appSettings.isSetupComplete = true
                        showSettings = false
                    },
                    onSaved = {
                        themeMode = appSettings.themeMode
                        settingsSaveToken++
                        // After the first-ever settings save, guide the user through
                        // QR-scan connect setup if they haven't done it yet.
                        if (!appSettings.isConnectSetupDone) {
                            showSettings = false
                            showConnectSetup = true
                        }
                    }
                )
            }
        } // end ModalNavigationDrawer
    }
}

/**
 * Parses a verse string into a set of 1-based verse numbers.
 *
 * Handles all common formats:
 *   "3"       → {3}
 *   "3-7"     → {3,4,5,6,7}
 *   "3,4,7"   → {3,4,7}
 *   "3-5,7"   → {3,4,5,7}
 *   null/"" → {}
 */
private fun parseVerseString(verseStr: String?): Set<Int> {
    if (verseStr.isNullOrBlank()) return emptySet()
    val result = mutableSetOf<Int>()
    for (token in verseStr.split(",")) {
        val part = token.trim()
        if (part.contains("-")) {
            val sides = part.split("-")
            val start = sides.firstOrNull()?.trim()?.toIntOrNull() ?: continue
            val end   = sides.lastOrNull()?.trim()?.toIntOrNull()  ?: start
            for (v in start..end) result += v
        } else {
            part.toIntOrNull()?.let { result += it }
        }
    }
    return result
}
