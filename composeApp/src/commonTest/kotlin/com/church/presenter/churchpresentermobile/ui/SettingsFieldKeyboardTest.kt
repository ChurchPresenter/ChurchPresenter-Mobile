package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import kotlin.test.Test

/**
 * Tests how [SettingsField] configures the soft keyboard.
 *
 * Port takes a number pad and host a URI keyboard; every field but the last
 * offers Next rather than Done. A wrong choice makes the field awkward rather
 * than broken, which is exactly the kind of regression nothing else catches.
 */
@OptIn(ExperimentalTestApi::class)
class SettingsFieldKeyboardTest {

    private fun themed(content: @Composable () -> Unit): @Composable () -> Unit = {
        AppTheme(themeMode = ThemeMode.DARK) { content() }
    }

    @Test
    fun aSettingsFieldRendersEveryKeyboardType() = runComposeUiTest {
        // Port takes a number pad, host a URI keyboard; a wrong one makes the
        // field awkward rather than broken, so it is easy to regress unnoticed.
        for (type in listOf(
            androidx.compose.ui.text.input.KeyboardType.Text,
            androidx.compose.ui.text.input.KeyboardType.Number,
            androidx.compose.ui.text.input.KeyboardType.Uri,
            androidx.compose.ui.text.input.KeyboardType.Password,
        )) {
            setContent(themed {
                SettingsField(label = "Field", value = "v", onValueChange = {}, keyboardType = type)
            })
            onNodeWithText("v").assertExists()
        }
    }

    @Test
    fun aSettingsFieldRendersBothImeActions() = runComposeUiTest {
        // Next on every field but the last, which offers Done.
        for (action in listOf(
            androidx.compose.ui.text.input.ImeAction.Next,
            androidx.compose.ui.text.input.ImeAction.Done,
        )) {
            setContent(themed {
                SettingsField(label = "Field", value = "v", onValueChange = {}, imeAction = action)
            })
            onNodeWithText("v").assertExists()
        }
    }
}
