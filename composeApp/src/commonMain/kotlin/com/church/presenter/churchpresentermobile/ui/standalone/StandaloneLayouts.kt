package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.tab_present
import com.church.presenter.churchpresentermobile.ui.PresentSidePaneWidth
import com.church.presenter.churchpresentermobile.ui.ScreenHeader
import com.church.presenter.churchpresentermobile.ui.theme.AppDimens
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/*
 * The two shapes the Present tab takes.
 *
 * Both are handed the same five pieces by [StandaloneControllerScreen] and do
 * nothing but place them, which is why they live together and away from the
 * screen that builds them: the difference between a phone and a tablet here is
 * entirely a matter of arrangement, and it reads that way only when the two
 * arrangements sit side by side.
 */

/**
 * The phone's arrangement: two parts, everything that can scroll and the handful
 * of controls that must never scroll away.
 *
 * The preview, the section list and the two pickers together overflow a phone
 * screen, which used to squash Prev/Next and Blank/Live into the bottom edge —
 * the controls an operator reaches for without looking, mid-service.
 */
@Composable
internal fun StandaloneOnePane(
    outputChip: @Composable () -> Unit,
    preview: @Composable () -> Unit,
    controls: @Composable ColumnScope.() -> Unit,
    sections: @Composable () -> Unit,
    lookControls: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.space16),
            verticalArrangement = Arrangement.spacedBy(AppDimens.space14),
        ) {
            outputChip()
            preview()
            sections()
            lookControls()
            Spacer(Modifier.height(AppDimens.space8))
        }

        HorizontalDivider(color = colors.borderSubtle)

        Column(
            modifier = Modifier.padding(
                horizontal = AppDimens.space16,
                vertical = AppDimens.space12,
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.space14),
        ) {
            controls()
        }
    }
}

/**
 * The tablet's arrangement for the Present tab.
 *
 * The split is by *urgency*, not by hierarchy like the other tabs': the live
 * surface and the four controls an operator hits without looking — Prev, Next,
 * Blank, Live — take the left, where they never move and never scroll. The deck
 * and the look settings go right, where scrolling costs nothing because nothing
 * over there is reached mid-verse.
 *
 * That is also why the controls sit under the preview rather than under the whole
 * row: on a phone they are pinned to the bottom edge, and pinning them to the
 * bottom of a 1024dp-tall tablet would put them a hand's span from the slide they
 * act on.
 */
@Composable
internal fun StandaloneTwoPane(
    outputChip: @Composable () -> Unit,
    preview: @Composable () -> Unit,
    controls: @Composable ColumnScope.() -> Unit,
    sections: @Composable () -> Unit,
    lookControls: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    onMenu: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    Row(modifier = modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            ScreenHeader(
                title = stringResource(Res.string.tab_present),
                onMenu = onMenu,
                onSettings = onSettings,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = AppDimens.space16),
                verticalArrangement = Arrangement.spacedBy(AppDimens.space14),
            ) {
                outputChip()
                preview()
            }

            HorizontalDivider(color = colors.borderSubtle)

            Column(
                modifier = Modifier.padding(
                    horizontal = AppDimens.space16,
                    vertical = AppDimens.space12,
                ),
                verticalArrangement = Arrangement.spacedBy(AppDimens.space14),
            ) {
                controls()
            }
        }

        VerticalDivider(color = colors.borderSubtle)

        Column(
            modifier = Modifier
                .width(PresentSidePaneWidth)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.space16, vertical = AppDimens.space16),
            verticalArrangement = Arrangement.spacedBy(AppDimens.space14),
        ) {
            sections()
            lookControls()
            Spacer(Modifier.height(AppDimens.space8))
        }
    }
}
