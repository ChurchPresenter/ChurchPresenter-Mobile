package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.action_clear_display
import churchpresentermobile.composeapp.generated.resources.action_go_live
import churchpresentermobile.composeapp.generated.resources.standalone_no_output
import churchpresentermobile.composeapp.generated.resources.web_not_a_link
import churchpresentermobile.composeapp.generated.resources.web_refuses_framing
import churchpresentermobile.composeapp.generated.resources.web_url_placeholder
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.ui.OutlineActionButton
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.LocalWebViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * A link on the audience screen, put there by this device.
 *
 * The remote Web viewer hands a URL to a desktop. Standalone has no desktop, so
 * the phone shows the page on its own outputs. A link that looks like a video
 * is played rather than framed — operators paste both into the same box.
 */
@Composable
fun LocalWebScreen(
    presenter: StandaloneEngine?,
    hasOutput: Boolean,
    modifier: Modifier = Modifier,
    /** Supplied by tests only; the screen owns its own otherwise. */
    providedViewModel: LocalWebViewModel? = null,
) {
    val colors = LocalAppColors.current
    val vm: LocalWebViewModel = providedViewModel
        ?: viewModel(key = "local_web") { LocalWebViewModel(presenter) }
    val url by vm.url.collectAsState()
    val canProject by vm.canProject.collectAsState()
    val projecting by vm.projecting.collectAsState()
    val refusedByFraming by vm.refusedByFraming.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().background(colors.background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = url,
            onValueChange = { vm.setUrl(it) },
            placeholder = { Text("https://" + stringResource(Res.string.web_url_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            modifier = Modifier.fillMaxWidth().testTag(StandaloneTags.WEB_URL),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlineActionButton(
                label = stringResource(Res.string.action_go_live),
                icon = Icons.Filled.Wifi,
                onClick = { if (canProject) vm.project() },
                modifier = Modifier.weight(1f).testTag(StandaloneTags.WEB_GO_LIVE),
            )
            if (projecting != null) {
                OutlineActionButton(
                    label = stringResource(Res.string.action_clear_display),
                    icon = Icons.Outlined.Delete,
                    onClick = { vm.clearDisplay() },
                    modifier = Modifier.weight(1f).testTag(StandaloneTags.WEB_CLEAR),
                )
            }
        }

        // The button did nothing at all for an address it could not use, which is
        // indistinguishable from the feature being broken.
        if (url.isNotBlank() && !canProject) {
            Text(
                text = stringResource(Res.string.web_not_a_link),
                color = colors.danger,
                fontSize = 12.sp,
                modifier = Modifier.testTag(StandaloneTags.WEB_NOT_A_LINK),
            )
        }

        if (!hasOutput) {
            Text(
                stringResource(Res.string.standalone_no_output),
                color = colors.muted,
                fontSize = 12.sp,
                modifier = Modifier.testTag(StandaloneTags.WEB_NO_OUTPUT),
            )
        }

        // The browser screen frames the page, and a great many sites forbid that.
        // Nothing here can override it — the browser enforces it on the site's
        // behalf — so the operator is told which output is affected and which
        // still works, rather than being left with a browser error on the wall.
        refusedByFraming?.let { host ->
            Text(
                text = stringResource(Res.string.web_refuses_framing, host),
                color = colors.amber,
                fontSize = 12.sp,
                modifier = Modifier.testTag(StandaloneTags.WEB_REFUSES_FRAMING),
            )
        }

        projecting?.let { live ->
            Text(
                text = live,
                color = colors.muted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.testTag(StandaloneTags.WEB_LIVE_URL),
            )
        }
    }
}
