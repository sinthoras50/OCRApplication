package com.example.templateeditorapp.ui.overview

import android.graphics.Bitmap
import androidx.recyclerview.widget.DiffUtil

class ImageDiffCallback(private val oldList: List<Bitmap>, private val newList: List<Bitmap>) : DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] === newList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].sameAs(newList[newItemPosition])
    }
}