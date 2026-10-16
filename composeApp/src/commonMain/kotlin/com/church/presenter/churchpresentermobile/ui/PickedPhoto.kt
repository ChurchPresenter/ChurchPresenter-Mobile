package com.church.presenter.churchpresentermobile.ui

/**
 * A single picked photo: raw JPEG/PNG bytes + a suggested file name.
 */
data class PickedPhoto(val bytes: ByteArray, val fileName: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PickedPhoto) return false
        return fileName == other.fileName && bytes.contentEquals(other.bytes)
    }
    override fun hashCode(): Int = 31 * bytes.contentHashCode() + fileName.hashCode()
}
