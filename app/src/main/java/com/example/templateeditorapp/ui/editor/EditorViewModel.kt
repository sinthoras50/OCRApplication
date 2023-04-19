package com.example.templateeditorapp.ui.editor

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.widget.Spinner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.templateeditorapp.db.AnnotatedImage
import com.example.templateeditorapp.db.ImageDatabase
import com.example.templateeditorapp.utils.ImageUtils
import kotlinx.coroutines.launch
import kotlin.math.abs

class EditorViewModel(val database: ImageDatabase) : ViewModel() {

    private lateinit var canvas: Canvas
    private lateinit var currentBitmap: Bitmap

    private val boundingBoxes = mutableListOf<BoundingBox>()
    private val fillPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val _imageData = MutableLiveData<ImageData>()

    private var cropRect: RectF? = null

    var scaleMult = 1f
    val imageData: LiveData<ImageData> = _imageData

    fun handleImageResult(data: Intent?, contentResolver: ContentResolver) {
        val imageUri = data?.data
        val imageStream = imageUri?.let { contentResolver.openInputStream(it) }
        val selectedBitmap = BitmapFactory.decodeStream(imageStream)
        val mutableBitmap = selectedBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val identity = Matrix()
        boundingBoxes.clear()
        currentBitmap = selectedBitmap
        _imageData.value = ImageData(mutableBitmap, identity, false)
        canvas = Canvas(mutableBitmap)
    }

    fun handleImageResult(path: String, context: Context) {
        val bitmap = ImageUtils.loadPhoto(path, context)
        val mutableBitmap = bitmap!!.copy(Bitmap.Config.ARGB_8888, true)
        val identity = Matrix()
        boundingBoxes.clear()
        currentBitmap = bitmap
        _imageData.value = ImageData(mutableBitmap, identity, false)
        canvas = Canvas(mutableBitmap)
    }

    fun updateSpinnerItems(spinner: Spinner) {
        val adapter = spinner.adapter as EditorSpinnerAdapter
        val selected = boundingBoxes.map { it.fieldName }
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i) in selected) {
                adapter.disableItem(i)
            } else {
                adapter.enableItem(i)
            }
        }
    }

    private fun drawCropRect() {
        if (cropRect != null) {
            canvas.drawRect(cropRect!!, strokePaint)
        }
    }

    private fun drawBoundingBoxes() {
        boundingBoxes.forEach {
            canvas.drawRect(it.rect, fillPaint)
            canvas.drawRect(it.rect, strokePaint)

            drawText(canvas, it.fieldName, it.rect)
        }
    }

    private fun refreshCanvas(imageMatrix: Matrix) {
        val mutableBitmap = currentBitmap.copy(Bitmap.Config.ARGB_8888, true)
        _imageData.value = ImageData(mutableBitmap, imageMatrix, true)

        canvas = Canvas(mutableBitmap)

        drawBoundingBoxes()
        drawCropRect()

    }

    fun rotateRight90Deg(spinner: Spinner) {
        boundingBoxes.clear()
        cropRect = null
        updateSpinnerItems(spinner)

        val rotation = Matrix()
        rotation.postRotate(90f)
        val rotatedBitmap = Bitmap.createBitmap(currentBitmap, 0, 0, currentBitmap.width, currentBitmap.height, rotation, true)
        currentBitmap = rotatedBitmap
        val mutableBitmap = rotatedBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val identity = Matrix()
        _imageData.value = ImageData(mutableBitmap, identity, false)

        canvas = Canvas(mutableBitmap)
    }

    fun removeLastBoundingBox(spinner: Spinner, imageMatrix: Matrix) {
        if (boundingBoxes.size == 0) return

        val adapter = spinner.adapter as EditorSpinnerAdapter
        val position = adapter.getPosition(boundingBoxes.last().fieldName)
        adapter.enableItem(position)
        spinner.setSelection(position)

        boundingBoxes.removeLast()

        refreshCanvas(imageMatrix)
//
//        val mutableBitmap = currentBitmap.copy(Bitmap.Config.ARGB_8888, true)
//        _imageData.value = ImageData(mutableBitmap, imageMatrix, true)
//
//        canvas = Canvas(mutableBitmap)
//
//        drawBoundingBoxes()
    }

    fun removeBoundingBoxAt(position: Int, spinner: Spinner, imageMatrix: Matrix) {
        if (boundingBoxes.size == 0) return

        val boundingBoxName = spinner.selectedItem as String
        val containsBoundingBox = boundingBoxes.any { it.fieldName == boundingBoxName }

        if (!containsBoundingBox) return

        val adapter = spinner.adapter as EditorSpinnerAdapter
        adapter.enableItem(position)

        boundingBoxes.removeIf { it.fieldName == boundingBoxName }

        refreshCanvas(imageMatrix)

//        val mutableBitmap = currentBitmap.copy(Bitmap.Config.ARGB_8888, true)
//        _imageData.value = ImageData(mutableBitmap, imageMatrix, true)
//
//        canvas = Canvas(mutableBitmap)
//
//        drawBoundingBoxes()
    }

    fun onTouchListenerEdit(view: View, event: MotionEvent, spinner: Spinner): Boolean {
        val spinnerSelectedPosition = spinner.selectedItemPosition
        val adapter = spinner.adapter as EditorSpinnerAdapter
        val boundingBoxName = spinner.selectedItem
        val photoView = view as MyPhotoView

        if (adapter.isDisabled(spinnerSelectedPosition)) {

            if (event.actionMasked == MotionEvent.ACTION_UP) {
                val matrix = photoView.imageMatrix
                val matrixValues = FloatArray(9)
                matrix.getValues(matrixValues)

                val scale = photoView.scale
                val translateX = matrixValues[Matrix.MTRANS_X]
                val translateY = matrixValues[Matrix.MTRANS_Y]
                val currentRect = boundingBoxes.find { it.fieldName == boundingBoxName}?.rect


                if (currentRect != null && currentRect.contains(
                        (event.x - translateX) / scale * scaleMult,
                        (event.y - translateY) / scale * scaleMult))
                {

                    val suppMatrix = Matrix()
                    photoView.getSuppMatrix(suppMatrix)
                    removeBoundingBoxAt(spinnerSelectedPosition, spinner, suppMatrix)
                }
            }
            return true
        }



        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                photoView.selection = RectF(event.x, event.y, event.x, event.y)
                photoView.invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                photoView.selection.apply {
                    right = event.x
                    bottom = event.y
                }
                photoView.invalidate()
            }
            MotionEvent.ACTION_UP -> {
                val scale = view.scale
                val matrix = view.imageMatrix
                val matrixValues = FloatArray(9)
                matrix.getValues(matrixValues)
                val translateX = matrixValues[Matrix.MTRANS_X]
                val translateY = matrixValues[Matrix.MTRANS_Y]

                val boundingBox = BoundingBox(
                    (photoView.selection.left - translateX) / scale * scaleMult,
                    (photoView.selection.top - translateY) / scale * scaleMult,
                    (event.x - translateX) / scale * scaleMult,
                    (event.y - translateY) / scale * scaleMult,
                    boundingBoxName as String
                )

                val rect = boundingBox.rect

                if (rect.width() < 10f || rect.height() < 10f) {
                    photoView.selection = RectF()
                    photoView.invalidate()
                    return true
                }

                canvas.drawRect(boundingBox.rect, fillPaint)
                canvas.drawRect(boundingBox.rect, strokePaint)
                boundingBoxes.add(boundingBox)

                drawText(canvas, boundingBoxName, boundingBox.rect)

                adapter.disableItem(spinnerSelectedPosition)
                spinner.setSelection((spinner.selectedItemPosition + 1) % spinner.adapter.count)

                photoView.selection = RectF()
                photoView.invalidate()
            }
        }

        return true
    }

    fun onTouchListenerSelect(view: View, event: MotionEvent): Boolean {
        val photoView = view as MyPhotoView

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                photoView.selection = RectF(event.x, event.y, event.x, event.y)
                photoView.invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                photoView.selection.apply {
                    right = event.x
                    bottom = event.y
                }
                photoView.invalidate()
            }
            MotionEvent.ACTION_UP -> {
                cropRect = null
                val scale = view.scale
                val matrix = view.imageMatrix
                val matrixValues = FloatArray(9)
                matrix.getValues(matrixValues)
                val translateX = matrixValues[Matrix.MTRANS_X]
                val translateY = matrixValues[Matrix.MTRANS_Y]

                val rect = RectF(
                    (photoView.selection.left - translateX) / scale * scaleMult,
                    (photoView.selection.top - translateY) / scale * scaleMult,
                    (event.x - translateX) / scale * scaleMult,
                    (event.y - translateY) / scale * scaleMult
                )

                if (rect.width() < 10f || rect.height() < 10f) {
                    photoView.selection = RectF()
                    photoView.invalidate()
                    return true
                }

                val suppMatrix = Matrix()
                photoView.getSuppMatrix(suppMatrix)
                refreshCanvas(suppMatrix)

                canvas.drawRect(rect, strokePaint)
                cropRect = rect

                photoView.selection = RectF()
                photoView.invalidate()
            }
        }

        return true
    }

    private fun drawText(canvas: Canvas, text: String, rect: RectF) {
        val vMargin = 10f
        val textPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = (abs(rect.height()) - 2 * vMargin).coerceAtLeast(15f).coerceAtMost(100f)
        }

        val maxWidth = rect.width() - 5f
        val trimmedText = TextUtils.ellipsize(text, textPaint, maxWidth, TextUtils.TruncateAt.END)
        val layout = StaticLayout.Builder.obtain(
            trimmedText,
            0,
            trimmedText.length,
            textPaint,
            maxWidth.toInt()
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .build()

        val textHeight = layout.height.toFloat()

        val centerY = rect.centerY()
        val y = centerY - textHeight / 2

        canvas.save()
        canvas.translate(rect.left + 5f, y)
        layout.draw(canvas)
        canvas.restore()
    }

    fun clearSelectionRect(view: View) {
        cropRect = null
        val imageView = view as MyPhotoView
        val suppMatrix = Matrix()
        imageView.getSuppMatrix(suppMatrix)
        refreshCanvas(suppMatrix)
    }

    fun saveTemplate(filename: String, context: Context) {
        if (::currentBitmap.isInitialized.not()) return

        viewModelScope.launch {
            try {
                database.annotatedImageDao().insertImage(AnnotatedImage(filename, boundingBoxes, cropRect))
                ImageUtils.savePhoto(filename, currentBitmap, context)
            } catch (e: SQLiteException) {
                e.printStackTrace()
            }

        }
    }

    fun loadTemplate(filename: String, context: Context, spinner: Spinner) {
        viewModelScope.launch {
            val annotatedImage = database.annotatedImageDao().getImage(filename)
            val bitmap = ImageUtils.loadPhoto(filename, context)

            if (annotatedImage == null || bitmap == null) return@launch

            currentBitmap = bitmap
            boundingBoxes.apply {
                clear()
                addAll(annotatedImage.boundingBoxes)
            }
            cropRect = annotatedImage.cropRect

            val mutableBitmap = currentBitmap.copy(Bitmap.Config.ARGB_8888, true)
            _imageData.value = ImageData(mutableBitmap, Matrix(), false)
            canvas = Canvas(mutableBitmap)
            drawBoundingBoxes()
            drawCropRect()

            updateSpinnerItems(spinner)
        }
    }
}