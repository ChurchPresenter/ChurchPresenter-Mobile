package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.ui.GearButton
import com.church.presenter.churchpresentermobile.ui.IconTileButton
import com.church.presenter.churchpresentermobile.ui.SquareFab
import com.church.presenter.churchpresentermobile.ui.UploadProgressOverlay
import com.church.presenter.churchpresentermobile.ui.library.OutcomeCard
import com.church.presenter.churchpresentermobile.ui.library.SheetButton
import com.church.presenter.churchpresentermobile.ui.standalone.ColorField
import com.church.presenter.churchpresentermobile.ui.standalone.ColorSwatch
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import kotlin.test.Test

/**
 * The one-off parts screens are trimmed with: buttons, swatches, an overlay.
 *
 * Small enough that nobody looks at them on their own, which is exactly why a
 * golden is worth having — a change to a shared dimension or tint moves all of
 * them at once and shows up here before it shows up in a service.
 */
class SmallPartsScreenshotTest {

    @Composable
    private fun Padded(content: @Composable () -> Unit) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content() }
    }

    @Test
    fun gearButton() = screenshot("gear-button__default", width = null) {
        Padded { GearButton(onClick = {}) }
    }

    @Test
    fun iconTileButton() = screenshot("icon-tile-button__default", width = null) {
        Padded {
            IconTileButton(
                icon = Icons.Filled.Cast,
                contentDescription = "Cast",
                tint = LocalAppColors.current.accent,
                onClick = {},
            )
        }
    }

    @Test
    fun squareFab() = screenshot("square-fab__default", width = null) {
        Padded {
            SquareFab(
                icon = Icons.Filled.Add,
                contentDescription = "Add",
                containerColor = LocalAppColors.current.accent,
                iconColor = Color.White,
                shadowColor = LocalAppColors.current.accent,
                onClick = {},
            )
        }
    }

    @Test
    fun sheetButton() = screenshot("sheet-button__default") {
        Padded { SheetButton(label = "Sync now", onClick = {}) }
    }

    @Test
    fun sheetButtonDestructive() = screenshot("sheet-button__destructive") {
        Padded { SheetButton(label = "Delete everything", isDestructive = true, onClick = {}) }
    }

    @Test
    fun sheetButtonDisabled() = screenshot("sheet-button__disabled") {
        // Disabled is a real state with its own tint, and one a behavioural test
        // can only read off the semantics tree — never see.
        Padded { SheetButton(label = "Sync now", enabled = false, onClick = {}) }
    }

    @Test
    fun outcomeCardSuccess() = screenshot("outcome-card__success") {
        Padded { OutcomeCard(message = "412 songs copied from your computer.", tint = LocalAppColors.current.accent) }
    }

    @Test
    fun outcomeCardFailure() = screenshot("outcome-card__failure") {
        Padded {
            OutcomeCard(
                message = "Could not reach your computer. Check that both devices are on the same Wi-Fi network.",
                tint = LocalAppColors.current.danger,
            )
        }
    }

    @Test
    fun colorFieldWithValue() = screenshot("color-field__with-value") {
        Padded { ColorField(label = "Background", value = "#101014", onValueChange = {}) }
    }

    @Test
    fun colorFieldEmpty() = screenshot("color-field__empty") {
        Padded { ColorField(label = "Background", value = "", onValueChange = {}) }
    }

    @Test
    fun colorSwatch() = screenshot("color-swatch__filled", width = null) {
        Padded { ColorSwatch(color = Color(0xFF2ECC71), onClick = {}) }
    }

    @Test
    fun colorSwatchEmpty() = screenshot("color-swatch__empty", width = null) {
        // A null colour is "not set" and draws as an empty outline, not black.
        Padded { ColorSwatch(color = null, onClick = {}) }
    }

    @Test
    fun uploadIndeterminate() = screenshot("upload-overlay__indeterminate", dialog = true) {
        UploadProgressOverlay(title = "Sending to your computer", progress = null)
    }

    @Test
    fun uploadHalfway() = screenshot("upload-overlay__halfway", dialog = true) {
        UploadProgressOverlay(
            title = "Sending to your computer",
            progress = 0.5f,
            subtitle = "sermon-slides.pdf",
            detail = "4.2 MB of 8.4 MB",
        )
    }

    @Test
    fun uploadComplete() = screenshot("upload-overlay__complete", dialog = true) {
        UploadProgressOverlay(
            title = "Sending to your computer",
            progress = 1f,
            subtitle = "sermon-slides.pdf",
            detail = "8.4 MB of 8.4 MB",
        )
    }
}
