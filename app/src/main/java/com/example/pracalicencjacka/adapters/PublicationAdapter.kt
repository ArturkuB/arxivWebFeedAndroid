package com.example.pracalicencjacka.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pracalicencjacka.datamodel.Publication
import com.example.arxivWebFeed.R
import io.realm.RealmList

class PublicationAdapter(private var publications: RealmList<Publication>?) :
    RecyclerView.Adapter<PublicationAdapter.ViewHolder>() {

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
            .inflate(R.layout.row_publication, parent, false)

        return ViewHolder(view, mListener)
    }

    fun updateAdapter(publications: RealmList<Publication>?) {
        this.publications = publications
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        if (publications?.size == 0) {
            return 1
        }
        return publications?.size ?: 1
    }

    class ViewHolder(ItemView: View, listener: OnItemClickListener) :
        RecyclerView.ViewHolder(ItemView) {
        val authors: TextView = itemView.findViewById(R.id.authorsPubs)
        val title: TextView = itemView.findViewById(R.id.titlePubs)


        init {
            ItemView.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition)
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cell = publications?.getOrNull(position)
        holder.title.text = cell?.title ?: "No publications"
        holder.authors.text = cell?.author ?: "Check selectors or internet connection"
        holder.itemView.isClickable = cell != null
    }
}
