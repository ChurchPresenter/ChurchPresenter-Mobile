package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.standalone_look
import churchpresentermobile.composeapp.generated.resources.standalone_backdrop
import churchpresentermobile.composeapp.generated.resources.standalone_backdrop_black
import churchpresentermobile.composeapp.generated.resources.standalone_backdrop_gradient
import churchpresentermobile.composeapp.generated.resources.standalone_backdrop_image
import churchpresentermobile.composeapp.generated.resources.standalone_blank
import churchpresentermobile.composeapp.generated.resources.standalone_empty_body
import churchpresentermobile.composeapp.generated.resources.standalone_empty_title
import churchpresentermobile.composeapp.generated.resources.standalone_live
import churchpresentermobile.composeapp.generated.resources.standalone_next
import churchpresentermobile.composeapp.generated.resources.standalone_no_output
import churchpresentermobile.composeapp.generated.resources.standalone_output_count
import churchpresentermobile.composeapp.generated.resources.standalone_prev
import churchpresentermobile.composeapp.generated.resources.standalone_preview
import churchpresentermobile.composeapp.generated.resources.standalone_size_large
import churchpresentermobile.composeapp.generated.resources.standalone_size_medium
import churchpresentermobile.composeapp.generated.resources.standalone_size_small
import churchpresentermobile.composeapp.generated.resources.standalone_slide_position
import churchpresentermobile.composeapp.generated.resources.standalone_text_size
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.SlideBackdrop
import com.church.presenter.churchpresentermobile.model.SlideTextSize
import com.church.presenter.churchpresentermobile.present.SinkStatus
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.present.SinkRegistry
import com.church.presenter.churchpresentermobile.ui.OverlineRow
import com.church.presenter.churchpresentermobile.ui.SegmentedControl
import com.church.presenter.churchpresentermobile.ui.theme.AppDimens
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.StandaloneViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.stringResource

/**
 * The live controller: the phone drives the screen, it never mirrors it.
 *
 * A port of the phone half of `docs/cast-mockup.html` — source list, backdrop
 * and text-size pickers, Prev/Next, Blank as a first-class control, a Live
 * toggle, and a 16:9 preview of exactly what the audience sees.
 *
 * Owns its own ViewModel per the project rule; the shared [StandaloneEngine]
 * and [SinkRegistry] are plain collaborators, not ViewModels, so passing them
 * in is safe and keeps a single source of truth for what is projected.
 */
@Composable
fun StandaloneControllerScreen(
    engine: StandaloneEngine,
    registry: SinkRegistry,
    settings: AppSettings,
    modifier: Modifier = Modifier,
) {
    val viewModel: StandaloneViewModel = viewModel(key = "standalone") {
        StandaloneViewModel(engine, registry, settings)
    }
    val colors = LocalAppColors.current

    val deck by viewModel.deck.collectAsState()
    val index by viewModel.index.collectAsState()
    val slide by viewModel.currentSlide.collectAsState()
    val isBlank by viewModel.isBlank.collectAsState()
    val isLive by viewModel.isLive.collectAsState()
    val textSize by viewModel.textSize.collectAsState()
    val backdrop by viewModel.backdrop.collectAsState()
    val sinks by viewModel.sinks.collectAsState()
    val theme by viewModel.theme.collectAsState()
    var showOutputs by remember { mutableStateOf(false) }
    var showLook by remember { mutableStateOf(false) }

    if (showOutputs) {
        OutputTargetsSheet(sinks = sinks, onDismiss = { showOutputs = false })
    }

    if (showLook) {
        LookSheet(
            theme = theme,
            onThemeChange = viewModel::updateTheme,
            onDismiss = { showLook = false },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = AppDimens.space16),
        verticalArrangement = Arrangement.spacedBy(AppDimens.space14),
    ) {
        OutputChip(sinks, onClick = { showOutputs = true })

        SlidePreview(slide, deck.slides.size, index)

        if (deck.isEmpty) {
            EmptyDeckHint()
        } else {
            SectionList(
                title = deck.title,
                slides = deck.slides,
                selectedIndex = index,
                onSelect = viewModel::showSlide,
            )
        }

        // The gradient's colours live behind here, with the rest of the look — off the live
        // surface, so nothing pushes Next and Blank further down the screen.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OverlineRow(stringResource(Res.string.standalone_backdrop), modifier = Modifier.weight(1f))
            Text(
                text = stringResource(Res.string.standalone_look),
                color = colors.accent,
                fontSize = 12.sp,
                modifier = Modifier.clickable { showLook = true },
            )
        }
        SegmentedControl(
            options = listOf(
                stringResource(Res.string.standalone_backdrop_gradient),
                stringResource(Res.string.standalone_backdrop_image),
                stringResource(Res.string.standalone_backdrop_black),
            ),
            selectedIndex = BACKDROPS.indexOf(backdrop).coerceAtLeast(0),
            onSelect = { viewModel.setBackdrop(BACKDROPS[it]) },
        )

        OverlineRow(stringResource(Res.string.standalone_text_size))
        SegmentedControl(
            options = listOf(
                stringResource(Res.string.standalone_size_small),
                stringResource(Res.string.standalone_size_medium),
                stringResource(Res.string.standalone_size_large),
            ),
            selectedIndex = TEXT_SIZES.indexOf(textSize).coerceAtLeast(0),
            onSelect = { viewModel.setTextSize(TEXT_SIZES[it]) },
        )

        TransportRow(
            canStepBack = index > 0,
            canStepForward = index < deck.slides.lastIndex,
            onPrevious = viewModel::previous,
            onNext = viewModel::next,
        )

        StateRow(
            isBlank = isBlank,
            isLive = isLive,
            onToggleBlank = viewModel::toggleBlank,
            onToggleLive = { viewModel.setLive(!isLive) },
        )
    }
}

/** "Casting to Sanctuary TV" — or an honest note that nothing is connected yet. */
@Composable
private fun OutputChip(sinks: List<SinkStatus>, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val attached = sinks.filter { it.isAttached }
    val connected = attached.isNotEmpty()
    val tint = if (connected) colors.accent else colors.muted

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(AppDimens.radiusPill))
            .background(if (connected) colors.accentTint else colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = AppDimens.space12, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = if (connected) Icons.Filled.CastConnected else Icons.Filled.Cast,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = when {
                attached.size == 1 -> attached.first().displayName
                attached.size > 1 -> stringResource(Res.string.standalone_output_count, attached.size.toString())
                else -> stringResource(Res.string.standalone_no_output)
            },
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** A 16:9 window onto the real renderer — not a mock-up of it. */
@Composable
private fun SlidePreview(slide: Slide, total: Int, index: Int) {
    val colors = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OverlineRow(
            label = stringResource(Res.string.standalone_preview),
            trailing = if (total > 0) {
                stringResource(
                    Res.string.standalone_slide_position,
                    (index + 1).toString(),
                    total.toString(),
                )
            } else null,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(AppDimens.radiusChip))
                .border(1.dp, colors.border, RoundedCornerShape(AppDimens.radiusChip)),
        ) {
            StandaloneOutputScreen(slide)
        }
    }
}

/** The steppable list of sections/verses — the operator's main working surface. */
@Composable
private fun SectionList(
    title: String,
    slides: List<Slide>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val colors = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.space8)) {
        OverlineRow(title)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 230.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(slides) { i, slide ->
                val active = i == selectedIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (active) colors.accent else Color.Transparent)
                        .clickable { onSelect(i) }
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = (i + 1).toString(),
                        color = if (active) colors.onAccent.copy(alpha = 0.6f) else colors.muted,
                        fontSize = 11.sp,
                        modifier = Modifier.size(width = 26.dp, height = 16.dp),
                    )
                    Text(
                        text = slide.body.lineSequence().firstOrNull()?.trim().orEmpty(),
                        color = if (active) colors.onAccent else colors.text,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDeckHint() {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.radiusCard))
            .background(colors.surface)
            .padding(AppDimens.space16),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(Res.string.standalone_empty_title),
            color = colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(Res.string.standalone_empty_body),
            color = colors.muted,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun TransportRow(
    canStepBack: Boolean,
    canStepForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space8),
    ) {
        ControlButton(
            label = stringResource(Res.string.standalone_prev),
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            enabled = canStepBack,
            onClick = onPrevious,
            modifier = Modifier.weight(1f),
        )
        ControlButton(
            label = stringResource(Res.string.standalone_next),
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            enabled = canStepForward,
            onClick = onNext,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StateRow(
    isBlank: Boolean,
    isLive: Boolean,
    onToggleBlank: () -> Unit,
    onToggleLive: () -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = AppDimens.space16),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.space8),
    ) {
        ControlButton(
            label = stringResource(Res.string.standalone_blank),
            icon = Icons.Filled.HideSource,
            onClick = onToggleBlank,
            modifier = Modifier.weight(1f),
            background = if (isBlank) Color.Black else colors.surface,
            contentColor = if (isBlank) Color.White else colors.text,
        )
        ControlButton(
            label = stringResource(Res.string.standalone_live),
            icon = Icons.Filled.Sensors,
            onClick = onToggleLive,
            modifier = Modifier.weight(1f),
            background = if (isLive) colors.danger else colors.surface,
            contentColor = if (isLive) Color.White else colors.text,
        )
    }
}

@Composable
private fun ControlButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    background: Color? = null,
    contentColor: Color? = null,
) {
    val colors = LocalAppColors.current
    val bg = background ?: colors.surface
    val fg = (contentColor ?: colors.text).copy(alpha = if (enabled) 1f else 0.35f)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(bg)
            .border(1.dp, colors.border, RoundedCornerShape(11.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/** Declaration order must match the segmented-control label order above. */
private val BACKDROPS = listOf(SlideBackdrop.GRADIENT, SlideBackdrop.IMAGE, SlideBackdrop.BLACK)
private val TEXT_SIZES = listOf(SlideTextSize.SMALL, SlideTextSize.MEDIUM, SlideTextSize.LARGE)
