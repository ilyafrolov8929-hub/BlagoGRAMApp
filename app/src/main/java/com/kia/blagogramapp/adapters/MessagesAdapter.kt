package com.kia.blagogramapp.adapters

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
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
        val timeContainer: View = v.findViewById(R.id.msgTimeContainer)
        val imagesRecycler: RecyclerView = v.findViewById(R.id.msgImages)
        val editedIcon: ImageView = v.findViewById(R.id.msgEditedIcon)
        val statusIcon: ImageView? = v.findViewById(R.id.msgStatusIcon)
        val bubble: View = if (isOutgoing) v.findViewById(R.id.bubbleRight) else v.findViewById(R.id.bubbleLeft)

        fun bind(item: ChatListItem.Msg, onImageClick: (String, Message) -> Unit, onLongClick: (Message) -> Unit) {
            val msg = item.message
            val emojiCount = getEmojiCount(msg.content)
            val isOnlyEmoji = emojiCount > 0 && msg.images.isEmpty() && isStringOnlyEmoji(msg.content)

            if (msg.content.isEmpty()) text.visibility = View.GONE else {
                text.visibility = View.VISIBLE
                if (isOnlyEmoji) {
                    text.text = msg.content
                } else {
                    markwon.setMarkdown(text, msg.content)
                }
            }
            
            // ПРИМЕНЯЕМ РАЗМЕР ТЕКСТА И ПАДДИНГИ
            if (isOnlyEmoji) {
                val emojiSize = when (emojiCount) {
                    1 -> 64f // Очень крупный
                    2 -> 48f
                    3 -> 36f
                    else -> 26f
                }
                text.setTextSize(TypedValue.COMPLEX_UNIT_SP, emojiSize)
                text.setPadding(LayoutHelper.dp(8), LayoutHelper.dp(4), LayoutHelper.dp(8), LayoutHelper.dp(4))
                val params = text.layoutParams as? ViewGroup.MarginLayoutParams
                params?.rightMargin = 0
                text.layoutParams = params
            } else {
                text.setTextSize(TypedValue.COMPLEX_UNIT_SP, DesignEngine.fontSize.toFloat())
                text.setPadding(LayoutHelper.dp(10), LayoutHelper.dp(6), LayoutHelper.dp(10), LayoutHelper.dp(10))
                val params = text.layoutParams as? ViewGroup.MarginLayoutParams
                params?.rightMargin = if (isOutgoing) LayoutHelper.dp(45) else LayoutHelper.dp(30)
                text.layoutParams = params
            }
            
            time.text = TimeUtils.formatTime(msg.sent_at)
            editedIcon.visibility = if (msg.edited_at != null) View.VISIBLE else View.GONE
            
            val bubbleDrawable = (bubble.background as GradientDrawable).mutate() as GradientDrawable
            
            // ПРИМЕНЯЕМ РАДИУС ИЗ НАСТРОЕК
            val radius = LayoutHelper.dp(DesignEngine.bubbleRadius).toFloat()
            val smallRadius = LayoutHelper.dp(4).toFloat()

            if (isOnlyEmoji) {
                bubbleDrawable.setColor(Color.TRANSPARENT)
                bubbleDrawable.setStroke(0, Color.TRANSPARENT)
                
                val timeBg = GradientDrawable().apply {
                    setColor(0x40000000)
                    cornerRadius = LayoutHelper.dp(10).toFloat()
                }
                timeContainer.background = timeBg
                
                text.setTextColor(DesignEngine.Theme.colorTextPrimary)
                time.setTextColor(0xFFFFFFFF.toInt())
                editedIcon.setColorFilter(0xFFFFFFFF.toInt(), PorterDuff.Mode.SRC_IN)
            } else {
                timeContainer.background = null
                if (isOutgoing) {
                    bubbleDrawable.setColor(DesignEngine.Theme.colorBlue)
                    text.setTextColor(0xFFFFFFFF.toInt())
                    time.setTextColor(0xCCFFFFFF.toInt())
                    editedIcon.setColorFilter(0xCCFFFFFF.toInt(), PorterDuff.Mode.SRC_IN)
                    
                    val tr = if (item.isPrevSame) smallRadius else radius
                    val br = if (item.isNextSame) smallRadius else radius
                    bubbleDrawable.cornerRadii = floatArrayOf(radius, radius, tr, tr, br, br, radius, radius)
                } else {
                    bubbleDrawable.setColor(DesignEngine.Theme.colorCardBackground)
                    text.setTextColor(DesignEngine.Theme.colorTextPrimary)
                    time.setTextColor(DesignEngine.Theme.colorTextSecondary)
                    editedIcon.setColorFilter(DesignEngine.Theme.colorTextSecondary, PorterDuff.Mode.SRC_IN)
                    
                    val tl = if (item.isPrevSame) smallRadius else radius
                    val bl = if (item.isNextSame) smallRadius else radius
                    bubbleDrawable.cornerRadii = floatArrayOf(tl, tl, radius, radius, radius, radius, bl, bl)
                }
            }

            if (isOutgoing) {
                statusIcon?.apply {
                    visibility = View.VISIBLE
                    setImageResource(if (msg.is_read == 1) R.drawable.ic_double_check else R.drawable.ic_check)
                    val iconColor = if (isOnlyEmoji) 0xFFFFFFFF.toInt() else 0xCCFFFFFF.toInt()
                    setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
                }
            } else {
                statusIcon?.visibility = View.GONE
            }

            if (msg.images.isNotEmpty()) {
                imagesRecycler.visibility = View.VISIBLE
                val urls = msg.images.map { if (it.startsWith("content:") || it.startsWith("file:")) it else "https://blagogram.ru/uploads/${msg.sender_id}/$it" }
                
                val currentAdapter = imagesRecycler.adapter as? MessageImagesAdapter
                if (currentAdapter == null || currentAdapter.imageUrls != urls) {
                    val spanCount = if (msg.images.size >= 3) 3 else if (msg.images.size == 2) 2 else 1
                    imagesRecycler.layoutManager = GridLayoutManager(itemView.context, spanCount)
                    imagesRecycler.adapter = MessageImagesAdapter(urls, msg.images.size > 1) { onImageClick(it, msg) }
                }

                val params = imagesRecycler.layoutParams
                params.width = LayoutHelper.dp(250)
                imagesRecycler.layoutParams = params
            } else {
                imagesRecycler.visibility = View.GONE
            }

            bubble.setOnLongClickListener { onLongClick(msg); true }
        }

        private fun isStringOnlyEmoji(s: String): Boolean {
            if (s.isEmpty()) return false
            var i = 0
            while (i < s.length) {
                val cp = s.codePointAt(i)
                if (!isEmoji(cp)) return false
                i += Character.charCount(cp)
            }
            return true
        }

        private fun getEmojiCount(s: String): Int {
            var count = 0
            var i = 0
            while (i < s.length) {
                val cp = s.codePointAt(i)
                if (isEmoji(cp)) count++
                i += Character.charCount(cp)
            }
            return count
        }

        private fun isEmoji(codePoint: Int): Boolean {
            return (codePoint in 0x1F600..0x1F64F) || // Emoticons
                   (codePoint in 0x1F300..0x1F5FF) || // Misc Symbols and Pictographs
                   (codePoint in 0x1F680..0x1F6FF) || // Transport and Map
                   (codePoint in 0x2600..0x26FF)   || // Misc Symbols
                   (codePoint in 0x2700..0x27BF)   || // Dingbats
                   (codePoint in 0xFE00..0xFE0F)   || // Variation Selectors
                   (codePoint in 0x1F900..0x1F9FF) || // Supplemental Symbols and Pictographs
                   (codePoint in 0x1F1E6..0x1F1FF)    // Flags
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
