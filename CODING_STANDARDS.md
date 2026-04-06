# Coding Standards for ChurchPresenter Mobile

## Import Guidelines

### ✅ DO: Use proper imports
```kotlin
import com.church.presenter.churchpresentermobile.model.Song
import com.church.presenter.churchpresentermobile.viewmodel.SongsViewModel
import androidx.compose.material3.Button

fun displaySong(song: Song, viewModel: SongsViewModel) { ... }
```

### ❌ DON'T: Use fully qualified class names
```kotlin
// WRONG - Don't do this!
fun displaySong(
    song: com.church.presenter.churchpresentermobile.model.Song,
    viewModel: com.church.presenter.churchpresentermobile.viewmodel.SongsViewModel
) { ... }
```

### Import Style Rules

1. **Always import classes explicitly** - Add the proper import statement at the top of the file
2. **Never use wildcard imports** - Avoid `import com.church.presenter.churchpresentermobile.model.*`
3. **Remove unused imports** - Clean up imports that are no longer needed
4. **Group imports properly** - Keep imports organized by package:
   - Kotlin/Java standard library first
   - Third-party libraries (Compose, Ktor, etc.)
   - Project packages last

## String Resources

### ✅ DO: Use string resources for user-facing text (when available)
```kotlin
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.loading

Text(stringResource(Res.string.loading))
```

### ✅ DO: Use constants for configuration and API-related strings
```kotlin
object ApiConstants {
    const val API_BASE_URL = "http://<server-ip>:8765/api"
    const val SONGS_ENDPOINT = "songs"
    const val SONG_SELECT_ENDPOINT = "select"
}
```

### ❌ DON'T: Use hard-coded strings in critical paths
```kotlin
// WRONG - Don't do this!
val url = "http://<server-ip>:8765/api/songs"  // Hard-coded URL scattered in code
```

## Type Declarations

### ✅ DO: Use proper type imports for Compose
```kotlin
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Modifier

width: Dp = 120.dp
modifier: Modifier = Modifier.padding(16.dp)
```

### ❌ DON'T: Use fully qualified types
```kotlin
// WRONG - Don't do this!
width: androidx.compose.ui.unit.Dp = 120.dp
modifier: androidx.compose.foundation.layout.Modifier = Modifier
```

## Material Design

### ✅ DO: Always use Material3
```kotlin
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
```

### ❌ DON'T: Mix Material2 and Material3
```kotlin
// WRONG - Don't do this!
import androidx.compose.material.Button // Material2
import androidx.compose.material3.Text  // Material3
```

## ViewModel Ownership — **CRITICAL RULE**

### ✅ DO: ViewModels own their own state and data fetching
```kotlin
// CORRECT - ViewModel manages its own data
class SongsViewModel : ViewModel() {
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()
    
    fun loadSongs() { ... }
    fun selectSong(song: Song) { ... }
}

// CORRECT - Composable creates and owns its ViewModel
@Composable
fun SongsScreen() {
    val viewModel: SongsViewModel = viewModel { SongsViewModel() }
    val songs by viewModel.songs.collectAsState()
    
    SongsTable(songs = songs, onSongSelect = { viewModel.selectSong(it) })
}
```

### ❌ DON'T: Pass ViewModels between composables or screens
```kotlin
// WRONG - ViewModel passed as parameter
@Composable
fun SongsScreen(viewModel: SongsViewModel) { ... }

// WRONG - Returning ViewModel from function
fun getViewModel(): SongsViewModel { ... }

// WRONG - Storing ViewModel in external variable
var globalViewModel = songsViewModel
```

### ❌ DON'T: Pass ViewModel out via callbacks
```kotlin
// WRONG - Exposing ViewModel through callback
onViewModelReady: (SongsViewModel) -> Unit

// WRONG - Storing reference outside original scope
externalRef = songsViewModel
```

### ✅ DO: Pass data via StateFlow and typed callbacks
```kotlin
// CORRECT - Pass data, not ViewModels
@Composable
fun SongsTable(
    songs: List<Song>,
    selectedSong: Song? = null,
    onSongSelect: (Song) -> Unit
) { ... }

// CORRECT - Use StateFlow for reactive data
val selectedSong: StateFlow<Song?> = _selectedSong.asStateFlow()
```

**Why**: Each composable should only manage its own ViewModel. Passing ViewModels creates tight coupling, breaks testability, and can cause memory leaks.

## Coroutines and Async Operations

### ✅ DO: Use viewModelScope for ViewModel operations
```kotlin
class SongsViewModel : ViewModel() {
    fun loadSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = songService.getSongs()
                result.onSuccess { songs ->
                    _songs.value = songs
                }.onFailure { error ->
                    _error.value = error.message
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}
```

### ✅ DO: Use LaunchedEffect for one-time operations in Composables
```kotlin
@Composable
fun SongsScreen() {
    val viewModel: SongsViewModel = viewModel { SongsViewModel() }
    
    LaunchedEffect(Unit) {
        viewModel.loadSongs()
    }
    
    SongsTable(viewModel = viewModel)
}
```

### ❌ DON'T: Launch coroutines without a lifecycle scope
```kotlin
// WRONG - Coroutine will leak if composable is disposed
GlobalScope.launch { ... }

// WRONG - Manual coroutine without proper scope
Thread { ... }.start()
```

## Network Requests

### ✅ DO: Handle errors gracefully and show to user
```kotlin
private val _error = MutableStateFlow<String?>(null)
val error: StateFlow<String?> = _error.asStateFlow()

try {
    val result = songService.getSongs()
    result.onSuccess { ... }
    result.onFailure { exception ->
        _error.value = "Failed to load songs: ${exception.message}"
    }
} catch (e: Exception) {
    _error.value = "Network error: ${e.message}"
}
```

### ✅ DO: Show loading states
```kotlin
private val _isLoading = MutableStateFlow(false)
val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

// In UI
if (isLoading) {
    CircularProgressIndicator()
} else {
    SongsTable(songs = songs)
}
```

### ❌ DON'T: Silently fail or crash on network errors
```kotlin
// WRONG - No error handling
val songs = songService.getSongs().getOrNull() ?: emptyList()

// WRONG - Crashes the app
val songs = songService.getSongs().getOrThrow()
```

## Code Organization

### File Structure
```
commonMain/kotlin/com/church/presenter/churchpresentermobile/
├── model/              # Data models (Song, etc.)
├── network/            # API services (SongService, etc.)
├── viewmodel/          # ViewModels (SongsViewModel, etc.)
└── ui/                 # Composable UI components
```

### Clean Up Unused Code
- Remove unused imports regularly
- Delete commented-out code
- Remove unused functions, variables, and properties

### Avoid Code Duplication
- Extract reusable components into separate functions
- Move shared code into utility files
- Use composition over copy-paste

## Documentation

### Document Public APIs
```kotlin
/**
 * Displays a table of songs with selection capability.
 *
 * @param songs List of songs to display
 * @param selectedSong Currently selected song, highlighted in table
 * @param onSongSelect Called when user clicks a song row
 * @param modifier The modifier to apply to this composable
 */
@Composable
fun SongsTable(
    songs: List<Song>,
    selectedSong: Song? = null,
    onSongSelect: (Song) -> Unit,
    modifier: Modifier = Modifier
) { ... }
```

### Document ViewModel functions
```kotlin
/**
 * Loads songs from the API and updates the UI state.
 * 
 * Sets [isLoading] to true during the request and updates [songs]
 * or [error] based on the result.
 */
fun loadSongs() { ... }
```

## Best Practices Summary

1. **Use proper imports** - Never fully qualify class names
2. **Use constants** - For URLs, keys, and configuration values
3. **Use Material3** - Consistently across the entire app
4. **Own your ViewModels** - Each composable manages only its own ViewModel
5. **Handle errors** - Always show meaningful error messages
6. **Show loading states** - Keep users informed during async operations
7. **Clean code** - Remove unused code and imports
8. **Avoid duplication** - Extract and reuse common code
9. **Document** - Add documentation for public APIs
10. **Organize** - Keep files and code well-structured

---

**Remember**: Code is read more often than it's written. Make it clean, consistent, and easy to understand!

