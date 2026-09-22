package com.church.presenter.churchpresentermobile.calendar.sync

import com.church.presenter.churchpresentermobile.model.Song
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SongCatalogStoreTest {

    private val storage = InMemoryFileStorage()
    private val store = SongCatalogStore(storage)

    private val hymnal = CatalogRecord(
        "Hymnal",
        songs = listOf(
            CatalogSong("42", "Here I Am to Worship", 270, "Вот я, Господь"),
            CatalogSong("7", "Never Sung"),
        ),
    )
    private val praise = CatalogRecord("Songs of Praise", songs = listOf(CatalogSong("1", "Shout", 95)))

    @Test
    fun theLanCopyReplacesEverythingAndComesBackAfterARestart() {
        store.replaceAll(listOf(hymnal, praise))
        assertEquals(setOf("catalog:Hymnal", "catalog:Songs_of_Praise"), store.records.value.keys)

        store.replaceAll(listOf(praise))
        assertEquals(setOf("catalog:Songs_of_Praise"), SongCatalogStore(storage).load().keys)
    }

    @Test
    fun aRelayPullMergesRecordByRecordAndTombstonesRemove() {
        store.replaceAll(listOf(hymnal))

        store.merge(put = mapOf("catalog:Songs_of_Praise" to praise), removed = emptySet())
        assertEquals(2, store.records.value.size)

        store.merge(put = emptyMap(), removed = setOf("catalog:Hymnal"))
        assertEquals(setOf("catalog:Songs_of_Praise"), store.records.value.keys)
    }

    @Test
    fun songsCarryTheirBookNumberSecondTitleAndTheDesktopsKey() {
        store.replaceAll(listOf(praise, hymnal))

        val songs = store.songs()

        assertEquals(listOf("Hymnal", "Hymnal", "Songs of Praise"), songs.map { it.bookName })
        val hereIAm = songs.first { it.number == "42" }
        assertEquals("Hymnal::42", hereIAm.desktopSongId)
        assertEquals("Вот я, Господь", hereIAm.secondaryTitle)
        assertNull(songs.first { it.number == "7" }.secondaryTitle)
        assertEquals(songs.size, songs.map { it.identity }.toSet().size, "every song keys a list row on its own")
    }

    @Test
    fun durationsAreTheLengthsTheBooksCarry() {
        store.replaceAll(listOf(hymnal, praise))

        val durations = store.durations()

        val hereIAm = Song(number = "42", title = "Here I Am to Worship", bookName = "Hymnal")
        assertEquals(270, durations.secondsFor(hereIAm))
        assertEquals(95, durations.secondsFor(Song(number = "9", title = "shout", bookName = "Other")))
        assertNull(durations.secondsFor(Song(number = "7", title = "Never Sung", bookName = "Hymnal")))
    }

    @Test
    fun whatArrivesIsCleanedAndCapped() {
        val hostile = CatalogRecord(
            "‮Book\u0000" + "x".repeat(300),
            part = -3,
            songs = listOf(
                CatalogSong("1", "\u0007Fine​", s = -10, t2 = "  "),
                CatalogSong("2", "", s = 99_999_999),
            ) + List(2_500) { CatalogSong("$it", "Filler $it") },
        )

        store.replaceAll(listOf(hostile))

        val record = store.records.value.values.single()
        assertEquals(120, record.songbook.length)
        assertEquals(0, record.part)
        assertEquals(2_000 - 1, record.songs.size)
        assertEquals(CatalogSong("1", "Fine", 0), record.songs.first())
        assertTrue(record.songs.none { it.t.isEmpty() })
    }

    @Test
    fun anUnreadableFileStartsEmpty() {
        store.replaceAll(listOf(hymnal))
        storage.corrupt(SONG_CATALOG_FILE)

        assertTrue(SongCatalogStore(storage).isEmpty)
    }
}
