package com.kia.blagogramapp.adapters

import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kia.blagogramapp.DesignEngine
import com.kia.blagogramapp.R
import com.kia.blagogramapp.model.Message
import com.kia.blagogramapp.models.ChatListItem
import com.kia.blagogramapp.utils.TimeUtils
import com.kia.blagogramapp.LayoutHelper

class MessagesAdapter(
    private val myId: Int,
    private val onImageClick: (String, Message) -> Unit,
    private val onLongClick: (Message) -> Unit
) : ListAdapter<ChatListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    private val typeHeader = 0
    private val typeLeft = 1
    private val typeRight = 2
    private val typeUnreadSeparator = 3

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is ChatListItem.Header -> typeHeader
            is ChatListItem.UnreadSeparator -> typeUnreadSeparator
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
            typeUnreadSeparator -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_unread_separator, parent, false)
                UnreadSeparatorHolder(v)
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
        when (val item = getItem(position)) {
            is ChatListItem.Header -> (holder as HeaderHolder).bind(item.title)
            is ChatListItem.UnreadSeparator -> (holder as UnreadSeparatorHolder).bind()
            is ChatListItem.Msg -> {
                val prev = if (position > 0) getItem(position - 1) else null
                val next = if (position < itemCount - 1) getItem(position + 1) else null

                val isPrevSame = prev is ChatListItem.Msg && prev.message.sender_id == item.message.sender_id
                val isNextSame = next is ChatListItem.Msg && next.message.sender_id == item.message.sender_id

                (holder as MsgHolder).bind(
                    item.message,
                    onImageClick,
                    onLongClick,
                    isPrevSame,
                    isNextSame
                )
            }
        }
    }

    fun update(messages: List<Message>, onUpdated: (() -> Unit)? = null) {
        submitList(buildList(messages), onUpdated)
    }

    fun addMessage(message: Message, onAdded: (() -> Unit)? = null) {
        val currentItems = currentList.toMutableList()
        val header = TimeUtils.headerFor(message.sent_at)
        val lastItem = currentItems.lastOrNull()
        
        var needsHeader = true
        if (lastItem is ChatListItem.Msg) {
            if (TimeUtils.headerFor(lastItem.message.sent_at) == header) needsHeader = false
        } else if (lastItem is ChatListItem.Header) {
            if (lastItem.title == header) needsHeader = false
        } else if (currentItems.isEmpty()) {
            needsHeader = true
        }

        if (needsHeader) {
            currentItems.add(ChatListItem.Header(header))
        }
        currentItems.add(ChatListItem.Msg(message))
        submitList(currentItems, onAdded)
    }

    private fun buildList(messages: List<Message>): List<ChatListItem> {
        val list = mutableListOf<ChatListItem>()
        var lastHeader = ""
        var unreadSeparatorAdded = false
        
        for (m in messages) {
            val header = TimeUtils.headerFor(m.sent_at)
            if (header != lastHeader) {
                list.add(ChatListItem.Header(header))
                lastHeader = header
            }
            
            // Если сообщение не моё и не прочитано, и мы еще не добавили разделитель
            if (!unreadSeparatorAdded && m.sender_id != myId && m.is_read == 0) {
                list.add(ChatListItem.UnreadSeparator)
                unreadSeparatorAdded = true
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
        val statusIcon: ImageView? = v.findViewById(R.id.msgStatusIcon)
        val bubble: View = if (isOutgoing) v.findViewById(R.id.bubbleRight) else v.findViewById(R.id.bubbleLeft)

        fun bind(
            msg: Message,
            onImageClick: (String, Message) -> Unit,
            onLongClick: (Message) -> Unit,
            isPrevSame: Boolean,
            isNextSame: Boolean
        ) {
            text.text = msg.content
            time.text = TimeUtils.formatTime(msg.sent_at)
            editedIcon.visibility = if (msg.edited_at != null) View.VISIBLE else View.GONE

            val bubbleDrawable = (bubble.background as GradientDrawable).mutate() as GradientDrawable
            
            val radius = LayoutHelper.dp(16).toFloat()
            val smallRadius = LayoutHelper.dp(4).toFloat()

            if (isOutgoing) {
                bubbleDrawable.setColor(DesignEngine.Theme.colorBlue)
                text.setTextColor(0xFFFFFFFF.toInt())
                time.setTextColor(0xCCFFFFFF.toInt())

                val tr = if (isPrevSame) smallRadius else radius
                val br = if (isNextSame) smallRadius else radius

                bubbleDrawable.cornerRadii = floatArrayOf(
                    radius, radius, // Top-left
                    tr, tr,         // Top-right
                    br, br,         // Bottom-right
                    radius, radius  // Bottom-left
                )
                
                statusIcon?.apply {
                    visibility = View.VISIBLE
                    setImageResource(if (msg.is_read == 1) R.drawable.ic_double_check else R.drawable.ic_check)
                    setColorFilter(0xCCFFFFFF.toInt(), PorterDuff.Mode.SRC_IN)
                }
            } else {
                bubbleDrawable.setColor(DesignEngine.Theme.colorCardBackground)
                text.setTextColor(DesignEngine.Theme.colorTextPrimary)
                time.setTextColor(DesignEngine.Theme.colorTextSecondary)

                val tl = if (isPrevSame) smallRadius else radius
                val bl = if (isNextSame) smallRadius else radius

                bubbleDrawable.cornerRadii = floatArrayOf(
                    tl, tl,         // Top-left
                    radius, radius, // Top-right
                    radius, radius, // Bottom-right
                    bl, bl          // Bottom-left
                )
                
                statusIcon?.visibility = View.GONE
            }

            text.visibility = if (msg.content.isEmpty()) View.GONE else View.VISIBLE

            if (msg.images.isNotEmpty()) {
                imagesRecycler.visibility = View.VISIBLE
                
                val spanCount = when {
                    msg.images.size >= 3 -> 3
                    msg.images.size == 2 -> 2
                    else -> 1
                }
                
                val isGrid = msg.images.size > 1
                imagesRecycler.layoutManager = GridLayoutManager(itemView.context, spanCount)
                
                val params = imagesRecycler.layoutParams
                params.width = if (isGrid || msg.images.size == 1) LayoutHelper.dp(250) else ViewGroup.LayoutParams.WRAP_CONTENT
                imagesRecycler.layoutParams = params

                val urls = msg.images.map {
                    if (it.startsWith("content:") || it.startsWith("file:")) it
                    else "https://blagogram.ru/uploads/${msg.sender_id}/$it"
                }
                imagesRecycler.adapter = MessageImagesAdapter(urls, isGrid) { clickedUrl ->
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
    
    class UnreadSeparatorHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val title: TextView = v.findViewById(R.id.unreadTitle)
        private val line: View? = v.findViewById<View>(R.id.unreadTitle).parent.let { 
            if (it is ViewGroup) it.getChildAt(0) else null 
        }

        fun bind() {
            title.setTextColor(DesignEngine.Theme.colorBlue)
            title.setBackgroundResource(R.drawable.day_header_background)
            (title.background as? GradientDrawable)?.apply {
                setColor(DesignEngine.Theme.colorCardBackground)
                alpha = 240
            }
            line?.setBackgroundColor(DesignEngine.Theme.colorBlue)
            line?.alpha = 0.3f
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatListItem>() {
        override fun areItemsTheSame(oldItem: ChatListItem, newItem: ChatListItem): Boolean {
            if (oldItem is ChatListItem.Header && newItem is ChatListItem.Header) {
                return oldItem.title == newItem.title
            }
            if (oldItem is ChatListItem.UnreadSeparator && newItem is ChatListItem.UnreadSeparator) {
                return true
            }
            if (oldItem is ChatListItem.Msg && newItem is ChatListItem.Msg) {
                val oldMsg = oldItem.message
                val newMsg = newItem.message
                if (oldMsg.id != 0 && oldMsg.id == newMsg.id) return true
                return oldMsg.id == 0 && newMsg.sender_id == oldMsg.sender_id && newMsg.content == oldMsg.content && oldMsg.sent_at == newMsg.sent_at
            }
            return false
        }

        override fun areContentsTheSame(oldItem: ChatListItem, newItem: ChatListItem): Boolean {
            return oldItem == newItem
        }
    }
}
