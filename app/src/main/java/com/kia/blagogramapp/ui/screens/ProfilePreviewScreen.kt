package com.kia.blagogramapp

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.imageview.ShapeableImageView
import com.google.gson.Gson
import com.kia.blagogramapp.api.ApiClient
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.database.AppDatabase
import com.kia.blagogramapp.database.ChatMediaCacheEntity
import com.kia.blagogramapp.model.UserProfile
import com.kia.blagogramapp.models.ChatInfoResponse
import com.kia.blagogramapp.models.RecentLinkMessage
import com.kia.blagogramapp.models.RecentPhoto
import com.kia.blagogramapp.ui.screens.ProfileScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfilePreviewScreen(context: Context, private val targetUserId: Int = 0) : BaseScreen(context) {
    private val TAG = "ProfilePreview"
    private lateinit var root: FrameLayout
    private lateinit var contentScroll: ScrollView
    private lateinit var container: LinearLayout

    // Header views
    private lateinit var avatarView: ShapeableImageView
    private lateinit var onlineBadge: TextView
    private lateinit var nameView: TextView
    private lateinit var statusView: TextView

    // Info items
    private lateinit var phoneItem: ProfileInfoItem
    private lateinit var usernameItem: ProfileInfoItem
    private var isCurrentThemeDark: Boolean = false
    private var isCurrentUseMonet: Boolean = false

    private lateinit var repo: ApiRepository
    private lateinit var db: AppDatabase
    private val gson = Gson()

    // Media content
    private lateinit var mediaTabsContainer: LinearLayout
    private lateinit var mediaContentLayout: FrameLayout
    private var currentTab = "photo"

    private var userProfile: UserProfile? = null
    private var chatInfo: ChatInfoResponse? = null

    override fun createView(): View {


        val root = FrameLayout(context)

        val themePrefs = context.getSharedPreferences("app_theme", MODE_PRIVATE)
        isCurrentThemeDark = themePrefs.getBoolean("is_dark_theme", false)
        isCurrentUseMonet = themePrefs.getBoolean("use_monet", false)

        repo = ApiRepository(ApiClient.retrofit.create(BlagoApi::class.java))
        db = AppDatabase.getDatabase(context)

        contentScroll = ScrollView(context).apply {
            isFillViewport = true
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setPadding(0, LayoutHelper.dp(80), 0, 0)
        }
        root.addView(contentScroll, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16))
        }
        contentScroll.addView(container)

        val toolbar = DesignEngine.createToolBar(
            context = context,
            leftButtonIconRes = R.drawable.ic_back,
            isDark = isCurrentThemeDark,
            leftButtonClickListener = { finishFragment() }
        )
        root.addView(toolbar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP))

        buildUI()
        loadUserProfileFromCache(targetUserId)
        loadUserProfile(targetUserId)
        loadChatMediaInfo()

        return root
    }

    private fun buildUI() {
        val cardColor = DesignEngine.Theme.colorCardBackground

        // 1. Header Card
        val headerCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, LayoutHelper.dp(24), 0, LayoutHelper.dp(24))
            background = GradientDrawable().apply {
                setColor(cardColor)
                cornerRadius = LayoutHelper.dp(20).toFloat()
            }
        }

        val avatarContainer = FrameLayout(context)
        avatarView = ShapeableImageView(context).apply {
            shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(LayoutHelper.dp(60).toFloat()).build()
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.LTGRAY)
        }
        avatarContainer.addView(avatarView, LayoutHelper.createFrame(120, 120, Gravity.CENTER))

        onlineBadge = TextView(context).apply {
            text = "был(а) недавно"
            textSize = 9f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(2), LayoutHelper.dp(8), LayoutHelper.dp(2))
            background = GradientDrawable().apply {
                setColor(0x80000000.toInt())
                cornerRadius = LayoutHelper.dp(10).toFloat()
            }
        }
        avatarContainer.addView(onlineBadge, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 0, 0, 4))

        headerCard.addView(avatarContainer, LayoutHelper.createLinear(120, 120, gravity = Gravity.CENTER_HORIZONTAL))
        nameView = TextView(context).apply {
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            gravity = Gravity.CENTER
        }
        headerCard.addView(nameView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 12, 0, 0))

        statusView = TextView(context).apply {
            textSize = 15f
            setTextColor(DesignEngine.Theme.colorTextSecondary)
            gravity = Gravity.CENTER
        }
        headerCard.addView(statusView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 4, 0, 0))

        container.addView(headerCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 12))

        // 2. Info Card
        val infoCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cardColor)
                cornerRadius = LayoutHelper.dp(20).toFloat()
            }
        }
        phoneItem = ProfileInfoItem(context).apply { setOnClickListener { makeCall() } }
        usernameItem = ProfileInfoItem(context)

        infoCard.addView(phoneItem)
        infoCard.addView(createDivider())
        infoCard.addView(usernameItem)

        container.addView(infoCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 12))

        // 3. Actions Row
        val actionsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 5f
        }

        val btnWrite = ProfileActionButton(context).apply {
            setData("Написать", R.drawable.ic_chat)
            setOnClickListener { openChat() }
        }
        val btnCall = ProfileActionButton(context).apply {
            setData("Звонок", R.drawable.ic_mic)
            setOnClickListener { makeCall() }
        }
        val btnProfile = ProfileActionButton(context).apply {
            setData("Профиль", R.drawable.ic_user)
            setOnClickListener { openFullProfile() }
        }
        val btnAdd = ProfileActionButton(context).apply { setData("Добавить...", R.drawable.ic_add) }
        val btnNotify = ProfileActionButton(context).apply { setData("Уведомл...", R.drawable.ic_pin) }

        val p = LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f)
        p.marginEnd = LayoutHelper.dp(8)

        actionsRow.addView(btnWrite, p)
        actionsRow.addView(btnCall, p)
        actionsRow.addView(btnProfile, p)
        actionsRow.addView(btnAdd, p)
        actionsRow.addView(btnNotify, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f))

        container.addView(actionsRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 12))

        // 4. Media Container
        val mediaContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, LayoutHelper.dp(12), 0, LayoutHelper.dp(12))
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorCardBackground)
                cornerRadius = LayoutHelper.dp(20).toFloat()
            }
        }

        val tabsScroll = HorizontalScrollView(context).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            isHorizontalScrollBarEnabled = false
        }
        mediaTabsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(LayoutHelper.dp(12), 0, LayoutHelper.dp(12), 0)
        }
        tabsScroll.addView(mediaTabsContainer, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))

        val tabs = listOf("Фото 0", "Видео 0", "Музыка 0", "Ссылки 0", "Файлы 0")
        tabs.forEach { title ->
            val tabView = createTabPill(title)
            mediaTabsContainer.addView(tabView)
        }
        mediaContainer.addView(tabsScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        mediaContentLayout = FrameLayout(context).apply {
            minimumHeight = LayoutHelper.dp(200)
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(12), LayoutHelper.dp(12), 0)
        }
        mediaContainer.addView(mediaContentLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        container.addView(mediaContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
    }

    private fun createDivider() = View(context).apply {
        setBackgroundColor(DesignEngine.Theme.colorDivider)
        layoutParams = LinearLayout.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.dp(1)).apply {
            setMargins(LayoutHelper.dp(16), 0, LayoutHelper.dp(16), 0)
        }
    }

    private fun createTabPill(title: String): TextView {
        return TextView(context).apply {
            text = title
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(6), LayoutHelper.dp(16), LayoutHelper.dp(6))
            gravity = Gravity.CENTER

            val isSelected = (title == "Фото" && currentTab == "photo")
            background = GradientDrawable().apply {
                setColor(if (isSelected) DesignEngine.Theme.colorBlue else DesignEngine.Theme.colorDivider)
                cornerRadius = LayoutHelper.dp(16).toFloat()
            }
            setTextColor(if (isSelected) Color.WHITE else DesignEngine.Theme.colorTextSecondary)

            layoutParams = LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT).apply {
                marginEnd = LayoutHelper.dp(8)
            }

            setOnClickListener { updateTabs(title) }
        }
    }

    private fun updateTabs(selectedTitle: String) {
        currentTab = when {
            selectedTitle.startsWith("Фото") -> "photo"
            selectedTitle.startsWith("Видео") -> "video"
            selectedTitle.startsWith("Музыка") -> "music"
            selectedTitle.startsWith("Ссылки") -> "links"
            else -> "files"
        }

        for (i in 0 until mediaTabsContainer.childCount) {
            val child = mediaTabsContainer.getChildAt(i) as TextView
            val isSelected = (currentTab == "photo" && child.text.toString().startsWith("Фото")) ||
                    (currentTab == "links" && child.text.toString().startsWith("Ссылки")) ||
                    (child.text == selectedTitle)

            (child.background as GradientDrawable).setColor(if (isSelected) DesignEngine.Theme.colorBlue else DesignEngine.Theme.colorDivider)
            child.setTextColor(if (isSelected) Color.WHITE else DesignEngine.Theme.colorTextSecondary)
        }

        renderMediaContent()
    }

    private fun refreshTabTitles() {
        val info = chatInfo ?: return
        val titles = listOf(
            "Фото ${info.statistics.photos_count}",
            "Видео 0",
            "Музыка 0",
            "Ссылки ${info.statistics.links_messages}",
            "Файлы 0"
        )
        for (i in 0 until mediaTabsContainer.childCount) {
            (mediaTabsContainer.getChildAt(i) as TextView).text = titles[i]
        }
    }

    private fun renderMediaContent() {
        Log.d(TAG, "renderMediaContent: currentTab=$currentTab")
        mediaContentLayout.removeAllViews()
        val info = chatInfo
        if (info == null) {
            val tv = TextView(context).apply {
                text = "Загрузка..."
                gravity = Gravity.CENTER
                setTextColor(DesignEngine.Theme.colorTextSecondary)
            }
            mediaContentLayout.addView(tv, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 200, Gravity.CENTER))
            return
        }

        when(currentTab) {
            "photo" -> {
                Log.d(TAG, "renderMediaContent: photos size=${info.recent_photos.size}")
                if (info.recent_photos.isEmpty()) {
                    showEmptyContent()
                    return
                }
                val grid = RecyclerView(context).apply {
                    layoutManager = GridLayoutManager(context, 3)
                    adapter = PhotoGridAdapter(info.chat_id, info.recent_photos) { photo ->
                        openViewer("https://blagogram.ru/uploads/${info.chat_id}/${photo.filename}", nameView.text.toString(), photo.sent_at, null)
                    }
                }
                mediaContentLayout.addView(grid, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
            }
            "links" -> {
                Log.d(TAG, "renderMediaContent: links size=${info.recent_link_messages.size}")
                if (info.recent_link_messages.isEmpty()) {
                    showEmptyContent()
                    return
                }
                val list = RecyclerView(context).apply {
                    layoutManager = LinearLayoutManager(context)
                    adapter = LinksAdapter(info.recent_link_messages) { msg ->
                        openLink(msg.content_preview)
                    }
                }
                mediaContentLayout.addView(list, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
            }
            else -> showEmptyContent()
        }
    }

    private fun showEmptyContent() {
        val tv = TextView(context).apply {
            text = "Нет данных"
            gravity = Gravity.CENTER
            setTextColor(DesignEngine.Theme.colorTextSecondary)
        }
        mediaContentLayout.addView(tv, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 200, Gravity.CENTER))
    }

    private fun loadChatMediaInfo() {
        val token = context.getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        Log.d(TAG, "loadChatMediaInfo: token is ${if(token.isEmpty()) "empty" else "present"}")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val chats = repo.getChats(token)
                Log.d(TAG, "loadChatMediaInfo: found ${chats.size} chats")
                val chat = chats.find { it.user_id == targetUserId }
                if (chat != null) {
                    val chatId = chat.id
                    Log.d(TAG, "loadChatMediaInfo: found chat for targetUser, chatId=$chatId")

                    val cached = db.chatMediaDao().getChatMedia(chatId)
                    if (cached != null) {
                        Log.d(TAG, "loadChatMediaInfo: loading from cache")
                        chatInfo = gson.fromJson(cached.jsonResponse, ChatInfoResponse::class.java)
                        withContext(Dispatchers.Main) {
                            refreshTabTitles()
                            renderMediaContent()
                        }
                    }

                    Log.d(TAG, "loadChatMediaInfo: fetching fresh info from network")
                    val freshInfo = repo.getChatInfo(token, chatId)
                    Log.d(TAG, "loadChatMediaInfo: freshInfo received, photos=${freshInfo.recent_photos.size}")
                    chatInfo = freshInfo
                    db.chatMediaDao().insertChatMedia(ChatMediaCacheEntity(chatId, gson.toJson(freshInfo)))

                    withContext(Dispatchers.Main) {
                        refreshTabTitles()
                        renderMediaContent()
                    }
                } else {
                    Log.d(TAG, "loadChatMediaInfo: chat not found for targetUserId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadChatMediaInfo error", e)
            }
        }
    }

    private fun loadUserProfile(userId: Int) {
        val token = context.getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profile = repo.getUserProfile(token, userId)
                userProfile = profile
                withContext(Dispatchers.Main) {
                    nameView.text = profile.display_name ?: profile.username
                    statusView.text = profile.status ?: "Без статуса"
                    phoneItem.setData(profile.phone ?: "Скрыт", "Телефон")
                    usernameItem.setData("@${profile.username}", "Юзернейм")
                    avatarView.load("https://blagogram.ru/avatars/${profile.avatar}") {
                        crossfade(true)
                        placeholder(R.drawable.ic_user_placeholder)
                        error(R.drawable.ic_user_placeholder)
                        transformations(CircleCropTransformation())
                    }
                }
            } catch (e: Exception) { Log.e(TAG, "loadUserProfile error", e) }
        }
    }

    private fun loadUserProfileFromCache(userId: Int) {
        val prefs = context.getSharedPreferences("app_cache", MODE_PRIVATE)
        val json = prefs.getString("profile_$userId", null)
        if (!json.isNullOrEmpty()) {
            try {
                val profile = gson.fromJson(json, UserProfile::class.java)
                nameView.text = profile.display_name ?: profile.username
                statusView.text = profile.status ?: "Без статуса"
                phoneItem.setData(profile.phone ?: "Скрыт", "Телефон")
                usernameItem.setData("@${profile.username}", "Юзернейм")
                avatarView.load("https://blagogram.ru/avatars/${profile.avatar}") {
                    crossfade(true)
                    placeholder(R.drawable.ic_user_placeholder)
                    error(R.drawable.ic_user_placeholder)
                    transformations(CircleCropTransformation())
                }
            } catch (e: Exception) { Log.e(TAG, "loadUserProfileFromCache error", e) }
        }
    }

    private fun openChat() {
        //val intent = Intent(this, ChatActivity::class.java).apply {
        //    putExtra("user_id", targetUserId)
        //    putExtra("chat_name", nameView.text.toString())
        //    putExtra("chat_avatar", userProfile?.avatar)
        //}
        //startActivity(intent)
    }

    private fun makeCall() {
        val number = phoneItem.valueView.text.toString()
        if (number.isNotEmpty() && number != "Скрыт") {
            showCallConfirmationDialog(number)
        } else {
            Toast.makeText(context, "Номер телефона скрыт или недоступен", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCallConfirmationDialog(number: String) {
        val dialog = BottomSheetDialog(context)

        // Создаем контейнер программно в стиле твоего DesignEngine
        val contentView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(24), LayoutHelper.dp(32), LayoutHelper.dp(24), LayoutHelper.dp(32))
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorCardBackground)
                val r = LayoutHelper.dp(28).toFloat()
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f) // Скругляем только верх
            }

            // Иконка вызова
            val iconWrapper = FrameLayout(context)
            val icon = ImageView(context).apply {
                setImageResource(R.drawable.ic_mic) // Можно заменить на иконку трубки, если есть
                setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                setPadding(LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16))
                background = GradientDrawable().apply {
                    setColor(DesignEngine.Theme.colorBlue)
                    shape = GradientDrawable.OVAL
                }
            }
            iconWrapper.addView(icon, LayoutHelper.createFrame(64, 64, Gravity.CENTER))
            addView(iconWrapper, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 16))

            // Заголовок
            val titleView = TextView(context).apply {
                text = "Вызов через телефон"
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(DesignEngine.Theme.colorTextPrimary)
                gravity = Gravity.CENTER
            }
            addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 8))

            // Номер
            val subTitleView = TextView(context).apply {
                text = number
                textSize = 16f
                setTextColor(DesignEngine.Theme.colorBlue)
                gravity = Gravity.CENTER
            }
            addView(subTitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 16))

            // Текст предупреждения (то, что ты просил)
            val warningView = TextView(context).apply {
                text = "Внимание: это обычный телефонный звонок. Будут списываться минуты вашего тарифа, если у вас нет безлимитного пакета."
                textSize = 14f
                setTextColor(DesignEngine.Theme.colorTextSecondary)
                gravity = Gravity.CENTER
                setLineSpacing(LayoutHelper.dp(2).toFloat(), 1.1f)
            }
            addView(warningView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 24))

            // В файле ProfilePreviewActivity.kt меняем обработчик кнопки "Позвонить"
            // Кнопка "Позвонить"
            val callBtn = Button(context).apply {
                text = "Позвонить"
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(DesignEngine.Theme.colorBlue)
                    cornerRadius = LayoutHelper.dp(16).toFloat()
                }
                setOnClickListener {
                    dialog.dismiss()
                    // Просто кидаем в системный звонок
                    try {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Не удалось открыть звонилку", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            addView(callBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 54))

            // Кнопка "Отмена"
            val cancelBtn = TextView(context).apply {
                text = "Отмена"
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(DesignEngine.Theme.colorTextSecondary)
                gravity = Gravity.CENTER
                setPadding(0, LayoutHelper.dp(16), 0, 0)
                setOnClickListener { dialog.dismiss() }
            }
            addView(cancelBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        }

        dialog.setContentView(contentView)
        dialog.show()
    }

    private fun openFullProfile() {
        presentFragment(ProfileScreen(context, targetUserId))
    }

    private fun openViewer(url: String, senderName: String, sentTime: String, caption: String?) {
        //startActivity(Intent(this, PhotoViewerActivity::class.java).apply {
        //    putExtra("image_url", url)
        //    putExtra("sender_name", senderName)
        //    putExtra("sent_time", sentTime)
        //    putExtra("caption", caption)
        //})
    }

    private fun openLink(content: String) {
        val urlRegex = "(https?://[\\w-]+(\\.[\\w-]+)+(/\\S*)?)".toRegex()
        val match = urlRegex.find(content)
        match?.let {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.value))
            context.startActivity(intent)
        }
    }

    // --- Adapters ---

    inner class PhotoGridAdapter(private val chatId: Int, private val photos: List<RecentPhoto>, private val onClick: (RecentPhoto) -> Unit) : RecyclerView.Adapter<PhotoGridAdapter.PhotoVH>() {
        inner class PhotoVH(val iv: ImageView) : RecyclerView.ViewHolder(iv)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoVH {
            val iv = ImageView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutHelper.dp(110))
                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(LayoutHelper.dp(2), LayoutHelper.dp(2), LayoutHelper.dp(2), LayoutHelper.dp(2))
            }
            return PhotoVH(iv)
        }
        override fun onBindViewHolder(holder: PhotoVH, position: Int) {
            val photo = photos[position]
            holder.iv.load("https://blagogram.ru/uploads/$chatId/${photo.filename}")
            holder.iv.setOnClickListener { onClick(photo) }
        }
        override fun getItemCount() = photos.size
    }

    inner class LinksAdapter(private val links: List<RecentLinkMessage>, private val onClick: (RecentLinkMessage) -> Unit) : RecyclerView.Adapter<LinksAdapter.LinkVH>() {
        inner class LinkVH(val view: LinkItemView) : RecyclerView.ViewHolder(view)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LinkVH = LinkVH(LinkItemView(parent.context))
        override fun onBindViewHolder(holder: LinkVH, position: Int) {
            holder.view.setData(links[position])
            holder.view.setOnClickListener { onClick(links[position]) }
        }
        override fun getItemCount() = links.size
    }

    inner class LinkItemView(context: Context) : LinearLayout(context) {
        private val nameView: TextView
        private val timeView: TextView
        private val contentView: TextView
        private val avatarView: ShapeableImageView
        init {
            orientation = HORIZONTAL
            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
            avatarView = ShapeableImageView(context).apply {
                shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(LayoutHelper.dp(20).toFloat()).build()
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            addView(avatarView, LayoutHelper.createLinear(40, 40, 0f, Gravity.TOP, 0, 0, 12, 0))
            val rightLayout = LinearLayout(context).apply { orientation = VERTICAL }
            nameView = TextView(context).apply {
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(DesignEngine.Theme.colorTextPrimary)
            }
            timeView = TextView(context).apply {
                textSize = 11f
                setTextColor(DesignEngine.Theme.colorTextSecondary)
            }
            contentView = TextView(context).apply {
                textSize = 13f
                setTextColor(DesignEngine.Theme.colorTextPrimary)
                maxLines = 3
                ellipsize = TextUtils.TruncateAt.END
            }
            rightLayout.addView(nameView)
            rightLayout.addView(timeView)
            rightLayout.addView(contentView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 4, 0, 0))
            addView(rightLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

            val outValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            foreground = ContextCompat.getDrawable(context, outValue.resourceId)
            isClickable = true
        }
        fun setData(msg: RecentLinkMessage) {
            nameView.text = msg.sender_name
            timeView.text = msg.sent_at
            contentView.text = msg.content_preview
            avatarView.load("https://blagogram.ru/avatars/${msg.sender_avatar}") {
                placeholder(R.drawable.ic_user_placeholder)
                error(R.drawable.ic_user_placeholder)
                transformations(CircleCropTransformation())
            }
        }
    }

    inner class ProfileInfoItem(context: Context) : LinearLayout(context) {
        val valueView: TextView
        val labelView: TextView
        init {
            orientation = VERTICAL
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(12), LayoutHelper.dp(16), LayoutHelper.dp(12))
            valueView = TextView(context).apply {
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(DesignEngine.Theme.colorTextPrimary)
            }
            addView(valueView)
            labelView = TextView(context).apply {
                textSize = 12f
                setTextColor(DesignEngine.Theme.colorTextSecondary)
            }
            addView(labelView)
        }
        fun setData(value: String, label: String) {
            valueView.text = value
            labelView.text = label
        }
    }

    inner class ProfileActionButton(context: Context) : LinearLayout(context) {
        private val iconView: ImageView
        private val textView: TextView
        init {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, LayoutHelper.dp(12), 0, LayoutHelper.dp(12))
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorCardBackground)
                cornerRadius = LayoutHelper.dp(12).toFloat()
            }
            iconView = ImageView(context).apply { setColorFilter(DesignEngine.Theme.colorIconPrimary, PorterDuff.Mode.SRC_IN) }
            addView(iconView, LayoutHelper.createLinear(24, 24, 0f, Gravity.CENTER, 0, 0, 0, 4))
            textView = TextView(context).apply {
                textSize = 10f
                setTextColor(DesignEngine.Theme.colorTextPrimary)
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }
            addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
            val outValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            foreground = ContextCompat.getDrawable(context, outValue.resourceId)
            isClickable = true
        }
        fun setData(text: String, iconRes: Int) {
            textView.text = text
            iconView.setImageResource(iconRes)
        }
    }
}
