package com.church.presenter.churchpresentermobile.viewmodel

import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.testutil.InMemorySettingsStorage
import kotlin.test.Test
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
}
