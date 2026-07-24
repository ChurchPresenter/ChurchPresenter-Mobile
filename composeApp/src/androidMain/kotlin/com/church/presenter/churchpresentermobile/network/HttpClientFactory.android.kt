package com.church.presenter.churchpresentermobile.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private val lenientJson = Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true }

actual fun createHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(HttpTimeout) {
        requestTimeoutMillis = ApiConstants.REQUEST_TIMEOUT_MS
        connectTimeoutMillis = ApiConstants.CONNECT_TIMEOUT_MS
        socketTimeoutMillis = ApiConstants.SOCKET_TIMEOUT_MS
    }
    install(ContentNegotiation) { json(lenientJson) }
}

actual fun createActionHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(HttpTimeout) {
        connectTimeoutMillis = ApiConstants.CONNECT_TIMEOUT_MS
        requestTimeoutMillis = null  // no timeout — waits for user Allow/Deny
        socketTimeoutMillis  = null
    }
    install(ContentNegotiation) { json(lenientJson) }
}

actual fun createImageHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(HttpTimeout) {
        // Thumbnails are generated on-demand by the desktop and can be slow under a
        // burst of grid requests, so give them generous timeouts.
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 30_000
    }
    engine {
        // OkHttp defaults to maxRequestsPerHost = 5, which starves a photo/slide
        // grid that loads many thumbnails from the same server at once (queued
        // requests then time out). Raise the per-host cap so the grid fills in.
        config {
            dispatcher(okhttp3.Dispatcher().apply {
                maxRequests = 64
                maxRequestsPerHost = 12
            })
        }
    }
    // No ContentNegotiation — lets Coil read raw image bytes without interference
}

actual fun createWebSocketClient(): HttpClient = HttpClient(OkHttp) {
    install(HttpTimeout) {
        // Bound the WS connect so a reconnect attempt against an unreachable
        // server fails fast instead of blocking ~10s on OkHttp's default.
        connectTimeoutMillis = ApiConstants.WS_CONNECT_TIMEOUT_MS
    }
    install(WebSockets)
}

