package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.tv.FoundService
import com.church.presenter.churchpresentermobile.tv.TvSource
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "TvPickerViewModel"

/**
 * Picking a TV from the phone: tells it what to show, and remembers which one
 * was told, so its row can say so and offer to stop.
 *
 * @param show Tells a TV to show a source.
 * @param clear Tells a TV to go back to its home screen.
 * @param release Frees what the two above use, when this ViewModel is done.
 */
class TvPickerViewModel(
    private val show: suspend (FoundService, TvSource) -> Result<Unit>,
    private val clear: suspend (FoundService) -> Result<Unit>,
    private val release: () -> Unit = {},
) : ViewModel() {

    override fun onCleared() {
        super.onCleared()
        release()
    }

    private val _showingOn = MutableStateFlow<String?>(null)

    /** The name of the TV this phone last sent something to. */
    val showingOn: StateFlow<String?> = _showingOn.asStateFlow()

    private val _failedOn = MutableStateFlow<String?>(null)

    /** The name of a TV that could not be reached, until the next attempt. */
    val failedOn: StateFlow<String?> = _failedOn.asStateFlow()

    /** Sends [source] to [tv]. */
    fun send(tv: FoundService, source: TvSource) {
        _failedOn.value = null
        viewModelScope.launch {
            show(tv, source)
                .onSuccess { _showingOn.value = tv.name }
                .onFailure { e ->
                    Logger.e(TAG, "could not reach ${tv.name}: ${e.message}", e)
                    _failedOn.value = tv.name
                }
        }
    }

    /** Stops showing on [tv]. */
    fun stop(tv: FoundService) {
        _failedOn.value = null
        viewModelScope.launch {
            clear(tv)
                .onSuccess { _showingOn.value = null }
                .onFailure { e ->
                    Logger.e(TAG, "could not reach ${tv.name}: ${e.message}", e)
                    _failedOn.value = tv.name
                }
        }
    }
}
