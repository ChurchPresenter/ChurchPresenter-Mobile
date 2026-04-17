package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.DemoData
import com.church.presenter.churchpresentermobile.model.ScheduleItem
import com.church.presenter.churchpresentermobile.network.ScheduleService
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.network.recordNetworkError
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "ScheduleViewModel"

/**
 * Manages loading and state for the service schedule drawer.
 *
 * @param appSettings The shared [AppSettings] used to configure the API service.
 * @param isDemoMode  When true, demo content from [DemoData] is used instead of live API calls.
 */
class ScheduleViewModel(private val appSettings: AppSettings, private val isDemoMode: Boolean = false) : ViewModel() {
    private var scheduleService = ScheduleService(appSettings)
    private var eventService = ServerEventService(appSettings)

    private val _items = MutableStateFlow<List<ScheduleItem>>(emptyList())
    val items = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        loadSchedule()
        if (!isDemoMode) {
            // Start persistent WS listener — reloads the schedule whenever the
            // server pushes a schedule_updated event (e.g. item added/approved).
            viewModelScope.launch {
                eventService.scheduleUpdated.collect {
                    Logger.d(TAG, "scheduleUpdated event received — reloading schedule")
                    loadSchedule()
                }
            }
            viewModelScope.launch { eventService.listen() }
        }
    }

    /** Loads (or reloads) the schedule from the API, or from [DemoData] in demo mode. */
    fun loadSchedule() {
        if (isDemoMode) {
            _items.value = DemoData.scheduleItems
            Logger.d(TAG, "loadSchedule — demo mode: loaded ${DemoData.scheduleItems.size} items")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            scheduleService.getSchedule()
                .onSuccess { items ->
                    _items.value = items
                    Logger.d(TAG, "loadSchedule — loaded ${items.size} items")
                }
                .onFailure { e ->
                    _error.value = e.recordNetworkError(TAG, "loadSchedule")
                    Logger.e(TAG, "loadSchedule — error: ${e.message}", e)
                }
            _isLoading.value = false
        }
    }

    /**
     * Called when the user saves new settings.
     * Rebuilds the service and reloads data against the new server.
     */
    fun onSettingsSaved() {
        if (isDemoMode) return
        Logger.d(TAG, "onSettingsSaved — rebuilding service and reloading")
        scheduleService.closeClient()
        scheduleService = ScheduleService(appSettings)
        eventService.closeClient()
        eventService = ServerEventService(appSettings)
        viewModelScope.launch { eventService.listen() }
        viewModelScope.launch {
            eventService.scheduleUpdated.collect {
                Logger.d(TAG, "scheduleUpdated event received — reloading schedule")
                loadSchedule()
            }
        }
        loadSchedule()
    }

    override fun onCleared() {
        super.onCleared()
        scheduleService.closeClient()
        eventService.closeClient()
    }
}
