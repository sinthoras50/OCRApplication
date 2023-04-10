package com.example.templateeditorapp.db

import android.graphics.RectF
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.templateeditorapp.ui.editor.BoundingBox

@Entity(tableName = "annotated_images")
data class AnnotatedImage(
    @PrimaryKey val imageName: String,
    val boundingBoxes: List<BoundingBox>,
    @ColumnInfo(defaultValue = "NULL") val cropRect: RectF?)