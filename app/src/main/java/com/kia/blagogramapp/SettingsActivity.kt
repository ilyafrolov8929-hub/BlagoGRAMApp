package com.kia.blagogramapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import com.kia.blagogramapp.ui.modules.BottomNavigationModule
import com.kia.blagogramapp.utils.DevModeManager

class SettingsActivity : AppCompatActivity() {
    private var isCurrentThemeDark: Boolean = false
    private var isCurrentUseMonet: Boolean = false
    private var animImageView: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Инициализируем экран и сразу получаем готовый корневой layout
        val rootLayout = DesignEngine.initializeScreenWithFrameLayout(this)
        setContentView(rootLayout)

        // 2. Получаем тему, так как она нужна для логики тулбара и кнопки "Разработчик"
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        isCurrentThemeDark = themePrefs.getBoolean("is_dark_theme", false)
        isCurrentUseMonet = themePrefs.getBoolean("use_monet", false)

        // Автоматическая настройка нижней навигации
        BottomNavigationModule(this).create(rootLayout, BottomNavigationModule.Screen.SETTINGS)

        // --- Дальше идет ваш код по наполнению rootLayout ---
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val displayName = prefs.getString("display_name", "Пользователь") ?: "Пользователь"
        val status = prefs.getString("status", "") ?: "Нет статуса"
        val phone = prefs.getString("phone", "") ?: "Не известно"
        val avatar = prefs.getString("avatar", "") ?: ""
        val username = prefs.getString("login", "")
        val isPremium = prefs.getBoolean("is_premium", false)


        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
            if (DesignEngine.useBottomNavigation) {
                setPadding(0, 0, 0, LayoutHelper.dp(80))
            }
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        contentLayout.addView(SpacerCell(this, LayoutHelper.dp(35)))

        // Обертка для шапки профиля, чтобы были такие же отступы по бокам как у карточек ниже
        val profileContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(20), 0, LayoutHelper.dp(20), 0)
        }

        val profileHeader = ProfileHeaderCell(this)
        profileHeader.setData(displayName, status, "https://blagogram.ru/avatars/$avatar", isPremium)
        profileContainer.addView(profileHeader)

        contentLayout.addView(profileContainer)
        val sheetContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // Убран фон и наложение, теперь элементы просто идут друг за другом
            setPadding(LayoutHelper.dp(20), LayoutHelper.dp(10), LayoutHelper.dp(20), LayoutHelper.dp(40))
        }

        contentLayout.addView(SpacerCell(this, LayoutHelper.dp(10)))

        sheetContainer.addView(HeaderCell(this).apply { text = "Аккаунт" })
        val infoUserCard = ContainerCell(this)

        infoUserCard.addCell(TextDetailCell(this).apply {
            setData(phone, "Нажмите, чтобы изменить номер телефона")
            // Здесь можно добавить переход на смену номера, если нужно
        })

        infoUserCard.addCell(TextDetailCell(this).apply {
            setData("@$username", "Ваш юзернейм")
        })

        infoUserCard.addCell(TextDetailCell(this).apply {
            setData(status, "О себе")
        }, isLast = true)

        sheetContainer.addView(infoUserCard)
        sheetContainer.addView(SpacerCell(this, LayoutHelper.dp(10)))

        sheetContainer.addView(HeaderCell(this).apply { text = "Настройки" })
        val accountCard = ContainerCell(this)
        accountCard.addCell(TextCell(this).apply {
            setData("Редактировать профиль", R.drawable.ic_edit)
            setOnClickListener { startActivity(Intent(context, ProfileEditActivity::class.java)) }
        })
        accountCard.addCell(TextCell(this).apply {
            setData("Lume Infinity", R.drawable.ic_premium_diamond)
            setIconColor(DesignEngine.COLOR_BLUE)
            setOnClickListener { startActivity(Intent(context, PremiumActivity::class.java)) }
        })
        accountCard.addCell(TextCell(this).apply {
            setData("Приватность", R.drawable.ic_user)
            setOnClickListener { startActivity(Intent(context, PrivaciActivity::class.java)) }
        }, isLast = true)
        accountCard.addCell(TextCell(this).apply {
            setData("Настройки чатов", R.drawable.ic_chat)
            setOnClickListener { startActivity(Intent(context, ChatSettingsActivity::class.java)) }
        })
        accountCard.addCell(TextCell(this).apply {
            setData("Уведомления и звуки", R.drawable.ic_music) })
        accountCard.addCell(TextCell(this).apply {
            setData("Кастомизация", R.drawable.ic_brush)
            setOnClickListener { startActivity(Intent(context, ThemeSelectionActivity::class.java)) }
        })
        accountCard.addCell(TextCell(this).apply {
            setData("AI Настройки", R.drawable.ic_ai)
            setOnClickListener { startActivity(Intent(context, AiSettingsActivity::class.java)) }
        }, isLast = true)
        sheetContainer.addView(accountCard)
        sheetContainer.addView(SpacerCell(this, LayoutHelper.dp(10)))

        sheetContainer.addView(HeaderCell(this).apply { text = "ИНФОРМАЦИЯ" })
        val infoCard = ContainerCell(this)
        infoCard.addCell(TextCell(this).apply {
            setData("Данные и память", R.drawable.ic_data_and_storage)
            setOnClickListener { startActivity(Intent(context, StatisticsActivity::class.java)) }
        })
        infoCard.addCell(TextCell(this).apply {
            setData("О приложении", R.drawable.ic_more)
            setOnClickListener { startActivity(Intent(context, AboutActivity::class.java)) }
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
                val intent = Intent(this@SettingsActivity, StartActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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
            isDark = isCurrentThemeDark,
            rightButtonIconRes = R.drawable.ic_moments,
            leftButtonClickListener = { finish() },
            rightButtonClickListener = {
                themePrefs.edit().putBoolean("is_dark_theme", !isCurrentThemeDark).apply()
                restartWithFade()
            }
        )
        rootLayout.addView(toolbar, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP))
    }

    override fun onResume() {
        super.onResume()
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        if (themePrefs.getBoolean("is_dark_theme", false) != isCurrentThemeDark ||
            themePrefs.getBoolean("use_bottom_navigation", false) != DesignEngine.useBottomNavigation) {
            restartWithFade()
        }
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
