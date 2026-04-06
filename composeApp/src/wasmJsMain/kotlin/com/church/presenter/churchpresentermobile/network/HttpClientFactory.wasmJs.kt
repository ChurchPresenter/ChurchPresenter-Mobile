package com.church.presenter.churchpresentermobile.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

actual fun createHttpClient(): HttpClient {
    return HttpClient {
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

