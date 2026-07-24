package com.church.presenter.churchpresentermobile.testutil

import com.church.presenter.churchpresentermobile.network.WsSender

/**
 * Records every [sendAction] call so service action methods (which build a JSON
 * payload and send it over the WebSocket) can be unit-tested without a socket.
 */
class FakeWsSender(private var result: Result<Unit> = Result.success(Unit)) : WsSender {
    val calls = mutableListOf<Triple<String, String, Boolean>>()
    val lastType: String get() = calls.last().first
    val lastPayload: String get() = calls.last().second

    fun failWith(t: Throwable) { result = Result.failure(t) }

    override suspend fun sendAction(type: String, payloadJson: String, fireAndForget: Boolean): Result<Unit> {
        calls += Triple(type, payloadJson, fireAndForget)
        return result
    }
}
