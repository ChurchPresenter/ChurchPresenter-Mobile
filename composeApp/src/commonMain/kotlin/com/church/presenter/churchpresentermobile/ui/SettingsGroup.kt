package com.church.presenter.churchpresentermobile.ui

import androidx.compose.ui.graphics.Color
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.settings_group_connection
import churchpresentermobile.composeapp.generated.resources.settings_group_help
import churchpresentermobile.composeapp.generated.resources.settings_group_presenting
import churchpresentermobile.composeapp.generated.resources.settings_this_device
import com.church.presenter.churchpresentermobile.model.AppMode
import com.church.presenter.churchpresentermobile.ui.theme.AppColors
import org.jetbrains.compose.resources.StringResource

internal enum class SettingsGroup(val label: StringResource) {
    CONNECTION(Res.string.settings_group_connection),
    PRESENTING(Res.string.settings_group_presenting),
    DEVICE(Res.string.settings_this_device),
    HELP(Res.string.settings_group_help),
}

internal val SettingsSection.group: SettingsGroup?
    get() = when (this) {
        SettingsSection.MODE -> null
        SettingsSection.SERVER, SettingsSection.TVS -> SettingsGroup.CONNECTION
        SettingsSection.DISPLAY, SettingsSection.COMPUTER -> SettingsGroup.PRESENTING
        SettingsSection.DEVICE, SettingsSection.APPEARANCE -> SettingsGroup.DEVICE
        SettingsSection.DIAGNOSTICS, SettingsSection.ABOUT -> SettingsGroup.HELP
    }

internal fun AppColors.groupTint(group: SettingsGroup, mode: AppMode): Color = when (group) {
    SettingsGroup.CONNECTION -> modeTint(mode)
    SettingsGroup.PRESENTING -> greekAccent
    SettingsGroup.DEVICE -> warning
    SettingsGroup.HELP -> amber
}

internal fun AppColors.toneColor(tone: ConnectionTone): Color = when (tone) {
    ConnectionTone.GOOD -> accent
    ConnectionTone.WARNING -> warning
    ConnectionTone.BAD -> danger
    ConnectionTone.PENDING -> muted
}

internal fun groupsOf(sections: List<SettingsSection>): List<Pair<SettingsGroup, List<SettingsSection>>> =
    sections.mapNotNull { section -> section.group?.let { it to section } }
        .groupBy({ it.first }, { it.second })
        .toList()
