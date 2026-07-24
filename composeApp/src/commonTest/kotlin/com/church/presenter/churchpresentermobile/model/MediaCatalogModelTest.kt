package com.church.presenter.churchpresentermobile.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests display/count fallbacks on [PicturesFolder] and [Presentation] DTOs. */
class MediaCatalogModelTest {

    // ── PicturesFolder ───────────────────────────────────────────────────────

    @Test
    fun folderDisplayNameFallsBackToPictures() {
        assertEquals("Nature", PicturesFolder(folderName = "Nature").displayName)
        assertEquals("Pictures", PicturesFolder(folderName = "  ").displayName)
        assertEquals("Pictures", PicturesFolder().displayName)
    }

    @Test
    fun folderTotalImagesPrefersTotalThenListSize() {
        assertEquals(12, PicturesFolder(imageTotal = 12).totalImages)
        assertEquals(2, PicturesFolder(images = listOf(PictureImage(0), PictureImage(1))).totalImages)
        assertEquals(0, PicturesFolder().totalImages)
    }

    @Test
    fun folderAllImagesDefaultsToEmpty() {
        val imgs = listOf(PictureImage(0))
        assertEquals(imgs, PicturesFolder(images = imgs).allImages)
        assertTrue(PicturesFolder().allImages.isEmpty())
    }

    // ── Presentation ─────────────────────────────────────────────────────────

    @Test
    fun presentationDisplayNameFallsBackToIdThenUnknown() {
        assertEquals("Sermon.pptx", Presentation(fileName = "Sermon.pptx", id = "p1").displayName)
        assertEquals("p1", Presentation(fileName = "  ", id = "p1").displayName)
        assertEquals("Unknown", Presentation().displayName)
    }

    @Test
    fun presentationDisplayIdIsIdOrEmpty() {
        assertEquals("p1", Presentation(id = "p1").displayId)
        assertEquals("", Presentation().displayId)
    }

    @Test
    fun presentationTotalSlidesPrefersTotalThenListSize() {
        assertEquals(20, Presentation(slideTotal = 20).totalSlides)
        assertEquals(3, Presentation(slides = List(3) { PresentationSlide(it) }).totalSlides)
        assertEquals(0, Presentation().totalSlides)
    }

    @Test
    fun presentationsResponseAllPresentationsDefaultsToEmpty() {
        val list = listOf(Presentation(id = "p1"))
        assertEquals(list, PresentationsResponse(presentations = list).allPresentations)
        assertTrue(PresentationsResponse().allPresentations.isEmpty())
    }
}
