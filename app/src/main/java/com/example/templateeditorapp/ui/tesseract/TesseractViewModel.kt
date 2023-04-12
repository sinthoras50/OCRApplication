package com.example.templateeditorapp.ui.tesseract

import android.app.Application
import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.core.graphics.component3
import androidx.core.graphics.component4
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.RoomDatabase
import com.example.templateeditorapp.db.ImageDatabase
import com.example.templateeditorapp.ui.editor.BoundingBox
import com.example.templateeditorapp.utils.ImageUtils
import com.example.templateeditorapp.utils.TAG_IMAGE
import com.googlecode.tesseract.android.TessBaseAPI
import com.googlecode.tesseract.android.TessBaseAPI.ProgressValues
import kotlinx.coroutines.*
import java.io.File
import java.util.*

class TesseractViewModel(private val database: ImageDatabase) : ViewModel() {
    private val TAG = "MainViewModel"

    private var tessApi: TessBaseAPI

    private val _progress = MutableLiveData<Int>()
    private val _partialProgress = MutableLiveData<Int>()
    private val _resultMap: MutableLiveData<MutableMap<String, String>> = MutableLiveData(mutableMapOf())

    private var tessInit = false
    private var stopped = false
    private var processing = false


    val resultMap: LiveData<MutableMap<String, String>> = _resultMap
    val progress: LiveData<Int> = _progress

    init {
        tessApi =
            TessBaseAPI { progressValues: ProgressValues -> _partialProgress.postValue(progressValues.percent) }

    }

    override fun onCleared() {
        if (isProcessing()) {
            tessApi.stop()
        }

        tessApi.recycle()
    }

    fun initTesseract(dataPath: String, language: String, engineMode: Int) {
        Log.i(
            TAG, "Initializing Tesseract with: dataPath = [" + dataPath + "], " +
                    "language = [" + language + "], engineMode = [" + engineMode + "]"
        )
        try {
            tessInit = tessApi.init(dataPath, language, engineMode)
        } catch (e: IllegalArgumentException) {
            tessInit = false
            Log.e(TAG, "Cannot initialize Tesseract:", e)
        }
    }

    fun recognizeImage(imagePath: String, templatePath: String, scalingFactor: Double, context: Context) {
        if (!tessInit) {
            Log.e(TAG, "recognizeImage: Tesseract is not initialized")
            return
        }
        if (isProcessing()) {
            Log.e(TAG, "recognizeImage: Processing is in progress")
            return
        }

        _resultMap.value = mutableMapOf()
        _progress.value = 0
        processing = true
        stopped = false



        // Start process in a coroutine

        viewModelScope.launch(Dispatchers.Default) {

            var boundingBoxes: List<BoundingBox>? = null

            withContext(Dispatchers.IO) {
                val annotatedImage = database.annotatedImageDao().getImage(templatePath)
                boundingBoxes = annotatedImage?.boundingBoxes
            }

            val bitmap = ImageUtils.loadPhoto(imagePath, context)

            if (boundingBoxes.isNullOrEmpty() || bitmap == null) {
                stopped = true
                processing = false
                _progress.postValue(100)
                return@launch
            }

            val progressIncrements = 100 / boundingBoxes!!.size

            tessApi.setImage(bitmap)

            boundingBoxes!!.forEach {

                if (stopped) {
                    return@forEach
                }

                val (x, y, w, h) = it.rect.run {
                    listOf(
                        left * scalingFactor,
                        top * scalingFactor,
                        (right - left) * scalingFactor,
                        (bottom - top) * scalingFactor
                    )
                }

                tessApi.setRectangle(
                    x.toInt(),
                    y.toInt(),
                    w.toInt(),
                    h.toInt())
                tessApi.getHOCRText(0)
                val text = tessApi.utF8Text

                Log.d(TAG_IMAGE, "field = ${it.fieldName} bbox = [$x, $y, $w, $h] text = $text")
                val newMap = _resultMap.value?.toMutableMap()?.apply { put(it.fieldName, text) }
                _resultMap.postValue(newMap!!)
                _progress.postValue(_progress.value!! + progressIncrements)
            }

            _progress.postValue(100)
            stopped = true
            processing = false
        }
    }

    suspend fun loadTemplate(templateName: String): List<BoundingBox>? {
        val annotatedImage = database.annotatedImageDao().getImage(templateName)
        return annotatedImage?.boundingBoxes
    }

    fun stop() {
        if (!isProcessing()) {
            return
        }
        tessApi.stop()
        stopped = true
    }

    fun isProcessing(): Boolean {
        return processing
    }

    fun isInitialized(): Boolean {
        return tessInit
    }


}