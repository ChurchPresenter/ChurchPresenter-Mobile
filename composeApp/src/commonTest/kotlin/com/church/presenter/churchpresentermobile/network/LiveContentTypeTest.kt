package com.church.presenter.churchpresentermobile.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Reading what the desktop has live from its `live_state_changed` payload. */
class LiveContentTypeTest {

    @Test
    fun readsTheContentType() {
        assertEquals("DRAWING", liveContentTypeOf("""{"contentType":"DRAWING","liveSlide":null}"""))
        assertEquals("LYRICS", liveContentTypeOf("""{"contentType":"LYRICS"}"""))
    }

    @Test
    fun anythingUnreadableIsNull() {
        assertNull(liveContentTypeOf(""))
        assertNull(liveContentTypeOf("not json"))
        assertNull(liveContentTypeOf("""{"other":1}"""))
        assertNull(liveContentTypeOf("""{"contentType":""}"""))
    }
}
