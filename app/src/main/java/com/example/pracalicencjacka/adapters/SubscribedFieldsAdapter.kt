package com.example.pracalicencjacka.adapters

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ToggleButton
import androidx.recyclerview.widget.RecyclerView
import com.example.arxivWebFeed.R
import com.example.pracalicencjacka.datamodel.Subcategory
import io.realm.RealmList

class SubscribedFieldsAdapter(
    private var context: Context,
    private var subcategories: RealmList<Subcategory>?
) : RecyclerView.Adapter<SubscribedFieldsAdapter.ViewHolder>() {

    private lateinit var mListener: OnItemClickListener

    interface OnItemClickListener {
        fun onItemClick(subcategory: Subcategory) {}
        fun unsubscribeItem(subcategory: Subcategory) {}
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_subcategory, parent, false)

        return ViewHolder(context, view, mListener)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    fun updateAdapter(subcategories: RealmList<Subcategory>?) {
        this.subcategories = subcategories
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        if (subcategories?.size == 0) {
            return 1
        }
        return subcategories?.size ?: 1
    }

    inner class ViewHolder(context: Context, ItemView: View, listener: OnItemClickListener) :
        RecyclerView.ViewHolder(ItemView) {
        val field: TextView = itemView.findViewById(R.id.rowSubFieldText)
        val button: ToggleButton = itemView.findViewById(R.id.subscriptionButton)

        init {
            ItemView.setOnClickListener {
                listener.onItemClick(subcategories?.get(bindingAdapterPosition)!!)
            }
            button.setOnClickListener {

                val builder: AlertDialog.Builder = AlertDialog.Builder(context)
                builder.setMessage("Are you sure you want to unsubscribe?")
                    .setCancelable(false)
                    .setPositiveButton(
                        "Yes"
                    ) { _, _ ->
                        listener.unsubscribeItem(subcategories!![bindingAdapterPosition])
                        subcategories?.removeAt(bindingAdapterPosition)
                        notifyItemRemoved(bindingAdapterPosition)
                        notifyItemRangeChanged(bindingAdapterPosition, subcategories?.size ?: 0)
                    }
                    .setNegativeButton(
                        "No"
                    ) { dialog, _ -> dialog.dismiss() }
                val alert: AlertDialog = builder.create()
                alert.setTitle("Warning")
                alert.show()
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cell = subcategories?.getOrNull(position)
        holder.field.text = cell?.title ?: "No subscribed subcategories"
        if (cell == null) {
            holder.itemView.isClickable = false
            holder.button.background = context.resources.getDrawable(R.drawable.starclick)
            holder.button.isClickable = false
        } else {
            holder.itemView.isClickable = true
            holder.button.background = context.resources.getDrawable(R.drawable.starclick)
            holder.button.isClickable = true
        }
    }
}
