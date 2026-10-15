package com.church.presenter.churchpresentermobile.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** OkHttp's overall in-flight cap, raised along with the per-host one. */
private const val MAX_IMAGE_REQUESTS = 64

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
        requestTimeoutMillis = ApiConstants.IMAGE_REQUEST_TIMEOUT_MS
        connectTimeoutMillis = ApiConstants.IMAGE_CONNECT_TIMEOUT_MS
        socketTimeoutMillis = ApiConstants.IMAGE_SOCKET_TIMEOUT_MS
    }
    engine {
        // OkHttp defaults to maxRequestsPerHost = 5, which starves a photo/slide
        // grid that loads many thumbnails from the same server at once (queued
        // requests then time out). Raise the per-host cap so the grid fills in.
        config {
            dispatcher(okhttp3.Dispatcher().apply {
                maxRequests = MAX_IMAGE_REQUESTS
                maxRequestsPerHost = ApiConstants.IMAGE_MAX_CONNECTIONS_PER_HOST
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

