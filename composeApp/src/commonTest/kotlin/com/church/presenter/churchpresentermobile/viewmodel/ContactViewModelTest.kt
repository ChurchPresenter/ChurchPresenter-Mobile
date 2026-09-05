package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.network.ContactReporter
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import com.church.presenter.churchpresentermobile.testutil.tearDown
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The contact form: what it will send, and what it says about what came back.
 *
 * The submit function is injected, so none of this opens a socket.
 */
class ContactViewModelTest {

    private fun vm(
        outcome: ContactReporter.Outcome = ContactReporter.Outcome.Success,
        record: MutableList<ContactReporter.ContactRequest> = mutableListOf(),
    ) = ContactViewModel { request ->
        record += request
        outcome
    }

    @Test
    fun nothingIsSentUntilThereIsANameAndAMessage() = runVmTest {
        // The server requires both, so a form missing either would only come back
        // 400 — better to keep the button off than to spend a rate-limit slot.
        val vm = vm()
        try {
            assertFalse(vm.canSend, "an empty form is not sendable")

            vm.setName("Ada")
            assertFalse(vm.canSend, "a name alone is not sendable")

            vm.setMessage("The Present tab will not scroll")
            assertTrue(vm.canSend)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun aBlankMessageOfSpacesIsStillBlank() = runVmTest {
        val vm = vm()
        try {
            vm.setName("  ")
            vm.setMessage("   ")
            assertFalse(vm.canSend)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun theRequestIsTrimmedAndCarriesTheTypeAndClient() = runVmTest {
        val sent = mutableListOf<ContactReporter.ContactRequest>()
        val vm = vm(record = sent)
        try {
            vm.setType(ContactType.BUG)
            vm.setName("  Ada  ")
            vm.setEmail("  ada@example.org ")
            vm.setMessage("  It stopped  ")
            vm.send("error", "network", "rate limited")
            advanceUntilIdle()

            val request = sent.single()
            assertEquals("bugReport", request.type)
            assertEquals("Ada", request.name)
            assertEquals("ada@example.org", request.email)
            assertEquals("It stopped", request.message)
            assertEquals(ContactReporter.clientId(), request.client)
            assertEquals("", request.company, "the honeypot must stay empty")
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun aDeliveredMessageIsReportedAsSent() = runVmTest {
        val vm = vm(ContactReporter.Outcome.Success)
        try {
            vm.setName("Ada")
            vm.setMessage("Hello")
            vm.send("error", "network", "rate limited")
            advanceUntilIdle()

            assertEquals(SendStatus.Sent, vm.status.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun theRateLimitTellsTheUserToFinishInTheBrowser() = runVmTest {
        val vm = vm(ContactReporter.Outcome.RateLimited)
        try {
            vm.setName("Ada")
            vm.setMessage("Hello")
            vm.send("error", "network", "rate limited")
            advanceUntilIdle()

            assertEquals(SendStatus.Error("rate limited"), vm.status.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun theServersOwnReasonIsPreferredToOurGenericOne() = runVmTest {
        val vm = vm(ContactReporter.Outcome.Invalid("Name is required"))
        try {
            vm.setName("Ada")
            vm.setMessage("Hello")
            vm.send("error", "network", "rate limited")
            advanceUntilIdle()

            assertEquals(SendStatus.Error("Name is required"), vm.status.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun anInvalidAnswerWithNoReasonFallsBackToTheGenericError() = runVmTest {
        val vm = vm(ContactReporter.Outcome.Invalid(null))
        try {
            vm.setName("Ada")
            vm.setMessage("Hello")
            vm.send("error", "network", "rate limited")
            advanceUntilIdle()

            assertEquals(SendStatus.Error("error"), vm.status.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun anUnreachableServerBlamesTheConnection() = runVmTest {
        val vm = vm(ContactReporter.Outcome.NetworkError)
        try {
            vm.setName("Ada")
            vm.setMessage("Hello")
            vm.send("error", "network", "rate limited")
            advanceUntilIdle()

            assertEquals(SendStatus.Error("network"), vm.status.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun aFailedSendCanBeCorrectedAndRetried() = runVmTest {
        val sent = mutableListOf<ContactReporter.ContactRequest>()
        val vm = ContactViewModel { request -> sent += request; ContactReporter.Outcome.NetworkError }
        try {
            vm.setName("Ada")
            vm.setMessage("Hello")
            vm.send("error", "network", "rate limited")
            advanceUntilIdle()

            vm.dismissError()
            assertEquals(SendStatus.Idle, vm.status.value)
            assertTrue(vm.canSend, "the form must still be sendable after a failure")

            vm.send("error", "network", "rate limited")
            advanceUntilIdle()
            assertEquals(2, sent.size)
        } finally {
            tearDown(vm)
        }
    }
}
