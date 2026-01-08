package com.kia.blagogramapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.kia.blagogramapp.R
import com.kia.blagogramapp.LayoutHelper

class MessageImagesAdapter(
    val imageUrls: List<String>,
    private val isGrid: Boolean = false,
    private val onImageClick: (String) -> Unit
) : RecyclerView.Adapter<MessageImagesAdapter.Holder>() {

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.msgImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_message_image, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val url = imageUrls[position]
        
        val lp = holder.itemView.layoutParams ?: RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        if (isGrid) {
            // Если коллаж, делаем картинки квадратными или фиксированной высоты
            lp.height = LayoutHelper.dp(if (imageUrls.size == 2) 150 else 100)
            holder.img.scaleType = ImageView.ScaleType.CENTER_CROP
        } else {
            // Одиночное фото: даем фиксированную высоту, чтобы не прыгало при загрузке/обновлении
            lp.height = LayoutHelper.dp(250)
            holder.img.scaleType = ImageView.ScaleType.CENTER_CROP
        }
        holder.itemView.layoutParams = lp

        holder.img.load(url) {
            crossfade(true)
            placeholder(R.drawable.ic_photo)
        }
        
        holder.img.setOnClickListener {
            onImageClick(url)
        }
    }

    override fun getItemCount(): Int = imageUrls.size
}
