package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * That every service identifies itself the same way.
 *
 * A desktop with an API key set rejects an unauthenticated request outright, so
 * one service forgetting the header is a whole tab that silently shows nothing
 * while the rest of the app works. The same goes for the device headers: the
 * desktop's approval dialog names the phone asking, and a request without them
 * is attributed to nobody.
 *
 * The check is written once and run across the services rather than being
 * repeated in each service's own test, because the failure is exactly the sort
 * that gets missed when a new service is added.
 */
class AuthenticatedRequestsTest {

    /** Headers of the last request each service made. */
    private class Recorder {
        var seen: Headers = Headers.Empty

        fun client() = HttpClient(MockEngine { request ->
            seen = request.headers
            respond(
                content = BODIES,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        })
    }

    private fun settings(apiKey: String = "", deviceName: String = "") =
        AppSettings(InMemorySettingsStorage()).apply {
            this.apiKey = apiKey
            customDeviceName = deviceName
        }

    /**
     * One read call per service, each over its own recorder.
     *
     * Reads rather than writes: they take no arguments worth inventing, and the
     * headers are applied by the same helper either way.
     */
    private fun calls(
        settings: AppSettings,
    ): List<Pair<String, suspend () -> Unit>> {
        val ws = FakeWsSender()
        return listOf(
            "SongService" to recorded { SongService(settings, ws, it).getSongs() },
            "BibleService" to recorded { BibleService(settings, ws, it).getBooks() },
            "PicturesService" to recorded { PicturesService(settings, ws, it).getPictures() },
            "PresentationService" to recorded { PresentationService(settings, ws, it).getPresentations() },
            "ScheduleService" to recorded { ScheduleService(settings, it).getSchedule() },
            "QAService" to recorded { QAService(settings, it).fetchQuestions() },
            "DictionaryService" to recorded { DictionaryService(settings, ws, it).lookup("H1") },
        )
    }

    private val recorders = mutableListOf<Recorder>()

    private fun recorded(call: suspend (HttpClient) -> Unit): suspend () -> Unit {
        val recorder = Recorder().also { recorders += it }
        return { call(recorder.client()) }
    }

    /** Runs every service's read and hands back what each one sent, by name. */
    private suspend fun headersFrom(settings: AppSettings): Map<String, Headers> {
        recorders.clear()
        val entries = calls(settings)
        entries.forEach { (_, call) -> call() }
        return entries.mapIndexed { i, (name, _) -> name to recorders[i].seen }.toMap()
    }

    @Test
    fun `every service sends the API key when one is set`() = runTest {
        val sent = headersFrom(settings(apiKey = "secret-key"))

        for ((service, headers) in sent) {
            assertEquals("secret-key", headers[ApiConstants.API_KEY_HEADER], service)
        }
    }

    @Test
    fun `no service sends an empty API key header when none is set`() = runTest {
        // An empty header is not the same as no header: a desktop checking for
        // the header's presence would treat it as a wrong key rather than none.
        val sent = headersFrom(settings(apiKey = ""))

        for ((service, headers) in sent) {
            assertNull(headers[ApiConstants.API_KEY_HEADER], service)
        }
    }

    @Test
    fun `every service identifies the device`() = runTest {
        // The desktop's approval dialog names the phone asking; without this it
        // says nothing and the operator cannot tell who pressed Project.
        val settings = settings(apiKey = "secret-key")
        val sent = headersFrom(settings)

        for ((service, headers) in sent) {
            assertEquals(settings.deviceId, headers[ApiConstants.DEVICE_ID_HEADER], service)
        }
    }

    @Test
    fun `every service sends the device name when one is set`() = runTest {
        val sent = headersFrom(settings(deviceName = "Sound desk"))

        for ((service, headers) in sent) {
            assertEquals("Sound desk", headers[ApiConstants.DEVICE_NAME_HEADER], service)
        }
    }

    @Test
    fun `a name outside ASCII is encoded rather than sent raw`() = runTest {
        // A raw non-ASCII header value throws before the request is made, which
        // would take out every tab at once for a church that named its phone in
        // its own language.
        val sent = headersFrom(settings(deviceName = "Звукова рубка"))

        for ((service, headers) in sent) {
            val name = assertNotNull(headers[ApiConstants.DEVICE_NAME_HEADER], service)
            assertTrue(name.all { it.code < ASCII_LIMIT }, "$service sent $name")
        }
    }

    @Test
    fun `the API key and the device headers travel together`() = runTest {
        // Both are applied by the same helper; a service applying one without
        // the other means the helper was bypassed.
        val settings = settings(apiKey = "secret-key", deviceName = "Sound desk")
        val sent = headersFrom(settings)

        for ((service, headers) in sent) {
            assertNotNull(headers[ApiConstants.API_KEY_HEADER], service)
            assertNotNull(headers[ApiConstants.DEVICE_ID_HEADER], service)
            assertNotNull(headers[ApiConstants.DEVICE_NAME_HEADER], service)
        }
    }

    private companion object {
        const val ASCII_LIMIT = 128

        /**
         * A body every service's decoder accepts something from. Each reads a
         * different shape and ignores unknown keys, so one object with every
         * field they might look for serves all of them; the response is not what
         * is under test here.
         */
        const val BODIES = """{"song-book":[],"books":[],"images":[],"presentations":[],
            "items":[],"questions":[],"number":"H1","word":"","transliteration":"",
            "pronunciation":"","definition":"","occurrences":0}"""
    }
}
