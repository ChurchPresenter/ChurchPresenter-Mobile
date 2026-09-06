package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.LocalAnnouncement
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.present.SinkRegistry
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.testutil.FakeOutputSink
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import com.church.presenter.churchpresentermobile.testutil.tearDown
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests the notices presenting surface — the screen that puts a notice live,
 * as distinct from the Library tab that keeps them.
 */
class LocalNoticesViewModelTest {

    private class Fixture(
        notices: List<LocalAnnouncement> = emptyList(),
        withPresenter: Boolean = true,
    ) {
        val repository = LibraryRepository(InMemoryFileStorage()) { 1_000L }
        val registry = SinkRegistry()
        val sink = FakeOutputSink()
        val engine: StandaloneEngine
        val viewModel: LocalNoticesViewModel

        init {
            registry.register(sink)
            engine = StandaloneEngine(MutableStateFlow(AppMode.STANDALONE), registry) { }
            notices.forEach(repository::upsertAnnouncement)
            viewModel = LocalNoticesViewModel(repository, engine.takeIf { withPresenter })
        }
    }

    private val welcome = LocalAnnouncement("a1", title = "Welcome", body = "Service starts at 10")
    private val giving = LocalAnnouncement("a2", body = "Giving details at the back")

    // ── The list ─────────────────────────────────────────────────────────

    @Test
    fun `the notices already in the library are listed straight away`() = runVmTest {
        // Eagerly shared from the repository's current value, so the first frame
        // is not an empty list that fills in a moment later.
        val f = Fixture(notices = listOf(welcome, giving))
        try {

            assertEquals(listOf("a1", "a2"), f.viewModel.notices.value.map { it.id })
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `a notice written in the Library tab appears here without a reload`() = runVmTest {
        val f = Fixture(notices = listOf(welcome))
        try {

            f.repository.upsertAnnouncement(giving)
            // The list is stateIn(viewModelScope, Eagerly); let its collector run.
            advanceUntilIdle()

            assertEquals(listOf("a1", "a2"), f.viewModel.notices.value.map { it.id })
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `nothing is live before anything is pressed`() = runVmTest {
        val f = Fixture(notices = listOf(welcome))
        try {

            assertNull(f.viewModel.liveId.value)
        } finally {
            tearDown(f.viewModel)
        }
    }

    // ── Projecting ───────────────────────────────────────────────────────

    @Test
    fun `projecting a notice puts it on the screen and marks it live`() = runVmTest {
        val f = Fixture(notices = listOf(welcome))
        try {

            f.viewModel.project(welcome)

            assertEquals("a1", f.viewModel.liveId.value)
            assertEquals(SlideKind.ANNOUNCEMENT, f.engine.deck.value.kind)
            assertTrue(f.sink.rendered.isNotEmpty(), "the notice should have reached the sink")
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `the projected slide carries the notice's own words`() = runVmTest {
        val f = Fixture(notices = listOf(welcome))
        try {

            f.viewModel.project(welcome)

            assertTrue(
                f.engine.deck.value.slides.any { it.body.contains("Service starts at 10") },
                "expected the body text on a slide, got ${f.engine.deck.value.slides.map { it.body }}",
            )
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `a notice with no title still projects`() = runVmTest {
        // The title is optional; a blank one must not become an empty heading slide.
        val f = Fixture(notices = listOf(giving))
        try {

            f.viewModel.project(giving)

            assertEquals("a2", f.viewModel.liveId.value)
            assertTrue(f.engine.deck.value.slides.isNotEmpty())
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `projecting a second notice replaces the first`() = runVmTest {
        val f = Fixture(notices = listOf(welcome, giving))
        try {
            f.viewModel.project(welcome)

            f.viewModel.project(giving)

            assertEquals("a2", f.viewModel.liveId.value)
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `a notice can be projected again after the screen was cleared`() = runVmTest {
        // The deck is supplied on every press rather than loaded once, so what
        // cleared the screen beforehand does not matter.
        val f = Fixture(notices = listOf(welcome))
        try {
            f.viewModel.project(welcome)
            f.viewModel.clear()

            f.viewModel.project(welcome)

            assertEquals("a1", f.viewModel.liveId.value)
            assertTrue(f.engine.deck.value.slides.isNotEmpty())
        } finally {
            tearDown(f.viewModel)
        }
    }

    // ── Clearing ─────────────────────────────────────────────────────────

    @Test
    fun `clearing takes the notice off the screen`() = runVmTest {
        val f = Fixture(notices = listOf(welcome))
        try {
            f.viewModel.project(welcome)

            f.viewModel.clear()

            assertNull(f.viewModel.liveId.value)
            assertTrue(f.engine.deck.value.isEmpty)
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `clearing when nothing is live is harmless`() = runVmTest {
        val f = Fixture(notices = listOf(welcome))
        try {

            f.viewModel.clear()

            assertNull(f.viewModel.liveId.value)
        } finally {
            tearDown(f.viewModel)
        }
    }

    // ── Remote mode ──────────────────────────────────────────────────────

    @Test
    fun `with no presenter, projecting does nothing at all`() = runVmTest {
        // Remote mode has no local presenter — the press must not mark a notice
        // live when nothing can possibly be showing it.
        val f = Fixture(notices = listOf(welcome), withPresenter = false)
        try {

            f.viewModel.project(welcome)

            assertNull(f.viewModel.liveId.value)
            assertTrue(f.sink.rendered.isEmpty())
        } finally {
            tearDown(f.viewModel)
        }
    }

    @Test
    fun `with no presenter, clearing is still safe`() = runVmTest {
        val f = Fixture(notices = listOf(welcome), withPresenter = false)
        try {

            f.viewModel.clear()

            assertNull(f.viewModel.liveId.value)
        } finally {
            tearDown(f.viewModel)
        }
    }
}
