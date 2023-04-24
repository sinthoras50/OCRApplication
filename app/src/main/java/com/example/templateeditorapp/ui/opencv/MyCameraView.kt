package com.example.templateeditorapp.ui.opencv

import android.content.Context
import android.graphics.Bitmap
import android.hardware.Camera
import android.hardware.Camera.Parameters.FLASH_MODE_ON
import android.hardware.Camera.Parameters.FOCUS_MODE_AUTO
import android.hardware.Camera.PictureCallback
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.templateeditorapp.R
import com.example.templateeditorapp.ui.testing.TestingFragment
import kotlinx.coroutines.*
import org.opencv.android.JavaCameraView
import java.io.FileOutputStream
import java.io.IOException

class MyCameraView(context: Context?, attrs: AttributeSet?) : JavaCameraView(context, attrs) {

    private val TAG = "myCameraView"

    private lateinit var pictureTakenListener: PictureTakenListener
    private var mPictureFileName: String? = null

    fun setOnPictureTakenListener(listener: PictureTakenListener) {
        pictureTakenListener = listener
    }

    fun setFlash(mode: String) {
        val params = mCamera.parameters
        params.flashMode = mode
        mCamera.parameters = params
    }

    fun setFocus(mode: String) {
        val params = mCamera.parameters
        params.focusMode = mode
        mCamera.parameters = params
    }

    fun takePicture(fileName: String?) {
        Log.i(TAG, "Taking picture")
        mPictureFileName = fileName
        mCamera.setPreviewCallback(null)

        val sizes = mCamera.parameters.supportedPictureSizes
        sizes.sortBy { it.height + it.width }
        val effects = mCamera.parameters.supportedColorEffects
        Log.i(TAG, "sizes: ||| ${sizes.joinToString(separator=", ") { "w = ${it.width}, h = ${it.height}" } }")
        Log.i(TAG, "effects: ||| ${effects.joinToString(separator=", ") }")
        val params = mCamera.parameters
        params.setRotation(90)
        // params that dont work on android emulator
//        params.focusMode = FOCUS_MODE_AUTO
//        params.flashMode = FLASH_MODE_ON
        params.jpegQuality = 75

        val size = sizes.last()
        Log.i(TAG, "size = ${size.width}, ${size.height}")
        params.setPictureSize(size.width, size.height)
        mCamera.parameters = params
        mCamera.setPreviewCallback(null)
        mCamera.takePicture(null, null) { data, camera ->
//            mCamera.startPreview()
//            mCamera.setPreviewCallback(this@MyCameraView)
            context.openFileOutput("$mPictureFileName.jpg", Context.MODE_PRIVATE).use { stream ->
                stream.write(data)
                Log.i(TAG, "File saved")
                pictureTakenListener.onPictureTaken()
            }
        }
    }
}