package com.kia.blagogramapp.ui.screens

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kia.blagogramapp.AboutActivity
import com.kia.blagogramapp.AiChatActivity
import com.kia.blagogramapp.BaseScreen
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.database.AppDatabase
import com.kia.blagogramapp.network.RetrofitClient
import com.kia.blagogramapp.ui.modules.ChatListLayout
import com.kia.blagogramapp.ui.modules.SideMenuModule
import com.kia.blagogramapp.DesignEngine
import com.kia.blagogramapp.LayoutHelper
import com.kia.blagogramapp.MusicActivity
import com.kia.blagogramapp.MusicScreen
import com.kia.blagogramapp.NewsActivity
import com.kia.blagogramapp.ProfileActivity
import com.kia.blagogramapp.R
import com.kia.blagogramapp.SettingsActivity
import com.kia.blagogramapp.adapters.ChatsAdapter
import com.kia.blagogramapp.database.ChatEntity
import com.kia.blagogramapp.model.Chat
import com.kia.blagogramapp.models.ChatItem
import com.kia.blagogramapp.ui.modules.BottomNavigationModule
import com.kia.blagogramapp.ui.modules.FullPlayer
import com.kia.blagogramapp.ui.modules.MessageOptionsSheet
import com.kia.blagogramapp.utils.DevModeManager
import com.kia.blagogramapp.utils.MusicManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatsScreen(context: Context) : BaseScreen(context) {
    val activity = context as? AppCompatActivity

    private lateinit var layout: ChatListLayout
    private var sideMenu: SideMenuModule? = null
    private var isCurrentThemeDark: Boolean = false
    private var isDrawerOpen = false
    private var isCurrentUseMonet: Boolean = false
    private var repo = ApiRepository(RetrofitClient.api)
    private var pollingJob: Job? = null
    private var fullPlayer: FullPlayer? = null

    // ИСПРАВЛЕНИЕ: Используем 'context' вместо 'this'
    private val db by lazy { AppDatabase.getDatabase(context) }

    override fun createView(): View {
        layout = ChatListLayout(context)

        val themePrefs = context.getSharedPreferences("app_theme", MODE_PRIVATE)
        isCurrentThemeDark = themePrefs.getBoolean("is_dark_theme", false)
        isCurrentUseMonet = themePrefs.getBoolean("use_monet", false)

        if (DesignEngine.useBottomNavigation) {
            BottomNavigationModule(context).create(layout, BottomNavigationModule.Screen.CHATS)
            layout.recycler.setPadding(0, layout.recycler.paddingTop, 0, LayoutHelper.dp(100))
        } else {
            sideMenu = SideMenuModule(context).create(layout, getStatusBarHeight())
            setupSideMenu()
        }

        // Добавляем тулбар (как было в ChatsActivity)
        val toolbar = DesignEngine.createToolBar(
            context = context,
            title = "Lume",
            leftButtonIconRes = R.drawable.ic_menu,
            rightButtonIconRes = R.drawable.ic_search,
            isDark = isCurrentThemeDark,
            leftButtonClickListener = {
                if (DesignEngine.useBottomNavigation) {
                presentFragment(ProfileScreen(context))
                } else {
                    toggleDrawer()
                } },
            rightButtonClickListener = {
                presentFragment(SearchScreen(context))
            }
        )

        layout.addView(toolbar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        layout.swipeRefresh.setOnRefreshListener { loadChats() }

        observeMusic()

        // Сначала загружаем кэш
        loadCachedChats()

        layout.swipeRefresh.post {
            layout.swipeRefresh.isRefreshing = true
            loadChats()
        }

        return layout
    }

    private fun loadCachedChats() {
        activity?.lifecycleScope?.launch {
            val cached = withContext(Dispatchers.IO) { db.chatDao().getAll() }
            if (cached.isNotEmpty()) {
                val chats = cached.map {
                    Chat(it.id, it.name, it.avatar, it.lastMsg, it.lastTime, it.unread, it.lastSenderId, it.userId, it.pinned, it.isPremium)
                }
               updateAdapter(chats)
            }
        }
    }

    private fun loadChats() {
        val token = context.getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        activity?.lifecycleScope?.launch {
            try {
                val chats: List<Chat> = withContext(Dispatchers.IO) { repo.getChats(token) }

                // Кэшируем в БД
                withContext(Dispatchers.IO) {
                    db.chatDao().deleteAll()
                    db.chatDao().insertAll(chats.map {
                        ChatEntity(it.id, it.name, it.avatar, it.last_msg, it.last_time, it.unread, it.last_sender_id, it.user_id, it.pinned, it.is_premium)
                    })
                }

                layout.swipeRefresh.isRefreshing = false
                updateAdapter(chats)
            } catch (e: Exception) {
                layout.swipeRefresh.isRefreshing = false
                e.printStackTrace()
            }
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = activity?.lifecycleScope?.launch {
            val token = context.getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
            while (isActive) {
                try {
                    val chats: List<Chat> = withContext(Dispatchers.IO) { repo.getChats(token) }
                    updateAdapter(chats)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(5000)
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
    }

    private fun updateAdapter(chats: List<Chat>) {
        val myId = if (DevModeManager.isEnabled(context)) 1 else context.getSharedPreferences("app", MODE_PRIVATE).getInt("user_id", 0)

        // Мапим чаты из API
        val items = chats.map { c ->
            val isSavedMessages = c.user_id == myId
            val prefix = if (c.last_sender_id == myId && !isSavedMessages) "Вы: " else ""

            ChatItem(
                chat_id = c.id,
                user_id = c.user_id ?: 0,
                name = if (isSavedMessages) "Избранное" else (c.name ?: "").ifBlank { "Неизвестно" },
                avatar = if (isSavedMessages) "action_saved" else c.avatar,
                last_message = prefix + (c.last_msg ?: ""),
                last_time = c.last_time,
                unread = c.unread,
                pinned = c.pinned,
                is_premium = c.is_premium
            )
        }.toMutableList()

        // Добавляем AI Помощника в начало списка (если его нет)
        if (items.none { it.chat_id == -100 }) {
            items.add(0, ChatItem(
                chat_id = -100,
                user_id = -2,
                name = "Lume AI",
                avatar = "ai_bot", // Специальный тег для аватара
                last_message = "Ваш персональный помощник",
                last_time = "",
                unread = 0,
                pinned = true,
                is_premium = true
            ))
        }

        layout.recycler.adapter = ChatsAdapter(
            items = items,
            onClick = { chat ->
                if (chat.chat_id == -100) {
                    //(Intent(this, AiChatActivity::class.java))
                    return@ChatsAdapter
                }
                presentFragment(ChatScreen(context, chat.chat_id, chat.user_id, chat.name, chat.avatar.toString(), chat.is_premium))
            },
            onLongClick = { chat ->
                if (chat.chat_id != -100) showChatMenu(chat)
            }
        )
    }

    private fun showChatMenu(chat: ChatItem) {
        val token = context.getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        MessageOptionsSheet.showForChat(
            context = context,
            pinned = chat.pinned,
            onPinToggle = {
                if (!chat.pinned) {
                    val adapter = layout.recycler.adapter as? ChatsAdapter
                    val pinnedCount = adapter?.items?.filterIsInstance<ChatItem>()?.count { it.pinned } ?: 0
                    if (pinnedCount >= 5) {
                        Toast.makeText(context, "Максимально можно закрепить 5 человек", Toast.LENGTH_SHORT).show()
                        return@showForChat
                    }
                }

                activity?.lifecycleScope?.launch {
                    try {
                        val success = if (chat.pinned) {
                            withContext(Dispatchers.IO) { repo.unpinChat(token, chat.chat_id) }
                        } else {
                            withContext(Dispatchers.IO) { repo.pinChat(token, chat.chat_id) }
                        }
                        if (success) {
                            val msg = if (chat.pinned) "Чат откреплен" else "Чат закреплен"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            loadChats()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onDelete = {
                MessageOptionsSheet.showDeleteConfirm(context, chat.name, chat.avatar) { forBoth ->
                    val mode = if (forBoth) "for_everyone" else "for_me"

                    activity?.lifecycleScope?.launch {
                        try {
                            val success = withContext(Dispatchers.IO) { repo.deleteChat(token, chat.chat_id, mode) }
                            if (success) {
                                Toast.makeText(context, "Чат удален", Toast.LENGTH_SHORT).show()
                                loadChats()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        )
    }

    private fun setupSideMenu() {
        val sm = sideMenu ?: return
        if (DevModeManager.isEnabled(context)) {
            val mockProfile = DevModeManager.getMockUserProfile()
            sm.updateHeader(
                mockProfile.display_name ?: "Разработчик",
                mockProfile.username,
                mockProfile.status ?: "В локальном режиме",
                ""
            )
        } else {
            val prefs = context.getSharedPreferences("app", MODE_PRIVATE)
            sm.updateHeader(
                prefs.getString("display_name", "Вы") ?: "Вы",
                prefs.getString("login", "username") ?: "username",
                prefs.getString("status", "Статус") ?: "Нет статуса",
                prefs.getString("avatar", "") ?: ""
            )
        }

        sm.onMusicClick = {
            showFullPlayer()
            toggleDrawer()
        }

        sm.addItem("Профиль", R.drawable.ic_user) {
            presentFragment(ProfileScreen(context))
            toggleDrawer()
        }

        sm.addItem("AI Помошник", R.drawable.ic_ai) {
            //startActivity(Intent(this, AiChatActivity::class.java))
            toggleDrawer()
        }

        sm.addItem("Избранное", R.drawable.ic_saved_messages) {
            val myId = if (DevModeManager.isEnabled(context)) 1 else context.getSharedPreferences("app", MODE_PRIVATE).getInt("user_id", 0)

            val savedChat = (layout.recycler.adapter as? ChatsAdapter)?.items?.filterIsInstance<ChatItem>()?.find {
                it.user_id == myId
            }

            if (savedChat != null) {
                presentFragment(ChatScreen(context, savedChat.chat_id, myId, "Избранное", "action_saved", savedChat.is_premium))

            } else {
                Toast.makeText(context, "Напишите самому себе, чтобы активировать Избранное", Toast.LENGTH_SHORT).show()
            }
            toggleDrawer()
        }

        sm.addItem("Музыка", R.drawable.ic_music) {
            presentFragment(MusicScreen(context))
            toggleDrawer()
        }

        sm.addItem("Меню", R.drawable.ic_menu) {
            //startActivity(Intent(this, NewsActivity::class.java))
            toggleDrawer()
        }


        sm.addItem("Настройки", R.drawable.ic_settings) {
            presentFragment(SettingsScreen(context))
            toggleDrawer()
        }

        sm.addSpacer()
        sm.addDivider()

        sm.addItem("О приложении", R.drawable.ic_more) {
            presentFragment(AboutScreen(context))
            toggleDrawer()
        }

        sm.setPaddingBottom(20)

        sm.drawerOverlay.setOnClickListener { if (isDrawerOpen) toggleDrawer() }
    }

    private fun toggleDrawer() {
        val sm = sideMenu ?: return
        if (isDrawerOpen) {
            sm.drawerPanel.animate().translationX(-sm.drawerPanel.width.toFloat())
                .setInterpolator(DecelerateInterpolator())
                .setDuration(300).start()
            sm.drawerOverlay.animate().alpha(0f).setDuration(300)
                .withEndAction { sm.drawerOverlay.visibility = View.GONE }.start()
        } else {
            sm.drawerOverlay.visibility = View.VISIBLE
            sm.drawerOverlay.animate().alpha(1f).setDuration(300).start()
            sm.drawerPanel.animate().translationX(0f)
                .setInterpolator(DecelerateInterpolator())
                .setDuration(300).start()
        }
        isDrawerOpen = !isDrawerOpen
    }

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) result = context.resources.getDimensionPixelSize(resourceId)
        return result
    }

    private fun observeMusic() {
        activity?.lifecycleScope?.launchWhenStarted {
            combine(MusicManager.currentTrack, MusicManager.isPlaying) { track, playing ->
                track to playing
            }.collect { (track, playing) ->
                sideMenu?.updateMusic(track, playing)
                if (track != null) {
                    fullPlayer?.bind(track, playing)
                }
            }
        }
    }

    private fun showFullPlayer() {
        val track = MusicManager.currentTrack.value ?: return
        if (fullPlayer == null) {
            fullPlayer = FullPlayer(context).apply {
                layoutParams = FrameLayout.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT)
                onCloseListener = { closeFullPlayer() }
            }
        }
        if (fullPlayer?.parent == null) {
            layout.addView(fullPlayer)
            val anchor = sideMenu?.miniPlayerContainer ?: layout
            fullPlayer?.animateOpen(anchor)
        }
        fullPlayer?.bind(track, MusicManager.isPlaying.value)
    }

    private fun closeFullPlayer() {
        val anchor = sideMenu?.miniPlayerContainer ?: layout
        fullPlayer?.animateClose(anchor) {
            layout.removeView(fullPlayer)
        }
    }
}