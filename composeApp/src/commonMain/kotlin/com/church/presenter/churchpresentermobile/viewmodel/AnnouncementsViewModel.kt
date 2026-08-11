package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.model.AnnouncementAnimation
import com.church.presenter.churchpresentermobile.model.AnnouncementItemPayload
import com.church.presenter.churchpresentermobile.model.AnnouncementType
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.ScheduleItem
import com.church.presenter.churchpresentermobile.network.AnnouncementService
import com.church.presenter.churchpresentermobile.network.WsSender
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.coroutines.launch

private const val TAG = "AnnouncementsViewModel"
private val json = Json { ignoreUnknownKeys = true }

/** Immutable snapshot of the announcement composer form. */
@Serializable
data class AnnouncementForm(
    val type: AnnouncementType = AnnouncementType.TEXT,
    val text: String = "",
    val textColor: String = "#FFFFFF",
    val backgroundColor: String = "#000000",
    val fontSize: Int = 48,
    val animation: AnnouncementAnimation = AnnouncementAnimation.SLIDE_BOTTOM,
    val animationDuration: Int = 5000,
    val hours: Int = 0,
    val minutes: Int = 5,
    val seconds: Int = 0,
    val targetHour: Int = 12,
    val targetMinute: Int = 0,
    val expiredText: String = "",
)

/** A persisted announcement the user can reuse. */
@Serializable
data class SavedAnnouncement(val id: String, val form: AnnouncementForm) {
    /** Short label for the saved list row. */
    val label: String
        get() = when (form.type) {
            AnnouncementType.TEXT -> form.text.ifBlank { "Untitled" }
            else -> "${form.type.label}"
        }
}

class AnnouncementsViewModel(
    private val appSettings: AppSettings,
    eventService: WsSender,
) : ViewModel() {

    private val service = AnnouncementService(eventService)

    private val _form = MutableStateFlow(AnnouncementForm())
    val form = _form.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private val _saved = MutableStateFlow<List<SavedAnnouncement>>(loadSaved())
    val saved = _saved.asStateFlow()

    private var nextId = 0

    fun update(transform: (AnnouncementForm) -> AnnouncementForm) {
        _form.value = transform(_form.value)
    }

    /** Reconstructs the composer from a schedule item (used when tapped in the schedule drawer). */
    fun preload(item: ScheduleItem) {
        val label = item.displayText ?: ""
        // Prefer the structured isTimer/timerMode fields (newer desktop). Fall back to the
        // display label, which every desktop version sends (e.g. "Timer 05:00", "Clock", "Until 12:00:00").
        val type = when {
            item.isTimer == true -> when (item.timerMode) {
                "count_up" -> AnnouncementType.COUNT_UP
                "clock_display" -> AnnouncementType.CLOCK
                "clock" -> AnnouncementType.COUNTDOWN_TO_TIME
                else -> AnnouncementType.COUNTDOWN
            }
            item.isTimer == false -> AnnouncementType.TEXT
            label.startsWith("Until ") -> AnnouncementType.COUNTDOWN_TO_TIME
            label.equals("Duration Timer", ignoreCase = true) -> AnnouncementType.COUNT_UP
            label.equals("Clock", ignoreCase = true) -> AnnouncementType.CLOCK
            label.startsWith("Timer ") -> AnnouncementType.COUNTDOWN
            else -> AnnouncementType.TEXT
        }
        // Numbers embedded in the label — used to recover timer values when the structured
        // timer fields aren't provided by an older desktop.
        val nums = Regex("\\d+").findAll(label).map { it.value.toIntOrNull() ?: 0 }.toList()
        val hours = item.timerHours
            ?: if (type == AnnouncementType.COUNTDOWN && nums.size >= 3) nums[0] else 0
        val minutes = item.timerMinutes
            ?: when {
                type == AnnouncementType.COUNTDOWN && nums.size >= 3 -> nums[1]
                type == AnnouncementType.COUNTDOWN && nums.size == 2 -> nums[0]
                else -> 5
            }
        val seconds = item.timerSeconds
            ?: when {
                type == AnnouncementType.COUNTDOWN && nums.size >= 3 -> nums[2]
                type == AnnouncementType.COUNTDOWN && nums.size == 2 -> nums[1]
                else -> 0
            }
        val targetHour = item.targetHour
            ?: if (type == AnnouncementType.COUNTDOWN_TO_TIME && nums.isNotEmpty()) nums[0] else 12
        val targetMinute = item.targetMinute
            ?: if (type == AnnouncementType.COUNTDOWN_TO_TIME && nums.size >= 2) nums[1] else 0
        val animation = AnnouncementAnimation.entries.firstOrNull { it.value == item.animationType }
            ?: AnnouncementAnimation.SLIDE_BOTTOM
        _form.value = AnnouncementForm(
            type = type,
            text = if (type == AnnouncementType.TEXT) (item.text ?: label) else (item.text ?: ""),
            textColor = item.textColor ?: "#FFFFFF",
            backgroundColor = item.backgroundColor ?: "#000000",
            fontSize = item.fontSize ?: 48,
            animation = animation,
            animationDuration = item.animationDuration ?: 5000,
            hours = hours,
            minutes = minutes,
            seconds = seconds,
            targetHour = targetHour,
            targetMinute = targetMinute,
            expiredText = item.timerExpiredText ?: "",
        )
    }

    fun clearMessage() { _message.value = null }

    // ── Saved announcements (persisted as JSON in settings) ────────────────

    private fun loadSaved(): List<SavedAnnouncement> = try {
        json.decodeFromString(ListSerializer(SavedAnnouncement.serializer()), appSettings.savedAnnouncementsJson)
    } catch (_: Exception) { emptyList() }

    private fun persist() {
        appSettings.savedAnnouncementsJson =
            json.encodeToString(ListSerializer(SavedAnnouncement.serializer()), _saved.value)
    }

    /** Saves the current composer as a new reusable announcement. */
    fun saveCurrent() {
        val id = "s${appSettings.deviceId.take(4)}-${nextId++}-${_saved.value.size}"
        _saved.value = _saved.value + SavedAnnouncement(id, _form.value)
        persist()
        _message.value = "Saved"
    }

    /** Loads a saved announcement back into the composer. */
    fun loadSaved(id: String) {
        _saved.value.firstOrNull { it.id == id }?.let { _form.value = it.form }
    }

    fun deleteSaved(id: String) {
        _saved.value = _saved.value.filterNot { it.id == id }
        persist()
    }

    private fun buildPayload(): AnnouncementItemPayload {
        val f = _form.value
        return AnnouncementItemPayload(
            id = "", // server assigns a UUID when blank
            announcementText = if (f.type == AnnouncementType.TEXT) f.text else "",
            textColor = f.textColor,
            backgroundColor = f.backgroundColor,
            fontSize = f.fontSize,
            animationType = f.animation.value,
            animationDuration = f.animationDuration,
            isTimer = f.type.isTimer,
            timerHours = if (f.type == AnnouncementType.COUNTDOWN) f.hours else 0,
            timerMinutes = if (f.type == AnnouncementType.COUNTDOWN) f.minutes else 0,
            timerSeconds = if (f.type == AnnouncementType.COUNTDOWN) f.seconds else 0,
            timerMode = f.type.timerMode,
            targetHour = if (f.type == AnnouncementType.COUNTDOWN_TO_TIME) f.targetHour else 0,
            targetMinute = if (f.type == AnnouncementType.COUNTDOWN_TO_TIME) f.targetMinute else 0,
            targetSecond = 0,
            timerTextColor = f.textColor,
            timerExpiredText = f.expiredText,
        )
    }

    fun addToSchedule() {
        val payload = buildPayload()
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

    fun showOnScreen() {
        val payload = buildPayload()
        viewModelScope.launch {
            service.showOnScreen(payload).fold(
                onSuccess = { _message.value = "Sent to screen" },
                onFailure = { e ->
                    Logger.e(TAG, "showOnScreen failed: ${e.message}", e)
                    _message.value = e.message ?: "Failed to show on screen"
                }
            )
        }
    }

    /** Clears the announcement from the desktop screen. */
    fun clearScreen() {
        viewModelScope.launch {
            service.clearScreen().fold(
                onSuccess = { _message.value = "Cleared" },
                onFailure = { e ->
                    Logger.e(TAG, "clearScreen failed: ${e.message}", e)
                    _message.value = e.message ?: "Failed to clear"
                }
            )
        }
    }
}
