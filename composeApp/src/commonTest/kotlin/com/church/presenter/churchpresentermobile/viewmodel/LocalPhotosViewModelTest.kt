package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.SlideBackdrop
import com.church.presenter.churchpresentermobile.model.SlideKind
import com.church.presenter.churchpresentermobile.present.PhotoLibrary
import com.church.presenter.churchpresentermobile.present.SinkRegistry
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.testutil.runVmTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Picking photos on the device and projecting them from it. */
class LocalPhotosViewModelTest {

    private fun engine() = StandaloneEngine(
        mode = MutableStateFlow(AppMode.STANDALONE),
        registry = SinkRegistry(),
        publish = {},
    )

    private fun library(): PhotoLibrary {
        var next = 0
        return PhotoLibrary(newId = { "photo-${next++}" })
    }

    private fun bytes(vararg values: Int) = values.map { it.toByte() }.toByteArray()

    @Test
    fun photosCannotBeProjectedUntilTheServerIsUp() = runVmTest {
        val library = library()
        val vm = LocalPhotosViewModel(library, engine())
        advanceUntilIdle()

        assertFalse(vm.canProject.value)

        library.serveFrom("http://192.168.1.50:8080")
        advanceUntilIdle()

        assertTrue(vm.canProject.value)
    }

    @Test
    fun projectingLoadsEveryPickedPhotoAndOpensTheOneTapped() = runVmTest {
        // The whole set becomes the deck so the operator steps through with the
        // same next/previous they use for a song, rather than coming back here.
        val library = library()
        library.serveFrom("http://192.168.1.50:8080")
        val engine = engine()
        val vm = LocalPhotosViewModel(library, engine)
        vm.add("first.jpg", bytes(1))
        val second = vm.add("second.jpg", bytes(2))
        vm.add("third.jpg", bytes(3))
        advanceUntilIdle()

        vm.project(second)
        advanceUntilIdle()

        assertEquals(3, engine.deck.value.slides.size)
        assertEquals(1, engine.index.value)
        assertEquals(second.id, vm.projectingId.value)
        val slide = engine.currentSlide.value
        assertEquals(SlideKind.IMAGE, slide.kind)
        assertEquals(SlideBackdrop.IMAGE, slide.backdrop)
        assertEquals("http://192.168.1.50:8080/photo/${second.id}", slide.backdropUrl)
    }

    @Test
    fun projectingWithNoServerDoesNothingRatherThanShowingABlackScreen() = runVmTest {
        val library = library()
        val engine = engine()
        val vm = LocalPhotosViewModel(library, engine)
        val photo = vm.add("a.jpg", bytes(1))
        advanceUntilIdle()

        vm.project(photo)
        advanceUntilIdle()

        assertTrue(engine.deck.value.slides.isEmpty())
        assertNull(vm.projectingId.value)
    }

    @Test
    fun removingThePhotoOnScreenClearsTheScreen() = runVmTest {
        val library = library()
        library.serveFrom("http://192.168.1.50:8080")
        val engine = engine()
        val vm = LocalPhotosViewModel(library, engine)
        val photo = vm.add("a.jpg", bytes(1))
        advanceUntilIdle()
        vm.project(photo)
        advanceUntilIdle()

        vm.remove(photo.id)
        advanceUntilIdle()

        assertNull(vm.projectingId.value)
        assertTrue(engine.deck.value.slides.isEmpty())
        assertTrue(vm.photos.value.isEmpty())
    }

    @Test
    fun removingAPhotoThatIsNotOnScreenLeavesTheScreenAlone() = runVmTest {
        val library = library()
        library.serveFrom("http://192.168.1.50:8080")
        val engine = engine()
        val vm = LocalPhotosViewModel(library, engine)
        val shown = vm.add("a.jpg", bytes(1))
        val other = vm.add("b.jpg", bytes(2))
        advanceUntilIdle()
        vm.project(shown)
        advanceUntilIdle()

        vm.remove(other.id)
        advanceUntilIdle()

        assertEquals(shown.id, vm.projectingId.value)
        assertEquals(2, engine.deck.value.slides.size, "the deck is left as projected until the next project")
    }
}
