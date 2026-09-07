package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
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
 * Tests [SearchField] — the search box at the top of the songs and bible lists.
 *
 * It reads colours from `LocalAppColors`, so every case renders inside [AppTheme]:
 * a bare `setContent` would take the composition-local default and test a
 * configuration the app never shows.
 */
@OptIn(ExperimentalTestApi::class)
class SearchFieldTest {

    private fun themed(content: @Composable () -> Unit): @Composable () -> Unit = {
        AppTheme(themeMode = ThemeMode.DARK) { content() }
    }

    @Test
    fun aSearchFieldShowsItsPlaceholderWhileEmpty() = runComposeUiTest {
        setContent(themed { SearchField(value = "", onValueChange = {}, placeholder = "Search songs") })

        onNodeWithText("Search songs").assertExists()
    }

    @Test
    fun aSearchFieldWithTextHidesThePlaceholder() = runComposeUiTest {
        setContent(themed { SearchField(value = "grace", onValueChange = {}, placeholder = "Search songs") })

        onNodeWithText("grace").assertExists()
        onNodeWithText("Search songs").assertDoesNotExist()
    }

    @Test
    fun typingInASearchFieldReportsEveryChange() = runComposeUiTest {
        var typed = ""
        setContent(themed { SearchField(value = "", onValueChange = { typed = it }, placeholder = "Search") })

        onNode(hasSetTextAction()).performTextInput("am")

        assertEquals("am", typed)
    }

    @Test
    fun aSearchFieldRendersWhatItIsGiven() = runComposeUiTest {
        // Stateless by design: it shows `value`, and the caller owns the state.
        var value by mutableStateOf("first")
        setContent(themed { SearchField(value = value, onValueChange = { value = it }, placeholder = "p") })
        onNodeWithText("first").assertExists()

        value = "second"
        waitForIdle()

        onNodeWithText("second").assertExists()
        onNodeWithText("first").assertDoesNotExist()
    }

    @Test
    fun aSearchFieldHandlesAVeryLongQuery() = runComposeUiTest {
        // Pasted from a schedule row, which can be a whole first line.
        val long = "Amazing grace how sweet the sound that saved a wretch like me ".repeat(4)
        setContent(themed { SearchField(value = long, onValueChange = {}, placeholder = "Search") })

        onNodeWithText(long).assertExists()
    }

    @Test
    fun aSearchFieldHandlesNonLatinText() = runComposeUiTest {
        // Songbooks in this app are not all English.
        setContent(themed { SearchField(value = "Благодать", onValueChange = {}, placeholder = "Search") })

        onNodeWithText("Благодать").assertExists()
    }

    @Test
    fun aSearchPlaceholderIsNotMistakenForAValue() = runComposeUiTest {
        // The placeholder must not be reported back through onValueChange.
        var reported: String? = null
        setContent(themed { SearchField(value = "", onValueChange = { reported = it }, placeholder = "Search") })

        assertEquals(null, reported)
    }
}
