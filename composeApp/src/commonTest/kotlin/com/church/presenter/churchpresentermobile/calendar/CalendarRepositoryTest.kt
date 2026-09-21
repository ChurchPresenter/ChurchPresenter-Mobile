package com.church.presenter.churchpresentermobile.calendar

import com.church.presenter.churchpresentermobile.model.CalendarDocument
import com.church.presenter.churchpresentermobile.model.PlanRow
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.model.PresetSummary
import com.church.presenter.churchpresentermobile.model.SavedTemplate
import com.church.presenter.churchpresentermobile.testutil.InMemoryFileStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalendarRepositoryTest {

    private val storage = InMemoryFileStorage()
    private var clock = "2026-09-20T10:00:00Z"
    private val repository = CalendarRepository(storage, now = { clock })

    private fun service(id: String = "svc-1", name: String = "Sunday", rev: Long = 0) =
        PlannedService(id, "2026-09-27", name, "10:00", rows = listOf(PlanRow.Section("r1", "Worship")), rev = rev)

    @Test
    fun savingStampsMarksPendingAndWritesTheFile() {
        repository.saveService(service())
        val doc = repository.document.value
        assertEquals(clock, doc.serviceById("svc-1")!!.updatedAt)
        assertEquals(setOf("svc-1"), doc.pendingPush)
        assertEquals(1, storage.writeCount)
        assertTrue(storage.contains(CALENDAR_FILE))
    }

    @Test
    fun anUnchangedSaveWritesNothing() {
        repository.saveService(service())
        repository.saveService(repository.service("svc-1")!!)
        assertEquals(1, storage.writeCount)
    }

    @Test
    fun aServiceTheRulesRefuseIsNotSaved() {
        repository.saveService(service(id = "bad id"))
        assertTrue(repository.document.value.services.isEmpty())
        assertEquals(0, storage.writeCount)
    }

    @Test
    fun whatWasSavedLoadsBackSanitized() {
        repository.saveService(service(name = "Sunday\u0000 Service"))
        val reloaded = CalendarRepository(storage, now = { clock }).load()
        assertEquals("Sunday Service", reloaded.serviceById("svc-1")!!.name)
        assertEquals(setOf("svc-1"), reloaded.pendingPush)
    }

    @Test
    fun anUnreadableFileStartsEmptyRatherThanCrashing() {
        repository.saveService(service())
        storage.corrupt(CALENDAR_FILE)
        assertEquals(CalendarDocument.EMPTY, CalendarRepository(storage).load())
    }

    @Test
    fun deletingLeavesATombstoneAndAPendingDelete() {
        repository.saveService(service())
        clock = "2026-09-21T10:00:00Z"
        repository.deleteService("svc-1")
        val doc = repository.document.value
        assertNull(doc.serviceById("svc-1"))
        assertEquals(mapOf("svc-1" to clock), doc.deletedServices)
        assertEquals(setOf("svc-1"), doc.pendingDeletes)
        assertTrue(doc.pendingPush.isEmpty())
    }

    @Test
    fun applySyncTakesTheRelaysCopiesAndClearsWhatWasAccepted() {
        repository.saveService(service(name = "Local edit"))
        repository.saveService(service(id = "svc-2", name = "Also local"))
        repository.deleteService("svc-2")
        val fromRelay = service(name = "Local edit", rev = 4).copy(updatedAt = "2026-09-20T10:05:00Z")
        val newFromRelay = service(id = "svc-3", name = "Planned on the desktop", rev = 5)
        val reAdded = service(id = "svc-2", name = "Deleted here", rev = 6)
        repository.applySync(
            accepted = mapOf("svc-1" to fromRelay, "svc-3" to newFromRelay, "svc-2" to reAdded),
            removed = setOf("svc-9"),
            presets = listOf(PresetSummary("p1", "Countdown")),
            pushedIds = setOf("svc-1"),
            deletedIds = emptySet(),
        )
        val doc = repository.document.value
        assertEquals(listOf("svc-1", "svc-3"), doc.services.map { it.id })
        assertEquals(4L, doc.serviceById("svc-1")!!.rev)
        assertTrue(doc.pendingPush.isEmpty())
        assertEquals(setOf("svc-2"), doc.pendingDeletes)
        assertEquals(listOf(PresetSummary("p1", "Countdown")), doc.presets)
    }

    @Test
    fun aRemovalFromTheRelayDropsTheServiceAndItsPendingMarks() {
        repository.saveService(service())
        repository.applySync(
            accepted = emptyMap(),
            removed = setOf("svc-1"),
            presets = null,
            pushedIds = emptySet(),
            deletedIds = emptySet(),
        )
        val doc = repository.document.value
        assertTrue(doc.services.isEmpty())
        assertTrue(doc.pendingPush.isEmpty())
        assertTrue(doc.presets.isEmpty())
    }

    @Test
    fun anAcknowledgedDeleteForgetsTheTombstone() {
        repository.saveService(service())
        repository.deleteService("svc-1")
        repository.applySync(
            accepted = emptyMap(),
            removed = emptySet(),
            presets = null,
            pushedIds = emptySet(),
            deletedIds = setOf("svc-1"),
        )
        val doc = repository.document.value
        assertTrue(doc.pendingDeletes.isEmpty())
        assertTrue(doc.deletedServices.isEmpty())
    }

    @Test
    fun templatesReplaceByIdOrName() {
        repository.saveTemplate(SavedTemplate("t1", "Sunday"))
        repository.saveTemplate(SavedTemplate("t2", "sunday", startTime = "11:00"))
        assertEquals(listOf("t2"), repository.document.value.templates.map { it.id })
        repository.deleteTemplate("t2")
        assertTrue(repository.document.value.templates.isEmpty())
    }

    @Test
    fun aFailedWriteKeepsTheInMemoryCopy() {
        storage.failWrites = true
        repository.saveService(service())
        assertFalse(storage.contains(CALENDAR_FILE))
        assertEquals("Sunday", repository.service("svc-1")!!.name)
    }
}
