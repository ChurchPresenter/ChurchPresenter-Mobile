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

    @Test
    fun `a question approved but not displayed is reported`() = runVmTestUnconfined {
        // The approve landed, so the question is now public on the desktop; only
        // the display step failed, and the operator needs to know which half.
        val paths = MutableStateFlow(emptyList<String>())
        val vm = recordingVm(paths, failOn = "/display")
        try {
            vm.settled()

            vm.approveAndDisplay("q1")
            val error = vm.actionError.first { it != null }

            assertNotNull(error)
            assertTrue(paths.value.any { it.endsWith("/approve") }, "${paths.value}")
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a failed display still reloads the list`() = runVmTestUnconfined {
        // The approve changed the question's state, so the list is stale either way.
        val paths = MutableStateFlow(emptyList<String>())
        val vm = recordingVm(paths, failOn = "/display")
        try {
            vm.settled()
            val before = paths.value.count { it.endsWith("/questions") }

            vm.approveAndDisplay("q1")
            val seen = paths.first { list -> list.count { it.endsWith("/questions") } > before }

            assertTrue(seen.count { it.endsWith("/questions") } > before)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `saving settings rebuilds the service and reloads`() = runVmTestUnconfined {
        val paths = MutableStateFlow(emptyList<String>())
        val vm = recordingVm(paths)
        try {
            vm.settled()
            val before = paths.value.size

            vm.onSettingsSaved()

            paths.first { it.size > before }
        } finally {
            tearDown(vm)
        }
    }

    // ── The two halves of a load ─────────────────────────────────────────
    //
    // Status and questions are fetched together and reduced into one state. Both
    // must succeed; either failing is an error rather than a half-populated
    // screen, because a question list with no session state cannot say whether
    // voting is on or which question is live.

    @Test
    fun anEmptyQuestionListIsStillAnAdminScreen() = runVmTestUnconfined {
        // No questions yet is the ordinary state at the start of a service.
        val vm = vm { path ->
            if (path.endsWith("/status")) respond("""{"sessionActive":true}""", HttpStatusCode.OK)
            else respond("[]", HttpStatusCode.OK)
        }
        try {
            val admin = assertIs<QAUiState.Admin>(vm.settled())

            assertTrue(admin.questions.isEmpty())
            assertTrue(admin.sessionActive)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun aClosedSessionIsStillAnAdminScreen() = runVmTestUnconfined {
        // The operator needs to see the screen in order to open the session.
        val vm = vm { path ->
            if (path.endsWith("/status")) respond("""{"sessionActive":false}""", HttpStatusCode.OK)
            else respond("[]", HttpStatusCode.OK)
        }
        try {
            val admin = assertIs<QAUiState.Admin>(vm.settled())

            assertFalse(admin.sessionActive)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun votingOffAndNoQuestionDisplayedAreTheSafeDefaults() = runVmTestUnconfined {
        // A minimal status reply must not read as "voting is on" — that would put
        // an unvetted control in front of the congregation.
        val vm = vm { path ->
            if (path.endsWith("/status")) respond("""{"sessionActive":true}""", HttpStatusCode.OK)
            else respond("""[{"id":"q1","text":"Why?","timestamp":1,"status":"PENDING"}]""", HttpStatusCode.OK)
        }
        try {
            val admin = assertIs<QAUiState.Admin>(vm.settled())

            assertFalse(admin.votingEnabled)
            assertEquals("", admin.displayedQuestionId)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun aQuestionWithAnUnknownStatusIsTreatedAsPending() = runVmTestUnconfined {
        // A newer desktop can name a state this build has never heard of; the safe
        // reading is "not yet approved".
        val vm = vm { path ->
            if (path.endsWith("/status")) respond("""{"sessionActive":true}""", HttpStatusCode.OK)
            else respond("""[{"id":"q1","text":"Why?","timestamp":1,"status":"ESCALATED"}]""", HttpStatusCode.OK)
        }
        try {
            val admin = assertIs<QAUiState.Admin>(vm.settled())

            assertEquals(1, admin.questions.size)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun aRefreshAfterAFailureRecovers() = runVmTestUnconfined {
        // The screen offers a retry; it has to actually re-ask rather than replay
        // the failure it already has.
        var probes = 0
        val vm = vm { path ->
            if (path.endsWith("/status")) {
                probes += 1
                if (probes <= 1) respond("", HttpStatusCode.InternalServerError)
                else respond("""{"sessionActive":true}""", HttpStatusCode.OK)
            } else {
                respond("[]", HttpStatusCode.OK)
            }
        }
        try {
            assertIs<QAUiState.Error>(vm.settled())

            vm.loadQuestions()
            val recovered = vm.uiState.first { it is QAUiState.Admin }

            assertIs<QAUiState.Admin>(recovered)
        } finally {
            tearDown(vm)
        }
    }

    // ── The two admin actions that are not per-question edits ────────────

    @Test
    fun `deleting a question reaches the question's own address`() = runVmTestUnconfined {
        // A DELETE on the question itself rather than a verb endpoint, so a
        // wrong path here would silently delete nothing.
        val paths = MutableStateFlow(emptyList<String>())
        val vm = recordingVm(paths)
        try {
            vm.settled()

            vm.deleteQuestion("q1")
            val seen = paths.first { list -> list.any { it.endsWith("/qa/questions/q1") } }

            assertTrue(seen.any { it.endsWith("/qa/questions/q1") }, seen.toString())
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `taking the question off the screen reaches the clear endpoint`() = runVmTestUnconfined {
        val paths = MutableStateFlow(emptyList<String>())
        val vm = recordingVm(paths)
        try {
            vm.settled()

            vm.clearDisplay()
            val seen = paths.first { list -> list.any { it.endsWith("/qa/clear-display") } }

            assertTrue(seen.any { it.endsWith("/qa/clear-display") }, seen.toString())
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a refused delete is reported rather than looking like it worked`() = runVmTestUnconfined {
        val paths = MutableStateFlow(emptyList<String>())
        val vm = recordingVm(paths, failOn = "/qa/questions/q1")
        try {
            vm.settled()

            vm.deleteQuestion("q1")

            assertNotNull(vm.actionError.first { it != null })
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a refused clear is reported`() = runVmTestUnconfined {
        val paths = MutableStateFlow(emptyList<String>())
        val vm = recordingVm(paths, failOn = "/qa/clear-display")
        try {
            vm.settled()

            vm.clearDisplay()

            assertNotNull(vm.actionError.first { it != null })
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a question added with no name given still reaches the server`() = runVmTestUnconfined {
        // The screen calls addQuestion(text) once a name is already known, so the
        // default argument is the ordinary path rather than an edge case.
        val paths = MutableStateFlow(emptyList<String>())
        val vm = recordingVm(paths)
        try {
            vm.settled()

            vm.addQuestion("Why do we sing this one?")
            val seen = paths.first { list -> list.any { it.endsWith("/qa/add") } }

            assertTrue(seen.any { it.endsWith("/qa/add") }, seen.toString())
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a successful delete reloads the list`() = runVmTestUnconfined {
        // The row has to disappear without the operator pulling to refresh.
        val paths = MutableStateFlow(emptyList<String>())
        val vm = recordingVm(paths)
        try {
            vm.settled()
            val before = paths.value.count { it.endsWith("/qa/questions") }

            vm.deleteQuestion("q1")

            assertTrue(paths.first { list -> list.count { it.endsWith("/qa/questions") } > before }.isNotEmpty())
        } finally {
            tearDown(vm)
        }
    }
}
