package com.church.presenter.churchpresentermobile.model

/**
 * Typed toast/snackbar event emitted by the ViewModel.
 * Resolved to a localised string in the UI layer via `stringResource()`.
 */
sealed class ToastEvent {
    // ── Static messages ───────────────────────────────────────────────────
    data object SongLive          : ToastEvent()
    data object RequestFailed     : ToastEvent()
    data object NoSongSelected    : ToastEvent()
    data object RequestDenied     : ToastEvent()
    data object SessionBlocked    : ToastEvent()

    // ── Dynamic messages (carry data resolved by the UI) ──────────────────
    data class SongAddedToSchedule(val title: String)  : ToastEvent()
    data class FailedToProject(val reason: String)     : ToastEvent()
    data class FailedToAddSchedule(val reason: String) : ToastEvent()
    data class RequestRejected(val httpStatus: Int)    : ToastEvent()
    data class RequestRejectedWithReason(val reason: String) : ToastEvent()

    // ── Bible-specific messages ───────────────────────────────────────────────
    data object BibleLive                                  : ToastEvent()
    data class BibleAddedToSchedule(val reference: String) : ToastEvent()
    data class FailedToProjectBible(val reason: String)    : ToastEvent()
    data class FailedToAddBibleSchedule(val reason: String): ToastEvent()

    // ── Presentation-specific messages ────────────────────────────────────────
    data class FailedToSelectPresentation(val reason: String)      : ToastEvent()
    data class FailedToAddPresentationSchedule(val reason: String)  : ToastEvent()
    data object UploadUnsupported                                    : ToastEvent()
    data object UploadFileTooLarge                                   : ToastEvent()
    data class UploadServerError(val msg: String)                    : ToastEvent()
    data class UploadFailed(val reason: String)                      : ToastEvent()
    data class UploadReloadFailed(val reason: String)                : ToastEvent()
}
