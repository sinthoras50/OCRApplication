package com.example.templateeditorapp.ui.opencv

import android.hardware.Camera
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.room.RoomDatabase
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
    AUTO(Camera.Parameters.FOCUS_MODE_AUTO),
    FIXED(Camera.Parameters.FOCUS_MODE_FIXED)
}

class CameraViewModel(database: RoomDatabase) : ViewModel() {

    private val _focusMode = MutableLiveData<String>(FocusMode.FIXED.value)
    private val _flashMode = MutableLiveData<String>(FlashMode.OFF.value)

    private var cameraWidth: Int? = null
    private var cameraHeight: Int? = null

    private lateinit var preprocessMat: Mat

    val focusMode: LiveData<String> = _focusMode
    val flashMode: LiveData<String> = _flashMode

    fun initDimensions(width: Int, height: Int) {
        cameraWidth = width
        cameraHeight = height

        preprocessMat = Mat(width, height, CvType.CV_8UC1)
    }

    fun takePicture(filename: String, bridgeViewBase: CameraBridgeViewBase) {
        val camera = bridgeViewBase as MyCameraView
        camera.setFlash(_flashMode.value!!)
        camera.setFocus(_focusMode.value!!)
        camera.takePicture(filename)
    }

    fun alignImage(mat1: Mat, mat2: Mat, maxFeatures: Int = 500, keepPercent: Double = 0.2): Mat {
        val resizedImage = Mat()
        val resizedTemplate = Mat()

        val desiredWidth = min(mat1.width(), mat2.width()).toDouble()
        val aspectRatio1 = mat1.width().toDouble() / mat1.height()
        val aspectRatio2 = mat2.width().toDouble() / mat2.height()

        Imgproc.resize(mat1, resizedImage, Size(desiredWidth, desiredWidth / aspectRatio1))
        Imgproc.resize(mat2, resizedTemplate, Size(desiredWidth, desiredWidth / aspectRatio2))

        val grayImage = Mat()
        val grayTemplate = Mat()

        Imgproc.cvtColor(resizedImage, grayImage, Imgproc.COLOR_RGB2GRAY)
        Imgproc.cvtColor(resizedTemplate, grayTemplate, Imgproc.COLOR_RGB2GRAY)


        val orb = ORB.create(maxFeatures)
        val kp1 = MatOfKeyPoint()
        val kp2 = MatOfKeyPoint()
        val descsA = Mat()
        val descsB = Mat()
        orb.detectAndCompute(grayImage, Mat(), kp1, descsA)
        orb.detectAndCompute(grayTemplate, Mat(), kp2, descsB)

        val matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING)
        val matches = MatOfDMatch()
        matcher.match(descsA, descsB, matches)

        val matchesList = matches.toList()
        matchesList.sortBy { it.distance }

        val reducedMatches = matchesList.subList(0, (keepPercent * matchesList.size).toInt())

        // homography matrix
        val pts1 = mutableListOf<Point>()
        val pts2 = mutableListOf<Point>()

        val keypoints1 = kp1.toArray()
        val keypoints2 = kp2.toArray()

        for (match in reducedMatches) {
            pts1.add(keypoints1[match.queryIdx].pt)
            pts2.add(keypoints2[match.trainIdx].pt)
        }

        val pts1Mat = MatOfPoint2f(*pts1.toTypedArray())
        val pts2Mat = MatOfPoint2f(*pts2.toTypedArray())

        val model = Calib3d.findHomography(pts1Mat, pts2Mat, Calib3d.RANSAC, 5.0)

        // align images using the perspective transformation

        val h = resizedTemplate.rows()
        val w = resizedTemplate.cols()
        val aligned = Mat()

        Imgproc.warpPerspective(resizedImage, aligned, model, Size(w.toDouble(), h.toDouble()))

        return aligned

    }

    fun preprocess(mat: Mat, method: PreprocessMethod): Mat {
        return when(method) {
            PreprocessMethod.NONE -> mat
            PreprocessMethod.THRESH -> thresh(mat)
            PreprocessMethod.HOUGH -> hough(mat)
            PreprocessMethod.MORPH -> morph(mat)
        }
    }

    private fun thresh(mat: Mat): Mat {
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY)
        val thresh = Mat()
        Imgproc.threshold(gray, thresh, 0.0, 255.0, Imgproc.THRESH_BINARY_INV+Imgproc.THRESH_OTSU)

        thresh.copyTo(preprocessMat)

        gray.release()
        thresh.release()

        return preprocessMat
    }

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

    fun onClickFlash() {
        when(_flashMode.value) {
            FlashMode.ON.value -> _flashMode.value = FlashMode.AUTO.value
            FlashMode.AUTO.value -> _flashMode.value = FlashMode.OFF.value
            FlashMode.OFF.value -> _flashMode.value = FlashMode.ON.value
        }
    }

    fun onClickFocus() {
        when(_focusMode.value) {
            FocusMode.FIXED.value -> _focusMode.value = FocusMode.AUTO.value
            FocusMode.AUTO.value -> _focusMode.value = FocusMode.FIXED.value
        }
    }


}