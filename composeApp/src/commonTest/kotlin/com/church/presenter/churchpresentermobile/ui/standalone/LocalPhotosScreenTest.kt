package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.ui.awaitThat
import com.church.presenter.churchpresentermobile.ui.click
import com.church.presenter.churchpresentermobile.ui.exists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Photos from this phone, on this phone's own screens.
 *
 * The pictures are served by the phone's presentation server and fetched back
 * by every display watching, so until that server is up a photo slide points at
 * an address that answers nothing — a black screen mid-service. That is why
 * "can this be projected yet" is a state the screen shows rather than something
 * a tap discovers, and it is the main thing these hold.
 */
@OptIn(ExperimentalTestApi::class)
class LocalPhotosScreenTest {

    // ── Nothing picked yet ───────────────────────────────────────────────

    @Test
    fun noPhotosSaysSo() = runComposeUiTest {
        val f = StandaloneFixture()

        showLocalPhotos(photoLibraryWith(0), f.engine)

        assertTrue(exists(StandaloneTags.PHOTOS_EMPTY))
    }

    @Test
    fun noPhotosStillOffersThePicker() = runComposeUiTest {
        // It is the only way out of an empty screen.
        val f = StandaloneFixture()

        showLocalPhotos(photoLibraryWith(0), f.engine)

        assertTrue(exists(StandaloneTags.PHOTOS_PICK))
    }

    @Test
    fun noPhotosOffersNoClear() = runComposeUiTest {
        val f = StandaloneFixture()

        showLocalPhotos(photoLibraryWith(0), f.engine)

        assertFalse(exists(StandaloneTags.PHOTOS_CLEAR))
    }

    // ── Picked photos ────────────────────────────────────────────────────

    @Test
    fun aPickedPhotoIsListed() = runComposeUiTest {
        val f = StandaloneFixture()

        showLocalPhotos(photoLibraryWith(1), f.engine)

        assertTrue(exists(StandaloneTags.photo("p0")))
    }

    @Test
    fun everyPickedPhotoIsListed() = runComposeUiTest {
        val f = StandaloneFixture()

        showLocalPhotos(photoLibraryWith(3), f.engine)

        assertTrue(exists(StandaloneTags.photo("p0")))
        assertTrue(exists(StandaloneTags.photo("p2")))
    }

    @Test
    fun havingPhotosTakesTheEmptyMessageAway() = runComposeUiTest {
        val f = StandaloneFixture()

        showLocalPhotos(photoLibraryWith(1), f.engine)

        assertFalse(exists(StandaloneTags.PHOTOS_EMPTY))
    }

    @Test
    fun aPhotoAddedWhileTheScreenIsOpenAppears() = runComposeUiTest {
        val f = StandaloneFixture()
        val library = photoLibraryWith(1)
        showLocalPhotos(library, f.engine)

        library.add("later.jpg", byteArrayOf(9))

        awaitThat { exists(StandaloneTags.photo("p1")) }
    }

    @Test
    fun everyPhotoOffersRemoval() = runComposeUiTest {
        val f = StandaloneFixture()

        showLocalPhotos(photoLibraryWith(2), f.engine)

        assertTrue(exists(StandaloneTags.photoRemove("p0")))
        assertTrue(exists(StandaloneTags.photoRemove("p1")))
    }

    @Test
    fun removingAPhotoTakesItOutOfTheList() = runComposeUiTest {
        val f = StandaloneFixture()
        val library = photoLibraryWith(2)
        showLocalPhotos(library, f.engine)

        click(StandaloneTags.photoRemove("p0"))

        awaitThat { !exists(StandaloneTags.photo("p0")) }
    }

    @Test
    fun removingAPhotoLeavesTheOthers() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalPhotos(photoLibraryWith(2), f.engine)

        click(StandaloneTags.photoRemove("p0"))

        awaitThat { exists(StandaloneTags.photo("p1")) }
    }

    @Test
    fun removingTheLastPhotoLeavesTheEmptyMessage() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalPhotos(photoLibraryWith(1), f.engine)

        click(StandaloneTags.photoRemove("p0"))

        awaitThat { exists(StandaloneTags.PHOTOS_EMPTY) }
    }

    @Test
    fun removingAPhotoForgetsItsBytes() = runComposeUiTest {
        val f = StandaloneFixture()
        val library = photoLibraryWith(2)
        showLocalPhotos(library, f.engine)

        click(StandaloneTags.photoRemove("p0"))

        awaitThat { library.photos.value.none { it.id == "p0" } }
    }

    // ── Projecting ───────────────────────────────────────────────────────

    @Test
    fun tappingAPhotoPutsItOnTheScreen() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalPhotos(photoLibraryWith(2), f.engine)

        click(StandaloneTags.photo("p0"))

        awaitThat { !f.engine.deck.value.isEmpty }
    }

    @Test
    fun tappingAPhotoMarksItAsTheLiveOne() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalPhotos(photoLibraryWith(2), f.engine)

        click(StandaloneTags.photo("p0"))

        awaitThat { exists(StandaloneTags.photoLive("p0")) }
    }

    @Test
    fun onlyTheLivePhotoIsMarked() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalPhotos(photoLibraryWith(2), f.engine)

        click(StandaloneTags.photo("p1"))

        awaitThat { exists(StandaloneTags.photoLive("p1")) }
        assertFalse(exists(StandaloneTags.photoLive("p0")))
    }

    @Test
    fun tappingTheRightPhotoOfSeveral() = runComposeUiTest {
        // The whole set becomes the deck, so which one is *shown* is the index —
        // and an off-by-one here puts the wrong picture on the wall.
        val f = StandaloneFixture()
        showLocalPhotos(photoLibraryWith(3), f.engine)

        click(StandaloneTags.photo("p2"))

        awaitThat { f.engine.index.value == 2 }
    }

    @Test
    fun thewholePickedSetBecomesTheDeck() = runComposeUiTest {
        // So the operator can step through with the same next/previous they use
        // for a song rather than coming back here between images.
        val f = StandaloneFixture()
        showLocalPhotos(photoLibraryWith(3), f.engine)

        click(StandaloneTags.photo("p0"))

        awaitThat { f.engine.deck.value.slides.size == 3 }
    }

    @Test
    fun projectingOffersAWayToClear() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalPhotos(photoLibraryWith(1), f.engine)

        click(StandaloneTags.photo("p0"))

        awaitThat { exists(StandaloneTags.PHOTOS_CLEAR) }
    }

    @Test
    fun clearingTakesThePhotoOffTheScreen() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalPhotos(photoLibraryWith(1), f.engine)
        click(StandaloneTags.photo("p0"))
        awaitThat { exists(StandaloneTags.PHOTOS_CLEAR) }

        click(StandaloneTags.PHOTOS_CLEAR)

        awaitThat { f.engine.deck.value.isEmpty }
    }

    @Test
    fun clearingUnmarksTheLivePhoto() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalPhotos(photoLibraryWith(1), f.engine)
        click(StandaloneTags.photo("p0"))
        awaitThat { exists(StandaloneTags.photoLive("p0")) }

        click(StandaloneTags.PHOTOS_CLEAR)

        awaitThat { !exists(StandaloneTags.photoLive("p0")) }
    }

    @Test
    fun clearingTakesTheClearButtonAway() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalPhotos(photoLibraryWith(1), f.engine)
        click(StandaloneTags.photo("p0"))
        awaitThat { exists(StandaloneTags.PHOTOS_CLEAR) }

        click(StandaloneTags.PHOTOS_CLEAR)

        awaitThat { !exists(StandaloneTags.PHOTOS_CLEAR) }
    }

    @Test
    fun projectingASecondPhotoMovesTheMark() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalPhotos(photoLibraryWith(2), f.engine)
        click(StandaloneTags.photo("p0"))
        awaitThat { exists(StandaloneTags.photoLive("p0")) }

        click(StandaloneTags.photo("p1"))

        awaitThat { exists(StandaloneTags.photoLive("p1")) }
    }

    @Test
    fun removingTheLivePhotoClearsTheScreen() = runComposeUiTest {
        // Leaving a slide pointing at bytes that have been forgotten would show
        // a broken image on the wall.
        val f = StandaloneFixture()
        showLocalPhotos(photoLibraryWith(2), f.engine)
        click(StandaloneTags.photo("p0"))
        awaitThat { !f.engine.deck.value.isEmpty }

        click(StandaloneTags.photoRemove("p0"))

        awaitThat { f.engine.deck.value.isEmpty }
    }

    @Test
    fun removingAPhotoThatIsNotLiveLeavesTheScreenAlone() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalPhotos(photoLibraryWith(2), f.engine)
        click(StandaloneTags.photo("p0"))
        awaitThat { !f.engine.deck.value.isEmpty }

        click(StandaloneTags.photoRemove("p1"))

        awaitThat { !exists(StandaloneTags.photo("p1")) }
        assertFalse(f.engine.deck.value.isEmpty)
    }

    @Test
    fun aProjectedPhotoCanBeShownAgainAfterClearing() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalPhotos(photoLibraryWith(1), f.engine)
        click(StandaloneTags.photo("p0"))
        awaitThat { exists(StandaloneTags.PHOTOS_CLEAR) }
        click(StandaloneTags.PHOTOS_CLEAR)
        awaitThat { f.engine.deck.value.isEmpty }

        click(StandaloneTags.photo("p0"))

        awaitThat { !f.engine.deck.value.isEmpty }
    }

    // ── Before the server is up ──────────────────────────────────────────

    @Test
    fun noServerYetIsSaidPlainly() = runComposeUiTest {
        // Rather than a tap that silently does nothing.
        val f = StandaloneFixture()

        showLocalPhotos(photoLibraryWith(1, baseUrl = null), f.engine)

        assertTrue(exists(StandaloneTags.PHOTOS_NO_SERVER))
    }

    @Test
    fun aRunningServerIsNotNaggedAbout() = runComposeUiTest {
        val f = StandaloneFixture()

        showLocalPhotos(photoLibraryWith(1), f.engine)

        assertFalse(exists(StandaloneTags.PHOTOS_NO_SERVER))
    }

    @Test
    fun withNoServerAPhotoCannotBeProjected() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalPhotos(photoLibraryWith(1, baseUrl = null), f.engine)

        click(StandaloneTags.photo("p0"))

        assertTrue(f.engine.deck.value.isEmpty)
    }

    @Test
    fun withNoServerThePhotosAreStillListed() = runComposeUiTest {
        // They are picked and kept; only projecting them has to wait.
        val f = StandaloneFixture()

        showLocalPhotos(photoLibraryWith(2, baseUrl = null), f.engine)

        assertTrue(exists(StandaloneTags.photo("p0")))
        assertTrue(exists(StandaloneTags.photo("p1")))
    }

    @Test
    fun withNoServerPhotosCanStillBeRemoved() = runComposeUiTest {
        val f = StandaloneFixture()
        showLocalPhotos(photoLibraryWith(1, baseUrl = null), f.engine)

        click(StandaloneTags.photoRemove("p0"))

        awaitThat { exists(StandaloneTags.PHOTOS_EMPTY) }
    }

    @Test
    fun withNoServerThePickerIsStillOffered() = runComposeUiTest {
        val f = StandaloneFixture()

        showLocalPhotos(photoLibraryWith(0, baseUrl = null), f.engine)

        assertTrue(exists(StandaloneTags.PHOTOS_PICK))
    }

    // ── With no presenter ────────────────────────────────────────────────

    @Test
    fun withoutAPresenterTheScreenStillOpens() = runComposeUiTest {
        showLocalPhotos(photoLibraryWith(1), engine = null)

        assertTrue(exists(StandaloneTags.photo("p0")))
    }

    @Test
    fun withoutAPresenterTappingAPhotoIsHarmless() = runComposeUiTest {
        showLocalPhotos(photoLibraryWith(1), engine = null)

        click(StandaloneTags.photo("p0"))

        assertFalse(exists(StandaloneTags.photoLive("p0")))
    }

    @Test
    fun thePhotosKeepThePickedOrder() = runComposeUiTest {
        // The deck is built from this list, so its order is the order the
        // operator steps through.
        val library = photoLibraryWith(3)

        assertEquals(listOf("p0", "p1", "p2"), library.photos.value.map { it.id })
    }
}
