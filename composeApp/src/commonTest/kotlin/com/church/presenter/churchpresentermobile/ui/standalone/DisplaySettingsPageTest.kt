package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.SlideBackdrop
import com.church.presenter.churchpresentermobile.present.SinkRegistry
import com.church.presenter.churchpresentermobile.present.SinkState
import com.church.presenter.churchpresentermobile.present.SinkStatus
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import com.church.presenter.churchpresentermobile.ui.tagged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Settings → Display: which screens are attached, and the backdrop behind the
 * words.
 *
 * The screens list is the only place the app admits that nothing is connected,
 * so a TV unplugged ten minutes ago still listed as showing the slide is the
 * failure worth catching. A backdrop travels to every output as part of each
 * slide, so its tests assert on the engine, which is what the screens read.
 */
@OptIn(ExperimentalTestApi::class)
class DisplaySettingsPageTest {

    /** An engine over sinks whose state the test decides. */
    private class Screens(vararg sinks: TestSink) {
        val registry = SinkRegistry()
        val engine = StandaloneEngine(MutableStateFlow(AppMode.STANDALONE), registry)

        init {
            sinks.forEach { registry.register(it) }
        }

        fun publish(sink: TestSink, status: SinkStatus) {
            sink.publish(status)
            registry.refreshStatuses()
        }
    }

    // ── Screens ──────────────────────────────────────────────────────────

    @Test
    fun withNoScreensTheListSaysSo() = runComposeUiTest {
        val s = Screens()

        showDisplaySettings(s.engine, s.registry)

        assertTrue(exists(StandaloneTags.OUTPUTS_EMPTY))
    }

    @Test
    fun anAttachedScreenIsListed() = runComposeUiTest {
        val s = Screens(TestSink("tv", "Sanctuary TV", SinkState.ATTACHED))

        showDisplaySettings(s.engine, s.registry)

        assertTrue(exists(StandaloneTags.sink("tv")))
        assertFalse(exists(StandaloneTags.OUTPUTS_EMPTY))
    }

    @Test
    fun aScreenThatArrivesLaterIsListedAsItArrives() = runComposeUiTest {
        val tv = TestSink("tv", "External display", SinkState.DETACHED)
        val s = Screens(tv)
        showDisplaySettings(s.engine, s.registry)

        s.publish(tv, SinkStatus("tv", "Sanctuary TV", SinkState.ATTACHED))

        awaitThat { exists(StandaloneTags.sink("tv")) }
    }

    @Test
    fun aServingBrowserShowsTheAddressToType() = runComposeUiTest {
        val s = Screens(
            TestSink("web_page", "Browser", SinkState.ATTACHED, detail = "http://192.168.1.50:8766"),
        )

        showDisplaySettings(s.engine, s.registry)

        assertTrue(exists(StandaloneTags.sinkUrl("web_page")))
    }

    // ── Backdrop ─────────────────────────────────────────────────────────

    @Test
    fun theBackdropStartsAsAGradient() = runComposeUiTest {
        val f = StandaloneFixture()

        showDisplaySettings(f.engine, f.registry)

        tagged(StandaloneTags.backdrop(0)).assertIsSelected()
    }

    @Test
    fun theBackdropCanBeMadeBlack() = runComposeUiTest {
        val f = StandaloneFixture()
        showDisplaySettings(f.engine, f.registry)

        click(StandaloneTags.backdrop(2))

        awaitThat { f.engine.backdrop.value == SlideBackdrop.BLACK }
    }

    @Test
    fun theBackdropCanGoBackToAGradient() = runComposeUiTest {
        val f = StandaloneFixture()
        showDisplaySettings(f.engine, f.registry)
        click(StandaloneTags.backdrop(2))
        awaitThat { f.engine.backdrop.value == SlideBackdrop.BLACK }

        click(StandaloneTags.backdrop(0))

        awaitThat { f.engine.backdrop.value == SlideBackdrop.GRADIENT }
    }

    @Test
    fun choosingAnImageBackdropOffersThePhotos() = runComposeUiTest {
        // Choosing IMAGE is only half the instruction — without a photo the
        // audience page falls back to the gradient and the option looks broken.
        val f = StandaloneFixture()
        showDisplaySettings(f.engine, f.registry, photos = photoLibraryWith(2))

        click(StandaloneTags.backdrop(1))

        awaitThat { exists(StandaloneTags.backdropPhoto("p0")) }
    }

    @Test
    fun theOtherBackdropsOfferNoPhotos() = runComposeUiTest {
        val f = StandaloneFixture()

        showDisplaySettings(f.engine, f.registry, photos = photoLibraryWith(2))

        assertFalse(exists(StandaloneTags.backdropPhoto("p0")))
    }

    @Test
    fun anImageBackdropWithNoPhotosSaysSo() = runComposeUiTest {
        val f = StandaloneFixture()
        showDisplaySettings(f.engine, f.registry, photos = photoLibraryWith(0))

        click(StandaloneTags.backdrop(1))

        awaitThat { exists(StandaloneTags.BACKDROP_NO_PHOTOS) }
    }

    @Test
    fun pickingAPhotoBackdropSendsItsAddress() = runComposeUiTest {
        // A backdrop travels as a URL, so the one that reaches the screen is the
        // thing worth asserting.
        val f = StandaloneFixture()
        showDisplaySettings(f.engine, f.registry, photos = photoLibraryWith(2))
        click(StandaloneTags.backdrop(1))
        awaitThat { exists(StandaloneTags.backdropPhoto("p1")) }

        click(StandaloneTags.backdropPhoto("p1"))

        awaitThat { f.engine.backdropUrl.value?.contains("p1") == true }
    }

    @Test
    fun aPhotoBackdropNeedsTheServer() = runComposeUiTest {
        // Until the phone's own server is up there is no address to send.
        val f = StandaloneFixture()
        showDisplaySettings(f.engine, f.registry, photos = photoLibraryWith(2, baseUrl = null))

        click(StandaloneTags.backdrop(1))

        awaitThat { exists(StandaloneTags.BACKDROP_NEEDS_SERVER) }
    }

    @Test
    fun theLookIsReachableFromHere() = runComposeUiTest {
        val f = StandaloneFixture()

        showDisplaySettings(f.engine, f.registry)

        assertTrue(exists(StandaloneTags.LOOK))
    }
}
