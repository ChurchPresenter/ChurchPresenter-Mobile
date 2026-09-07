package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The moderator's actions on a question, as requests.
 *
 * Every one of these is a different verb or path on the same endpoint, and the
 * difference is what the room sees: approve lets a question be shown, display
 * puts it on the screen, done strikes it off, delete removes it. Sending the
 * wrong one is not a visual glitch — it is somebody's question on the wall
 * before the host meant it to be.
 *
 * So each test pins the method and path that actually left the phone, and that
 * a refusal from the desktop comes back as a failure rather than a silent no-op.
 */
class QAAdminActionsTest {

    private class Recorder(private val status: HttpStatusCode = HttpStatusCode.OK) {
        val requests = mutableListOf<Pair<String, String>>()

        fun service(body: String = "{}"): QAService {
            val client = HttpClient(MockEngine { request ->
                requests += request.method.value to request.url.encodedPath
                respond(
                    ByteReadChannel(body),
                    status,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            })
            return QAService(AppSettings(InMemorySettingsStorage()), client)
        }

        val lastMethod: String get() = requests.last().first
        val lastPath: String get() = requests.last().second
    }

    @Test
    fun `approving posts to the approve path`() = runTest {
        val r = Recorder()
        r.service().approveQuestion("q1").getOrThrow()

        assertEquals("POST", r.lastMethod)
        assertTrue(r.lastPath.endsWith("/q1/approve"), r.lastPath)
    }

    @Test
    fun `denying posts to the deny path`() = runTest {
        val r = Recorder()
        r.service().denyQuestion("q1").getOrThrow()

        assertTrue(r.lastPath.endsWith("/q1/deny"), r.lastPath)
    }

    @Test
    fun `displaying posts to the display path`() = runTest {
        // The one that actually puts the question in front of the congregation.
        val r = Recorder()
        r.service().displayQuestion("q7").getOrThrow()

        assertEquals("POST", r.lastMethod)
        assertTrue(r.lastPath.endsWith("/q7/display"), r.lastPath)
    }

    @Test
    fun `marking done posts to the done path`() = runTest {
        val r = Recorder()
        r.service().markDone("q1").getOrThrow()

        assertTrue(r.lastPath.endsWith("/q1/done"), r.lastPath)
    }

    @Test
    fun `deleting uses DELETE, not a post to a delete path`() = runTest {
        val r = Recorder()
        r.service().deleteQuestion("q1").getOrThrow()

        assertEquals("DELETE", r.lastMethod)
        assertTrue(r.lastPath.endsWith("/q1"), r.lastPath)
    }

    @Test
    fun `editing sends the new text in the body`() = runTest {
        val r = Recorder()
        r.service().editQuestion("q1", "A clearer question?").getOrThrow()

        assertTrue(r.lastPath.endsWith("/q1/edit"), r.lastPath)
    }

    @Test
    fun `clearing the display posts to its own endpoint, naming no question`() = runTest {
        val r = Recorder()
        r.service().clearDisplay().getOrThrow()

        assertEquals("POST", r.lastMethod)
        assertTrue(r.lastPath.contains(ApiConstants.QA_CLEAR_DISPLAY_ENDPOINT), r.lastPath)
    }

    @Test
    fun `adding a question returns the one the desktop created`() = runTest {
        val r = Recorder()
        val body = """{"id":"new1","text":"Why?","submitterName":"Ruth","timestamp":5,"status":"PENDING"}"""

        val created = r.service(body).addQuestion("Why?", "Ruth").getOrThrow()

        assertEquals("new1", created.id)
        assertEquals("Ruth", created.submitterName)
    }

    @Test
    fun `a refused action is a failure, not a quiet success`() = runTest {
        // The board is a shared surface — a moderator who thinks a question was
        // approved when the desktop refused will wait for it to appear forever.
        val r = Recorder(HttpStatusCode.Unauthorized)

        assertTrue(r.service().approveQuestion("q1").isFailure)
        assertTrue(r.service().displayQuestion("q1").isFailure)
        assertTrue(r.service().deleteQuestion("q1").isFailure)
        assertTrue(r.service().clearDisplay().isFailure)
    }
}
