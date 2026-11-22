package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** [fitScale] and [FitToHeight]: a tablet start screen that fits a landscape iPad without scrolling. */
@OptIn(ExperimentalTestApi::class)
class FitToHeightTest {

    @Test
    fun contentThatFitsIsLeftAtItsOwnSize() {
        assertEquals(1f, fitScale(natural = 600, available = 800))
        assertEquals(1f, fitScale(natural = 800, available = 800))
    }

    @Test
    fun contentThatOverflowsIsShrunkToTheAvailableHeight() {
        assertEquals(0.75f, fitScale(natural = 1000, available = 750))
    }

    @Test
    fun shrinkingStopsWhereTheTypeWouldBeTooSmall() {
        assertEquals(FIT_MIN_SCALE, fitScale(natural = 3000, available = 600))
    }

    @Test
    fun anUnmeasuredSideIsLeftAlone() {
        assertEquals(1f, fitScale(natural = 0, available = 600))
        assertEquals(1f, fitScale(natural = 600, available = 0))
    }

    @Test
    fun theTabletPickersContinueButtonIsOnScreenInLandscape() = runComposeUiTest {
        // 1024×768 less the status bar: the iPad's landscape window, which the picker overflowed.
        setContent {
            AppTheme(themeMode = ThemeMode.DARK) {
                Box(Modifier.size(width = 1024.dp, height = 744.dp)) {
                    ModePickerScreen(onModeChosen = {}, twoPane = true, initialMode = AppMode.CALENDAR)
                }
            }
        }

        // onNodeWithTag also proves the measuring copy is invisible: two matches would fail it.
        // In pixels, since in dp the button reports in its own shrunken density; and unclipped, since
        // boundsInRoot is cut to what is visible and a button scrolled out of sight reads as on screen.
        val node = onNodeWithTag(UiTags.MODE_CONTINUE).fetchSemanticsNode()
        val bottom = node.positionInRoot.y + node.size.height
        val right = node.positionInRoot.x + node.size.width
        val window = onRoot().fetchSemanticsNode().size
        assertTrue(bottom <= window.height, "Continue ends at ${bottom}px, below the ${window.height}px window")
        assertTrue(right <= window.width, "Continue ends at ${right}px, past the ${window.width}px window")
    }

    @Test
    fun aWindowTooShortToShrinkIntoFallsBackToScrolling() = runComposeUiTest {
        // Shrinking stops at FIT_MIN_SCALE so the type stays readable; below that the picker scrolls.
        setContent {
            AppTheme(themeMode = ThemeMode.DARK) {
                Box(Modifier.size(width = 1024.dp, height = 300.dp)) {
                    ModePickerScreen(onModeChosen = {}, twoPane = true)
                }
            }
        }

        val window = onRoot().fetchSemanticsNode().size
        val before = onNodeWithTag(UiTags.MODE_CONTINUE).fetchSemanticsNode()
        assertTrue(before.positionInRoot.y + before.size.height > window.height, "the picker fitted after all")

        onNodeWithTag(UiTags.MODE_CONTINUE).performScrollTo()

        val after = onNodeWithTag(UiTags.MODE_CONTINUE).fetchSemanticsNode()
        assertTrue(after.positionInRoot.y + after.size.height <= window.height, "Continue cannot be scrolled to")
    }

    @Test
    fun anUnboundedHeightLaysTheContentOutAtItsOwnSize() = runComposeUiTest {
        var measured = 0
        setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                FitToHeight(Modifier.onSizeChanged { measured = it.height }) {
                    Box(Modifier.size(width = 100.dp, height = 1_000.dp))
                }
            }
        }

        waitForIdle()
        assertEquals(with(density) { 1_000.dp.roundToPx() }, measured)
    }
}
