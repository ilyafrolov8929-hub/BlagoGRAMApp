package com.kia.blagogramapp

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kia.blagogramapp.adapters.SearchUsersAdapter
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.models.SearchUserItem
import com.kia.blagogramapp.network.RetrofitClient
import kotlinx.coroutines.*

class SearchUserActivity : AppCompatActivity() {

    private val repo = ApiRepository(RetrofitClient.api)
    private var searchJob: Job? = null

    private lateinit var input: EditText
    private lateinit var recycler: RecyclerView
    private lateinit var progress: ProgressBar
    private var isCurrentThemeDark: Boolean = false
    private var isCurrentUseMonet: Boolean = false


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

        // Enable Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT // Make nav bar transparent like in ChatsActivity

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !isDarkTheme
        controller.isAppearanceLightNavigationBars = !isDarkTheme

        // Root Layout
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(DesignEngine.Theme.colorBackground) // Theme color
        }
        setContentView(rootLayout)

        // Toolbar from DesignEngine
        val toolbar = DesignEngine.createToolBar(
            context = this,
            title = "Поиск",
            leftButtonIconRes = R.drawable.ic_back,
            leftButtonClickListener = { finish() },
            isDark = isCurrentThemeDark
        )
        rootLayout.addView(toolbar)


        // Search Input
        val searchContainer = FrameLayout(this)
        input = EditText(this).apply {
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorCardBackground)
                cornerRadius = LayoutHelper.dp(18).toFloat()
                setStroke(LayoutHelper.dp(1), DesignEngine.Theme.colorDivider)
            }
            hint = "Поиск пользователей..."
            setHintTextColor(DesignEngine.Theme.colorTextSecondary)
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            textSize = 16f
            maxLines = 1
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(12), LayoutHelper.dp(16), LayoutHelper.dp(12))

            // Search Icon
            val outValue = TypedValue()
            theme.resolveAttribute(android.R.attr.actionModeWebSearchDrawable, outValue, true)
            val searchDrawable = getDrawable(outValue.resourceId)?.mutate()
            searchDrawable?.setTint(DesignEngine.Theme.colorTextSecondary)
            setCompoundDrawablesWithIntrinsicBounds(searchDrawable, null, null, null)
            compoundDrawablePadding = LayoutHelper.dp(12)
        }
        searchContainer.addView(input, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 16, 8, 16, 8))
        rootLayout.addView(searchContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        // ProgressBar
        progress = ProgressBar(this).apply {
            visibility = View.GONE
        }
        rootLayout.addView(progress, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, Gravity.CENTER_HORIZONTAL, 0, 8, 0, 0))

        // RecyclerView
        recycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@SearchUserActivity)
            clipToPadding = false
            setPadding(0, LayoutHelper.dp(8), 0, LayoutHelper.dp(8))
        }
        rootLayout.addView(recycler, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        // Listeners
        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(t: CharSequence?, start: Int, before: Int, count: Int) {
                val q = t.toString().trim()

                if (q.isNotEmpty()) {
                    progress.visibility = View.VISIBLE
                    search(q)
                } else {
                    searchJob?.cancel()
                    progress.visibility = View.GONE
                    recycler.adapter = SearchUsersAdapter(emptyList()) {}
                }
            }
        })

        // Handle Window Insets
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Apply bottom padding to root (navigation bar)
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                insets.bottom
            )

            WindowInsetsCompat.CONSUMED
        }
    }

    private fun search(query: String) {
        val token = getSharedPreferences("app", MODE_PRIVATE)
            .getString("token", "") ?: ""

        searchJob?.cancel()

        searchJob = CoroutineScope(Dispatchers.IO).launch {
            delay(300) // небольшая задержка для более плавного поиска
            try {
                val users: List<SearchUserItem> = repo.searchUsers(token, query)

                withContext(Dispatchers.Main) {
                    progress.visibility = View.GONE
                    recycler.adapter = SearchUsersAdapter(users) { user ->
                        createChat(user)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progress.visibility = View.GONE
                    recycler.adapter = SearchUsersAdapter(emptyList()) {}
                }
            }
        }
    }

    private fun createChat(user: SearchUserItem) {
        val token = getSharedPreferences("app", MODE_PRIVATE)
            .getString("token", "") ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val chatId = repo.createChat(token, user.id)

                val i = Intent(this@SearchUserActivity, ChatActivity::class.java)
                i.putExtra("chat_id", chatId)
                i.putExtra("chat_name", user.display_name)
                i.putExtra("chat_avatar", user.avatar)
                i.putExtra("target_user_id", user.id) // Передаем ID пользователя явно
                startActivity(i)
                finish()

            } catch (_: Exception) {}
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
    }
}
