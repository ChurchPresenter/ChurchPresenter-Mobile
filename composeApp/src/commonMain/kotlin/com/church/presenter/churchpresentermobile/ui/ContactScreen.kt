package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.contact_email_label
import churchpresentermobile.composeapp.generated.resources.contact_error
import churchpresentermobile.composeapp.generated.resources.contact_message_label
import churchpresentermobile.composeapp.generated.resources.contact_name_label
import churchpresentermobile.composeapp.generated.resources.contact_network_error
import churchpresentermobile.composeapp.generated.resources.contact_open_browser
import churchpresentermobile.composeapp.generated.resources.contact_rate_limited_browser
import churchpresentermobile.composeapp.generated.resources.contact_send
import churchpresentermobile.composeapp.generated.resources.contact_sending
import churchpresentermobile.composeapp.generated.resources.contact_sent
import churchpresentermobile.composeapp.generated.resources.contact_type_bug
import churchpresentermobile.composeapp.generated.resources.contact_type_feature
import churchpresentermobile.composeapp.generated.resources.contact_type_feedback
import churchpresentermobile.composeapp.generated.resources.contact_type_label
import churchpresentermobile.composeapp.generated.resources.contact_type_testimonial
import com.church.presenter.churchpresentermobile.network.ContactReporter
import com.church.presenter.churchpresentermobile.openUrl
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.ContactType
import com.church.presenter.churchpresentermobile.viewmodel.ContactViewModel
import com.church.presenter.churchpresentermobile.viewmodel.SendStatus
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Send a feature request, bug report, feedback or testimonial to the
 * ChurchPresenter team. The mobile counterpart of the desktop's Contact Us
 * dialog, feeding the same inbox.
 *
 * Available in both modes: the endpoint is a public HTTPS one, so this needs no
 * desktop — only an internet connection. The browser button is always offered,
 * both as the rate-limit escape hatch and for anyone who would rather use the
 * public web form.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContactScreen(
    modifier: Modifier = Modifier,
    /**
     * Injected only by tests, matching the seam [SongsTable] and
     * [PicturesScreen] already use. The screen still owns its ViewModel in every
     * real caller — this is how the send path is reachable without a socket.
     */
    providedViewModel: ContactViewModel? = null,
) {
    val colors = LocalAppColors.current
    val vm: ContactViewModel = providedViewModel
        ?: viewModel(key = "contact") { ContactViewModel() }

    val type by vm.type.collectAsState()
    val name by vm.name.collectAsState()
    val email by vm.email.collectAsState()
    val message by vm.message.collectAsState()
    val status by vm.status.collectAsState()

    // Captured in composable scope so the ViewModel can report without resource lookups.
    val errorText = stringResource(Res.string.contact_error)
    val networkText = stringResource(Res.string.contact_network_error)
    val rateLimitedText = stringResource(Res.string.contact_rate_limited_browser)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(Res.string.contact_type_label),
            color = colors.muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            typeLabels.forEach { (value, label) ->
                FilterChip(
                    selected = type == value,
                    onClick = { vm.setType(value) },
                    label = { Text(stringResource(label)) },
                    modifier = Modifier.testTag(UiTags.contactType(value.key)),
                )
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { vm.setName(it) },
            label = { Text(stringResource(Res.string.contact_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag(UiTags.CONTACT_NAME),
        )
        OutlinedTextField(
            value = email,
            onValueChange = { vm.setEmail(it) },
            label = { Text(stringResource(Res.string.contact_email_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().testTag(UiTags.CONTACT_EMAIL),
        )
        OutlinedTextField(
            value = message,
            onValueChange = { vm.setMessage(it) },
            label = { Text(stringResource(Res.string.contact_message_label)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp).testTag(UiTags.CONTACT_MESSAGE),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlineActionButton(
                label = if (status is SendStatus.Sending) stringResource(Res.string.contact_sending)
                        else stringResource(Res.string.contact_send),
                icon = Icons.Filled.Send,
                onClick = { vm.send(errorText, networkText, rateLimitedText) },
                modifier = Modifier.weight(1f).testTag(UiTags.CONTACT_SEND),
            )
            OutlineActionButton(
                label = stringResource(Res.string.contact_open_browser),
                icon = Icons.Outlined.OpenInNew,
                onClick = { openUrl(ContactReporter.WEB_CONTACT_URL) },
                modifier = Modifier.weight(1f).testTag(UiTags.CONTACT_OPEN_BROWSER),
            )
        }

        when (val s = status) {
            SendStatus.Sent -> Text(
                text = stringResource(Res.string.contact_sent),
                color = colors.accent,
                fontSize = 13.sp,
                modifier = Modifier.testTag(UiTags.CONTACT_SENT),
            )
            is SendStatus.Error -> Text(
                text = s.text,
                color = colors.danger,
                fontSize = 13.sp,
                modifier = Modifier.testTag(UiTags.CONTACT_ERROR),
            )
            else -> Unit
        }
    }
}

/** The four kinds the server accepts, paired with their localized labels. */
private val typeLabels: List<Pair<ContactType, StringResource>> = listOf(
    ContactType.FEATURE to Res.string.contact_type_feature,
    ContactType.FEEDBACK to Res.string.contact_type_feedback,
    ContactType.TESTIMONIAL to Res.string.contact_type_testimonial,
    ContactType.BUG to Res.string.contact_type_bug,
)
