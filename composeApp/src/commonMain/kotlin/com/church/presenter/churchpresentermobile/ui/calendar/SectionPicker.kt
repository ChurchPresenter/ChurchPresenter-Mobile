package com.church.presenter.churchpresentermobile.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.calendar_section_closing
import churchpresentermobile.composeapp.generated.resources.calendar_section_communion
import churchpresentermobile.composeapp.generated.resources.calendar_section_hint
import churchpresentermobile.composeapp.generated.resources.calendar_section_pre_service
import churchpresentermobile.composeapp.generated.resources.calendar_section_response
import churchpresentermobile.composeapp.generated.resources.calendar_section_word
import churchpresentermobile.composeapp.generated.resources.calendar_section_worship
import com.church.presenter.churchpresentermobile.model.SectionPalette
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Naming a section of the run of show, and giving it a color.
 *
 * Its own file rather than another tab beside the others: a section is the one thing the picker
 * adds that is not content -- there is nothing to search, only a name to type and a swatch to pick.
 */

private val SECTION_SUGGESTIONS: List<Pair<StringResource, String>> = listOf(
    Res.string.calendar_section_pre_service to SectionPalette.SKY,
    Res.string.calendar_section_worship to SectionPalette.BLUE,
    Res.string.calendar_section_word to SectionPalette.AMBER,
    Res.string.calendar_section_response to SectionPalette.GREEN,
    Res.string.calendar_section_communion to SectionPalette.VIOLET,
    Res.string.calendar_section_closing to SectionPalette.ROSE,
)

/** The usual section names, each with its color; the search field is the name for a new one. */
@Composable
internal fun SectionTab(
    query: String,
    color: String,
    onPick: (name: String, color: String) -> Unit,
    onColor: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SECTION_SUGGESTIONS.forEach { (nameRes, hex) ->
            val name = stringResource(nameRes)
            if (query.isBlank() || name.contains(query.trim(), ignoreCase = true)) {
                CalendarCard(selected = name.equals(query.trim(), ignoreCase = true), onClick = { onPick(name, hex) }) {
                    ColorDot(hex, size = 10.dp)
                    TitleText(name, size = 14, modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = LocalAppColors.current.accent,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        SwatchRow(color = color, onColor = onColor, modifier = Modifier.padding(top = 4.dp))
        HintText(stringResource(Res.string.calendar_section_hint))
    }
}

/** The section palette as round swatches, the chosen one ringed. */
@Composable
internal fun SwatchRow(color: String, onColor: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        SectionPalette.ALL.forEach { hex ->
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorOf(hex))
                    .border(
                        2.dp,
                        if (hex == color) LocalAppColors.current.text else colorOf(hex),
                        RoundedCornerShape(12.dp),
                    )
                    .clickable { onColor(hex) },
            )
        }
    }
}
