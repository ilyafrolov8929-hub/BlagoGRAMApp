package com.kia.blagogramapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.kia.blagogramapp.adapters.ChatsAdapter
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.model.Chat
import com.kia.blagogramapp.models.ChatItem
import com.kia.blagogramapp.network.RetrofitClient
import com.kia.blagogramapp.ui.modules.*
import com.kia.blagogramapp.utils.DevModeManager
import com.kia.blagogramapp.utils.MusicManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine

class ChatsActivity : AppCompatActivity() {

    private lateinit var layout: ChatListLayout
    private var sideMenu: SideMenuModule? = null
    private var repo = ApiRepository(RetrofitClient.api)

    private var isCurrentThemeDark: Boolean = false
    private var isCurrentUseMonet: Boolean = false
    private var isDrawerOpen = false

    private var pollingJob: Job? = null
    private var fullPlayer: FullPlayer? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Уведомления отключены. Вы можете пропустить важные сообщения.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkNotificationPermission()

        DesignEngine.initializeScreen(this)

        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        isCurrentThemeDark = themePrefs.getBoolean("is_dark_theme", false)
        isCurrentUseMonet = themePrefs.getBoolean("use_monet", false)

        layout = ChatListLayout(this)
        setContentView(layout)

        if (DesignEngine.useBottomNavigation) {
            BottomNavigationModule(this).create(layout, BottomNavigationModule.Screen.CHATS)
            layout.recycler.setPadding(0, layout.recycler.paddingTop, 0, LayoutHelper.dp(100))
        } else {
            sideMenu = SideMenuModule(this).create(layout, getStatusBarHeight())
            setupSideMenu()
        }

        observeMusic()

        val toolbarLeftIcon = if (DesignEngine.useBottomNavigation) R.drawable.ic_user else R.drawable.ic_menu
        val toolbar = DesignEngine.createToolBar(
            context = this,
            title = "Lume",
            leftButtonIconRes = toolbarLeftIcon,
            rightButtonIconRes = R.drawable.ic_search,
            isDark = isCurrentThemeDark,
            leftButtonClickListener = { 
                if (DesignEngine.useBottomNavigation) {
                    startActivity(Intent(this, ProfileActivity::class.java))
                } else {
                    toggleDrawer()
                }
            },
            rightButtonClickListener = { startActivity(Intent(this, SearchUserActivity::class.java)) }
        )
        layout.addView(toolbar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        layout.swipeRefresh.setOnRefreshListener { loadChats() }

        layout.swipeRefresh.post {
            layout.swipeRefresh.isRefreshing = true
            loadChats()
        }

        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val chatId = intent?.getIntExtra("open_chat_id", 0) ?: 0
        if (chatId != 0) {
            val name = intent?.getStringExtra("open_chat_name")
            val avatar = intent?.getStringExtra("open_chat_avatar")
            val userId = intent?.getIntExtra("open_chat_user_id", 0) ?: 0
            val isPremium = intent?.getBooleanExtra("open_chat_is_premium", false) ?: false

            val chatIntent = Intent(this, ChatActivity::class.java).apply {
                putExtra("chat_id", chatId)
                putExtra("chat_name", name)
                putExtra("chat_avatar", avatar)
                putExtra("user_id", userId)
                putExtra("is_premium", isPremium)
            }
            startActivity(chatIntent)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupSideMenu() {
        val sm = sideMenu ?: return
        if (DevModeManager.isEnabled(this)) {
            val mockProfile = DevModeManager.getMockUserProfile()
            sm.updateHeader(
                mockProfile.display_name ?: "Разработчик",
                mockProfile.username,
                mockProfile.status ?: "В локальном режиме",
                ""
            )
        } else {
            val prefs = getSharedPreferences("app", MODE_PRIVATE)
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
            startActivity(Intent(this, ProfileActivity::class.java))
            toggleDrawer()
        }

        sm.addItem("AI Помошник", R.drawable.ic_ai) {
            startActivity(Intent(this, AiChatActivity::class.java))
            toggleDrawer()
        }

        sm.addItem("Избранное", R.drawable.ic_saved_messages) {
            val myId = if (DevModeManager.isEnabled(this)) 1 else getSharedPreferences("app", MODE_PRIVATE).getInt("user_id", 0)

            val savedChat = (layout.recycler.adapter as? ChatsAdapter)?.items?.filterIsInstance<ChatItem>()?.find {
                it.user_id == myId
            }

            if (savedChat != null) {
                val i = Intent(this, ChatActivity::class.java)
                i.putExtra("chat_id", savedChat.chat_id)
                i.putExtra("chat_name", "Избранное")
                i.putExtra("chat_avatar", "action_saved")
                i.putExtra("user_id", myId)
                i.putExtra("is_premium", savedChat.is_premium)
                startActivity(i)
            } else {
                Toast.makeText(this, "Напишите самому себе, чтобы активировать Избранное", Toast.LENGTH_SHORT).show()
            }
            toggleDrawer()
        }

        sm.addItem("Музыка", R.drawable.ic_music) {
            startActivity(Intent(this, MusicActivity::class.java))
            toggleDrawer()
        }

        sm.addItem("Настройки", R.drawable.ic_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            toggleDrawer()
        }

        sm.addSpacer()
        sm.addDivider()

        sm.addItem("О приложении", R.drawable.ic_more) {
            startActivity(Intent(this, AboutActivity::class.java))
            toggleDrawer()
        }

        sm.setPaddingBottom(20)

        sm.drawerOverlay.setOnClickListener { if (isDrawerOpen) toggleDrawer() }
    }

    private fun observeMusic() {
        lifecycleScope.launchWhenStarted {
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
            fullPlayer = FullPlayer(this).apply {
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

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            val token = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
            while (isActive) {
                try {
                    val chats: List<Chat> = repo.getChats(token)
                    runOnUiThread {
                        updateAdapter(chats)
                    }
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
        val myId = if (DevModeManager.isEnabled(this)) 1 else getSharedPreferences("app", MODE_PRIVATE).getInt("user_id", 0)
        
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
                    startActivity(Intent(this, AiChatActivity::class.java))
                    return@ChatsAdapter
                }
                val i = Intent(this, ChatActivity::class.java)
                i.putExtra("chat_id", chat.chat_id)
                i.putExtra("chat_name", chat.name)
                i.putExtra("chat_avatar", chat.avatar)
                i.putExtra("user_id", chat.user_id)
                i.putExtra("is_premium", chat.is_premium)
                startActivity(i)
            },
            onLongClick = { chat ->
                if (chat.chat_id != -100) showChatMenu(chat)
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
            themePrefs.getBoolean("use_monet", false) != isCurrentUseMonet ||
            themePrefs.getBoolean("use_bottom_navigation", false) != DesignEngine.useBottomNavigation) {
            recreate()
            return
        }

        startPolling()
        sideMenu?.premiumBadge?.let { DesignEngine.setPremiumStatus(it, SubscriptionManager.isPremium(this)) }
    }

    override fun onPause() {
        super.onPause()
        stopPolling()
    }

    override fun onBackPressed() {
        if (fullPlayer?.parent != null) {
            closeFullPlayer()
            return
        }
        if (isDrawerOpen) {
            toggleDrawer()
            return
        }
        super.onBackPressed()
    }
}
