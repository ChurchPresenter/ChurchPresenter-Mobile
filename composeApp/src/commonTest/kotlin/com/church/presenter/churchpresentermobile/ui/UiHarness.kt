package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme

/**
 * Shared setup for the app screens' UI tests.
 *
 * Controls are reached by their [UiTags], not by position — see the
 * "tag it, don't count it" section of AGENT.md. Content the operator or the
 * desktop supplied (a song title, an error message) is plain data and is
 * matched by its text.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showScreen(content: @Composable () -> Unit) {
    setContent { AppTheme(themeMode = ThemeMode.DARK) { content() } }
}

/** The node carrying [tag], scrolling a lazy list until it exists if need be. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.tagged(tag: String): SemanticsNodeInteraction {
    if (onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isEmpty()) {
        onNode(hasScrollAction()).performScrollToNode(hasTestTag(tag))
    }
    return onNodeWithTag(tag)
}

/**
 * Presses the control carrying [tag].
 *
 * Invokes the click action rather than tapping a coordinate. A control whose
 * only content is a `stringResource` label renders *empty* in this runtime and
 * so has zero width: a positional tap lands outside it and silently does
 * nothing, which looks exactly like a handler that was never wired up.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.click(tag: String) =
    tagged(tag).performSemanticsAction(SemanticsActions.OnClick)

/**
 * Whether anything carrying [tag] is on screen.
 *
 * Searches the unmerged tree: a tag inside a `selectable` or `clickable` row is
 * merged into its parent, and the merged tree then hides it — so a badge or
 * pill *inside* a card would read as absent when it is plainly there.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.exists(tag: String): Boolean =
    onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

/**
 * Whether [text] is anywhere on screen.
 *
 * Searches the unmerged tree, for the same reason [exists] does: a `Text`
 * inside a `selectable` or `clickable` row is merged into its parent, and the
 * merged node does not answer [hasText] for the words its children carry. A
 * verse row is exactly that shape, so the words were plainly on screen while
 * this reported them absent.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.isShowing(text: String): Boolean =
    onAllNodes(hasText(text, substring = true), useUnmergedTree = true)
        .fetchSemanticsNodes().isNotEmpty()

/**
 * The editable node behind [tag].
 *
 * A tag passed to a wrapper such as `SearchField` lands on its outer layout,
 * not on the text field inside it, and only the field carries the focus and
 * set-text actions.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.textField(tag: String): SemanticsNodeInteraction {
    tagged(tag)
    val inside = onAllNodes(
        hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)),
        useUnmergedTree = true,
    )
    return if (inside.fetchSemanticsNodes().isNotEmpty()) inside[0] else onNodeWithTag(tag)
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.type(tag: String, text: String) =
    textField(tag).performTextReplacement(text)

/**
 * Waits until [condition] holds, or fails the test.
 *
 * A screen driven through its real ViewModel does its work in a coroutine, and
 * how soon that coroutine resumes is a property of the runtime, not of the
 * screen. On wasm it happened to have finished by the time the click returned;
 * on the JVM it has not. Awaiting the outcome is correct on both — and is the
 * difference between a test that passes everywhere and one that passed by
 * accident on the only runtime it was ever run on.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.awaitThat(condition: () -> Boolean) =
    waitUntil(timeoutMillis = AWAIT_TIMEOUT_MS) { condition() }

private const val AWAIT_TIMEOUT_MS = 5_000L
