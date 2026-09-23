package com.church.presenter.churchpresentermobile.calendar

import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.model.RowKind
import com.church.presenter.churchpresentermobile.model.RowTiming
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServiceCopyTest {

    private var counter = 0
    private val newId: () -> String = { "id-${++counter}" }

    private val source = PlannedService(
        id = "svc",
        date = "2026-09-27",
        name = "Sunday",
        startTime = "10:00",
        kind = "special",
        rows = listOf(
            PlanRow.Section("s", "Worship"),
            PlanRow.Song("a", "Opening"),
            PlanRow.Ref("c", "Blank outputs", kind = RowKind.CUE),
            PlanRow.Ref("w", "Website", kind = RowKind.OTHER),
        ),
        plannedSeconds = mapOf("a" to 270, "ghost" to 1),
        timing = mapOf("a" to RowTiming(repeats = 2), "c" to RowTiming(startAt = "10:30")),
        armed = false,
        seriesId = "series-1",
    )

    @Test
    fun repeatDatesFollowTheRule() {
        val from = LocalDate(2026, 9, 27)
        assertEquals(listOf(LocalDate(2026, 10, 4)), repeatDates(from, RepeatRule.ONCE, 5))
        assertEquals(listOf(LocalDate(2026, 10, 4), LocalDate(2026, 10, 11)), repeatDates(from, RepeatRule.WEEKLY, 2))
        assertEquals(listOf(LocalDate(2026, 10, 11)), repeatDates(from, RepeatRule.FORTNIGHTLY, 1))
        assertEquals(listOf(LocalDate(2026, 10, 27), LocalDate(2026, 11, 27)), repeatDates(from, RepeatRule.MONTHLY, 2))
        assertEquals(1, repeatDates(from, RepeatRule.WEEKLY, 0).size)
    }

    @Test
    fun aCopyKeepsEverythingUnderFreshRowIds() {
        val copy = copyService(source, LocalDate(2026, 10, 4), newId, at = "2026-09-20T10:00:00Z")
        assertEquals("2026-10-04", copy.date)
        assertEquals("Sunday", copy.name)
        assertEquals("special", copy.kind)
        assertEquals("series-1", copy.seriesId)
        assertEquals("2026-09-20T10:00:00Z", copy.updatedAt)
        assertTrue(!copy.armed)
        assertEquals(source.rows.map { it.title }, copy.rows.map { it.title })
        assertTrue(copy.rows.map { it.id }.none { it in source.rows.map { r -> r.id } })
        assertTrue(copy.id !in copy.rows.map { it.id })
        val song = copy.rows[1]
        assertEquals(mapOf(song.id to 270), copy.plannedSeconds)
        assertEquals(RowTiming(repeats = 2), copy.timing[song.id])
        assertEquals(RowTiming(startAt = "10:30"), copy.timing[copy.rows[2].id])
    }

    @Test
    fun cuesAndRowsCanBeLeftBehind() {
        val noCues = copyService(source, LocalDate(2026, 10, 4), newId, includeCues = false, at = "")
        assertEquals(listOf("Worship", "Opening", "Website"), noCues.rows.map { it.title })
        assertTrue(noCues.timing.values.none { it.startAt == "10:30" })
        val empty = copyService(source, LocalDate(2026, 10, 4), newId, includeRows = false, seriesId = "", at = "")
        assertTrue(empty.rows.isEmpty())
        assertTrue(empty.plannedSeconds.isEmpty())
        assertEquals("", empty.seriesId)
    }

    @Test
    fun aTemplateRoundTripsIntoANewService() {
        val template = templateFrom(source, "Standard Sunday", newId)
        assertEquals("Standard Sunday", template.name)
        assertEquals("10:00", template.startTime)
        assertEquals(source.rows, template.rows)
        val allSaints = LocalDate(2026, 11, 1)
        val heading = ServiceHeading("All Saints", "09:30", "sunday")
        val service = serviceFromTemplate(template, allSaints, heading, newId, at = "t")
        assertEquals("All Saints", service.name)
        assertEquals("09:30", service.startTime)
        assertEquals("2026-11-01", service.date)
        assertEquals(source.rows.map { it.title }, service.rows.map { it.title })
        assertTrue(service.rows.map { it.id }.none { it in source.rows.map { r -> r.id } })
        assertEquals(270, service.plannedSeconds.values.single())
        val blankHeading = ServiceHeading("Blank", "09:30", "sunday")
        val blank = serviceFromTemplate(null, LocalDate(2026, 11, 1), blankHeading, newId, at = "t")
        assertTrue(blank.rows.isEmpty())
    }

    @Test
    fun everyRowKindCanBeRekeyed() {
        val rows = listOf(
            PlanRow.Section("1", "s"),
            PlanRow.Song("2", "s"),
            PlanRow.Bible("3", "b"),
            PlanRow.Ministry("4", "m"),
            PlanRow.Preset("5", "p", presetId = "x"),
            PlanRow.Ref("6", "r"),
        )
        assertEquals(List(6) { "new" }, rows.map { it.withId("new").id })
        assertEquals(rows.map { it.title }, rows.map { it.withId("new").title })
    }
}
