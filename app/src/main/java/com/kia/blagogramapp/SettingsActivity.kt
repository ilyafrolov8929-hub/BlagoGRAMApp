package com.kia.blagogramapp

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Тест графического движка a-la Telegram.
 * Воссоздаем дизайн из activity_settings.xml с помощью кода.
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- 1. Темы ---
        DesignEngine.init(this)

        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        val isDarkTheme = themePrefs.getBoolean("is_dark_theme", false)

        DesignEngine.Theme.apply(isDarkTheme)

        // --- 2. Настройка окна ---
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        if (!isDarkTheme) {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        window.statusBarColor = Color.TRANSPARENT

        val statusBarHeight = getStatusBarHeight()

        // Корневой FrameLayout
        val rootLayout = FrameLayout(this)
        rootLayout.setBackgroundColor(DesignEngine.Theme.colorBackground)
        setContentView(rootLayout)

        // 3. Скролл-контейнер
        val scrollView = ScrollView(this)
        scrollView.isFillViewport = true
        scrollView.clipToPadding = false
        scrollView.overScrollMode = View.OVER_SCROLL_NEVER

        val contentLayout = LinearLayout(this)
        contentLayout.orientation = LinearLayout.VERTICAL
        contentLayout.setPadding(0, 0, 0, 0)

        scrollView.addView(contentLayout)

        // 4. Шапка профиля
        // Отступ сверху = высота статус бара + примерная высота тулбара (56dp) + запас
        // В оригинале marginTop="100dp", здесь рассчитываем динамически
        val topMarginContent = LayoutHelper.dp(20)

        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val displayName = prefs.getString("display_name", "Пользователь") ?: "Пользователь"
        val status = prefs.getString("status", "") ?: "Нет статуса"
        val avatar = prefs.getString("avatar", "") ?: ""

        val profileHeader = ProfileHeaderCell(this)
        profileHeader.setData(displayName, status, "https://blagogram.ru/avatars/$avatar")

        contentLayout.addView(profileHeader, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = topMarginContent
        })

        contentLayout.addView(SpacerCell(this, LayoutHelper.dp(10)))

        // 5. Белая подложка (sheet)
        val sheetContainer = LinearLayout(this)
        sheetContainer.orientation = LinearLayout.VERTICAL
        sheetContainer.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_sheet_top_rounded)
        sheetContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(DesignEngine.Theme.colorSheetBackground)
        sheetContainer.minimumHeight = LayoutHelper.dp(800)
        sheetContainer.setPadding(LayoutHelper.dp(20), LayoutHelper.dp(20), LayoutHelper.dp(20), LayoutHelper.dp(50))

        // --- Секция АККАУНТ ---
        sheetContainer.addView(HeaderCell(this).apply { text = "АККАУНТ" })

        val accountCard = ContainerCell(this)
        accountCard.addCell(TextCell(this).apply {
            setData("Редактировать профиль", R.drawable.ic_edit)
            setOnClickListener { startActivity(android.content.Intent(context, ProfileEditActivity::class.java)) }
        })
        accountCard.addCell(TextCell(this).apply {
            setData("Приватность", R.drawable.ic_user)
            setOnClickListener { Toast.makeText(context, "Приватность", Toast.LENGTH_SHORT).show() }
        }, isLast = true)

        sheetContainer.addView(accountCard)
        sheetContainer.addView(SpacerCell(this, LayoutHelper.dp(10)))

        // --- Секция НАСТРОЙКИ ---
        sheetContainer.addView(HeaderCell(this).apply { text = "НАСТРОЙКИ" })

        val settingsCard = ContainerCell(this)
        settingsCard.addCell(TextCell(this).apply {
            setData("Настройки чатов", R.drawable.ic_chat)
        })
        settingsCard.addCell(TextCell(this).apply {
            setData("Уведомления и звуки", R.drawable.ic_music)
        })
        settingsCard.addCell(TextCell(this).apply {
            setData("Темы и кастомизация", R.drawable.ic_music)
            setOnClickListener { startActivity(android.content.Intent(context, ThemeSelectionActivity::class.java)) }
        }, isLast = true)

        sheetContainer.addView(settingsCard)
        sheetContainer.addView(SpacerCell(this, LayoutHelper.dp(10)))

        // --- Секция ИНФОРМАЦИЯ ---
        sheetContainer.addView(HeaderCell(this).apply { text = "ИНФОРМАЦИЯ" })

        val infoCard = ContainerCell(this)
        infoCard.addCell(TextCell(this).apply {
            setData("О приложении", R.drawable.ic_more)
            setOnClickListener { startActivity(android.content.Intent(context, AboutActivity::class.java)) }
        }, isLast = true)

        sheetContainer.addView(infoCard)
        sheetContainer.addView(SpacerCell(this, LayoutHelper.dp(10)))

        // --- Кнопка ВЫЙТИ ---
        val logoutCard = ContainerCell(this)
        logoutCard.addCell(TextCell(this).apply {
            setData("Выйти", R.drawable.ic_logout)
            setTextColor(DesignEngine.Theme.colorLogout)
            setIconColor(DesignEngine.Theme.colorLogout)
            setOnClickListener {
                val p = getSharedPreferences("app", MODE_PRIVATE)
                p.edit().clear().apply()
                getSharedPreferences("app_cache", MODE_PRIVATE).edit().clear().apply()
                val intent = android.content.Intent(this@SettingsActivity, StartActivity::class.java)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }, isLast = true)

        sheetContainer.addView(logoutCard)

        val versionText = TextView(this)
        versionText.text = "BlagoGRAM v1.0 for Kia"
        versionText.textSize = 12f
        versionText.setTextColor(Color.parseColor("#909090"))
        versionText.gravity = Gravity.CENTER
        versionText.setPadding(0, LayoutHelper.dp(20), 0, LayoutHelper.dp(20))
        sheetContainer.addView(versionText)

        contentLayout.addView(sheetContainer)
        rootLayout.addView(scrollView)

        // 6. Тулбар (Прозрачный)
        val toolbarLayout = RelativeLayout(this)
        toolbarLayout.setBackgroundColor(Color.TRANSPARENT)

        // Отступы внутри тулбара
        toolbarLayout.setPadding(
            LayoutHelper.dp(16),
            statusBarHeight + LayoutHelper.dp(10),
            LayoutHelper.dp(16),
            LayoutHelper.dp(10) // Вернули 10dp
        )

        // Кнопка BACK
        val backBtn = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_stat_pill)
            val btnColor = if (isDarkTheme) Color.parseColor("#333333") else Color.parseColor("#E0E0E0")
            backgroundTintList = android.content.res.ColorStateList.valueOf(btnColor)
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(6), LayoutHelper.dp(12), LayoutHelper.dp(6))
            setOnClickListener { finish() }
        }
        val backIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_back)
            val iconColor = if (isDarkTheme) Color.WHITE else Color.BLACK
            setColorFilter(iconColor)
            layoutParams = LinearLayout.LayoutParams(LayoutHelper.dp(20), LayoutHelper.dp(20))
        }
        backBtn.addView(backIcon)

        // Заголовок
        val titleView = TextView(this).apply {
            text = "Настройки"
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }

        // Кнопка ТЕМА
        val themeBtn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_stat_pill)
            val btnColor = if (isDarkTheme) Color.parseColor("#333333") else Color.parseColor("#E0E0E0")
            backgroundTintList = android.content.res.ColorStateList.valueOf(btnColor)
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(6), LayoutHelper.dp(12), LayoutHelper.dp(6))
            setOnClickListener {
                themePrefs.edit().putBoolean("is_dark_theme", !isDarkTheme).apply()
                finish()
                overridePendingTransition(0, 0)
                startActivity(intent)
                overridePendingTransition(0, 0)
            }
        }
        val themeIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_moments)
            val iconColor = if (isDarkTheme) Color.WHITE else Color.BLACK
            setColorFilter(iconColor)
            layoutParams = LinearLayout.LayoutParams(LayoutHelper.dp(20), LayoutHelper.dp(20))
        }
        themeBtn.addView(themeIcon)

        // Контейнер контента
        val toolbarContent = RelativeLayout(this)
        toolbarContent.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)

        toolbarContent.addView(backBtn, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_START)
            addRule(RelativeLayout.CENTER_VERTICAL)
        })

        toolbarContent.addView(titleView, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.CENTER_IN_PARENT)
        })

        toolbarContent.addView(themeBtn, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_END)
            addRule(RelativeLayout.CENTER_VERTICAL)
        })

        toolbarLayout.addView(toolbarContent)

        // Тулбар фиксирован сверху
        rootLayout.addView(toolbarLayout, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP
        })
    }

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }
}
