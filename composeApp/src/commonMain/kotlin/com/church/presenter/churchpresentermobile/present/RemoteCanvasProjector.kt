package com.church.presenter.churchpresentermobile.present

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.ApiConstants
import com.church.presenter.churchpresentermobile.network.createActionHttpClient
import com.church.presenter.churchpresentermobile.network.identifyDevice
import com.church.presenter.churchpresentermobile.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val TAG = "RemoteCanvasProjector"

/** What the desktop calls a canvas page on its outputs, in `live_state_changed`. */
internal const val DESKTOP_DRAWING = "DRAWING"

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * Puts a canvas page on the desktop's outputs in remote mode.
 *
 * Each frame is sent as a PNG to `POST /api/drawing/frame`. Going live asks the
 * operator at the desktop first, exactly as projecting a song does, and the request
 * waits for their answer — which can take as long as they take, so it goes through
 * the client with no request timeout. Frames sent while the answer is pending are kept
 * by the desktop, so what the operator approves is the page as it is now. After that,
 * frames refresh the page without asking again, until something else takes the screen.
 *
 * Whether the page is still up comes from the desktop's own `live_state_changed`
 * event ([liveContentType]), so the canvas notices at once when the operator puts
 * something else up.
 *
 * @param liveContentType What the desktop's outputs are showing, from its events.
 */
class RemoteCanvasProjector(
    private val settings: AppSettings,
    liveContentType: StateFlow<String?>,
    private val client: HttpClient = createActionHttpClient(),
) : CanvasProjection {

    override val asksFirst: Boolean = true

    override val showing: Flow<Boolean> = liveContentType.map { it == DESKTOP_DRAWING }.distinctUntilChanged()

    override suspend fun show(png: ByteArray, paper: String, title: String, takeOver: Boolean): CanvasShowResult =
        runCatching {
            val response = client.post("${settings.apiBaseUrl}/${ApiConstants.DRAWING_FRAME_ENDPOINT}") {
                parameter(PARAM_TAKE_OVER, takeOver)
                parameter(PARAM_PAPER, paper)
                if (title.isNotBlank()) parameter(PARAM_TITLE, title)
                contentType(ContentType.Image.PNG)
                setBody(png)
                authorise()
            }
            showResultOf(response.status, response.bodyAsText())
        }.getOrElse { e ->
            if (e is CancellationException) throw e
            // Unreachable, refused or reset: the page is not on the desktop's screen.
            Logger.e(TAG, "show — failed: ${e.message}", e)
            CanvasShowResult.FAILED
        }

    override suspend fun stop() {
        runCatching {
            client.post("${settings.apiBaseUrl}/${ApiConstants.DRAWING_END_ENDPOINT}") { authorise() }
        }.onFailure { e ->
            if (e is CancellationException) throw e
            // Nothing more to do: the operator can still clear the screen at the desktop.
            Logger.e(TAG, "stop — failed: ${e.message}", e)
        }
    }

    private fun HttpRequestBuilder.authorise() {
        val key = settings.apiKey
        if (key.isNotBlank()) header(ApiConstants.API_KEY_HEADER, key)
        identifyDevice(settings)
    }

    private companion object {
        const val PARAM_TAKE_OVER = "takeOver"
        const val PARAM_PAPER = "paper"
        const val PARAM_TITLE = "title"
    }
}

/**
 * What a `/api/drawing/frame` answer means for the canvas: shown; not shown because
 * something else has the screen (or approval is still pending); declined by the
 * operator; or failed — including a desktop too old to know the route, which
 * answers 404. A plain function over the status and body, so it is testable
 * without a server.
 */
internal fun showResultOf(status: HttpStatusCode, body: String): CanvasShowResult {
    val reason = fieldOf(body, "reason")
    return when {
        status == HttpStatusCode.Forbidden && reason == REASON_DENIED -> CanvasShowResult.DECLINED
        !status.isSuccess() -> CanvasShowResult.FAILED
        fieldOf(body, "showing")?.toBooleanStrictOrNull() == true -> CanvasShowResult.SHOWN
        else -> CanvasShowResult.NOT_SHOWING
    }
}

private const val REASON_DENIED = "denied"

/** A top-level string or boolean field of a JSON object [body], or null. */
private fun fieldOf(body: String, name: String): String? = runCatching {
    json.parseToJsonElement(body).jsonObject[name]?.jsonPrimitive?.let { it.booleanOrNull?.toString() ?: it.content }
}.getOrNull()
