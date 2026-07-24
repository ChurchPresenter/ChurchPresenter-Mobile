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

/** Tests [SongService.getSongs] (catalog flatten) and [SongService.getSongDetail] (2-pass parse). */
class SongServiceTest {

    private fun service(body: String, status: HttpStatusCode = HttpStatusCode.OK): SongService {
        val settings = AppSettings(InMemorySettingsStorage())
        return SongService(settings, ServerEventService(settings), mockClient { respond(body, status) })
    }

    @Test
    fun getSongsFlattensBooksAndStampsBookName() = runTest {
        val body = """
            {"song-book":[
              {"book-name":"Hymns","song-total":2,"songs":[
                {"id":1,"number":"1","title":"Amazing Grace"},
                {"id":2,"number":"2","title":"How Great"}
              ]},
              {"book-name":"Chorus","song-total":1,"songs":[
                {"id":3,"number":"10","title":"Shout"}
              ]}
            ]}
        """.trimIndent()
        val songs = service(body).getSongs().getOrThrow()
        assertEquals(3, songs.size)
        assertEquals("Amazing Grace", songs[0].title)
        assertEquals("Hymns", songs[0].bookName)
        assertEquals("Chorus", songs[2].bookName)
    }

    @Test
    fun getSongsNonSuccessIsFailure() = runTest {
        assertTrue(service("boom", HttpStatusCode.ServiceUnavailable).getSongs().isFailure)
    }

    @Test
    fun getSongDetailParsesVersesAndBookName() = runTest {
        val body = """
            {"number":"1","title":"Amazing Grace","book-name":"Hymns",
             "verses":[{"number":1,"lines":["Amazing grace","how sweet"]}]}
        """.trimIndent()
        val detail = service(body).getSongDetail("1", "Hymns").getOrThrow()
        assertEquals("Hymns", detail.bookName)
        assertTrue(detail.hasLyrics)
        assertEquals(1, detail.allVerses.size)
        assertEquals("Amazing grace\nhow sweet", detail.allVerses[0].displayText)
    }

    @Test
    fun getSongDetailFlexibleScanRecoversVersesFromUnknownKey() = runTest {
        // No recognised verse container — the flexible second pass should still find lyrics.
        val body = """
            {"number":"1","title":"T","customStanzas":[{"lines":["hello world"]}]}
        """.trimIndent()
        val detail = service(body).getSongDetail("1", null).getOrThrow()
        assertTrue(detail.hasLyrics)
        assertEquals("hello world", detail.allVerses.first().displayText)
    }

    @Test
    fun getSongDetailNonSuccessIsFailure() = runTest {
        assertTrue(service("nope", HttpStatusCode.NotFound).getSongDetail("1", null).isFailure)
    }
}
