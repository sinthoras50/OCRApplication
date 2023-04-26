package com.example.templateeditorapp.ui.overview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.templateeditorapp.db.AnnotatedImage
import com.example.templateeditorapp.db.ImageDatabase
import com.example.templateeditorapp.utils.ImageUtils
import com.example.templateeditorapp.utils.TAG_IMAGE
import kotlinx.coroutines.launch


/**
 * ViewModel class for the OverviewFragment.
 * @param database: The ImageDatabase object that provides access to the annotated image data.
 */
class OverviewViewModel(val database: ImageDatabase) : ViewModel() {

    private val annotatedImages = mutableListOf<AnnotatedImage>()

    private val _imageSet = MutableLiveData<List<Bitmap>>()
    private val _currentImageName = MutableLiveData<String>("")

    val imageSet: LiveData<List<Bitmap>> = _imageSet
    val currentImageName: LiveData<String> = _currentImageName

    var currentIdx = 0
    var currentImageBoundingBox: RectF? = null

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

            currentIdx = 0
            currentImageBoundingBox = annotatedImages[currentIdx].cropRect
            _currentImageName.value = annotatedImages[currentIdx].imageName
            _imageSet.value = annotatedImages.mapNotNull { ImageUtils.loadPhoto(it.imageName, context, reqWidth, reqHeight) }
        }
    }


    /**
     * Loads the annotated images from the database and updates the current set of images, starting at the specified image.
     * @param imageName: The name of the image to start at.
     * @param context: The context used to load the images.
     * @param reqWidth: The required width for the loaded images.
     * @param reqHeight: The required height for the loaded images.
     */
    fun loadImages(imageName: String, context: Context, reqWidth: Int, reqHeight: Int) {
        if (annotatedImages.isNotEmpty()) return

        viewModelScope.launch {
            annotatedImages.addAll(database.annotatedImageDao().getAllImages())

            if (annotatedImages.isEmpty()) return@launch

            currentIdx = annotatedImages.indexOfFirst { it.imageName == imageName }
            currentImageBoundingBox = annotatedImages[currentIdx].cropRect
            _currentImageName.value = annotatedImages[currentIdx].imageName
            _imageSet.value = annotatedImages.mapNotNull { ImageUtils.loadPhoto(it.imageName, context, reqWidth, reqHeight) }
        }
    }

    /**
     * Loads the next image in the current set of images.
     * @return true if there is a next image, false otherwise.
     */
    fun loadNextPhoto(): Boolean {
        if (currentIdx+1 >= annotatedImages.size) return false

        currentIdx++
        currentImageBoundingBox = annotatedImages[currentIdx].cropRect
        _currentImageName.value = annotatedImages[currentIdx].imageName

        return true
    }

    /**
     * Loads the previous image in the current set of images.
     * @return true if there is a previous image, false otherwise.
     */
    fun loadPreviousPhoto(): Boolean {
        if (currentIdx-1 < 0) return false

        currentIdx--
        currentImageBoundingBox = annotatedImages[currentIdx].cropRect
        _currentImageName.value = annotatedImages[currentIdx].imageName

        return true
    }

    /**
     * Deletes the currently selected image template.
     *
     * @param context The context to use for deleting the image file.
     * @return `true` if the image was deleted successfully, `false` otherwise.
     */
    fun deleteCurrentTemplate(context: Context): Boolean {
        if (_currentImageName.value.isNullOrEmpty()) return false

        ImageUtils.deletePhoto(_currentImageName.value!!, context)

        viewModelScope.launch {
            database.annotatedImageDao().deleteImage(_currentImageName.value!!)
        }

        val idx = annotatedImages.indexOfFirst { it.imageName == _currentImageName.value }

        Log.d(TAG_IMAGE, "size pre remove = ${annotatedImages.size}")
        annotatedImages.removeAt(idx)
        val updatedList = _imageSet.value!!.let {
            val lst = it.toMutableList()
            lst.removeAt(idx)
            lst.toList()
        }

        currentIdx = (currentIdx).coerceAtMost((annotatedImages.size-1).coerceAtLeast(0))
        currentImageBoundingBox = if (annotatedImages.size > 0) annotatedImages[currentIdx].cropRect else null

        _currentImageName.value = if (annotatedImages.size > 0) annotatedImages[currentIdx].imageName else ""
        _imageSet.value = updatedList

        return true
    }
}