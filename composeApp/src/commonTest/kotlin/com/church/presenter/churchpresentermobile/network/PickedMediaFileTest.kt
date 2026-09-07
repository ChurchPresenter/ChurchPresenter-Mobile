package com.church.presenter.churchpresentermobile.network

import io.ktor.utils.io.ByteWriteChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests [PickedMediaFile] — the streaming handle the media upload writes from.
 *
 * It streams rather than holding bytes so a large video never sits in memory
 * (Android's heap cannot fit a 100 MB array), which makes the progress callback
 * the only way a caller can tell how far along it is.
 */
class PickedMediaFileTest {

    @Test
    fun itCarriesTheNameAndSizeTheUploadNeeds() {
        val picked = PickedMediaFile("sermon.mp4", 1_234L) { _, _ -> }

        assertEquals("sermon.mp4", picked.fileName)
        assertEquals(1_234L, picked.sizeBytes)
    }

    @Test
    fun anUnknownSizeIsZeroRatherThanNegative() {
        // 0 means "no Content-Length"; the uploader checks for it before dividing.
        assertEquals(0L, PickedMediaFile("clip.mov", 0L) { _, _ -> }.sizeBytes)
    }

    @Test
    fun streamingReportsCumulativeProgress() = runTest {
        val reported = mutableListOf<Long>()
        val picked = PickedMediaFile("clip.mp4", 300L) { _, onProgress ->
            onProgress(100L)
            onProgress(200L)
            onProgress(300L)
        }

        picked.streamTo(NoOpChannel) { reported += it }

        assertEquals(listOf(100L, 200L, 300L), reported)
        assertTrue(reported.zipWithNext().all { (a, b) -> b > a }, "progress must not go backwards")
    }

    @Test
    fun aFileThatWritesNothingReportsNoProgress() {
        val reported = mutableListOf<Long>()
        val picked = PickedMediaFile("empty.mp4", 0L) { _, _ -> }

        kotlinx.coroutines.test.runTest {
            picked.streamTo(NoOpChannel) { reported += it }
        }

        assertTrue(reported.isEmpty())
    }
}

/** Accepts nothing: these tests care about the progress callback, not the bytes. */
private val NoOpChannel: ByteWriteChannel get() = io.ktor.utils.io.ByteChannel()
