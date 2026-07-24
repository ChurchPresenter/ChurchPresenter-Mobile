package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.mockClient
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests [PresentationService] list/by-id decoding + slide thumbnail resolution. */
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
}
