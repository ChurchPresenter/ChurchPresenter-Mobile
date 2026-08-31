package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.sync_address_section
import churchpresentermobile.composeapp.generated.resources.sync_section_bible
import churchpresentermobile.composeapp.generated.resources.sync_section_songs
import churchpresentermobile.composeapp.generated.resources.sync_title
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.library.LocalBibleRepository
import com.church.presenter.churchpresentermobile.model.AppSettings
import com.church.presenter.churchpresentermobile.network.WsSender
import com.church.presenter.churchpresentermobile.ui.DesktopAddressFields
import com.church.presenter.churchpresentermobile.ui.SegmentedControl
import com.church.presenter.churchpresentermobile.ui.theme.AppDimens
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import org.jetbrains.compose.resources.stringResource

/** Which half of the sheet opens first, so an empty state can land on the right one. */
enum class SyncSection { SONGS, BIBLE }

/**
 * Copying content from a computer: where it is, then what to take.
 *
 * One sheet with two sections rather than two sheets, because both need the same address and an
 * operator who has just fixed it for songs should not have to fix it again for Bibles. The
 * address sits above the choice for the same reason it is here at all — in standalone there is
 * nowhere else it can be reached, and without it every copy silently targets the default host.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSheet(
    repository: LibraryRepository,
    bibles: LocalBibleRepository,
    settings: AppSettings,
    sender: WsSender,
    initialSection: SyncSection = SyncSection.SONGS,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    var section by remember { mutableStateOf(initialSection) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.sheetBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                // Two sections plus an address block overflow a phone screen.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.space20, vertical = AppDimens.space8),
            verticalArrangement = Arrangement.spacedBy(AppDimens.space12),
        ) {
            Text(
                text = stringResource(Res.string.sync_title),
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = stringResource(Res.string.sync_address_section),
                color = colors.accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            DesktopAddressFields(settings = settings)

            SegmentedControl(
                options = listOf(
                    stringResource(Res.string.sync_section_songs),
                    stringResource(Res.string.sync_section_bible),
                ),
                selectedIndex = if (section == SyncSection.SONGS) 0 else 1,
                onSelect = { section = if (it == 0) SyncSection.SONGS else SyncSection.BIBLE },
            )

            when (section) {
                SyncSection.SONGS -> SongSyncSection(repository, settings, sender, onDone = onDismiss)
                SyncSection.BIBLE -> BibleSyncSection(bibles, settings)
            }

            Box(Modifier.padding(bottom = AppDimens.space16))
        }
    }
}
