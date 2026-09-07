package com.church.presenter.churchpresentermobile.ui.standalone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.action_clear_display
import churchpresentermobile.composeapp.generated.resources.cd_delete
import churchpresentermobile.composeapp.generated.resources.pictures_no_items
import churchpresentermobile.composeapp.generated.resources.pictures_pick_from_device
import churchpresentermobile.composeapp.generated.resources.standalone_no_output
import coil3.compose.AsyncImage
import com.church.presenter.churchpresentermobile.present.PhotoLibrary
import com.church.presenter.churchpresentermobile.present.StandaloneEngine
import com.church.presenter.churchpresentermobile.present.StoredPhoto
import com.church.presenter.churchpresentermobile.ui.IconTileButton
import com.church.presenter.churchpresentermobile.ui.OutlineActionButton
import com.church.presenter.churchpresentermobile.ui.PhotoPickerLauncher
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.LocalPhotosViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Photos from this device, projected by this device.
 *
 * The remote screen browses the desktop's picture folders. Standalone has no
 * desktop and therefore no folders, so this offers the phone's own picker
 * instead and projects straight from what comes back.
 *
 * @param library Shared with the presentation server, which serves the bytes.
 * @param presenter The local presenter.
 */
@Composable
fun LocalPhotosScreen(
    library: PhotoLibrary,
    presenter: StandaloneEngine?,
    modifier: Modifier = Modifier,
    /** Supplied by tests only; the screen owns its own otherwise. */
    providedViewModel: LocalPhotosViewModel? = null,
) {
    val colors = LocalAppColors.current
    val vm: LocalPhotosViewModel = providedViewModel
        ?: viewModel(key = "local_photos") { LocalPhotosViewModel(library, presenter) }
    val photos by vm.photos.collectAsState()
    val canProject by vm.canProject.collectAsState()
    val projectingId by vm.projectingId.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().background(colors.background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            PhotoPickerLauncher(
                onPhotoPicked = { picked -> picked.forEach { vm.add(it.fileName, it.bytes) } },
            ) { launch ->
                OutlineActionButton(
                    label = stringResource(Res.string.pictures_pick_from_device),
                    icon = Icons.Outlined.Image,
                    onClick = launch,
                    modifier = Modifier.weight(1f).testTag(StandaloneTags.PHOTOS_PICK),
                )
            }
            if (projectingId != null) {
                OutlineActionButton(
                    label = stringResource(Res.string.action_clear_display),
                    icon = Icons.Outlined.Delete,
                    onClick = { vm.clearDisplay() },
                    modifier = Modifier.weight(1f).testTag(StandaloneTags.PHOTOS_CLEAR),
                )
            }
        }

        // Photos are fetched from the phone's own server by both displays, so
        // until it is up there is nothing to project and saying so beats a tap
        // that silently does nothing.
        if (!canProject) {
            Text(
                text = stringResource(Res.string.standalone_no_output),
                color = colors.muted,
                fontSize = 12.sp,
                modifier = Modifier.testTag(StandaloneTags.PHOTOS_NO_SERVER),
            )
        }

        if (photos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.pictures_no_items),
                    color = colors.muted,
                    fontSize = 14.sp,
                    modifier = Modifier.testTag(StandaloneTags.PHOTOS_EMPTY),
                )
            }
            return@Column
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(photos, key = { it.id }) { photo ->
                PhotoTile(
                    photo = photo,
                    url = library.urlFor(photo.id),
                    isProjecting = photo.id == projectingId,
                    enabled = canProject,
                    onClick = { vm.project(photo) },
                    onRemove = { vm.remove(photo.id) },
                    modifier = Modifier.testTag(StandaloneTags.photo(photo.id)),
                    removeModifier = Modifier.testTag(StandaloneTags.photoRemove(photo.id)),
                    liveModifier = Modifier.testTag(StandaloneTags.photoLive(photo.id)),
                )
            }
        }
    }
}

@Composable
private fun PhotoTile(
    photo: StoredPhoto,
    url: String?,
    isProjecting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    removeModifier: Modifier = Modifier,
    liveModifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(colors.surface)
            .border(
                width = if (isProjecting) 2.dp else 1.dp,
                color = if (isProjecting) colors.accent else colors.borderSubtle,
                shape = shape,
            )
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = photo.fileName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // No server, no thumbnail — name it rather than showing an empty tile.
            Text(
                text = photo.fileName,
                color = colors.muted,
                fontSize = 11.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.Center).padding(8.dp),
            )
        }

        IconTileButton(
            icon = Icons.Outlined.Delete,
            contentDescription = stringResource(Res.string.cd_delete),
            tint = colors.muted,
            onClick = onRemove,
            modifier = removeModifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp),
        )

        if (isProjecting) {
            Icon(
                imageVector = Icons.Filled.Visibility,
                contentDescription = null,
                tint = colors.accent,
                modifier = liveModifier.align(Alignment.BottomStart).padding(8.dp).size(18.dp),
            )
        }
    }
}
