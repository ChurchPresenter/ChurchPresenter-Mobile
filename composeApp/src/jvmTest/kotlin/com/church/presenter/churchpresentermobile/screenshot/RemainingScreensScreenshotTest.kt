package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.hasText
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.QAService
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.QAAdminScreen
import com.church.presenter.churchpresentermobile.ui.SplashScreen
import com.church.presenter.churchpresentermobile.viewmodel.QAViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test

/**
 * The screens left over: the Q&A tab as the app assembles it, and the splash.
 *
 * The Q&A tab is captured through a real [QAViewModel] over a mock desktop, so
 * the board arrives the way it does in the app — a status call, then the
 * questions — rather than being handed a state object as `QAScreenshotTest`
 * does for the board itself.
 */
class RemainingScreensScreenshotTest {

    private val openSession = """{"sessionActive":true,"votingEnabled":true}"""

    private val questions = """
        [
          {"id":"1","text":"How do we know the promises still apply today?","submitterName":"Ruth",
           "timestamp":0,"status":"PENDING","upvotes":0,"downvotes":0},
          {"id":"2","text":"What does 'grace' mean in verse 8?","submitterName":"Sam",
           "timestamp":0,"status":"APPROVED","upvotes":7,"downvotes":0}
        ]
    """.trimIndent()

    private fun qaTab(
        statusBody: String = openSession,
        questionsBody: String = questions,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): @Composable () -> Unit {
        val settings = AppSettings(InMemorySettingsStorage())
        val client = HttpClient(MockEngine { request ->
            when {
                status != HttpStatusCode.OK -> respond("nope", status)
                request.url.encodedPath.endsWith("/status") -> respond(statusBody)
                else -> respond(questionsBody)
            }
        })
        val viewModel = QAViewModel(
            appSettings = settings,
            eventService = ServerEventService(settings),
            serviceFactory = { QAService(it, client) },
        )
        return { QAAdminScreen(viewModel = viewModel) }
    }

    @Test
    fun qaTabWithQuestions() = screenshot(
        "qa-tab__open-session",
        until = { onAllNodes(hasText("grace", substring = true)).fetchSemanticsNodes().isNotEmpty() },
        content = qaTab(),
    )

    @Test
    fun qaTabNoQuestions() = screenshot(
        "qa-tab__no-questions",
        content = qaTab(questionsBody = "[]"),
    )

    @Test
    fun qaTabSessionClosed() = screenshot(
        "qa-tab__session-closed",
        content = qaTab(statusBody = """{"sessionActive":false,"votingEnabled":false}""", questionsBody = "[]"),
    )

    @Test
    fun qaTabDesktopUnreachable() = screenshot(
        "qa-tab__unreachable",
        content = qaTab(status = HttpStatusCode.ServiceUnavailable),
    )

    @Test
    fun splash() = screenshot("splash__initial") {
        // The splash animates and then calls onComplete; the capture is of the
        // frame the composition settles on, which is what a cold launch shows.
        SplashScreen(onComplete = {})
    }
}
