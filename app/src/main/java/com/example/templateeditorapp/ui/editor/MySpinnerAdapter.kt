package com.example.templateeditorapp.ui.editor

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class MySpinnerAdapter(context: Context, resource: Int, objects: List<String>) : ArrayAdapter<String>(context, resource, objects) {

    private val disabledPositions = mutableSetOf<Int>()

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)

        if (isDisabled(position)) {
            (view as TextView).setTextColor(Color.GRAY)
        } else {
            (view as TextView).setTextColor(Color.BLACK)
        }

        return view
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view =  super.getView(position, convertView, parent)
        if (isDisabled(position)) {
            (view as TextView).setTextColor(Color.GRAY)
        } else {
            (view as TextView).setTextColor(Color.BLACK)
        }

        return view
    }

    fun isDisabled(position: Int): Boolean {
        return disabledPositions.contains(position)
    }

    fun disableItem(position: Int) {
        disabledPositions.add(position)
        notifyDataSetChanged()
    }

    fun enableItem(position: Int) {
        disabledPositions.remove(position)
        notifyDataSetChanged()
    }
}