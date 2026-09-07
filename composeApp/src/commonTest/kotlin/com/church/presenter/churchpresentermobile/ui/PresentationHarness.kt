package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.Presentation
import com.church.presenter.churchpresentermobile.model.PresentationSlide
import com.church.presenter.churchpresentermobile.model.PresentationsResponse
import com.church.presenter.churchpresentermobile.model.UploadPresentationResponse
import com.church.presenter.churchpresentermobile.network.PresentationService
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.viewmodel.PresentationsViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

/**
 * Setup for the presentations list.
 *
 * As with the pictures grid, slides carry no thumbnail URL: Coil is handed a
 * real loader, a tile with nothing to fetch settles immediately, and no test
 * waits on a socket. What is asserted is which slide the tap named — the desktop
 * jumps a live deck to it.
 */
private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

internal fun deck(
    id: String? = "sermon",
    fileName: String? = "Sermon.pptx",
    slides: Int = 4,
    slideTotal: Int? = null,
) = Presentation(
    id = id,
    fileName = fileName,
    fileType = "pptx",
    slideTotal = slideTotal ?: slides,
    slides = (0 until slides).map { PresentationSlide(slideIndex = it, thumbnailUrl = null) },
)

/** The desktop the presentations list mirrors. */
internal class FakeDeckDesktop(
    var decks: List<Presentation> = listOf(deck(), deck(id = "notices", fileName = "Notices.pdf", slides = 2)),
    var listStatus: HttpStatusCode = HttpStatusCode.OK,
    var byId: Presentation? = null,
    var byIdStatus: HttpStatusCode = HttpStatusCode.OK,
    /**
     * The upload answer. Its id names a deck the list already returns, so the
     * ViewModel's "wait until the desktop has rendered it" poll finds it on the
     * first attempt — an id that never appears would leave a 30-second poll
     * running behind the test.
     */
    var upload: UploadPresentationResponse? = UploadPresentationResponse(id = "sermon", name = "Sermon.pptx"),
    var uploadStatus: HttpStatusCode = HttpStatusCode.OK,
    /** Makes the upload take long enough for a test to see the screen mid-upload. */
    var uploadDelayMs: Long = 0,
) {
    val sender = FakeWsSender()

    /** The path of every list or by-id request the screen has made. */
    val listRequests = mutableListOf<String>()

    /** The body of every uploaded file. */
    val uploads = mutableListOf<String>()

    val actions: List<String> get() = sender.calls.map { it.first }

    fun payloadsOf(type: String): List<String> =
        sender.calls.filter { it.first == type }.map { it.second }

    private fun client() = HttpClient(MockEngine { request ->
        val path = request.url.encodedPath
        when {
            path.endsWith("/upload") -> {
                uploads += request.body.toString()
                if (uploadDelayMs > 0) delay(uploadDelayMs)
                val body = upload
                if (uploadStatus != HttpStatusCode.OK || body == null) {
                    respond("upload refused", HttpStatusCode.InternalServerError)
                } else {
                    respond(json.encodeToString(UploadPresentationResponse.serializer(), body))
                }
            }
            path.endsWith("/presentations") -> {
                listRequests += path
                if (listStatus != HttpStatusCode.OK) respond("boom", listStatus)
                else respond(
                    json.encodeToString(
                        PresentationsResponse.serializer(),
                        PresentationsResponse(presentations = decks, total = decks.size),
                    )
                )
            }
            else -> {
                listRequests += path
                val one = byId ?: decks.firstOrNull { path.endsWith("/${it.id}") }
                if (byIdStatus != HttpStatusCode.OK || one == null) {
                    respond("no such presentation", HttpStatusCode.NotFound)
                } else {
                    respond(json.encodeToString(Presentation.serializer(), one))
                }
            }
        }
    })

    fun viewModel(): PresentationsViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        return PresentationsViewModel(
            appSettings = settings,
            eventService = sender,
            serviceFactory = { PresentationService(it, sender, client(), client()) },
        )
    }
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showPresentations(
    viewModel: PresentationsViewModel,
    settingsSaveToken: Int = 0,
    canUploadFiles: Boolean = true,
    pendingNavPresentationId: String? = null,
    onPendingNavHandled: () -> Unit = {},
    onScheduleRefresh: () -> Unit = {},
) = showScreen {
    PresentationScreen(
        appSettings = AppSettings(InMemorySettingsStorage()),
        settingsSaveToken = settingsSaveToken,
        imageLoader = offlineImageLoader(),
        pendingNavPresentationId = pendingNavPresentationId,
        onPendingNavHandled = onPendingNavHandled,
        onScheduleRefresh = onScheduleRefresh,
        canUploadFiles = canUploadFiles,
        providedViewModel = viewModel,
    )
}
