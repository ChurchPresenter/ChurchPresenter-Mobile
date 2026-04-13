package com.church.presenter.churchpresentermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.presentation_loading_error
import churchpresentermobile.composeapp.generated.resources.presentation_no_items
import churchpresentermobile.composeapp.generated.resources.presentation_retry
import churchpresentermobile.composeapp.generated.resources.presentation_slides
import coil3.ImageLoader
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Scale
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.Presentation
import com.church.presenter.churchpresentermobile.model.PresentationSlide
import com.church.presenter.churchpresentermobile.viewmodel.PresentationsViewModel
import org.jetbrains.compose.resources.stringResource

private const val SLIDE_COLUMNS = 2

/**
 * Presentations tab screen. Shows each presentation's slides as a scrollable list.
 * Tapping a slide selects that presentation on the server.
 *
 * @param appSettings              Shared [AppSettings] used to create and configure [PresentationsViewModel].
 * @param settingsSaveToken        Incremented each time settings are saved; triggers a service rebuild + reload.
 * @param imageLoader              Coil [ImageLoader] configured with the app's HTTP client (SSL bypass).
 * @param pendingNavPresentationId When non-null, the screen loads this presentation by ID and scrolls to it.
 * @param onPendingNavHandled      Called once the pending navigation has been applied so the parent can clear it.
 * @param modifier                 The modifier to apply to this composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresentationScreen(
    appSettings: AppSettings,
    isDemoMode: Boolean = false,
    settingsSaveToken: Int,
    imageLoader: ImageLoader,
    pendingNavPresentationId: String? = null,
    onPendingNavHandled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: PresentationsViewModel = viewModel(key = isDemoMode.toString()) { PresentationsViewModel(appSettings, isDemoMode) }

    // React to settings changes – rebuild the service and reload
    LaunchedEffect(settingsSaveToken) {
        if (settingsSaveToken > 0) viewModel.onSettingsSaved()
    }

    // React to schedule navigation – load the specific presentation by ID
    LaunchedEffect(pendingNavPresentationId) {
        if (!pendingNavPresentationId.isNullOrBlank()) {
            viewModel.navigateTo(pendingNavPresentationId)
            onPendingNavHandled()
        }
    }

    val presentations by viewModel.presentations.collectAsState()
    val selectedPresentation by viewModel.selectedPresentation.collectAsState()
    val selectedSlideIndex by viewModel.selectedSlideIndex.collectAsState()
    val pendingScrollToId by viewModel.pendingScrollToId.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isProjecting by viewModel.isProjecting.collectAsState()
    val scheduleAdded by viewModel.scheduleAdded.collectAsState()

    val listState = rememberLazyListState()

    // Scroll to the navigated presentation once the list updates
    LaunchedEffect(pendingScrollToId, presentations) {
        val targetId = pendingScrollToId ?: return@LaunchedEffect
        if (presentations.isNotEmpty()) {
            val targetIndex = presentations.indexOfFirst { it.id == targetId }
            if (targetIndex >= 0) {
                // Each presentation occupies (1 header + N slide rows + 1 divider) items.
                // The header is always the first item for that presentation — scroll to it.
                var flatIndex = 0
                for (i in 0 until targetIndex) {
                    val p = presentations[i]
                    flatIndex += 1 + (p.slides?.size?.let { (it + 1) / SLIDE_COLUMNS } ?: 0) + 1
                }
                listState.animateScrollToItem(flatIndex)
            }
            viewModel.onPendingScrollHandled()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {

        // ── Error banner ──────────────────────────────────────────────
        if (error != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = error ?: stringResource(Res.string.presentation_loading_error),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.loadPresentations() }) {
                        Text(
                            text = stringResource(Res.string.presentation_retry),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // ── Content ───────────────────────────────────────────────────
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.loadPresentations() },
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            when {
                presentations.isNotEmpty() -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        presentations.forEach { presentation ->
                            // ── Presentation header ───────────────────────
                            item(key = "header_${presentation.id}") {
                                PresentationHeader(
                                    presentation = presentation,
                                    isSelected = presentation == selectedPresentation
                                )
                            }

                            // ── Slide grid rows ───────────────────────────
                            val rows = presentation.slides.orEmpty().chunked(SLIDE_COLUMNS)
                            items(
                                items = rows,
                                key = { row -> "slide_${presentation.id}_${row.first().slideIndex}" }
                            ) { rowSlides ->
                                SlideRow(
                                    slides = rowSlides,
                                    selectedPresentationId = selectedPresentation?.id,
                                    thisPresentationId = presentation.id,
                                    selectedSlideIndex = selectedSlideIndex,
                                    imageLoader = imageLoader,
                                    onSlideTap = { slideIndex ->
                                        viewModel.selectPresentation(presentation, slideIndex)
                                    }
                                )
                            }

                            item(key = "divider_${presentation.id}") {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
                !isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(Res.string.presentation_no_items),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // else: isLoading with no data — PTR indicator handles visual feedback
            }
        }
    }   // end Column

        // ── Action buttons (bottom-right, above snackbar) ─────────────────
        ContentActionButtons(
            isProjecting       = isProjecting,
            scheduleAdded      = scheduleAdded,
            onToggleProjecting = {
                if (isProjecting) viewModel.clearDisplay()
                else {
                    val pres = selectedPresentation
                    val idx  = selectedSlideIndex
                    if (pres != null && idx != null) viewModel.selectPresentation(pres, idx)
                }
            },
            onAddToSchedule = { viewModel.addToSchedule() },
            modifier        = Modifier.align(Alignment.BottomEnd),
        )
    }   // end Box
}

// ── Sub-composables ────────────────────────────────────────────────────────────

@Composable
private fun PresentationHeader(presentation: Presentation, isSelected: Boolean) {
    val bg = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
             else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
             else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = presentation.displayName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.weight(1f)
        )
        if (presentation.totalSlides > 0) {
            Text(
                text = "${presentation.totalSlides} ${stringResource(Res.string.presentation_slides)}",
                style = MaterialTheme.typography.labelSmall,
                color = fg.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun SlideRow(
    slides: List<PresentationSlide>,
    selectedPresentationId: String?,
    thisPresentationId: String?,
    selectedSlideIndex: Int?,
    imageLoader: ImageLoader,
    onSlideTap: (slideIndex: Int) -> Unit
) {
    val selectedBorderColor = MaterialTheme.colorScheme.secondary
    val isThisPresSelected = thisPresentationId != null && thisPresentationId == selectedPresentationId

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val context = LocalPlatformContext.current
        val epochDay = Clock.System.now().toEpochMilliseconds() / 86_400_000L
        slides.forEach { slide ->
            val isSlideSelected = isThisPresSelected && slide.slideIndex == selectedSlideIndex
            val request = ImageRequest.Builder(context)
                .data(slide.thumbnailUrl)
                .diskCacheKey("${slide.thumbnailUrl}_d$epochDay")
                .size(500, 500)
                .scale(Scale.FILL)
                .crossfade(true)
                .build()

            SubcomposeAsyncImage(
                model = request,
                contentDescription = "Slide ${slide.slideIndex + 1}",
                imageLoader = imageLoader,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 500.dp)
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (isSlideSelected) Modifier.border(2.dp, selectedBorderColor, RoundedCornerShape(8.dp))
                        else Modifier
                    )
                    .clickable { onSlideTap(slide.slideIndex) }
            ) {
                val state by painter.state.collectAsState()
                when (state) {
                    is AsyncImagePainter.State.Loading,
                    AsyncImagePainter.State.Empty -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    is AsyncImagePainter.State.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.errorContainer)
                        )
                    }
                    else -> SubcomposeAsyncImageContent()
                }
            }
        }
        // Fill gap when last row has fewer slides than SLIDE_COLUMNS
        if (slides.size < SLIDE_COLUMNS) {
            Spacer(modifier = Modifier.weight((SLIDE_COLUMNS - slides.size).toFloat()))
        }
    }
}
