package com.example.templateeditorapp.utils

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.templateeditorapp.ui.editor.DEBUG
import org.opencv.android.Utils
import org.opencv.calib3d.Calib3d
import org.opencv.core.*
import org.opencv.features2d.DescriptorMatcher
import org.opencv.features2d.ORB
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.IOException

const val TAG_IMAGE = "TAG_IMAGE"
/**
 * Singleton object containing utility functions for working with images.
 */
object ImageUtils {

    /**
     * Converts a [Bitmap] image to a [Mat] image, which is the image format used by OpenCV.
     * @param bitmap the bitmap image to convert
     * @return the [Mat] image converted from the [bitmap]
     */
    fun bitmapToMat(bitmap: Bitmap): Mat {
        val res = Mat()
        try {
            Utils.bitmapToMat(bitmap, res)
        } catch (e: CvException) {
            Log.d(TAG_IMAGE, "Exception ${e.message}")
        }

        return res
    }

    /**
     * Converts a [Mat] image to a [Bitmap] image.
     * @param mat the Mat image to convert
     * @return the [Bitmap] image converted from the [mat]
     */
    fun matToBitmap(mat: Mat): Bitmap? {
        var bmp : Bitmap? = null
        try {
            bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(mat, bmp)
        }
        catch (e: CvException) {
            Log.d(TAG_IMAGE, "Exception ${e.message}")
        }
        return bmp
    }

    /**
     * Loads a photo from the internal storage of the app.
     * @param filename the name of the file to load
     * @param context the context of the app
     * @return the loaded [Bitmap] image, or null if the file could not be found or loaded
     */
    fun loadPhoto(filename: String, context: Context): Bitmap? {
        return try {
            val file = context.filesDir.listFiles()?.find { it.nameWithoutExtension == filename } ?: throw IOException("File could not be found")
            val bytes = file.readBytes()
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            bitmap
        } catch(e: IOException) {
            Log.d(DEBUG, e.toString())
            e.printStackTrace()
            null
        }
    }

    /**
     * Loads a photo from the internal storage of the app, scaling it to the given dimensions if necessary.
     * @param filename the name of the file to load
     * @param context the context of the app
     * @param reqWidth the maximum required width of the image
     * @param reqHeight the maximum required height of the image
     * @return the loaded and possibly scaled [Bitmap] image, or null if the file could not be found or loaded
     */
    fun loadPhoto(filename: String, context: Context, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val file = context.filesDir.listFiles()?.find { it.nameWithoutExtension == filename } ?: throw IOException("File could not be found")
            val bytes = file.readBytes()

            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true

            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            val width = options.outWidth
            val height = options.outHeight

            Log.d(TAG_IMAGE, "original width = $width, original height = $height")

            if (width <= reqWidth || height <= reqHeight) {
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                bitmap
            } else {
                var sampleSize = 1

                while (width / sampleSize > reqWidth && height / sampleSize > reqHeight) {
                    sampleSize *= 2
                }

                options.inSampleSize = sampleSize
                options.inJustDecodeBounds = false

                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                Log.d(TAG_IMAGE, "scaled width = ${bitmap.width}, scaled height = ${bitmap.height}")
                bitmap
            }
        } catch(e: IOException) {
            Log.d(DEBUG, e.toString())
            e.printStackTrace()
            null
        }
    }

    /**
     * Deletes the photo with the given [filename] from the app's internal storage.
     * @param filename The name of the file to be deleted.
     * @param context The context of the app.
     * @return `true` if the file was successfully deleted, `false` otherwise.
     */
    fun deletePhoto(filename: String, context: Context): Boolean {
        return try {
            context.deleteFile(filename)
        } catch(e: IOException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Saves the given [bitmap] to internal storage with the specified filename.
     *
     * @param filename The name of the file to save the bitmap to.
     * @param bitmap The bitmap to be saved.
     * @param context The context of the current state of the application.
     * @return Returns `true` if the bitmap was successfully saved, `false` otherwise.
     */
    fun savePhoto(filename: String, bitmap: Bitmap, context: Context): Boolean {
        return try {
            context.openFileOutput("$filename.jpg", Context.MODE_PRIVATE).use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                    throw IOException("Couldn't save bitmap.")
                }
            }
            true

        } catch(e: IOException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Saves a [Bitmap] image to the external storage, with the given [filename].
     *
     * @param filename the name of the file to save the image to
     * @param bitmap the bitmap image to save
     * @param context the context in which to save the image
     * @return `true` if the image was successfully saved, `false` otherwise
     * @throws IOException if there was an error during the saving process
     */
    fun savePhotoToExternalStorage(filename: String, bitmap: Bitmap, context: Context): Boolean {
        val imageCollection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
        }
        return try {
            val uri = context.contentResolver.insert(imageCollection, contentValues)?.also { uri ->
                context.contentResolver.openOutputStream(uri).use { outputStream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                        throw IOException("Couldn't save bitmap")
                    }
                }
            } ?: throw IOException("Couldn't save bitmap")

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val file = File(uri.path)
                val newFile = File(file.parent, "$filename.jpg")
                if (file.renameTo(newFile)) {
                    context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(newFile)))
                }
            }

            true
        } catch(e: IOException) {
            e.printStackTrace()
            false
        }
    }
}