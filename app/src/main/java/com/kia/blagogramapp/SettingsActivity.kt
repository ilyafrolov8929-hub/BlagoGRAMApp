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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.kia.blagogramapp.utils.DevModeManager

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

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !isDarkTheme
        controller.isAppearanceLightNavigationBars = !isDarkTheme

        val rootLayout = FrameLayout(this)
        rootLayout.setBackgroundColor(DesignEngine.Theme.colorBackground)
        setContentView(rootLayout)

        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val displayName = prefs.getString("display_name", "Пользователь") ?: "Пользователь"
        val status = prefs.getString("status", "") ?: "Нет статуса"
        val avatar = prefs.getString("avatar", "") ?: ""

        val profileHeader = ProfileHeaderCell(this)
        profileHeader.setData(displayName, status, "https://blagogram.ru/avatars/$avatar")
        rootLayout.addView(profileHeader, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        val dimView = View(this).apply {
            setBackgroundColor(Color.BLACK)
            alpha = 0f
        }
        rootLayout.addView(dimView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.dp(220)))

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val headerVisibleHeight = LayoutHelper.dp(63)
        contentLayout.addView(SpacerCell(this, headerVisibleHeight + 10))

        val sheetContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = androidx.core.content.ContextCompat.getDrawable(this@SettingsActivity, R.drawable.bg_sheet_top_rounded)
            backgroundTintList = android.content.res.ColorStateList.valueOf(DesignEngine.Theme.colorSheetBackground)
            setPadding(LayoutHelper.dp(20), LayoutHelper.dp(20), LayoutHelper.dp(20), LayoutHelper.dp(40))
        }

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
            setOnClickListener { startActivity(android.content.Intent(context, PrivaciActivity::class.java)) }
        }, isLast = true)
        sheetContainer.addView(accountCard)
        sheetContainer.addView(SpacerCell(this, LayoutHelper.dp(10)))

        sheetContainer.addView(HeaderCell(this).apply { text = "НАСТРОЙКИ" })
        val settingsCard = ContainerCell(this)
        settingsCard.addCell(TextCell(this).apply {
            setData("Настройки чатов", R.drawable.ic_chat)
            setOnClickListener { startActivity(android.content.Intent(context, ChatSettingsActivity::class.java)) }
        })
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

        sheetContainer.addView(HeaderCell(this).apply { text = "ИНФОРМАЦИЯ" })
        val infoCard = ContainerCell(this)
        infoCard.addCell(TextCell(this).apply {
            setData("Данные и память", R.drawable.ic_data_and_storage)
            setOnClickListener { startActivity(android.content.Intent(context, StatisticsActivity::class.java)) }
        })
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

        val toolbar = DesignEngine.createToolBar(
            context = this,
            title = "Настройки",
            leftButtonIconRes = R.drawable.ic_back,
            isDark = isDarkTheme,
            rightButtonIconRes = R.drawable.ic_moments,
            leftButtonClickListener = { finish() },
            rightButtonClickListener = {
                themePrefs.edit().putBoolean("is_dark_theme", !isDarkTheme).apply()
                restartWithFade()
            }
        )
        rootLayout.addView(toolbar, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP))

        scrollView.viewTreeObserver.addOnScrollChangedListener {
            val scrollY = scrollView.scrollY
            val progress = (scrollY.toFloat() / headerVisibleHeight).coerceIn(0f, 1f)
            dimView.alpha = progress * 0.6f
            dimView.translationY = profileHeader.translationY
        }
    }

    override fun onResume() {
        super.onResume()
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        if (themePrefs.getBoolean("is_dark_theme", false) != isCurrentThemeDark) restartWithFade()
    }

    private fun restartWithFade() {
        val intent = intent
        finish() 
        startActivity(intent) 
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun animateThemeChange(startX: Int, startY: Int) {
        val rootView = window.decorView
        rootView.isDrawingCacheEnabled = true
        val bitmap = Bitmap.createBitmap(rootView.drawingCache)
        rootView.isDrawingCacheEnabled = false

        if (animImageView == null) {
            animImageView = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(-1, -1)
            }
        }
        val root = findViewById<FrameLayout>(android.R.id.content)
        animImageView?.setImageBitmap(bitmap)
        root.addView(animImageView)

        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        val isDark = themePrefs.getBoolean("is_dark_theme", false)
        val useMonet = themePrefs.getBoolean("use_monet", false)

        DesignEngine.Theme.apply(isDark, useMonet)

        recreate()
    }
}