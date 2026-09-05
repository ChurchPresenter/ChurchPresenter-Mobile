package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.network.WsSender
import kotlinx.coroutines.flow.StateFlow

/**
 * Sits where [com.church.presenter.churchpresentermobile.network.ServerEventService]
 * used to sit and decides, per action, whether it belongs to the desktop or to
 * the phone's own presenter.
 *
 * Because it implements [WsSender], every existing service — `SongService`,
 * `BibleService`, `PresentationService`, `PicturesService`, `AnnouncementService`,
 * `MediaCastService` — is constructed exactly as before and is unaware that
 * standalone mode exists.
 *
 * The router carries *actions*, not content. The desktop protocol is
 * index-based (`{"number":"42","section":2}`), which a local renderer cannot
 * turn into text, so the materialised slides take a second, explicit path:
 * the ViewModel that already holds the lyrics or verses calls
 * [StandaloneEngine.setDeck] / [StandaloneEngine.showSlide] directly, and those
 * calls are no-ops in remote mode.
 *
 * @param mode Read per call, so switching mode takes effect without rebuilding anything.
 * @param remote The real WebSocket transport to the desktop.
 * @param standalone The local presenter engine.
 */
class ProjectionRouter(
    private val mode: StateFlow<AppMode>,
    private val remote: WsSender,
    private val standalone: StandaloneEngine,
) : WsSender {

    override suspend fun sendAction(
        type: String,
        payloadJson: String,
        fireAndForget: Boolean,
    ): Result<Unit> = when (mode.value) {
        AppMode.REMOTE -> remote.sendAction(type, payloadJson, fireAndForget)
        AppMode.STANDALONE -> standalone.handleRemoteAction(type, payloadJson)
    }
}
