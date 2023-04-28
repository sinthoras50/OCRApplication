package com.example.templateeditorapp

import android.content.Context
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.templateeditorapp.db.AnnotatedImage
import com.example.templateeditorapp.db.ImageDatabase
import com.example.templateeditorapp.ui.editor.ImageData
import com.example.templateeditorapp.utils.ImageUtils
import com.example.templateeditorapp.utils.TAG_IMAGE
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class SharedViewModel(val database: ImageDatabase) : ViewModel() {

    private val annotatedImages = mutableListOf<AnnotatedImage>()

    private val _isLoading = MutableLiveData<Boolean>(true)
    private val _imageSet = MutableLiveData<List<Bitmap>>()
    private val _currentImageName = MutableLiveData<String>("")
    private val _currentIdx = MutableLiveData<Int>(0)

    val isLoading: LiveData<Boolean> = _isLoading
    val imageSet: LiveData<List<Bitmap>> = _imageSet
    val currentImageName: LiveData<String> = _currentImageName
    val currentIdx: LiveData<Int> = _currentIdx

    var currentImageBoundingBox: RectF? = null
    var currentHighResBitmap: Bitmap? = null

    /**
     * Loads the annotated images from the database and updates the current set of images.
     * @param context: The context used to load the images.
     * @param reqWidth: The required width for the loaded images.
     * @param reqHeight: The required height for the loaded images.
     */
    fun loadImages(context: Context, reqWidth: Int, reqHeight: Int) {
        if (annotatedImages.isNotEmpty()) return

        viewModelScope.launch {
            annotatedImages.addAll(database.annotatedImageDao().getAllImages())

            if (annotatedImages.isEmpty()) return@launch

            _currentIdx.value = 0
            currentImageBoundingBox = annotatedImages[_currentIdx.value!!].cropRect
            _currentImageName.value = annotatedImages[_currentIdx.value!!].imageName
            _imageSet.value = annotatedImages.mapNotNull { ImageUtils.loadPhoto(it.imageName, context, reqWidth, reqHeight) }
            _isLoading.value = false

            withContext(Dispatchers.IO) {
                currentHighResBitmap = ImageUtils.loadPhoto(_currentImageName.value!!, context)
            }
        }
    }

    fun getIndexOf(name: String): Int {
        return annotatedImages.indexOfFirst { it.imageName == name }
    }

    fun saveTemplate(context: Context, annotatedImage: AnnotatedImage, bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                database.annotatedImageDao().insertImage(annotatedImage)
                ImageUtils.savePhoto(annotatedImage.imageName, bitmap, context)

                val idx = getIndexOf(annotatedImage.imageName)

                val updatedList: List<Bitmap>?
                if (idx == -1) {
                    annotatedImages.add(annotatedImage)

                    updatedList = _imageSet.value!!.let {
                        val lst = it.toMutableList()
                        lst.add(bitmap)
                        lst.toList()
                    }

                } else {
                    annotatedImages[idx] = annotatedImage

                    updatedList = _imageSet.value!!.let {
                        val lst = it.toMutableList()
                        lst[idx] = bitmap
                        lst.toList()
                    }
                }

                _imageSet.value = updatedList!!
                _currentIdx.value = getIndexOf(annotatedImage.imageName)
                _currentImageName.value = annotatedImages[_currentIdx.value!!].imageName

                currentImageBoundingBox = annotatedImages[_currentIdx.value!!].cropRect

                withContext(Dispatchers.IO) {
                    currentHighResBitmap = ImageUtils.loadPhoto(_currentImageName.value!!, context)
                }


            } catch (e: SQLiteException) {
                e.printStackTrace()
            }
        }
    }

    fun loadTemplate(filename: String): Template? {
        val idx = getIndexOf(filename)

        if (idx == -1) return null

        return Template(annotatedImages[idx], currentHighResBitmap!!)
    }

    fun deleteTemplate(context: Context, filename: String?): Boolean {
        if (filename == null) return false

        val idx = annotatedImages.indexOfFirst { it.imageName == filename }

        if (idx == -1) return false

        ImageUtils.deletePhoto(filename, context)

        viewModelScope.launch {
            database.annotatedImageDao().deleteImage(filename)
        }

        annotatedImages.removeAt(idx)

        val updatedList = _imageSet.value!!.let {
            val lst = it.toMutableList()
            lst.removeAt(idx)
            lst.toList()
        }

        _currentIdx.value = (_currentIdx.value!!).coerceAtMost((annotatedImages.size-1).coerceAtLeast(0))
        currentImageBoundingBox = if (annotatedImages.size > 0) annotatedImages[_currentIdx.value!!].cropRect else null

        _currentImageName.value = if (annotatedImages.size > 0) annotatedImages[_currentIdx.value!!].imageName else ""
        _imageSet.value = updatedList

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                currentHighResBitmap = ImageUtils.loadPhoto(_currentImageName.value!!, context)
            }
        }

        return true
    }

    /**
     * Loads the next image in the current set of images.
     * @param context Context used to load high res photo from the app's storage
     *
     * @return true if there is a next image, false otherwise.
     */
    fun loadNextPhoto(context: Context): Boolean {
        if (_currentIdx.value!!+1 >= annotatedImages.size) return false

        _currentIdx.value = _currentIdx.value!! + 1
        currentImageBoundingBox = annotatedImages[_currentIdx.value!!].cropRect
        _currentImageName.value = annotatedImages[_currentIdx.value!!].imageName

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                currentHighResBitmap = ImageUtils.loadPhoto(_currentImageName.value!!, context)
            }
        }


        return true
    }

    /**
     * Loads the previous image in the current set of images.
     * @param context Context used to load high res photo from the app's storage
     *
     * @return true if there is a previous image, false otherwise.
     */
    fun loadPreviousPhoto(context: Context): Boolean {
        if (_currentIdx.value!!-1 < 0) return false

        _currentIdx.value = _currentIdx.value!! - 1
        currentImageBoundingBox = annotatedImages[_currentIdx.value!!].cropRect
        _currentImageName.value = annotatedImages[_currentIdx.value!!].imageName

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                currentHighResBitmap = ImageUtils.loadPhoto(_currentImageName.value!!, context)
            }
        }

        return true
    }
}