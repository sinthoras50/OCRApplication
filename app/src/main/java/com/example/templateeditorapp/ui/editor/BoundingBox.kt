package com.example.templateeditorapp.ui.editor

import android.graphics.RectF

data class BoundingBox(val x0: Float, val y0: Float, val x1: Float, val y1: Float, val fieldName: String) {
    val rect = RectF(x0, y0, x1, y1)
}
