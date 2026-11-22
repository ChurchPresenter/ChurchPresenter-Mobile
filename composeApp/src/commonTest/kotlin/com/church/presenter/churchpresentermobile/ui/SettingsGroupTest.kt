package com.church.presenter.churchpresentermobile.ui

import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.ui.theme.appColorsFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsGroupTest {

    @Test
    fun modeHasNoGroupOfItsOwn() = assertNull(SettingsSection.MODE.group)

    @Test
    fun everyOtherSectionBelongsToAGroup() {
        assertEquals(SettingsGroup.CONNECTION, SettingsSection.SERVER.group)
        assertEquals(SettingsGroup.CONNECTION, SettingsSection.TVS.group)
        assertEquals(SettingsGroup.PRESENTING, SettingsSection.DISPLAY.group)
        assertEquals(SettingsGroup.PRESENTING, SettingsSection.COMPUTER.group)
        assertEquals(SettingsGroup.DEVICE, SettingsSection.DEVICE.group)
        assertEquals(SettingsGroup.DEVICE, SettingsSection.APPEARANCE.group)
        assertEquals(SettingsGroup.HELP, SettingsSection.DIAGNOSTICS.group)
        assertEquals(SettingsGroup.HELP, SettingsSection.ABOUT.group)
    }

    @Test
    fun theMenuGroupsSectionsInTheOrderTheyAreListed() {
        val sections = settingsSections(hasDesktop = true, supportsStandalone = true)
        assertEquals(
            listOf(
                SettingsGroup.CONNECTION to listOf(SettingsSection.SERVER),
                SettingsGroup.DEVICE to listOf(SettingsSection.DEVICE, SettingsSection.APPEARANCE),
                SettingsGroup.HELP to listOf(SettingsSection.DIAGNOSTICS, SettingsSection.ABOUT),
            ),
            groupsOf(sections),
        )
    }

    @Test
    fun eachModeHasItsOwnColor() {
        val colors = appColorsFor(isDark = true)
        assertEquals(colors.accent, colors.modeTint(AppMode.REMOTE))
        assertEquals(colors.greekAccent, colors.modeTint(AppMode.STANDALONE))
        assertEquals(colors.amber, colors.modeTint(AppMode.CALENDAR))
    }

    @Test
    fun theConnectionGroupTakesTheModeColor() {
        val colors = appColorsFor(isDark = false)
        assertEquals(colors.modeTint(AppMode.REMOTE), colors.groupTint(SettingsGroup.CONNECTION, AppMode.REMOTE))
        assertEquals(colors.greekAccent, colors.groupTint(SettingsGroup.PRESENTING, AppMode.STANDALONE))
        assertEquals(colors.warning, colors.groupTint(SettingsGroup.DEVICE, AppMode.REMOTE))
        assertEquals(colors.amber, colors.groupTint(SettingsGroup.HELP, AppMode.REMOTE))
    }

    @Test
    fun everyToneHasAColor() {
        val colors = appColorsFor(isDark = true)
        assertEquals(colors.accent, colors.toneColor(ConnectionTone.GOOD))
        assertEquals(colors.warning, colors.toneColor(ConnectionTone.WARNING))
        assertEquals(colors.danger, colors.toneColor(ConnectionTone.BAD))
        assertEquals(colors.muted, colors.toneColor(ConnectionTone.PENDING))
    }
}
