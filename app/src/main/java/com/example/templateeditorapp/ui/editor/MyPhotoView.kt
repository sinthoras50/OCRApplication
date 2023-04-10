package com.example.templateeditorapp.ui.editor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import com.github.chrisbanes.photoview.PhotoView


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