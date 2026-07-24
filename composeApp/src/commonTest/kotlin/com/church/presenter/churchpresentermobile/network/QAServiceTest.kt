package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.QuestionStatus
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.mockClient
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
