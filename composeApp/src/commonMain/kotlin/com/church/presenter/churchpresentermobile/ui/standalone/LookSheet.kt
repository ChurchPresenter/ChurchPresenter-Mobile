package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.standalone_accent_colour
import churchpresentermobile.composeapp.generated.resources.standalone_brand_line
import churchpresentermobile.composeapp.generated.resources.standalone_brand_line_hint
import churchpresentermobile.composeapp.generated.resources.standalone_font
import churchpresentermobile.composeapp.generated.resources.standalone_font_sans
import churchpresentermobile.composeapp.generated.resources.standalone_font_serif
import churchpresentermobile.composeapp.generated.resources.standalone_gradient_bottom
import churchpresentermobile.composeapp.generated.resources.standalone_gradient_top
import churchpresentermobile.composeapp.generated.resources.standalone_look
import churchpresentermobile.composeapp.generated.resources.standalone_look_reset
import churchpresentermobile.composeapp.generated.resources.standalone_show_clock
import churchpresentermobile.composeapp.generated.resources.standalone_text_colour
import com.church.presenter.churchpresentermobile.model.SlideFont
import com.church.presenter.churchpresentermobile.model.SlideTheme
import com.church.presenter.churchpresentermobile.ui.OverlineRow
import com.church.presenter.churchpresentermobile.ui.SegmentedControl
import com.church.presenter.churchpresentermobile.ui.SettingsField
import com.church.presenter.churchpresentermobile.ui.theme.AppDimens
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/**
 * Everything about how the audience screen looks.
 *
 * A sheet rather than more rows on the Present tab: that tab is the live surface during a
 * service, and Next, Blank and Live must not move further down the screen because someone once
 * wanted to change a colour.
 *
 * Every control writes straight through — the theme rides inside each slide, so the phone's own
 * output, an attached screen and any browser on the hosted page all follow the same edit without
 * being told separately. There is no Apply, and no preview to keep in step with.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LookSheet(
    theme: SlideTheme,
    onThemeChange: ((SlideTheme) -> SlideTheme) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.sheetBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.space20, vertical = AppDimens.space8),
            verticalArrangement = Arrangement.spacedBy(AppDimens.space12),
        ) {
            Text(
                text = stringResource(Res.string.standalone_look),
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.space8)) {
                ColorField(
                    label = stringResource(Res.string.standalone_gradient_top),
                    value = theme.gradientTop,
                    onValueChange = { hex -> onThemeChange { it.copy(gradientTop = hex) } },
                    modifier = Modifier.weight(1f),
                )
                ColorField(
                    label = stringResource(Res.string.standalone_gradient_bottom),
                    value = theme.gradientBottom,
                    onValueChange = { hex -> onThemeChange { it.copy(gradientBottom = hex) } },
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.space8)) {
                ColorField(
                    label = stringResource(Res.string.standalone_text_colour),
                    value = theme.textColor,
                    onValueChange = { hex -> onThemeChange { it.copy(textColor = hex) } },
                    modifier = Modifier.weight(1f),
                )
                ColorField(
                    label = stringResource(Res.string.standalone_accent_colour),
                    value = theme.accentColor,
                    onValueChange = { hex -> onThemeChange { it.copy(accentColor = hex) } },
                    modifier = Modifier.weight(1f),
                )
            }

            OverlineRow(stringResource(Res.string.standalone_font))
            SegmentedControl(
                options = listOf(
                    stringResource(Res.string.standalone_font_serif),
                    stringResource(Res.string.standalone_font_sans),
                ),
                selectedIndex = FONTS.indexOf(theme.font).coerceAtLeast(0),
                onSelect = { index -> onThemeChange { it.copy(font = FONTS[index]) } },
            )

            SettingsField(
                label = stringResource(Res.string.standalone_brand_line),
                value = theme.brandLine.orEmpty(),
                onValueChange = { typed ->
                    // Blank means "no corner line" rather than an empty one taking up space.
                    onThemeChange { it.copy(brandLine = typed.takeIf { line -> line.isNotBlank() }) }
                },
                placeholder = stringResource(Res.string.standalone_brand_line_hint),
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(Res.string.standalone_show_clock),
                    color = colors.text,
                    fontSize = 14.sp,
                )
                Switch(
                    checked = theme.showClock,
                    onCheckedChange = { on -> onThemeChange { it.copy(showClock = on) } },
                )
            }

            Text(
                text = stringResource(Res.string.standalone_look_reset),
                color = colors.accent,
                fontSize = 13.sp,
                modifier = Modifier
                    .padding(top = AppDimens.space8)
                    .clickable { onThemeChange { SlideTheme() } },
            )

            Box(Modifier.padding(bottom = AppDimens.space16))
        }
    }
}

private val FONTS = listOf(SlideFont.SERIF, SlideFont.SANS)
