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


/**
 * A custom camera view that extends the JavaCameraView class and adds additional functionality
 * for taking pictures, setting flash and focus modes, and providing callbacks for when a picture is taken.
 *
 * @param context The context used for creating the camera view.
 * @param attrs The attribute set used for configuring the camera view.
 */
class MyCameraView(context: Context?, attrs: AttributeSet?) : JavaCameraView(context, attrs) {

    private val TAG = "myCameraView"

    /**
     * The listener to be called when a picture is taken.
     */
    private lateinit var pictureTakenListener: PictureTakenListener

    /**
     * The file name to be used when saving the picture.
     */
    private var mPictureFileName: String? = null


    /**
     * Sets the listener to be called when a picture is taken.
     *
     * @param listener The listener to be called.
     */
    fun setOnPictureTakenListener(listener: PictureTakenListener) {
        pictureTakenListener = listener
    }


    /**
     * Sets the flash mode of the camera.
     *
     * @param mode The flash mode to be set.
     */
    fun setFlash(mode: String) {
        val params = mCamera.parameters
        params.flashMode = mode
        mCamera.parameters = params
    }


    /**
     * Sets the focus mode of the camera.
     *
     * @param mode The focus mode to be set.
     */
    fun setFocus(mode: String) {
        val params = mCamera.parameters
        params.focusMode = mode
        mCamera.parameters = params
    }

    /**
     * Takes a picture and saves it to the device.
     *
     * @param fileName The name to be used when saving the picture.
     */
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