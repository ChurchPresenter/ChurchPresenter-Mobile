package com.church.presenter.churchpresentermobile.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Reading what the desktop has live from its `live_state_changed` payload. */
class LiveSlideTest {

    @Test
    fun readsTheLiveSlide() {
        assertEquals("DRAWING", liveSlideOf("""{"contentType":"DRAWING","liveSlide":"DRAWING"}"""))
    }

    @Test
    fun anOverlayOverTheDrawingLeavesItLive() {
        // A lower third going up is the change; the drawing is still the slide.
        assertEquals("DRAWING", liveSlideOf("""{"contentType":"LOWER_THIRD","liveSlide":"DRAWING"}"""))
    }

    @Test
    fun anOldDesktopFallsBackToTheContentType() {
        assertEquals("LYRICS", liveSlideOf("""{"contentType":"LYRICS"}"""))
        assertEquals("LYRICS", liveSlideOf("""{"contentType":"LYRICS","liveSlide":null}"""))
        assertEquals("LYRICS", liveSlideOf("""{"contentType":"LYRICS","liveSlide":""}"""))
    }

    @Test
    fun anythingUnreadableIsNull() {
        assertNull(liveSlideOf(""))
        assertNull(liveSlideOf("not json"))
        assertNull(liveSlideOf("""{"other":1}"""))
        assertNull(liveSlideOf("""{"contentType":""}"""))
    }
}
