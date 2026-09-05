package com.church.presenter.churchpresentermobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.InstalledBible
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private const val TAG = "BibleChoiceViewModel"

/**
 * Which downloaded translation is read and presented.
 *
 * Split out from [BibleSyncViewModel] because choosing is not downloading: the Library tab
 * offers this without an address, a network client, or anything else the sync sheet needs, and
 * both surfaces have to agree about which translation is live.
 */
class BibleChoiceViewModel(
    private val repository: LocalBibleRepository,
) : ViewModel() {

    /** Translations on this device, in the order they were copied. */
    val installed: StateFlow<List<InstalledBible>> = repository.index
        .map { it.bibles }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.index.value.bibles)

    /**
     * The one being read.
     *
     * Resolved through [com.church.presenter.churchpresentermobile.model.BibleLibraryIndex.active]
     * rather than read raw, so a library whose chosen translation has since been deleted still
     * names the one actually in use.
     */
    val activeId: StateFlow<String> = repository.index
        .map { it.active?.id.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.index.value.active?.id.orEmpty())

    /** The one being read, for naming it. */
    val active: StateFlow<InstalledBible?> = repository.index
        .map { it.active }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.index.value.active)

    /** Chooses the translation to read and present. */
    fun setActive(id: String) {
        repository.setActive(id)
        Logger.d(TAG, "setActive — reading '$id'")
    }
}
