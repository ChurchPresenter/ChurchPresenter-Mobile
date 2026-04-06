# Songs API Integration Feature

## Overview
This feature integrates with the Songs API at `http://<server-ip>:8765/api/songs` to display song data in a table format with the ability to select songs and send requests back to the API.

## Architecture

### Components

1. **Song Model** (`model/Song.kt`)
   - Represents a song with properties: id, title, artist, album, duration, url
   - Uses `@Serializable` annotation for JSON deserialization

2. **SongService** (`network/SongService.kt`)
   - Handles HTTP communication with the API using Ktor Client
   - Methods:
     - `getSongs()`: Fetches list of songs from the API
     - `selectSong(songId)`: Sends a selection request for a specific song

3. **SongsViewModel** (`viewmodel/SongsViewModel.kt`)
   - Manages UI state using StateFlow
   - Properties:
     - `songs`: List of fetched songs
     - `selectedSong`: Currently selected song
     - `isLoading`: Loading state indicator
     - `error`: Error messages
   - Functions:
     - `loadSongs()`: Triggers API call to fetch songs
     - `selectSong(song)`: Selects a song and sends request to API

4. **SongsTable UI** (`ui/SongsTable.kt`)
   - Displays songs in a table format with columns: Title, Artist, Album
   - Shows loading state and error messages
   - Handles item selection with visual feedback (highlighted row for selected item)
   - Clickable rows trigger song selection

## Dependencies Added

- **Ktor Client** (3.0.0)
  - `ktor-client-core`: Core HTTP client
  - `ktor-client-android`: Android platform implementation
  - `ktor-client-contentNegotiation`: Content negotiation support
  - `ktor-serialization-json`: JSON serialization
  
- **Kotlinx Serialization** (1.6.2)
  - JSON serialization/deserialization support

## Usage

### Fetching and Displaying Songs
The app automatically loads songs when it starts. The `SongsTable` composable displays:
- A loading indicator while fetching data
- Error messages if the request fails
- A table with all songs, showing title, artist, and album

### Selecting a Song
Click on any row to select it:
- The selected row is highlighted with a different background color
- A POST request is sent to `http://<server-ip>:8765/api/songs/{songId}/select`
- The selected song is tracked in the `selectedSong` state

## API Endpoints

### GET /api/songs
Returns a list of songs in JSON format.

**Expected Response Format:**
```json
[
  {
    "id": "song-1",
    "title": "Song Title",
    "artist": "Artist Name",
    "album": "Album Name",
    "duration": 300,
    "url": "https://example.com/song.mp3"
  }
]
```

### POST /api/songs/{songId}/select
Sends a selection request for a specific song.

**Parameters:**
- `songId`: The ID of the song to select

## Features

- ✅ Automatic song fetching on app launch
- ✅ Table format display with Title, Artist, Album columns
- ✅ Loading and error state handling
- ✅ Song selection with visual feedback
- ✅ API request on song selection
- ✅ Multiplatform support (Android, iOS, Web, etc.)
- ✅ Responsive UI using Compose

## Error Handling

The app handles various error scenarios:
- Network failures
- JSON parsing errors
- API request errors
- All errors are displayed to the user in the UI

## Future Enhancements

- Add pagination support for large datasets
- Add search/filter functionality
- Add sorting by different columns
- Add refresh button to manually reload songs
- Add more song metadata (duration formatting, etc.)
- Add confirmation dialogs before selection
- Add analytics for song selections

