package com.kia.blagogramapp

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
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

/**
 * Экран "О приложении" на новом графическом движке.
 */
class AboutActivity : AppCompatActivity() {
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

        // 2. Настройка окна
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
        contentLayout.gravity = Gravity.CENTER_HORIZONTAL
        contentLayout.setPadding(0, 0, 0, LayoutHelper.dp(20))
        
        scrollView.addView(contentLayout)
        
        // Отступ сверху под тулбар
        val topMarginContent = LayoutHelper.dp(56)
        contentLayout.addView(SpacerCell(this, topMarginContent))

        // --- Логотип ---
        val logoView = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            layoutParams = LinearLayout.LayoutParams(LayoutHelper.dp(100), LayoutHelper.dp(100)).apply {
                bottomMargin = LayoutHelper.dp(16)
            }
        }
        contentLayout.addView(logoView)

        // --- Название ---
        val appNameView = TextView(this).apply {
            text = "BlagoGRAM"
            textSize = 24f
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        contentLayout.addView(appNameView)

        // --- Версия ---
        val versionTextView = TextView(this).apply {
            textSize = 14f
            setTextColor(DesignEngine.Theme.colorProfileStatus) // Серый цвет
            gravity = Gravity.CENTER
            setPadding(0, LayoutHelper.dp(4), 0, LayoutHelper.dp(32))
        }
        
        // Логика получения версии
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            versionTextView.text = "Версия ${packageInfo.versionName}"
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            versionTextView.text = "Версия не определена"
        }
        contentLayout.addView(versionTextView)

        // --- Карточка с сайтом ---
        val websiteCard = ContainerCell(this)
        
        // Создаем кастомную ячейку для сайта, так как там нужен цветной текст справа
        val websiteCell = FrameLayout(this).apply {
            val outValue = android.util.TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            foreground = context.getDrawable(outValue.resourceId)
            isClickable = true
            setOnClickListener {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://blagogram.ru"))
                startActivity(browserIntent)
            }
            
            // Иконка
            val icon = ImageView(context).apply {
                setImageResource(R.drawable.ic_more)
                setColorFilter(DesignEngine.Theme.colorIconPrimary, android.graphics.PorterDuff.Mode.SRC_IN)
            }
            addView(icon, LayoutHelper.createFrame(24, 24, Gravity.CENTER_VERTICAL or Gravity.START, 16, 0, 0, 0))
            
            // Текст "Наш сайт"
            val title = TextView(context).apply {
                text = "Наш сайт"
                textSize = 16f
                setTextColor(DesignEngine.Theme.colorTextPrimary)
            }
            // Отступ слева 56dp (16+24+16), отступ справа (под текст ссылки и шеврон)
            addView(title, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.START, 56, 0, 0, 0))
            
            // Ссылка справа (синяя)
            val linkText = TextView(context).apply {
                text = "blagogram.ru"
                textSize = 14f
                setTextColor(Color.parseColor("#00BCD4")) // Cyan цвет из XML
            }
            // Располагаем справа от шеврона (примерно 40dp от края)
            addView(linkText, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.END, 0, 0, 40, 0))
            
            // Шеврон
            val chevron = ImageView(context).apply {
                setImageResource(R.drawable.ic_chevron_right)
                setColorFilter(DesignEngine.Theme.colorChevron, android.graphics.PorterDuff.Mode.SRC_IN)
            }
            addView(chevron, LayoutHelper.createFrame(24, 24, Gravity.CENTER_VERTICAL or Gravity.END, 0, 0, 8, 0))
            
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LayoutHelper.dp(54))
        }
        
        websiteCard.addView(websiteCell)
        
        // Добавляем карточку с отступами
        val cardParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            leftMargin = LayoutHelper.dp(24)
            rightMargin = LayoutHelper.dp(24)
        }
        contentLayout.addView(websiteCard, cardParams)

        // --- Копирайт ---
        val copyrightView = TextView(this).apply {
            text = "© 2025 BlagoGRAM Inc."
            textSize = 12f
            setTextColor(Color.parseColor("#909090"))
            gravity = Gravity.CENTER
            setPadding(0, LayoutHelper.dp(20), 0, LayoutHelper.dp(20))
        }
        contentLayout.addView(copyrightView)

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
            text = "О приложении"
            textSize = 20f
            setTextColor(DesignEngine.Theme.colorTextPrimary) // Используем цвет текста темы (черный/белый), а не фиксированный белый, т.к. фон страницы может быть светлым
            // В оригинале заголовок был черным на белом фоне.
            // В нашем движке на светлой теме фон серый, текст черный. На темной - фон черный, текст белый.
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
    
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
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
