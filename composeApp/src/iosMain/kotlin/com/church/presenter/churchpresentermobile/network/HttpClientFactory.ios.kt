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
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 30_000
    }
    engine {
        // NSURLSession defaults to ~6 connections per host, which starves a photo/
        // slide grid loading many thumbnails from the same server at once.
        configureSession {
            setHTTPMaximumConnectionsPerHost(12)
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
    }
}

