package com.kia.blagogramapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.google.gson.Gson
import com.kia.blagogramapp.adapters.ChatsAdapter
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.model.Chat
import com.kia.blagogramapp.models.ChatItem
import com.kia.blagogramapp.network.RetrofitClient
import com.kia.blagogramapp.ui.modules.ChatListLayout
import com.kia.blagogramapp.ui.modules.MessageOptionsSheet
import com.kia.blagogramapp.ui.modules.SideMenuModule
import com.kia.blagogramapp.utils.DevModeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatsActivity : AppCompatActivity() {

    private lateinit var layout: ChatListLayout
    private lateinit var sideMenu: SideMenuModule
    private var repo = ApiRepository(RetrofitClient.api)

    private var isCurrentThemeDark: Boolean = false
    private var isCurrentUseMonet: Boolean = false
    private var isDrawerOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Инициализация тем
        DesignEngine.init(this)
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        val isDarkTheme = themePrefs.getBoolean("is_dark_theme", false)
        val useMonet = themePrefs.getBoolean("use_monet", false)
        
        isCurrentThemeDark = isDarkTheme
        isCurrentUseMonet = useMonet
        DesignEngine.Theme.apply(isDarkTheme, useMonet)

        // 2. Настройка окна
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !isDarkTheme
        controller.isAppearanceLightNavigationBars = !isDarkTheme

        // 3. UI
        layout = ChatListLayout(this)
        setContentView(layout)

        // 4. Боковое меню
        sideMenu = SideMenuModule(this).create(layout, getStatusBarHeight())
        setupSideMenu()
        sideMenu.premiumBadge.visibility = if (SubscriptionManager.isPremium(this)) View.VISIBLE else View.GONE

        // 5. Обработчики
        layout.menuBtn.setOnClickListener { toggleDrawer() }
        layout.editBtn.setOnClickListener {
            startActivity(Intent(this, SearchUserActivity::class.java))
        }
        layout.swipeRefresh.setOnRefreshListener { loadChats() }

        // Показываем анимацию загрузки сразу при входе
        layout.swipeRefresh.post {
            layout.swipeRefresh.isRefreshing = true
            loadChats()
        }
    }

    private fun setupSideMenu() {
        if (DevModeManager.isEnabled(this)) {
            val mockProfile = DevModeManager.getMockUserProfile()
            sideMenu.updateHeader(
                mockProfile.display_name ?: "Разработчик",
                mockProfile.username,
                mockProfile.status ?: "В локальном режиме",
                ""
            )
        } else {
            val prefs = getSharedPreferences("app", MODE_PRIVATE)
            sideMenu.updateHeader(
                prefs.getString("display_name", "Вы") ?: "Вы",
                prefs.getString("login", "username") ?: "username",
                prefs.getString("status", "Статус") ?: "Нет статуса",
                prefs.getString("avatar", "") ?: ""
            )
        }

        sideMenu.addItem("Профиль", R.drawable.ic_user) {
            startActivity(Intent(this, ProfileActivity::class.java))
            toggleDrawer()
        }

        sideMenu.addItem("AI Помошник", R.drawable.ic_ai) {
            startActivity(Intent(this, AiChatActivity::class.java))
            toggleDrawer()
        }

        sideMenu.addItem("Избранное", R.drawable.ic_saved_messages) {
            val myId = if (DevModeManager.isEnabled(this)) 1 else getSharedPreferences("app", MODE_PRIVATE).getInt("user_id", 0)

            val savedChat = (layout.recycler.adapter as? ChatsAdapter)?.items?.find {
                it is ChatItem && it.user_id == myId
            } as? ChatItem

            if (savedChat != null) {
                val i = Intent(this, ChatActivity::class.java)
                i.putExtra("chat_id", savedChat.chat_id)
                i.putExtra("chat_name", "Избранное")
                i.putExtra("chat_avatar", "action_saved")
                i.putExtra("target_user_id", myId)
                startActivity(i)
            } else {
                Toast.makeText(this, "Напишите самому себе, чтобы активировать Избранное", Toast.LENGTH_SHORT).show()
            }
            toggleDrawer()
        }

        sideMenu.addItem("Музыка", R.drawable.ic_music) {
            startActivity(Intent(this, MusicActivity::class.java))
            toggleDrawer()
        }

        sideMenu.addItem("Настройки", R.drawable.ic_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            toggleDrawer()
        }

        sideMenu.addSpacer()
        sideMenu.addDivider()

        sideMenu.addItem("О приложении", R.drawable.ic_more) {
            startActivity(Intent(this, AboutActivity::class.java))
            toggleDrawer()
        }
        
        sideMenu.setPaddingBottom(20)
        
        sideMenu.drawerOverlay.setOnClickListener { if (isDrawerOpen) toggleDrawer() }
    }

    private fun toggleDrawer() {
        if (isDrawerOpen) {
            sideMenu.drawerPanel.animate().translationX(-sideMenu.drawerPanel.width.toFloat())
                .setInterpolator(DecelerateInterpolator())
                .setDuration(300).start()
            sideMenu.drawerOverlay.animate().alpha(0f).setDuration(300)
                .withEndAction { sideMenu.drawerOverlay.visibility = View.GONE }.start()
        } else {
            sideMenu.drawerOverlay.visibility = View.VISIBLE
            sideMenu.drawerOverlay.animate().alpha(1f).setDuration(300).start()
            sideMenu.drawerPanel.animate().translationX(0f)
                .setInterpolator(DecelerateInterpolator())
                .setDuration(300).start()
        }
        isDrawerOpen = !isDrawerOpen
    }

    private fun loadChats() {
        val token = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val chats: List<Chat> = repo.getChats(token)
                val json = Gson().toJson(chats)
                getSharedPreferences("app_cache", MODE_PRIVATE).edit().putString("chats_list", json).apply()
                runOnUiThread {
                    layout.swipeRefresh.isRefreshing = false
                    updateAdapter(chats)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    layout.swipeRefresh.isRefreshing = false
                    e.printStackTrace()
                }
            }
        }
    }

    private fun updateAdapter(chats: List<Chat>) {
        val myId = if (DevModeManager.isEnabled(this)) 1 else getSharedPreferences("app", MODE_PRIVATE).getInt("user_id", 0)
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
                pinned = c.pinned
            )
        }
        layout.recycler.adapter = ChatsAdapter(
            items = items,
            onClick = { chat ->
                val i = Intent(this, ChatActivity::class.java)
                i.putExtra("chat_id", chat.chat_id)
                i.putExtra("chat_name", chat.name)
                i.putExtra("chat_avatar", chat.avatar)
                i.putExtra("target_user_id", chat.user_id)
                startActivity(i)
            },
            onLongClick = { chat ->
                showChatMenu(chat)
            }
        )
    }

    private fun showChatMenu(chat: ChatItem) {
        val token = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        MessageOptionsSheet.showForChat(
            context = this,
            pinned = chat.pinned,
            onPinToggle = {
                if (!chat.pinned) {
                    val adapter = layout.recycler.adapter as? ChatsAdapter
                    val pinnedCount = adapter?.items?.filterIsInstance<ChatItem>()?.count { it.pinned } ?: 0
                    if (pinnedCount >= 5) {
                        Toast.makeText(this, "Максимально можно закрепить 5 человек", Toast.LENGTH_SHORT).show()
                        return@showForChat
                    }
                }

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val success = if (chat.pinned) {
                            repo.unpinChat(token, chat.chat_id)
                        } else {
                            repo.pinChat(token, chat.chat_id)
                        }
                        if (success) {
                            runOnUiThread {
                                val msg = if (chat.pinned) "Чат откреплен" else "Чат закреплен"
                                Toast.makeText(this@ChatsActivity, msg, Toast.LENGTH_SHORT).show()
                                loadChats()
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@ChatsActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            onDelete = {
                MessageOptionsSheet.showDeleteConfirm(this, chat.name, chat.avatar) { forBoth ->
                    val mode = if (forBoth) "for_everyone" else "for_me"
                    
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val success = repo.deleteChat(token, chat.chat_id, mode)
                            if (success) {
                                runOnUiThread {
                                    Toast.makeText(this@ChatsActivity, "Чат удален", Toast.LENGTH_SHORT).show()
                                    loadChats() 
                                }
                            }
                        } catch (e: Exception) {
                            runOnUiThread {
                                Toast.makeText(this@ChatsActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        )
    }

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) result = resources.getDimensionPixelSize(resourceId)
        return result
    }

    override fun onResume() {
        super.onResume()
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        if (themePrefs.getBoolean("is_dark_theme", false) != isCurrentThemeDark || 
            themePrefs.getBoolean("use_monet", false) != isCurrentUseMonet) {
            recreate()
            return
        }
        layout.swipeRefresh.post {
            layout.swipeRefresh.isRefreshing = true
            loadChats()
        }
        sideMenu.premiumBadge.visibility = if (SubscriptionManager.isPremium(this)) View.VISIBLE else View.GONE
    }
}
