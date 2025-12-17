package com.kia.blagogramapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kia.blagogramapp.R
import com.vanniktech.emoji.EmojiTextView

class EmojiAdapter(
    private val emojis: List<String>,
    private val onEmojiClick: (String) -> Unit
) : RecyclerView.Adapter<EmojiAdapter.EmojiViewHolder>() {

    class EmojiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val emojiText: EmojiTextView = itemView.findViewById(R.id.emojiText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_emoji, parent, false)
        return EmojiViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
        val emoji = emojis[position]
        holder.emojiText.text = emoji
        holder.emojiText.setOnClickListener {
            onEmojiClick(emoji)
        }
    }

    override fun getItemCount(): Int = emojis.size
}
