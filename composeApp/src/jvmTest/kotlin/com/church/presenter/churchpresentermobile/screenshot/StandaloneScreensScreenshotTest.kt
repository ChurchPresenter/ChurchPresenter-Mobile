package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.runtime.Composable
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.SlideDeckBuilder
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.standalone.LocalNoticesScreen
import com.church.presenter.churchpresentermobile.ui.standalone.LocalPhotosScreen
import com.church.presenter.churchpresentermobile.ui.standalone.DisplaySettingsPage
import com.church.presenter.churchpresentermobile.ui.standalone.LiveBar
import com.church.presenter.churchpresentermobile.ui.standalone.LiveChip
import com.church.presenter.churchpresentermobile.ui.standalone.StandaloneFixture
import com.church.presenter.churchpresentermobile.ui.standalone.libraryWith
import com.church.presenter.churchpresentermobile.ui.standalone.notice
import com.church.presenter.churchpresentermobile.ui.standalone.photoLibraryWith
import com.church.presenter.churchpresentermobile.viewmodel.LocalPhotosViewModel
import com.church.presenter.churchpresentermobile.viewmodel.StandaloneViewModel
import kotlin.test.Test

/**
 * Standalone mode's own screens — the phone driving the room by itself.
 *
 * Built on `StandaloneFixture`, the engine-and-registry pair the behavioural
 * standalone tests use, so the live bar and Display page are looking at a real
 * registry with a real sink in it.
 *
 * The state that matters most across these is whether there is an output at
 * all: with nothing attached, projecting is not something the operator should
 * be invited to do, and each screen says so differently.
 */
class StandaloneScreensScreenshotTest {

    /** A hymn loaded on the fixture's engine, as the live bar is only drawn with something up. */
    private fun loaded(): Pair<StandaloneFixture, AppSettings> {
        val fixture = StandaloneFixture()
        fixture.engine.setDeck(
            SlideDeckBuilder.fromAnnouncements(
                items = listOf("Amazing grace, how sweet the sound", "That saved a wretch like me"),
                title = "Amazing Grace",
            )
        )
        return fixture to AppSettings(InMemorySettingsStorage())
    }

    private fun liveBar(blank: Boolean = false): @Composable () -> Unit {
        val (fixture, settings) = loaded()
        if (blank) fixture.engine.toggleBlank()
        return {
            LiveBar(
                engine = fixture.engine,
                registry = fixture.registry,
                settings = settings,
                onCollapse = {},
                onOpenScreens = {},
                providedViewModel = StandaloneViewModel(fixture.engine, fixture.registry, settings, null),
            )
        }
    }

    @Test
    fun liveBarLoaded() = screenshot("live-bar__loaded", content = liveBar())

    @Test
    fun liveBarBlank() = screenshot("live-bar__blank", content = liveBar(blank = true))

    @Test
    fun liveChip() = screenshot("live-chip__loaded") {
        val (fixture, settings) = loaded()
        LiveChip(
            engine = fixture.engine,
            registry = fixture.registry,
            settings = settings,
            onExpand = {},
            providedViewModel = StandaloneViewModel(fixture.engine, fixture.registry, settings, null),
        )
    }

    private fun displayPage(photoCount: Int? = null): @Composable () -> Unit {
        val fixture = StandaloneFixture()
        val settings = AppSettings(InMemorySettingsStorage())
        val photos = photoCount?.let { photoLibraryWith(it) }
        return {
            DisplaySettingsPage(
                engine = fixture.engine,
                registry = fixture.registry,
                settings = settings,
                photos = photos,
                providedViewModel = StandaloneViewModel(fixture.engine, fixture.registry, settings, photos),
            )
        }
    }

    @Test
    fun displaySettings() = screenshot("display-settings__idle", content = displayPage())

    @Test
    fun displaySettingsWithPhotos() = screenshot(
        "display-settings__with-photos",
        content = displayPage(photoCount = 4),
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
}
