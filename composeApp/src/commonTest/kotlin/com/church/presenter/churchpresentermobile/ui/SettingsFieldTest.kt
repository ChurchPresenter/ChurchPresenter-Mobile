package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the reusable building blocks every screen is assembled from.
 *
 * They read colours from `LocalAppColors`, so each case wraps its content in
 * [AppTheme] — a bare `setContent` would take the composition-local default and
 * test a configuration the app never renders.
 */
/**
 * Tests [SettingsField] — the labelled inputs on the settings screen.
 *
 * It reads colours from `LocalAppColors`, so every case renders inside [AppTheme]:
 * a bare `setContent` would take the composition-local default and test a
 * configuration the app never shows.
 */
@OptIn(ExperimentalTestApi::class)
class SettingsFieldTest {

    private fun themed(content: @Composable () -> Unit): @Composable () -> Unit = {
        AppTheme(themeMode = ThemeMode.DARK) { content() }
    }

    @Test
    fun aSettingsFieldShowsItsLabelAndValue() = runComposeUiTest {
        setContent(themed { SettingsField(label = "Host", value = "10.0.0.5", onValueChange = {}) })

        onNodeWithText("HOST").assertExists()
        onNodeWithText("10.0.0.5").assertExists()
    }

    @Test
    fun anEmptySettingsFieldShowsItsPlaceholder() = runComposeUiTest {
        setContent(themed {
            SettingsField(label = "Host", value = "", onValueChange = {}, placeholder = "192.168.1.10")
        })

        onNodeWithText("192.168.1.10").assertExists()
    }

    @Test
    fun editingASettingsFieldReportsTheNewValue() = runComposeUiTest {
        var typed = ""
        setContent(themed { SettingsField(label = "Host", value = "old", onValueChange = { typed = it }) })

        onNodeWithText("old").performTextReplacement("new")

        assertEquals("new", typed)
    }

    @Test
    fun aSettingsFieldShowsItsErrorWhenGivenOne() = runComposeUiTest {
        setContent(themed {
            SettingsField(label = "Port", value = "70000", onValueChange = {}, error = "Port must be 1-65535")
        })

        onNodeWithText("Port must be 1-65535").assertExists()
    }

    @Test
    fun aValidSettingsFieldShowsNoError() = runComposeUiTest {
        setContent(themed { SettingsField(label = "Port", value = "8765", onValueChange = {}) })

        onNodeWithText("Port must be 1-65535").assertDoesNotExist()
    }

    @Test
    fun aPasswordFieldRendersItsValue() = runComposeUiTest {
        // Masking is a visual transformation: the value stays in the semantics
        // tree, so what a test can check here is that the field renders at all.
        // Whether the glyphs are dots is a pixel concern, not a semantic one.
        setContent(themed {
            SettingsField(label = "API key", value = "s3cret", onValueChange = {}, password = true)
        })

        onNodeWithText("API KEY").assertExists()
    }

    @Test
    fun aRevealedPasswordFieldShowsItsValue() = runComposeUiTest {
        setContent(themed {
            SettingsField(
                label = "API key",
                value = "s3cret",
                onValueChange = {},
                password = true,
                passwordVisible = true,
            )
        })

        onNodeWithText("s3cret").assertExists()
    }

    @Test
    fun aMonospacedSettingsFieldRenders() = runComposeUiTest {
        // Used for the API key, where character shape matters when reading it out.
        setContent(themed { SettingsField(label = "Key", value = "abc123", onValueChange = {}, mono = true) })

        onNodeWithText("abc123").assertExists()
    }

    @Test
    fun aSettingsFieldShowsBothAValueAndAnErrorTogether() = runComposeUiTest {
        // The bad value has to stay on screen — clearing it would lose what the
        // operator typed and leave them nothing to correct.
        setContent(themed {
            SettingsField(label = "Port", value = "70000", onValueChange = {}, error = "Out of range")
        })

        onNodeWithText("70000").assertExists()
        onNodeWithText("Out of range").assertExists()
    }
}
