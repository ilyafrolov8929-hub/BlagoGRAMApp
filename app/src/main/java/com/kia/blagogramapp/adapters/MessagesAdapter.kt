package com.kia.blagogramapp.adapters

import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import com.kia.blagogramapp.DesignEngine
import com.kia.blagogramapp.R
import com.kia.blagogramapp.model.Message
import com.kia.blagogramapp.models.ChatListItem
import com.kia.blagogramapp.utils.TimeUtils
import com.kia.blagogramapp.LayoutHelper
import io.noties.markwon.Markwon
import java.text.SimpleDateFormat
import java.util.*

class MessagesAdapter(
    private val myId: Int,
    private val onImageClick: (String, Message) -> Unit,
    private val onLongClick: (Message) -> Unit
) : ListAdapter<ChatListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    private val typeHeader = 0
    private val typeLeft = 1
    private val typeRight = 2
    private val typeUnreadSeparator = 3
    private val typeSpace = 4
    
    private var markwon: Markwon? = null

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is ChatListItem.Header -> typeHeader
            is ChatListItem.Space -> typeSpace
            is ChatListItem.UnreadSeparator -> typeUnreadSeparator
            is ChatListItem.Msg ->
                if (item.message.sender_id == myId) typeRight else typeLeft
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (markwon == null) markwon = Markwon.create(parent.context)
        return when (viewType) {
            typeHeader -> HeaderHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_day_header, parent, false))
            typeSpace -> SpaceHolder(View(parent.context).apply { 
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutHelper.dp(12)) 
            })
            typeUnreadSeparator -> UnreadSeparatorHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_unread_separator, parent, false))
            typeRight -> MsgHolder(LayoutInflater.from(parent.context).inflate(R.layout.message_right, parent, false), true, markwon!!)
            else -> MsgHolder(LayoutInflater.from(parent.context).inflate(R.layout.message_left, parent, false), false, markwon!!)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ChatListItem.Header -> (holder as HeaderHolder).bind(item.title)
            is ChatListItem.Msg -> (holder as MsgHolder).bind(item, onImageClick, onLongClick)
            else -> {}
        }
    }

    fun update(messages: List<Message>, onUpdated: (() -> Unit)? = null) {
        submitList(buildList(messages), onUpdated)
    }

    private fun buildList(messages: List<Message>): List<ChatListItem> {
        val list = mutableListOf<ChatListItem>()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        var lastDate = ""
        var lastTime: Long = 0
        var unreadSeparatorAdded = false

        for (i in messages.indices) {
            val m = messages[i]
            val date = TimeUtils.headerFor(m.sent_at)
            val currentTime = try { sdf.parse(m.sent_at)?.time ?: 0L } catch (e: Exception) { 0L }

            if (date != lastDate) {
                list.add(ChatListItem.Header(date))
                lastDate = date
                lastTime = currentTime
            } 
            else if (lastTime != 0L && currentTime - lastTime > 5 * 60 * 1000) {
                list.add(ChatListItem.Space)
                lastTime = currentTime
            }

            if (!unreadSeparatorAdded && m.sender_id != myId && m.is_read == 0) {
                list.add(ChatListItem.UnreadSeparator)
                unreadSeparatorAdded = true
            }

            val prevMsg = if (i > 0) messages[i-1] else null
            val nextMsg = if (i < messages.size - 1) messages[i+1] else null
            val nextTime = if (nextMsg != null) try { sdf.parse(nextMsg.sent_at)?.time ?: 0L } catch (e: Exception) { 0L } else 0L

            // Группировка углов сбрасывается, если между сообщениями есть Space (разница > 5 мин)
            val isPrevSame = prevMsg?.sender_id == m.sender_id && (currentTime - (try { sdf.parse(prevMsg!!.sent_at)?.time ?: 0L } catch (e: Exception) { 0L }) <= 5 * 60 * 1000)
            val isNextSame = nextMsg?.sender_id == m.sender_id && (nextTime - currentTime <= 5 * 60 * 1000)

            list.add(ChatListItem.Msg(m, isPrevSame, isNextSame))
            lastTime = currentTime
        }
        return list
    }

    class MsgHolder(v: View, private val isOutgoing: Boolean, private val markwon: Markwon) : RecyclerView.ViewHolder(v) {
        val text: TextView = v.findViewById(R.id.msgText)
        val time: TextView = v.findViewById(R.id.msgTime)
        val imagesRecycler: RecyclerView = v.findViewById(R.id.msgImages)
        val editedIcon: ImageView = v.findViewById(R.id.msgEditedIcon)
        val statusIcon: ImageView? = v.findViewById(R.id.msgStatusIcon)
        val bubble: View = if (isOutgoing) v.findViewById(R.id.bubbleRight) else v.findViewById(R.id.bubbleLeft)

        fun bind(item: ChatListItem.Msg, onImageClick: (String, Message) -> Unit, onLongClick: (Message) -> Unit) {
            val msg = item.message
            if (msg.content.isEmpty()) text.visibility = View.GONE else {
                text.visibility = View.VISIBLE
                markwon.setMarkdown(text, msg.content)
            }
            time.text = TimeUtils.formatTime(msg.sent_at)
            editedIcon.visibility = if (msg.edited_at != null) View.VISIBLE else View.GONE
            
            val bubbleDrawable = (bubble.background as GradientDrawable).mutate() as GradientDrawable
            val radius = LayoutHelper.dp(16).toFloat()
            val smallRadius = LayoutHelper.dp(4).toFloat()

            if (isOutgoing) {
                bubbleDrawable.setColor(DesignEngine.Theme.colorBlue)
                text.setTextColor(0xFFFFFFFF.toInt())
                time.setTextColor(0xCCFFFFFF.toInt())
                editedIcon.setColorFilter(0xCCFFFFFF.toInt(), PorterDuff.Mode.SRC_IN)
                
                val tr = if (item.isPrevSame) smallRadius else radius
                val br = if (item.isNextSame) smallRadius else radius
                bubbleDrawable.cornerRadii = floatArrayOf(radius, radius, tr, tr, br, br, radius, radius)
                
                statusIcon?.apply {
                    visibility = View.VISIBLE
                    setImageResource(if (msg.is_read == 1) R.drawable.ic_double_check else R.drawable.ic_check)
                    setColorFilter(0xCCFFFFFF.toInt(), PorterDuff.Mode.SRC_IN)
                }
            } else {
                bubbleDrawable.setColor(DesignEngine.Theme.colorCardBackground)
                text.setTextColor(DesignEngine.Theme.colorTextPrimary)
                time.setTextColor(DesignEngine.Theme.colorTextSecondary)
                editedIcon.setColorFilter(DesignEngine.Theme.colorTextSecondary, PorterDuff.Mode.SRC_IN)
                
                val tl = if (item.isPrevSame) smallRadius else radius
                val bl = if (item.isNextSame) smallRadius else radius
                bubbleDrawable.cornerRadii = floatArrayOf(tl, tl, radius, radius, radius, radius, bl, bl)
                statusIcon?.visibility = View.GONE
            }

            if (msg.images.isNotEmpty()) {
                imagesRecycler.visibility = View.VISIBLE
                val spanCount = if (msg.images.size >= 3) 3 else if (msg.images.size == 2) 2 else 1
                imagesRecycler.layoutManager = GridLayoutManager(itemView.context, spanCount)
                val params = imagesRecycler.layoutParams
                params.width = LayoutHelper.dp(250)
                imagesRecycler.layoutParams = params
                val urls = msg.images.map { if (it.startsWith("content:") || it.startsWith("file:")) it else "https://blagogram.ru/uploads/${msg.sender_id}/$it" }
                imagesRecycler.adapter = MessageImagesAdapter(urls, msg.images.size > 1) { onImageClick(it, msg) }
            } else imagesRecycler.visibility = View.GONE

            bubble.setOnLongClickListener { onLongClick(msg); true }
        }
    }

    class HeaderHolder(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.dayTitle)
        fun bind(text: String) {
            title.text = text
            (title.background as GradientDrawable).setColor(DesignEngine.Theme.colorCardBackground)
            title.setTextColor(DesignEngine.Theme.colorTextSecondary)
        }
    }

    class SpaceHolder(v: View) : RecyclerView.ViewHolder(v)

    class UnreadSeparatorHolder(v: View) : RecyclerView.ViewHolder(v) {
        fun bind() {
            val title = itemView.findViewById<TextView>(R.id.unreadTitle)
            title.setTextColor(DesignEngine.Theme.colorBlue)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatListItem>() {
        override fun areItemsTheSame(oldItem: ChatListItem, newItem: ChatListItem): Boolean {
            if (oldItem is ChatListItem.Msg && newItem is ChatListItem.Msg) return oldItem.message.id == newItem.message.id
            if (oldItem is ChatListItem.Header && newItem is ChatListItem.Header) return oldItem.title == newItem.title
            return oldItem == newItem
        }
        override fun areContentsTheSame(oldItem: ChatListItem, newItem: ChatListItem) = oldItem == newItem
    }
}
