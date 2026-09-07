# Agent Development Notes - ChurchPresenter Mobile
read CODING_STANDARDS.md

This document tracks coding standards, common patterns, and debugging notes for the ChurchPresenter Mobile (Kotlin Multiplatform Compose) project.

## Coding Standards

### 1. Import Statements
- ❌ **NEVER** use wildcard imports like `import com.church.presenter.churchpresentermobile.model.*`
- ✅ **ALWAYS** use explicit imports for each class/resource
- ❌ **NEVER** use fully-qualified class names in function signatures
- ✅ **ALWAYS** add an import at the top of the file and use the short name

### 2. API Configuration
- ✅ **ALWAYS** store API URLs in constants
- ✅ **ALWAYS** use `const val` for configuration that doesn't change at runtime
- 📍 **Location**: `network/ApiConstants.kt` or similar
- 🔗 **Current API**: `http://<server-ip>:8765/api/songs`

### 3. Material Design
- ✅ **ALWAYS** use Material 3 (`androidx.compose.material3.*`)
- ❌ **AVOID** mixing Material 2 components
- ✅ **USE** `MaterialTheme.colorScheme.*` for consistent theming

### 4. Icons — **NEVER USE EMOJI OR TEXT AS ICONS**
- ❌ **NEVER** use emoji or Unicode characters as icons (e.g. `Text("⚙")`, `Text("←")`, `Text("🎵")`)
- ❌ **NEVER** use `Text` inside `IconButton` to simulate an icon
- ✅ **ALWAYS** use `Icon(imageVector = Icons.*, contentDescription = "...")` with a real vector asset
- ✅ **ALWAYS** use `Icons.Filled.*`, `Icons.AutoMirrored.Filled.*`, or `Icons.Outlined.*` from `androidx.compose.material.icons`
- 📦 The `compose.materialIconsExtended` dependency is already in `commonMain` — all icons are available

**Example - WRONG**:
```kotlin
IconButton(onClick = { ... }) {
    Text("⚙", fontSize = 20.sp)   // ❌ emoji as icon
}
IconButton(onClick = { ... }) {
    Text("←", fontSize = 20.sp)   // ❌ text character as icon
}
```

**Example - CORRECT**:
```kotlin
IconButton(onClick = { ... }) {
    Icon(
        imageVector = Icons.Filled.Settings,
        contentDescription = "Settings",
        tint = MaterialTheme.colorScheme.onPrimaryContainer
    )
}
IconButton(onClick = { ... }) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Back",
        tint = MaterialTheme.colorScheme.onPrimaryContainer
    )
}
```

### 5. Type Annotations
- ❌ **AVOID**: `width: androidx.compose.ui.unit.Dp = 120.dp`
- ✅ **PREFER**: `width: Dp = 120.dp` (with proper import)

### 5. ViewModel Ownership — **CRITICAL RULE**
- ❌ **NEVER** pass a ViewModel as a parameter to another composable or class
- ❌ **NEVER** return or expose a ViewModel from a class/function
- ✅ **Each composable/screen owns and creates its own ViewModel(s)**
- ✅ **Pass data via StateFlow/Flow, not the ViewModel itself**
- ✅ **Use typed callbacks for actions** instead of passing the ViewModel

**Example - WRONG**:
```kotlin
// WRONG - ViewModel passed as parameter
@Composable
fun SongsScreen(viewModel: SongsViewModel) { ... }

// WRONG - Returning ViewModel
fun getViewModel(): SongsViewModel { ... }
```

**Example - CORRECT**:
```kotlin
// CORRECT - Each screen owns its ViewModel
@Composable
fun SongsScreen() {
    val viewModel: SongsViewModel = viewModel { SongsViewModel() }
    val songs by viewModel.songs.collectAsState()
    
    SongsTable(
        songs = songs,
        onSongSelect = { viewModel.selectSong(it) }
    )
}
```

### 6. Coroutines and Async Operations
- ✅ **ALWAYS** use `viewModelScope.launch` in ViewModels for background work
- ✅ **ALWAYS** use `LaunchedEffect` in Composables for one-time initialization
- ❌ **NEVER** use `GlobalScope.launch` — it leaks memory and ignores lifecycle
- ✅ **ALWAYS** set loading/error states to inform the user

### 7. Error Handling
- ✅ **ALWAYS** catch exceptions and convert to user-friendly error messages
- ✅ **ALWAYS** expose errors via StateFlow so UI can display them
- ❌ **NEVER** let exceptions crash silently
- ✅ **ALWAYS** call `.onFailure { }` on Result types

**Example**:
```kotlin
private val _error = MutableStateFlow<String?>(null)
val error: StateFlow<String?> = _error.asStateFlow()

try {
    val result = songService.getSongs()
    result.onSuccess { songs ->
        _songs.value = songs
    }.onFailure { exception ->
        _error.value = "Failed to load: ${exception.message}"
    }
} finally {
    _isLoading.value = false
}
```

### 8. Network Requests
- ✅ **ALWAYS** use Ktor HttpClient with proper configuration
- ✅ **ALWAYS** use `runCatching` to wrap network calls
- ✅ **ALWAYS** set loading state before making requests
- ✅ **ALWAYS** clear errors when new requests start
- ❌ **NEVER** make untracked network calls (no loading/error state)

### 9. StateFlow and State Management
- ✅ **ALWAYS** use `MutableStateFlow` for mutable state in ViewModels
- ✅ **ALWAYS** expose as `StateFlow` (read-only) via `.asStateFlow()` method
- ✅ **ALWAYS** use `collectAsState()` in Composables to consume StateFlow
- ✅ **ALWAYS** initialize with sensible defaults (empty lists, false, null, etc.)

**Example**:
```kotlin
// In ViewModel
private val _songs = MutableStateFlow<List<Song>>(emptyList())
val songs: StateFlow<List<Song>> = _songs.asStateFlow()

// In Composable
val songs by viewModel.songs.collectAsState()
```

## Project Structure

### Multiplatform Targets
- **Android**: Main platform
- **iOS**: Supported via CocoaPods bridge
- **Web**: JavaScript/WebAssembly support (browser)
- **Common**: Shared Kotlin code in `commonMain/`

### Source Organization
```
composeApp/src/
├── commonMain/
│   └── kotlin/com/church/presenter/churchpresentermobile/
│       ├── model/           # Data models (Song.kt, etc.)
│       ├── network/         # API services (SongService.kt, etc.)
│       ├── viewmodel/       # ViewModels (SongsViewModel.kt, etc.)
│       └── ui/              # Composable UI (SongsTable.kt, etc.)
├── androidMain/             # Android-specific code (if needed)
└── iosMain/                 # iOS-specific code (if needed)
```

### File Naming Convention
- **Models**: `Song.kt`, `Album.kt` (singular, PascalCase)
- **Services**: `SongService.kt`, `BibleService.kt` (singular + "Service")
- **ViewModels**: `SongsViewModel.kt`, `ScheduleViewModel.kt` (plural + "ViewModel")
- **UI Components**: `SongsTable.kt`, `BibleBrowser.kt` (PascalCase, descriptive)
- **Constants**: `ApiConstants.kt`, `UiConstants.kt` (PascalCase + "Constants")

## Current Features

### Songs API Integration (Implemented)
**Status**: ✅ Fully implemented

**Components**:
1. **Song Model** (`model/Song.kt`)
   - `@Serializable` for JSON deserialization
   - Fields: id, title, artist, album, duration, url

2. **SongService** (`network/SongService.kt`)
   - Uses Ktor HttpClient with JSON content negotiation
   - `getSongs()` - Fetches from `GET /api/songs`
   - `selectSong(songId)` - Sends to `POST /api/songs/{id}/select`
   - Proper error handling with `Result<T>` type

3. **SongsViewModel** (`viewmodel/SongsViewModel.kt`)
   - Manages songs list, selected song, loading, and error states
   - Auto-loads songs on init via `LaunchedEffect`
   - Updates states reactively

4. **SongsTable UI** (`ui/SongsTable.kt`)
   - Table format with Title, Artist, Album columns
   - Clickable rows for selection (highlighted in secondary color)
   - Shows loading spinner and error messages
   - Responsive design with overflow handling

**API Endpoints Used**:
- `GET http://<server-ip>:8765/api/songs` — Fetch all songs
- `POST http://<server-ip>:8765/api/songs/{songId}/select` — Select a song

**Dependencies**:
- Ktor Client 3.0.0
- Kotlinx Serialization 1.6.2

### Build Configuration
- **Kotlin**: 2.3.0
- **Compose Multiplatform**: 1.10.0
- **Material3**: 1.10.0-alpha05
- **Lifecycle**: 2.9.6

**Files Modified**:
- `gradle/libs.versions.toml` — Added Ktor and Serialization versions
- `composeApp/build.gradle.kts` — Added serialization plugin and dependencies
- `composeApp/src/commonMain/kotlin/com/church/presenter/churchpresentermobile/App.kt` — Updated to show SongsTable

## Debugging Guidelines

### When Adding Debug Logs
1. ✅ **ALWAYS** keep logs until the fix is confirmed working
2. ✅ **ASK** before removing logs if uncertain whether they're needed
3. ✅ **DOCUMENT** what was learned from the logs in this file

### Common Issues and Solutions

**Issue**: Network requests timeout or fail
- **Check**: Is the API server running at `http://<server-ip>:8765`?
- **Check**: Is the device on the same network as the API server?
- **Fix**: Update `ApiConstants.API_BASE_URL` to correct IP/port
- **Debug**: Add logs in `SongService.getSongs()` to see request/response

**Issue**: Songs don't display in table
- **Check**: Is `getSongs()` being called? Check `SongsViewModel.loadSongs()` logs
- **Check**: Is the API response valid JSON? Log the raw response
- **Check**: Does the JSON structure match `Song` data class?
- **Debug**: Add `ignoreUnknownKeys = true` in JSON parser (already done)

**Issue**: Selected song not sending request to API
- **Check**: Is `selectSong()` being called? Add logs in `SongsViewModel.selectSong()`
- **Check**: Is the POST request being sent? Add logs in `SongService.selectSong()`
- **Check**: Is the API URL correct? Check `ApiConstants.SONG_SELECT_ENDPOINT`
- **Debug**: Print request URL and response status code

**Issue**: Composable not updating when state changes
- **Check**: Is state exposed via `StateFlow`?
- **Check**: Are you using `.collectAsState()` to consume it in Composable?
- **Check**: Is the state being mutated correctly (e.g., `_songs.value = newList`)?
- **Fix**: Make sure every state change is assigned to `.value` property

## Dependencies

### Ktor Client (HTTP Library)
- 📦 **Core**: `io.ktor:ktor-client-core:3.0.0`
- 📦 **Android**: `io.ktor:ktor-client-android:3.0.0`
- 📦 **iOS**: `io.ktor:ktor-client-ios:3.0.0`
- 📦 **JS/Web**: `io.ktor:ktor-client-js:3.0.0`
- 📦 **Negotiation**: `io.ktor:ktor-client-content-negotiation:3.0.0`
- 📦 **JSON**: `io.ktor:ktor-serialization-kotlinx-json:3.0.0`
- ⚠️ **Important**: After adding these, run `./gradlew build` to sync dependencies
- 🔍 **Symptom if missing**: `Unresolved reference: HttpClient` or similar import errors
- ✅ **Solution**: Run Gradle sync

### Kotlinx Serialization (JSON Parser)
- 📦 **Dependency**: `org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2`
- ⚠️ **Important**: After adding, run `./gradlew build` to sync dependencies
- 🔍 **Symptom if missing**: `Unresolved reference: @Serializable` or JSON parsing fails
- ✅ **Solution**: Run Gradle sync and ensure `kotlin("plugin.serialization")` is in `build.gradle.kts`

### Compose (Already configured)
- 📦 **Multiplatform**: 1.10.0
- 📦 **Material3**: 1.10.0-alpha05
- 📦 **Lifecycle**: 2.9.6

## Platform-Specific Notes

### Android
- Min SDK: 24
- Target SDK: 36
- Compile SDK: 36
- Ktor uses `HttpClientAndroid` engine (already configured)

### iOS
- Targets: ARM64 + Simulator ARM64
- Framework: `ComposeApp` (static)
- Ktor uses `HttpClientIos` engine (already configured)

### Web (JavaScript)
- Browser target with WebAssembly support
- Ktor uses `HttpClientJs` engine
- May have CORS restrictions — ensure backend allows cross-origin requests

## Testing Notes

### Coverage: NEVER lower the bar to meet the code — **ASK FIRST**

Coverage is measured with **JaCoCo** (`./gradlew :composeApp:jacocoTestReport`),
over the Android unit-test run. Report: `composeApp/build/reports/jacoco/jacocoTestReport/html/index.html`.

- 🚫 **NEVER** change the coverage floor (the `LINE` `minimum` in
  `jacocoTestCoverageVerification`, `composeApp/build.gradle.kts`)
  without explicit permission. Not to make a build green, not "temporarily".
- 🚫 **NEVER** add an `exclude(...)` to `jacocoTestReport`'s `classDirectories`
  without explicit permission — no packages, no class patterns.
- ✅ **ALWAYS** raise the number by adding tests. If coverage is below the floor,
  the answer is more tests or a conversation — never a wider exclusion.
- 📉 **Why**: the exclusion list once hid 64% of the source, and the reported figure
  was 91% over 3,292 hand-picked lines. Measured honestly over the whole module the
  same suite gives ~29% of 15,688 lines. An exclusion does not improve anything; it
  only stops the number describing the code.
- 💬 If a gate genuinely cannot be met, say so and let the owner decide. Do not
  quietly adjust the gate.

### Unit Testing
- Tests go in `composeApp/src/commonTest/kotlin/`
- Use existing test framework setup
- Mock `SongService` for ViewModel tests

### Reach for a seam before declaring code untestable

If something cannot be tested as written, **change the code so it can be** —
widening visibility or extracting the logic is nearly always the right trade.

- ✅ **PREFER** `internal` over `private` when a test needs to reach it. `internal`
  is still invisible to the rest of the app, and the test source set already sees it.
- ✅ **PREFER** pulling the decision out of the plumbing: move the part that can be
  wrong into an `internal fun` that takes plain values and returns a plain value,
  and leave the untestable framework call as a one-line caller of it.
- ✅ **PREFER** injecting a collaborator with a default — the `serviceFactory` /
  `uploadClient` / `WsSender` seams already in this codebase — over reaching for a
  mocking framework.
- 📈 **Why**: covering thirty lines of real logic and leaving one framework call
  uncovered beats leaving all thirty-one uncovered because the last one needs an
  emulator. The line you cannot reach is usually `manager.notify(...)` or
  `Bitmap.createScaledBitmap(...)` — plumbing, not behaviour.
- 💬 Say so in a KDoc when visibility was widened purely for a test
  (see `FirebasePushService`), so the next reader does not "tidy" it back.
- ✅ **MockK is available** in `androidUnitTest` (`libs.mockk`) for what no seam can
  reach: the final, inert framework classes the stub `android.jar` leaves behind —
  `PowerManager`, `WifiManager`, `BitmapFactory`, `RemoteMessage`,
  `FirebaseRemoteConfig`, the Play Core `Task` API. `spyk(service,
  recordPrivateCalls = true)` also lets a real method run with only its one
  untestable call stubbed out. Unlike Robolectric it instruments **only** the
  classes it is asked to mock, so the code under test still reports real JaCoCo
  coverage. Not available in `commonTest` — it is JVM-only.
- ❌ **STILL NEVER** reach for Robolectric to make a test run, and never widen the
  coverage exclusions or lower the floor instead (see the section above).

**Example** — `android.jar` is stubbed in unit tests, so a `Notification.Builder`
chain cannot run. Extracting the choice it encodes still covers the behaviour:

```kotlin
// Untestable: needs a real Context and a real Builder.
private fun buildNotification(url: String?): Notification = ...

// Testable: the decision the notification actually encodes.
internal fun notificationText(url: String?, fallback: String): String =
    url?.takeIf { it.isNotBlank() } ?: fallback
```

### Compose UI tests: assert what the operator can see and do

A Compose test that only checks a screen renders is a coverage figure, not a
test. Assert the things a change can actually break:

- 📝 **Text** — the words on screen, including the ones derived from data
  (`song.displayTitle`, a notice's first line). `onNodeWithText`, `hasText`.
- 👁️ **Visibility** — that a row, hint or dialog is *there*, and equally that it
  is **not**: an empty-state hint with content behind it, a confirmation that
  never appeared, a chip shown for a feature the device does not have.
- 🔤 **Content description** — every `Icon` has one (see the icons rule above),
  and it is the only label a screen-reader user gets.
  `onNodeWithContentDescription`.
- 👆 **Click actions** — that tapping reaches the right callback *with the right
  argument*. Two rows on screen and the callback carrying the first one's id is
  a real bug that a "does it render" test never sees.
- 🎚️ **State** — enabled/disabled (`assertIsEnabled`, `assertIsNotEnabled`),
  selected, checked, expanded. A disabled `Modifier.clickable` **still publishes
  a click action**, so counting clickable nodes cannot tell you whether a button
  is pressable — read the node's state.
- 🔁 **The effect** — where a screen writes through to a repository or a sender,
  assert on *that*, not on the composable. "The song is gone from the
  repository" beats "a dialog closed".

❌ **Do NOT** assert on pixels, colour, spacing or font in these tests. Layout
and appearance belong to screenshot testing (Roborazzi on Android,
Paparazzi for JVM-rendered previews); neither is set up here yet, and a
hand-written assertion about a `dp` value is a test that fails on every design
tweak while catching nothing.

### Sample Compose UI test

```kotlin
class MyComposeTest {

    @get:Rule val composeTestRule = createComposeRule()
    // use createAndroidComposeRule<YourActivity>() if you need access to
    // an activity

    @Test
    fun myTest() {
        // Start the app
        composeTestRule.setContent {
            MyAppTheme {
                MainScreen(uiState = fakeUiState, /*...*/)
            }
        }

        composeTestRule.onNodeWithText("Continue").performClick()

        composeTestRule.onNodeWithText("Welcome").assertIsDisplayed()
    }
}
```

### Selecting nodes in a Compose test — tag it, don't count it

- ✅ **PREFER a test tag.** Add `Modifier.testTag(...)` to the production
  composable and select with `onNodeWithTag`. Tags are named in one place — see
  `ui/library/LibraryTags.kt`, an `internal object` referenced from both
  sides so a rename cannot drift. A tag is a semantics property; it costs
  nothing at runtime and ships in no view.
- ✅ **Tag per item, not per screen**: `LibraryTags.rowDelete(song.id)` lets a
  test name the row it means, which is how "the callback carried the wrong id"
  becomes catchable.
- ✅ **`performScrollToNode(hasTestTag(...))`** reaches a `LazyColumn` item that
  has not been composed yet. Indices into the semantics tree cannot.
- ✅ **Content the operator typed** — song titles, notice bodies — is matched by
  its **text**, since it is plain data and needs no tag.
- ⚠️ **A tag on a wrapper is not a tag on the text field.** `EditorField` and
  `SearchField` put the caller's modifier on their outer layout, so the
  focus/set-text actions live on a *descendant*. Match
  `hasSetTextAction() and hasAnyAncestor(hasTestTag(tag))` — `LibraryHarness.textField`
  does this.
- ❌ **NEVER select by index** (`onAllNodes(hasClickAction())[3]`). It turns any
  layout change into a failure somewhere unrelated, and it silently starts
  pointing at a different control.
- 🌐 **On wasmJs, `stringResource` renders empty** — compose-resources does not
  resolve in that test runtime, and its bundle loads on a real dispatcher that
  the test clock cannot advance, so waiting does not help. That is *why* labels
  are unusable as selectors here, and why tags are not optional.
- 💥 **…which also means such a control has *zero width*.** A text-only button
  whose label is a `stringResource` lays out empty, so `performClick()` — which
  taps the centre of the node's bounds — lands outside it and silently does
  nothing. That looks exactly like a handler which was never wired up, and cost
  an hour to diagnose. Press with the semantics action instead:

  ```kotlin
  internal fun ComposeUiTest.click(tag: String) =
      tagged(tag).performSemanticsAction(SemanticsActions.OnClick)
  ```

- ⚠️ **The semantics action ignores `enabled`.** It invokes the handler
  directly, so it will "press" a disabled button. To assert something is *not*
  pressable, read the state — `assertIsNotEnabled()` — never try to press it and
  check nothing happened.

### A slow or flaky test is a broken test — fix it when you see it

- ⏱️ **NEVER** leave a test that takes tens of seconds. The suite is run on every
  change; a minute spent waiting is a minute nobody spends reading the failure.
  If a test is slow, find out why before writing another one.
- 🎲 **NEVER** leave a test that passes intermittently. A flaky test is worse than
  no test: it trains everyone to re-run the build instead of reading it.
- ✅ **PREFER** virtual time. `runTest` fakes `delay` and `withTimeout`, so a
  ten-second production timeout costs nothing — see `ServerEventServiceTest`,
  which exercises the three-attempt retry ladder in microseconds.
- ✅ **When a test genuinely needs real time** (a real socket, a real server —
  `runTest`'s clock would expire the timeout before the handshake), use
  `runBlocking` **and give every test a hard budget**, so a stall fails fast
  instead of blocking the suite. `ServerEventServiceLiveTest.liveTest` is the
  pattern:

  ```kotlin
  private fun liveTest(block: suspend CoroutineScope.() -> Unit) =
      runBlocking { withTimeout(TEST_BUDGET_MS) { block() } }
  ```

- ✅ **ALWAYS** await a condition rather than sleeping for one: `flow.first { … }`,
  not `delay(2_000)`. A sleep is either too short (flaky) or too long (slow), and
  usually both on different machines.
- 🔍 **Where to look**: `build/test-results/*/*.xml` carries a `time` per test.
  Sort by it after any change that adds real I/O.
- 📉 **Worked example**: two tests in `ServerEventServiceLiveTest` waited out the
  send path's three ten-second connection timeouts — 60 of the class's 70
  seconds, which read as a hang. Both were already covered on virtual time in
  `ServerEventServiceTest`, so they were deleted and every remaining test given a
  20-second budget: **70s → 6s**.
- ❌ **NEVER** paper over flakiness with a retry, a `Thread.sleep`, or a longer
  timeout. Find the race — it is usually a missing await on the *other* side, as
  with the fake desktop's connection counter, which the client's own "connected"
  flag can beat.

### Where each kind of test runs

| Kind | Source set | Task |
|---|---|---|
| Logic / ViewModel / service | `commonTest` | `:composeApp:jsBrowserTest` (the CI gate) |
| Compose UI (`runComposeUiTest`) | `wasmJsTest` | `:composeApp:wasmJsBrowserTest` |
| Coverage measurement | — | `:composeApp:jacocoTestReport` (Android unit-test JVM) |

- **Compose UI tests must live in `wasmJsTest`, not `commonTest`.** They need a Skia
  surface; the js (legacy) Karma runtime has none and fails with
  `org_jetbrains_skia_Surface__1nMakeRasterN32Premul is not defined`. The wasmJs
  target ships skiko with its test bundle and runs them unchanged.
- ❌ **NEVER** add Robolectric to run Compose tests. Compose Multiplatform does not
  need it, and Robolectric's instrumenting classloader defeats JaCoCo — the tests
  pass while the code they exercise still reports 0% covered.
- ⚠️ JaCoCo measures the **Android unit-test JVM**, so Compose UI tests on wasmJs
  prove behaviour but do not move the coverage figure. Don't expect them to.
- ⚠️ A ViewModel test that creates a ViewModel inside `runVmTest` must
  `tearDown(vm)` in a `finally` (see `testutil/CoroutineTest.kt`). Without it a
  coroutine can resume after `resetMain()` and fail an unrelated later test with
  "Dispatchers.Main was accessed when the platform dispatcher was absent".
  `SettingsViewModelTest`, `BibleViewModelTest` and `LocalNoticesViewModelTest`
  still need this fix; until then they are named in the Android test filter.

### Manual Testing on Android
1. Build: `./gradlew build`
2. Run: `./gradlew installDebug && adb shell am start -n com.church.presenter.churchpresentermobile/.MainActivity`
3. Check: Open Settings → Server on desktop app, verify IP and port
4. Verify: Navigate to Songs tab on mobile, should load and display table

### Manual Testing on iOS (via Xcode)
1. Open `iosApp/iosApp.xcworkspace` in Xcode  ← **always use .xcworkspace, not .xcodeproj** (CocoaPods requires it)
2. Select `iosApp` scheme and device/simulator
3. Build and run (⌘R)
4. Verify songs display in table format

## Performance Tips

### Network Performance
- Cache song data locally if needed (out of scope for now)
- Implement pagination for large song lists (future enhancement)
- Use GZIP compression (Ktor supports automatically)

### UI Performance
- Use `LazyColumn` for large lists (already done in `SongsTable`)
- Avoid recomposing entire table on minor changes
- Use `key {}` for list items if available in future

## Future Enhancements

- [ ] Add pagination support for large datasets
- [ ] Add search/filter functionality
- [ ] Add sorting by Title, Artist, or Album
- [ ] Add refresh button to manually reload songs
- [ ] Add local caching of song list
- [ ] Add duration formatting (seconds → MM:SS)
- [ ] Add confirmation dialog before selecting a song
- [ ] Add analytics for song selections
- [ ] Support WebSocket for real-time schedule updates (from ChurchPresenter desktop)
- [ ] Add Bible browsing feature (similar to Songs)
- [ ] Add schedule viewing feature

## Common Git Commands

```bash
# View recent changes
git log --oneline -10

# Check what changed
git status

# Add files
git add .

# Commit changes
git commit -m "Add songs API integration"

# Push to remote
git push

# Pull latest
git pull
```

## Resources

- **Ktor Documentation**: https://ktor.io/docs/client.html
- **Compose Multiplatform**: https://www.jetbrains.com/help/kotlin-multiplatform-dev/
- **Kotlinx Serialization**: https://github.com/Kotlin/kotlinx.serialization
- **Material3 Docs**: https://developer.android.com/develop/ui/compose/designsystems/material3
- **Android Docs**: https://developer.android.com/docs

## Document Purpose

This is an **AGENT-ONLY** document for AI agents working on ChurchPresenter Mobile. It helps:
- Remember coding standards across sessions
- Avoid repeating mistakes
- Document solutions for similar issues
- Track implementation progress and status

The user should not need to read this document, but it's available as a reference if needed.

## Related Projects

- **ChurchPresenter Desktop**: https://github.com/ChurchPresenter/ChurchPresenter
  - Contains backend server (`CompanionServer.kt`)
  - Hosts Songs, Bible, and Schedule APIs
  - Server runs on port 8765 by default

- **This Mobile App**: ChurchPresenterMobile
  - Kotlin Multiplatform Compose client
  - Connects to ChurchPresenter Desktop companion API
  - Supports Android, iOS, and Web platforms

