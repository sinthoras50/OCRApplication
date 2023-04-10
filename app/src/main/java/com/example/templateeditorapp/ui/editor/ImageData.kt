package com.example.templateeditorapp.ui.editor

import android.graphics.Bitmap
import android.graphics.Matrix

data class ImageData(val bitmap: Bitmap, val imageMatrix: Matrix, val isUpdate: Boolean) {
}