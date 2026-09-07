package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.network.ContactReporter
import com.church.presenter.churchpresentermobile.viewmodel.ContactType
import com.church.presenter.churchpresentermobile.viewmodel.ContactViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The contact form — the only route a user has to report a bug from inside the
 * app, usually right after something went wrong during a service.
 *
 * The submit function is injected, so these assert on the request that would
 * reach the server rather than on which composable ran. The failures worth
 * guarding: a send that fires with an empty message (the server rejects it and
 * the user's report is lost), a type chip that reports the wrong kind, and a
 * failure shown as a success.
 */
@OptIn(ExperimentalTestApi::class)
class ContactScreenTest {

    /** Records what would be sent, and answers with [outcome]. */
    private class Recorder(private val outcome: ContactReporter.Outcome) {
        var request: ContactReporter.ContactRequest? = null
        val submit: suspend (ContactReporter.ContactRequest) -> ContactReporter.Outcome = {
            request = it
            outcome
        }
    }

    private fun ComposeUiTest.showContact(vm: ContactViewModel) =
        showScreen { ContactScreen(providedViewModel = vm) }

    // ── The fields ───────────────────────────────────────────────────────

    @Test
    fun everyFieldIsOffered() = runComposeUiTest {
        showContact(ContactViewModel())

        assertTrue(exists(UiTags.CONTACT_NAME))
        assertTrue(exists(UiTags.CONTACT_EMAIL))
        assertTrue(exists(UiTags.CONTACT_MESSAGE))
    }

    @Test
    fun typingANameIsKept() = runComposeUiTest {
        val vm = ContactViewModel()
        showContact(vm)

        type(UiTags.CONTACT_NAME, "Andrei")

        assertEquals("Andrei", vm.name.value)
    }

    @Test
    fun typingAnEmailIsKept() = runComposeUiTest {
        val vm = ContactViewModel()
        showContact(vm)

        type(UiTags.CONTACT_EMAIL, "someone@example.org")

        assertEquals("someone@example.org", vm.email.value)
    }

    @Test
    fun typingAMessageIsKept() = runComposeUiTest {
        val vm = ContactViewModel()
        showContact(vm)

        type(UiTags.CONTACT_MESSAGE, "The songs tab hung mid-service")

        assertEquals("The songs tab hung mid-service", vm.message.value)
    }

    @Test
    fun whatWasTypedIsShownBack() = runComposeUiTest {
        val vm = ContactViewModel()
        showContact(vm)

        type(UiTags.CONTACT_NAME, "Andrei")

        assertTrue(isShowing("Andrei"))
    }

    // ── The kind of message ──────────────────────────────────────────────

    @Test
    fun allFourKindsAreOffered() = runComposeUiTest {
        showContact(ContactViewModel())

        ContactType.entries.forEach { type ->
            assertTrue(exists(UiTags.contactType(type.key)), type.name)
        }
    }

    @Test
    fun aFeatureRequestIsTheDefault() = runComposeUiTest {
        val vm = ContactViewModel()
        showContact(vm)

        assertEquals(ContactType.FEATURE, vm.type.value)
    }

    @Test
    fun choosingBugReportsThatKind() = runComposeUiTest {
        val vm = ContactViewModel()
        showContact(vm)

        click(UiTags.contactType(ContactType.BUG.key))

        assertEquals(ContactType.BUG, vm.type.value)
    }

    @Test
    fun choosingTestimonialReportsThatKind() = runComposeUiTest {
        // Each chip has to carry its own kind — one wired to the wrong constant
        // files every report under the wrong heading.
        val vm = ContactViewModel()
        showContact(vm)

        click(UiTags.contactType(ContactType.TESTIMONIAL.key))

        assertEquals(ContactType.TESTIMONIAL, vm.type.value)
    }

    @Test
    fun choosingFeedbackReportsThatKind() = runComposeUiTest {
        val vm = ContactViewModel()
        showContact(vm)

        click(UiTags.contactType(ContactType.FEEDBACK.key))

        assertEquals(ContactType.FEEDBACK, vm.type.value)
    }

    @Test
    fun theChosenKindReachesTheRequest() = runComposeUiTest {
        val recorder = Recorder(ContactReporter.Outcome.Success)
        val vm = ContactViewModel(recorder.submit)
        showContact(vm)

        click(UiTags.contactType(ContactType.BUG.key))
        type(UiTags.CONTACT_NAME, "Andrei")
        type(UiTags.CONTACT_MESSAGE, "It hung")
        click(UiTags.CONTACT_SEND)

        awaitThat { recorder.request != null }
        assertEquals(ContactType.BUG.key, recorder.request?.type)
    }

    @Test
    fun theLastKindChosenIsTheOneSent() = runComposeUiTest {
        val recorder = Recorder(ContactReporter.Outcome.Success)
        val vm = ContactViewModel(recorder.submit)
        showContact(vm)

        click(UiTags.contactType(ContactType.BUG.key))
        click(UiTags.contactType(ContactType.FEEDBACK.key))
        type(UiTags.CONTACT_NAME, "Andrei")
        type(UiTags.CONTACT_MESSAGE, "It hung")
        click(UiTags.CONTACT_SEND)

        awaitThat { recorder.request != null }
        assertEquals(ContactType.FEEDBACK.key, recorder.request?.type)
    }

    // ── Sending ──────────────────────────────────────────────────────────

    @Test
    fun aCompleteFormIsSent() = runComposeUiTest {
        val recorder = Recorder(ContactReporter.Outcome.Success)
        val vm = ContactViewModel(recorder.submit)
        showContact(vm)

        type(UiTags.CONTACT_NAME, "Andrei")
        type(UiTags.CONTACT_MESSAGE, "The songs tab hung")
        click(UiTags.CONTACT_SEND)

        awaitThat { recorder.request != null }
        assertEquals("Andrei", recorder.request?.name)
        assertEquals("The songs tab hung", recorder.request?.message)
    }

    @Test
    fun theEmailIsSentWhenGiven() = runComposeUiTest {
        val recorder = Recorder(ContactReporter.Outcome.Success)
        val vm = ContactViewModel(recorder.submit)
        showContact(vm)

        type(UiTags.CONTACT_NAME, "Andrei")
        type(UiTags.CONTACT_EMAIL, "someone@example.org")
        type(UiTags.CONTACT_MESSAGE, "It hung")
        click(UiTags.CONTACT_SEND)

        awaitThat { recorder.request != null }
        assertEquals("someone@example.org", recorder.request?.email)
    }

    @Test
    fun surroundingSpaceIsTrimmedBeforeSending() = runComposeUiTest {
        // The server's validation counts characters; a message of spaces is a
        // 400 the user never sees the reason for.
        val recorder = Recorder(ContactReporter.Outcome.Success)
        val vm = ContactViewModel(recorder.submit)
        showContact(vm)

        type(UiTags.CONTACT_NAME, "  Andrei  ")
        type(UiTags.CONTACT_MESSAGE, "  It hung  ")
        click(UiTags.CONTACT_SEND)

        awaitThat { recorder.request != null }
        assertEquals("Andrei", recorder.request?.name)
        assertEquals("It hung", recorder.request?.message)
    }

    @Test
    fun anEmptyFormIsNotSent() = runComposeUiTest {
        // The server would reject it — better not to spend the user's only
        // attempt, nor their rate limit, on a request that cannot succeed.
        val recorder = Recorder(ContactReporter.Outcome.Success)
        val vm = ContactViewModel(recorder.submit)
        showContact(vm)

        click(UiTags.CONTACT_SEND)

        assertNull(recorder.request)
    }

    @Test
    fun aFormWithNoMessageIsNotSent() = runComposeUiTest {
        val recorder = Recorder(ContactReporter.Outcome.Success)
        val vm = ContactViewModel(recorder.submit)
        showContact(vm)

        type(UiTags.CONTACT_NAME, "Andrei")
        click(UiTags.CONTACT_SEND)

        assertNull(recorder.request)
    }

    @Test
    fun aFormWithNoNameIsNotSent() = runComposeUiTest {
        val recorder = Recorder(ContactReporter.Outcome.Success)
        val vm = ContactViewModel(recorder.submit)
        showContact(vm)

        type(UiTags.CONTACT_MESSAGE, "It hung")
        click(UiTags.CONTACT_SEND)

        assertNull(recorder.request)
    }

    @Test
    fun aMessageOfOnlySpacesIsNotSent() = runComposeUiTest {
        val recorder = Recorder(ContactReporter.Outcome.Success)
        val vm = ContactViewModel(recorder.submit)
        showContact(vm)

        type(UiTags.CONTACT_NAME, "Andrei")
        type(UiTags.CONTACT_MESSAGE, "     ")
        click(UiTags.CONTACT_SEND)

        assertNull(recorder.request)
    }

    // ── What the user is told afterwards ─────────────────────────────────

    @Test
    fun aSuccessfulSendIsConfirmed() = runComposeUiTest {
        val recorder = Recorder(ContactReporter.Outcome.Success)
        val vm = ContactViewModel(recorder.submit)
        showContact(vm)

        type(UiTags.CONTACT_NAME, "Andrei")
        type(UiTags.CONTACT_MESSAGE, "It hung")
        click(UiTags.CONTACT_SEND)

        awaitThat { exists(UiTags.CONTACT_SENT) }
        assertTrue(exists(UiTags.CONTACT_SENT))
    }

    @Test
    fun nothingIsConfirmedBeforeASend() = runComposeUiTest {
        showContact(ContactViewModel())

        assertFalse(exists(UiTags.CONTACT_SENT))
        assertFalse(exists(UiTags.CONTACT_ERROR))
    }

    @Test
    fun aRejectedSendIsNotShownAsSuccess() = runComposeUiTest {
        // The report did not arrive; saying it did loses it silently.
        val recorder = Recorder(ContactReporter.Outcome.Invalid("Message too short"))
        val vm = ContactViewModel(recorder.submit)
        showContact(vm)

        type(UiTags.CONTACT_NAME, "Andrei")
        type(UiTags.CONTACT_MESSAGE, "It hung")
        click(UiTags.CONTACT_SEND)

        awaitThat { exists(UiTags.CONTACT_ERROR) }
        assertFalse(exists(UiTags.CONTACT_SENT))
    }

    @Test
    fun theServersOwnReasonIsShownWhenItGivesOne() = runComposeUiTest {
        val recorder = Recorder(ContactReporter.Outcome.Invalid("Message too short"))
        val vm = ContactViewModel(recorder.submit)
        showContact(vm)

        type(UiTags.CONTACT_NAME, "Andrei")
        type(UiTags.CONTACT_MESSAGE, "It hung")
        click(UiTags.CONTACT_SEND)

        awaitThat { exists(UiTags.CONTACT_ERROR) }
        assertTrue(isShowing("Message too short"))
    }

    @Test
    fun aNetworkFailureIsReportedAsAFailure() = runComposeUiTest {
        val recorder = Recorder(ContactReporter.Outcome.NetworkError)
        val vm = ContactViewModel(recorder.submit)
        showContact(vm)

        type(UiTags.CONTACT_NAME, "Andrei")
        type(UiTags.CONTACT_MESSAGE, "It hung")
        click(UiTags.CONTACT_SEND)

        awaitThat { exists(UiTags.CONTACT_ERROR) }
        assertFalse(exists(UiTags.CONTACT_SENT))
    }

    @Test
    fun beingRateLimitedIsReportedAsAFailure() = runComposeUiTest {
        val recorder = Recorder(ContactReporter.Outcome.RateLimited)
        val vm = ContactViewModel(recorder.submit)
        showContact(vm)

        type(UiTags.CONTACT_NAME, "Andrei")
        type(UiTags.CONTACT_MESSAGE, "It hung")
        click(UiTags.CONTACT_SEND)

        awaitThat { exists(UiTags.CONTACT_ERROR) }
        assertFalse(exists(UiTags.CONTACT_SENT))
    }

    @Test
    fun theBrowserFallbackIsAlwaysOffered() = runComposeUiTest {
        // The escape hatch when the in-app send will not work at all.
        showContact(ContactViewModel())

        assertTrue(exists(UiTags.CONTACT_OPEN_BROWSER))
    }
}
