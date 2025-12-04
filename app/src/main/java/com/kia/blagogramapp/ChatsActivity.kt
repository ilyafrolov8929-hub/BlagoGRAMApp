package com.kia.blagogramapp

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kia.blagogramapp.adapters.ChatsAdapter
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.model.Chat
import com.kia.blagogramapp.models.ChatItem
import com.kia.blagogramapp.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class VerticalSpaceDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: android.graphics.Rect,
        view: android.view.View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.bottom = space
    }
}


class ChatsActivity : AppCompatActivity() {

    private lateinit var logo: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var bottomNav: BottomNavigationView
    private var repo = ApiRepository(RetrofitClient.api)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // делать обложку под статусбар
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = Color.TRANSPARENT
        setContentView(R.layout.activity_chats)

        val swipe = findViewById<SwipeRefreshLayout>(R.id.swipeChats)

        swipe.setOnRefreshListener {
            loadChats()
        }

        logo = findViewById(R.id.logo)
        bottomNav = findViewById(R.id.bottomNav)

        applyThemeAuto()

        recycler = findViewById(R.id.recyclerChats)
        recycler.layoutManager = LinearLayoutManager(this)

        // ОТСТУП МЕЖДУ ЭЛЕМЕНТАМИ (важно!)
        recycler.addItemDecoration(VerticalSpaceDecoration(18))

        logo.text = "Чаты"

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chats -> {
                    true
                }
                R.id.nav_search -> {
                    startActivity(Intent(this, SearchUserActivity::class.java))
                    false
                }
                R.id.nav_profile -> {
                    val myId = getSharedPreferences("app", MODE_PRIVATE).getInt("user_id", 0)
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("user_id", myId)
                    startActivity(intent)
                    false
                }
                else -> false
            }
        }
        
        // Сначала пытаемся загрузить из кэша
        loadChatsFromCache()
        // Потом грузим с сервера
        loadChats()
    }

    private fun setStatusBarIconsDark(isDarkIcons: Boolean) {
        val window = window

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            if (controller != null) {
                if (isDarkIcons) {
                    controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else {
                    controller.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                }
            }
        } else {
            @Suppress("DEPRECATION")
            var flags = window.decorView.systemUiVisibility
            flags = if (isDarkIcons) {
                flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
            window.decorView.systemUiVisibility = flags
        }
    }


    override fun onResume() {
        super.onResume()
        bottomNav.selectedItemId = R.id.nav_chats
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyThemeAuto()
    }

    private fun applyThemeAuto() {
        val isDark = (resources.configuration.uiMode
                and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        if (isDark) {
            // ТЁМНАЯ ТЕМА → светлые элементы
            logo.setTextColor(Color.WHITE)
            setStatusBarIconsDark(false)  // ← ИКОНКИ СТАТУСБАРА СВЕТЛЫЕ

        } else {
            // СВЕТЛАЯ ТЕМА → тёмные элементы
            logo.setTextColor(Color.BLACK)
            setStatusBarIconsDark(true)  // ← ИКОНКИ СТАТУСБАРА СВЕТЛЫЕ
        }
    }

    private fun loadChatsFromCache() {
        val prefs = getSharedPreferences("app_cache", MODE_PRIVATE)
        val json = prefs.getString("chats_list", null)
        if (!json.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<Chat>>() {}.type
                val chats: List<Chat> = Gson().fromJson(json, type)
                if (chats.isNotEmpty()) {
                    updateAdapter(chats)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateAdapter(chats: List<Chat>) {
        val myId = getSharedPreferences("app", MODE_PRIVATE).getInt("user_id", 0)
        
        val items = chats.map { c ->
            val prefix = if (c.last_sender_id == myId) "Вы: " else ""
            ChatItem(
                chat_id = c.id,
                user_id = c.user_id ?: 0,
                name = (c.name ?: "").ifBlank { "Неизвестно" },
                avatar = c.avatar,
                last_message = prefix + (c.last_msg ?: ""),
                last_time = c.last_time,
                unread = c.unread
            )
        }

        recycler.adapter = ChatsAdapter(items) { chat ->
            val i = Intent(this@ChatsActivity, ChatActivity::class.java)
            i.putExtra("chat_id", chat.chat_id)
            i.putExtra("chat_name", chat.name)
            i.putExtra("chat_avatar", chat.avatar)
            i.putExtra("target_user_id", chat.user_id) // Передаем ID собеседника
            startActivity(i)
        }
    }

    private fun loadChats() {
        val token = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        val swipe = findViewById<SwipeRefreshLayout>(R.id.swipeChats)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val chats: List<Chat> = repo.getChats(token)

                // Сохраняем в кэш JSON
                val json = Gson().toJson(chats)
                getSharedPreferences("app_cache", MODE_PRIVATE)
                    .edit()
                    .putString("chats_list", json)
                    .apply()

                runOnUiThread {
                    swipe.isRefreshing = false  // ⬅ ОСТАНАВЛИВАЕМ
                    updateAdapter(chats)
                }

            } catch (e: Exception) {
                runOnUiThread {
                    swipe.isRefreshing = false  // ⬅ ОСТАНАВЛИВАЕМ
                    e.printStackTrace() 
                }
            }
        }
    }
}
