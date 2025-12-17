package com.kia.blagogramapp.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kia.blagogramapp.ChatCell
import com.kia.blagogramapp.models.ChatItem
import com.kia.blagogramapp.utils.TimeUtils

class ChatsAdapter(
    val items: List<ChatItem>,
    val onClick: (ChatItem) -> Unit
) : RecyclerView.Adapter<ChatsAdapter.ViewHolder>() {

    inner class ViewHolder(val cell: ChatCell) : RecyclerView.ViewHolder(cell) {
        init {
            cell.setOnClickListener {
                onClick(items[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cell = ChatCell(parent.context)
        // Чтобы ячейка растягивалась на всю ширину
        cell.layoutParams = RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT, 
            RecyclerView.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(cell)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val c = items[position]
        val time = TimeUtils.formatTime(c.last_time ?: "")
        
        holder.cell.setData(
            name = c.name,
            message = c.last_message ?: "",
            time = time,
            unread = c.unread,
            avatarUrl = "https://blagogram.ru/avatars/${c.avatar}"
        )
    }
}
