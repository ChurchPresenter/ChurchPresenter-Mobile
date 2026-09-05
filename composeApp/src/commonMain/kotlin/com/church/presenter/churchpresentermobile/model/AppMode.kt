package com.church.presenter.churchpresentermobile.model

/**
 * How the app projects content.
 *
 * [REMOTE] is the original behaviour: every projection action is a WebSocket
 * message to the ChurchPresenter desktop, which owns the presenter window.
 *
 * [STANDALONE] makes the phone itself the presenter — slides are rendered
 * locally and pushed to attached output sinks (external display, phone-hosted
 * web page, Cast). No desktop is required.
 */
enum class AppMode {
    REMOTE,
    STANDALONE,
}
