package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Standalone has no Bible: the text lives on a desktop and none is bundled on
 * the device. The tab must therefore say so quietly rather than firing a request
 * at a machine that isn't there and rendering the timeout as an error.
 */
class BibleViewModelStandaloneTest {

    private fun vm(mode: AppMode) = BibleViewModel(
        appSettings = AppSettings(InMemorySettingsStorage()),
        eventService = FakeWsSender(),
        isDemoMode = false,
        presenter = null,
        mode = MutableStateFlow(mode),
    )

    @Test
    fun standaloneLoadsNothingAndReportsNoError() = runVmTest {
        val vm = vm(AppMode.STANDALONE)
        advanceUntilIdle()

        assertTrue(vm.books.value.isEmpty())
        assertNull(vm.error.value, "an absent computer is not an error the operator can fix")
        assertFalse(vm.isLoading.value, "nothing is loading, so nothing may spin")
        assertTrue(vm.hasNoLocalBibles.value)
    }

    @Test
    fun refreshInStandaloneStaysQuiet() = runVmTest {
        val vm = vm(AppMode.STANDALONE)
        advanceUntilIdle()

        vm.refresh()
        advanceUntilIdle()

        assertNull(vm.error.value)
        assertTrue(vm.books.value.isEmpty())
    }

    @Test
    fun remoteModeStillReportsThatBiblesExist() = runVmTest {
        val vm = vm(AppMode.REMOTE)
        advanceUntilIdle()

        assertFalse(vm.hasNoLocalBibles.value)
    }
}
