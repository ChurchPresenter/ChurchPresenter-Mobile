package com.church.presenter.churchpresentermobile.ui

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * The three ViewTree owners a `ComposeView` needs in order to run.
 *
 * An Activity supplies these automatically, but a `Presentation` window on a
 * second display does not — its decor view has no owners, and a `ComposeView`
 * placed inside it crashes at `show()` with "ViewTreeLifecycleOwner not found".
 * This is a minimal standalone implementation so the presentation window can
 * host Compose content.
 *
 * The lifecycle is driven manually by the presentation: [onShow] moves it to
 * RESUMED, [onDismiss] tears it down and clears the ViewModel store.
 */
class PresentationOwners : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    /** Installs the owners on [view] (typically the presentation's decor view). */
    fun attachTo(view: View) {
        savedStateController.performRestore(null)
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
    }

    /** Moves the lifecycle to RESUMED so Compose starts composing and animating. */
    fun onShow() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    /** Tears the lifecycle down and releases any ViewModels the content created. */
    fun onDismiss() {
        // Guard against a double dismiss: LifecycleRegistry throws on a
        // backwards transition once it has already reached DESTROYED.
        if (lifecycleRegistry.currentState == Lifecycle.State.DESTROYED) return
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }
}
