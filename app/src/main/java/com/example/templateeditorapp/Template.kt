package com.example.templateeditorapp

import android.graphics.Bitmap
import com.example.templateeditorapp.db.AnnotatedImage

data class Template(val annotatedImage: AnnotatedImage, val bitmap: Bitmap)
