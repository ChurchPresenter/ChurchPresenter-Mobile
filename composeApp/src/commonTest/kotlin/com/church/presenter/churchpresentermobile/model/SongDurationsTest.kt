package com.church.presenter.churchpresentermobile.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SongDurationsTest {

    private val durations = SongDurations.of(
        listOf(
            Song(number = "42", title = "Here I Am to Worship", bookName = "Hymnal") to 270,
            Song(number = "", title = "Untitled Chorus", bookName = "Hymnal") to 95,
            Song(number = "3", title = "Amazing Grace", bookName = "Praise") to 200,
            Song(number = "1", title = "Amazing Grace", bookName = "Hymnal") to 240,
        ),
    )

    @Test
    fun theDesktopKeyIsBookAndNumberOrBookAndTitle() {
        val numbered = Song(id = 12, number = "42", title = "Here I Am", bookName = "Hymnal")
        assertEquals("Hymnal::42", numbered.desktopSongId)
        val unnumbered = Song(number = "", title = "Untitled Chorus", bookName = "Hymnal")
        assertEquals("Hymnal::Untitled Chorus", unnumbered.desktopSongId)
        assertEquals("::7", Song(number = "7", title = "No book").desktopSongId)
    }

    @Test
    fun aSongIsFoundByItsKeyFirst() {
        val hereIAm = Song(number = "42", title = "Here I Am to Worship", bookName = "Hymnal")
        assertEquals(270, durations.secondsFor(hereIAm))
        assertEquals(95, durations.secondsFor(Song(number = "", title = "Untitled Chorus", bookName = "Hymnal")))
    }

    @Test
    fun thenByTitleWhenTheTitleIsUnambiguous() {
        val byTitle = Song(number = "9", title = "  here i am to worship ", bookName = "Other")
        assertEquals(270, durations.secondsFor(byTitle))
        assertNull(durations.secondsFor(Song(number = "9", title = "Amazing Grace", bookName = "Other")))
        assertNull(durations.secondsFor(Song(number = "9", title = "Never Sung", bookName = "Hymnal")))
        val known = Song(number = "42", title = "Here I Am to Worship", bookName = "Hymnal")
        assertNull(SongDurations.NONE.secondsFor(known))
    }
}
