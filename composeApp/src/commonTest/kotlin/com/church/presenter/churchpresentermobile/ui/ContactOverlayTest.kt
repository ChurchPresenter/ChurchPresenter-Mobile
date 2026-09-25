package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The contact form over the app: calendar mode has no More tab and no tab bar, so Settings'
 * Contact us opens it here, and Back has to return to the planner.
 */
@OptIn(ExperimentalTestApi::class)
class ContactOverlayTest {

    @Test
    fun theFormIsShownUnderAHeaderWithABackArrow() = runComposeUiTest {
        showScreen { ContactOverlay(onClose = {}, form = { Box(it.testTag(FORM)) }) }

        assertTrue(exists(UiTags.CONTACT_OVERLAY))
        assertTrue(exists(FORM), "the contact form")
        assertTrue(exists(UiTags.HEADER_BACK))
    }

    @Test
    fun backClosesIt() = runComposeUiTest {
        var closed = 0
        showScreen { ContactOverlay(onClose = { closed++ }, form = { Box(it.testTag(FORM)) }) }

        click(UiTags.HEADER_BACK)

        assertEquals(1, closed)
    }

    private companion object {
        const val FORM = "test:contactForm"
    }
}
