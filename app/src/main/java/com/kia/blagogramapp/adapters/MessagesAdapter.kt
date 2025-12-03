package com.kia.blagogramapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kia.blagogramapp.R
import com.kia.blagogramapp.model.Message
import com.kia.blagogramapp.models.ChatListItem
import com.kia.blagogramapp.utils.TimeUtils

class MessagesAdapter(
    itemsInput: List<ChatListItem>,
    private val myId: Int,
    // ИЗМЕНЕНИЕ 1: Теперь колбэк принимает URL и само сообщение
    private val onImageClick: (String, Message) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: MutableList<ChatListItem> = itemsInput.toMutableList()

    private val typeHeader = 0
    private val typeLeft = 1
    private val typeRight = 2

    override fun getItemViewType(position: Int): Int {
        return when (val item = items[position]) {
            is ChatListItem.Header -> typeHeader
            is ChatListItem.Msg ->
                if (item.message.sender_id == myId) typeRight else typeLeft
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when (viewType) {
            typeHeader -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_day_header, parent, false)
                HeaderHolder(v)
            }
            typeRight -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.message_right, parent, false)
                MsgHolder(v)
            }
            else -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.message_left, parent, false)
                MsgHolder(v)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (val item = items[position]) {

            is ChatListItem.Header -> {
                (holder as HeaderHolder).title.text = item.title
            }

            is ChatListItem.Msg -> {
                val msg = item.message
                val h = holder as MsgHolder
                h.text.text = msg.content
                h.time.text = TimeUtils.formatTime(msg.sent_at)

                if (msg.content.isEmpty()) {
                    h.text.visibility = View.GONE
                } else {
                    h.text.visibility = View.VISIBLE
                }

                if (msg.images.isNotEmpty()) {
                    h.imagesRecycler.visibility = View.VISIBLE
                    h.imagesRecycler.layoutManager =
                        LinearLayoutManager(h.itemView.context)

                    val urls = msg.images.map {
                        if (it.startsWith("content:") || it.startsWith("file:")) {
                            it
                        } else {
                            "https://blagogram.ru/uploads/${msg.sender_id}/$it"
                        }
                    }

                    // ИЗМЕНЕНИЕ 2: Оборачиваем вызов, чтобы добавить объект msg
                    // MessageImagesAdapter остается старым (принимает (String)->Unit),
                    // но мы внутри лямбды вызываем наш обновленный onImageClick
                    h.imagesRecycler.adapter = MessageImagesAdapter(urls) { clickedUrl ->
                        onImageClick(clickedUrl, msg)
                    }

                } else {
                    h.imagesRecycler.visibility = View.GONE
                }
            }
        }
    }

    override fun getItemCount() = items.size

    fun update(messages: List<Message>) {
        val newItems = buildList(messages)

        val diffCallback = MessagesDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        items.clear()
        items.addAll(newItems)

        diffResult.dispatchUpdatesTo(this)
    }

    fun addMessage(message: Message) {
        val header = TimeUtils.headerFor(message.sent_at)
        val lastItem = items.lastOrNull()

        var needsHeader = true
        if (lastItem is ChatListItem.Msg) {
            val lastHeader = TimeUtils.headerFor(lastItem.message.sent_at)
            if (lastHeader == header) {
                needsHeader = false
            }
        } else if (lastItem is ChatListItem.Header) {
            if (lastItem.title == header) {
                needsHeader = false
            }
        } else if (items.isEmpty()) {
            needsHeader = true
        }

        if (needsHeader) {
            items.add(ChatListItem.Header(header))
            notifyItemInserted(items.size - 1)
        }

        items.add(ChatListItem.Msg(message))
        notifyItemInserted(items.size - 1)
    }

    private fun buildList(messages: List<Message>): List<ChatListItem> {
        val list = mutableListOf<ChatListItem>()
        var lastHeader = ""

        for (m in messages) {
            val header = TimeUtils.headerFor(m.sent_at)

            if (header != lastHeader) {
                list.add(ChatListItem.Header(header))
                lastHeader = header
            }
            list.add(ChatListItem.Msg(m))
        }
        return list
    }

    class MsgHolder(v: View) : RecyclerView.ViewHolder(v) {
        val text: TextView = v.findViewById(R.id.msgText)
        val time: TextView = v.findViewById(R.id.msgTime)
        val imagesRecycler: RecyclerView = v.findViewById(R.id.msgImages)
    }

    class HeaderHolder(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.dayTitle)
    }

    class MessagesDiffCallback(
        private val oldList: List<ChatListItem>,
        private val newList: List<ChatListItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]

            if (oldItem is ChatListItem.Header && newItem is ChatListItem.Header) {
                return oldItem.title == newItem.title
            }

            if (oldItem is ChatListItem.Msg && newItem is ChatListItem.Msg) {
                val oldMsg = oldItem.message
                val newMsg = newItem.message
                if (oldMsg.id != 0 && oldMsg.id == newMsg.id) return true
                if (oldMsg.id == 0 && newMsg.sender_id == oldMsg.sender_id && newMsg.content == oldMsg.content) {
                    return true
                }
                return false
            }
            return false
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
