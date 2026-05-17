package com.pning80.watchmycalories.data

import android.content.Context
import android.graphics.Bitmap
import com.pning80.watchmycalories.security.JpegConfig
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Image persistence for captured food / menu photos (PORTING_CRITERIA.md T1.4).
 *
 * Mirrors iOS: JPEG written to `filesDir/{imageID}.jpg`, keyed by a UUID stored
 * on the FoodEntry / MenuScan row. The write happens at user "Save" time on the
 * estimation review screen — not at capture time — so an abandoned analysis
 * leaves no orphan file on disk.
 *
 * The on-disk filename uses `UUID.toString()` exactly (lowercase, hyphenated) to
 * round-trip with iOS's `UUID().uuidString.lowercased()`.
 */
object ImageStorage {

    /**
     * Persist a bitmap as JPEG under `filesDir/{imageID}.jpg`.
     * Returns the saved File. Overwrites if the file already exists.
     */
    fun saveJpeg(context: Context, bitmap: Bitmap, imageID: String): File {
        val file = imageFile(context, imageID)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JpegConfig.QUALITY, out)
        }
        return file
    }

    /**
     * Look up the persisted image for an imageID, or null if it doesn't exist.
     */
    fun getImageFile(context: Context, imageID: String): File? {
        val f = imageFile(context, imageID)
        return if (f.exists()) f else null
    }

    /**
     * Delete a persisted image. Idempotent.
     */
    fun deleteImage(context: Context, imageID: String) {
        imageFile(context, imageID).delete()
    }

    /**
     * Generate a fresh imageID. Lowercase, hyphenated to match iOS.
     */
    fun newImageID(): String = UUID.randomUUID().toString()

    private fun imageFile(context: Context, imageID: String): File =
        File(context.filesDir, "$imageID.jpg")
}
