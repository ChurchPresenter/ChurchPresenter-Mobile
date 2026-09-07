package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.runtime.Composable
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.standalone.LocalNoticesScreen
import com.church.presenter.churchpresentermobile.ui.standalone.LocalPhotosScreen
import com.church.presenter.churchpresentermobile.ui.standalone.LocalWebScreen
import com.church.presenter.churchpresentermobile.ui.standalone.StandaloneControllerScreen
import com.church.presenter.churchpresentermobile.ui.standalone.StandaloneFixture
import com.church.presenter.churchpresentermobile.ui.standalone.libraryWith
import com.church.presenter.churchpresentermobile.ui.standalone.notice
import com.church.presenter.churchpresentermobile.ui.standalone.photoLibraryWith
import com.church.presenter.churchpresentermobile.viewmodel.LocalPhotosViewModel
import com.church.presenter.churchpresentermobile.viewmodel.LocalWebViewModel
import com.church.presenter.churchpresentermobile.viewmodel.StandaloneViewModel
import kotlin.test.Test

/**
 * Standalone mode's own screens — the phone driving the room by itself.
 *
 * Built on `StandaloneFixture`, the engine-and-registry pair the behavioural
 * standalone tests use, so the controller is looking at a real registry with a
 * real sink in it.
 *
 * The state that matters most across these is whether there is an output at
 * all: with nothing attached, projecting is not something the operator should
 * be invited to do, and each screen says so differently.
 */
class StandaloneScreensScreenshotTest {

    private fun controller(photoCount: Int? = null): @Composable () -> Unit {
        val fixture = StandaloneFixture()
        val settings = AppSettings(InMemorySettingsStorage())
        val photos = photoCount?.let { photoLibraryWith(it) }
        return {
            StandaloneControllerScreen(
                engine = fixture.engine,
                registry = fixture.registry,
                settings = settings,
                photos = photos,
                providedViewModel = StandaloneViewModel(fixture.engine, fixture.registry, settings, photos),
            )
        }
    }

    @Test
    fun controllerScreen() = screenshot("standalone-controller__idle", content = controller())

    @Test
    fun controllerWithPhotos() = screenshot(
        "standalone-controller__with-photos",
        content = controller(photoCount = 4),
    )

    @Test
    fun notices() = screenshot("local-notices__populated") {
        val fixture = StandaloneFixture()
        LocalNoticesScreen(
            repository = libraryWith(
                notice("n1", title = "Welcome", body = "Coffee in the hall after the service"),
                notice("n2", title = "Working bee", body = "Saturday 9am"),
            ),
            presenter = fixture.engine,
            hasOutput = true,
        )
    }

    @Test
    fun noticesEmpty() = screenshot("local-notices__empty") {
        val fixture = StandaloneFixture()
        LocalNoticesScreen(repository = libraryWith(), presenter = fixture.engine, hasOutput = true)
    }

    @Test
    fun noticesWithNoOutput() = screenshot("local-notices__no-output") {
        // Nothing is attached, so there is nowhere to project to — the screen
        // has to say that rather than offer a button that does nothing.
        val fixture = StandaloneFixture()
        LocalNoticesScreen(
            repository = libraryWith(notice("n1")),
            presenter = fixture.engine,
            hasOutput = false,
        )
    }

    @Test
    fun photos() = screenshot("local-photos__populated") {
        val fixture = StandaloneFixture()
        val library = photoLibraryWith(count = 4)
        LocalPhotosScreen(
            library = library,
            presenter = fixture.engine,
            providedViewModel = LocalPhotosViewModel(library, fixture.engine),
        )
    }

    @Test
    fun photosEmpty() = screenshot("local-photos__empty") {
        val fixture = StandaloneFixture()
        val library = photoLibraryWith(count = 0)
        LocalPhotosScreen(
            library = library,
            presenter = fixture.engine,
            providedViewModel = LocalPhotosViewModel(library, fixture.engine),
        )
    }

    @Test
    fun photosBeforeTheServerIsUp() = screenshot("local-photos__no-server") {
        // Photos are picked but the phone's own server has no address yet, so
        // none of them can be shown.
        val fixture = StandaloneFixture()
        val library = photoLibraryWith(count = 2, baseUrl = null)
        LocalPhotosScreen(
            library = library,
            presenter = fixture.engine,
            providedViewModel = LocalPhotosViewModel(library, fixture.engine),
        )
    }

    @Test
    fun webPage() = screenshot("local-web__ready") {
        val fixture = StandaloneFixture()
        LocalWebScreen(
            presenter = fixture.engine,
            hasOutput = true,
            providedViewModel = LocalWebViewModel(presenter = fixture.engine, refusesFraming = { false }),
        )
    }

    @Test
    fun webPageWithNoOutput() = screenshot("local-web__no-output") {
        val fixture = StandaloneFixture()
        LocalWebScreen(
            presenter = fixture.engine,
            hasOutput = false,
            providedViewModel = LocalWebViewModel(presenter = fixture.engine, refusesFraming = { false }),
        )
    }
}
