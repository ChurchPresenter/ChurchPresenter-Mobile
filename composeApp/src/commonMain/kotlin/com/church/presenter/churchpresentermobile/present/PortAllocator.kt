package com.church.presenter.churchpresentermobile.present

/**
 * Picks the port the presentation server should listen on.
 *
 * Pure, with binding injected as a lambda, so the awkward cases — preferred
 * port busy, whole range busy, a candidate that throws — are unit-testable
 * without opening a socket.
 */
object PortAllocator {

    /**
     * Returns the first port that [canBind] accepts: [preferred] first, then
     * each of [candidates] in order. Returns `null` when none is free, which
     * the caller should treat as "fall back to an ephemeral port" rather than
     * as a failure — an unusual port still works, it just isn't memorable.
     *
     * [canBind] is allowed to throw; a throwing probe is treated as "busy",
     * since a port we cannot test is a port we should not claim.
     */
    fun choose(
        preferred: Int,
        candidates: Iterable<Int>,
        canBind: (Int) -> Boolean,
    ): Int? {
        val ordered = buildList {
            if (preferred in VALID_PORTS) add(preferred)
            candidates.forEach { if (it in VALID_PORTS && it != preferred) add(it) }
        }
        return ordered.firstOrNull { port -> runCatching { canBind(port) }.getOrDefault(false) }
    }

    /** Ports a user application may bind without privileges. */
    val VALID_PORTS: IntRange = 1024..65535
}
