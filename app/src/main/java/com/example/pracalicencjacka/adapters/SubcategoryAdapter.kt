package com.example.pracalicencjacka.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ToggleButton
import androidx.recyclerview.widget.RecyclerView
import com.example.arxivWebFeed.R
import com.example.pracalicencjacka.datamodel.Category

class SubcategoryAdapter(context: Context, private var category: Category?) :
    RecyclerView.Adapter<SubcategoryAdapter.ViewHolder>() {

    private lateinit var mListener: OnItemClickListener
    private var contexts = context

    interface OnItemClickListener {
        fun cellClick(position: Int) {}
        fun starClick(position: Int, checked: Boolean) {}
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_subcategory, parent, false)

        return ViewHolder(contexts, view, mListener)
    }

    fun updateAdapter(category: Category?) {
        this.category = category
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return category?.subcategories?.size ?: 1
    }

    // Holds the views for adding it to image and text
    class ViewHolder(context: Context, ItemView: View, listener: OnItemClickListener) :
        RecyclerView.ViewHolder(ItemView) {
        val field: TextView = itemView.findViewById(R.id.rowSubFieldText)
        val starbutton: ToggleButton = itemView.findViewById(R.id.subscriptionButton)

        init {
            ItemView.setOnClickListener {
                listener.cellClick(bindingAdapterPosition)
            }
            starbutton.setOnClickListener {
                manageClick(starbutton, context, listener, bindingAdapterPosition)
            }
        }

        private fun manageClick(
            button: ToggleButton,
            context: Context,
            listener: OnItemClickListener,
            position: Int
        ) {
            if (!button.isChecked) {
                button.background = context.resources.getDrawable(R.drawable.star)
            } else {
                button.background = context.resources.getDrawable(R.drawable.starclick)
            }
            listener.starClick(position, button.isChecked)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cell = category?.subcategories?.get(position)
        holder.field.text = cell?.title ?: "Error"

        if (cell?.subscribed == true) {
            holder.starbutton.isChecked = true
            holder.starbutton.background = contexts.resources.getDrawable(R.drawable.starclick)
        } else {
            holder.starbutton.isChecked = false
            holder.starbutton.background = contexts.resources.getDrawable(R.drawable.star)
        }
    }
}
