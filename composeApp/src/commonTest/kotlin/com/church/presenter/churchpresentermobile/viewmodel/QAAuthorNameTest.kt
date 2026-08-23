package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.deviceName
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.QAService
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.runVmTestUnconfined
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Who a question is attributed to. The name is asked for once and remembered;
 * with nothing given it falls back to the device the operator recognises, and
 * only then to the id — a question is never unattributed.
 */
class QAAuthorNameTest {

    /** Captures the JSON body of the POST to qa/add, which is where the author travels. */
    private class Harness {
        val settings = AppSettings(InMemorySettingsStorage())
        private val addBody = CompletableDeferred<String>()

        val viewModel: QAViewModel by lazy {
            val client = HttpClient(MockEngine { request ->
                val path = request.url.encodedPath
                when {
                    path.endsWith("/qa/add") -> {
                        addBody.complete((request.body as TextContent).text)
                        respond(
                            """{"id":"q9","text":"hi","timestamp":5,"status":"PENDING"}""",
                            HttpStatusCode.OK,
                        )
                    }
                    path.endsWith("/status") -> respond("""{"sessionActive":true}""", HttpStatusCode.OK)
                    else -> respond("[]", HttpStatusCode.OK)
                }
            })
            QAViewModel(settings, ServerEventService(settings)) { QAService(it, client) }
        }

        /** Waits until the tab has loaded, so the add POST is the one awaited below. */
        suspend fun ready(): Harness = also { viewModel.uiState.first { it !is QAUiState.Loading } }

        suspend fun add(text: String, name: String = ""): String {
            viewModel.addQuestion(text, name)
            // The POST is fired from viewModelScope; await the body rather than
            // virtual time, which cannot await a MockEngine call on JS.
            return addBody.await()
        }
    }

    private fun authorIn(body: String): String =
        Regex(""""name"\s*:\s*"([^"]*)"""").find(body)?.groupValues?.get(1) ?: ""

    @Test
    fun aNameTypedInTheDialogIsTheAuthor() = runVmTestUnconfined {
        val h = Harness().ready()

        assertEquals("Ada", authorIn(h.add("Why?", " Ada ")))
    }

    @Test
    fun aNameTypedOnceIsRememberedAndNotAskedAgain() = runVmTestUnconfined {
        val h = Harness().ready()
        assertTrue(h.viewModel.needsAuthorName.value)

        h.add("Why?", "Ada")

        assertFalse(h.viewModel.needsAuthorName.value)
        assertEquals("Ada", h.settings.displayName)
    }

    @Test
    fun aSavedNameIsUsedWhenTheDialogAsksForNothing() = runVmTestUnconfined {
        val h = Harness()
        h.settings.displayName = "Ada"
        h.ready()

        assertFalse(h.viewModel.needsAuthorName.value)
        assertEquals("Ada", authorIn(h.add("Why?")))
    }

    @Test
    fun withNoNameAtAllTheDeviceIsCredited() = runVmTestUnconfined {
        // Better a device the operator already approved by name than a raw UUID.
        val h = Harness()
        h.settings.customDeviceName = "Sound desk"
        h.ready()

        assertEquals("Sound desk", authorIn(h.add("Why?")))
    }

    @Test
    fun theIdIsTheLastResortNotABlankAuthor() = runVmTestUnconfined {
        val h = Harness().ready()

        val author = authorIn(h.add("Why?"))
        // deviceName() is blank only on web; elsewhere the OS name stands in.
        assertEquals(deviceName().trim().ifBlank { h.settings.deviceId }, author)
        assertTrue(author.isNotBlank())
    }
}
