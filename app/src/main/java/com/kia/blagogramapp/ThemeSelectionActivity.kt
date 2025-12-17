package com.kia.blagogramapp

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
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
import androidx.appcompat.app.AppCompatActivity

class ThemeSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Инициализация и применение темы
        DesignEngine.init(this)
        
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        val isDarkTheme = themePrefs.getBoolean("is_dark_theme", false)
        
        DesignEngine.Theme.apply(isDarkTheme)
        
        // 2. Настройка окна
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        if (!isDarkTheme) {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        window.statusBarColor = Color.TRANSPARENT

        val statusBarHeight = getStatusBarHeight()

        // Корневой Layout
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
        contentLayout.setPadding(0, 0, 0, LayoutHelper.dp(20))
        
        scrollView.addView(contentLayout)
        
        // Отступ сверху для контента (под тулбар)
        val topMarginContent = statusBarHeight + LayoutHelper.dp(56) + LayoutHelper.dp(20)
        
        // Заполнитель места под тулбар
        contentLayout.addView(SpacerCell(this, topMarginContent))

        // --- Секция ТЕМЫ ---
        contentLayout.addView(HeaderCell(this).apply { 
            text = "ТЕМЫ ОФОРМЛЕНИЯ" 
            setPadding(LayoutHelper.dp(20), 0, LayoutHelper.dp(20), LayoutHelper.dp(8))
        })
        
        val themesCard = ContainerCell(this)
        
        // Светлая тема
        val lightCell = TextCell(this).apply {
            setData("Светлая", R.drawable.ic_moments) // Используем ic_moments как плейсхолдер
            // Если выбрана светлая - можно как-то выделить.
            // Пока просто список.
            setOnClickListener {
                if (isDarkTheme) {
                    themePrefs.edit().putBoolean("is_dark_theme", false).apply()
                    recreateActivity()
                }
            }
        }
        // Хак: Если тема светлая, красим иконку в синий (активная), иначе в стандартный
        if (!isDarkTheme) {
            lightCell.setIconColor(DesignEngine.COLOR_BLUE)
            lightCell.setTextColor(DesignEngine.COLOR_BLUE)
        }
        themesCard.addCell(lightCell)

        // Темная тема
        val darkCell = TextCell(this).apply {
            setData("Тёмная", R.drawable.ic_moments)
            setOnClickListener {
                if (!isDarkTheme) {
                    themePrefs.edit().putBoolean("is_dark_theme", true).apply()
                    recreateActivity()
                }
            }
        }
        if (isDarkTheme) {
            darkCell.setIconColor(DesignEngine.COLOR_BLUE)
            darkCell.setTextColor(DesignEngine.COLOR_BLUE)
        }
        themesCard.addCell(darkCell, isLast = true)
        
        // Добавляем карточку с отступами по бокам
        val cardParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            leftMargin = LayoutHelper.dp(16)
            rightMargin = LayoutHelper.dp(16)
        }
        contentLayout.addView(themesCard, cardParams)
        
        contentLayout.addView(SpacerCell(this, LayoutHelper.dp(20)))

        // --- Секция КАСТОМИЗАЦИЯ (Пример на будущее) ---
        contentLayout.addView(HeaderCell(this).apply { 
            text = "АКЦЕНТНЫЙ ЦВЕТ"
            setPadding(LayoutHelper.dp(20), 0, LayoutHelper.dp(20), LayoutHelper.dp(8))
        })
        
        val accentCard = ContainerCell(this)
        accentCard.addCell(TextCell(this).apply {
            setData("Синий (По умолчанию)", 0)
        }, isLast = true)
        
        contentLayout.addView(accentCard, cardParams)

        rootLayout.addView(scrollView)
        
        // 4. Тулбар (Прозрачный)
        val toolbarLayout = RelativeLayout(this)
        toolbarLayout.setBackgroundColor(Color.TRANSPARENT)
        toolbarLayout.setPadding(
            LayoutHelper.dp(16), 
            statusBarHeight + LayoutHelper.dp(10),
            LayoutHelper.dp(16), 
            LayoutHelper.dp(10)
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
            text = "Темы"
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val toolbarContent = RelativeLayout(this)
        toolbarContent.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        
        toolbarContent.addView(backBtn, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_START)
            addRule(RelativeLayout.CENTER_VERTICAL)
        })
        
        toolbarContent.addView(titleView, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.CENTER_IN_PARENT)
        })
        
        toolbarLayout.addView(toolbarContent)
        
        rootLayout.addView(toolbarLayout, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP
        })
    }

    private fun recreateActivity() {
        finish()
        overridePendingTransition(0, 0)
        startActivity(intent)
        overridePendingTransition(0, 0)
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
