package com.kia.blagogramapp.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kia.blagogramapp.ChatCell
import com.kia.blagogramapp.ChatSectionHeaderCell
import com.kia.blagogramapp.models.ChatItem
import com.kia.blagogramapp.utils.TimeUtils

class ChatsAdapter(
    val items: List<Any>, // Может быть ChatItem или String (заголовок)
    val onClick: (ChatItem) -> Unit,
    val onLongClick: (ChatItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CHAT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is String) TYPE_HEADER else TYPE_CHAT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val cell = ChatSectionHeaderCell(parent.context)
            cell.layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            HeaderViewHolder(cell)
        } else {
            val cell = ChatCell(parent.context)
            cell.layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            ChatViewHolder(cell)
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            holder.cell.setTitle(items[position] as String)
        } else if (holder is ChatViewHolder) {
            val c = items[position] as ChatItem
            val time = TimeUtils.formatTime(c.last_time ?: "")
            
            holder.cell.setData(
                name = c.name,
                message = c.last_message ?: "",
                time = time,
                unread = c.unread,
                avatar = c.avatar,
                pinned = c.pinned
            )
            
            holder.cell.setOnClickListener { onClick(c) }
            holder.cell.setOnLongClickListener { 
                onLongClick(c)
                true
            }
        }
    }

    inner class ChatViewHolder(val cell: ChatCell) : RecyclerView.ViewHolder(cell)
    inner class HeaderViewHolder(val cell: ChatSectionHeaderCell) : RecyclerView.ViewHolder(cell)
}
