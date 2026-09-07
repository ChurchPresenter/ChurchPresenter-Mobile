package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.QAService
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.viewmodel.QAViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The Q&A tab around the board: what it shows before the questions arrive, and
 * what it shows when they never do.
 *
 * The board itself is covered in [QABoardTest] and [QAActionsTest], composed
 * directly. What only exists here is the wrapper — the spinner, the failure
 * message and the retry that has to actually ask again, because a Q&A tab stuck
 * on a stale 401 after the operator has just entered the API key is the failure
 * this screen was given a retry for.
 */
@OptIn(ExperimentalTestApi::class)
class QAAdminScreenTest {

    private val statusJson =
        """{"sessionActive":true,"displayedQuestionId":"","votingEnabled":false}"""

    private val questionsJson = """
        [{"id":"q1","text":"Why is the sky blue?","submitterName":"Sam",
          "timestamp":1000,"status":"PENDING","upvotes":2}]
    """.trimIndent()

    /** The desktop's Q&A endpoints, and how often each was asked. */
    private class FakeQaDesktop(
        var status: HttpStatusCode = HttpStatusCode.OK,
        var statusBody: String,
        var questionsBody: String,
    ) {
        val requests = mutableListOf<String>()

        fun client() = HttpClient(MockEngine { request ->
            val path = request.url.encodedPath
            requests += path
            if (status != HttpStatusCode.OK) respond("nope", status)
            else if (path.endsWith("/status")) respond(statusBody)
            else respond(questionsBody)
        })
    }

    private fun desktop(
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = FakeQaDesktop(status = status, statusBody = statusJson, questionsBody = questionsJson)

    private fun viewModelFor(desktop: FakeQaDesktop): QAViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        return QAViewModel(
            appSettings = settings,
            eventService = ServerEventService(settings),
            serviceFactory = { QAService(it, desktop.client()) },
        )
    }

    private fun ComposeUiTest.showQa(vm: QAViewModel, settingsSaveToken: Int = 0) = showScreen {
        QAAdminScreen(viewModel = vm, settingsSaveToken = settingsSaveToken)
    }

    // ── Loading ──────────────────────────────────────────────────────────

    @Test
    fun theQuestionsArriveOnTheBoard() = runComposeUiTest {
        val vm = viewModelFor(desktop())
        showQa(vm)

        awaitThat { exists(UiTags.qaCard("q1")) }
    }

    @Test
    fun theQuestionTextArrivesWithIt() = runComposeUiTest {
        val vm = viewModelFor(desktop())
        showQa(vm)

        awaitThat { isShowing("Why is the sky blue?") }
    }

    @Test
    fun aLoadedBoardShowsNoSpinner() = runComposeUiTest {
        val vm = viewModelFor(desktop())
        showQa(vm)

        awaitThat { exists(UiTags.qaCard("q1")) }
        assertFalse(exists(UiTags.QA_LOADING))
    }

    @Test
    fun aLoadedBoardShowsNoError() = runComposeUiTest {
        val vm = viewModelFor(desktop())
        showQa(vm)

        awaitThat { exists(UiTags.qaCard("q1")) }
        assertFalse(exists(UiTags.QA_ERROR))
    }

    @Test
    fun bothEndpointsAreAsked() = runComposeUiTest {
        // The board needs the questions *and* the session status: which one is
        // live comes from the latter.
        val backend = desktop()
        val vm = viewModelFor(backend)
        showQa(vm)

        awaitThat { backend.requests.any { it.endsWith("/status") } }
        awaitThat { backend.requests.any { it.endsWith("/questions") } }
    }

    // ── When the desktop refuses ─────────────────────────────────────────

    @Test
    fun aRefusedLoadIsReported() = runComposeUiTest {
        val vm = viewModelFor(desktop(status = HttpStatusCode.Unauthorized))
        showQa(vm)

        awaitThat { exists(UiTags.QA_ERROR) }
    }

    @Test
    fun aRefusedLoadOffersARetry() = runComposeUiTest {
        val vm = viewModelFor(desktop(status = HttpStatusCode.Unauthorized))
        showQa(vm)

        awaitThat { exists(UiTags.QA_RETRY) }
    }

    @Test
    fun aRefusedLoadShowsNoBoard() = runComposeUiTest {
        val vm = viewModelFor(desktop(status = HttpStatusCode.Unauthorized))
        showQa(vm)

        awaitThat { exists(UiTags.QA_ERROR) }
        assertFalse(exists(UiTags.qaTab(0)))
    }

    @Test
    fun aRefusedLoadIsNotShownAsAnEmptyQueue() = runComposeUiTest {
        // "Nothing waiting" would tell the operator there are no questions when
        // the truth is the phone cannot see them.
        val vm = viewModelFor(desktop(status = HttpStatusCode.InternalServerError))
        showQa(vm)

        awaitThat { exists(UiTags.QA_ERROR) }
        assertFalse(exists(UiTags.QA_EMPTY_INCOMING))
    }

    @Test
    fun retryingAsksTheDesktopAgain() = runComposeUiTest {
        val backend = desktop(status = HttpStatusCode.Unauthorized)
        val vm = viewModelFor(backend)
        showQa(vm)
        awaitThat { exists(UiTags.QA_RETRY) }
        val before = backend.requests.size

        click(UiTags.QA_RETRY)

        awaitThat { backend.requests.size > before }
    }

    @Test
    fun aRecoveredRetryShowsTheBoard() = runComposeUiTest {
        // Exactly the API-key case: the key was wrong, the operator fixed it,
        // and the tab must not stay on the old failure.
        val backend = desktop(status = HttpStatusCode.Unauthorized)
        val vm = viewModelFor(backend)
        showQa(vm)
        awaitThat { exists(UiTags.QA_ERROR) }

        backend.status = HttpStatusCode.OK
        click(UiTags.QA_RETRY)

        awaitThat { exists(UiTags.qaCard("q1")) }
    }

    @Test
    fun aRecoveredRetryClearsTheError() = runComposeUiTest {
        val backend = desktop(status = HttpStatusCode.Unauthorized)
        val vm = viewModelFor(backend)
        showQa(vm)
        awaitThat { exists(UiTags.QA_ERROR) }

        backend.status = HttpStatusCode.OK
        click(UiTags.QA_RETRY)

        awaitThat { !exists(UiTags.QA_ERROR) }
    }

    // ── After a settings save ────────────────────────────────────────────

    @Test
    fun savingSettingsReloadsTheBoard() = runComposeUiTest {
        val backend = desktop()
        val vm = viewModelFor(backend)
        showQa(vm, settingsSaveToken = 1)

        awaitThat { backend.requests.count { it.endsWith("/questions") } >= 2 }
    }

    @Test
    fun openingWithoutASettingsSaveLoadsOnce() = runComposeUiTest {
        val backend = desktop()
        val vm = viewModelFor(backend)
        showQa(vm, settingsSaveToken = 0)

        awaitThat { exists(UiTags.qaCard("q1")) }
        assertEquals(1, backend.requests.count { it.endsWith("/questions") })
    }

    @Test
    fun savingSettingsRecoversARefusedBoard() = runComposeUiTest {
        // The key the operator just entered is why this reload exists.
        val backend = desktop(status = HttpStatusCode.Unauthorized)
        val vm = viewModelFor(backend)
        showQa(vm)
        awaitThat { exists(UiTags.QA_ERROR) }

        backend.status = HttpStatusCode.OK
        vm.onSettingsSaved()

        awaitThat { exists(UiTags.qaCard("q1")) }
    }

    // ── What the loaded board carries through ────────────────────────────

    @Test
    fun theQuestionsStatusDecidesItsTab() = runComposeUiTest {
        val vm = viewModelFor(desktop())
        showQa(vm)

        awaitThat { exists(UiTags.qaCard("q1")) }
        click(UiTags.qaTab(1))
        assertFalse(exists(UiTags.qaCard("q1")))
    }

    @Test
    fun theDesktopsVotingSettingReachesTheCards() = runComposeUiTest {
        val backend = desktop()
        backend.statusBody =
            """{"sessionActive":true,"displayedQuestionId":"","votingEnabled":true}"""
        val vm = viewModelFor(backend)
        showQa(vm)

        awaitThat { exists(UiTags.qaVotes("q1")) }
    }

    @Test
    fun aDesktopWithVotingOffShowsNoVotes() = runComposeUiTest {
        val vm = viewModelFor(desktop())
        showQa(vm)

        awaitThat { exists(UiTags.qaCard("q1")) }
        assertFalse(exists(UiTags.qaVotes("q1")))
    }

    @Test
    fun theQuestionTheDesktopIsShowingIsBadgedLive() = runComposeUiTest {
        val backend = desktop()
        backend.statusBody =
            """{"sessionActive":true,"displayedQuestionId":"q1","votingEnabled":false}"""
        val vm = viewModelFor(backend)
        showQa(vm)

        awaitThat { exists(UiTags.qaBadge("q1", QaBadge.LIVE)) }
    }

    @Test
    fun theSubmitterNameSurvivesTheRoundTrip() = runComposeUiTest {
        val vm = viewModelFor(desktop())
        showQa(vm)

        awaitThat { isShowing("Sam") }
    }

    @Test
    fun anEmptyQueueIsShownAsEmptyRatherThanBroken() = runComposeUiTest {
        val backend = desktop()
        backend.questionsBody = "[]"
        val vm = viewModelFor(backend)
        showQa(vm)

        awaitThat { exists(UiTags.QA_EMPTY_INCOMING) }
        assertFalse(exists(UiTags.QA_ERROR))
    }

    @Test
    fun aQuestionWithAnUnknownStatusIsTreatedAsPending() = runComposeUiTest {
        // Better in the queue awaiting a decision than silently filed away.
        val backend = desktop()
        backend.questionsBody =
            """[{"id":"q1","text":"Odd one","timestamp":1000,"status":"WHATEVER"}]"""
        val vm = viewModelFor(backend)
        showQa(vm)

        awaitThat { exists(UiTags.qaCard("q1")) }
        assertTrue(exists(UiTags.qaApprove("q1")))
    }
}
