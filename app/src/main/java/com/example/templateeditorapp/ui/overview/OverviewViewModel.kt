package com.example.templateeditorapp.ui.overview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.Size
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.templateeditorapp.db.AnnotatedImage
import com.example.templateeditorapp.db.ImageDatabase
import com.example.templateeditorapp.ui.editor.DEBUG
import com.example.templateeditorapp.utils.ImageUtils
import kotlinx.coroutines.launch
import java.io.IOException

class OverviewViewModel(val database: ImageDatabase) : ViewModel() {

    private val annotatedImages = mutableListOf<AnnotatedImage>()
    private val _currentImage = MutableLiveData<Bitmap>()
    private val _currentImageName = MutableLiveData<String>()

    val currentImageName: LiveData<String> = _currentImageName
    val currentImage: LiveData<Bitmap> = _currentImage

    fun loadImages(context: Context, reqWidth: Int, reqHeight: Int) {
        viewModelScope.launch {
            annotatedImages.clear()
            annotatedImages.addAll(database.annotatedImageDao().getAllImages())

            if (annotatedImages.isNotEmpty()) {
                val current = if (_currentImageName.value.isNullOrEmpty()) annotatedImages.first().imageName else _currentImageName.value
                _currentImage.value = ImageUtils.loadPhoto(current!!, context, reqWidth, reqHeight)
                _currentImageName.value = current!!
            }
        }
    }

    fun loadImages(context: Context, imageName: String, reqWidth: Int, reqHeight: Int) {
        viewModelScope.launch {
            annotatedImages.clear()
            annotatedImages.addAll(database.annotatedImageDao().getAllImages())

            if (annotatedImages.isNotEmpty()) {
                _currentImage.value = ImageUtils.loadPhoto(imageName, context, reqWidth, reqHeight)
                _currentImageName.value = imageName
            }
        }
    }

    fun loadNextPhoto(context: Context, reqWidth: Int, reqHeight: Int) {
        if (annotatedImages.isEmpty()) return

        val currIdx = annotatedImages.indexOfFirst { it.imageName == _currentImageName.value }
        val nextIdx = (currIdx + 1) % annotatedImages.size
        Log.d(DEBUG, "currIdx = $currIdx nextIdx = $nextIdx")

        if (currIdx != nextIdx) {
            val nextImage = annotatedImages[nextIdx]
            _currentImage.value = ImageUtils.loadPhoto(nextImage.imageName, context, reqWidth, reqHeight)
            _currentImageName.value = nextImage.imageName
            Log.d(DEBUG, "current image = $currentImageName")
        }
    }

    fun loadPreviousPhoto(context: Context, reqWidth: Int, reqHeight: Int) {
        if (annotatedImages.isEmpty()) return

        val currIdx = annotatedImages.indexOfFirst { it.imageName == _currentImageName.value }
        val prevIdx = if (currIdx - 1 < 0) annotatedImages.lastIndex else currIdx - 1
        Log.d(DEBUG, "currIdx = $currIdx prevIdx = $prevIdx")

        if (currIdx != prevIdx) {
            val prevImage = annotatedImages[prevIdx]
            _currentImage.value = ImageUtils.loadPhoto(prevImage.imageName, context, reqWidth, reqHeight)
            _currentImageName.value = prevImage.imageName
            Log.d(DEBUG, "current image = $currentImageName")
        }
    }

    fun deleteCurrentTemplate(context: Context, reqWidth: Int, reqHeight: Int) {
        if (_currentImageName.value.isNullOrEmpty()) return

        viewModelScope.launch {
            database.annotatedImageDao().deleteImage(_currentImageName.value!!)
            ImageUtils.deletePhoto(_currentImageName.value!!, context)
            val currIdx = annotatedImages.indexOfFirst { it.imageName == _currentImageName.value }
            annotatedImages.removeAt(currIdx)
            _currentImageName.value = ""
            _currentImage.value = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            loadPreviousPhoto(context, reqWidth, reqHeight)
        }
    }
}