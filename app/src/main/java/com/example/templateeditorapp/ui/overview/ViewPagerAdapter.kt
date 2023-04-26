package com.example.templateeditorapp.ui.overview

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.templateeditorapp.R


/**
 * RecyclerView adapter for displaying a list of images in a ViewPager.
 *
 * @param images The list of images to display in the ViewPager.
 */
class ViewPagerAdapter(val images: List<Bitmap>) : RecyclerView.Adapter<ViewPagerAdapter.ViewPagerViewHolder>() {

    /**
     * ViewHolder class for the ViewPager.
     *
     * @param itemView The view for the ViewHolder.
     */
    inner class ViewPagerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    /**
     * Creates a new ViewHolder for the ViewPager.
     *
     * @param parent The parent ViewGroup.
     * @param viewType The view type.
     * @return A new ViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewPagerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_view_pager, parent, false)
        return ViewPagerViewHolder(view)
    }

    /**
     * Binds the data to the ViewHolder at the given position.
     *
     * @param holder The ViewHolder to bind the data to.
     * @param position The position of the data to bind.
     */
    override fun onBindViewHolder(holder: ViewPagerViewHolder, position: Int) {
        val image = images[position]
        holder.itemView.findViewById<ImageView>(R.id.ivImageView).setImageBitmap(image)
    }

    /**
     * Returns the total number of items in the data set.
     *
     * @return The number of items in the data set.
     */
    override fun getItemCount(): Int {
        return images.size
    }
}