package com.kia.blagogramapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.kia.blagogramapp.R
import com.kia.blagogramapp.models.ChatItem
import com.kia.blagogramapp.utils.TimeUtils

class ChatsAdapter(
    val items: List<ChatItem>,
    val onClick: (ChatItem) -> Unit
) : RecyclerView.Adapter<ChatsAdapter.ViewHolder>() {

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.chatName)
        val last: TextView = v.findViewById(R.id.chatLastMessage)
        val avatar: ImageView = v.findViewById(R.id.chatAvatar)

        val  chatTime: TextView = v.findViewById(R.id.chatTime)

        init {
            v.setOnClickListener {
                onClick(items[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val c = items[position]
        holder.name.text = c.name
        holder.last.text = c.last_message ?: ""
        holder.chatTime.text = TimeUtils.formatTime(c.last_time ?: "")


        holder.avatar.load("https://blagogram.ru/avatars/${c.avatar}") {
            placeholder(R.mipmap.ic_launcher)
            crossfade(true)
        }
    }
}
