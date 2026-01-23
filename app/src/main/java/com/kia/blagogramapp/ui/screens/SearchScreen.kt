package com.kia.blagogramapp.ui.screens

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.drawable.GradientDrawable
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
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kia.blagogramapp.BaseScreen
import com.kia.blagogramapp.DesignEngine
import com.kia.blagogramapp.LayoutHelper
import com.kia.blagogramapp.R
import com.kia.blagogramapp.adapters.SearchUsersAdapter
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.models.SearchUserItem
import com.kia.blagogramapp.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchScreen(context: Context) : BaseScreen(context) {
    val activity = context as? AppCompatActivity
    private var isCurrentThemeDark: Boolean = false
    private var isCurrentUseMonet: Boolean = false
    private val repo = ApiRepository(RetrofitClient.api)
    private var searchJob: Job? = null

    private lateinit var input: EditText
    private lateinit var recycler: RecyclerView
    private lateinit var progress: ProgressBar

    override fun createView(): View {
        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(DesignEngine.Theme.colorBackground)
        }

        val themePrefs = context.getSharedPreferences("app_theme", MODE_PRIVATE)
        isCurrentThemeDark = themePrefs.getBoolean("is_dark_theme", false)
        isCurrentUseMonet = themePrefs.getBoolean("use_monet", false)

        val toolbar = DesignEngine.createToolBar(
            context = context,
            title = "Поиск",
            leftButtonIconRes = R.drawable.ic_back,
            leftButtonClickListener = { finishFragment() },
            isDark = isCurrentThemeDark
        )
        rootLayout.addView(toolbar)

        val searchContainer = FrameLayout(context)
        input = EditText(context).apply {
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

            val outValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.actionModeWebSearchDrawable, outValue, true)
            val searchDrawable = context.getDrawable(outValue.resourceId)?.mutate()
            searchDrawable?.setTint(DesignEngine.Theme.colorTextSecondary)
            setCompoundDrawablesWithIntrinsicBounds(searchDrawable, null, null, null)
            compoundDrawablePadding = LayoutHelper.dp(12)
        }
        searchContainer.addView(input, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 16, 8, 16, 8))
        rootLayout.addView(searchContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        progress = ProgressBar(context).apply {
            visibility = View.GONE
        }
        rootLayout.addView(progress, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, Gravity.CENTER_HORIZONTAL, 0, 8, 0, 0))

        recycler = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            clipToPadding = false
            setPadding(0, LayoutHelper.dp(8), 0, LayoutHelper.dp(8))
        }
        rootLayout.addView(recycler, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1f))

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

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        return rootLayout
    }

    private fun search(query: String) {
        val token = context.getSharedPreferences("app", MODE_PRIVATE)
            .getString("token", "") ?: ""

        searchJob?.cancel()
        searchJob = CoroutineScope(Dispatchers.IO).launch {
            delay(300)
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
        val token = context.getSharedPreferences("app", MODE_PRIVATE)
            .getString("token", "") ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val chatId = repo.createChat(token, user.id)
                // ПЕРЕХОД ДОЛЖЕН БЫТЬ В ГЛАВНОМ ПОТОКЕ
                withContext(Dispatchers.Main) {
                    presentFragment(ChatScreen(context, chatId, user.id, user.display_name, user.avatar.toString(), user.is_premium))
                }
            } catch (_: Exception) {}
        }
    }

    override fun onResume() {
        super.onResume()
        val themePrefs = context.getSharedPreferences("app_theme", MODE_PRIVATE)
        val isNowDark = themePrefs.getBoolean("is_dark_theme", false)
        val isNowMonet = themePrefs.getBoolean("use_monet", false)
        if (isNowDark != isCurrentThemeDark || isNowMonet != isCurrentUseMonet) {
            // Очищаем и пересоздаем UI если нужно
        }
    }
}
