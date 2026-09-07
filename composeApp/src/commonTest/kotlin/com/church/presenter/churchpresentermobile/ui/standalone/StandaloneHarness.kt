package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.model.SlideEnvelope
import com.church.presenter.churchpresentermobile.model.SlideMessageType
import com.church.presenter.churchpresentermobile.present.OutputSink
import com.church.presenter.churchpresentermobile.present.PhotoLibrary
import com.church.presenter.churchpresentermobile.present.SinkState
import com.church.presenter.churchpresentermobile.present.SinkStatus
import com.church.presenter.churchpresentermobile.present.SinkRegistry
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.testutil.FakeOutputSink
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.ui.showScreen
import com.church.presenter.churchpresentermobile.viewmodel.LocalPhotosViewModel
import com.church.presenter.churchpresentermobile.viewmodel.StandaloneViewModel
import com.church.presenter.churchpresentermobile.viewmodel.LocalWebViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared setup for the standalone screens' UI tests.
 *
 * These screens drive this phone's own outputs rather than a desktop, so the
 * thing worth asserting is what reached the screen: a real [StandaloneEngine]
 * with a [FakeOutputSink] behind it records the envelopes, which is a great
 * deal more honest than asserting that a button changed colour.
 */
internal class StandaloneFixture {
    val registry = SinkRegistry()
    val sink = FakeOutputSink(id = "screen", displayName = "The hall screen")
    val engine = StandaloneEngine(MutableStateFlow(AppMode.STANDALONE), registry)

    init {
        // Registration is enough: the registry broadcasts to every sink it holds,
        // and attaching is what a real display does with the frames it receives.
        registry.register(sink)
    }

    /** The words on the audience screen right now, if any. */
    val liveText: String?
        get() = sink.rendered.lastOrNull()?.slide?.body

    /** The page or video the screen is showing, if any. */
    val liveMedia: String?
        get() = sink.rendered.lastOrNull()?.slide?.mediaUrl

    /** What the last envelope asked the screen to do. */
    val lastMessage: SlideMessageType?
        get() = sink.rendered.lastOrNull()?.type
}

/**
 * A sink whose state can be set outright.
 *
 * [FakeOutputSink.attach] is a suspend function, which a composition test has no
 * scope to call; the chip and the outputs list only ever read [status], so this
 * publishes one directly.
 */
internal class TestSink(
    override val id: String,
    displayName: String,
    state: SinkState = SinkState.DETACHED,
    detail: String? = null,
) : OutputSink {
    private val _status = MutableStateFlow(
        SinkStatus(id = id, displayName = displayName, state = state, detail = detail)
    )
    override val status: StateFlow<SinkStatus> = _status.asStateFlow()

    val rendered = mutableListOf<SlideEnvelope>()

    fun publish(status: SinkStatus) { _status.value = status }

    override suspend fun attach() = Unit

    override suspend fun detach() = Unit

    override fun render(envelope: SlideEnvelope) { rendered += envelope }
}

internal fun libraryWith(vararg notices: LocalAnnouncement) =
    LibraryRepository(InMemoryFileStorage()) { 1_000L }.apply {
        notices.forEach { upsertAnnouncement(it) }
    }

internal fun notice(id: String, title: String = "Welcome", body: String = "Coffee in the hall") =
    LocalAnnouncement(id = id, title = title, body = body)

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showController(
    fixture: StandaloneFixture,
    photos: PhotoLibrary? = null,
    settings: AppSettings = AppSettings(InMemorySettingsStorage()),
) = showScreen {
    StandaloneControllerScreen(
        engine = fixture.engine,
        registry = fixture.registry,
        settings = settings,
        photos = photos,
        providedViewModel = StandaloneViewModel(fixture.engine, fixture.registry, settings, photos),
    )
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showLocalNotices(
    repository: LibraryRepository,
    engine: StandaloneEngine?,
    hasOutput: Boolean = true,
) = showScreen {
    LocalNoticesScreen(
        repository = repository,
        presenter = engine,
        hasOutput = hasOutput,
    )
}

/**
 * A photo library with [count] photos already picked, served from [baseUrl].
 *
 * A null [baseUrl] is the phone's own server not being up yet, which is the
 * state where nothing can be projected.
 */
internal fun photoLibraryWith(count: Int, baseUrl: String? = "http://192.168.1.50:8080"): PhotoLibrary {
    var next = 0
    val library = PhotoLibrary(newId = { "p${next++}" }, downscale = { it })
    library.serveFrom(baseUrl)
    repeat(count) { i -> library.add("photo$i.jpg", byteArrayOf(1, 2, 3)) }
    return library
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showLocalPhotos(
    library: PhotoLibrary,
    engine: StandaloneEngine?,
) = showScreen {
    LocalPhotosScreen(
        library = library,
        presenter = engine,
        providedViewModel = LocalPhotosViewModel(library, engine),
    )
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showLocalWeb(
    engine: StandaloneEngine?,
    hasOutput: Boolean = true,
    refusesFraming: suspend (String) -> Boolean = { false },
) = showScreen {
    LocalWebScreen(
        presenter = engine,
        hasOutput = hasOutput,
        providedViewModel = LocalWebViewModel(presenter = engine, refusesFraming = refusesFraming),
    )
}
