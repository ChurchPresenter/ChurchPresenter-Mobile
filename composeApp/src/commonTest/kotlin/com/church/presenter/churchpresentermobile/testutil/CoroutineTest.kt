package com.church.presenter.churchpresentermobile.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
