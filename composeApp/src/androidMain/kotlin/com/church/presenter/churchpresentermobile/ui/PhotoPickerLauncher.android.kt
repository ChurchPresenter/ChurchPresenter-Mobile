package com.church.presenter.churchpresentermobile.ui

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.church.presenter.churchpresentermobile.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

private const val TAG = "PhotoPickerLauncher"

/** Android actual — uses the modern system Photo Picker (no permission needed). Supports multi-select. */
@Composable
actual fun PhotoPickerLauncher(
    onPhotoPicked: OnPhotoPickedCallback,
    content: @Composable (launch: () -> Unit) -> Unit,
) {
    // The application's resolver: a photo's loader can outlive this screen's activity.
    val resolver = LocalContext.current.applicationContext.contentResolver
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isEmpty()) { onPhotoPicked(emptyList()); return@rememberLauncherForActivityResult }
        // Only the names are looked up here. The bytes are read when the caller asks
        // for each photo: reading every original up front held them all in memory
        // at once, and a large multi-select ran the heap out (OutOfMemoryError).
        // The lookup is still IPC, so it stays off the main thread
        // (CHURCH-PRESENTER-MOBILE-25); the answer arrives on the main thread.
        scope.launch {
            val photos = withContext(Dispatchers.IO) {
                uris.map { uri ->
                    PickedPhoto(displayName(resolver, uri) ?: "photo_${System.currentTimeMillis()}.jpg") {
                        readPhoto(resolver, uri)
                    }
                }
            }
            onPhotoPicked(photos)
        }
    }

    content {
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

private fun displayName(resolver: ContentResolver, uri: Uri): String? =
    resolver.query(uri, null, null, null, null)?.use { cursor ->
        val col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (col >= 0 && cursor.moveToFirst()) cursor.getString(col) else null
    }

/**
 * One photo's bytes, or null when it cannot be read. A single photo too big for
 * the heap is skipped rather than taking the app down with it.
 */
private suspend fun readPhoto(resolver: ContentResolver, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
    try {
        resolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (e: IOException) {
        Logger.e(TAG, "readPhoto — could not read $uri: ${e.message}", e)
        null
    } catch (e: SecurityException) {
        Logger.e(TAG, "readPhoto — access to $uri was revoked: ${e.message}", e)
        null
    } catch (e: OutOfMemoryError) {
        Logger.e(TAG, "readPhoto — $uri is too large to read: ${e.message}", e)
        null
    }
}
