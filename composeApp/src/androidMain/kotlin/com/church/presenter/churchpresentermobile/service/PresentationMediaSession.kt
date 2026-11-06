package com.church.presenter.churchpresentermobile.service

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import com.church.presenter.churchpresentermobile.R

private const val SESSION_TAG = "ChurchPresenter"
private const val CLOSE_ACTION = "close_app"

/**
 * Puts the presenting notification in the system's media player, the panel with
 * the album art behind it: the art is the slide, and the one control is Close.
 *
 * A media session is what the system's player is built from. Nothing here plays
 * sound; the session says it is playing because a paused player is taken away
 * after a few minutes, and the screens are still live.
 */
internal class PresentationMediaSession(context: Context, onClose: () -> Unit) {

    private val session = MediaSession(context, SESSION_TAG).apply {
        setCallback(object : MediaSession.Callback() {
            override fun onCustomAction(action: String, extras: Bundle?) {
                if (action == CLOSE_ACTION) onClose()
            }
        })
        setPlaybackState(
            PlaybackState.Builder()
                .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 0f)
                .addCustomAction(
                    CLOSE_ACTION,
                    context.getString(R.string.presentation_close_app),
                    android.R.drawable.ic_menu_close_clear_cancel,
                )
                .build()
        )
        isActive = true
    }

    val token: MediaSession.Token get() = session.sessionToken

    /** Shows [art] behind the player, with [title] over it. */
    fun show(art: Bitmap?, title: String) {
        session.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putBitmap(MediaMetadata.METADATA_KEY_ART, art)
                .build()
        )
    }

    fun release() = session.release()
}
