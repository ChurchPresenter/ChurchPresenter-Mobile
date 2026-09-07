package com.church.presenter.churchpresentermobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.runComposeUiTest
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests [ContentActionButtons] — the cast / add-to-schedule stack shared by the
 * songs, bible and pictures screens.
 *
 * Several of its controls are optional and appear only when the screen passes a
 * handler: standalone has no desktop schedule, and only the bible screen holds.
 * A button rendered without a handler is one the operator can press to no effect.
 */
@OptIn(ExperimentalTestApi::class)
class ContentActionButtonsTest {

    private fun themed(content: @Composable () -> Unit): @Composable () -> Unit = {
        AppTheme(themeMode = ThemeMode.DARK) { content() }
    }

    private fun countClickables(test: androidx.compose.ui.test.ComposeUiTest): Int =
        test.onAllNodes(hasClickAction()).fetchSemanticsNodes().size

    @Test
    fun theCastButtonIsAlwaysPresent() = runComposeUiTest {
        setContent(themed {
            ContentActionButtons(isProjecting = false, scheduleAdded = false, onToggleProjecting = {})
        })

        assertTrue(countClickables(this) >= 1)
    }

    @Test
    fun addingToScheduleIsOfferedOnlyWithAHandler() = runComposeUiTest {
        var withHandler = 0
        setContent(themed {
            ContentActionButtons(
                isProjecting = false,
                scheduleAdded = false,
                onToggleProjecting = {},
                onAddToSchedule = {},
            )
        })
        withHandler = countClickables(this)

        setContent(themed {
            ContentActionButtons(isProjecting = false, scheduleAdded = false, onToggleProjecting = {})
        })
        val withoutHandler = countClickables(this)

        assertTrue(withHandler > withoutHandler, "$withHandler vs $withoutHandler")
    }

    @Test
    fun holdIsOfferedOnlyWithAHandler() = runComposeUiTest {
        setContent(themed {
            ContentActionButtons(
                isProjecting = true,
                scheduleAdded = false,
                onToggleProjecting = {},
                onToggleHold = {},
            )
        })
        val withHold = countClickables(this)

        setContent(themed {
            ContentActionButtons(isProjecting = true, scheduleAdded = false, onToggleProjecting = {})
        })

        assertTrue(withHold > countClickables(this))
    }

    @Test
    fun clearDisplayIsOfferedOnlyWithAHandler() = runComposeUiTest {
        setContent(themed {
            ContentActionButtons(
                isProjecting = true,
                scheduleAdded = false,
                onToggleProjecting = {},
                onClearDisplay = {},
            )
        })
        val withClear = countClickables(this)

        setContent(themed {
            ContentActionButtons(isProjecting = true, scheduleAdded = false, onToggleProjecting = {})
        })

        assertTrue(withClear > countClickables(this))
    }

    @Test
    fun multiSelectIsOfferedOnlyWithAHandler() = runComposeUiTest {
        setContent(themed {
            ContentActionButtons(
                isProjecting = false,
                scheduleAdded = false,
                onToggleProjecting = {},
                onToggleMultiSelect = {},
            )
        })
        val withMulti = countClickables(this)

        setContent(themed {
            ContentActionButtons(isProjecting = false, scheduleAdded = false, onToggleProjecting = {})
        })

        assertTrue(withMulti > countClickables(this))
    }

    @Test
    fun everyStateCombinationRenders() = runComposeUiTest {
        // The stack changes icon and tint on each of these; a missing branch is a
        // crash on the screen the operator uses most.
        for (projecting in listOf(true, false)) {
            for (added in listOf(true, false)) {
                for (holding in listOf(true, false)) {
                    for (multi in listOf(true, false)) {
                        setContent(themed {
                            ContentActionButtons(
                                isProjecting = projecting,
                                scheduleAdded = added,
                                onToggleProjecting = {},
                                onAddToSchedule = {},
                                isHolding = holding,
                                onToggleHold = {},
                                onClearDisplay = {},
                                isMultiSelectMode = multi,
                                onToggleMultiSelect = {},
                            )
                        })
                    }
                }
            }
        }
        assertTrue(countClickables(this) >= 1)
    }

    @Test
    fun extraLeadingContentIsRendered() = runComposeUiTest {
        // Screens slot their own control in above the stack.
        var composed = 0
        setContent(themed {
            ContentActionButtons(
                isProjecting = false,
                scheduleAdded = false,
                onToggleProjecting = {},
                extraLeadingContent = { composed++ },
            )
        })

        assertEquals(1, composed)
    }

    @Test
    fun everyBadgeCountAndThemeRenders() = runComposeUiTest {
        // The badge shows how many screens are attached in standalone; both it and
        // the stack read their colours from the palette.
        for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
            for (count in BADGE_COUNTS) {
                setContent {
                    AppTheme(themeMode = mode) {
                        ContentActionButtons(
                            isProjecting = true,
                            scheduleAdded = false,
                            onToggleProjecting = {},
                            castBadgeCount = count,
                        )
                    }
                }
            }
        }
        assertTrue(countClickables(this) >= 1)
    }
}

/** Empty, single digit, and two digit — the three ways the badge is drawn. */
private val BADGE_COUNTS = listOf(0, 1, 99)
