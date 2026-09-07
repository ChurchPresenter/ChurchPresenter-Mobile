package com.church.presenter.churchpresentermobile.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private val lenientJson = Json {
    prettyPrint = true
    isLenient = true
    ignoreUnknownKeys = true
}

actual fun createHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(HttpTimeout) {
        requestTimeoutMillis = ApiConstants.REQUEST_TIMEOUT_MS
        connectTimeoutMillis = ApiConstants.CONNECT_TIMEOUT_MS
    }
    install(ContentNegotiation) { json(lenientJson) }
}

actual fun createActionHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) { json(lenientJson) }
}

actual fun createImageHttpClient(): HttpClient = HttpClient(OkHttp)

actual fun createWebSocketClient(): HttpClient = HttpClient(OkHttp) {
    install(WebSockets)
}
