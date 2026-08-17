package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.LocalSong
import com.church.presenter.churchpresentermobile.model.LocalSongAdapter
import com.church.presenter.churchpresentermobile.model.SlideDeck
import com.church.presenter.churchpresentermobile.model.SlideDeckBuilder
import com.church.presenter.churchpresentermobile.model.Song
import com.church.presenter.churchpresentermobile.model.SongDetail
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

private const val TAG = "SongCatalog"

/**
 * The read side of the songs feature, so [SongCatalog] can be tested — and
 * satisfied by the library — without an HTTP client.
 *
 * [SongService] already implements these two methods; it is declared here as a
 * seam rather than depending on the concrete service.
 */
interface SongReader {
    suspend fun getSongs(): Result<List<Song>>
    suspend fun getSongDetail(
        number: String,
        bookName: String? = null,
        songId: Int = -1,
        title: String? = null,
    ): Result<SongDetail>
}

/** A song's lyrics together with the deck built from them. */
data class LoadedSong(val detail: SongDetail, val deck: SlideDeck)

/**
 * Where song content comes from, decided per call by the current [AppMode].
 *
 * This is the content twin of
 * [com.church.presenter.churchpresentermobile.present.ProjectionRouter]: actions
 * were already routed by mode, but the two HTTP *reads* were not, so standalone
 * — which by definition has no desktop — kept asking a machine that wasn't there
 * and showed "Failed to load songs" on a tab the operator may never have opened.
 *
 * The mode is read on each call rather than captured, because the operator can
 * switch modes in Settings without the app restarting.
 *
 * @param library The on-device library. Null means remote-only, which is the
 *   right default for previews and for tests that predate standalone.
 */
class SongCatalog(
    private val mode: StateFlow<AppMode>,
    private val remote: SongReader,
    private val library: LibraryRepository? = null,
) {
    /** True when songs are being served from this device rather than a desktop. */
    val isLocal: Boolean
        get() = mode.value == AppMode.STANDALONE && library != null

    /**
     * [isLocal] as a stream, for UI that must follow a mode switch made in
     * Settings without the screen being rebuilt.
     */
    val isLocalSource: Flow<Boolean> =
        mode.map { it == AppMode.STANDALONE && library != null }

    /**
     * The song list.
     *
     * An empty library is a legitimate answer, never a failure — that is what
     * replaces the error banner with an ordinary empty state.
     */
    suspend fun list(): Result<List<Song>> {
        val local = library.takeIf { isLocal } ?: return remote.getSongs()
        local.load()
        val songs = local.library.value.songs.map(LocalSongAdapter::toSong)
        Logger.d(TAG, "list — ${songs.size} songs from the on-device library")
        return Result.success(songs)
    }

    /**
     * Lyrics for [song], plus the deck to project.
     *
     * The deck is built here rather than by the caller because only this class
     * knows which source the song came from, and the two builders are not
     * interchangeable: a library song goes through
     * [SlideDeckBuilder.fromLocalSong], which keeps its typed sections, the
     * user's own section labels and the copyright footer. Building it from the
     * adapted [SongDetail] instead would quietly degrade all three.
     */
    suspend fun detail(song: Song): Result<LoadedSong> {
        val local = library.takeIf { isLocal }
            ?: return remote.getSongDetail(song.number, song.bookName, song.id, song.title)
                .map { LoadedSong(it, SlideDeckBuilder.fromSong(song, it)) }

        val match = resolve(local, song)
            ?: return Result.failure(NoSuchElementException("That song is no longer in your library"))

        return Result.success(
            LoadedSong(LocalSongAdapter.toDetail(match), SlideDeckBuilder.fromLocalSong(match))
        )
    }

    /**
     * Finds [song] in the library as it stands *now*, so a song edited in the
     * Library tab opens with the new words even though the list was built before
     * the edit. Falls back to number+title for a row that predates [Song.localId].
     */
    private fun resolve(library: LibraryRepository, song: Song): LocalSong? {
        val songs = library.library.value.songs
        return song.localId?.let { id -> songs.firstOrNull { it.id == id } }
            ?: songs.firstOrNull { it.number == song.number && it.title == song.title }
    }
}
