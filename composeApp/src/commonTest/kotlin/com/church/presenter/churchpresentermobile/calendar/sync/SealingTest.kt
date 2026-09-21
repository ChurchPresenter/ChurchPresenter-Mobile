package com.church.presenter.churchpresentermobile.calendar.sync

import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.model.PresetSummary
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class SealingTest {

    private val key = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8"

    /** `{"id":"svc-1",…}` sealed by the desktop's `Envelope` under [key] for instance `inst-1`. */
    private val desktopBox =
        "ZGVmZ2hpamtsbW5vMzm3AlvTdO1IAXLZ+ElImSO2YyixTsFCleeBeMKOl3+2xWKzOHPpcKmaFVrfeOj+K2Go1wJmY0abnMn0" +
        "1aP2/2bounf2GF53/QaOJjOmE40NFnsWnef+s7GVV71ZP2wj4QbhXFEWw1zHg66ggDeam0dI9HauF5sYkBD3k+CWevYQs7rc" +
        "AEYuTutzdFLm8i/q/7ffanIo6f5e41x3wjKSvtQCYR+yoc2bSSzYh4lWmezlnqPOFZ9wphwjK2qwsj3i6FtvnsRwVjtgNw4R" +
        "IBq7KAEltnboeFvSgGtqvZzHPck9SMyc6+EJBDjkJbzGJrrrZOQl1q65edxW80n6T4qPOG+5jRODjA=="

    private val service = PlannedService(
        id = "svc-1",
        date = "2026-09-27",
        name = "Sunday Service",
        startTime = "10:00",
        rows = listOf(
            PlanRow.Section("r1", "Worship"),
            PlanRow.Song("r2", "Here I Am to Worship", songId = "Hymns::42"),
        ),
        updatedAt = "2026-09-20T10:00:00Z",
        rev = 7,
    )

    private suspend fun sealing(instance: String = "inst-1") = Sealing.fromEncodedKey(key, instance)!!

    @Test
    fun aRecordSealedByTheDesktopOpensHere() = runTest {
        val record = SealedRecord("svc-1", "2026-12-26", desktopBox, updatedAt = "2026-09-20T10:00:00Z", rev = 3)
        val opened = sealing().open(record)!!
        assertEquals("Sunday Service", opened.name)
        assertEquals(listOf("r1", "r2"), opened.rows.map { it.id })
        assertEquals("Hymns::42", (opened.rows[1] as PlanRow.Song).songId)
        assertEquals("2026-09-20T10:00:00Z", opened.updatedAt)
        assertEquals(3L, opened.rev)
    }

    @Test
    fun whatIsSealedHereOpensHereWithTheRelayStampsApplied() = runTest {
        val record = sealing().seal(service)
        assertEquals("svc-1", record.id)
        assertEquals("2026-12-26", record.keepUntil)
        val opened = sealing().open(record.copy(updatedAt = "2026-09-21T00:00:00Z", rev = 9))!!
        assertEquals(service.copy(updatedAt = "2026-09-21T00:00:00Z", rev = 9), opened)
    }

    @Test
    fun everySealDrawsAFreshNonce() = runTest {
        val s = sealing()
        assertNotEquals(s.seal(service).box, s.seal(service).box)
    }

    @Test
    fun aBoxDoesNotOpenUnderAnotherRecordIdInstanceOrKey() = runTest {
        val record = sealing().seal(service)
        assertNull(sealing().open(record.copy(id = "svc-2")))
        assertNull(sealing(instance = "inst-2").open(record))
        val otherKey = Sealing.fromEncodedKey("AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh4", "inst-1")!!
        assertNull(otherKey.open(record))
    }

    @Test
    fun aTamperedOrMalformedBoxOpensToNothing() = runTest {
        val s = sealing()
        val record = s.seal(service)
        val flipped = record.box.toCharArray().also { it[20] = if (it[20] == 'A') 'B' else 'A' }.concatToString()
        assertNull(s.open(record.copy(box = flipped)))
        assertNull(s.open(record.copy(box = "")))
        assertNull(s.open(record.copy(box = "not base64!")))
        assertNull(s.open(record.copy(box = "AAAA")))
        assertNull(s.open(record.copy(box = "A".repeat(256 * 1024 + 4))))
    }

    @Test
    fun aRecordWhosePlaintextClaimsAnotherIdIsRefused() = runTest {
        val s = sealing()
        val box = s.sealText("""{"id":"svc-9","date":"2026-09-27","name":"Sunday","startTime":"10:00"}""", "svc-1")
        assertNull(s.open(SealedRecord("svc-1", "2026-12-26", box)))
        assertNull(s.open(SealedRecord("svc-1", "2026-12-26", s.sealText("not json", "svc-1"))))
    }

    @Test
    fun thePresetIndexOpensUnderItsOwnRecordId() = runTest {
        val s = sealing()
        val box = s.sealText("""{"presets":[{"id":"p1","name":"Countdown","kind":"timer"}]}""", PRESETS_RECORD)
        assertEquals(listOf(PresetSummary("p1", "Countdown", kind = "timer")), s.openPresets(box)!!.presets)
        assertNull(s.openPresets(s.sealText("{}", "svc-1")))
    }

    @Test
    fun onlyA32ByteUrlSafeKeyIsAccepted() = runTest {
        assertNull(Sealing.fromEncodedKey("tooshort", "inst-1"))
        assertNull(Sealing.fromEncodedKey("AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8A", "inst-1"))
        assertNull(Sealing.fromEncodedKey("!!!!", "inst-1"))
    }
}
