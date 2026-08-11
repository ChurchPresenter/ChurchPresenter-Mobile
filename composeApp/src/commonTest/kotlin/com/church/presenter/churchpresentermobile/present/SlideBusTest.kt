package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.SlideEnvelope
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SlideBusTest {

    @BeforeTest fun setUp() = SlideBus.resetForTest()

    @AfterTest fun tearDown() = SlideBus.resetForTest()

    private fun envelope(rev: Long, body: String) =
        SlideEnvelope(rev = rev, slide = Slide(body = body))

    @Test
    fun `starts on the initial blank envelope`() {
        assertEquals(SlideEnvelope.INITIAL, SlideBus.current.value)
    }

    @Test
    fun `publishing a newer envelope replaces the current one`() {
        SlideBus.publish(envelope(1, "first"))
        assertEquals("first", SlideBus.current.value.slide?.body)

        SlideBus.publish(envelope(2, "second"))
        assertEquals("second", SlideBus.current.value.slide?.body)
    }

    @Test
    fun `an out-of-order envelope is dropped`() {
        SlideBus.publish(envelope(5, "current"))
        SlideBus.publish(envelope(3, "stale"))

        assertEquals("current", SlideBus.current.value.slide?.body)
        assertEquals(5L, SlideBus.current.value.rev)
    }

    @Test
    fun `a re-publish at the same revision is allowed so a display can be resynced`() {
        SlideBus.publish(envelope(4, "original"))
        SlideBus.publish(envelope(4, "corrected"))

        assertEquals("corrected", SlideBus.current.value.slide?.body)
    }
}
