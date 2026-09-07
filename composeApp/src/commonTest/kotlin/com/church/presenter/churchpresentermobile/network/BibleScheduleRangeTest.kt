package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.BibleVerse
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.mockClient
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What [BibleService.addBibleToSchedule] puts on the wire for a verse selection.
 *
 * The reference the congregation ends up seeing is built here, not on the
 * desktop: the phone decides whether a selection is "3", "3-7" or "3,5,9", and
 * gets one payload to say it in. The desktop shows whatever arrives, so a wrong
 * range is not a display bug that can be corrected — it is the wrong reference
 * on the wall.
 *
 * The verses are also deliberately passed out of order in most of these: the
 * operator taps them in whatever order they read them, and the payload has to
 * come out in verse order regardless.
 */
class BibleScheduleRangeTest {

    private fun verse(number: Int, text: String) = BibleVerse(verse = number, text = text)

    private class Fixture {
        val sender = FakeWsSender()
        val service: BibleService

        init {
            val settings = AppSettings(InMemorySettingsStorage())
            service = BibleService(settings, sender, mockClient { respond("{}") })
        }

        /** The payload of the last add-to-schedule message, or null if none was sent. */
        val payload: String? get() = sender.calls.lastOrNull()?.second
    }

    @Test
    fun `an empty selection sends nothing at all`() = runTest {
        val f = Fixture()

        val result = f.service.addBibleToSchedule("Genesis", 1, emptyList())

        assertTrue(result.isSuccess, "nothing to send is not a failure")
        assertTrue(f.sender.calls.isEmpty(), "no message should reach the desktop")
    }

    @Test
    fun `a single verse carries no range`() = runTest {
        val f = Fixture()

        f.service.addBibleToSchedule("John", 3, listOf(verse(16, "For God so loved the world")))

        val payload = f.payload!!
        assertTrue(payload.contains("\"verseNumber\":16"), payload)
        // A lone verse is "John 3:16", not "John 3:16-16".
        assertFalse(payload.contains("verseRange\":\""), payload)
    }

    @Test
    fun `a contiguous run is sent as a dash range`() = runTest {
        val f = Fixture()

        f.service.addBibleToSchedule(
            "Genesis",
            1,
            listOf(verse(1, "In the beginning"), verse(2, "And the earth"), verse(3, "And God said")),
        )

        assertTrue(f.payload!!.contains("1-3"), f.payload!!)
    }

    @Test
    fun `a broken run is sent as a comma list`() = runTest {
        val f = Fixture()

        f.service.addBibleToSchedule(
            "Genesis",
            1,
            listOf(verse(1, "one"), verse(3, "three"), verse(5, "five")),
        )

        assertTrue(f.payload!!.contains("1,3,5"), f.payload!!)
    }

    @Test
    fun `verses tapped out of order are sorted before the range is worked out`() = runTest {
        // Tapping 3, then 1, then 2 is a contiguous selection — not "3,1,2".
        val f = Fixture()

        f.service.addBibleToSchedule(
            "Genesis",
            1,
            listOf(verse(3, "three"), verse(1, "one"), verse(2, "two")),
        )

        val payload = f.payload!!
        assertTrue(payload.contains("1-3"), payload)
        assertTrue(payload.contains("\"verseNumber\":1"), payload)
    }

    @Test
    fun `the text is joined in verse order, not tap order`() = runTest {
        val f = Fixture()

        f.service.addBibleToSchedule(
            "Genesis",
            1,
            listOf(verse(2, "second"), verse(1, "first")),
        )

        val payload = f.payload!!
        assertTrue(payload.indexOf("first") < payload.indexOf("second"), payload)
    }

    @Test
    fun `the message is an add-to-schedule, not a projection`() = runTest {
        // The distinction is the whole point of the button: this must not put
        // the verse on the screen in the room.
        val f = Fixture()

        f.service.addBibleToSchedule("John", 3, listOf(verse(16, "text")))

        assertEquals(WsMessageType.ADD_TO_SCHEDULE, f.sender.lastType)
    }

    @Test
    fun `a refusal from the desktop is reported as a failure`() = runTest {
        val f = Fixture()
        f.sender.failWith(IllegalStateException("socket closed"))

        val result = f.service.addBibleToSchedule("John", 3, listOf(verse(16, "text")))

        assertTrue(result.isFailure)
    }
}
