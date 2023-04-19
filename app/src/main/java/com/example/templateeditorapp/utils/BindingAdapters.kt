package com.example.templateeditorapp.utils

import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.databinding.BindingAdapter

class BindingAdapters {

    @BindingAdapter("app:setTextColorIfCondition")
    fun setTextColorIfCondition(view: View, bool: Boolean) {
        if (bool) {
            (view as TextView).setTextColor(Color.RED)
        }
    }
}