package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.network.ContactReporter
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "ContactViewModel"

/** The kinds of message the server accepts, in the order the desktop dialog lists them. */
enum class ContactType(val key: String) {
    FEATURE("featureRequest"),
    FEEDBACK("feedback"),
    TESTIMONIAL("testimonial"),
    BUG("bugReport"),
}

/** Where a submission has got to, for the screen to render. */
sealed interface SendStatus {
    data object Idle : SendStatus
    data object Sending : SendStatus
    data object Sent : SendStatus
    data class Error(val text: String) : SendStatus
}

/**
 * The contact form: what the user typed, and the one press that sends it.
 *
 * @param submit Injected so the send path is testable without a socket. Defaults
 *   to the real reporter.
 */
class ContactViewModel(
    private val submit: suspend (ContactReporter.ContactRequest) -> ContactReporter.Outcome =
        ContactReporter::submit,
) : ViewModel() {

    private val _type = MutableStateFlow(ContactType.FEATURE)
    val type: StateFlow<ContactType> = _type.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    private val _status = MutableStateFlow<SendStatus>(SendStatus.Idle)
    val status: StateFlow<SendStatus> = _status.asStateFlow()

    fun setType(value: ContactType) { _type.value = value }
    fun setName(value: String) { _name.value = value }
    fun setEmail(value: String) { _email.value = value }
    fun setMessage(value: String) { _message.value = value }

    /**
     * Whether the form is worth sending.
     *
     * Mirrors the server's own rules — name and message required — so a request
     * that would come back 400 is never sent in the first place.
     */
    val canSend: Boolean
        get() = _name.value.isNotBlank() &&
            _message.value.isNotBlank() &&
            _status.value !is SendStatus.Sending

    /** Builds the request from the current fields, trimmed the way the server expects. */
    internal fun buildRequest(): ContactReporter.ContactRequest = ContactReporter.ContactRequest(
        type = _type.value.key,
        name = _name.value.trim(),
        message = _message.value.trim(),
        email = _email.value.trim(),
        context = ContactReporter.defaultContext(),
        client = ContactReporter.clientId(),
    )

    /**
     * Turns an outcome into the status to show, using the caller-supplied texts
     * so this stays free of resource lookups and testable headlessly.
     */
    internal fun statusFor(
        outcome: ContactReporter.Outcome,
        errorText: String,
        networkText: String,
        rateLimitedText: String,
    ): SendStatus = when (outcome) {
        ContactReporter.Outcome.Success -> SendStatus.Sent
        ContactReporter.Outcome.RateLimited -> SendStatus.Error(rateLimitedText)
        is ContactReporter.Outcome.Invalid -> SendStatus.Error(outcome.error ?: errorText)
        ContactReporter.Outcome.NetworkError -> SendStatus.Error(networkText)
        ContactReporter.Outcome.Failure -> SendStatus.Error(errorText)
    }

    fun send(errorText: String, networkText: String, rateLimitedText: String) {
        if (!canSend) return
        _status.value = SendStatus.Sending
        viewModelScope.launch {
            val outcome = submit(buildRequest())
            _status.value = statusFor(outcome, errorText, networkText, rateLimitedText)
            Logger.d(TAG, "send — outcome=$outcome")
        }
    }

    /** Clears an error so the user can correct the form and try again. */
    fun dismissError() {
        if (_status.value is SendStatus.Error) _status.value = SendStatus.Idle
    }
}
