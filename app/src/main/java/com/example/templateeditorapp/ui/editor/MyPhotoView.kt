package com.example.templateeditorapp.ui.editor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import com.github.chrisbanes.photoview.PhotoView

/**
 * A custom [PhotoView] that allows for drawing a red rectangle on top of the image.
 *
 * @property selection the [RectF] representing the currently selected region of the image
 */
class MyPhotoView(context: Context, attrs: AttributeSet): PhotoView(context, attrs) {

    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    var selection: RectF = RectF()

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas!!.drawRect(selection, paint)
    }
}