package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.model.AppModeHolder
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.Question
import com.church.presenter.churchpresentermobile.network.QAService
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "QAViewModel"

sealed class QAUiState {
    object Loading : QAUiState()
    data class Admin(
        val sessionActive: Boolean,
        val questions: List<Question>,
        val displayedQuestionId: String,
        val votingEnabled: Boolean = false
    ) : QAUiState()
    data class Error(val message: String) : QAUiState()
}

class QAViewModel(
    private val appSettings: AppSettings,
    private val eventService: ServerEventService,
    private val serviceFactory: (AppSettings) -> QAService = { QAService(it) },
) : ViewModel() {

    private val service = serviceFactory(appSettings)

    private val _uiState = MutableStateFlow<QAUiState>(QAUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError = _actionError.asStateFlow()

    init {
        loadQuestions()
        viewModelScope.launch {
            eventService.questionsUpdated.collect {
                // Reload regardless of current state so the tab self-heals from a
                // transient error (e.g. a 401 during the brief window before the
                // API key was saved). silent = true avoids flashing the spinner.
                loadQuestions(silent = true)
            }
        }
    }

    /**
     * Called by the Q&A screen after the user saves Settings (host/port/API key).
     * Re-fetches so a newly-entered API key takes effect immediately — without it
     * the tab would keep showing the pre-key 401 until the app restarts.
     */
    fun onSettingsSaved() {
        loadQuestions()
    }

    fun loadQuestions(silent: Boolean = false) {
        // Standalone has no desktop to ask, and this screen is not reachable
        // there — loading would only time out against an absent computer.
        if (!AppModeHolder.hasDesktop) return

        if (!silent) _uiState.value = QAUiState.Loading
        viewModelScope.launch {
            val statusResult = service.fetchStatus()
            val questionsResult = service.fetchQuestions()
            if (questionsResult.isSuccess && statusResult.isSuccess) {
                val status = statusResult.getOrNull()!!
                _uiState.value = QAUiState.Admin(
                    sessionActive = status.sessionActive,
                    questions = questionsResult.getOrNull()!!,
                    displayedQuestionId = status.displayedQuestionId,
                    votingEnabled = status.votingEnabled
                )
            } else if (!silent) {
                val err = questionsResult.exceptionOrNull() ?: statusResult.exceptionOrNull()
                _uiState.value = QAUiState.Error(err?.message ?: "Failed to load questions")
            }
        }
    }

    fun approveQuestion(id: String) = runAdminAction { service.approveQuestion(id) }
    fun denyQuestion(id: String) = runAdminAction { service.denyQuestion(id) }
    fun editQuestion(id: String, newText: String) = runAdminAction { service.editQuestion(id, newText) }
    fun markDone(id: String) = runAdminAction { service.markDone(id) }
    fun displayQuestion(id: String) = runAdminAction { service.displayQuestion(id) }
    fun deleteQuestion(id: String) = runAdminAction { service.deleteQuestion(id) }
    fun addQuestion(text: String) = runAdminAction {
        // Same name the desktop was given at approval, so a question arrives from
        // the device the operator already recognises rather than from a UUID.
        val name = appSettings.reportedDeviceName.ifBlank { appSettings.deviceId }
        service.addQuestion(text, name).map { }
    }
    fun clearDisplay() = runAdminAction { service.clearDisplay() }

    fun approveAndDisplay(id: String) {
        viewModelScope.launch {
            val approveResult = service.approveQuestion(id)
            if (approveResult.isFailure) {
                _actionError.value = approveResult.exceptionOrNull()?.message ?: "Failed to approve"
                return@launch
            }
            val displayResult = service.displayQuestion(id)
            displayResult.fold(
                onSuccess = { loadQuestions(silent = true) },
                onFailure = { e ->
                    Logger.e(TAG, "approveAndDisplay failed: ${e.message}", e)
                    _actionError.value = e.message ?: "Failed to go live"
                    loadQuestions(silent = true)
                }
            )
        }
    }

    fun clearActionError() { _actionError.value = null }

    private fun runAdminAction(action: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            action().fold(
                onSuccess = { loadQuestions(silent = true) },
                onFailure = { e ->
                    Logger.e(TAG, "Admin action failed: ${e.message}", e)
                    _actionError.value = e.message ?: "Action failed"
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        service.closeClient()
    }
}
