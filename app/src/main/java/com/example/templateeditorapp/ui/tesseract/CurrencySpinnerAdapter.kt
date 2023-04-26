package com.example.templateeditorapp.ui.tesseract

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.templateeditorapp.R
import com.example.templateeditorapp.ui.qrgen.Currency

/**
 * A custom [BaseAdapter] class used for populating a spinner with values from a list of [Currency] items.
 */
class CurrencySpinnerAdapter(val context: Context, val objects: List<Currency>) : BaseAdapter() {

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.currency_spinner_item, parent, false)

        val image = view.findViewById<ImageView>(R.id.currencyImageView)
        val textView = view.findViewById<TextView>(R.id.currencyTextView)

        textView.text = objects[position].value
        image.setImageResource(objects[position].image)

        return view
    }

    /**
     * Returns the number of items in the spinner.
     * @return Returns the size of the [objects] list
     */
    override fun getCount(): Int {
        return objects.size
    }

    /**
     * Returns the specified item.
     * @param p0 Position
     * @return Returns the [Currency] item at position [p0]
     */
    override fun getItem(p0: Int): Any {
        return objects[p0].value
    }

    /**
     * Returns the id of the item
     * @param p0 Position
     * @return Returns the id of the item at position [p0]
     */
    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }
}