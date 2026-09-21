package com.church.presenter.churchpresentermobile.calendar.sync

import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.model.PresetSummary
import com.church.presenter.churchpresentermobile.model.RowEnd
import com.church.presenter.churchpresentermobile.model.RowTiming
import com.church.presenter.churchpresentermobile.model.SectionPalette
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SanitizeTest {

    private val today = LocalDate(2026, 9, 20)

    private fun service(vararg rows: PlanRow, date: String = "2026-09-27") = PlannedService(
        id = "svc-1",
        date = date,
        name = "Sunday Service",
        startTime = "10:00",
        rows = rows.toList(),
    )

    @Test
    fun textKeepsTheJoinersPersianHindiAndEmojiNeed() {
        val persian = "می\u200Cخواهم"
        val family = "👨\u200D👩\u200D👧"
        val conjunct = "क्\u200Dष"
        assertEquals(persian, Sanitize.text(persian, 100))
        assertEquals(family, Sanitize.text(family, 100))
        assertEquals(conjunct, Sanitize.text(conjunct, 100))
    }

    @Test
    fun textStripsTheInvisibleCharactersThatDisguiseText() {
        assertEquals("abcdefg", Sanitize.text("a\u202Eb\u200Bc\u2060d\uFEFFe\u2066f\u200Eg", 100))
        assertEquals("xy", Sanitize.text("x\uDB40\uDC41y", 100), "a tag character leaves no half a surrogate pair behind")
    }

    @Test
    fun textStripsControlAndFormatCharactersAndCaps() {
        assertEquals("Hello World", Sanitize.text("‮Hel\u0000lo\t \n World​", 100))
        assertEquals("abcde", Sanitize.text("abcdefgh", 5))
        assertEquals("", Sanitize.text("   \u0007  ", 10))
    }

    @Test
    fun idsAreShortAndPlain() {
        assertTrue(Sanitize.isId("svc_1.a:b-c"))
        assertFalse(Sanitize.isId(""))
        assertFalse(Sanitize.isId("has space"))
        assertFalse(Sanitize.isId("x".repeat(65)))
        assertFalse(Sanitize.isId("../etc"))
    }

    @Test
    fun aServiceWithABadHeaderIsRefused() {
        assertNull(Sanitize.service(service().copy(id = "bad id"), now = today))
        assertNull(Sanitize.service(service(date = "2026-02-30"), now = today))
        assertNull(Sanitize.service(service().copy(startTime = "25:00"), now = today))
    }

    @Test
    fun relayServicesMustFallInsideTheRetentionWindowButLocalOnesNeedNot() {
        val old = service(date = "2025-01-01")
        assertNull(Sanitize.service(old, fromRelay = true, now = today))
        assertNotNull(Sanitize.service(old, fromRelay = false, now = today))
        val farAhead = service(date = "2029-01-01")
        assertNull(Sanitize.service(farAhead, fromRelay = true, now = today))
        assertNotNull(Sanitize.service(service(date = "2026-06-25"), fromRelay = true, now = today))
    }

    @Test
    fun rowsAreCleanedDedupedAndCapped() {
        val rows = buildList {
            add(PlanRow.Section("r1", "‮Worship\u0000", color = "javascript:alert(1)"))
            add(PlanRow.Section("r1", "Duplicate id"))
            add(PlanRow.Song("bad id", "Dropped"))
            add(PlanRow.Song("r2", "", songbook = "Hymns"))
            add(PlanRow.Bible("r3", "John 3:16", preview = "x".repeat(500)))
            repeat(300) { add(PlanRow.Ministry("m$it", "Filler")) }
        }
        val clean = Sanitize.service(service(*rows.toTypedArray()), now = today)!!
        assertEquals(198, clean.rows.size)
        val section = clean.rows[0] as PlanRow.Section
        assertEquals("Worship", section.title)
        assertEquals(SectionPalette.DEFAULT, section.color)
        assertEquals("Song", (clean.rows[1] as PlanRow.Song).title)
        assertEquals(200, (clean.rows[2] as PlanRow.Bible).preview.length)
        assertEquals(1, clean.rows.count { it.id == "r1" })
        assertTrue(clean.rows.none { it.title == "Dropped" })
    }

    @Test
    fun timingAndSecondsAreKeptOnlyForRowsThatSurviveAndClamped() {
        val raw = service(PlanRow.Ministry("r1", "Sermon"), PlanRow.Ministry("r2", "Offering")).copy(
            plannedSeconds = mapOf("r1" to 999_999, "ghost" to 60),
            timing = mapOf(
                "r1" to RowTiming(startAt = "9am", followsPrevious = true, repeats = 500, atEnd = "explode"),
                "r2" to RowTiming(followsPrevious = true),
                "ghost" to RowTiming(startAt = "10:00"),
            ),
            kind = "party",
            seriesId = "not valid!",
            updatedAt = "yesterday",
            rev = -5,
        )
        val clean = Sanitize.service(raw, now = today)!!
        assertEquals(mapOf("r1" to 24 * 60 * 60), clean.plannedSeconds)
        val timing = clean.timing.getValue("r1")
        assertEquals("", timing.startAt)
        assertFalse(timing.followsPrevious)
        assertTrue(clean.timing.getValue("r2").followsPrevious)
        assertEquals(99, timing.repeats)
        assertEquals(RowEnd.HOLD, timing.atEnd)
        assertFalse("ghost" in clean.timing)
        assertEquals("sunday", clean.kind)
        assertEquals("", clean.seriesId)
        assertEquals("", clean.updatedAt)
        assertEquals(0L, clean.rev)
    }

    @Test
    fun defaultTimingIsNotStored() {
        val raw = service(PlanRow.Ministry("r1", "Sermon")).copy(timing = mapOf("r1" to RowTiming()))
        assertTrue(Sanitize.service(raw, now = today)!!.timing.isEmpty())
    }

    @Test
    fun headerFieldsAreNormalized() {
        val clean = Sanitize.service(service().copy(name = "  \u0007 ", startTime = "09:05"), now = today)!!
        assertEquals("Service", clean.name)
        assertEquals("09:05", clean.startTime)
        assertNull(Sanitize.service(service().copy(startTime = "9:05"), now = today))
    }

    @Test
    fun instantsMustBeIsoUtc() {
        assertEquals("2026-09-20T10:00:00Z", Sanitize.instant("2026-09-20T10:00:00Z"))
        assertEquals("2026-09-20T10:00:00.123456789Z", Sanitize.instant("2026-09-20T10:00:00.123456789Z"))
        assertEquals("", Sanitize.instant("2026-09-20T10:00:00+02:00"))
        assertEquals("", Sanitize.instant("now"))
    }

    @Test
    fun relayUrlsMustBeTlsAndAHostOnly() {
        assertEquals("https://sync.example.org", Sanitize.relayUrl(" https://sync.example.org/ "))
        assertEquals("https://sync.example.org:8443", Sanitize.relayUrl("https://sync.example.org:8443"))
        assertNull(Sanitize.relayUrl("http://sync.example.org"))
        assertNull(Sanitize.relayUrl("https://sync.example.org/path"))
        assertNull(Sanitize.relayUrl("https://user@sync.example.org"))
        assertNull(Sanitize.relayUrl(""))
    }

    @Test
    fun secretsAreUrlSafeBase64OfABoundedLength() {
        assertEquals("abcDEF123_-abcDEF", Sanitize.secret("abcDEF123_-abcDEF"))
        assertNull(Sanitize.secret("short"))
        assertNull(Sanitize.secret("has+plus/and=padding"))
        assertNull(Sanitize.secret("x".repeat(129)))
    }

    @Test
    fun presetsAreFilteredCleanedAndCapped() {
        val presets = buildList {
            add(PresetSummary("bad id", "Dropped"))
            add(PresetSummary("p1", "", kind = "timer\u0000", detail = "d".repeat(300)))
            repeat(600) { add(PresetSummary("p$it-x", "Preset $it")) }
        }
        val clean = Sanitize.presets(presets)
        assertEquals(500, clean.size)
        assertEquals("Preset", clean[0].name)
        assertEquals("timer", clean[0].kind)
        assertEquals(200, clean[0].detail.length)
        assertTrue(clean.none { it.name == "Dropped" })
    }
}
