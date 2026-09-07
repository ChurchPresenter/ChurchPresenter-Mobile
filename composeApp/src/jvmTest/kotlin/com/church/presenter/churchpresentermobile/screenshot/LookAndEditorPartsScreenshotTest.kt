package com.church.presenter.churchpresentermobile.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.model.NamedTheme
import com.church.presenter.churchpresentermobile.model.SlideTextSize
import com.church.presenter.churchpresentermobile.model.SlideTheme
import com.church.presenter.churchpresentermobile.model.SlideThemePresets
import com.church.presenter.churchpresentermobile.ui.library.DiscardDialog
import com.church.presenter.churchpresentermobile.ui.library.EditorActions
import com.church.presenter.churchpresentermobile.ui.library.EditorField
import com.church.presenter.churchpresentermobile.ui.library.ProblemText
import com.church.presenter.churchpresentermobile.ui.standalone.LookSheetContent
import kotlin.test.Test

/**
 * The look sheet, and the editor's own parts.
 *
 * [EditorField] carries three states an operator can be looking at — plain, a
 * warning, and an error — and the warning is the one that only differs by
 * colour, which is precisely what no behavioural test can check.
 */
class LookAndEditorPartsScreenshotTest {

    @Composable
    private fun Padded(content: @Composable () -> Unit) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content() }
    }

    private val saved = listOf(
        NamedTheme("Sunday morning", SlideTheme()),
        NamedTheme("Evening service", SlideTheme(textColor = "#F5F5DC")),
    )

    // ── The look sheet ───────────────────────────────────────────────────

    @Test
    fun lookSheet() = screenshot("look-sheet__default") {
        LookSheetContent(
            theme = SlideTheme(),
            onThemeChange = {},
            showChords = false,
            onShowChordsChange = {},
            textSize = SlideTextSize.MEDIUM,
            onTextSizeChange = {},
            presets = SlideThemePresets.all,
            savedThemes = emptyList(),
            onApplyTheme = {},
            onSaveTheme = {},
            onDeleteTheme = {},
        )
    }

    @Test
    fun lookSheetWithSavedThemes() = screenshot("look-sheet__saved-themes") {
        LookSheetContent(
            theme = SlideTheme(),
            onThemeChange = {},
            showChords = true,
            onShowChordsChange = {},
            textSize = SlideTextSize.LARGE,
            onTextSizeChange = {},
            presets = SlideThemePresets.all,
            savedThemes = saved,
            onApplyTheme = {},
            onSaveTheme = {},
            onDeleteTheme = {},
        )
    }

    // ── Editor parts ─────────────────────────────────────────────────────

    @Test
    fun editorField() = screenshot("editor-field__plain") {
        Padded { EditorField(label = "Title", value = "Amazing Grace", onValueChange = {}) }
    }

    @Test
    fun editorFieldEmpty() = screenshot("editor-field__empty") {
        Padded { EditorField(label = "Title", value = "", onValueChange = {}) }
    }

    @Test
    fun editorFieldWithError() = screenshot("editor-field__error") {
        Padded {
            EditorField(
                label = "Number",
                value = "",
                onValueChange = {},
                error = "A song needs a number",
            )
        }
    }

    @Test
    fun editorFieldWithWarning() = screenshot("editor-field__warning") {
        // Amber rather than red, and nothing else about the field changes —
        // a difference only a picture can hold on to.
        Padded {
            EditorField(
                label = "Number",
                value = "42",
                onValueChange = {},
                warning = "Another song already uses this number",
            )
        }
    }

    @Test
    fun editorFieldMultiline() = screenshot("editor-field__multiline") {
        Padded {
            EditorField(
                label = "Verse 1",
                value = "Amazing grace! how sweet the sound\nThat saved a wretch like me!",
                onValueChange = {},
                minLines = 4,
            )
        }
    }

    @Test
    fun problemTextError() = screenshot("problem-text__error") {
        Padded { ProblemText(message = "A song needs a number", isError = true) }
    }

    @Test
    fun problemTextWarning() = screenshot("problem-text__warning") {
        Padded { ProblemText(message = "Another song already uses this number", isError = false) }
    }

    @Test
    fun editorActionsSavable() = screenshot("editor-actions__can-save") {
        EditorActions(canSave = true, onCancel = {}, onSave = {})
    }

    @Test
    fun editorActionsNotSavable() = screenshot("editor-actions__cannot-save") {
        EditorActions(canSave = false, onCancel = {}, onSave = {})
    }

    @Test
    fun discardDialog() = screenshot("discard-dialog__open", dialog = true) {
        DiscardDialog(onDiscard = {}, onKeepEditing = {})
    }
}
