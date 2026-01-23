package com.kia.blagogramapp.adapters

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.imageview.ShapeableImageView
import com.kia.blagogramapp.*
import android.view.LayoutInflater
import android.widget.Toast
import com.kia.blagogramapp.api.ApiClient
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.database.AppDatabase
import com.kia.blagogramapp.database.UserEntity
import com.kia.blagogramapp.model.Message
import com.kia.blagogramapp.model.Post
import com.kia.blagogramapp.models.ChatListItem
import com.kia.blagogramapp.utils.TimeUtils
import io.noties.markwon.Markwon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation

class MessagesAdapter(
    private val myId: Int,
    private val onImageClick: (String, Message) -> Unit,
    private val onLongClick: (Message) -> Unit,
    private val onReplyClick: (Message) -> Unit
) : ListAdapter<ChatListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    private val typeHeader = 0
    private val typeLeft = 1
    private val typeRight = 2
    private val typeUnreadSeparator = 3
    private val typeSpace = 4

    private var markwon: Markwon? = null
    private val userCache = mutableMapOf<Int, UserEntity>()

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
            is ChatListItem.Msg -> (holder as MsgHolder).bind(item, onImageClick, onLongClick, onReplyClick, userCache)
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

            // КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ: группируем по sender_id, а не по ID сообщения
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

        val replyContainer: View = v.findViewById(R.id.replyContainer)
        val replyName: TextView = v.findViewById(R.id.replyName)
        val replyText: TextView = v.findViewById(R.id.replyText)
        val replyImage: ImageView = v.findViewById(R.id.replyImage)

        private var sharedPostRoot: LinearLayout? = null
        private var sharedPostHeaderPill: LinearLayout? = null
        private var sharedPostAvatar: ShapeableImageView? = null
        private var sharedPostName: TextView? = null
        private var sharedPostTime: TextView? = null
        private var sharedPostImage: ShapeableImageView? = null
        private var sharedPostContent: TextView? = null

        private fun setStatusIcon(msg: Message, isOnlyEmoji: Boolean = false) {
            val icon = statusIcon ?: return
            icon.clearAnimation()
            if (msg.id < 0) {
                icon.setImageResource(R.drawable.ic_clock)
                val rotate = RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
                    duration = 1000
                    repeatCount = Animation.INFINITE
                    interpolator = LinearInterpolator()
                }
                icon.startAnimation(rotate)
            } else {
                icon.setImageResource(if (msg.is_read == 1) R.drawable.ic_double_check else R.drawable.ic_check)
            }
            val iconColor = if (isOnlyEmoji) 0xFFFFFFFF.toInt() else 0xCCFFFFFF.toInt()
            icon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
        }

        fun bind(item: ChatListItem.Msg, onImageClick: (String, Message) -> Unit, onLongClick: (Message) -> Unit, onReplyClick: (Message) -> Unit, userCache: MutableMap<Int, UserEntity>) {
            val msg = item.message

            sharedPostRoot?.visibility = View.GONE
            text.visibility = View.GONE
            text.typeface = Typeface.DEFAULT
            imagesRecycler.visibility = View.GONE

            if (msg.reply != null) {
                replyContainer.visibility = View.VISIBLE
                replyName.text = msg.reply.display_name
                replyText.text = if (msg.reply.content.isNotEmpty()) msg.reply.content else "Фотография"
                if (msg.reply.images.isNotEmpty()) {
                    replyImage.visibility = View.VISIBLE
                    replyImage.load("https://blagogram.ru/uploads/${msg.chat_id}/${msg.reply.images[0]}")
                } else {
                    replyImage.visibility = View.GONE
                }
                replyContainer.setOnClickListener { onReplyClick(msg) }
            } else {
                replyContainer.visibility = View.GONE
            }

            var sharedPostId: Int? = null
            var comment: String? = null

            try {
                val json = JSONObject(msg.content.trim())
                if (json.optString("type") == "shared_post") {
                    sharedPostId = json.optInt("post_id", 0).takeIf { it > 0 }
                    comment = json.optString("comment", null)?.takeIf { it.isNotBlank() }
                }
            } catch (_: Exception) { }

            if (sharedPostId != null) {
                if (comment != null) {
                    text.visibility = View.VISIBLE
                    markwon.setMarkdown(text, comment)
                    text.setTextColor(if (isOutgoing) Color.WHITE else DesignEngine.Theme.colorTextPrimary)
                    text.setTextSize(TypedValue.COMPLEX_UNIT_SP, DesignEngine.fontSize.toFloat())
                    text.setPadding(LayoutHelper.dp(12), LayoutHelper.dp(6), LayoutHelper.dp(12), LayoutHelper.dp(4))
                    val params = text.layoutParams as? ViewGroup.MarginLayoutParams
                    params?.rightMargin = if (isOutgoing) LayoutHelper.dp(45) else LayoutHelper.dp(30)
                    text.layoutParams = params
                }

                ensureSharedPostUI()
                sharedPostRoot?.visibility = View.VISIBLE
                loadAndDisplaySharedPost(sharedPostId, userCache)

                timeContainer.visibility = View.VISIBLE
                time.visibility = View.VISIBLE
                time.text = TimeUtils.formatTime(msg.sent_at)
                timeContainer.background = null
                editedIcon.visibility = if (msg.edited_at != null) View.VISIBLE else View.GONE

                timeContainer.bringToFront()
                statusIcon?.bringToFront()

                if (isOutgoing) {
                    statusIcon?.visibility = View.VISIBLE
                    setStatusIcon(msg)
                } else {
                    statusIcon?.visibility = View.GONE
                }

                val bubbleDrawable = (bubble.background as GradientDrawable).mutate() as GradientDrawable
                val radius = LayoutHelper.dp(DesignEngine.bubbleRadius).toFloat()
                val smallRadius = LayoutHelper.dp(4).toFloat()
                if (isOutgoing) {
                    val tr = if (item.isPrevSame) smallRadius else radius
                    val br = if (item.isNextSame) smallRadius else radius
                    bubbleDrawable.cornerRadii = floatArrayOf(radius, radius, tr, tr, br, br, radius, radius)
                } else {
                    val tl = if (item.isPrevSame) smallRadius else radius
                    val bl = if (item.isNextSame) smallRadius else radius
                    bubbleDrawable.cornerRadii = floatArrayOf(tl, tl, radius, radius, radius, radius, bl, bl)
                }
            } else {
                text.typeface = Typeface.DEFAULT
                bindNormalMessage(item, onImageClick, onLongClick)
            }
            
            bubble.setOnLongClickListener { 
                onLongClick(msg)
                true
            }
        }

        private fun ensureSharedPostUI() {
            if (sharedPostRoot == null) {
                val container = bubble as ViewGroup
                val context = itemView.context
                sharedPostRoot = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(LayoutHelper.dp(280), ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                val divider = View(context).apply {
                    background = GradientDrawable().apply {
                        setColor(if (isOutgoing) 0x40FFFFFF else 0x20000000)
                        cornerRadius = LayoutHelper.dp(2).toFloat()
                    }
                }
                sharedPostRoot?.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 4, 0f, 8, 0, 30))
                sharedPostHeaderPill = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = GradientDrawable().apply {
                        setColor(if (isOutgoing) 0x33000000 else 0x15000000)
                        cornerRadius = LayoutHelper.dp(20).toFloat()
                    }
                    setPadding(LayoutHelper.dp(8), LayoutHelper.dp(6), LayoutHelper.dp(16), LayoutHelper.dp(6))
                }
                sharedPostAvatar = ShapeableImageView(context).apply {
                    shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(LayoutHelper.dp(16).toFloat()).build()
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
                sharedPostHeaderPill?.addView(sharedPostAvatar, LayoutHelper.createLinear(32, 32))
                val nameBox = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(LayoutHelper.dp(8), 0, 0, 0)
                }
                sharedPostName = TextView(context).apply {
                    textSize = 13f
                    setTextColor(if (isOutgoing) Color.WHITE else DesignEngine.Theme.colorTextPrimary)
                }
                sharedPostTime = TextView(context).apply {
                    textSize = 11f
                    setTextColor(if (isOutgoing) 0xB3FFFFFF.toInt() else DesignEngine.Theme.colorTextSecondary)
                }
                nameBox.addView(sharedPostName)
                nameBox.addView(sharedPostTime)
                sharedPostHeaderPill?.addView(nameBox)
                sharedPostRoot?.addView(sharedPostHeaderPill, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 10))
                sharedPostContent = TextView(context).apply {
                    textSize = DesignEngine.fontSize.toFloat()
                    setTextColor(if (isOutgoing) Color.WHITE else DesignEngine.Theme.colorTextPrimary)
                    setPadding(LayoutHelper.dp(14), 0, LayoutHelper.dp(14), LayoutHelper.dp(8))
                }
                sharedPostRoot?.addView(sharedPostContent)
                sharedPostImage = ShapeableImageView(context).apply {
                    visibility = View.GONE
                    adjustViewBounds = true
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                        .setAllCornerSizes(LayoutHelper.dp(DesignEngine.bubbleRadius).toFloat())
                        .build()
                }
                sharedPostRoot?.addView(sharedPostImage, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 24))
                container.addView(sharedPostRoot)
            }
        }

        private fun loadAndDisplaySharedPost(postId: Int, userCache: MutableMap<Int, UserEntity>) {
            val context = itemView.context
            val token = context.getSharedPreferences("app", android.content.Context.MODE_PRIVATE).getString("token", "") ?: ""
            val apiRepo = ApiRepository(ApiClient.retrofit.create(BlagoApi::class.java))
            val db = AppDatabase.getDatabase(context)

            val bubbleDrawable = (bubble.background as GradientDrawable).mutate() as GradientDrawable
            if (isOutgoing) {
                bubbleDrawable.setColor(DesignEngine.Theme.colorBlue)
                time.setTextColor(0xCCFFFFFF.toInt())
            } else {
                bubbleDrawable.setColor(DesignEngine.Theme.colorCardBackground)
                time.setTextColor(DesignEngine.Theme.colorTextSecondary)
            }

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val cachedPost = withContext(Dispatchers.IO) { db.postDao().getById(postId) }
                    var post: Post? = cachedPost?.let { Post(it.id, it.authorId, it.title, it.content, it.image, it.createdAt, it.likes, it.liked) }
                    if (post == null) {
                        val posts = withContext(Dispatchers.IO) { apiRepo.getPosts(token) }
                        post = posts.find { it.id == postId }
                    }
                    if (post != null) {
                        val params = sharedPostRoot?.layoutParams
                        params?.width = LayoutHelper.dp(280)
                        sharedPostHeaderPill?.visibility = View.VISIBLE
                        loadAuthorInfo(post.authorId, userCache, token, apiRepo, db)
                        sharedPostTime?.text = formatPostDate(post.createdAt)
                        sharedPostContent?.text = post.content
                        sharedPostContent?.setTextColor(if (isOutgoing) Color.WHITE else DesignEngine.Theme.colorTextPrimary)
                        if (!post.image.isNullOrEmpty()) {
                            sharedPostImage?.visibility = View.VISIBLE
                            sharedPostImage?.load("https://blagogram.ru/posts/${post.image}")
                        } else {
                            sharedPostImage?.visibility = View.GONE
                        }
                    } else {
                        val params = sharedPostRoot?.layoutParams
                        params?.width = LayoutHelper.dp(200)
                        sharedPostHeaderPill?.visibility = View.GONE
                        sharedPostImage?.visibility = View.GONE
                        sharedPostContent?.text = "Пост не найден"
                        sharedPostContent?.setTextColor(if (isOutgoing) Color.WHITE else DesignEngine.Theme.colorTextPrimary)
                    }
                } catch (e: Exception) {
                    sharedPostHeaderPill?.visibility = View.GONE
                    sharedPostImage?.visibility = View.GONE
                    sharedPostContent?.text = "Ошибка загрузки"
                }
            }
        }

        private suspend fun loadAuthorInfo(userId: Int, userCache: MutableMap<Int, UserEntity>, token: String, apiRepo: ApiRepository, db: AppDatabase) {
            userCache[userId]?.let {
                sharedPostName?.text = it.displayName ?: it.username
                sharedPostAvatar?.load(if (!it.avatar.isNullOrEmpty()) "https://blagogram.ru/avatars/${it.avatar}" else null) {
                    placeholder(R.drawable.ic_user_placeholder)
                    error(R.drawable.ic_user_placeholder)
                    transformations(CircleCropTransformation())
                }
                return
            }
            val cachedUser = withContext(Dispatchers.IO) { db.userDao().getById(userId) }
            if (cachedUser != null && (System.currentTimeMillis() - cachedUser.lastUpdated < 3600000)) {
                userCache[userId] = cachedUser
                sharedPostName?.text = cachedUser.displayName ?: cachedUser.username
                sharedPostAvatar?.load(if (!cachedUser.avatar.isNullOrEmpty()) "https://blagogram.ru/avatars/${cachedUser.avatar}" else null) {
                    placeholder(R.drawable.ic_user_placeholder)
                    error(R.drawable.ic_user_placeholder)
                    transformations(CircleCropTransformation())
                }
            } else {
                try {
                    val profile = apiRepo.getUserProfile(token, userId)
                    val userEntity = UserEntity(profile.id, profile.username, profile.display_name, profile.avatar, System.currentTimeMillis())
                    withContext(Dispatchers.IO) { db.userDao().insert(userEntity) }
                    userCache[userId] = userEntity
                    sharedPostName?.text = userEntity.displayName ?: userEntity.username
                    sharedPostAvatar?.load(if (!userEntity.avatar.isNullOrEmpty()) "https://blagogram.ru/avatars/${userEntity.avatar}" else null) {
                        placeholder(R.drawable.ic_user_placeholder)
                        error(R.drawable.ic_user_placeholder)
                        transformations(CircleCropTransformation())
                    }
                } catch (_: Exception) {
                    sharedPostName?.text = "Пользователь #$userId"
                    sharedPostAvatar?.setImageResource(R.drawable.ic_user_placeholder)
                }
            }
        }

        private fun formatPostDate(dateStr: String): String {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val date = sdf.parse(dateStr) ?: return dateStr
                val now = Calendar.getInstance()
                val postCal = Calendar.getInstance().apply { time = date }
                val timePart = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                if (now.get(Calendar.YEAR) == postCal.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == postCal.get(Calendar.DAY_OF_YEAR)) "сегодня в $timePart"
                else SimpleDateFormat("d MMM в HH:mm", Locale.getDefault()).format(date)
            } catch (e: Exception) { dateStr }
        }

        private fun bindNormalMessage(item: ChatListItem.Msg, onImageClick: (String, Message) -> Unit, onLongClick: (Message) -> Unit) {
            val msg = item.message
            val emojiCount = getEmojiCount(msg.content)
            val isOnlyEmoji = emojiCount > 0 && msg.images.isEmpty() && isStringOnlyEmoji(msg.content)

            if (msg.content.isEmpty()) text.visibility = View.GONE else {
                text.visibility = View.VISIBLE
                if (isOnlyEmoji) text.text = msg.content else markwon.setMarkdown(text, msg.content)
            }
            
            if (isOnlyEmoji) {
                val emojiSize = when (emojiCount) {
                    1 -> 64f
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
                    
                    // КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ: группируем баблы правильно
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
                statusIcon?.visibility = View.VISIBLE
                setStatusIcon(msg, isOnlyEmoji)
            } else {
                statusIcon?.visibility = View.GONE
            }

            if (msg.images.isNotEmpty()) {
                imagesRecycler.visibility = View.VISIBLE
                val urls = msg.images.map { if (it.startsWith("content:") || it.startsWith("file:")) it else "https://blagogram.ru/uploads/${msg.chat_id}/$it" }
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
            return (codePoint in 0x1F600..0x1F64F) || (codePoint in 0x1F300..0x1F5FF) || (codePoint in 0x1F680..0x1F6FF) || (codePoint in 0x2600..0x26FF) || (codePoint in 0x2700..0x27BF) || (codePoint in 0xFE00..0xFE0F) || (codePoint in 0x1F900..0x1F9FF) || (codePoint in 0x1F1E6..0x1F1FF)
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
            if (oldItem is ChatListItem.Msg && newItem is ChatListItem.Msg) {
                val m1 = oldItem.message
                val m2 = newItem.message
                if (m1.id == m2.id) return true
                if ((m1.id < 0 || m2.id < 0) && m1.sender_id == m2.sender_id && m1.content == m2.content) return true
                return false
            }
            if (oldItem is ChatListItem.Header && newItem is ChatListItem.Header) return oldItem.title == newItem.title
            return oldItem == newItem
        }
        override fun areContentsTheSame(oldItem: ChatListItem, newItem: ChatListItem) = oldItem == newItem
    }
}
