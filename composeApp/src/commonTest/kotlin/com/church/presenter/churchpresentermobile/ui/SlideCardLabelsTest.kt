package com.church.presenter.churchpresentermobile.ui

import com.church.presenter.churchpresentermobile.model.SongVerse
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The label on each verse card — what the operator reads to find "the second
 * chorus" mid-song, so it has to number the way the desktop does.
 */
class SlideCardLabelsTest {

    private fun section(type: String?, part: Int = 0, parts: Int = 1) =
        SongVerse(type = type, part = part, parts = parts)

    @Test
    fun aSectionTypeThatAppearsOnceIsNotNumbered() {
        assertEquals(listOf("Verse", "Chorus"), slideCardLabels(listOf(section("verse"), section("chorus"))))
    }

    @Test
    fun aRepeatedSectionTypeIsNumbered() {
        val labels = slideCardLabels(listOf(section("Verse"), section("Chorus"), section("Verse")))

        assertEquals(listOf("Verse 1", "Chorus", "Verse 2"), labels)
    }

    @Test
    fun aSectionWithNoTypeFallsBackToItsPosition() {
        assertEquals(listOf("Verse", "2"), slideCardLabels(listOf(section("Verse"), section(null))))
    }

    @Test
    fun bothHalvesOfASplitSectionShareItsLabel() {
        val labels = slideCardLabels(
            listOf(
                section("Verse", part = 0, parts = 2),
                section("Verse", part = 1, parts = 2),
                section("Chorus"),
            ),
        )

        assertEquals(listOf("Verse (1/2)", "Verse (2/2)", "Chorus"), labels)
    }
}
