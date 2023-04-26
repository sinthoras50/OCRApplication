package com.example.templateeditorapp.ui.camera

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.templateeditorapp.ui.opencv.FlashMode
import com.example.templateeditorapp.ui.opencv.FocusMode
import com.example.templateeditorapp.ui.opencv.MyCameraView
import com.example.templateeditorapp.ui.opencv.PreprocessMethod
import com.example.templateeditorapp.utils.TAG_IMAGE
import org.opencv.android.CameraBridgeViewBase
import org.opencv.calib3d.Calib3d
import org.opencv.core.*
import org.opencv.features2d.DescriptorMatcher
import org.opencv.features2d.ORB
import org.opencv.imgproc.Imgproc
import java.math.BigInteger
import kotlin.math.abs
import kotlin.math.min

/**
 * ViewModel class that manages the camera functionality of the [TemplateCameraFragment].
 * Provides methods to take pictures, change focus mode and flash mode, and draw a grid over an OpenCV Mat object.
 */
class TemplateCameraViewModel : ViewModel() {

    private val _focusMode = MutableLiveData<String>(FocusMode.FIXED.value)
    private val _flashMode = MutableLiveData<String>(FlashMode.OFF.value)

    val focusMode: LiveData<String> = _focusMode
    val flashMode: LiveData<String> = _flashMode

    /**
     * Takes a picture using the camera specified by the given [CameraBridgeViewBase].
     * @param filename The name of the file to save the picture to.
     * @param bridgeViewBase The CameraBridgeViewBase object representing the camera.
     */
    fun takePicture(filename: String, bridgeViewBase: CameraBridgeViewBase) {
        val camera = bridgeViewBase as MyCameraView
        camera.setFlash(_flashMode.value!!)
        camera.setFocus(_focusMode.value!!)
        camera.takePicture(filename)
    }

    /**
     * Toggles the flash mode between ON, AUTO, and OFF.
     */
    fun onClickFlash() {
        when(_flashMode.value) {
            FlashMode.ON.value -> _flashMode.value = FlashMode.AUTO.value
            FlashMode.AUTO.value -> _flashMode.value = FlashMode.OFF.value
            FlashMode.OFF.value -> _flashMode.value = FlashMode.ON.value
        }
    }

    /**
     * Toggles the focus mode between FIXED and AUTO.
     */
    fun onClickFocus() {
        when(_focusMode.value) {
            FocusMode.FIXED.value -> _focusMode.value = FocusMode.AUTO.value
            FocusMode.AUTO.value -> _focusMode.value = FocusMode.FIXED.value
        }
    }

    /**
     * Draws a grid over the given OpenCV [Mat] object.
     * @param mat The Mat object to draw the grid on.
     * @param thickness The thickness of the lines of the grid.
     * @return The modified Mat object.
     */
    fun drawGrid(mat: Mat, thickness: Int): Mat {
        val gcd = BigInteger.valueOf(mat.width().toLong())
            .gcd(BigInteger.valueOf(mat.height().toLong()))
            .toInt()

        val w = mat.width() / gcd
        val h = mat.height() / gcd
        val wIncrement = mat.width().toDouble() / w
        val hIncrement = mat.height().toDouble() / h
        val color = Scalar(0.0, 0.0, 0.0)

        for (i in 1 until w) {
            Imgproc.line(mat, Point(i * wIncrement, 0.0), Point(i * wIncrement, mat.height().toDouble()), color, thickness)
        }

        for (i in 1 until h) {
            Imgproc.line(mat, Point(0.0, i * hIncrement), Point(mat.width().toDouble(), i * hIncrement), color, thickness)
        }

        Log.d(TAG_IMAGE, "gcd = $gcd w = $w h = $h")
        return mat
    }
}