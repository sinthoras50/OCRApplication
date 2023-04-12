package com.example.templateeditorapp.utils

import android.content.Context
import android.content.res.AssetManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object Assets {

    fun getTessDataPath(context: Context): String {
        return context.filesDir.absolutePath
    }

    val language: String
        get() = "slk"


    fun extractAssets(context: Context) {
        val am = context.assets

        val tessDir = File(getTessDataPath(context), "tessdata")
        if (!tessDir.exists()) {
            tessDir.mkdir()
        }
        val engFile = File(tessDir, "$language.traineddata")
        if (!engFile.exists()) {
            copyFile(am, "$language.traineddata", engFile)
        }
    }


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