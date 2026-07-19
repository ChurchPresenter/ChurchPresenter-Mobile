package com.church.presenter.churchpresentermobile.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.pictures_loading_error
import churchpresentermobile.composeapp.generated.resources.pictures_no_items
import churchpresentermobile.composeapp.generated.resources.pictures_photos
import churchpresentermobile.composeapp.generated.resources.pictures_pick_from_device
import churchpresentermobile.composeapp.generated.resources.pictures_retry
import churchpresentermobile.composeapp.generated.resources.pictures_uploading
import churchpresentermobile.composeapp.generated.resources.upload_blocked_toast
import churchpresentermobile.composeapp.generated.resources.upload_overlay_photo_counter
import coil3.ImageLoader
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Scale
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.PictureImage
import com.church.presenter.churchpresentermobile.network.ServerEventService
import com.church.presenter.churchpresentermobile.viewmodel.PicturesViewModel
import org.jetbrains.compose.resources.stringResource

private const val GRID_COLUMNS = 3

/**
 * Pictures tab screen. Loads images from GET /api/pictures and displays them in a scrollable
 * 3-column grid. Tapping an image sends it to the display via POST /api/pictures/select.
 *
 * @param appSettings          Shared [AppSettings] used to create and configure [PicturesViewModel].
 * @param settingsSaveToken    Incremented by the parent each time settings are saved; triggers
 *   [PicturesViewModel.onSettingsSaved] so images reload against the new server.
 * @param imageLoader          Coil [ImageLoader] configured with the app's SSL-bypass HTTP client.
 * @param pendingNavFolderId   When non-null, the screen will load this specific folder on arrival.
 * @param pendingNavImageIndex When non-null, the screen will scroll to and highlight this image index.
 * @param onPendingNavHandled  Called once the pending navigation has been applied so the parent
 *   can clear the values and avoid re-triggering.
 * @param providedViewModel    Session-scoped [PicturesViewModel] passed from the root App composable
 *   so the state survives tab switches. Falls back to a locally-scoped instance for previews/tests.
 * @param modifier             The modifier to apply to this composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PicturesScreen(
    appSettings: AppSettings,
    isDemoMode: Boolean = false,
    settingsSaveToken: Int,
    imageLoader: ImageLoader,
    pendingNavFolderId: String? = null,
    pendingNavImageIndex: Int? = null,
    onPendingNavHandled: () -> Unit = {},
    onScheduleRefresh: () -> Unit = {},
    canUploadFiles: Boolean = false,
    providedViewModel: PicturesViewModel? = null,
    modifier: Modifier = Modifier
) {
    // Use the session-scoped ViewModel passed from App.kt when available.
    // The internal fallback is only here so the composable still works in
    // isolation (e.g. Compose Previews or tests).
    val viewModel: PicturesViewModel = providedViewModel
        ?: viewModel(key = isDemoMode.toString()) { PicturesViewModel(appSettings, ServerEventService(appSettings), isDemoMode) }

    // React to settings changes – rebuild the service and reload
    LaunchedEffect(settingsSaveToken) {
        if (settingsSaveToken > 0) viewModel.onSettingsSaved()
    }

    // React to schedule navigation – load the specific folder and scroll to the image
    LaunchedEffect(pendingNavFolderId, pendingNavImageIndex) {
        if (pendingNavFolderId != null || pendingNavImageIndex != null) {
            viewModel.navigateTo(pendingNavFolderId, pendingNavImageIndex)
            onPendingNavHandled()
        }
    }

    val folder by viewModel.folder.collectAsState()
    val selectedImage by viewModel.selectedImage.collectAsState()
    val pendingScrollIndex by viewModel.pendingScrollIndex.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isProjecting by viewModel.isProjecting.collectAsState()
    val scheduleAdded by viewModel.scheduleAdded.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val uploadPhotoIndex by viewModel.uploadPhotoIndex.collectAsState()
    val uploadPhotoTotal by viewModel.uploadPhotoTotal.collectAsState()
    val scheduleRefreshTrigger by viewModel.scheduleRefreshTrigger.collectAsState()

    LaunchedEffect(scheduleRefreshTrigger) {
        if (scheduleRefreshTrigger > 0) onScheduleRefresh()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val uploadBlockedMsg = stringResource(Res.string.upload_blocked_toast)

    // Grid state for programmatic scrolling when navigating from schedule
    val gridState = rememberLazyGridState()

    // Scroll to + auto-select the pending image once the folder data arrives.
    LaunchedEffect(pendingScrollIndex, folder) {
        val idx = pendingScrollIndex ?: return@LaunchedEffect
        val currentFolder = folder ?: return@LaunchedEffect
        val image = currentFolder.allImages.find { it.index == idx }
        val listPos = if (image != null) currentFolder.allImages.indexOf(image) else -1
        if (listPos >= 0) {
            // +1 because the first grid item is the folder-header span row
            gridState.animateScrollToItem(listPos + 1)
        }
        if (image != null) viewModel.selectPicture(image)
        viewModel.onPendingScrollHandled()
    }

    val colors = LocalAppColors.current
    Box(modifier = modifier.fillMaxSize().background(colors.background)) {

        Column(modifier = Modifier.fillMaxSize()) {

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
                    text = error ?: stringResource(Res.string.pictures_loading_error),
                    color = colors.danger,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(Res.string.pictures_retry),
                    color = colors.danger,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 12.dp).clickable { viewModel.loadPictures() }
                )
            }
        }

        // ── Content ───────────────────────────────────────────────────
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.loadPictures(folder?.folderId) },
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            when {
                folder != null && folder!!.allImages.isNotEmpty() -> {
                    // Capture in a local val so the non-null smart cast holds inside
                    // all item/items lambdas, even if the StateFlow emits null mid-frame.
                    val currentFolder = folder!!
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(GRID_COLUMNS),
                        state = gridState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // ── Folder header (overline) ──────────────────────
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            OverlineRow(
                                label = currentFolder.displayName,
                                trailing = "${currentFolder.totalImages} ${stringResource(Res.string.pictures_photos)}",
                                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp)
                            )
                        }

                        // ── Image grid ────────────────────────────────────
                        items(
                            items = currentFolder.allImages,
                            key = { it.index }
                        ) { image ->
                            PictureCell(
                                image = image,
                                imageLoader = imageLoader,
                                apiKey = appSettings.apiKey,
                                deviceId = appSettings.deviceId,
                                isSelected = selectedImage?.let { sel ->
                                    if (sel.fileName != null && image.fileName != null) sel.fileName == image.fileName
                                    else sel.index == image.index
                                } ?: false,
                                onTap = { viewModel.selectPicture(image) }
                            )
                        }
                    }
                }
                !isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(Res.string.pictures_no_items),
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
        // When uploads are blocked the real PhotoPickerLauncher is never composed,
        // so the OS photo picker cannot be presented under any timing condition.
        if (canUploadFiles) {
            PhotoPickerLauncher(
                onPhotoPicked = { photos ->
                    if (photos.isNotEmpty()) viewModel.uploadDevicePhotos(photos)
                }
            ) { launchPicker ->
                ContentActionButtons(
                    isProjecting       = isProjecting,
                    scheduleAdded      = scheduleAdded,
                    onToggleProjecting = {
                        if (isProjecting) viewModel.clearDisplay()
                        else selectedImage?.let { viewModel.selectPicture(it) }
                    },
                    onAddToSchedule    = { viewModel.addToSchedule() },
                    modifier           = Modifier.align(Alignment.BottomEnd),
                    extraLeadingContent = {
                        // From Device FAB — picker is live
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
                                icon = Icons.Filled.AddPhotoAlternate,
                                contentDescription = stringResource(Res.string.pictures_pick_from_device),
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
                    else selectedImage?.let { viewModel.selectPicture(it) }
                },
                onAddToSchedule    = { viewModel.addToSchedule() },
                modifier           = Modifier.align(Alignment.BottomEnd),
                extraLeadingContent = {
                    val neutralShadow = if (colors.isDark) androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)
                        else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.18f)
                    SquareFab(
                        icon = Icons.Filled.Block,
                        contentDescription = stringResource(Res.string.pictures_pick_from_device),
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

        // Non-dismissible upload overlay — shown until the upload completes
        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.align(Alignment.BottomCenter),
        )

        if (isUploading) {
            val detail = if (uploadPhotoTotal > 1) {
                stringResource(Res.string.upload_overlay_photo_counter, uploadPhotoIndex, uploadPhotoTotal)
            } else null
            UploadProgressOverlay(
                title    = stringResource(Res.string.pictures_uploading),
                progress = uploadProgress,
                detail   = detail,
            )
        }
    }   // end Box
}

@OptIn(ExperimentalTime::class)
@Composable
private fun PictureCell(
    image: PictureImage,
    imageLoader: ImageLoader,
    apiKey: String,
    deviceId: String,
    isSelected: Boolean,
    onTap: () -> Unit
) {
    val colors = LocalAppColors.current
    val selectedBorderColor = colors.accent
    val context = LocalPlatformContext.current

    // Epoch day (increments every 24 h) used as a cache-buster: images cached
    // under key "<url>_d<day>" are automatically stale the next day.
    val epochDay = Clock.System.now().toEpochMilliseconds() / 86_400_000L

    // Build an explicit ImageRequest capping the decode size at 500×500 px.
    // The API-key header is required — the /api/pictures/.../images endpoint is
    // behind checkApiKey, so without it Coil gets a 401 and shows nothing.
    val request = ImageRequest.Builder(context)
        .data(image.thumbnailUrl)
        .httpHeaders(apiImageHeaders(apiKey, deviceId))
        .diskCacheKey("${image.thumbnailUrl}_d$epochDay")
        .size(500, 500)
        .scale(Scale.FILL)
        .crossfade(true)
        .build()

    SubcomposeAsyncImage(
        model = request,
        contentDescription = image.fileName,
        imageLoader = imageLoader,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .sizeIn(maxWidth = 500.dp, maxHeight = 500.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onTap() }
            .then(
                if (isSelected) Modifier.border(3.dp, selectedBorderColor, RoundedCornerShape(8.dp))
                else Modifier
            )
    ) {
        val state by painter.state.collectAsState()
        when (state) {
            is AsyncImagePainter.State.Loading,
            AsyncImagePainter.State.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.surface),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = colors.accent
                    )
                }
            }
            is AsyncImagePainter.State.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.danger.copy(alpha = 0.12f))
                )
            }
            else -> SubcomposeAsyncImageContent()
        }
    }
}
