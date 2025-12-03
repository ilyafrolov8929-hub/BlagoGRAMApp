package com.kia.blagogramapp

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowInsetsController
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil.load
import com.google.android.material.navigation.NavigationView
import com.kia.blagogramapp.adapters.ChatsAdapter
import com.kia.blagogramapp.api.ApiClient
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.model.Chat
import com.kia.blagogramapp.models.ChatItem
import com.kia.blagogramapp.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    private lateinit var menuBtn: ImageView
    private lateinit var logo: TextView

    private lateinit var drawer: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var recycler: RecyclerView
    private var repo = ApiRepository(RetrofitClient.api)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // делать обложку под статусбар
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = Color.TRANSPARENT
        setContentView(R.layout.activity_chats)
        drawer = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        val swipe = findViewById<SwipeRefreshLayout>(R.id.swipeChats)

        swipe.setOnRefreshListener {
            loadChats()
        }

        menuBtn = findViewById(R.id.menuBtn)
        logo = findViewById(R.id.logo)

        applyThemeAuto()

        recycler = findViewById(R.id.recyclerChats)
        recycler.layoutManager = LinearLayoutManager(this)

        // ОТСТУП МЕЖДУ ЭЛЕМЕНТАМИ (важно!)
        recycler.addItemDecoration(VerticalSpaceDecoration(18))


        findViewById<ImageView>(R.id.menuBtn).setOnClickListener {
            drawer.openDrawer(Gravity.LEFT)
        }

        logo.text = "Привет, ${getSharedPreferences("app", MODE_PRIVATE).getString("display_name", "Имя") ?: "Имя"}!"

        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.item_logout -> {
                    getSharedPreferences("app", MODE_PRIVATE).edit().clear().apply()
                    finish()
                }

                R.id.item_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    drawer.closeDrawers()
                }

                R.id.item_settings -> {
                    startActivity(Intent(this, ProfileEditActivity::class.java))
                    drawer.closeDrawers()
                }

                R.id.item_new_chat -> {
                    startActivity(Intent(this, SearchUserActivity::class.java))
                    drawer.closeDrawers()
                }

            }
            true
        }

        updateHeader()
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
        updateHeader()
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
            menuBtn.setColorFilter(Color.WHITE)
            logo.setTextColor(Color.WHITE)
            setStatusBarIconsDark(false)  // ← ИКОНКИ СТАТУСБАРА СВЕТЛЫЕ

        } else {
            // СВЕТЛАЯ ТЕМА → тёмные элементы
            menuBtn.setColorFilter(Color.BLACK)
            logo.setTextColor(Color.BLACK)
            setStatusBarIconsDark(true)  // ← ИКОНКИ СТАТУСБАРА СВЕТЛЫЕ
        }
    }

    private fun updateHeader() {
        val prefs = getSharedPreferences("app", MODE_PRIVATE)

        val avatar = prefs.getString("avatar", "")
        val name = prefs.getString("display_name", "Имя")
        val login = prefs.getString("login", "")

        val header = navigationView.getHeaderView(0)

        val headerAvatar = header.findViewById<ImageView>(R.id.navAvatar)
        val headerName = header.findViewById<TextView>(R.id.navName)         // display_name
        val headerUsername = header.findViewById<TextView>(R.id.navUsername) // @login

        headerName.text = name
        headerUsername.text = "@$login"

        headerAvatar.load("https://blagogram.ru/avatars/${avatar}?v=${System.currentTimeMillis()}") {
            crossfade(true)
            placeholder(R.drawable.ic_user_placeholder)
            error(R.drawable.ic_user_placeholder)
        }

        Log.d("HEADER", "updated avatar=$avatar name=$name login=$login")
    }

    private fun loadChats() {
        val token = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
        val swipe = findViewById<SwipeRefreshLayout>(R.id.swipeChats)
        val myId = getSharedPreferences("app", MODE_PRIVATE).getInt("user_id", 0)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val chats: List<Chat> = repo.getChats(token)

                val items = chats.map { c ->
                    val prefix = if (c.last_sender_id == myId) "Вы: " else ""
                    ChatItem(
                        chat_id = c.id,
                        user_id = 0,
                        name = (c.name ?: "").ifBlank { "Неизвестно" },
                        avatar = c.avatar,
                        last_message = prefix + (c.last_msg ?: ""),
                        last_time = c.last_time,
                        unread = c.unread
                    )
                }

                runOnUiThread {
                    swipe.isRefreshing = false  // ⬅ ОСТАНАВЛИВАЕМ
                    recycler.adapter = ChatsAdapter(items) { chat ->
                        val i = Intent(this@ChatsActivity, ChatActivity::class.java)
                        i.putExtra("chat_id", chat.chat_id)
                        i.putExtra("chat_name", chat.name)
                        i.putExtra("chat_avatar", chat.avatar)
                        startActivity(i)
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    swipe.isRefreshing = false  // ⬅ ОСТАНАВЛИВАЕМ
                    e.printStackTrace() }
            }
        }
    }
}
