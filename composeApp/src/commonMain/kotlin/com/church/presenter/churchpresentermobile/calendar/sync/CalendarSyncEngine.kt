package com.church.presenter.churchpresentermobile.calendar.sync

import com.church.presenter.churchpresentermobile.calendar.CalendarRepository
import com.church.presenter.churchpresentermobile.calendar.nowIso
import com.church.presenter.churchpresentermobile.model.PlannedService
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.io.IOException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "CalendarSync"
private const val NAME_CHARS = 120

/** 2,000 rows a page: far beyond what the relay holds, so this only stops a runaway. */
private const val MAX_PULL_PAGES = 50

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
    private val clientKeys: ClientKeySource,
    private val pushToken: () -> String = { "" },
    /** What this phone calls itself, for the desktop's list of who is enrolled. */
    private val deviceName: () -> String = { "" },
    private val clientFor: (CalendarSyncState, suspend () -> String) -> RelayClient = { s, k -> RelayClient(s, k) },
    /** Where the desktop's songbooks land when they arrive through the relay. */
    private val catalogStore: SongCatalogStore? = null,
) {
    private val _status = MutableStateFlow<SyncStatus>(
        if (state().isEnrolled) SyncStatus.Synced("", 0, 0) else SyncStatus.NotEnrolled,
    )
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
            val client = clientFor(current, clientKeys::current)
            try {
                round(client, sealing, current)
            } catch (rotated: RelayFailure.ClientKey) {
                // The key rotated under us: fetch the new one and go once more.
                if (clientKeys.refresh() == null) throw rotated
                round(client, sealing, state())
            }
            true
        } catch (refused: RelayFailure.Unauthorized) {
            Logger.e(TAG, "the relay no longer accepts this phone: ${refused.message}")
            _status.value = SyncStatus.Unauthorized
            false
        } catch (e: RelayFailure) {
            Logger.e(TAG, "sync failed: ${e.message}")
            _status.value = SyncStatus.Failed(e.message.orEmpty())
            false
        } catch (e: IllegalStateException) {
            // Whatever the wire handed us could not be made sense of. A round is background work:
            // it reports and waits for the next one rather than taking the app down.
            Logger.e(TAG, "sync failed: ${e.message}", e)
            _status.value = SyncStatus.Failed(e.message.orEmpty())
            false
        } catch (e: IllegalArgumentException) {
            Logger.e(TAG, "sync failed: ${e.message}", e)
            _status.value = SyncStatus.Failed(e.message.orEmpty())
            false
        } catch (e: IOException) {
            // The network itself: no route, a dropped WiFi, a captive portal, a certificate the
            // device's clock says is not valid yet. Uncaught, one took the app down.
            Logger.e(TAG, "sync failed: ${e.message}", e)
            _status.value = SyncStatus.Failed(e.message.orEmpty())
            false
        }
    }

    private suspend fun round(client: RelayClient, sealing: Sealing, current: CalendarSyncState) {
        registerPushIfChanged(client, current)
        registerNameIfChanged(client, sealing, current)
        val pushed = push(client, sealing)
        val pulled = pull(client, sealing, state())
        _status.value = SyncStatus.Synced(nowIso(), pulled, pushed.size)
    }

    private suspend fun registerPushIfChanged(client: RelayClient, current: CalendarSyncState) {
        val token = pushToken()
        if (token.isBlank() || token == current.registeredPushToken) return
        runCatching { client.registerPushToken(token) }
            .onSuccess { saveState(state().copy(registeredPushToken = token)) }
            .onFailure { Logger.e(TAG, "push token not registered: ${it.message}") }
    }

    /**
     * A phone enrolled from an invite has no name at the relay until it says one. Sent once, and
     * again only when the name changes; a phone enrolled the old way has no device id to seal
     * under and keeps the name the desktop gave it.
     */
    private suspend fun registerNameIfChanged(client: RelayClient, sealing: Sealing, current: CalendarSyncState) {
        val name = deviceName().trim().take(NAME_CHARS)
        if (current.deviceId.isEmpty() || name.isEmpty() || name == current.registeredName) return
        runCatching { client.registerName(sealing.sealText(name, current.deviceId)) }
            .onSuccess { saveState(state().copy(registeredName = name)) }
            .onFailure { Logger.e(TAG, "name not registered: ${it.message}") }
    }

    /**
     * Replays local edits and deletes. A write the relay refuses as stale stays pending: the pull
     * below brings the relay's copy, and whichever outranks the other (SYNC.md, *Which copy wins*)
     * is kept -- ours is then written again next round, against the revision the pull brought.
     */
    private suspend fun push(client: RelayClient, sealing: Sealing): Set<String> {
        val doc = repository.document.value
        val done = HashSet<String>()
        for (id in doc.pendingPush) {
            val service = doc.serviceById(id) ?: run { done += id; continue }
            try {
                client.putRecord(sealing.seal(service), ifRev = service.rev)
                done += id
            } catch (_: RelayFailure.Conflict) {
                Logger.d(TAG, "push $id — relay has another copy; the pull decides which stands")
            }
        }
        // A deletion is a sealed record like any other, never the relay's own unsigned tombstone.
        val deleted = HashSet<String>()
        for (id in doc.pendingDeletes) {
            val deletedAt = doc.deletedServices[id] ?: nowIso()
            val deletion = PlannedService(
                id = id,
                // Only for the relay's retention: kept as long as the deletion is remembered.
                date = deletedAt.take(DATE_CHARS),
                name = "",
                startTime = "",
                version = doc.deletedVersions[id] ?: 1L,
                editedAt = deletedAt,
                deleted = true,
            )
            try {
                client.putRecord(sealing.seal(deletion), ifRev = doc.deletedRevs[id] ?: 0L)
                deleted += id
            } catch (_: RelayFailure.Conflict) {
                Logger.d(TAG, "delete $id — relay has another copy; the pull decides which stands")
            }
        }
        repository.applySync(pushedIds = done, deletedIds = deleted)
        return done
    }

    private suspend fun pull(
        client: RelayClient,
        sealing: Sealing,
        current: CalendarSyncState,
        page: Int = 1,
    ): Int {
        val changes = client.changes(current.cursor)
        if (changes.rev < current.cursor) throw RelayFailure.Rejected(0, "revision went backwards")
        val accepted = HashMap<String, PlannedService>()
        val deletions = HashMap<String, PlannedService>()
        val catalog = HashMap<String, CatalogRecord>()
        var unreadable = 0
        var rejected = 0
        val now = nowIso()
        for (record in changes.records.take(Sanitize.RECORDS_PER_PULL)) {
            when {
                record.id.startsWith(CATALOG_PREFIX) ->
                    sealing.openCatalog(record)?.let { catalog[record.id] = it } ?: unreadable++
                else -> {
                    // A sealed edit time ahead of this phone's clock is taken as now, so a device set
                    // to next year cannot make its copies win for a year.
                    val opened = sealing.open(record)?.let { it.copy(editedAt = EditOrder.clamped(it.editedAt, now)) }
                    val service = opened?.takeUnless { it.deleted }?.let(Sanitize::service)
                    when {
                        opened == null -> unreadable++
                        opened.deleted && Sanitize.isId(opened.id) ->
                            deletions[opened.id] = opened.copy(version = opened.version.coerceAtLeast(0L))
                        service == null -> rejected++
                        else -> accepted[service.id] = service
                    }
                }
            }
        }
        if (unreadable > 0) Logger.e(TAG, "pull — $unreadable records did not open under this key")
        if (rejected > 0) Logger.e(TAG, "pull — $rejected records were not services this phone keeps")
        // The relay's plaintext tombstones delete no service: nobody signed them. Only the desktop's
        // songbooks still leave that way -- a list of songs, not a plan, and put back on its next push.
        val gone = changes.tombstones.map { it.id }.filter(Sanitize::isId).toSet()
        catalogStore?.merge(catalog, gone.filter { it.startsWith(CATALOG_PREFIX) }.toSet())
        val presets = changes.presetsBox.takeIf { it.isNotEmpty() }
            ?.let { box -> sealing.openPresets(box)?.presets?.let(Sanitize::presets) }
        // Merged copy by copy: a record we just pushed comes back stamped by the relay and replaces
        // our copy of it, and nothing that does not outrank what is here replaces it.
        repository.applySync(
            services = accepted.values.toList(),
            deletions = deletions.values.toList(),
            presets = presets,
        )
        val next = current.copy(cursor = changes.rev, lastSyncAt = nowIso())
        saveState(next)
        val count = accepted.size + deletions.size
        if (!changes.more) return count
        // A full page: the rest is behind it. Stopping here would leave the cursor short, which is safe,
        // but the phone would look up to date until the next round.
        if (page >= MAX_PULL_PAGES) throw RelayFailure.Rejected(0, "relay has more changes than one round will read")
        return count + pull(client, sealing, next, page + 1)
    }
}

/** `YYYY-MM-DD` at the front of an ISO instant. */
private const val DATE_CHARS = 10
