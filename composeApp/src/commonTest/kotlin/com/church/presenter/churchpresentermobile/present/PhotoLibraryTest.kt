package com.church.presenter.churchpresentermobile.present

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Photos picked on the device, served by the device.
 *
 * Standalone's Photos screen used to browse the desktop's picture folders,
 * which a phone with no desktop could never list.
 */
class PhotoLibraryTest {

    private fun library(): PhotoLibrary {
        var next = 0
        return PhotoLibrary(newId = { "photo-${next++}" })
    }

    private fun jpeg(vararg bytes: Int) = bytes.map { it.toByte() }.toByteArray()

    @Test
    fun aPickedPhotoIsListedAndItsBytesAreServable() {
        val library = library()

        val photo = library.add("sunrise.jpg", jpeg(1, 2, 3))

        assertEquals(listOf(photo), library.photos.value)
        assertEquals(jpeg(1, 2, 3).toList(), library.bytes(photo.id)?.toList())
    }

    @Test
    fun thereIsNoUrlUntilTheServerIsUp() {
        // Both displays fetch a photo over HTTP from the phone itself. Handing
        // out an address nothing answers would project a black screen.
        val library = library()
        val photo = library.add("sunrise.jpg", jpeg(1))

        assertNull(library.urlFor(photo.id))

        library.serveFrom("http://192.168.1.50:8080")

        assertEquals("http://192.168.1.50:8080/photo/${photo.id}", library.urlFor(photo.id))
    }

    @Test
    fun aTrailingSlashOnTheBaseUrlDoesNotDoubleUp() {
        val library = library()
        val photo = library.add("a.jpg", jpeg(1))

        library.serveFrom("http://192.168.1.50:8080/")

        assertEquals("http://192.168.1.50:8080/photo/${photo.id}", library.urlFor(photo.id))
    }

    @Test
    fun theUrlGoesAwayAgainWhenTheServerStops() {
        val library = library()
        val photo = library.add("a.jpg", jpeg(1))
        library.serveFrom("http://192.168.1.50:8080")

        library.serveFrom(null)

        assertNull(library.urlFor(photo.id))
    }

    @Test
    fun anUnknownIdHasNoUrlAndNoBytes() {
        val library = library()
        library.serveFrom("http://192.168.1.50:8080")

        assertNull(library.urlFor("never-picked"))
        assertNull(library.bytes("never-picked"))
    }

    @Test
    fun removingForgetsTheBytesToo() {
        val library = library()
        val photo = library.add("a.jpg", jpeg(1))

        library.remove(photo.id)

        assertTrue(library.photos.value.isEmpty())
        assertNull(library.bytes(photo.id))
    }

    @Test
    fun clearingEmptiesTheWholeSet() {
        val library = library()
        library.add("a.jpg", jpeg(1))
        library.add("b.jpg", jpeg(2))

        library.clear()

        assertTrue(library.photos.value.isEmpty())
    }

    @Test
    fun theServerIsToldWhatKindOfImageItIsServing() {
        val library = library()
        val png = library.add("logo.PNG", jpeg(1))
        val jpg = library.add("photo.jpeg", jpeg(2))
        val heic = library.add("iphone.heic", jpeg(3))
        val odd = library.add("no-extension", jpeg(4))

        assertEquals("image/png", library.source.photo(png.id)?.contentType)
        assertEquals("image/jpeg", library.source.photo(jpg.id)?.contentType)
        assertEquals("image/heic", library.source.photo(heic.id)?.contentType)
        assertEquals("image/jpeg", library.source.photo(odd.id)?.contentType, "an unknown extension is served as JPEG")
    }

    @Test
    fun theServerGetsNothingForAPhotoThatWasRemoved() {
        val library = library()
        val photo = library.add("a.jpg", jpeg(1))

        library.remove(photo.id)

        assertNull(library.source.photo(photo.id))
    }

    @Test
    fun servingNothingIsWhatRemoteModeUses() {
        assertNull(PhotoSource.NONE.photo("anything"))
    }

    // ── What gets kept ───────────────────────────────────────────────────────

    @Test
    fun aPickedPhotoIsShrunkBeforeItIsKept() {
        // A camera original is held in memory for the whole session and sent to
        // every screen watching, so it is paid for twice over. The real shrink is
        // platform code; this pins that the library actually asks for it.
        var asked = 0
        val library = PhotoLibrary(newId = { "id" }, downscale = { asked++; ByteArray(10) })

        val photo = library.add("holiday.jpg", ByteArray(5_000_000))

        assertEquals(1, asked)
        assertEquals(10, library.bytes(photo.id)?.size)
    }

    @Test
    fun theShrunkBytesAreWhatIsServed() {
        val library = PhotoLibrary(newId = { "id" }, downscale = { byteArrayOf(7, 7, 7) })
        val photo = library.add("holiday.jpg", ByteArray(900_000))

        assertContentEquals(byteArrayOf(7, 7, 7), library.source.photo(photo.id)?.bytes)
    }
}
