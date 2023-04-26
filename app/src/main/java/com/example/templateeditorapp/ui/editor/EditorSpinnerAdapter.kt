package com.example.templateeditorapp.ui.editor

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView


/**
 * An ArrayAdapter implementation to provide custom dropdown and view display for editor spinner.
 * It allows disabling and enabling certain spinner items.
 *
 * @param context The context of the adapter.
 * @param resource The resource ID of the layout file containing the layout to use when instantiating views.
 * @param objects The objects to represent in the adapter.
 */
class EditorSpinnerAdapter(context: Context, resource: Int, objects: List<String>) : ArrayAdapter<String>(context, resource, objects) {

    private val disabledPositions = mutableSetOf<Int>()


    /**
     * Override the method to provide custom drop down view display for editor spinner.
     *
     * @param position The position of the item within the adapter's data set of the item whose view we want.
     * @param convertView The old view to reuse, if possible.
     * @param parent The parent that this view will eventually be attached to.
     * @return A View corresponding to the data at the specified position.
     */
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)

        if (isDisabled(position)) {
            (view as TextView).setTextColor(Color.GRAY)
        } else {
            (view as TextView).setTextColor(Color.BLACK)
        }

        return view
    }

    /**
     * Override the method to provide custom view display for editor spinner.
     *
     * @param position The position of the item within the adapter's data set of the item whose view we want.
     * @param convertView The old view to reuse, if possible.
     * @param parent The parent that this view will eventually be attached to.
     * @return A View corresponding to the data at the specified position.
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view =  super.getView(position, convertView, parent)
        if (isDisabled(position)) {
            (view as TextView).setTextColor(Color.GRAY)
        } else {
            (view as TextView).setTextColor(Color.BLACK)
        }

        return view
    }

    /**
     * Check if the item at the given position is disabled or not.
     *
     * @param position The position of the item to check.
     * @return true if the item is disabled, false otherwise.
     */
    fun isDisabled(position: Int): Boolean {
        return disabledPositions.contains(position)
    }

    /**
     * Disable the item at the given position.
     *
     * @param position The position of the item to disable.
     */
    fun disableItem(position: Int) {
        disabledPositions.add(position)
        notifyDataSetChanged()
    }

    /**
     * Enable the item at the given position.
     *
     * @param position The position of the item to enable.
     */
    fun enableItem(position: Int) {
        disabledPositions.remove(position)
        notifyDataSetChanged()
    }
}