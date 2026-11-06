package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.testutil.runVmTest
import com.church.presenter.churchpresentermobile.testutil.tearDown
import com.church.presenter.churchpresentermobile.tv.FoundService
import com.church.presenter.churchpresentermobile.tv.TvSource
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Which TV the phone told to show something, and what happened. */
class TvPickerViewModelTest {

    private val tv = FoundService("Lobby", "192.168.1.31", 8771)
    private val source = TvSource("Pixel", "192.168.1.20", 8766)

    @Test
    fun `a TV that took it is the one showing`() = runVmTest {
        val sent = mutableListOf<Pair<FoundService, TvSource>>()
        val vm = TvPickerViewModel(
            show = { to, what ->
                sent += to to what
                Result.success(Unit)
            },
            clear = { Result.success(Unit) },
        )
        try {
            vm.send(tv, source)
            advanceUntilIdle()

            assertEquals(listOf(tv to source), sent)
            assertEquals("Lobby", vm.showingOn.value)
            assertNull(vm.failedOn.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a TV that could not be reached is not called showing`() = runVmTest {
        val vm = TvPickerViewModel(
            show = { _, _ -> Result.failure(IllegalStateException("refused")) },
            clear = { Result.success(Unit) },
        )
        try {
            vm.send(tv, source)
            advanceUntilIdle()

            assertNull(vm.showingOn.value)
            assertEquals("Lobby", vm.failedOn.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `stopping clears what it was showing on`() = runVmTest {
        val vm = TvPickerViewModel(show = { _, _ -> Result.success(Unit) }, clear = { Result.success(Unit) })
        try {
            vm.send(tv, source)
            advanceUntilIdle()

            vm.stop(tv)
            advanceUntilIdle()

            assertNull(vm.showingOn.value)
        } finally {
            tearDown(vm)
        }
    }
}
