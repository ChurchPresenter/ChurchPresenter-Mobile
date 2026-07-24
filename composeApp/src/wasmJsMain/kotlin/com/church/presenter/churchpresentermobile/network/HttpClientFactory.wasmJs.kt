package com.church.presenter.churchpresentermobile.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

actual fun createHttpClient(): HttpClient {
    return HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = ApiConstants.REQUEST_TIMEOUT_MS
            connectTimeoutMillis = ApiConstants.CONNECT_TIMEOUT_MS
        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }
}

actual fun createActionHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true })
    }
}

actual fun createImageHttpClient(): HttpClient = HttpClient()

actual fun createWebSocketClient(): HttpClient = HttpClient {
    install(WebSockets)
}

