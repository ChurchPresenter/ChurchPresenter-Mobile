package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.Presentation
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.mockClient
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests [PresentationService]: list/by-id decoding with slide thumbnail
 * resolution, and the WebSocket actions — slide navigation, clear and
 * add-to-schedule — which reach the desktop as JSON payloads, not HTTP.
 */
class PresentationServiceTest {

    private fun service(body: String, status: HttpStatusCode = HttpStatusCode.OK): PresentationService {
        val settings = AppSettings(InMemorySettingsStorage())
        return PresentationService(settings, ServerEventService(settings), mockClient { respond(body, status) })
    }

    @Test
    fun getPresentationsParsesAndResolvesThumbnails() = runTest {
        val body = """
            {"presentations":[
              {"id":"p1","file-name":"Sermon.pptx","slide-total":3,
               "slides":[{"slide-index":0,"thumbnail-url":"/s/0"}]}
            ]}
        """.trimIndent()
        val list = service(body).getPresentations().getOrThrow()
        assertEquals(1, list.size)
        assertEquals("Sermon.pptx", list[0].displayName)
        assertEquals(3, list[0].totalSlides)
        assertEquals("http://192.168.1.100:8765/s/0", list[0].slides!![0].thumbnailUrl)
    }

    @Test
    fun getPresentationByIdResolvesThumbnails() = runTest {
        val body = """{"id":"p1","file-name":"S.pptx","slides":[{"slide-index":0,"thumbnail-url":"/s/0"}]}"""
        val p = service(body).getPresentationById("p1").getOrThrow()
        assertEquals("p1", p.displayId)
        assertEquals("http://192.168.1.100:8765/s/0", p.slides!![0].thumbnailUrl)
    }

    @Test
    fun getPresentationsNonSuccessIsFailure() = runTest {
        assertTrue(service("no", HttpStatusCode.InternalServerError).getPresentations().isFailure)
    }

    @Test
    fun getPresentationsWithNoListReadsAsEmptyRatherThanFailing() = runTest {
        // The desktop omits the key entirely when no deck is open; allPresentations
        // falls back to an empty list so the screen shows its empty state.
        val list = service("""{"total":0}""").getPresentations().getOrThrow()

        assertTrue(list.isEmpty())
    }

    @Test
    fun getPresentationsRejectsABareArray() = runTest {
        // Unlike the schedule endpoint, this one decodes an object only — there is
        // no array-or-wrapper polymorphism here. Pinned so a desktop change that
        // starts sending a bare array is caught rather than silently returning none.
        assertTrue(service("""[{"id":"p1"}]""").getPresentations().isFailure)
    }

    @Test
    fun getPresentationsLeavesAMissingThumbnailNull() = runTest {
        val body = """{"presentations":[{"id":"p1","slides":[{"slide-index":0}]}]}"""

        val slide = service(body).getPresentations().getOrThrow()[0].slides!![0]

        assertEquals(null, slide.thumbnailUrl)
    }

    @Test
    fun getPresentationByIdNonSuccessIsFailure() = runTest {
        assertTrue(service("gone", HttpStatusCode.NotFound).getPresentationById("p1").isFailure)
    }

    // ── WebSocket actions ────────────────────────────────────────────────

    private fun wsService(ws: FakeWsSender): PresentationService {
        val settings = AppSettings(InMemorySettingsStorage())
        return PresentationService(settings, ws, mockClient { respond("{}") })
    }

    @Test
    fun selectPresentationSendsTheSlideIndexOverTheSocket() = runTest {
        val ws = FakeWsSender()

        wsService(ws).selectPresentation("p1", slideIndex = 7).getOrThrow()

        assertEquals(WsMessageType.SELECT_SLIDE, ws.lastType)
        assertTrue(ws.lastPayload.contains("\"id\":\"p1\""), ws.lastPayload)
        assertTrue(ws.lastPayload.contains("\"index\":7"), ws.lastPayload)
    }

    @Test
    fun selectPresentationIsFireAndForget() = runTest {
        // Slide navigation must feel immediate; waiting on an ack made paging lag.
        val ws = FakeWsSender()

        wsService(ws).selectPresentation("p1", 0).getOrThrow()

        assertTrue(ws.calls.last().third)
    }

    @Test
    fun selectPresentationReportsASocketFailure() = runTest {
        val ws = FakeWsSender()
        ws.failWith(IllegalStateException("socket closed"))

        assertTrue(wsService(ws).selectPresentation("p1", 0).isFailure)
    }

    @Test
    fun clearDisplaySendsClear() = runTest {
        val ws = FakeWsSender()

        wsService(ws).clearDisplay().getOrThrow()

        assertEquals(WsMessageType.CLEAR, ws.lastType)
        assertEquals("", ws.lastPayload)
        assertTrue(ws.calls.last().third)
    }

    @Test
    fun addToScheduleSendsTheNameTheDesktopWillShow() = runTest {
        val ws = FakeWsSender()

        wsService(ws).addToSchedule(Presentation(id = "p1", fileName = "Sermon.pptx")).getOrThrow()

        assertEquals(WsMessageType.ADD_TO_SCHEDULE, ws.lastType)
        assertTrue(ws.lastPayload.contains("\"id\":\"p1\""), ws.lastPayload)
        assertTrue(ws.lastPayload.contains("Sermon.pptx"), ws.lastPayload)
    }

    @Test
    fun addToScheduleFallsBackToTheIdWhenThereIsNoFileName() = runTest {
        // displayName falls back to the id, so the schedule row is never blank.
        val ws = FakeWsSender()

        wsService(ws).addToSchedule(Presentation(id = "p1")).getOrThrow()

        assertTrue(ws.lastPayload.contains("\"title\":\"p1\""), ws.lastPayload)
    }

    @Test
    fun addToScheduleWaitsForApproval() = runTest {
        // Unlike slide navigation, adding needs the operator's Allow on the desktop.
        val ws = FakeWsSender()

        wsService(ws).addToSchedule(Presentation(id = "p1")).getOrThrow()

        assertFalse(ws.calls.last().third)
    }

    @Test
    fun addToScheduleReportsARefusal() = runTest {
        val ws = FakeWsSender()
        ws.failWith(IllegalStateException("denied"))

        assertTrue(wsService(ws).addToSchedule(Presentation(id = "p1")).isFailure)
    }
}
