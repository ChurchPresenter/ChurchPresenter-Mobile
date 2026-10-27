package com.church.presenter.churchpresentermobile.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import kotlin.time.Duration.Companion.seconds
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private val lenientJson = Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true }

actual fun createHttpClient(): HttpClient = HttpClient(Darwin) {
    install(HttpTimeout) {
        requestTimeoutMillis = ApiConstants.REQUEST_TIMEOUT_MS
        connectTimeoutMillis = ApiConstants.CONNECT_TIMEOUT_MS
        socketTimeoutMillis = ApiConstants.SOCKET_TIMEOUT_MS
    }
    install(ContentNegotiation) { json(lenientJson) }
}

actual fun createActionHttpClient(): HttpClient = HttpClient(Darwin) {
    install(HttpTimeout) {
        connectTimeoutMillis = ApiConstants.CONNECT_TIMEOUT_MS
        requestTimeoutMillis = null  // no timeout — waits for user Allow/Deny
        socketTimeoutMillis  = null
    }
    install(ContentNegotiation) { json(lenientJson) }
}

actual fun createImageHttpClient(): HttpClient = HttpClient(Darwin) {
    install(HttpTimeout) {
        // Thumbnails are generated on-demand by the desktop and can be slow under a
        // burst of grid requests, so give them generous timeouts.
        requestTimeoutMillis = ApiConstants.IMAGE_REQUEST_TIMEOUT_MS
        connectTimeoutMillis = ApiConstants.IMAGE_CONNECT_TIMEOUT_MS
        socketTimeoutMillis = ApiConstants.IMAGE_SOCKET_TIMEOUT_MS
    }
    engine {
        // NSURLSession defaults to ~6 connections per host, which starves a photo/
        // slide grid loading many thumbnails from the same server at once.
        configureSession {
            setHTTPMaximumConnectionsPerHost(ApiConstants.IMAGE_MAX_CONNECTIONS_PER_HOST.toLong())
        }
    }
    // No ContentNegotiation — lets Coil read raw image bytes without interference
}

actual fun createWebSocketClient(): HttpClient = HttpClient(Darwin) {
    install(HttpTimeout) {
        connectTimeoutMillis = ApiConstants.WS_CONNECT_TIMEOUT_MS
    }
    install(WebSockets) {
        // pingInterval is required for Ktor's Darwin (NSURLSessionWebSocketTask) engine —
        // without it the iOS WebSocket handshake can fail with NSURLErrorBadServerResponse (-1011).
        pingInterval = 20.seconds
        // NSURLSessionWebSocketTask refuses any message over 1 MB by default and drops
        // the socket with "Message too long". The desktop's songs_updated carries the
        // whole catalog — 857 KB of mostly Cyrillic text, about 1.5 MB on the wire — so
        // every connect died seconds later, and actions failed or were sent twice.
        maxFrameSize = WS_MAX_MESSAGE_BYTES
    }
}

/** The largest message the iOS socket accepts: room for a catalog many times today's. */
private const val WS_MAX_MESSAGE_BYTES = 32L * 1024 * 1024

