package com.church.presenter.churchpresentermobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.app_title
import churchpresentermobile.composeapp.generated.resources.bible_chapter_label
import churchpresentermobile.composeapp.generated.resources.deep_link_connected
import churchpresentermobile.composeapp.generated.resources.schedule_drawer_open
import churchpresentermobile.composeapp.generated.resources.tab_bible
import churchpresentermobile.composeapp.generated.resources.tab_pictures
import churchpresentermobile.composeapp.generated.resources.tab_presentation
import churchpresentermobile.composeapp.generated.resources.tab_songs
import coil3.request.crossfade
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.AppTab
import com.church.presenter.churchpresentermobile.model.BibleBook
import com.church.presenter.churchpresentermobile.network.createImageHttpClient
import androidx.lifecycle.viewmodel.compose.viewModel
import com.church.presenter.churchpresentermobile.ui.BibleScreen
import com.church.presenter.churchpresentermobile.ui.PicturesScreen
import com.church.presenter.churchpresentermobile.ui.PresentationScreen
import com.church.presenter.churchpresentermobile.ui.ScheduleDrawerContent
import com.church.presenter.churchpresentermobile.ui.SettingsScreen
import com.church.presenter.churchpresentermobile.ui.SongsTable
import com.church.presenter.churchpresentermobile.ui.SplashScreen
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import com.church.presenter.churchpresentermobile.util.isDebugBuild
import com.church.presenter.churchpresentermobile.util.RemoteConfig
import com.church.presenter.churchpresentermobile.util.RemoteConfigDefaults
import com.church.presenter.churchpresentermobile.util.RemoteConfigKeys
import com.church.presenter.churchpresentermobile.util.Logger
import com.church.presenter.churchpresentermobile.viewmodel.BibleViewModel
import com.church.presenter.churchpresentermobile.viewmodel.PicturesViewModel
import com.church.presenter.churchpresentermobile.viewmodel.SongsViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    val appSettings = remember { AppSettings() }

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

    // ── Session-scoped ViewModels ─────────────────────────────────────────
    // Created ONCE here in App() which is never removed from the composition.
    // This guarantees the loaded book/song lists survive tab switches forever —
    // no data is ever discarded just because the user navigated to another tab.
    val bibleViewModel: BibleViewModel = viewModel(key = "bible_$isDemoMode") {
        BibleViewModel(appSettings, isDemoMode)
    }
    val songsViewModel: SongsViewModel = viewModel(key = "songs_$isDemoMode") {
        SongsViewModel(appSettings, isDemoMode)
    }
    val picturesViewModel: PicturesViewModel = viewModel(key = "pictures_$isDemoMode") {
        PicturesViewModel(appSettings, isDemoMode)
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
    var showSettings by remember { mutableStateOf(false) }

    // Apply deep-linked settings and notify screens to reload.
    // deepLinkConnectedMsg is evaluated in composable scope so it picks up the
    // fresh host/port values written by DeepLinkHandler before recomposition.
    val deepLinkConnectedMsg = stringResource(Res.string.deep_link_connected,
        appSettings.host, appSettings.port.toString())
    LaunchedEffect(deepLinkCount) {
        if (deepLinkCount > 0) {
            settingsSaveToken++
            showSettings = true   // open settings so the user sees the applied values
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

    val tabs = AppTab.entries

    // Seed the initial tab from any pending shortcut/quick-action so that
    // rememberPagerState starts on the correct page immediately.  Without this,
    // the snapshotFlow's first emission (settledPage == 0) races against the
    // LaunchedEffect(shortcutTab) and can reset navigation back to Songs.
    val initialTab = TabNavigationHandler.requestedTab.value ?: AppTab.SONGS
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
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
        initialPage = tabs.indexOf(selectedTab),
        pageCount = { tabs.size }
    )

    // Swipe → update selected tab (settledPage avoids emitting intermediate pages
    // during programmatic animateScrollToPage, which would fight the animation).
    // drop(1) skips the initial emission whose value always matches the already-
    // correct selectedTab; without it the emission races against a pending
    // shortcut navigation and resets the tab back to Songs.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.drop(1).collect { page ->
            selectedTab = tabs[page]
        }
    }

    // Tab click → animate pager to matching page
    LaunchedEffect(selectedTab) {
        val targetPage = tabs.indexOf(selectedTab)
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // ── Detail-screen flags (hoisted so both toolbar and pager can read them)
    val inSongDetail = selectedTab == AppTab.SONGS && songDetailTitle != null
    val inBibleDetail = selectedTab == AppTab.BIBLE && bibleBook != null

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

    AppTheme(themeMode = themeMode) {
        if (showSplash) {
            SplashScreen(onComplete = { showSplash = false })
            return@AppTheme
        }
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ScheduleDrawerContent(
                    appSettings = appSettings,
                    isDemoMode = isDemoMode,
                    settingsSaveToken = settingsSaveToken,
                    scheduleRefreshToken = scheduleRefreshToken,
                                    onItemClick = { item ->
                                        coroutineScope.launch {
                                            drawerState.close()
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
                                                    selectedTab              = AppTab.PICTURES
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
                                            }
                                        }
                                    }
                )
            }
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    Column {
                        val chapterLabel = stringResource(Res.string.bible_chapter_label)
                        val appTitle = stringResource(Res.string.app_title)

                        TopAppBar(
                            title = {
                                when {
                                    inSongDetail -> Column {
                                        Text(
                                            text = songDetailTitle!!,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!songDetailBookName.isNullOrBlank()) {
                                            Text(
                                                text = songDetailBookName!!,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                            )
                                        }
                                    }
                                    inBibleDetail -> Text(
                                        text = if (bibleChapter != null)
                                            "${bibleBook!!.displayName}  ›  $chapterLabel $bibleChapter"
                                        else
                                            bibleBook!!.displayName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    else -> Text(text = appTitle, maxLines = 1)
                                }
                            },
                            navigationIcon = {
                                when {
                                    inSongDetail -> IconButton(onClick = { songNavigateBack?.invoke() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    inBibleDetail -> IconButton(onClick = { bibleNavigateBack?.invoke() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    else -> IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                        Icon(
                                            imageVector = Icons.Filled.Menu,
                                            contentDescription = stringResource(Res.string.schedule_drawer_open),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            },
                            actions = {
                                if (!inSongDetail && !inBibleDetail) {
                                    IconButton(onClick = { showSettings = true }) {
                                        Icon(
                                            imageVector = Icons.Filled.Settings,
                                            contentDescription = "Settings",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )

                        // Tab row — hidden while drilling into a detail screen
                        if (!inSongDetail && !inBibleDetail) {
                            val tabLabels = listOf(
                                stringResource(Res.string.tab_songs),
                                stringResource(Res.string.tab_bible),
                                stringResource(Res.string.tab_pictures),
                                stringResource(Res.string.tab_presentation)
                            )
                            // Use pagerState.currentPage so the indicator tracks mid-swipe
                            ScrollableTabRow(
                                selectedTabIndex = pagerState.currentPage,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                edgePadding = 0.dp
                            ) {
                                tabs.forEachIndexed { index, tab ->
                                    Tab(
                                        selected = pagerState.currentPage == index,
                                        onClick = { selectedTab = tab },
                                        text = {
                                            Text(
                                                text = tabLabels[index],
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        unselectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                // Swipe-between-tabs — swiping is locked while inside a detail screen
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !inSongDetail && !inBibleDetail,
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    beyondViewportPageCount = 0  // only compose the visible page
                ) { page ->
                    when (tabs[page]) {
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
                        AppTab.PICTURES -> PicturesScreen(
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
                            providedViewModel = picturesViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                        AppTab.PRESENTATION -> PresentationScreen(
                            appSettings = appSettings,
                            isDemoMode = isDemoMode,
                            settingsSaveToken = settingsSaveToken,
                            imageLoader = imageLoader,
                            pendingNavPresentationId = pendingPresentationId,
                            onPendingNavHandled = { pendingPresentationId = null },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            if (showSettings) {
                SettingsScreen(
                    appSettings = appSettings,
                    onDismiss = { showSettings = false },
                    onSaved = {
                        // Pick up the new theme immediately
                        themeMode = appSettings.themeMode
                        // Signal every screen to rebuild its service and reload data
                        settingsSaveToken++
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
