package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.church.presenter.churchpresentermobile.model.VideoCue
import com.church.presenter.churchpresentermobile.model.expectedPositionMs
import com.church.presenter.churchpresentermobile.model.shouldSeek

/**
 * A live web page filling the audience screen.
 *
 * The browser display gets an iframe from the bundled page; this is the same
 * thing for the display the phone drives itself over HDMI, where there is no
 * browser to borrow. Only http(s) URLs ever reach here — see
 * [com.church.presenter.churchpresentermobile.model.SlideDeckBuilder.isProjectableLink].
 *
 * Implementations must survive [url] changing while composed, and must load
 * nothing at all when it is blank.
 */
@Composable
expect fun OutputWebView(url: String, modifier: Modifier)

/**
 * A video filling the audience screen, or the phone's own preview of it.
 *
 * With a [cue] the view follows the phone's transport: it plays or pauses as
 * told, and seeks only when it has drifted past [shouldSeek]'s tolerance from
 * [expectedPositionMs] — checked on every cue and every few seconds between, loops or
 * holds or goes black at the end per [VideoCue.endAction], and plays at
 * [VideoCue.volume]. Without one — a video slide from anywhere else — it keeps
 * the old behaviour: muted, looping, playing.
 *
 * Implementations must survive [url] and [cue] changing while composed, must
 * not reload the video when only the cue changes, and must load nothing at all
 * when [url] is blank.
 *
 * @param muted Overrides the cue's own muting. The phone's preview passes true
 *   unless the sound is meant to come from the phone, so the room never hears a
 *   clip twice.
 * @param soundOnDevice Keep this player's sound on the phone's own speaker or
 *   headphones. Without it the system sends a phone's audio wherever its
 *   picture goes — down the HDMI cable or to a Cast receiver — and "Sound plays
 *   on: This phone" came out of the TV. Honoured where the platform allows it.
 */
@Composable
expect fun OutputVideoView(
    url: String,
    cue: VideoCue?,
    modifier: Modifier,
    muted: Boolean = cue?.muted ?: true,
    soundOnDevice: Boolean = false,
)
