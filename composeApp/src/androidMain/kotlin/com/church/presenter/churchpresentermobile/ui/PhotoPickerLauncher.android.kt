package com.church.presenter.churchpresentermobile.ui

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Android actual — uses the modern system Photo Picker (no permission needed). Supports multi-select. */
@Composable
actual fun PhotoPickerLauncher(
    onPhotoPicked: OnPhotoPickedCallback,
    content: @Composable (launch: () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isEmpty()) { onPhotoPicked(emptyList()); return@rememberLauncherForActivityResult }
        // Reading several camera photos through the content resolver is disk and
        // IPC work: done in this callback, on the main thread, it froze small
        // phones into an ANR (CHURCH-PRESENTER-MOBILE-25). The answer still
        // arrives on the main thread, as the callback promises.
        scope.launch {
            val photos = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@mapNotNull null
                    val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (col >= 0 && cursor.moveToFirst()) cursor.getString(col) else null
                    } ?: "photo_${System.currentTimeMillis()}.jpg"
                    PickedPhoto(bytes, name)
                }
            }
            onPhotoPicked(photos)
        }
    }

    content {
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}
