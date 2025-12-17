package com.kia.blagogramapp

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.google.gson.Gson
import com.kia.blagogramapp.adapters.ChatsAdapter
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.model.Chat
import com.kia.blagogramapp.models.ChatItem
import com.kia.blagogramapp.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatsActivity : AppCompatActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recycler: RecyclerView
    private var repo = ApiRepository(RetrofitClient.api)

    private var isCurrentThemeDark: Boolean = false
    private var isCurrentUseMonet: Boolean = false

    // Элементы бокового меню
    private lateinit var drawerOverlay: FrameLayout
    private lateinit var drawerPanel: LinearLayout
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
        val window = window
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !isDarkTheme
        controller.isAppearanceLightNavigationBars = !isDarkTheme

        // 3. Верстка UI кодом
        val rootLayout = FrameLayout(this)
        rootLayout.setBackgroundColor(DesignEngine.Theme.colorBackground)

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, 0)
            WindowInsetsCompat.CONSUMED
        }

        setContentView(rootLayout)

        val statusBarHeight = getStatusBarHeight()

        // --- СПИСОК ЧАТОВ ---
        swipeRefresh = SwipeRefreshLayout(this)
        swipeRefresh.setProgressBackgroundColorSchemeColor(DesignEngine.Theme.colorCardBackground)
        swipeRefresh.setColorSchemeColors(DesignEngine.Theme.colorBlue)
        swipeRefresh.setProgressViewOffset(false, statusBarHeight + LayoutHelper.dp(60), statusBarHeight + LayoutHelper.dp(120))

        recycler = RecyclerView(this)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.clipToPadding = false
        // Обычные отступы
        recycler.setPadding(0, statusBarHeight + LayoutHelper.dp(80), 0, LayoutHelper.dp(20))

        swipeRefresh.addView(recycler)
        rootLayout.addView(swipeRefresh)

        // --- ЗАГОЛОВОК (Header) ---
        val titleView = TextView(this).apply {
            text = "Чаты"
            textSize = 28f
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        // Кнопка "Меню" (3 точки) СЛЕВА
        val menuBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_menu) // 3 точки
            setColorFilter(DesignEngine.Theme.colorIconPrimary, PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
            setOnClickListener {
                toggleDrawer()
            }
        }

        // Кнопка "Новый чат" (карандаш) СПРАВА
        val editBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_edit)
            setColorFilter(DesignEngine.Theme.colorBlue, PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
            setOnClickListener {
                 startActivity(Intent(this@ChatsActivity, SearchUserActivity::class.java))
            }
        }

        val headerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(LayoutHelper.dp(16), statusBarHeight + LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16))
            setBackgroundColor(DesignEngine.Theme.colorBackground)
            elevation = LayoutHelper.dp(4).toFloat()
        }

        headerContainer.addView(menuBtn, LinearLayout.LayoutParams(LayoutHelper.dp(40), LayoutHelper.dp(40)))
        headerContainer.addView(titleView, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        headerContainer.addView(editBtn, LinearLayout.LayoutParams(LayoutHelper.dp(40), LayoutHelper.dp(40)))

        rootLayout.addView(headerContainer, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP
        })

        // --- БОКОВОЕ МЕНЮ (Drawer) ---
        initDrawer(rootLayout)

        loadChats()

        swipeRefresh.setOnRefreshListener {
            loadChats()
        }
    }

    private fun initDrawer(root: FrameLayout) {
        val statusBarHeight = getStatusBarHeight()

        // 1. Затемнение фона (elevation должен быть выше чем у хедера (4dp))
        drawerOverlay = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#99000000"))
            alpha = 0f
            visibility = View.GONE
            // ВАЖНО: Поднимаем слой затемнения над хедером
            elevation = LayoutHelper.dp(10).toFloat()
            setOnClickListener {
                if (isDrawerOpen) toggleDrawer()
            }
        }
        root.addView(drawerOverlay, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        // 2. Панель меню (Выезжает СЛЕВА)
        drawerPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(DesignEngine.Theme.colorSheetBackground)
            isClickable = true
            // ВАЖНО: Поднимаем саму панель еще выше
            elevation = LayoutHelper.dp(10).toFloat()

            // --- HEADER MENU ---
            val headerLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(DesignEngine.Theme.colorCardBackground)
                // Отступ сверху равен высоте статус бара + немного места
                setPadding(LayoutHelper.dp(20), statusBarHeight + LayoutHelper.dp(20), LayoutHelper.dp(20), LayoutHelper.dp(20))
            }

            val prefs = getSharedPreferences("app", MODE_PRIVATE)
            val myName = prefs.getString("display_name", "Вы") ?: "Вы"
            val myLogin = prefs.getString("login", "username") ?: "username"
            val myStatus = prefs.getString("status", "Статус") ?: "Нет статуса"
            val myAvatar = prefs.getString("avatar", "") ?: ""

            // Avatar
            val avatarView = ShapeableImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(Color.LTGRAY)
                shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes(LayoutHelper.dp(35).toFloat())
                    .build()
            }
            avatarView.load("https://blagogram.ru/avatars/$myAvatar") {
                placeholder(R.drawable.ic_user_placeholder)
                error(R.drawable.ic_user_placeholder)
            }
            headerLayout.addView(avatarView, LayoutHelper.createLinear(70, 70, 0f, 0, 0, 0, 0, 16))

            // Name
            val nameTv = TextView(context).apply {
                text = myName
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(DesignEngine.Theme.colorTextPrimary)
            }
            headerLayout.addView(nameTv)

            // Login
            val loginTv = TextView(context).apply {
                text = "@$myLogin"
                textSize = 14f
                setTextColor(DesignEngine.Theme.colorBlue)
            }
            headerLayout.addView(loginTv, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 2, 0, 8))

            // Status
            val statusTv = TextView(context).apply {
                text = myStatus
                textSize = 14f
                maxLines = 1
                setTextColor(DesignEngine.Theme.colorTextSecondary)
            }
            headerLayout.addView(statusTv)

            addView(headerLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

            // --- MENU ITEMS ---
            addView(createDrawerItem("Профиль", R.drawable.ic_user) {
                startActivity(Intent(this@ChatsActivity, ProfileActivity::class.java))
                toggleDrawer()
            })

            addView(createDrawerItem("Настройки", R.drawable.ic_settings) {
                startActivity(Intent(this@ChatsActivity, SettingsActivity::class.java))
                toggleDrawer()
            })

            addView(createDrawerItem("Выход", R.drawable.ic_logout) {
                val p = getSharedPreferences("app", MODE_PRIVATE)
                p.edit().clear().apply()
                getSharedPreferences("app_cache", MODE_PRIVATE).edit().clear().apply()
                val intent = Intent(this@ChatsActivity, StartActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            })

            // Spacer to push "About" to bottom
            addView(View(context), LinearLayout.LayoutParams(LayoutHelper.MATCH_PARENT, 0, 1f))

            // --- BOTTOM ITEM ---
            val divider = View(context)
            divider.setBackgroundColor(DesignEngine.Theme.colorDivider)
            addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1))

            addView(createDrawerItem("О приложении", R.drawable.ic_more) {
                startActivity(Intent(this@ChatsActivity, AboutActivity::class.java))
                toggleDrawer()
            })

            // Padding bottom for navigation bar
            addView(View(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 20))
        }

        // Ширина панели - 80% экрана
        val width = (resources.displayMetrics.widthPixels * 0.8).toInt()
        val params = FrameLayout.LayoutParams(width, FrameLayout.LayoutParams.MATCH_PARENT).apply {
            gravity = Gravity.START // Прижимаем ВЛЕВО
        }
        root.addView(drawerPanel, params)

        // Изначально скрыто СЛЕВА (-width)
        drawerPanel.translationX = (-width).toFloat()
    }

    private fun createDrawerItem(text: String, icon: Int, onClick: () -> Unit): View {
        return TextCell(this).apply {
            setData(text, icon)
            setOnClickListener { onClick() }
        }
    }

    private fun toggleDrawer() {
        if (isDrawerOpen) {
            // Закрываем
            drawerPanel.animate().translationX(-drawerPanel.width.toFloat())
                .setInterpolator(DecelerateInterpolator())
                .setDuration(300)
                .start()

            drawerOverlay.animate().alpha(0f)
                .setDuration(300)
                .withEndAction { drawerOverlay.visibility = View.GONE }
                .start()
        } else {
            // Открываем
            drawerOverlay.visibility = View.VISIBLE
            drawerOverlay.animate().alpha(1f).setDuration(300).start()

            drawerPanel.animate().translationX(0f)
                .setInterpolator(DecelerateInterpolator())
                .setDuration(300)
                .start()
        }
        isDrawerOpen = !isDrawerOpen
    }

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
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
                    swipeRefresh.isRefreshing = false
                    updateAdapter(chats)
                }

            } catch (e: Exception) {
                runOnUiThread {
                    swipeRefresh.isRefreshing = false
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Проверяем смену темы
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        val isNowDark = themePrefs.getBoolean("is_dark_theme", false)
        val isNowMonet = themePrefs.getBoolean("use_monet", false)
        
        if (isNowDark != isCurrentThemeDark || isNowMonet != isCurrentUseMonet) {
            recreate()
            return
        }

        loadChats() // Обновляем список при возврате
    }
}
