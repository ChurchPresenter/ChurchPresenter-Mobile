package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.ui.LiveToast
import com.church.presenter.churchpresentermobile.ui.OutlineActionButton
import com.church.presenter.churchpresentermobile.ui.OverlineRow
import com.church.presenter.churchpresentermobile.ui.SearchField
import com.church.presenter.churchpresentermobile.ui.SegmentedControl
import com.church.presenter.churchpresentermobile.ui.SettingsField
import kotlin.test.Test

/**
 * The shared building blocks in `ui/DesignComponents.kt`.
 *
 * These are the pieces every screen is assembled from, so they are where a
 * design-token change shows up first — and where it is cheapest to look at. Each
 * test captures one component in the states it can actually be in: a field with
 * and without a value, a control with its selection at either end, an error
 * showing and not.
 */
class DesignComponentsScreenshotTest {

    /** Small components are captured with room around them, not flush to the edge. */
    @Composable
    private fun Padded(content: @Composable () -> Unit) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content() }
    }

    @Test
    fun searchFieldEmpty() = screenshot("search-field__empty") {
        Padded { SearchField(value = "", onValueChange = {}, placeholder = "Search songs") }
    }

    @Test
    fun searchFieldWithQuery() = screenshot("search-field__with-query") {
        Padded { SearchField(value = "Amazing Grace", onValueChange = {}, placeholder = "Search songs") }
    }

    @Test
    fun overlineRowLabelOnly() = screenshot("overline-row__label-only") {
        Padded { OverlineRow(label = "Songbooks") }
    }

    @Test
    fun overlineRowWithTrailing() = screenshot("overline-row__with-trailing") {
        Padded { OverlineRow(label = "Songbooks", trailing = "3 selected") }
    }

    @Test
    fun segmentedControlTwoOptions() = screenshot("segmented-control__two-options") {
        Padded { SegmentedControl(options = listOf("Songs", "Bible"), selectedIndex = 0, onSelect = {}) }
    }

    @Test
    fun segmentedControlLastOfThreeSelected() = screenshot("segmented-control__last-of-three") {
        // The selection indicator has to travel; the last slot is where a
        // rounding or padding error in it becomes visible.
        Padded {
            SegmentedControl(
                options = listOf("Light", "Dark", "System"),
                selectedIndex = 2,
                onSelect = {},
            )
        }
    }

    @Test
    fun settingsFieldEmpty() = screenshot("settings-field__empty") {
        Padded {
            SettingsField(
                label = "Computer address",
                value = "",
                onValueChange = {},
                placeholder = "192.168.1.10",
            )
        }
    }

    @Test
    fun settingsFieldWithValue() = screenshot("settings-field__with-value") {
        Padded {
            SettingsField(label = "Computer address", value = "192.168.1.10", onValueChange = {})
        }
    }

    @Test
    fun settingsFieldWithError() = screenshot("settings-field__with-error") {
        Padded {
            SettingsField(
                label = "Port",
                value = "87650",
                onValueChange = {},
                error = "Enter a port between 1 and 65535",
            )
        }
    }

    @Test
    fun settingsFieldPasswordHidden() = screenshot("settings-field__password-hidden") {
        Padded {
            SettingsField(
                label = "API key",
                value = "s3cret-key-value",
                onValueChange = {},
                password = true,
                passwordVisible = false,
                onTogglePasswordVisible = {},
            )
        }
    }

    @Test
    fun settingsFieldPasswordVisible() = screenshot("settings-field__password-visible") {
        Padded {
            SettingsField(
                label = "API key",
                value = "s3cret-key-value",
                onValueChange = {},
                password = true,
                passwordVisible = true,
                onTogglePasswordVisible = {},
                mono = true,
            )
        }
    }

    @Test
    fun outlineActionButton() = screenshot("outline-action-button__default") {
        Padded { OutlineActionButton(label = "Cast to computer", icon = Icons.Filled.Cast, onClick = {}) }
    }

    @Test
    fun liveToast() = screenshot("live-toast__short", width = null) {
        Padded { LiveToast(message = "Now live") }
    }

    @Test
    // Kept at a phone's width rather than wrapped: the point of this state is
    // what a message too long for the screen does, and a wrapped capture would
    // let the toast grow to 473dp — a width no phone will ever give it.
    fun liveToastLongMessage() = screenshot("live-toast__long") {
        Padded { LiveToast(message = "Amazing Grace — verse 3 is now showing on the projector") }
    }
}
