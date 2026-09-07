package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.FakePictureDesktop
import com.church.presenter.churchpresentermobile.ui.PicturesScreen
import com.church.presenter.churchpresentermobile.ui.UiTags
import com.church.presenter.churchpresentermobile.ui.offlineImageLoader
import com.church.presenter.churchpresentermobile.ui.picture
import com.church.presenter.churchpresentermobile.ui.picturesFolder
import io.ktor.http.HttpStatusCode
import kotlin.test.Test

/**
 * The pictures grid, mirrored from the folder the desktop has open.
 *
 * Built on `FakePictureDesktop` — the same stand-in computer `PicturesGridTest`
 * drives — so the screen is filled the way the app fills it: a folder request
 * over a mock engine, answered into a real [PicturesViewModel].
 *
 * Because that answer arrives on another dispatcher, each capture waits for a
 * tile (or the empty state) before the shutter opens. Without the wait these
 * would all be pictures of a spinner — and would then pass forever.
 */
@OptIn(ExperimentalTestApi::class)
class PicturesScreenshotTest {

    private fun pictures(desktop: FakePictureDesktop): @Composable () -> Unit {
        val viewModel = desktop.viewModel()
        return {
            PicturesScreen(
                appSettings = AppSettings(InMemorySettingsStorage()),
                settingsSaveToken = 0,
                imageLoader = offlineImageLoader(),
                canUploadFiles = true,
                providedViewModel = viewModel,
            )
        }
    }

    @Test
    fun grid() = screenshot(
        "pictures__grid",
        until = { onAllNodes(hasTestTag(UiTags.pictureCell(0))).fetchSemanticsNodes().isNotEmpty() },
        content = pictures(FakePictureDesktop()),
    )

    @Test
    fun oneImage() = screenshot(
        "pictures__single-image",
        until = { onAllNodes(hasTestTag(UiTags.pictureCell(0))).fetchSemanticsNodes().isNotEmpty() },
        content = pictures(
            FakePictureDesktop(folder = picturesFolder(images = listOf(picture(0)))),
        ),
    )

    @Test
    fun emptyFolder() = screenshot(
        "pictures__empty-folder",
        // A folder with no images does not draw the grid at all — and so never
        // draws the folder header either. The wait is on the empty notice, which
        // is what this state actually puts on screen.
        until = { onAllNodes(hasTestTag(UiTags.PICTURES_EMPTY)).fetchSemanticsNodes().isNotEmpty() },
        content = pictures(FakePictureDesktop(folder = picturesFolder(images = emptyList()))),
    )

    @Test
    fun desktopRefusedTheFolder() = screenshot(
        // The desktop answered, but not with a folder — the screen has to say
        // so rather than sit on an empty grid.
        "pictures__folder-error",
        until = { onAllNodes(hasTestTag(UiTags.PICTURES_EMPTY)).fetchSemanticsNodes().isNotEmpty() },
        content = pictures(
            FakePictureDesktop(folder = null, folderStatus = HttpStatusCode.InternalServerError),
        ),
    )

    @Test
    fun gridOnATablet() = screenshot(
        "pictures__tablet",
        width = Screenshots.TABLET_WIDTH,
        until = { onAllNodes(hasTestTag(UiTags.pictureCell(0))).fetchSemanticsNodes().isNotEmpty() },
        content = pictures(FakePictureDesktop()),
    )
}
