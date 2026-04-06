package com.church.presenter.churchpresentermobile.network

import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException

expect fun createHttpClient(): HttpClient

/** Bare HTTP client for binary/image fetching — no ContentNegotiation, same SSL bypass. */
expect fun createImageHttpClient(): HttpClient

/**
 * HTTP client for approval-required action endpoints (POST /api/schedule/add,
 * POST /api/project). These hold the connection open until the desktop user
 * clicks Allow/Deny — potentially several minutes — so request and socket
 * timeouts must be disabled. Connect timeout is kept to fail fast when the
 * server is unreachable.
 */
expect fun createActionHttpClient(): HttpClient

/**
 * Like [runCatching] but re-throws [CancellationException] so coroutine
 * cancellation propagates correctly instead of being silently swallowed as
 * a Result.failure and logged as an error.
 */
inline fun <T> apiRunCatching(block: () -> T): Result<T> =
    runCatching(block).also { result ->
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
    }

