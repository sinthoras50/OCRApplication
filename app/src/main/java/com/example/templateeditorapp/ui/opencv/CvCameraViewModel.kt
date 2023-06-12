package com.example.templateeditorapp.ui.opencv

import android.graphics.Rect
import android.graphics.RectF
import android.hardware.Camera
import android.util.Log
import androidx.core.graphics.toRect
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.room.RoomDatabase
import com.example.templateeditorapp.ui.camera.TemplateCameraFragment
import com.example.templateeditorapp.utils.TAG_IMAGE
import org.opencv.android.CameraBridgeViewBase
import org.opencv.calib3d.Calib3d
import org.opencv.core.*
import org.opencv.features2d.DescriptorMatcher
import org.opencv.features2d.ORB
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.min

enum class PreprocessMethod {
    NONE, THRESH, HOUGH, MORPH
}

enum class FlashMode(val value: String) {
    ON(Camera.Parameters.FLASH_MODE_ON),
    OFF(Camera.Parameters.FLASH_MODE_OFF),
    AUTO(Camera.Parameters.FLASH_MODE_AUTO)
}

enum class FocusMode(val value: String) {
    AUTO(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE),
    INFINITY(Camera.Parameters.FOCUS_MODE_FIXED)
}

/**
 * ViewModel class that manages the camera functionality of the [CameraFragment].
 * Provides methods to take pictures, preprocess images and perform homography to align images.
 */
class CameraViewModel(val database: RoomDatabase) : ViewModel() {

    private val _focusMode = MutableLiveData<String>(FocusMode.INFINITY.value)
    private val _flashMode = MutableLiveData<String>(FlashMode.OFF.value)

    private var cameraWidth: Int? = null
    private var cameraHeight: Int? = null

    private lateinit var preprocessMat: Mat

    var scalingFactor: Double? = null

    val focusMode: LiveData<String> = _focusMode
    val flashMode: LiveData<String> = _flashMode

    /**
     * Allocates memory of the [preprocessMat] according to the dimensions of the camera
     */
    fun initDimensions(width: Int, height: Int) {
        cameraWidth = width
        cameraHeight = height

        preprocessMat = Mat(width, height, CvType.CV_8UC1)
    }


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
     * Crops an OpenCV Mat image using the provided crop rectangle.
     *
     * @param image the input image to be cropped
     * @param cropRect the rectangle defining the region to be cropped
     * @return a new Mat image containing the cropped region
     */
    private fun cropImage(image: Mat, cropRect: Rect): Mat {
        val opencvRect = cropRect.let {
            org.opencv.core.Rect(it.left, it.top, it.width(), it.height())
        }

        val cropped = image.submat(opencvRect)
        Log.d(TAG_IMAGE, "width = ${cropped.width()} height = ${cropped.height()}")
        return cropped
    }

    /**
     * Aligns two images using the ORB feature detector and descriptor extractor.
     *
     * @param mat1 The first input image
     * @param mat2 The second input image to be aligned with the first image
     * @param maxFeatures The maximum number of features to be detected by ORB
     * @param cropRect Optional crop rectangle to apply to the second image before alignment
     * @return The aligned output image
     */
    fun alignImage(mat1: Mat, mat2: Mat, maxFeatures: Int = 500,  cropRect: RectF? = null): Mat? {
        var croppedTemplate = Mat()

        if (cropRect != null) {
            croppedTemplate = cropImage(mat2, cropRect.toRect())
        }

        val template = if (cropRect != null) croppedTemplate else mat2

        val resizedImage = Mat()
        val resizedTemplate = Mat()

        val desiredWidth = min(mat1.width(), min(template.width(), 2000)).toDouble()
        val aspectRatio1 = mat1.width().toDouble() / mat1.height()
        val aspectRatio2 = template.width().toDouble() / template.height()

        Imgproc.resize(mat1, resizedImage, Size(desiredWidth, desiredWidth / aspectRatio1))
        Imgproc.resize(template, resizedTemplate, Size(desiredWidth, desiredWidth / aspectRatio2))

        val grayImage = Mat()
        val grayTemplate = Mat()

        Imgproc.cvtColor(resizedImage, grayImage, Imgproc.COLOR_RGB2GRAY)
        Imgproc.cvtColor(resizedTemplate, grayTemplate, Imgproc.COLOR_RGB2GRAY)

        val matcher = SIFTMatcher(grayImage, grayTemplate, maxFeatures)
        val matchedPoints = matcher.matchedPoints() ?: return null

        val model = Calib3d.findHomography(matchedPoints[0], matchedPoints[1], Calib3d.RANSAC, 5.0)

        Log.d("HOMOGRAPHY", "model = ${model.width()}, ${model.height()}, ${model.type()}")

        // model needs to be a 3x3 matrix
        if (model.width() != 3 || model.height() != 3) return null

        // align images using the perspective transformation

        val h = resizedTemplate.rows()
        val w = resizedTemplate.cols()
        val aligned = Mat()

        Imgproc.warpPerspective(resizedImage, aligned, model, Size(w.toDouble(), h.toDouble()))

        scalingFactor = w.toDouble() / template.cols()

        return aligned

    }

    /**
     * Preprocesses the input image [mat] using the specified [PreprocessMethod].
     *
     * @param mat The input image to be preprocessed.
     * @param method The preprocessing method to be applied.
     *
     * @return The preprocessed image as a `Mat` object.
     */
    fun preprocess(mat: Mat, method: PreprocessMethod, cropped: Boolean = false): Mat {
        return when(method) {
            PreprocessMethod.NONE -> mat
            PreprocessMethod.THRESH -> thresh(mat, cropped)
            PreprocessMethod.HOUGH -> hough(mat)
            PreprocessMethod.MORPH -> morph(mat)
        }
    }


    /**
     * Applies a threshold operation to the input image.
     *
     * @param mat the input image as a [Mat] object.
     * @return the preprocessed image as a [Mat] object.
     */
    private fun thresh(mat: Mat, cropped: Boolean): Mat {
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY)
        val thresh = Mat()

        if (cropped) {
//            Imgproc.adaptiveThreshold(gray, thresh, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 10.0)
            Imgproc.threshold(gray, thresh, 0.0, 255.0, Imgproc.THRESH_BINARY_INV+Imgproc.THRESH_OTSU)
            Log.d("THRESH", "using otsu thresholding")
        } else {
//            Imgproc.threshold(gray, thresh, 0.0, 255.0, Imgproc.THRESH_BINARY_INV+Imgproc.THRESH_OTSU)
            Imgproc.adaptiveThreshold(gray, thresh, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 10.0)
            Log.d("THRESH", "using adaptive thresholding")
        }

        thresh.copyTo(preprocessMat)

        gray.release()
        thresh.release()

        return preprocessMat
    }

    /**
     * Applies the Hough transform to the input image.
     *
     * @param mat the input image as a [Mat] object.
     * @return the preprocessed image as a [Mat] object.
     */
    private fun hough(mat: Mat): Mat {
        val tolerance = 5.0

        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY)
        val thresh = Mat()
        Imgproc.threshold(gray, thresh, 0.0, 255.0, Imgproc.THRESH_BINARY_INV+Imgproc.THRESH_OTSU)

        val lines = Mat()
        Imgproc.HoughLinesP(thresh, lines, 1.0, Math.PI/180.0, 15, 20.0, 10.0)

        val output = Mat.zeros(gray.size(), gray.type())
        output.setTo(Scalar(255.0))

        for (line in 0 until lines.rows()) {
            val x1 = lines.get(line, 0)[0]
            val y1 = lines.get(line, 0)[1]
            val x2 = lines.get(line, 0)[2]
            val y2 = lines.get(line, 0)[3]

            if (abs(x1 - x2) < tolerance) {
                Imgproc.line(output, Point(x1, y1), Point(x2, y2), Scalar(0.0, 0.0, 0.0), 2)
            }
        }

        Core.bitwise_not(gray, gray)
        val res = Mat()

        Core.bitwise_and(thresh, thresh, res, output)
        Core.bitwise_not(res, res)

        res.copyTo(preprocessMat)

        gray.release()
        thresh.release()
        lines.release()
        output.release()
        res.release()

        return preprocessMat
    }


    /**
     * Applies morphological opening to the input image.
     *
     * @param mat The input image to be preprocessed.
     * @return The preprocessed image with morphological opening applied.
     */
    private fun morph(mat: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY)
        val thresh = Mat()
        Imgproc.threshold(gray, thresh, 0.0, 255.0, Imgproc.THRESH_BINARY_INV+Imgproc.THRESH_OTSU)
        val kernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, Size(1.0, 2.0))
        val opening = Mat()
        Imgproc.morphologyEx(thresh, opening, Imgproc.MORPH_OPEN, kernel)
        opening.copyTo(preprocessMat)

        gray.release()
        thresh.release()
        kernel.release()
        opening.release()

        return preprocessMat
    }

    /**
     * This function is called when the flash button is clicked. It cycles through the different flash modes (on, auto, off).
     */
    fun onClickFlash(flashModes: List<String>) {

        if (FlashMode.ON.value !in flashModes) return

        when(_flashMode.value) {
            FlashMode.ON.value -> _flashMode.value = FlashMode.AUTO.value
            FlashMode.AUTO.value -> _flashMode.value = FlashMode.OFF.value
            FlashMode.OFF.value -> _flashMode.value = FlashMode.ON.value
        }
    }

    /**
     * This function is called when the focus button is clicked. It cycles through the different focus modes (fixed, auto).
     */
    fun onClickFocus(focusModes: List<String>) {

        if (FocusMode.AUTO.value !in focusModes) return

        when(_focusMode.value) {
            FocusMode.INFINITY.value -> _focusMode.value = FocusMode.AUTO.value
            FocusMode.AUTO.value -> _focusMode.value = FocusMode.INFINITY.value
        }
    }


}