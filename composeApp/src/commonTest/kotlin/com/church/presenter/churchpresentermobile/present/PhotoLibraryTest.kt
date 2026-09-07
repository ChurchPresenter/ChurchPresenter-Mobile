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

    // ── Serving a photo ──────────────────────────────────────────────────
    //
    // The library hands the embedded server a [PhotoSource] rather than being
    // reached into, so the server keeps knowing nothing about picking or modes.
    // What it must get right is the content type: a browser told the wrong one
    // renders a broken image where a slide should be.

    @Test
    fun aServedPhotoCarriesItsBytesAndType() {
        val library = library()
        val photo = library.add("sunrise.jpg", jpeg(1, 2, 3))

        val served = library.source.photo(photo.id)

        assertContentEquals(jpeg(1, 2, 3), served?.bytes)
        assertEquals("image/jpeg", served?.contentType)
    }

    @Test
    fun anUnknownIdServesNothingRatherThanEmptyBytes() {
        // A slide pointing at a dead URL renders as a blank screen mid-service;
        // answering null lets the server 404 instead.
        val library = library()
        library.add("sunrise.jpg", jpeg(1))

        assertNull(library.source.photo("no-such-photo"))
    }

    @Test
    fun aRemovedPhotoStopsBeingServed() {
        val library = library()
        val photo = library.add("sunrise.jpg", jpeg(1))

        library.remove(photo.id)

        assertNull(library.source.photo(photo.id))
    }

    @Test
    fun theEmptySourceServesNothingAtAll() {
        // What remote mode and the web target are handed.
        assertNull(PhotoSource.NONE.photo("anything"))
    }

    // ── Content types ────────────────────────────────────────────────────

    @Test
    fun eachPickedFormatGetsItsOwnContentType() {
        val library = library()

        assertEquals("image/png", library.contentTypeFor("a.png"))
        assertEquals("image/gif", library.contentTypeFor("a.gif"))
        assertEquals("image/webp", library.contentTypeFor("a.webp"))
        assertEquals("image/heic", library.contentTypeFor("a.heic"))
        assertEquals("image/heic", library.contentTypeFor("a.heif"))
        assertEquals("image/jpeg", library.contentTypeFor("a.jpg"))
    }

    @Test
    fun anUnknownOrMissingExtensionIsServedAsJpeg() {
        // A camera roll is overwhelmingly JPEG, and a browser will sniff anyway.
        val library = library()

        assertEquals("image/jpeg", library.contentTypeFor("a.raw"))
        assertEquals("image/jpeg", library.contentTypeFor("photo"))
        assertEquals("image/jpeg", library.contentTypeFor(""))
    }

    @Test
    fun theExtensionIsReadCaseInsensitively() {
        // iOS hands back names in upper case.
        val library = library()

        assertEquals("image/png", library.contentTypeFor("A.PNG"))
        assertEquals("image/heic", library.contentTypeFor("IMG_0001.HEIC"))
    }

    @Test
    fun aServedPhotoWithNoRecognisedExtensionStillHasAType() {
        val library = library()
        val photo = library.add("scan", jpeg(1))

        assertEquals("image/jpeg", library.source.photo(photo.id)?.contentType)
    }

    @Test
    fun aPngIsServedAsAPng() {
        val library = library()
        val photo = library.add("logo.png", jpeg(1))

        assertEquals("image/png", library.source.photo(photo.id)?.contentType)
    }

    @Test
    fun clearingStopsEverythingBeingServed() {
        val library = library()
        val a = library.add("a.jpg", jpeg(1))
        val b = library.add("b.png", jpeg(2))

        library.clear()

        assertNull(library.source.photo(a.id))
        assertNull(library.source.photo(b.id))
    }
}
