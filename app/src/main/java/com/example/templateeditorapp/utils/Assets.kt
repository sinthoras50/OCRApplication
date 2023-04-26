package com.example.templateeditorapp.utils

import android.content.Context
import android.content.res.AssetManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


/**
 * A singleton object that provides utility functions to manage assets in an Android app.
 * This object provides methods to extract assets from the app's assets directory, copy them
 * to the app's internal storage, and manage the file paths for those assets.
 */
object Assets {

    /**
     * Returns the absolute path to the app's internal storage directory.
     *
     * @param context The context of the app.
     * @return The absolute path to the app's internal storage directory.
     */
    fun getDataPath(context: Context): String {
        return context.filesDir.absolutePath
    }

    /**
     * Returns the name of the pretrained language data to be extracted and used by Tesseract
     *
     * @return The name of the language asset to extract.
     */
    val language: String
        get() = "eng"

    /**
     * Extracts the specified asset from the app's assets directory and copies it to the
     * specified destination directory in the app's internal storage.
     *
     * @param context The context of the app.
     * @param assetName The name of the asset to extract.
     * @param destinationDirectory The name of the destination directory.
     * @param overwrite Whether to overwrite an existing file in the destination directory.
     */
    fun extractAsset(context: Context, assetName: String, destinationDirectory: String, overwrite: Boolean = false) {
        val am = context.assets

        val destDir = File(getDataPath(context), destinationDirectory)
        if (!destDir.exists()) {
            destDir.mkdir()
        }
        val toCopy = File(destDir, assetName)
        if (!toCopy.exists() || overwrite) {
            copyFile(am, assetName, toCopy)
        }
    }

    /**
     * Copies an asset file from the app's assets directory to a destination file in the
     * app's internal storage.
     *
     * @param am The AssetManager to access the asset file.
     * @param assetName The name of the asset file to copy.
     * @param outFile The destination file to copy the asset to.
     */
    private fun copyFile(
        am: AssetManager, assetName: String,
        outFile: File
    ) {
        try {
            am.open(assetName).use { `in` ->
                FileOutputStream(outFile).use { out ->
                    val buffer = ByteArray(1024)
                    var read: Int
                    while (`in`.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}