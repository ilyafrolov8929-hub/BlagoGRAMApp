package com.kia.blagogramapp.adapters

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kia.blagogramapp.DesignEngine
import com.kia.blagogramapp.LayoutHelper
import com.kia.blagogramapp.R
import com.kia.blagogramapp.model.Message
import com.kia.blagogramapp.models.ChatListItem
import com.kia.blagogramapp.utils.TimeUtils

class MessagesAdapter(
    itemsInput: List<ChatListItem>,
    private val myId: Int,
    private val onImageClick: (String, Message) -> Unit,
    private val onLongClick: (Message) -> Unit
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
                MsgHolder(v, isOutgoing = true)
            }
            else -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.message_left, parent, false)
                MsgHolder(v, isOutgoing = false)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {

            is ChatListItem.Header -> {
                (holder as HeaderHolder).bind(item.title)
            }

            is ChatListItem.Msg -> {
                (holder as MsgHolder).bind(item.message, onImageClick, onLongClick)
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

    class MsgHolder(v: View, private val isOutgoing: Boolean) : RecyclerView.ViewHolder(v) {
        val text: TextView = v.findViewById(R.id.msgText)
        val time: TextView = v.findViewById(R.id.msgTime)
        val imagesRecycler: RecyclerView = v.findViewById(R.id.msgImages)
        val editedIcon: ImageView = v.findViewById(R.id.msgEditedIcon)
        val bubble: View = if (isOutgoing) v.findViewById(R.id.bubbleRight) else v.findViewById(R.id.bubbleLeft)

        fun bind(msg: Message, onImageClick: (String, Message) -> Unit, onLongClick: (Message) -> Unit) {
            text.text = msg.content
            time.text = TimeUtils.formatTime(msg.sent_at)

            editedIcon.visibility = if (msg.edited_at != null) View.VISIBLE else View.GONE

            // --- Apply Theme Colors ---
            val bubbleDrawable = bubble.background as GradientDrawable
            if (isOutgoing) {
                bubbleDrawable.setColor(DesignEngine.Theme.colorBlue)
                text.setTextColor(0xFFFFFFFF.toInt())
                time.setTextColor(0xCCFFFFFF.toInt())
            } else {
                bubbleDrawable.setColor(DesignEngine.Theme.colorCardBackground)
                text.setTextColor(DesignEngine.Theme.colorTextPrimary)
                time.setTextColor(DesignEngine.Theme.colorTextSecondary)
            }

            if (msg.content.isEmpty()) {
                text.visibility = View.GONE
            } else {
                text.visibility = View.VISIBLE
            }

            if (msg.images.isNotEmpty()) {
                imagesRecycler.visibility = View.VISIBLE
                imagesRecycler.layoutManager = LinearLayoutManager(itemView.context)

                val urls = msg.images.map {
                    if (it.startsWith("content:") || it.startsWith("file:")) {
                        it
                    } else {
                        "https://blagogram.ru/uploads/${msg.sender_id}/$it"
                    }
                }

                imagesRecycler.adapter = MessageImagesAdapter(urls) { clickedUrl ->
                    onImageClick(clickedUrl, msg)
                }

            } else {
                imagesRecycler.visibility = View.GONE
            }

            bubble.setOnLongClickListener {
                onLongClick(msg)
                true
            }
        }
    }

    class HeaderHolder(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.dayTitle)
        
        fun bind(text: String) {
            title.text = text
            val bg = title.background as GradientDrawable
            bg.setColor(DesignEngine.Theme.colorCardBackground)
            bg.alpha = 200
            title.setTextColor(DesignEngine.Theme.colorTextSecondary)
        }
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
