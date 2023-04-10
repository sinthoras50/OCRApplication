package com.example.templateeditorapp.utils

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.io.IOException

const val TAG_IMAGE = "TAG_IMAGE"

object ImageUtils {

    fun bitmapToMat(bitmap: Bitmap): Mat {
        val res = Mat()
        try {
            Utils.bitmapToMat(bitmap, res)
        } catch (e: CvException) {
            Log.d(TAG_IMAGE, "Exception ${e.message}")
        }

        return res
    }

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

    fun deletePhoto(filename: String, context: Context): Boolean {
        return try {
            context.deleteFile(filename)
        } catch(e: IOException) {
            e.printStackTrace()
            false
        }
    }

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

    fun savePhotoToExternalStorage(filename: String, bitmap: Bitmap, context: Context): Boolean {
        val imageCollection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
        }
        return try {
            context.contentResolver.insert(imageCollection, contentValues)?.also { uri ->
                context.contentResolver.openOutputStream(uri).use { outputStream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                        throw IOException("Couldn't save bitmap")
                    }
                }
            } ?: throw IOException("Couldn't save bitmap")
            true
        } catch(e: IOException) {
            e.printStackTrace()
            false
        }
    }


}