package com.church.presenter.churchpresentermobile.calendar.sync

import com.church.presenter.churchpresentermobile.calendar.CalendarRepository
import com.church.presenter.churchpresentermobile.calendar.nowIso
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "CalendarSync"

/** Where this phone stands with the relay. */
sealed class SyncStatus {
    data object NotEnrolled : SyncStatus()
    data object Syncing : SyncStatus()
    data class Synced(val at: String, val pulled: Int, val pushed: Int) : SyncStatus()
    data class Failed(val message: String) : SyncStatus()
    /** The relay no longer accepts this phone: revoked, or the desktop started over with a new key. */
    data object Unauthorized : SyncStatus()
}

/**
 * One round of keeping this phone's `calendar.json` and the relay in step: replay what was edited
 * here, then take what changed there. A write the relay refuses as stale gives way to the other
 * copy, which the next pull brings.
 */
class CalendarSyncEngine(
    private val repository: CalendarRepository,
    private val state: () -> CalendarSyncState,
    private val saveState: (CalendarSyncState) -> Unit,
    private val clientFor: (CalendarSyncState) -> RelayClient = { RelayClient(it) },
) {
    private val _status = MutableStateFlow<SyncStatus>(if (state().isEnrolled) SyncStatus.Synced("", 0, 0) else SyncStatus.NotEnrolled)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val lock = Mutex()

    val isEnrolled: Boolean get() = state().isEnrolled

    /** Forgets the enrollment on this phone; the desktop's Revoke does the same at the relay. */
    fun leave() {
        saveState(CalendarSyncState())
        _status.value = SyncStatus.NotEnrolled
    }

    suspend fun sync(): Boolean = lock.withLock {
        val current = state()
        if (!current.isEnrolled) {
            _status.value = SyncStatus.NotEnrolled
            return@withLock false
        }
        val sealing = Sealing.fromEncodedKey(current.instanceKey, current.instanceId)
        if (sealing == null) {
            _status.value = SyncStatus.Unauthorized
            return@withLock false
        }
        _status.value = SyncStatus.Syncing
        try {
            val client = clientFor(current)
            val pushed = push(client, sealing)
            val pulled = pull(client, sealing, current, pushed)
            _status.value = SyncStatus.Synced(nowIso(), pulled, pushed.size)
            true
        } catch (e: RelayFailure.Unauthorized) {
            _status.value = SyncStatus.Unauthorized
            false
        } catch (e: RelayFailure) {
            Logger.e(TAG, "sync failed: ${e.message}")
            _status.value = SyncStatus.Failed(e.message.orEmpty())
            false
        } catch (e: Exception) {
            Logger.e(TAG, "sync failed: ${e.message}", e)
            _status.value = SyncStatus.Failed(e.message.orEmpty())
            false
        }
    }

    /** Replays local edits and deletes. A stale write is dropped here and the pull below brings the winner. */
    private suspend fun push(client: RelayClient, sealing: Sealing): Set<String> {
        val doc = repository.document.value
        val done = HashSet<String>()
        for (id in doc.pendingPush) {
            val service = doc.serviceById(id) ?: run { done += id; continue }
            try {
                client.putRecord(sealing.seal(service), ifRev = service.rev)
            } catch (_: RelayFailure.Conflict) {
                Logger.d(TAG, "push $id — relay has a newer copy, taking theirs")
            }
            done += id
        }
        val deleted = HashSet<String>()
        for (id in doc.pendingDeletes) {
            runCatching { client.deleteRecord(id) }
                .onFailure { if (it !is RelayFailure.Conflict) throw it }
            deleted += id
        }
        repository.applySync(accepted = emptyMap(), removed = emptySet(), presets = null, pushedIds = done, deletedIds = deleted)
        return done
    }

    private suspend fun pull(client: RelayClient, sealing: Sealing, current: CalendarSyncState, pushed: Set<String>): Int {
        val changes = client.changes(current.cursor)
        if (changes.rev < current.cursor) throw RelayFailure.Rejected(0, "revision went backwards")
        val accepted = HashMap<String, PlannedService>()
        var unreadable = 0
        var rejected = 0
        for (record in changes.records.take(Sanitize.RECORDS_PER_PULL)) {
            val opened = sealing.open(record)
            if (opened == null) {
                unreadable++
                continue
            }
            val service = Sanitize.service(opened)
            if (service == null) {
                rejected++
                continue
            }
            accepted[service.id] = service
        }
        if (unreadable > 0) Logger.e(TAG, "pull — $unreadable records did not open under this key")
        if (rejected > 0) Logger.e(TAG, "pull — $rejected records were not services this phone keeps")
        val removed = changes.tombstones.map { it.id }.filter(Sanitize::isId).toSet()
        val presets = changes.presetsBox.takeIf { it.isNotEmpty() }?.let { box -> sealing.openPresets(box)?.presets?.let(Sanitize::presets) }
        // An edit made here while this round ran stays pending; a record we just pushed comes
        // back stamped by the relay and replaces our unstamped copy.
        val stillPending = repository.document.value.pendingPush - pushed
        val applied = accepted.filterKeys { it !in stillPending }
        repository.applySync(accepted = applied, removed = removed, presets = presets, pushedIds = emptySet(), deletedIds = emptySet())
        saveState(current.copy(cursor = changes.rev, lastSyncAt = nowIso()))
        return applied.size + removed.size
    }
}
