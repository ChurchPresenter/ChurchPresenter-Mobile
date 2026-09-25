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
    fun aSyncKeepsWhatOutranksAndClearsWhatWasSent() {
        repository.saveService(service())
        repository.saveService(service(id = "svc-2", name = "Also local"))
        repository.deleteService("svc-2")
        // The phone's own write coming back stamped by the relay: the same edit, a new revision.
        val ownEchoed = repository.service("svc-1")!!.copy(rev = 4, updatedAt = "2026-09-20T10:05:00Z")
        val newFromRelay = service(id = "svc-3", name = "Planned on the desktop", rev = 5).copy(version = 1)
        val oldCopyOfDeleted = service(id = "svc-2", name = "Deleted here", rev = 6).copy(version = 1)
        repository.applySync(
            services = listOf(ownEchoed, newFromRelay, oldCopyOfDeleted),
            presets = listOf(PresetSummary("p1", "Countdown")),
            pushedIds = setOf("svc-1"),
        )
        val doc = repository.document.value
        assertEquals(listOf("svc-1", "svc-3"), doc.services.map { it.id })
        assertEquals(4L, doc.serviceById("svc-1")!!.rev)
        assertTrue(doc.pendingPush.isEmpty())
        assertEquals(setOf("svc-2"), doc.pendingDeletes, "the deletion outranks the copy it deleted")
        assertEquals(6L, doc.deletedRevs["svc-2"], "and is written next against the relay's revision")
        assertEquals(listOf(PresetSummary("p1", "Countdown")), doc.presets)
    }

    @Test
    fun aSealedDeletionThatOutranksTheServiceRemovesItAndItsPendingMarks() {
        repository.saveService(service())
        val deletion = PlannedService(
            "svc-1", "2026-09-20", "", "", version = 2, editedAt = "2026-09-20T11:00:00Z", deleted = true,
        )
        repository.applySync(deletions = listOf(deletion))
        val doc = repository.document.value
        assertTrue(doc.services.isEmpty())
        assertTrue(doc.pendingPush.isEmpty())
        assertTrue(doc.presets.isEmpty())
    }

    @Test
    fun anAcknowledgedDeleteIsNoLongerPendingButIsStillRemembered() {
        repository.saveService(service())
        repository.deleteService("svc-1")
        repository.applySync(deletedIds = setOf("svc-1"))
        val doc = repository.document.value
        assertTrue(doc.pendingDeletes.isEmpty())
        assertTrue("svc-1" in doc.deletedServices, "what refuses an old copy of it handed back later")
        assertEquals(2L, doc.deletedVersions["svc-1"], "one edit more than the copy it deleted")
    }

    @Test
    fun anEditCountsOnceUntilItHasBeenSent() {
        repository.saveService(service())
        assertEquals(1L, repository.service("svc-1")!!.version)
        repository.saveService(service().copy(name = "Again"))
        repository.saveService(service().copy(name = "And again"))
        assertEquals(1L, repository.service("svc-1")!!.version, "still one write waiting to go")
        repository.applySync(pushedIds = setOf("svc-1"))
        repository.saveService(service().copy(name = "After it went"))
        assertEquals(2L, repository.service("svc-1")!!.version)
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
