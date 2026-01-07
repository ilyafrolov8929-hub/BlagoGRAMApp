package com.kia.blagogramapp

import android.content.Context
import android.graphics.Bitmap
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
import androidx.core.view.WindowCompat
import com.kia.blagogramapp.utils.DevModeManager

/**
 * Исправленная версия: карточка накрывает профиль, профиль затемняется.
 * Навигационная панель сделана прозрачной.
 */
class SettingsActivity : AppCompatActivity() {
    private var isCurrentThemeDark: Boolean = false
    private var isCurrentUseMonet: Boolean = false
    private var animImageView: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DesignEngine.init(this)
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        val isDarkTheme = themePrefs.getBoolean("is_dark_theme", false)
        val useMonet = themePrefs.getBoolean("use_monet", false)

        isCurrentThemeDark = isDarkTheme
        isCurrentUseMonet = useMonet
        DesignEngine.Theme.apply(isDarkTheme, useMonet)

        // Настройка прозрачности системных панелей (статус-бар и док-бар)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !isDarkTheme
        controller.isAppearanceLightNavigationBars = !isDarkTheme

        val statusBarHeight = getStatusBarHeight()
        val navBarHeight = getNavigationBarHeight()

        // Корневой контейнер
        val rootLayout = FrameLayout(this)
        rootLayout.setBackgroundColor(DesignEngine.Theme.colorBackground)
        setContentView(rootLayout)

        // 1. ДАННЫЕ ПРОФИЛЯ
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val displayName = prefs.getString("display_name", "Пользователь") ?: "Пользователь"
        val status = prefs.getString("status", "") ?: "Нет статуса"
        val avatar = prefs.getString("avatar", "") ?: ""

        // 2. ШАПКА ПРОФИЛЯ (На заднем плане)
        val profileHeader = ProfileHeaderCell(this)
        profileHeader.setData(displayName, status, "https://blagogram.ru/avatars/$avatar")
        rootLayout.addView(profileHeader, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        // 3. СЛОЙ ЗАТЕМНЕНИЯ (Dim View)
        val dimView = View(this).apply {
            setBackgroundColor(Color.BLACK)
            alpha = 0f
        }
        rootLayout.addView(dimView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.dp(220)))

        // 4. СКРОЛЛ С НАСТРОЙКАМИ
        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // ПУСТОЕ МЕСТО (чтобы профиль был виден под скроллом)
        val headerVisibleHeight = LayoutHelper.dp(63)
        contentLayout.addView(SpacerCell(this, headerVisibleHeight))

        // БЕЛАЯ КАРТОЧКА С НАСТРОЙКАМИ
        val sheetContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = androidx.core.content.ContextCompat.getDrawable(this@SettingsActivity, R.drawable.bg_sheet_top_rounded)
            backgroundTintList = android.content.res.ColorStateList.valueOf(DesignEngine.Theme.colorSheetBackground)
            // Добавляем отступ снизу на высоту навигационной панели + запас
            setPadding(LayoutHelper.dp(20), LayoutHelper.dp(20), LayoutHelper.dp(20), navBarHeight + LayoutHelper.dp(40))
        }

        // Наполняем настройки (АККАУНТ)
        sheetContainer.addView(HeaderCell(this).apply { text = "АККАУНТ" })
        val accountCard = ContainerCell(this)
        accountCard.addCell(TextCell(this).apply {
            setData("Профиль", R.drawable.ic_edit)
            setOnClickListener { startActivity(android.content.Intent(context, ProfileEditActivity::class.java)) }
        })
        accountCard.addCell(TextCell(this).apply {
            setData("Lume Infinity", R.drawable.ic_premium_diamond)
            setIconColor(DesignEngine.COLOR_BLUE)
            setOnClickListener { startActivity(android.content.Intent(context, PremiumActivity::class.java)) }
        })
        accountCard.addCell(TextCell(this).apply {
            setData("Приватность", R.drawable.ic_user)
        }, isLast = true)
        sheetContainer.addView(accountCard)
        sheetContainer.addView(SpacerCell(this, LayoutHelper.dp(10)))

        // НАСТРОЙКИ
        sheetContainer.addView(HeaderCell(this).apply { text = "НАСТРОЙКИ" })
        val settingsCard = ContainerCell(this)
        settingsCard.addCell(TextCell(this).apply { setData("Настройки чатов", R.drawable.ic_chat) })
        settingsCard.addCell(TextCell(this).apply { setData("Уведомления и звуки", R.drawable.ic_music) })
        settingsCard.addCell(TextCell(this).apply {
            setData("Кастомизация", R.drawable.ic_brush)
            setOnClickListener { startActivity(android.content.Intent(context, ThemeSelectionActivity::class.java)) }
        })
        settingsCard.addCell(TextCell(this).apply {
            setData("AI Настройки", R.drawable.ic_ai)
            setOnClickListener { startActivity(android.content.Intent(context, AiSettingsActivity::class.java)) }
        }, isLast = true)
        sheetContainer.addView(settingsCard)
        sheetContainer.addView(SpacerCell(this, LayoutHelper.dp(10)))

        // ИНФОРМАЦИЯ И ВЫХОД
        sheetContainer.addView(HeaderCell(this).apply { text = "ИНФОРМАЦИЯ" })
        val infoCard = ContainerCell(this)
        infoCard.addCell(TextCell(this).apply {
            setData("О приложении", R.drawable.ic_more)
            setOnClickListener { startActivity(android.content.Intent(context, AboutActivity::class.java)) }
        })
        infoCard.addCell(TextCell(this).apply {
            val isDev = DevModeManager.isEnabled(this@SettingsActivity)
            setData("Разработчик: ${if (isDev) "ВКЛ" else "ВЫКЛ"}", R.drawable.ic_settings)
            if (isDev) setTextColor(DesignEngine.Theme.colorBlue)
            setOnClickListener {
                DevModeManager.setEnabled(this@SettingsActivity, !isDev)
                recreate()
            }
        }, isLast = true)
        sheetContainer.addView(infoCard)
        sheetContainer.addView(SpacerCell(this, LayoutHelper.dp(10)))

        val logoutCard = ContainerCell(this)
        logoutCard.addCell(TextCell(this).apply {
            setData("Выйти", R.drawable.ic_logout)
            setTextColor(DesignEngine.Theme.colorLogout)
            setIconColor(DesignEngine.Theme.colorLogout)
            setOnClickListener {
                getSharedPreferences("app", MODE_PRIVATE).edit().clear().apply()
                val intent = android.content.Intent(this@SettingsActivity, StartActivity::class.java)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }, isLast = true)
        sheetContainer.addView(logoutCard)

        contentLayout.addView(sheetContainer)
        scrollView.addView(contentLayout)
        rootLayout.addView(scrollView)

        // 5. ТУЛБАР (поверх всего)
        val toolbarLayout = RelativeLayout(this).apply {
            setPadding(LayoutHelper.dp(16), statusBarHeight + LayoutHelper.dp(10), LayoutHelper.dp(16), LayoutHelper.dp(10))
        }

        val backBtn = LinearLayout(this).apply {
            setBackgroundResource(R.drawable.bg_stat_pill)
            backgroundTintList = android.content.res.ColorStateList.valueOf(if (isDarkTheme) Color.parseColor("#333333") else Color.parseColor("#E0E0E0"))
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(6), LayoutHelper.dp(12), LayoutHelper.dp(6))
            setOnClickListener { finish() }
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_back)
                setColorFilter(if (isDarkTheme) Color.WHITE else Color.BLACK)
                layoutParams = LinearLayout.LayoutParams(LayoutHelper.dp(20), LayoutHelper.dp(20))
            })
        }

        val titleView = TextView(this).apply {
            text = "Настройки"
            textSize = 20f
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            typeface = Typeface.DEFAULT_BOLD
            alpha = 1f
        }

        val themeBtn = LinearLayout(this).apply {
            setBackgroundResource(R.drawable.bg_stat_pill)
            backgroundTintList = android.content.res.ColorStateList.valueOf(if (isDarkTheme) Color.parseColor("#333333") else Color.parseColor("#E0E0E0"))
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(6), LayoutHelper.dp(12), LayoutHelper.dp(6))
            setOnClickListener {
                themePrefs.edit().putBoolean("is_dark_theme", !isDarkTheme).apply()
                restartWithFade()
            }
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_moments)
                setColorFilter(if (isDarkTheme) Color.WHITE else Color.BLACK)
                layoutParams = LinearLayout.LayoutParams(LayoutHelper.dp(20), LayoutHelper.dp(20))
            })
        }

        val toolbarContent = RelativeLayout(this)
        toolbarContent.addView(backBtn, RelativeLayout.LayoutParams(-2, -2).apply { addRule(RelativeLayout.ALIGN_PARENT_START); addRule(RelativeLayout.CENTER_VERTICAL) })
        toolbarContent.addView(titleView, RelativeLayout.LayoutParams(-2, -2).apply { addRule(RelativeLayout.CENTER_IN_PARENT) })
        toolbarContent.addView(themeBtn, RelativeLayout.LayoutParams(-2, -2).apply { addRule(RelativeLayout.ALIGN_PARENT_END); addRule(RelativeLayout.CENTER_VERTICAL) })
        toolbarLayout.addView(toolbarContent)
        rootLayout.addView(toolbarLayout, FrameLayout.LayoutParams(-1, -2))

        // 6. ЛОГИКА СКРОЛЛА
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            val scrollY = scrollView.scrollY
            val progress = (scrollY.toFloat() / headerVisibleHeight).coerceIn(0f, 1f)
            dimView.alpha = progress * 0.6f
            dimView.translationY = profileHeader.translationY
        }
    }

    private fun getStatusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 0
    }

    private fun getNavigationBarHeight(): Int {
        val id = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 0
    }

    override fun onResume() {
        super.onResume()
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        if (themePrefs.getBoolean("is_dark_theme", false) != isCurrentThemeDark) restartWithFade()
    }

    private fun restartWithFade() {
        val intent = intent
        finish() // Закрываем текущую активити
        startActivity(intent) // Запускаем её же заново
        // Применяем стандартные анимации Android: fade_in (появление) и fade_out (затухание)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun animateThemeChange(startX: Int, startY: Int) {
        // 1. Делаем скриншот текущего состояния экрана
        val rootView = window.decorView
        rootView.isDrawingCacheEnabled = true
        val bitmap = Bitmap.createBitmap(rootView.drawingCache)
        rootView.isDrawingCacheEnabled = false

        // 2. Создаем временный ImageView и накладываем его поверх всего
        if (animImageView == null) {
            animImageView = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(-1, -1)
            }
        }
        val root = findViewById<FrameLayout>(android.R.id.content)
        animImageView?.setImageBitmap(bitmap)
        root.addView(animImageView)

        // 3. Мгновенно обновляем тему всех элементов "под" скриншотом
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        val isDark = themePrefs.getBoolean("is_dark_theme", false)
        val useMonet = themePrefs.getBoolean("use_monet", false)

        // Применяем новые цвета в движок
        DesignEngine.Theme.apply(isDark, useMonet)

        // Пересоздаем UI или обновляем цвета вручную.
        // В вашем случае проще вызвать recreate() БЕЗ анимации перехода,
        // но чтобы сохранить скриншот, мы просто вызовем перерисовку текущего Layout
        recreate()

        // Важно: чтобы круг сработал красиво, нужно передать координаты кнопки.
    }

}
