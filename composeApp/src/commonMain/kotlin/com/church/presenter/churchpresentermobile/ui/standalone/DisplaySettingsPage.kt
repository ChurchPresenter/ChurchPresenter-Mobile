package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.standalone_look
import churchpresentermobile.composeapp.generated.resources.standalone_backdrop
import churchpresentermobile.composeapp.generated.resources.standalone_backdrop_black
import churchpresentermobile.composeapp.generated.resources.standalone_backdrop_gradient
import churchpresentermobile.composeapp.generated.resources.standalone_backdrop_image
import churchpresentermobile.composeapp.generated.resources.standalone_backdrop_needs_server
import churchpresentermobile.composeapp.generated.resources.standalone_backdrop_no_photos
import churchpresentermobile.composeapp.generated.resources.standalone_backdrop_pick
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.model.SlideBackdrop
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.present.SinkRegistry
import com.church.presenter.churchpresentermobile.present.PhotoLibrary
import com.church.presenter.churchpresentermobile.present.StoredPhoto
import com.church.presenter.churchpresentermobile.ui.OverlineRow
import com.church.presenter.churchpresentermobile.ui.SegmentedControl
import com.church.presenter.churchpresentermobile.ui.theme.AppDimens
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.StandaloneViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.stringResource


/**
 * Settings → Display: which screens the phone is projecting onto, and how the
 * words on them look.
 *
 * Both used to be on the Present tab, beside the live controls. They are set up
 * once, before a service, and sharing a screen with Next and Blank only pushed
 * those further from the thumb that reaches for them mid-verse — so they moved
 * here, and the live controls moved to the live bar.
 *
 * Owns its own ViewModel per the project rule. The engine, sink registry and
 * photo library are plain collaborators, the same instances the live bar reads,
 * so a backdrop chosen here is on every screen at once.
 */
@Composable
fun DisplaySettingsPage(
    engine: StandaloneEngine,
    registry: SinkRegistry,
    settings: AppSettings,
    photos: PhotoLibrary? = null,
    /** Supplied by tests only; the page owns its own otherwise. */
    providedViewModel: StandaloneViewModel? = null,
) {
    val viewModel: StandaloneViewModel = providedViewModel
        ?: viewModel(key = "standalone") { StandaloneViewModel(engine, registry, settings, photos) }
    val sinks by viewModel.sinks.collectAsState()
    val backdrop by viewModel.backdrop.collectAsState()
    val backdropUrl by viewModel.backdropUrl.collectAsState()
    val backdropPhotos by viewModel.backdropPhotos.collectAsState()
    val canUsePhotoBackdrop by viewModel.canUsePhotoBackdrop.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val showChords by viewModel.showChords.collectAsState()
    val textSize by viewModel.textSize.collectAsState()
    val savedThemes by viewModel.savedThemes.collectAsState()
    var showLook by remember { mutableStateOf(false) }

    if (showLook) {
        LookSheet(
            theme = theme,
            onThemeChange = viewModel::updateTheme,
            showChords = showChords,
            onShowChordsChange = viewModel::setShowChords,
            textSize = textSize,
            onTextSizeChange = viewModel::setTextSize,
            presets = viewModel.presets,
            savedThemes = savedThemes,
            onApplyTheme = viewModel::applyTheme,
            onSaveTheme = viewModel::saveCurrentTheme,
            onDeleteTheme = viewModel::deleteSavedTheme,
            onDismiss = { showLook = false },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.space12)) {
        OutputTargetsContent(sinks, Modifier.testTag(StandaloneTags.DISPLAY_OUTPUTS))
        BackdropControls(
            backdrop = backdrop,
            backdropPhotos = backdropPhotos,
            backdropUrl = backdropUrl,
            canUsePhotoBackdrop = canUsePhotoBackdrop,
            urlFor = { photos?.urlFor(it) },
            onBackdropChange = viewModel::setBackdrop,
            onPickImage = viewModel::setImageBackdrop,
            onOpenLook = { showLook = true },
        )
    }
}

/** The backdrop picker and the way into the look sheet. */
@Composable
private fun ColumnScope.BackdropControls(
    backdrop: SlideBackdrop,
    backdropPhotos: List<StoredPhoto>,
    backdropUrl: String?,
    canUsePhotoBackdrop: Boolean,
    urlFor: (String) -> String?,
    onBackdropChange: (SlideBackdrop) -> Unit,
    onPickImage: (StoredPhoto) -> Unit,
    onOpenLook: () -> Unit,
) {
    val colors = LocalAppColors.current
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
            modifier = Modifier.testTag(StandaloneTags.LOOK).clickable { onOpenLook() },
        )
    }
    SegmentedControl(
        options = listOf(
            stringResource(Res.string.standalone_backdrop_gradient),
            stringResource(Res.string.standalone_backdrop_image),
            stringResource(Res.string.standalone_backdrop_black),
        ),
        selectedIndex = BACKDROPS.indexOf(backdrop).coerceAtLeast(0),
        onSelect = { onBackdropChange(BACKDROPS[it]) },
        optionTag = { StandaloneTags.backdrop(it) },
    )

    // Choosing IMAGE is only half the instruction — it needs a photo, and
    // without one the audience page falls back to the gradient, so the
    // option looked broken. The strip appears only for IMAGE so the
    // controls stay as short as they were for the other two.
    if (backdrop == SlideBackdrop.IMAGE) {
        BackdropPhotoStrip(
            photos = backdropPhotos,
            selectedUrl = backdropUrl,
            canUse = canUsePhotoBackdrop,
            urlFor = urlFor,
            onPick = onPickImage,
        )
    }
}

/**
 * The photos on this device, offered as a backdrop.
 *
 * A backdrop travels to every output as a URL, and photos are served by the
 * embedded web server, so there is nothing to send until that server is on.
 * Rather than let a tap do nothing, the strip says which of the two things is
 * missing — a photo, or the server.
 */
@Composable
private fun BackdropPhotoStrip(
    photos: List<StoredPhoto>,
    selectedUrl: String?,
    canUse: Boolean,
    urlFor: (String) -> String?,
    onPick: (StoredPhoto) -> Unit,
) {
    val colors = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            photos.isEmpty() -> Text(
                text = stringResource(Res.string.standalone_backdrop_no_photos),
                color = colors.muted,
                fontSize = 12.sp,
                modifier = Modifier.testTag(StandaloneTags.BACKDROP_NO_PHOTOS),
            )
            !canUse -> Text(
                text = stringResource(Res.string.standalone_backdrop_needs_server),
                color = colors.muted,
                fontSize = 12.sp,
                modifier = Modifier.testTag(StandaloneTags.BACKDROP_NEEDS_SERVER),
            )
            else -> {
                Text(
                    text = stringResource(Res.string.standalone_backdrop_pick),
                    color = colors.muted,
                    fontSize = 12.sp,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag(StandaloneTags.BACKDROP_PHOTOS),
                ) {
                    items(photos, key = { it.id }) { photo ->
                        val url = urlFor(photo.id)
                        val isSelected = url != null && url == selectedUrl
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.surface)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) colors.accent else colors.borderSubtle,
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .testTag(StandaloneTags.backdropPhoto(photo.id))
                                .clickable { onPick(photo) },
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = photo.fileName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Declaration order must match the segmented-control label order in [BackdropControls]. */
private val BACKDROPS = listOf(SlideBackdrop.GRADIENT, SlideBackdrop.IMAGE, SlideBackdrop.BLACK)
