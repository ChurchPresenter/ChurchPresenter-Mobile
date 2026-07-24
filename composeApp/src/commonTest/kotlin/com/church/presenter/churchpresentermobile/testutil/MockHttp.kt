package com.church.presenter.churchpresentermobile.testutil

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpResponseData

/**
 * Builds an [HttpClient] backed by a [MockEngine] whose responses are decided by
 * [handler], keyed on the request's encoded path (e.g. "/api/songs"). Use `respond(...)`
 * inside the handler; throw to simulate a connectivity failure.
 */
fun mockClient(handler: MockRequestHandleScope.(path: String) -> HttpResponseData): HttpClient =
    HttpClient(MockEngine { request -> handler(request.url.encodedPath) })
