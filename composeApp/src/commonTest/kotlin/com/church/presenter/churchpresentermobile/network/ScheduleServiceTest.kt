package com.church.presenter.churchpresentermobile.network

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.testutil.mockClient
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests [ScheduleService.getSchedule] — the array-or-wrapper-object polymorphic decode. */
class ScheduleServiceTest {

    private fun service(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        ScheduleService(AppSettings(InMemorySettingsStorage()), mockClient { respond(body, status) })

    @Test
    fun parsesBareJsonArray() = runTest {
        val svc = service("""[{"id":"a","type":"song","displayText":"Amazing"}]""")
        val items = svc.getSchedule().getOrThrow()
        assertEquals(1, items.size)
        assertEquals("a", items[0].id)
        assertEquals("Amazing", items[0].displayTitle)
    }

    @Test
    fun parsesItemsWrapperObject() = runTest {
        val svc = service("""{"items":[{"id":"a"},{"id":"b"}],"total":2}""")
        assertEquals(listOf("a", "b"), svc.getSchedule().getOrThrow().map { it.id })
    }

    @Test
    fun parsesScheduleWrapperObject() = runTest {
        val svc = service("""{"schedule":[{"id":"z"}]}""")
        assertEquals(listOf("z"), svc.getSchedule().getOrThrow().map { it.id })
    }

    @Test
    fun emptyWrapperYieldsEmptyList() = runTest {
        val svc = service("""{"total":0}""")
        assertTrue(svc.getSchedule().getOrThrow().isEmpty())
    }

    @Test
    fun nonSuccessStatusIsFailure() = runTest {
        val svc = service("nope", HttpStatusCode.InternalServerError)
        assertTrue(svc.getSchedule().isFailure)
    }
}
