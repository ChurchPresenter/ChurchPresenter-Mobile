package com.church.presenter.churchpresentermobile.ui.calendar

import com.church.presenter.churchpresentermobile.calendar.PickerBook
import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.model.PresetSummary
import com.church.presenter.churchpresentermobile.model.RowKind
import com.church.presenter.churchpresentermobile.model.RowTiming
import com.church.presenter.churchpresentermobile.model.SectionPalette
import com.church.presenter.churchpresentermobile.model.ServiceKind
import com.church.presenter.churchpresentermobile.model.Song
import kotlinx.datetime.LocalDate

/**
 * The service every calendar UI test plans against.
 *
 * One fixture rather than one per test: what these screens draw is mostly the row list, and a
 * service carrying one of every row kind exercises every branch of it at once.
 *
 * Note what these tests assert on. String resources do not resolve in this runtime — a label from
 * `strings.xml` renders empty — so an assertion names data instead: a service's name, a row's
 * title, a date this app formatted itself. The labels are what the screenshot suite is for.
 */
internal object CalendarFixtures {

    val SUNDAY: LocalDate = LocalDate(2026, 9, 20)
    val TODAY: LocalDate = LocalDate(2026, 9, 17)

    const val SERVICE_NAME = "Sunday Morning"
    const val FIRST_SONG = "Amazing Grace"
    const val SECOND_SONG = "How Great Thou Art"
    const val PASSAGE = "John 3:16-17"
    const val MINISTRY = "Welcome & notices"
    const val SECTION = "Worship"

    val service = PlannedService(
        id = "s1",
        date = "2026-09-20",
        name = SERVICE_NAME,
        startTime = "10:00",
        rows = listOf(
            PlanRow.Section(id = "r1", title = SECTION, color = SectionPalette.VIOLET),
            PlanRow.Song(id = "r2", title = FIRST_SONG, songbook = "Hymnal", number = "42"),
            PlanRow.Song(id = "r3", title = SECOND_SONG, songbook = "Hymnal", number = "108"),
            PlanRow.Bible(id = "r4", title = PASSAGE, preview = "For God so loved the world"),
            PlanRow.Ministry(id = "r5", title = MINISTRY, detail = "Anna"),
            PlanRow.Ref(id = "r6", title = "Countdown", kind = RowKind.CUE, subtitle = "Blank the screens"),
        ),
        plannedSeconds = mapOf("r2" to 270, "r3" to 300, "r4" to 120, "r5" to 300),
        timing = mapOf("r2" to RowTiming(startAt = "10:05")),
    )

    val empty = service.copy(rows = emptyList(), plannedSeconds = emptyMap(), timing = emptyMap())

    val midweek = PlannedService(
        id = "s2",
        date = "2026-09-23",
        name = "Midweek Prayer",
        startTime = "19:00",
        kind = ServiceKind.MIDWEEK.id,
    )

    val special = PlannedService(
        id = "s3",
        date = "2026-09-27",
        name = "Baptism Service",
        startTime = "10:00",
        kind = ServiceKind.SPECIAL.id,
    )

    val month = listOf(service, midweek, special)

    val songs = listOf(
        Song(number = "42", title = FIRST_SONG, bookName = "Hymnal"),
        Song(number = "108", title = SECOND_SONG, bookName = "Hymnal"),
    )

    val books = listOf(
        PickerBook(number = 19, name = "Psalms", chapters = 150),
        PickerBook(number = 43, name = "John", chapters = 21),
    )

    val presets = listOf(
        PresetSummary(id = "p1", name = "Welcome loop", kind = RowKind.MEDIA),
        PresetSummary(id = "p2", name = "Offering slide", kind = RowKind.PICTURES),
    )

    fun sources(
        songs: List<Song> = this.songs,
        books: List<PickerBook> = this.books,
        presets: List<PresetSummary> = this.presets,
    ) = PickerSources(songs, books, presets, chapterPreview = { _, _ -> null })
}
