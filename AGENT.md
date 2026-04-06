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

### Unit Testing
- Tests go in `composeApp/src/commonTest/kotlin/`
- Use existing test framework setup
- Mock `SongService` for ViewModel tests

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

