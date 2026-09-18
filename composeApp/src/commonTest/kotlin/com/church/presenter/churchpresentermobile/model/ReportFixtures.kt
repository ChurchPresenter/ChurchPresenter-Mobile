package com.church.presenter.churchpresentermobile.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/**
 * A play log the report tests share, with every timestamp in UTC so a test
 * can say "Jan 4" and mean it on every machine.
 */
internal object ReportFixtures {
    val zone: TimeZone = TimeZone.UTC

    val amazingGrace =
        SongCredit(id = "s1", number = "42", title = "Amazing Grace", author = "John Newton", songbook = "Hymnal")
    val comeThouFount =
        SongCredit(id = "s2", number = "7", title = "Come Thou Fount", author = "Robinson", songbook = "Hymnal")
    val blessedBe = SongCredit(id = "s3", number = "3", title = "Blessed Be", songbook = "Worship")
    val john316 = VerseCredit(bibleName = "KJV", bookName = "John", chapter = 3, verse = 16)
    val john317 = john316.copy(verse = 17)
    val psalm23 = VerseCredit(bibleName = "KJV", bookName = "Psalm", chapter = 23, verse = 1)
    val romans828 = VerseCredit(bibleName = "NIV", bookName = "Romans", chapter = 8, verse = 28)

    /** 10:30 UTC on [day]. */
    fun at(day: LocalDate): Long = ReportDates.startOf(day, zone) + SERVICE_TIME_MS

    fun day(year: Int, month: Int, day: Int): LocalDate = LocalDate(year, month, day)

    /** Twelve Sundays of 2026, Jan 4 to Mar 22: three songs and four verses. */
    val log = PlayLog(
        songs = listOf(
            SongPlay(amazingGrace, at(day(2026, 1, 4))),
            SongPlay(comeThouFount, at(day(2026, 1, 4))),
            SongPlay(amazingGrace, at(day(2026, 1, 18))),
            SongPlay(blessedBe, at(day(2026, 2, 1))),
            SongPlay(amazingGrace, at(day(2026, 2, 15))),
            SongPlay(comeThouFount, at(day(2026, 3, 1))),
            SongPlay(amazingGrace, at(day(2026, 3, 22))),
        ),
        verses = listOf(
            VersePlay(john316, at(day(2026, 1, 11))),
            VersePlay(psalm23, at(day(2026, 1, 18))),
            VersePlay(john316, at(day(2026, 2, 22))),
            VersePlay(john317, at(day(2026, 2, 22))),
            VersePlay(romans828, at(day(2026, 3, 8))),
        ),
    )

    val firstQuarter = ReportRange(day(2026, 1, 1), day(2026, 3, 31))

    private const val SERVICE_TIME_MS = (10 * 60 + 30) * 60_000L
}
