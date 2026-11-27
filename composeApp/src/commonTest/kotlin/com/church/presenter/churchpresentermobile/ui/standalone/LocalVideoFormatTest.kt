package com.church.presenter.churchpresentermobile.ui.standalone

import com.church.presenter.churchpresentermobile.model.LocalVideo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocalVideoFormatTest {

    @Test
    fun `sizes read the way a phone's storage screen reads them`() {
        assertEquals("0\u00A0B", formatBytes(0))
        assertEquals("212\u00A0MB", formatBytes(212L * 1024 * 1024))
        assertEquals("1.2\u00A0GB", formatBytes(1_288_490_189L))
        assertEquals("12\u00A0GB", formatBytes(12L * 1024 * 1024 * 1024))
    }

    @Test
    fun `resolution is named by the shorter side`() {
        assertEquals("1080p", resolutionLabel(1920, 1080))
        assertEquals("1080p", resolutionLabel(1080, 1920), "an upright phone clip is still 1080p")
        assertEquals("4K", resolutionLabel(3840, 2160))
        assertNull(resolutionLabel(0, 0))
    }

    @Test
    fun `a row's details are format, resolution and size, and loops when it does`() {
        val video = LocalVideo(
            id = "a",
            title = "t",
            fileName = "a.mp4",
            sizeBytes = 38L * 1024 * 1024,
            width = 1920,
            height = 1080,
        )
        assertEquals("MP4 · 1080p · 38\u00A0MB", videoDetails(video, looping = false, loopsLabel = "loops"))
        assertEquals("MP4 · 1080p · 38\u00A0MB · loops", videoDetails(video, looping = true, loopsLabel = "loops"))
        assertEquals("MOV", videoDetails(LocalVideo(id = "b", title = "t", fileName = "b.mov"), false, "loops"))
    }
}
