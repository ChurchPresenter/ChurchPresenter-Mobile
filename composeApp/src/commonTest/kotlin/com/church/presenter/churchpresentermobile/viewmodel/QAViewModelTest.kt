package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.QAService
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.mockClient
import com.church.presenter.churchpresentermobile.testutil.runVmTestUnconfined
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Tests [QAViewModel.loadQuestions] — the dual fetchStatus + fetchQuestions reduction. */
class QAViewModelTest {

    private fun vm(handler: MockRequestHandleScope.(path: String) -> HttpResponseData): QAViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        return QAViewModel(settings, ServerEventService(settings)) { QAService(it, mockClient(handler)) }
    }

    private suspend fun QAViewModel.settled(): QAUiState = uiState.first { it !is QAUiState.Loading }

    @Test
    fun bothCallsSucceedProducesAdmin() = runVmTestUnconfined {
        val vm = vm { path ->
            when {
                path.endsWith("/status") ->
                    respond("""{"sessionActive":true,"displayedQuestionId":"q1","votingEnabled":true}""", HttpStatusCode.OK)
                else ->
                    respond("""[{"id":"q1","text":"Why?","timestamp":1,"status":"PENDING"}]""", HttpStatusCode.OK)
            }
        }
        val admin = assertIs<QAUiState.Admin>(vm.settled())
        assertTrue(admin.sessionActive)
        assertTrue(admin.votingEnabled)
        assertEquals("q1", admin.displayedQuestionId)
        assertEquals(1, admin.questions.size)
    }

    @Test
    fun statusFailureProducesError() = runVmTestUnconfined {
        val vm = vm { path ->
            if (path.endsWith("/status")) respond("", HttpStatusCode.Unauthorized)
            else respond("[]", HttpStatusCode.OK)
        }
        assertIs<QAUiState.Error>(vm.settled())
    }

    @Test
    fun questionsFailureProducesError() = runVmTestUnconfined {
        val vm = vm { path ->
            if (path.endsWith("/status")) respond("""{"sessionActive":true}""", HttpStatusCode.OK)
            else respond("boom", HttpStatusCode.InternalServerError)
        }
        assertIs<QAUiState.Error>(vm.settled())
    }
}
