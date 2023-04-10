package com.example.templateeditorapp.db

import android.graphics.RectF
import androidx.room.TypeConverter
import com.example.templateeditorapp.ui.editor.BoundingBox
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DataConverter {

    @TypeConverter
    fun fromBoundingBoxList(value: List<BoundingBox>): String {
        val gson = Gson()
        val type = object : TypeToken<List<BoundingBox>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toBoundingBoxList(value: String): List<BoundingBox> {
        val gson = Gson()
        val type = object : TypeToken<List<BoundingBox>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromRectF(value: RectF?): String? {
        if (value == null)
            return null

        val gson = Gson()
        val type = object : TypeToken<RectF>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toRectF(value: String?): RectF? {
        if (value == null)
            return null

        val gson = Gson()
        val type = object : TypeToken<RectF>() {}.type
        return gson.fromJson(value, type)
    }
}