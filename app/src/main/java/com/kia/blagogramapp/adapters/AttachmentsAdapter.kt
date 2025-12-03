package com.kia.blagogramapp.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.kia.blagogramapp.R

class AttachmentsAdapter(
    private var items: MutableList<Uri>,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<AttachmentsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.imagePreview)
        val remove: ImageView = view.findViewById(R.id.removeBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attachment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uri = items[position]
        holder.image.load(uri) {
            crossfade(true)
            placeholder(R.drawable.ic_photo)
        }

        holder.remove.setOnClickListener {
            onRemove(position)
        }
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<Uri>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
