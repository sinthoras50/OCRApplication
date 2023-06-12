package com.example.templateeditorapp.utils

import android.content.Context
import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.graphics.RectF
import com.example.templateeditorapp.db.AnnotatedImage
import com.example.templateeditorapp.db.ImageDatabase
import com.example.templateeditorapp.ui.editor.BoundingBox
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class Field(
    @SerializedName("fieldName") val fieldName: String,
    @SerializedName("x0") val x0: Float,
    @SerializedName("y0") val y0: Float,
    @SerializedName("x1") val x1: Float,
    @SerializedName("y1") val y1: Float
)

/**
 * A singleton object that provides utility functions to manage assets in an Android app.
 * This object provides methods to extract assets from the app's assets directory, copy them
 * to the app's internal storage, and manage the file paths for those assets.
 */
object Assets {


    /**
     * Creates a standard postal cheque entry in the specified database.
     *
     * @param context The context of the app.
     * @param destinationDirectory The name of the destination directory where the cheque image should be stored.
     * @param db The Room Database of the app.
     */
    fun createDefaultChequeEntry(context: Context, destinationDirectory: String, db: ImageDatabase) {
        extractAsset(context, "Postal Cheque.png", destinationDirectory)
        extractAsset(context, "Postal Cheque.json", destinationDirectory)

        val file = File("${getDataPath(context)}/Postal Cheque.json")
        val jsonString = file.readText()

        val fieldListType = object : TypeToken<List<Field>>() {}.type
        val fieldList: List<Field> = Gson().fromJson(jsonString, fieldListType)

        val boundingBoxes = mutableListOf<BoundingBox>()
        for (field in fieldList) {
            boundingBoxes.add(BoundingBox(field.x0, field.y0, field.x1, field.y1, field.fieldName))
        }

        val image = File("${getDataPath(context)}/Postal Cheque.png")
        val bytes = image.readBytes()

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true

        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        val width = options.outWidth
        val height = options.outHeight

        val annotatedImage = AnnotatedImage("Postal Cheque", boundingBoxes, RectF(
            0f, 0f, width.toFloat()-0f, height.toFloat()-0f)
        )

        GlobalScope.launch {
            db.annotatedImageDao().insertImage(annotatedImage)
        }
    }

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

        val destDir = if (destinationDirectory.isBlank()) File(getDataPath(context)) else File(getDataPath(context), destinationDirectory)
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