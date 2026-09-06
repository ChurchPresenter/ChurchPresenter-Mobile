package com.church.presenter.churchpresentermobile.ui.library

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import com.church.presenter.churchpresentermobile.library.LibraryRepository
import com.church.presenter.churchpresentermobile.model.ThemeMode
import com.church.presenter.churchpresentermobile.ui.theme.AppTheme

/** Opening the library's two editor screens. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showSongEditor(
    repository: LibraryRepository,
    songId: String? = null,
    onClose: () -> Unit = {},
) {
    setContent {
        AppTheme(themeMode = ThemeMode.DARK) {
            SongEditorScreen(repository = repository, songId = songId, onClose = onClose)
        }
    }
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showNoticeEditor(
    repository: LibraryRepository,
    noticeId: String? = null,
    onClose: () -> Unit = {},
) {
    setContent {
        AppTheme(themeMode = ThemeMode.DARK) {
            AnnouncementEditorScreen(
                repository = repository,
                announcementId = noticeId,
                onClose = onClose,
            )
        }
    }
}
