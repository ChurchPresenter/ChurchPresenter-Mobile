package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Where a screen's toasts appear: along the top of its content, just under the
 * header.
 *
 * They used to hang from the bottom, which is where the floating Go Live and
 * Add to schedule buttons are — so "Song is live" sat over the button the
 * operator reaches for next, for the few seconds it mattered most. The top of
 * the content has nothing to press mid-service.
 *
 * One place for every screen, so the next screen with a toast cannot put it
 * back over the buttons.
 */
@Composable
fun BoxScope.ToastHost(hostState: SnackbarHostState) {
    SnackbarHost(
        hostState = hostState,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 8.dp)
            .testTag(UiTags.TOAST_HOST),
    )
}
