package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import com.church.presenter.churchpresentermobile.network.WsMessageType
import com.church.presenter.churchpresentermobile.testutil.FakeWsSender
import com.church.presenter.churchpresentermobile.testutil.runVmTestUnconfined
import com.church.presenter.churchpresentermobile.testutil.tearDown
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests [WebViewModel] bookmark management (normalize + dedup + JSON persistence). */
class WebViewModelTest {

    private fun vmWith(storage: InMemorySettingsStorage = InMemorySettingsStorage()): Pair<WebViewModel, AppSettings> {
        val settings = AppSettings(storage)
        return WebViewModel(settings, ServerEventService(settings)) to settings
    }

    @Test
    fun addBookmarkRequiresUrl() {
        val (vm, _) = vmWith()
        vm.setUrl("   ")
        vm.addBookmark()
        assertEquals("Enter a URL first", vm.message.value)
        assertTrue(vm.bookmarks.value.isEmpty())
    }

    @Test
    fun addBookmarkNormalizesAndStoresDomainTitle() {
        val (vm, _) = vmWith()
        vm.setUrl("example.com/page")
        vm.addBookmark()
        assertEquals(1, vm.bookmarks.value.size)
        assertEquals("https://example.com/page", vm.bookmarks.value[0].url)
        assertEquals("example.com", vm.bookmarks.value[0].title)
        assertEquals("Bookmarked", vm.message.value)
    }

    @Test
    fun addBookmarkDedupesByNormalizedUrl() {
        val (vm, _) = vmWith()
        vm.setUrl("example.com")
        vm.addBookmark()
        vm.setUrl("https://example.com") // normalizes to the same URL
        vm.addBookmark()
        assertEquals(1, vm.bookmarks.value.size)
        assertEquals("Already bookmarked", vm.message.value)
    }

    @Test
    fun bookmarksPersistAcrossReload() {
        val storage = InMemorySettingsStorage()
        val (vm, _) = vmWith(storage)
        vm.setUrl("grace.org")
        vm.addBookmark()
        val (vm2, _) = vmWith(storage)
        assertEquals(1, vm2.bookmarks.value.size)
        assertEquals("https://grace.org", vm2.bookmarks.value[0].url)
    }

    @Test
    fun loadBookmarkPopulatesUrlAndDeleteRemoves() {
        val storage = InMemorySettingsStorage()
        val (vm, _) = vmWith(storage)
        vm.setUrl("grace.org")
        vm.addBookmark()
        val id = vm.bookmarks.value[0].id

        vm.setUrl("something-else.com")
        vm.loadBookmark(id)
        assertEquals("https://grace.org", vm.url.value)

        vm.deleteBookmark(id)
        assertTrue(vm.bookmarks.value.isEmpty())
        assertTrue(vmWith(storage).first.bookmarks.value.isEmpty())
    }

    // ── The payload sent to the desktop ──────────────────────────────────

    private fun sendingVm(): Pair<WebViewModel, FakeWsSender> {
        val ws = FakeWsSender()
        return WebViewModel(AppSettings(InMemorySettingsStorage()), ws) to ws
    }

    @Test
    fun `projecting a page sends it down the project path`() = runVmTestUnconfined {
        val (vm, ws) = sendingVm()
        try {
            vm.setUrl("https://notes.example.org/sunday")

            vm.projectPage()

            assertEquals(WsMessageType.PROJECT, ws.lastType)
            assertTrue(ws.lastPayload.contains("\"url\":\"https://notes.example.org/sunday\""), ws.lastPayload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `the page is titled by its domain`() = runVmTestUnconfined {
        // The schedule row shows this; a full URL there is unreadable.
        val (vm, ws) = sendingVm()
        try {
            vm.setUrl("https://www.notes.example.org/sunday/week-one")

            vm.projectPage()

            assertTrue(ws.lastPayload.contains("\"websiteTitle\":\"notes.example.org\""), ws.lastPayload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a bare domain is sent as https`() = runVmTestUnconfined {
        val (vm, ws) = sendingVm()
        try {
            vm.setUrl("notes.example.org")

            vm.projectPage()

            assertTrue(ws.lastPayload.contains("\"url\":\"https://notes.example.org\""), ws.lastPayload)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `adding to schedule sends the same item down the schedule path`() = runVmTestUnconfined {
        val (vm, ws) = sendingVm()
        try {
            vm.setUrl("https://notes.example.org")

            vm.addToSchedule()

            assertEquals(WsMessageType.ADD_TO_SCHEDULE, ws.lastType)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `projecting records what is live`() = runVmTestUnconfined {
        val (vm, _) = sendingVm()
        try {
            vm.setUrl("https://notes.example.org")

            vm.projectPage()

            assertEquals("https://notes.example.org", vm.liveUrl.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `an empty url is refused with a message rather than sent`() = runVmTestUnconfined {
        val (vm, ws) = sendingVm()
        try {
            vm.projectPage()

            assertTrue(ws.calls.isEmpty())
            assertEquals("Enter a URL first", vm.message.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a whitespace-only url counts as empty`() = runVmTestUnconfined {
        val (vm, ws) = sendingVm()
        try {
            vm.setUrl("   ")

            vm.addToSchedule()

            assertTrue(ws.calls.isEmpty())
            assertEquals("Enter a URL first", vm.message.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `clearing the screen sends clear and forgets what was live`() = runVmTestUnconfined {
        val (vm, ws) = sendingVm()
        try {
            vm.setUrl("https://notes.example.org")
            vm.projectPage()

            vm.clearScreen()

            assertEquals(WsMessageType.CLEAR, ws.lastType)
            assertNull(vm.liveUrl.value)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a consumed message is cleared so it shows once`() = runVmTestUnconfined {
        val (vm, _) = sendingVm()
        try {
            vm.projectPage()
            assertNotNull(vm.message.value)

            vm.clearMessage()

            assertNull(vm.message.value)
        } finally {
            tearDown(vm)
        }
    }

    // ── When the desktop refuses ─────────────────────────────────────────
    //
    // Each action has a success and a failure half; only the success half was
    // exercised. A silent failure here leaves the operator believing a page is
    // on the wall when it is not.

    private fun failingVm(error: Throwable): Pair<WebViewModel, FakeWsSender> {
        val ws = FakeWsSender()
        ws.failWith(error)
        return WebViewModel(AppSettings(InMemorySettingsStorage()), ws) to ws
    }

    @Test
    fun `a failed projection is reported and nothing is recorded live`() = runVmTestUnconfined {
        val (vm, _) = failingVm(IllegalStateException("Connection refused"))
        try {
            vm.setUrl("https://notes.example.org")

            vm.projectPage()
            val message = vm.message.first { it != null }

            assertEquals("Connection refused", message)
            assertNull(vm.liveUrl.value, "nothing went live, so nothing may be shown as live")
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a failure with no message still says something`() = runVmTestUnconfined {
        val (vm, _) = failingVm(IllegalStateException())
        try {
            vm.setUrl("https://notes.example.org")

            vm.projectPage()
            val message = vm.message.first { it != null }

            assertNotNull(message)
            assertTrue(message!!.isNotBlank())
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a failed add to schedule is reported`() = runVmTestUnconfined {
        val (vm, _) = failingVm(IllegalStateException("denied"))
        try {
            vm.setUrl("https://notes.example.org")

            vm.addToSchedule()
            val message = vm.message.first { it != null }

            assertEquals("denied", message)
        } finally {
            tearDown(vm)
        }
    }

    @Test
    fun `a failed clear leaves the live url alone`() = runVmTestUnconfined {
        // The page is still up on the desktop, so forgetting it here would leave
        // the operator with no way to try clearing it again.
        val ws = FakeWsSender()
        val vm = WebViewModel(AppSettings(InMemorySettingsStorage()), ws)
        try {
            vm.setUrl("https://notes.example.org")
            vm.projectPage()
            vm.liveUrl.first { it != null }

            ws.failWith(IllegalStateException("socket closed"))
            vm.clearScreen()
            vm.message.first { it == "socket closed" }

            assertNotNull(vm.liveUrl.value, "the page is still on the desktop")
        } finally {
            tearDown(vm)
        }
    }
}
