package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.WebsiteItemPayload
import com.church.presenter.churchpresentermobile.network.WsSender
import com.church.presenter.churchpresentermobile.network.WebService
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private const val TAG = "WebViewModel"
private val json = Json { ignoreUnknownKeys = true }

/** A saved web page the user can reuse (matches the design's "Bookmarks" list). */
@Serializable
data class Bookmark(val id: String, val title: String, val url: String)

/** Normalises a user-typed address into a projectable URL (adds https:// if missing). */
fun normalizeUrl(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    return if (trimmed.contains("://")) trimmed else "https://$trimmed"
}

/** Extracts the bare host ("notes.gracefellowship.org") for display. */
fun domainOf(url: String): String =
    url.trim()
        .substringAfter("://", url.trim())
        .substringBefore('/')
        .removePrefix("www.")

class WebViewModel(
    private val appSettings: AppSettings,
    eventService: WsSender,
) : ViewModel() {

    private val service = WebService(eventService)

    private val _url = MutableStateFlow("")
    val url = _url.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private val _bookmarks = MutableStateFlow(loadBookmarks())
    val bookmarks = _bookmarks.asStateFlow()

    /** Full URL of whatever was last projected — drives the "● Live" marker. */
    private val _liveUrl = MutableStateFlow<String?>(null)
    val liveUrl = _liveUrl.asStateFlow()

    private var nextId = 0

    fun setUrl(value: String) { _url.value = value }

    fun clearMessage() { _message.value = null }

    // ── Bookmarks (persisted as JSON in settings) ──────────────────────────

    private fun loadBookmarks(): List<Bookmark> = try {
        json.decodeFromString(ListSerializer(Bookmark.serializer()), appSettings.savedBookmarksJson)
    } catch (_: Exception) { emptyList() }

    private fun persist() {
        appSettings.savedBookmarksJson =
            json.encodeToString(ListSerializer(Bookmark.serializer()), _bookmarks.value)
    }

    /** Saves the current URL as a bookmark (deduped by normalised URL). */
    fun addBookmark() {
        val full = normalizeUrl(_url.value)
        if (full.isBlank()) { _message.value = "Enter a URL first"; return }
        if (_bookmarks.value.any { it.url.equals(full, ignoreCase = true) }) {
            _message.value = "Already bookmarked"; return
        }
        val id = "b${appSettings.deviceId.take(4)}-${nextId++}-${_bookmarks.value.size}"
        _bookmarks.value = _bookmarks.value + Bookmark(id, domainOf(full).ifBlank { full }, full)
        persist()
        _message.value = "Bookmarked"
    }

    /** Loads a bookmark into the URL field. */
    fun loadBookmark(id: String) {
        _bookmarks.value.firstOrNull { it.id == id }?.let { _url.value = it.url }
    }

    fun deleteBookmark(id: String) {
        _bookmarks.value = _bookmarks.value.filterNot { it.id == id }
        persist()
    }

    private fun buildPayload(): WebsiteItemPayload? {
        val full = normalizeUrl(_url.value)
        if (full.isBlank()) { _message.value = "Enter a URL first"; return null }
        return WebsiteItemPayload(
            id = "",
            url = full,
            websiteTitle = domainOf(full).ifBlank { full },
        )
    }

    fun projectPage() {
        val payload = buildPayload() ?: return
        viewModelScope.launch {
            service.projectPage(payload).fold(
                onSuccess = {
                    _liveUrl.value = payload.url
                    _message.value = "Projected"
                },
                onFailure = { e ->
                    Logger.e(TAG, "projectPage failed: ${e.message}", e)
                    _message.value = e.message ?: "Failed to project page"
                }
            )
        }
    }

    fun addToSchedule() {
        val payload = buildPayload() ?: return
        viewModelScope.launch {
            service.addToSchedule(payload).fold(
                onSuccess = { _message.value = "Added to schedule" },
                onFailure = { e ->
                    Logger.e(TAG, "addToSchedule failed: ${e.message}", e)
                    _message.value = e.message ?: "Failed to add to schedule"
                }
            )
        }
    }

    fun clearScreen() {
        viewModelScope.launch {
            service.clearScreen().fold(
                onSuccess = {
                    _liveUrl.value = null
                    _message.value = "Cleared"
                },
                onFailure = { e ->
                    Logger.e(TAG, "clearScreen failed: ${e.message}", e)
                    _message.value = e.message ?: "Failed to clear"
                }
            )
        }
    }
}
