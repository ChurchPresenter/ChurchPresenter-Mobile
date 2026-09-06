package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import churchpresentermobile.composeapp.generated.resources.Res
import churchpresentermobile.composeapp.generated.resources.editor_field_body
import churchpresentermobile.composeapp.generated.resources.editor_field_title
import churchpresentermobile.composeapp.generated.resources.editor_notice_edit_title
import churchpresentermobile.composeapp.generated.resources.editor_notice_new_title
import churchpresentermobile.composeapp.generated.resources.editor_preview
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.LibraryField
import com.church.presenter.churchpresentermobile.model.SlideDeckBuilder
import com.church.presenter.churchpresentermobile.model.Slide
import com.church.presenter.churchpresentermobile.ui.ScreenHeader
import com.church.presenter.churchpresentermobile.ui.standalone.StandaloneOutputScreen
import com.church.presenter.churchpresentermobile.ui.theme.AppDimens
import com.church.presenter.churchpresentermobile.ui.theme.LocalAppColors
import com.church.presenter.churchpresentermobile.viewmodel.LibraryEditorViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Creates and edits a notice.
 *
 * Simpler than the song editor — one block of text, and no preview. A notice is
 * a line or two, and how it looks is set in the Look rather than here, so the
 * preview only took room away from the writing.
 */
@Composable
fun AnnouncementEditorScreen(
    repository: LibraryRepository,
    announcementId: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: LibraryEditorViewModel = viewModel(key = "editor_notice_$announcementId") {
        LibraryEditorViewModel(repository)
    }
    val colors = LocalAppColors.current

    LaunchedEffect(announcementId) { viewModel.editAnnouncement(announcementId) }

    val announcement by viewModel.announcement.collectAsState()
    val validation by viewModel.validation.collectAsState()
    val isDirty by viewModel.isDirty.collectAsState()

    var showDiscardPrompt by remember { mutableStateOf(false) }
    val close = { if (isDirty) showDiscardPrompt = true else onClose() }

    if (showDiscardPrompt) {
        DiscardDialog(
            onDiscard = { showDiscardPrompt = false; onClose() },
            onKeepEditing = { showDiscardPrompt = false },
        )
    }

    Column(modifier = modifier.fillMaxSize().background(colors.background).imePadding()) {
        ScreenHeader(
            title = stringResource(
                if (announcementId == null) Res.string.editor_notice_new_title
                else Res.string.editor_notice_edit_title
            ),
            largeTitle = false,
            onBack = close,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.space16),
            verticalArrangement = Arrangement.spacedBy(AppDimens.space12),
        ) {
            EditorField(
                label = stringResource(Res.string.editor_field_title),
                value = announcement.title,
                onValueChange = viewModel::setAnnouncementTitle,
                modifier = Modifier.testTag(LibraryTags.FIELD_TITLE),
            )
            EditorField(
                label = stringResource(Res.string.editor_field_body),
                value = announcement.body,
                onValueChange = viewModel::setAnnouncementBody,
                error = validation.errors[LibraryField.BODY],
                warning = validation.warnings[LibraryField.BODY],
                minLines = 5,
                modifier = Modifier.testTag(LibraryTags.FIELD_BODY),
            )

        }

        EditorActions(
            canSave = validation.isValid,
            onCancel = close,
            onSave = { if (viewModel.saveAnnouncement()) onClose() },
        )
    }
}
