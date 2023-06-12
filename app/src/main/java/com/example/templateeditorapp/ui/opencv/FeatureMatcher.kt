package com.example.templateeditorapp.ui.opencv

import android.util.Log
import org.opencv.core.*
import org.opencv.features2d.BFMatcher
import org.opencv.features2d.DescriptorMatcher
import org.opencv.features2d.ORB
import org.opencv.features2d.SIFT

interface FeatureMatcher {

    fun matchedPoints(): Array<MatOfPoint2f>?
}

class ORBMatcher(private val image1: Mat, private val image2: Mat, private val maxFeatures: Int) : FeatureMatcher {
    override fun matchedPoints(): Array<MatOfPoint2f>? {
        val orb = ORB.create(maxFeatures)
        val kp1 = MatOfKeyPoint()
        val kp2 = MatOfKeyPoint()
        val descsA = Mat()
        val descsB = Mat()
        orb.detectAndCompute(image1, Mat(), kp1, descsA)
        orb.detectAndCompute(image2, Mat(), kp2, descsB)

        val matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING)
        val matches = MatOfDMatch()
        matcher.match(descsA, descsB, matches)

        val matchesList = matches.toList()
        matchesList.sortBy { it.distance }

        // keep only top 20% of the matches
        val reducedMatches = matchesList.subList(0, (0.2 * matchesList.size).toInt())

        // findHomography requires at least 4 matches, so we return null
        if (reducedMatches.size < 4) return null

        // homography matrix
        val pts1 = mutableListOf<Point>()
        val pts2 = mutableListOf<Point>()

        val keypoints1 = kp1.toArray()
        val keypoints2 = kp2.toArray()


        Log.d("HOMOGRAPHY", "matches length = ${reducedMatches.size}")
        for (match in reducedMatches) {
            pts1.add(keypoints1[match.queryIdx].pt)
            pts2.add(keypoints2[match.trainIdx].pt)
        }

        val pts1Mat = MatOfPoint2f(*pts1.toTypedArray())
        val pts2Mat = MatOfPoint2f(*pts2.toTypedArray())

        return arrayOf(pts1Mat, pts2Mat)
    }
}

class SIFTMatcher(private val image1: Mat, private val image2: Mat, private val maxFeatures: Int) : FeatureMatcher {
    override fun matchedPoints(): Array<MatOfPoint2f>? {
        val sift = SIFT.create(maxFeatures)
        val kp1 = MatOfKeyPoint()
        val kp2 = MatOfKeyPoint()
        val descsA = Mat()
        val descsB = Mat()
        sift.detectAndCompute(image1, Mat(), kp1, descsA)
        sift.detectAndCompute(image2, Mat(), kp2, descsB)

        val matcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED)
        val matches = mutableListOf<MatOfDMatch>()

        matcher.knnMatch(descsA, descsB, matches, 2)

        // D. Lowe test
        val goodMatches = mutableListOf<DMatch>()

        for (match in matches) {
            val (m, n) = match.toList()

            if (m.distance < 0.75*n.distance) {
                goodMatches.add(m)
            }
        }

        // homography matrix
        val pts1 = mutableListOf<Point>()
        val pts2 = mutableListOf<Point>()

        val keypoints1 = kp1.toArray()
        val keypoints2 = kp2.toArray()

        for (match in goodMatches) {
            pts1.add(keypoints1[match.queryIdx].pt)
            pts2.add(keypoints2[match.trainIdx].pt)
        }

        val pts1Mat = MatOfPoint2f(*pts1.toTypedArray())
        val pts2Mat = MatOfPoint2f(*pts2.toTypedArray())

        return arrayOf(pts1Mat, pts2Mat)

    }

}