package com.church.presenter.churchpresentermobile.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.tv_output_key
import churchpresentermobile.composeapp.generated.resources.tv_output_needs_key
import churchpresentermobile.composeapp.generated.resources.tv_output_number
import churchpresentermobile.composeapp.generated.resources.tv_outputs_checking
import churchpresentermobile.composeapp.generated.resources.tv_outputs_none
import churchpresentermobile.composeapp.generated.resources.tv_outputs_retry
import com.church.presenter.churchpresentermobile.ui.layoutSafe
import com.church.presenter.churchpresentermobile.network.createActionHttpClient
import com.church.presenter.churchpresentermobile.tv.DesktopOutput
import com.church.presenter.churchpresentermobile.tv.TvSource
import com.church.presenter.churchpresentermobile.tv.findOutputs
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import io.ktor.client.HttpClient
import org.jetbrains.compose.resources.stringResource

/**
 * Which of a desktop's outputs to show. Goes straight on when there is only one
 * to choose; says what to do on the computer when there are none, since a
 * desktop only serves an output set to Browser Source.
 *
 * @param find Asks the desktop what it has. A seam so each answer can be tested
 *   without a desktop on the network.
 */
@Composable
internal fun OutputPicker(
    desktop: TvSource,
    onPick: (TvSource) -> Unit,
    find: suspend (TvSource, HttpClient) -> List<DesktopOutput> = { source, http -> findOutputs(source, http) },
) {
    val colors = LocalAppColors.current
    val client = remember { createActionHttpClient() }
    DisposableEffect(client) { onDispose { client.close() } }
    var outputs by remember(desktop) { mutableStateOf<List<DesktopOutput>?>(null) }
    var key by remember(desktop) { mutableStateOf(desktop.apiKey) }
    var attempt by remember(desktop) { mutableStateOf(0) }
    LaunchedEffect(desktop, attempt) {
        val found = find(desktop, client)
        outputs = found
        found.singleOrNull()?.takeUnless { it.needsKey }?.let { onPick(desktop.copy(output = it.number)) }
    }
    Column(
        modifier = Modifier.fillMaxSize().background(colors.background).padding(64.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(desktop.name, color = colors.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        val found = outputs
        when {
            found == null -> Text(stringResource(Res.string.tv_outputs_checking), color = colors.text, fontSize = 18.sp)
            found.isEmpty() -> {
                Text(
                    stringResource(Res.string.tv_outputs_none),
                    color = colors.text,
                    fontSize = 18.sp,
                    modifier = Modifier.testTag(TvTags.NO_OUTPUTS),
                )
                TvCard(
                    title = stringResource(Res.string.tv_outputs_retry),
                    body = null,
                    onClick = { attempt++ },
                    modifier = Modifier.width(380.dp).testTag(TvTags.OUTPUTS_RETRY),
                )
            }
            else -> OutputList(found, key, onKey = { key = it }) { onPick(desktop.copy(output = it, apiKey = key)) }
        }
    }
}

@Composable
private fun OutputList(
    outputs: List<DesktopOutput>,
    key: String,
    onKey: (String) -> Unit,
    onPick: (Int) -> Unit,
) {
    outputs.forEach { output ->
        TvCard(
            title = stringResource(Res.string.tv_output_number, output.number),
            body = if (output.needsKey) stringResource(Res.string.tv_output_needs_key) else null,
            onClick = { onPick(output.number) },
            modifier = Modifier.width(380.dp).testTag(TvTags.output(output.number)),
        )
    }
    if (outputs.any { it.needsKey }) {
        OutlinedTextField(
            value = key.layoutSafe(),
            onValueChange = { onKey(it.layoutSafe()) },
            singleLine = true,
            label = { Text(stringResource(Res.string.tv_output_key)) },
            modifier = Modifier.width(380.dp).testTag(TvTags.OUTPUT_KEY),
        )
    }
}
