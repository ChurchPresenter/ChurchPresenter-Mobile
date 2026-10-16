package com.church.presenter.churchpresentermobile.ui

/**
 * A single picked presentation file: raw bytes + the original file name.
 * Accepted types: PowerPoint (.pptx / .ppt), PDF (.pdf), Keynote (.key).
 */
data class PickedFile(val bytes: ByteArray, val fileName: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PickedFile) return false
        return fileName == other.fileName && bytes.contentEquals(other.bytes)
    }
    override fun hashCode(): Int = 31 * bytes.contentHashCode() + fileName.hashCode()
}
