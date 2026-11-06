package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.tv_picker_failed
import churchpresentermobile.composeapp.generated.resources.tv_picker_hint
import churchpresentermobile.composeapp.generated.resources.tv_picker_needs_source
import churchpresentermobile.composeapp.generated.resources.tv_picker_showing
import churchpresentermobile.composeapp.generated.resources.tv_picker_stop
import churchpresentermobile.composeapp.generated.resources.tv_picker_title
import com.church.presenter.churchpresentermobile.network.createActionHttpClient
import com.church.presenter.churchpresentermobile.tv.FoundService
import com.church.presenter.churchpresentermobile.tv.ServiceFinder
import com.church.presenter.churchpresentermobile.tv.TV_SERVICE_TYPE
import com.church.presenter.churchpresentermobile.tv.TvSource
import com.church.presenter.churchpresentermobile.tv.clearTv
import com.church.presenter.churchpresentermobile.tv.showOnTv
import com.church.presenter.churchpresentermobile.ui.theme.AppDimens
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.TvPickerViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * The TVs on this network running ChurchPresenter; tapping one sends [source]
 * to it, so nobody has to find the TV's remote.
 *
 * Finds its own TVs and owns its own ViewModel per the project rule.
 *
 * @param source What a tap sends, or null when there is nothing to send yet —
 *   the phone's display server has no address until it is presenting.
 */
@Composable
fun TvPicker(
    source: TvSource?,
    modifier: Modifier = Modifier,
    /** Supplied by tests only; the picker owns its own otherwise. */
    providedViewModel: TvPickerViewModel? = null,
) {
    val viewModel: TvPickerViewModel = providedViewModel ?: viewModel(key = "tv_picker") {
        // Made here, not remembered: the ViewModel outlives this screen, and a
        // client closed with the screen would fail every send after reopening it.
        val client = createActionHttpClient()
        TvPickerViewModel(
            show = { tv, what -> showOnTv(client, tv, what) },
            clear = { tv -> clearTv(client, tv) },
            release = client::close,
        )
    }
    val finder = remember { ServiceFinder(TV_SERVICE_TYPE) }
    DisposableEffect(finder) {
        finder.start()
        onDispose { finder.stop() }
    }
    val tvs by finder.found.collectAsState()
    val showingOn by viewModel.showingOn.collectAsState()
    val failedOn by viewModel.failedOn.collectAsState()

    TvPickerContent(
        tvs = tvs,
        canSend = source != null,
        showingOn = showingOn,
        failedOn = failedOn,
        onSend = { tv -> source?.let { viewModel.send(tv, it) } },
        onStop = viewModel::stop,
        modifier = modifier,
    )
}

/** The list itself, separate so it can be tested without a network to browse. */
@Composable
internal fun TvPickerContent(
    tvs: List<FoundService>,
    canSend: Boolean,
    showingOn: String?,
    failedOn: String?,
    onSend: (FoundService) -> Unit,
    onStop: (FoundService) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppDimens.space8)) {
        Text(
            text = stringResource(Res.string.tv_picker_title),
            color = colors.text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        if (tvs.isEmpty()) {
            Text(
                text = stringResource(Res.string.tv_picker_hint),
                color = colors.muted,
                fontSize = 13.sp,
                modifier = Modifier.testTag(StandaloneTags.TVS_EMPTY),
            )
        } else if (!canSend) {
            Text(
                text = stringResource(Res.string.tv_picker_needs_source),
                color = colors.muted,
                fontSize = 13.sp,
                modifier = Modifier.testTag(StandaloneTags.TVS_NEED_SOURCE),
            )
        }
        tvs.forEach { tv ->
            TvRow(
                tv = tv,
                enabled = canSend,
                showing = tv.name == showingOn,
                failed = tv.name == failedOn,
                onSend = { onSend(tv) },
                onStop = { onStop(tv) },
            )
        }
    }
}

@Composable
private fun TvRow(
    tv: FoundService,
    enabled: Boolean,
    showing: Boolean,
    failed: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(StandaloneTags.tv(tv.name))
            .clip(RoundedCornerShape(AppDimens.radiusCard))
            .background(if (showing) colors.accentTint else colors.surface)
            .clickable(enabled = enabled, onClick = onSend)
            .padding(AppDimens.space12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space12),
    ) {
        Icon(
            imageVector = Icons.Outlined.Tv,
            contentDescription = null,
            tint = if (showing) colors.accent else colors.muted,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = tv.name, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            when {
                failed -> Text(
                    text = stringResource(Res.string.tv_picker_failed),
                    color = colors.danger,
                    fontSize = 12.sp,
                    modifier = Modifier.testTag(StandaloneTags.tvFailed(tv.name)),
                )
                showing -> Text(
                    text = stringResource(Res.string.tv_picker_showing),
                    color = colors.accent,
                    fontSize = 12.sp,
                )
            }
        }
        if (showing) {
            Text(
                text = stringResource(Res.string.tv_picker_stop),
                color = colors.accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .testTag(StandaloneTags.tvStop(tv.name))
                    .clickable(onClick = onStop)
                    .padding(horizontal = AppDimens.space8, vertical = 4.dp),
            )
        }
    }
}
