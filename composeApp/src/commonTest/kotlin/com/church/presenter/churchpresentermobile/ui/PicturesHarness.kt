package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.PictureImage
import com.church.presenter.churchpresentermobile.model.PicturesFolder
import com.church.presenter.churchpresentermobile.model.UploadPhotoResponse
import com.church.presenter.churchpresentermobile.network.PicturesService
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.viewmodel.PicturesViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

/**
 * Setup for the pictures grid.
 *
 * The grid is a mirror of one folder open on the desktop, so [FakePictureDesktop]
 * stands in for that computer: it answers the folder request, records the
 * WebSocket actions the screen fires, and can be made slow or hostile.
 *
 * Thumbnails are deliberately left without a URL. Coil is given a real
 * [ImageLoader], and a tile with nothing to fetch settles into its error state
 * immediately — no socket is opened, so no test waits on one. What a tile *looks*
 * like is not what these tests are about; which tile the operator tapped is.
 */
private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

internal fun picture(index: Int, fileName: String? = "photo-$index.jpg") =
    PictureImage(index = index, fileName = fileName, thumbnailUrl = null)

internal fun picturesFolder(
    id: String? = "sunday-morning",
    name: String? = "Sunday Morning",
    images: List<PictureImage> = listOf(picture(0), picture(1), picture(2)),
    total: Int? = null,
) = PicturesFolder(
    folderId = id,
    folderName = name,
    folderPath = "/pictures/$id",
    imageTotal = total ?: images.size,
    images = images,
)

/** The desktop the pictures grid mirrors. */
internal class FakePictureDesktop(
    var folder: PicturesFolder? = picturesFolder(),
    var folderStatus: HttpStatusCode = HttpStatusCode.OK,
    var upload: UploadPhotoResponse? = UploadPhotoResponse(
        folderId = "device_uploads",
        imageIndex = 7,
        fileName = "from-phone.jpg",
    ),
    var uploadStatus: HttpStatusCode = HttpStatusCode.OK,
    /** Makes the upload take long enough for a test to see the screen mid-upload. */
    var uploadDelayMs: Long = 0,
) {
    val sender = FakeWsSender()

    /** The path of every folder request the screen has made. */
    val folderRequests = mutableListOf<String>()

    /** The body of every photo upload. */
    val uploads = mutableListOf<String>()

    /** Every WebSocket action type the screen has fired, in order. */
    val actions: List<String> get() = sender.calls.map { it.first }

    fun payloadsOf(type: String): List<String> =
        sender.calls.filter { it.first == type }.map { it.second }

    private fun client() = HttpClient(MockEngine { request ->
        val path = request.url.encodedPath
        if (path.endsWith("/upload")) {
            uploads += request.body.toString()
            if (uploadDelayMs > 0) delay(uploadDelayMs)
            val body = upload
            if (uploadStatus != HttpStatusCode.OK || body == null) {
                respond("upload refused", HttpStatusCode.InternalServerError)
            } else {
                respond(json.encodeToString(UploadPhotoResponse.serializer(), body))
            }
        } else {
            folderRequests += path
            val body = folder
            if (folderStatus != HttpStatusCode.OK) respond("boom", folderStatus)
            else if (body == null) respond("{}")
            else respond(json.encodeToString(PicturesFolder.serializer(), body))
        }
    })

    fun viewModel(): PicturesViewModel {
        val settings = AppSettings(InMemorySettingsStorage())
        return PicturesViewModel(
            appSettings = settings,
            eventService = sender,
            serviceFactory = { PicturesService(it, sender, client(), client()) },
        )
    }
}

/**
 * An image loader that never reaches the network.
 *
 * Every tile in these tests has a null thumbnail, so this loader is asked for
 * nothing it would have to fetch.
 */
@Composable
internal fun offlineImageLoader(): ImageLoader = ImageLoader(LocalPlatformContext.current)

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showPictures(
    viewModel: PicturesViewModel,
    settingsSaveToken: Int = 0,
    canUploadFiles: Boolean = true,
    pendingNavFolderId: String? = null,
    pendingNavImageIndex: Int? = null,
    onPendingNavHandled: () -> Unit = {},
    onScheduleRefresh: () -> Unit = {},
) = showScreen {
    PicturesScreen(
        appSettings = AppSettings(InMemorySettingsStorage()),
        settingsSaveToken = settingsSaveToken,
        imageLoader = offlineImageLoader(),
        pendingNavFolderId = pendingNavFolderId,
        pendingNavImageIndex = pendingNavImageIndex,
        onPendingNavHandled = onPendingNavHandled,
        onScheduleRefresh = onScheduleRefresh,
        canUploadFiles = canUploadFiles,
        providedViewModel = viewModel,
    )
}
