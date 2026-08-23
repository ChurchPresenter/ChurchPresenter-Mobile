package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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
 * A video filling the audience screen.
 *
 * Muted and looping by default: the hall's sound comes from the desk, not from
 * whatever screen happens to be showing the clip, and an unmuted autoplay is
 * blocked outright on some platforms.
 */
@Composable
expect fun OutputVideoView(url: String, modifier: Modifier)
