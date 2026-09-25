package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.contact_us_title
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.util.Analytics
import com.church.presenter.churchpresentermobile.util.AnalyticsScreen
import org.jetbrains.compose.resources.stringResource

/**
 * The contact form over the whole app, with a Back arrow to the screen it covered.
 *
 * For a mode with no More tab to open it in — calendar mode, whose only screen is the planner.
 * Reached from Settings, where every mode offers Contact us.
 *
 * @param form The form itself; replaced only by tests, which have no ViewModel store to build its
 *   ViewModel in.
 */
@Composable
internal fun ContactOverlay(
    onClose: () -> Unit,
    form: @Composable (Modifier) -> Unit = { ContactScreen(modifier = it) },
) {
    LaunchedEffect(Unit) { Analytics.logScreenView(AnalyticsScreen.CONTACT) }
    AppBackHandler(enabled = true, onBack = onClose)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.background)
            .testTag(UiTags.CONTACT_OVERLAY),
    ) {
        ScreenHeader(title = stringResource(Res.string.contact_us_title), onBack = onClose)
        HorizontalDivider(color = LocalAppColors.current.borderSubtle)
        form(Modifier.weight(1f))
    }
}
