package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.mockClient
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests [PicturesService.getPictures] decoding + thumbnail URL resolution. */
class PicturesServiceTest {

    private fun service(body: String, status: HttpStatusCode = HttpStatusCode.OK): PicturesService {
        val settings = AppSettings(InMemorySettingsStorage())
        return PicturesService(settings, ServerEventService(settings), mockClient { respond(body, status) })
    }

    @Test
    fun getPicturesParsesAndResolvesThumbnails() = runTest {
        val body = """
            {"folder-id":"f1","folder-name":"Nature","image-total":2,
             "images":[{"index":0,"file-name":"a.jpg","thumbnail-url":"/thumb/0"}]}
        """.trimIndent()
        val folder = service(body).getPictures().getOrThrow()
        assertEquals("Nature", folder.displayName)
        assertEquals(2, folder.totalImages)
        assertEquals("http://192.168.1.100:8765/thumb/0", folder.allImages[0].thumbnailUrl)
    }

    @Test
    fun getPicturesLeavesNullThumbnailNull() = runTest {
        val body = """{"folder-name":"F","images":[{"index":0,"file-name":"a.jpg"}]}"""
        assertNull(service(body).getPictures().getOrThrow().allImages[0].thumbnailUrl)
    }

    @Test
    fun getPicturesNonSuccessIsFailure() = runTest {
        assertTrue(service("no", HttpStatusCode.NotFound).getPictures().isFailure)
    }
}
