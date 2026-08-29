package com.church.presenter.churchpresentermobile.testutil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Runs a coroutine test with a [StandardTestDispatcher] installed as `Dispatchers.Main`,
 * so ViewModels whose `viewModelScope` / `stateIn(...)` run on the main dispatcher work
 * deterministically under virtual time (`advanceUntilIdle()` / `advanceTimeBy(...)`).
 * Never uses real delays.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun runVmTest(body: suspend TestScope.() -> Unit): TestResult = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
        body()
    } finally {
        Dispatchers.resetMain()
    }
}

/**
 * Like [runVmTest] but installs an [UnconfinedTestDispatcher] as `Dispatchers.Main`, so
 * `viewModelScope.launch` runs eagerly. Use for ViewModels whose fire-and-forget launches
 * do real async work (e.g. a Ktor MockEngine HTTP call) that virtual `advanceUntilIdle()`
 * can't await on JS — instead await the resulting state with `stateFlow.first { ... }`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun runVmTestUnconfined(body: suspend TestScope.() -> Unit): TestResult = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    try {
        body()
    } finally {
        Dispatchers.resetMain()
    }
}

/**
 * Cancels each ViewModel's `viewModelScope` and **waits for its work to finish**,
 * so nothing it launched is still in flight when the test returns and
 * `Dispatchers.resetMain()` runs.
 *
 * Needed because a ViewModel can chain work that outlives the assertion — e.g.
 * `QAViewModel.runAdminAction` calls `loadQuestions()` *after* its action
 * resolves, so a test that awaits only the action's HTTP body leaves a launch
 * pending. Under [runVmTestUnconfined] that continuation resumes on a real
 * worker thread; if `Dispatchers.Main` has been reset by then, resolving the
 * dispatcher throws `IllegalStateException` there. kotlinx-coroutines reports
 * the throw against whichever test starts next, as `UncaughtExceptionsBeforeTest`
 * — so the suite fails intermittently, in a class that is not the one at fault.
 *
 * Cancelling alone does not close the race: cancellation is cooperative, and the
 * worker runs on to the follow-up `launch` regardless. Joining is what guarantees
 * that launch happens while `Dispatchers.Main` is still installed.
 *
 * Call from a `finally` in any test whose ViewModel launches follow-up work:
 * ```
 * val h = Harness()
 * try { ... } finally { tearDown(h.viewModel) }
 * ```
 */
suspend fun tearDown(vararg viewModels: ViewModel) {
    viewModels.forEach { it.viewModelScope.coroutineContext[Job]?.cancelAndJoin() }
}
