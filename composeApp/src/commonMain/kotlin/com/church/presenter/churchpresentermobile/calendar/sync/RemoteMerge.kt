package com.church.presenter.churchpresentermobile.calendar.sync

import com.church.presenter.churchpresentermobile.model.CalendarDocument
import com.church.presenter.churchpresentermobile.model.PlannedService

// How this phone's copies meet the relay's, by SYNC.md's *Which copy wins*: a copy or a sealed
// deletion replaces what is here only when it outranks it. The relay's own stamps and its
// plaintext tombstones decide nothing, so a relay cannot roll a service back or delete one.

/**
 * [service] as an edit made here: stamped, and one edit more than the copy it was made from.
 * Edits made while one is already waiting to be sent count once -- they reach the relay as one
 * write, and ten quick changes here must not outrank a single later one on the desktop.
 */
internal fun CalendarDocument.edited(service: PlannedService, stamp: String): PlannedService {
    val before = serviceById(service.id)
    val base = before?.version ?: 0L
    val version = if (before != null && service.id in pendingPush) base else base + 1
    return service.copy(updatedAt = stamp, editedAt = stamp, version = version, deleted = false)
}

/**
 * A copy from the relay. One of ours that outranks it stays, takes the relay's revision so its
 * next write is not refused, and stays pending if it was; our own copy coming back stamped
 * replaces ours.
 */
internal fun CalendarDocument.receiving(remote: PlannedService): CalendarDocument {
    val id = remote.id
    val deletedAt = deletedServices[id]
    val local = serviceById(id)
    return when {
        deletedAt != null &&
            !EditOrder.outranks(remote.version, remote.editedAt, deletedVersions[id] ?: 0L, deletedAt) ->
            // Our deletion stands; its write is now conditioned on the revision the relay holds.
            copy(deletedRevs = deletedRevs + (id to remote.rev))
        deletedAt != null ->
            // Edited from the copy we deleted, after we deleted it: somebody went back to it.
            copy(
                deletedServices = deletedServices - id,
                deletedVersions = deletedVersions - id,
                deletedRevs = deletedRevs - id,
                pendingDeletes = pendingDeletes - id,
            ).withService(remote)
        local == null -> withService(remote)
        sameEdit(remote, local) || EditOrder.outranks(remote.version, remote.editedAt, local.version, local.editedAt) ->
            withService(remote).copy(pendingPush = pendingPush - id)
        else -> withService(local.copy(rev = remote.rev))
    }
}

/** A sealed deletion from the relay: it removes a copy here that does not outrank it. */
internal fun CalendarDocument.receivingDeletion(deletion: PlannedService): CalendarDocument {
    val id = deletion.id
    val local = serviceById(id)
    val remembered = deletedVersions[id] ?: -1L
    return when {
        local != null && EditOrder.outranks(local.version, local.editedAt, deletion.version, deletion.editedAt) ->
            withService(local.copy(rev = deletion.rev))
        local == null && remembered > deletion.version -> copy(deletedRevs = deletedRevs + (id to deletion.rev))
        else -> copy(
            services = services.filterNot { it.id == id },
            pendingPush = pendingPush - id,
            pendingDeletes = pendingDeletes - id,
            deletedServices = deletedServices + (id to deletion.editedAt),
            deletedVersions = deletedVersions + (id to deletion.version),
            deletedRevs = deletedRevs + (id to deletion.rev),
        )
    }
}

private fun sameEdit(a: PlannedService, b: PlannedService): Boolean = a.version == b.version && a.editedAt == b.editedAt
