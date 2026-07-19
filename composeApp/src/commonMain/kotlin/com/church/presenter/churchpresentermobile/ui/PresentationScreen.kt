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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.presentation_loading_error
import churchpresentermobile.composeapp.generated.resources.presentation_no_items
import churchpresentermobile.composeapp.generated.resources.presentation_retry
import churchpresentermobile.composeapp.generated.resources.presentation_slides
import churchpresentermobile.composeapp.generated.resources.presentation_upload_file
import churchpresentermobile.composeapp.generated.resources.presentation_uploading
import churchpresentermobile.composeapp.generated.resources.toast_failed_to_select_presentation
import churchpresentermobile.composeapp.generated.resources.toast_presentation_failed_to_add_schedule
import churchpresentermobile.composeapp.generated.resources.toast_upload_failed
import churchpresentermobile.composeapp.generated.resources.toast_upload_file_too_large
import churchpresentermobile.composeapp.generated.resources.toast_upload_reload_failed
import churchpresentermobile.composeapp.generated.resources.toast_upload_server_error
import churchpresentermobile.composeapp.generated.resources.toast_upload_unsupported
import churchpresentermobile.composeapp.generated.resources.upload_blocked_toast
import coil3.ImageLoader
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Scale
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.Presentation
import com.church.presenter.churchpresentermobile.model.PresentationSlide
import com.church.presenter.churchpresentermobile.model.ToastEvent
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.viewmodel.PresentationsViewModel
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
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
    onScheduleRefresh: () -> Unit = {},
    canUploadFiles: Boolean = false,
    providedViewModel: PresentationsViewModel? = null,
    modifier: Modifier = Modifier
) {
    val viewModel: PresentationsViewModel = providedViewModel
        ?: viewModel(key = isDemoMode.toString()) { PresentationsViewModel(appSettings, ServerEventService(appSettings), isDemoMode) }

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
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val scheduleRefreshTrigger by viewModel.scheduleRefreshTrigger.collectAsState()
    val toastEvent by viewModel.toastEvent.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val uploadBlockedMsg = stringResource(Res.string.upload_blocked_toast)

    val toastMessage = toastEvent?.toDisplayString()
    LaunchedEffect(toastEvent) {
        if (toastMessage != null) {
            snackbarHostState.showSnackbar(message = toastMessage, duration = SnackbarDuration.Short)
            viewModel.toastShown()
        }
    }

    LaunchedEffect(scheduleRefreshTrigger) {
        if (scheduleRefreshTrigger > 0) onScheduleRefresh()
    }

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

    val colors = LocalAppColors.current
    Box(modifier = modifier.fillMaxSize()) {

        Column(modifier = Modifier.fillMaxSize().background(colors.background)) {

        // ── Error banner ──────────────────────────────────────────────
        if (error != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.danger.copy(alpha = 0.12f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = error ?: stringResource(Res.string.presentation_loading_error),
                    color = colors.danger,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(Res.string.presentation_retry),
                    color = colors.danger,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 12.dp).clickable { viewModel.loadPresentations() }
                )
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
                                    apiKey = appSettings.apiKey,
                                    deviceId = appSettings.deviceId,
                                    onSlideTap = { slideIndex ->
                                        viewModel.selectPresentation(presentation, slideIndex)
                                    }
                                )
                            }

                            item(key = "divider_${presentation.id}") {
                                Spacer(modifier = Modifier.size(8.dp))
                            }
                        }
                    }
                }
                !isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(Res.string.presentation_no_items),
                            color = colors.muted,
                            fontSize = 15.sp
                        )
                    }
                }
                // else: isLoading with no data — PTR indicator handles visual feedback
            }
        }
    }   // end Column

        // ── Action buttons (bottom-right, above snackbar) ─────────────────
        // When uploads are blocked the real PresentationFilePicker is never composed,
        // so the OS document picker cannot be presented under any timing condition.
        if (canUploadFiles) {
            PresentationFilePicker(
                onFilePicked = { file ->
                    if (file != null) viewModel.uploadPresentationFile(file)
                },
                onError = { message -> viewModel.reportError(message) },
            ) { launchPicker ->
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
                    extraLeadingContent = {
                        // Upload Presentation FAB — picker is live
                        val neutralShadow = if (colors.isDark) androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)
                            else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.18f)
                        if (isUploading) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.surfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = colors.accent,
                                    strokeWidth = 2.5.dp,
                                )
                            }
                        } else {
                            SquareFab(
                                icon = Icons.Filled.UploadFile,
                                contentDescription = stringResource(Res.string.presentation_upload_file),
                                containerColor = colors.surfaceElevated,
                                iconColor = colors.accent,
                                shadowColor = neutralShadow,
                                onClick = { launchPicker() },
                            )
                        }
                    }
                )
            }
        } else {
            // Upload blocked — render a standalone FAB with NO picker wired up.
            // Tapping it shows the "upload disabled" snackbar; the OS picker is
            // never registered and therefore can never be presented.
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
                extraLeadingContent = {
                    val neutralShadow = if (colors.isDark) androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)
                        else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.18f)
                    SquareFab(
                        icon = Icons.Filled.Block,
                        contentDescription = stringResource(Res.string.presentation_upload_file),
                        containerColor = colors.surfaceElevated,
                        iconColor = colors.muted,
                        shadowColor = neutralShadow,
                        onClick = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message  = uploadBlockedMsg,
                                    duration = SnackbarDuration.Long,
                                )
                            }
                        },
                    )
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Non-dismissible upload overlay — shown until the upload completes
        if (isUploading) {
            UploadProgressOverlay(
                title    = stringResource(Res.string.presentation_uploading),
                progress = uploadProgress,
            )
        }
    }   // end Box
}

/** Resolves a [ToastEvent] to a localised display string using Compose string resources. */
@Composable
private fun ToastEvent.toDisplayString(): String = when (this) {
    is ToastEvent.FailedToSelectPresentation     -> stringResource(Res.string.toast_failed_to_select_presentation, reason)
    is ToastEvent.FailedToAddPresentationSchedule -> stringResource(Res.string.toast_presentation_failed_to_add_schedule, reason)
    is ToastEvent.UploadUnsupported               -> stringResource(Res.string.toast_upload_unsupported)
    is ToastEvent.UploadFileTooLarge              -> stringResource(Res.string.toast_upload_file_too_large)
    is ToastEvent.UploadServerError               -> stringResource(Res.string.toast_upload_server_error, msg)
    is ToastEvent.UploadFailed                    -> stringResource(Res.string.toast_upload_failed, reason)
    is ToastEvent.UploadReloadFailed              -> stringResource(Res.string.toast_upload_reload_failed, reason)
    else                                          -> ""
}

// ── Sub-composables ────────────────────────────────────────────────────────────

@Composable
private fun PresentationHeader(presentation: Presentation, isSelected: Boolean) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            tint = if (isSelected) colors.accent else colors.muted,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = presentation.displayName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colors.text,
        )
        if (presentation.totalSlides > 0) {
            Text(
                text = "· ${presentation.totalSlides} ${stringResource(Res.string.presentation_slides)}",
                fontSize = 11.sp,
                color = colors.muted,
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
    apiKey: String,
    deviceId: String,
    onSlideTap: (slideIndex: Int) -> Unit
) {
    val colors = LocalAppColors.current
    val isThisPresSelected = thisPresentationId != null && thisPresentationId == selectedPresentationId

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val context = LocalPlatformContext.current
        val epochDay = Clock.System.now().toEpochMilliseconds() / 86_400_000L
        slides.forEach { slide ->
            val isSlideSelected = isThisPresSelected && slide.slideIndex == selectedSlideIndex
            val request = ImageRequest.Builder(context)
                .data(slide.thumbnailUrl)
                .httpHeaders(apiImageHeaders(apiKey, deviceId))
                .diskCacheKey("${slide.thumbnailUrl}_d$epochDay")
                .size(500, 500)
                .scale(Scale.FILL)
                .crossfade(true)
                .build()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 500.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (isSlideSelected) Modifier.border(2.5.dp, colors.accent, RoundedCornerShape(8.dp))
                        else Modifier
                    )
                    .clickable { onSlideTap(slide.slideIndex) }
            ) {
                SubcomposeAsyncImage(
                    model = request,
                    contentDescription = "Slide ${slide.slideIndex + 1}",
                    imageLoader = imageLoader,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                ) {
                    val state by painter.state.collectAsState()
                    when (state) {
                        is AsyncImagePainter.State.Loading,
                        AsyncImagePainter.State.Empty -> {
                            Box(
                                modifier = Modifier.fillMaxSize().background(colors.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${slide.slideIndex + 1}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.dim
                                )
                            }
                        }
                        is AsyncImagePainter.State.Error -> {
                            Box(modifier = Modifier.fillMaxSize().background(colors.danger.copy(alpha = 0.12f)))
                        }
                        else -> SubcomposeAsyncImageContent()
                    }
                }
                // ── "▶ LIVE" chip on the selected slide ──────────────────
                if (isSlideSelected) {
                    Text(
                        text = "▶ LIVE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onAccent,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.accent)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
        // Fill gap when last row has fewer slides than SLIDE_COLUMNS
        if (slides.size < SLIDE_COLUMNS) {
            Spacer(modifier = Modifier.weight((SLIDE_COLUMNS - slides.size).toFloat()))
        }
    }
}
