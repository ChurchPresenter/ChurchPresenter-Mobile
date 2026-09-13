package com.church.presenter.churchpresentermobile.testutil

import com.church.presenter.churchpresentermobile.network.WsSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Records every [sendAction] call so service action methods (which build a JSON
 * payload and send it over the WebSocket) can be unit-tested without a socket.
 *
 * On the JVM a ViewModel's launch resumes on a Ktor worker thread after its
 * mock HTTP call, so [sendAction] can run on one thread while the test iterates
 * [calls] on another — a plain `MutableList` threw
 * `ConcurrentModificationException` from `payloadsOf` in `PicturesActionsTest`.
 * The log is therefore an immutable snapshot swapped atomically: every read of
 * [calls] is a consistent list, however many threads are appending.
 */
class FakeWsSender(private var result: Result<Unit> = Result.success(Unit)) : WsSender {
    private val _calls = MutableStateFlow<List<Triple<String, String, Boolean>>>(emptyList())

    val calls: List<Triple<String, String, Boolean>> get() = _calls.value
    val lastType: String get() = calls.last().first
    val lastPayload: String get() = calls.last().second

    fun failWith(t: Throwable) { result = Result.failure(t) }

    override suspend fun sendAction(type: String, payloadJson: String, fireAndForget: Boolean): Result<Unit> {
        _calls.update { it + Triple(type, payloadJson, fireAndForget) }
        return result
    }
}
