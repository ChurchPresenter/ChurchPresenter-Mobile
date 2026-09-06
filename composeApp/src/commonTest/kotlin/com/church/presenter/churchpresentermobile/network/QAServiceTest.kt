package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.QuestionStatus
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.mockClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests [QAService] status/questions decoding and the DTO->domain status mapping. */
class QAServiceTest {

    private fun service(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        QAService(AppSettings(InMemorySettingsStorage()), mockClient { respond(body, status) })

    @Test
    fun fetchStatusDecodes() = runTest {
        val s = service("""{"sessionActive":true,"cooldownSeconds":45,"votingEnabled":true}""")
            .fetchStatus().getOrThrow()
        assertTrue(s.sessionActive)
        assertEquals(45, s.cooldownSeconds)
        assertTrue(s.votingEnabled)
    }

    @Test
    fun fetchStatusNonSuccessIsFailure() = runTest {
        assertTrue(service("x", HttpStatusCode.Unauthorized).fetchStatus().isFailure)
    }

    @Test
    fun fetchQuestionsMapsDtosAndStatusFallback() = runTest {
        val body = """
            [{"id":"q1","text":"A?","timestamp":1,"status":"APPROVED"},
             {"id":"q2","text":"B?","timestamp":2,"status":"not-a-status"}]
        """.trimIndent()
        val qs = service(body).fetchQuestions().getOrThrow()
        assertEquals(2, qs.size)
        assertEquals(QuestionStatus.APPROVED, qs[0].status)
        assertEquals(QuestionStatus.PENDING, qs[1].status) // invalid -> PENDING
    }

    @Test
    fun addQuestionReturnsMappedQuestion() = runTest {
        val body = """{"id":"q9","text":"hi","timestamp":5,"status":"PENDING","upvotes":0,"downvotes":0}"""
        val q = service(body).addQuestion("hi", "Ada").getOrThrow()
        assertEquals("q9", q.id)
        assertEquals(QuestionStatus.PENDING, q.status)
    }

    @Test
    fun addQuestionNonSuccessIsFailure() = runTest {
        assertTrue(service("no", HttpStatusCode.BadRequest).addQuestion("hi").isFailure)
    }

    @Test
    fun adminActionsSucceedOn2xx() = runTest {
        val svc = service("", HttpStatusCode.OK)
        assertTrue(svc.approveQuestion("q1").isSuccess)
        assertTrue(svc.denyQuestion("q1").isSuccess)
        assertTrue(svc.editQuestion("q1", "new text").isSuccess)
        assertTrue(svc.markDone("q1").isSuccess)
        assertTrue(svc.displayQuestion("q1").isSuccess)
        assertTrue(svc.deleteQuestion("q1").isSuccess)
        assertTrue(svc.clearDisplay().isSuccess)
    }

    @Test
    fun adminActionFailsOnErrorStatus() = runTest {
        val svc = service("nope", HttpStatusCode.InternalServerError)
        assertTrue(svc.approveQuestion("q1").isFailure)
        assertTrue(svc.deleteQuestion("q1").isFailure)
    }

    // ── Endpoints and verbs ──────────────────────────────────────────────
    //
    // Each admin action is a different path on the same resource, and one is a
    // DELETE rather than a POST. A wrong verb or a wrong suffix answers 404 and
    // the UI reports "failed" with nothing to say why, so both are pinned.

    private class Captured {
        var method: String = ""
        var path: String = ""
        var apiKeyHeader: String? = null
        var adminHeader: String? = null
    }

    private fun capturing(cap: Captured, apiKey: String = ""): QAService {
        val settings = AppSettings(InMemorySettingsStorage()).also { it.apiKey = apiKey }
        return QAService(
            settings,
            HttpClient(MockEngine { request ->
                cap.method = request.method.value
                cap.path = request.url.encodedPath
                cap.apiKeyHeader = request.headers[ApiConstants.API_KEY_HEADER]
                cap.adminHeader = request.headers[ApiConstants.QA_ADMIN_PASSWORD_HEADER]
                respond("""{"id":"q1","text":"t","timestamp":1,"status":"PENDING"}""")
            }),
        )
    }

    @Test
    fun eachAdminActionPostsToItsOwnSuffix() = runTest {
        for ((suffix, call) in listOf<Pair<String, suspend QAService.() -> Unit>>(
            "approve" to { approveQuestion("q1").getOrThrow() },
            "deny" to { denyQuestion("q1").getOrThrow() },
            "edit" to { editQuestion("q1", "new text").getOrThrow() },
            "done" to { markDone("q1").getOrThrow() },
            "display" to { displayQuestion("q1").getOrThrow() },
        )) {
            val cap = Captured()
            capturing(cap).call()

            assertEquals("POST", cap.method, suffix)
            assertTrue(cap.path.endsWith("/q1/$suffix"), "$suffix → ${cap.path}")
        }
    }

    @Test
    fun deletingAQuestionUsesTheDeleteVerbAndNoSuffix() = runTest {
        val cap = Captured()

        capturing(cap).deleteQuestion("q1").getOrThrow()

        assertEquals("DELETE", cap.method)
        assertTrue(cap.path.endsWith("/q1"), cap.path)
    }

    @Test
    fun clearingTheDisplayHasItsOwnEndpoint() = runTest {
        val cap = Captured()

        capturing(cap).clearDisplay().getOrThrow()

        assertEquals("POST", cap.method)
        assertTrue(cap.path.isNotBlank())
    }

    // ── Authentication ───────────────────────────────────────────────────

    @Test
    fun withNoApiKeyNeitherHeaderIsSent() = runTest {
        val cap = Captured()

        capturing(cap, apiKey = "").approveQuestion("q1").getOrThrow()

        assertNull(cap.apiKeyHeader)
        assertNull(cap.adminHeader)
    }

    @Test
    fun anApiKeyIsAlsoSentAsTheAdminPassword() = runTest {
        // The Q&A admin routes authenticate on X-QA-Password, which the server sets
        // equal to the API key. Sending only the API key returns 401 "Invalid admin
        // password" — the failure this header exists to prevent.
        val cap = Captured()

        capturing(cap, apiKey = "s3cret").approveQuestion("q1").getOrThrow()

        assertEquals("s3cret", cap.apiKeyHeader)
        assertEquals("s3cret", cap.adminHeader)
    }

    @Test
    fun theAdminPasswordAccompaniesEveryAdminAction() = runTest {
        for (call in listOf<suspend QAService.() -> Unit>(
            { denyQuestion("q1").getOrThrow() },
            { markDone("q1").getOrThrow() },
            { displayQuestion("q1").getOrThrow() },
            { deleteQuestion("q1").getOrThrow() },
            { clearDisplay().getOrThrow() },
        )) {
            val cap = Captured()
            capturing(cap, apiKey = "s3cret").call()

            assertEquals("s3cret", cap.adminHeader)
        }
    }

    // ── Bodies ───────────────────────────────────────────────────────────

    @Test
    fun editingSendsTheNewTextAsJson() = runTest {
        var body = ""
        val settings = AppSettings(InMemorySettingsStorage())
        val service = QAService(
            settings,
            HttpClient(MockEngine { request ->
                body = (request.body as io.ktor.http.content.TextContent).text
                respond("{}")
            }),
        )

        service.editQuestion("q1", "corrected wording").getOrThrow()

        assertTrue(body.contains("corrected wording"), body)
    }

    @Test
    fun askingCarriesBothTheQuestionAndTheName() = runTest {
        var body = ""
        val settings = AppSettings(InMemorySettingsStorage())
        val service = QAService(
            settings,
            HttpClient(MockEngine { request ->
                body = (request.body as io.ktor.http.content.TextContent).text
                respond("""{"id":"q1","text":"Why?","submitterName":"Ada","timestamp":1,"status":"PENDING"}""")
            }),
        )

        val asked = service.addQuestion("Why?", "Ada").getOrThrow()

        assertTrue(body.contains("Why?"), body)
        assertTrue(body.contains("Ada"), body)
        assertEquals("Ada", asked.submitterName)
    }

    @Test
    fun askingAnonymouslyOmitsTheNameEntirely() = runTest {
        var body = ""
        val settings = AppSettings(InMemorySettingsStorage())
        val service = QAService(
            settings,
            HttpClient(MockEngine { request ->
                body = (request.body as io.ktor.http.content.TextContent).text
                respond("""{"id":"q1","text":"Why?","timestamp":1,"status":"PENDING"}""")
            }),
        )

        service.addQuestion("Why?").getOrThrow()

        // The encoder has no encodeDefaults, so a blank name is left off the wire
        // rather than sent as "". The server treats both as anonymous.
        assertTrue(body.contains("Why?"), body)
        assertFalse(body.contains("\"name\""), body)
    }

    @Test
    fun everyActionReportsANonSuccessAsAFailure() = runTest {
        val settings = AppSettings(InMemorySettingsStorage())
        val svc = QAService(settings, mockClient { respond("nope", HttpStatusCode.Forbidden) })

        assertTrue(svc.deleteQuestion("q1").isFailure)
        assertTrue(svc.editQuestion("q1", "t").isFailure)
        assertTrue(svc.markDone("q1").isFailure)
        assertTrue(svc.displayQuestion("q1").isFailure)
        assertTrue(svc.clearDisplay().isFailure)
        assertTrue(svc.fetchQuestions().isFailure)
    }
}
