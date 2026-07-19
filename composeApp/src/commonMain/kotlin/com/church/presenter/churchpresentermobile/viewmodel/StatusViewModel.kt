package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.ServerStatus
import com.church.presenter.churchpresentermobile.model.StatusWarning
import com.church.presenter.churchpresentermobile.model.deriveWarnings
import com.church.presenter.churchpresentermobile.network.StatusService
import com.church.presenter.churchpresentermobile.network.recordNetworkError
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "StatusViewModel"

/** UI state for the startup status / compatibility screen. */
sealed class StatusUiState {
    data object Loading : StatusUiState()
    /** Status fetched successfully. [warnings] may be empty (all good) or contain issues. */
    data class Success(
        val status: ServerStatus,
        val warnings: List<StatusWarning>,
    ) : StatusUiState()
    /** Network/parse error — could not reach the server at all. */
    data class Error(val message: String) : StatusUiState()
}

/**
 * Fetches GET /api/status on creation and exposes the result as [uiState].
 *
 * Callers should pass [settingsSaveToken] when the user updates server settings
 * so that a re-check is triggered automatically via [recheck].
 */
class StatusViewModel(private val appSettings: AppSettings) : ViewModel() {

    private val _uiState = MutableStateFlow<StatusUiState>(StatusUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var service = StatusService(appSettings)

    init {
        fetchStatus()
    }

    /** Re-creates the service (picks up new host/port) and retries the status call. */
    fun recheck() {
        service.closeClient()
        service = StatusService(appSettings)
        fetchStatus()
    }

    /**
     * Re-fetches status without flipping the UI back to [StatusUiState.Loading]. Used when the
     * user revisits a screen whose gating depends on live server permissions (e.g. the Media tab,
     * where the desktop operator may have toggled file uploads since the last fetch).
     */
    fun refreshQuietly() {
        viewModelScope.launch {
            service.fetchStatus().onSuccess { status ->
                _uiState.value = StatusUiState.Success(status, status.deriveWarnings())
            }
            // On failure keep the existing state — a transient blip shouldn't blank the UI.
        }
    }

    private fun fetchStatus() {
        _uiState.value = StatusUiState.Loading
        viewModelScope.launch {
            service.fetchStatus()
                .onSuccess { status ->
                    val warnings = status.deriveWarnings()
                    Logger.d(TAG, "fetchStatus ✓ — warnings: ${warnings.map { it::class.simpleName }}")
                    _uiState.value = StatusUiState.Success(status, warnings)
                }
                .onFailure { e ->
                    val friendly = e.recordNetworkError(TAG, "fetchStatus")
                    Logger.e(TAG, "fetchStatus ✗ — ${e.message}", e)
                    _uiState.value = StatusUiState.Error(friendly)
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        service.closeClient()
    }
}

