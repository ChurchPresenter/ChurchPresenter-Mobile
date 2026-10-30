package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.cd_live_collapse
import churchpresentermobile.composeapp.generated.resources.cd_live_expand
import churchpresentermobile.composeapp.generated.resources.cd_live_open
import churchpresentermobile.composeapp.generated.resources.cd_live_screens
import churchpresentermobile.composeapp.generated.resources.live_state_blank
import churchpresentermobile.composeapp.generated.resources.live_state_live
import churchpresentermobile.composeapp.generated.resources.live_state_off
import churchpresentermobile.composeapp.generated.resources.standalone_blank
import churchpresentermobile.composeapp.generated.resources.standalone_next
import churchpresentermobile.composeapp.generated.resources.standalone_prev
import churchpresentermobile.composeapp.generated.resources.standalone_slide_position
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.present.PhotoLibrary
import com.church.presenter.churchpresentermobile.present.SinkRegistry
import com.church.presenter.churchpresentermobile.present.SinkStatus
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.ui.SCREEN_ASPECT_RATIO
import com.church.presenter.churchpresentermobile.ui.theme.AppDimens
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.ui.verticalScrollWithScrollbar
import com.church.presenter.churchpresentermobile.viewmodel.StandaloneViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * The live controls, pinned above the tab bar on every standalone tab.
 *
 * Standalone used to keep these on a Present tab, which meant leaving the song
 * to step through it. Songs and Bible already put a verse live with a tap; what
 * they lacked was a way to see what the room sees and to step or blank without
 * hunting for it. The bar is that, and only that: a thumbnail of the real
 * output, the deck's place, ◀ ▶, Blank, and whether any screen is attached.
 *
 * Drawn only while something is loaded — an empty bar would be a strip of
 * disabled buttons above every screen. Tap the thumbnail for the full live view;
 * the handle along its top folds the bar into [LiveChip] in the header.
 *
 * Owns its own ViewModel per the project rule. The engine and registry are plain
 * collaborators shared with every other standalone screen, so the bar always
 * shows what is actually projected, whoever put it there.
 *
 * @param onCollapse Folds the bar away; the shell then shows [LiveChip].
 * @param onOpenScreens Opens Settings → Display, where screens are chosen.
 */
@Composable
fun LiveBar(
    engine: StandaloneEngine,
    registry: SinkRegistry,
    settings: AppSettings,
    onCollapse: () -> Unit,
    onOpenScreens: () -> Unit,
    modifier: Modifier = Modifier,
    photos: PhotoLibrary? = null,
    /** Supplied by tests only; the bar owns its own otherwise. */
    providedViewModel: StandaloneViewModel? = null,
) {
    val viewModel: StandaloneViewModel = providedViewModel
        ?: viewModel(key = "standalone") { StandaloneViewModel(engine, registry, settings, photos) }
    val deck by viewModel.deck.collectAsState()
    val index by viewModel.index.collectAsState()
    val slide by viewModel.currentSlide.collectAsState()
    val isBlank by viewModel.isBlank.collectAsState()
    val isLive by viewModel.isLive.collectAsState()
    val sinks by viewModel.sinks.collectAsState()
    var showLiveView by remember { mutableStateOf(false) }

    if (showLiveView) LiveViewSheet(viewModel, onDismiss = { showLiveView = false })
    if (deck.isEmpty) return

    val colors = LocalAppColors.current
    Column(modifier = modifier.fillMaxWidth().background(colors.background).testTag(StandaloneTags.LIVE_BAR)) {
        HorizontalDivider(color = colors.borderSubtle)
        // A handle rather than a sixth button: a phone's width is already spent
        // on the thumbnail, the title and the four controls that matter.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(StandaloneTags.LIVE_BAR_COLLAPSE)
                .clickable(onClick = onCollapse),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(Res.string.cd_live_collapse),
                tint = colors.muted,
                modifier = Modifier.size(18.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = AppDimens.space12, end = AppDimens.space12, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.space8),
        ) {
            val openLabel = stringResource(Res.string.cd_live_open)
            Thumbnail(
                slide = slide,
                width = 72.dp,
                modifier = Modifier
                    .testTag(StandaloneTags.LIVE_BAR_PREVIEW)
                    .semantics { contentDescription = openLabel }
                    .clickable { showLiveView = true },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deck.title,
                    color = colors.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    StateLabel(isLive = isLive, isBlank = isBlank)
                    Text(
                        text = stringResource(
                            Res.string.standalone_slide_position,
                            (index + 1).toString(),
                            deck.slides.size.toString(),
                        ),
                        color = colors.muted,
                        fontSize = 11.sp,
                    )
                }
            }
            BarButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                label = stringResource(Res.string.standalone_prev),
                enabled = index > 0,
                onClick = viewModel::previous,
                modifier = Modifier.testTag(StandaloneTags.LIVE_BAR_PREV),
            )
            BarButton(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                label = stringResource(Res.string.standalone_next),
                enabled = index < deck.slides.lastIndex,
                onClick = viewModel::next,
                modifier = Modifier.testTag(StandaloneTags.LIVE_BAR_NEXT),
            )
            BarButton(
                icon = Icons.Filled.HideSource,
                label = stringResource(Res.string.standalone_blank),
                onClick = viewModel::toggleBlank,
                active = isBlank,
                modifier = Modifier.testTag(StandaloneTags.LIVE_BAR_BLANK),
            )
            ScreensDot(sinks, onOpenScreens, Modifier.testTag(StandaloneTags.LIVE_BAR_STATUS))
        }
    }
}

/**
 * The live bar folded into the header, beside the settings gear.
 *
 * Costs no height at all, and still answers the one question that matters
 * mid-service — is something up, and is it blanked — with a thumbnail of the
 * output and a dot. Tapping it brings the bar back. Draws nothing while nothing
 * is loaded, like the bar.
 */
@Composable
fun LiveChip(
    engine: StandaloneEngine,
    registry: SinkRegistry,
    settings: AppSettings,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    photos: PhotoLibrary? = null,
    /** Supplied by tests only; the chip owns its own otherwise. */
    providedViewModel: StandaloneViewModel? = null,
) {
    val viewModel: StandaloneViewModel = providedViewModel
        ?: viewModel(key = "standalone") { StandaloneViewModel(engine, registry, settings, photos) }
    val deck by viewModel.deck.collectAsState()
    val slide by viewModel.currentSlide.collectAsState()
    val isBlank by viewModel.isBlank.collectAsState()
    val isLive by viewModel.isLive.collectAsState()
    if (deck.isEmpty) return

    val colors = LocalAppColors.current
    val label = stringResource(Res.string.cd_live_expand)
    Row(
        modifier = modifier
            .testTag(StandaloneTags.LIVE_CHIP)
            .semantics { contentDescription = label }
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
            .clickable(onClick = onExpand)
            .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Thumbnail(slide = slide, width = 46.dp)
        StateLabel(isLive = isLive, isBlank = isBlank)
    }
}

/**
 * Everything the old Present tab's live half had, one tap from any tab: the
 * output at full width, the deck's sections, Prev/Next, Blank, Clear and Live.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveViewSheet(viewModel: StandaloneViewModel, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = LocalAppColors.current.sheetBackground,
    ) {
        LiveViewContent(viewModel, onCleared = onDismiss)
    }
}

/**
 * The live view without the sheet around it.
 *
 * `internal` so a test can drive it directly: whether the preview and the
 * section list agree with the output is the thing worth asserting, and a
 * `ModalBottomSheet`'s animation is in the way of it.
 *
 * @param onCleared Called after Clear, which puts the deck down — and with it
 *   the bar this view opened from.
 */
@Composable
internal fun LiveViewContent(viewModel: StandaloneViewModel, onCleared: () -> Unit = {}) {
    val deck by viewModel.deck.collectAsState()
    val index by viewModel.index.collectAsState()
    val slide by viewModel.currentSlide.collectAsState()
    val isBlank by viewModel.isBlank.collectAsState()
    val isLive by viewModel.isLive.collectAsState()

    run {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(StandaloneTags.LIVE_VIEW)
                .navigationBarsPadding()
                .verticalScrollWithScrollbar(rememberScrollState())
                .padding(horizontal = AppDimens.space20, vertical = AppDimens.space8),
            verticalArrangement = Arrangement.spacedBy(AppDimens.space12),
        ) {
            SlidePreview(slide, deck.slides.size, index, Modifier.testTag(StandaloneTags.PREVIEW))
            TransportRow(
                canStepBack = index > 0,
                canStepForward = index < deck.slides.lastIndex,
                onPrevious = viewModel::previous,
                onNext = viewModel::next,
            )
            StateRow(
                isBlank = isBlank,
                isLive = isLive,
                hasContent = deck.slides.isNotEmpty(),
                onToggleBlank = viewModel::toggleBlank,
                onClear = {
                    viewModel.clear()
                    onCleared()
                },
            ) { viewModel.setLive(!isLive) }
            if (deck.isEmpty) EmptyDeckHint(Modifier.testTag(StandaloneTags.EMPTY_DECK))
            else SectionList(deck.title, deck.slides, index, viewModel::showSlide)
        }
    }
}

/** The real renderer, small — the same pixels the room sees, not a mock-up. */
@Composable
private fun Thumbnail(slide: Slide, width: Dp, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .width(width)
            .aspectRatio(SCREEN_ASPECT_RATIO)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, colors.border, RoundedCornerShape(6.dp)),
    ) {
        StandaloneOutputScreen(slide)
    }
}

/** LIVE, BLANK or OFF — what the room is seeing, in one word and a colour. */
@Composable
private fun StateLabel(isLive: Boolean, isBlank: Boolean) {
    val colors = LocalAppColors.current
    val (text, tint) = when {
        !isLive -> stringResource(Res.string.live_state_off) to colors.muted
        isBlank -> stringResource(Res.string.live_state_blank) to colors.text
        else -> stringResource(Res.string.live_state_live) to colors.danger
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(tint))
        Text(text = text, color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
    }
}

/**
 * Whether any screen is attached, as a dot that opens Settings → Display.
 *
 * Filled when something is showing the slide; hollow when nothing is, which is
 * the state worth a glance before the service starts.
 */
@Composable
private fun ScreensDot(sinks: List<SinkStatus>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val connected = sinks.any { it.isAttached }
    val label = stringResource(Res.string.cd_live_screens)
    Box(
        modifier = modifier
            .size(34.dp)
            .semantics { contentDescription = label }
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(if (connected) colors.accent else Color.Transparent)
                .border(2.dp, if (connected) colors.accent else colors.muted, CircleShape),
        )
    }
}

/**
 * One of the bar's buttons. Not [com.church.presenter.churchpresentermobile.ui.IconTileButton],
 * which has no disabled state — ◀ at the first slide must read as unavailable,
 * not as a button that does nothing.
 */
@Composable
private fun BarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = false,
) {
    val colors = LocalAppColors.current
    val tint = when {
        active -> Color.White
        enabled -> colors.text
        else -> colors.text.copy(alpha = 0.3f)
    }
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) Color.Black else colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
    }
}
