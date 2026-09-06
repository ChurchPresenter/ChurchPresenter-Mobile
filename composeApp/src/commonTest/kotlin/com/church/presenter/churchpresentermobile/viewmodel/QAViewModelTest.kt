package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.QAService
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.mockClient
import com.church.presenter.churchpresentermobile.testutil.tearDown
import com.church.presenter.churchpresentermobile.testutil.runVmTestUnconfined
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertFalse
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

    // ── Admin actions ────────────────────────────────────────────────────
    //
    // Every one of these runs through `runAdminAction`, which reloads the list on
    // success and surfaces the failure otherwise. The reload matters: without it
    // an approved question keeps showing as pending until the operator refreshes.

    /**
     * Answers every request, recording the paths hit.
     *
     * A StateFlow rather than a plain list so a test can await a request landing:
     * `first { }` only resumes on a new emission, and a mutable list produces none.
     */
    private fun recordingVm(
        paths: MutableStateFlow<List<String>>,
        failOn: String? = null,
    ): QAViewModel = vm { path ->
        paths.value = paths.value + path
        when {
            failOn != null && path.endsWith(failOn) -> respond("nope", HttpStatusCode.Forbidden)
            path.endsWith("/status") -> respond("""{"sessionActive":true}""", HttpStatusCode.OK)
            path.endsWith("/questions") ->
                respond("""[{"id":"q1","text":"Why?","timestamp":1,"status":"PENDING"}]""", HttpStatusCode.OK)
            else -> respond("""{"id":"q1","text":"Why?","timestamp":1,"status":"APPROVED"}""", HttpStatusCode.OK)
        }
    }

    @Test
    fun eachAdminActionReachesItsOwnEndpoint() = runVmTestUnconfined {
        for ((suffix, act) in listOf<Pair<String, QAViewModel.() -> Unit>>(
            "/approve" to { approveQuestion("q1") },
            "/deny" to { denyQuestion("q1") },
            "/edit" to { editQuestion("q1", "reworded") },
            "/done" to { markDone("q1") },
            "/display" to { displayQuestion("q1") },
        )) {
            val paths = MutableStateFlow(emptyList<String>())
            val vm = recordingVm(paths)
            try {
                vm.settled()

                vm.act()
                val seen = paths.first { list -> list.any { it.endsWith(suffix) } }

                assertTrue(seen.any { it.endsWith(suffix) }, "$suffix → $seen")
            } finally {
                tearDown(vm)
            }
        }
    }

    @Test
    fun asuccessfulActionReloadsTheListSoTheRowStopsLookingPending() = runVmTestUnconfined {
        val paths = MutableStateFlow(emptyList<String>())
        val vm = recordingVm(paths)
        try {
            vm.settled()
            val before = paths.value.count { it.endsWith("/questions") }

            vm.approveQuestion("q1")
            val seen = paths.first { list -> list.count { it.endsWith("/questions") } > before }

            assertTrue(seen.count { it.endsWith("/questions") } > before)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun aRefusedActionIsReportedToTheOperator() = runVmTestUnconfined {
        val paths = MutableStateFlow(emptyList<String>())
        val vm = recordingVm(paths, failOn = "/approve")
        try {
            vm.settled()

            vm.approveQuestion("q1")
            val error = vm.actionError.first { it != null }

            assertNotNull(error)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun aDismissedActionErrorGoesAway() = runVmTestUnconfined {
        val paths = MutableStateFlow(emptyList<String>())
        val vm = recordingVm(paths, failOn = "/approve")
        try {
            vm.settled()
            vm.approveQuestion("q1")
            vm.actionError.first { it != null }

            vm.clearActionError()

            assertNull(vm.actionError.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── Approve-and-display, the two-step action ─────────────────────────

    @Test
    fun approveAndDisplayDoesBothInOrder() = runVmTestUnconfined {
        val paths = MutableStateFlow(emptyList<String>())
        val vm = recordingVm(paths)
        try {
            vm.settled()

            vm.approveAndDisplay("q1")
            val seen = paths.first { list -> list.any { it.endsWith("/display") } }

            assertTrue(seen.any { it.endsWith("/approve") }, "$seen")
            assertTrue(
                seen.indexOfFirst { it.endsWith("/approve") } < seen.indexOfFirst { it.endsWith("/display") },
                "approve must land before display: $seen",
            )
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun aFailedApproveNeverGoesLive() = runVmTestUnconfined {
        // Displaying a question the server refused to approve would put unvetted
        // text on the wall, so the second step must not run.
        val paths = MutableStateFlow(emptyList<String>())
        val vm = recordingVm(paths, failOn = "/approve")
        try {
            vm.settled()

            vm.approveAndDisplay("q1")
            vm.actionError.first { it != null }

            assertFalse(paths.value.any { it.endsWith("/display") }, "${paths.value}")
        } finally {
            tearDown(vm)
        }
    }
}
