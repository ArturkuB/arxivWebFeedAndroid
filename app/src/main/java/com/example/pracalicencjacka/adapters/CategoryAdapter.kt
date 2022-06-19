package com.example.pracalicencjacka.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.arxivWebFeed.R
import com.example.pracalicencjacka.datamodel.Category
import io.realm.RealmResults

class CategoryAdapter(
    private var categories: RealmResults<Category>?,
    private val images: ArrayList<Int>
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private lateinit var mListener: OnItemClickListener

    interface OnItemClickListener {
        fun onItemClick(position: Int) {}
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_category, parent, false)

        return ViewHolder(view, mListener)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cell = categories?.getOrNull(position)
        val imageStrResource = images[position]
        holder.image.setImageResource(imageStrResource)
        holder.field.text = cell?.title ?: "No categories, reopen app with connection"
        holder.itemView.isClickable = cell != null
    }

    fun updateAdapter(categories: RealmResults<Category>?) {
        this.categories = categories
        notifyDataSetChanged()
    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        if (categories?.size == 0) {
            return 1
        }
        return categories?.size ?: 1
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View, listener: OnItemClickListener) :
        RecyclerView.ViewHolder(ItemView) {
        val field: TextView = itemView.findViewById(R.id.rowText)
        val image: ImageView = itemView.findViewById(R.id.rowImage)

        init {
            ItemView.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition)
            }
        }
    }
}
