package com.kia.blagogramapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.kia.blagogramapp.SingleActivity.NavigationLayout
import com.kia.blagogramapp.ui.screens.AboutScreen
import com.kia.blagogramapp.ui.screens.ChatsScreen
import com.kia.blagogramapp.ui.screens.StartScreen
import com.kia.blagogramapp.utils.MusicManager

class MainActivity : AppCompatActivity() {
    lateinit var navigationLayout: NavigationLayout
    
    private var isCurrentThemeDark: Boolean = false
    private var isCurrentUseMonet: Boolean = false

    val musicPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { MusicManager.addTrack(this, it) }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализируем настройки темы
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        isCurrentThemeDark = themePrefs.getBoolean("is_dark_theme", false)
        isCurrentUseMonet = themePrefs.getBoolean("use_monet", false)

        // Инициализируем движок дизайна
        DesignEngine.initializeScreenWithFrameLayout(this)

        // Создаем контейнер
        navigationLayout = NavigationLayout(this)
        setContentView(navigationLayout)

        // 3. ПРОВЕРКА ТОКЕНА (Твой запрос)
        if (savedInstanceState == null) {
            val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)
            val token = prefs.getString("token", "")

            if (!token.isNullOrEmpty()) {
                // Если токен есть — сразу грузим экран чатов
                navigationLayout.presentFragment(ChatsScreen(this))
            } else {
                // Если токена нет — грузим экран приветствия
                navigationLayout.presentFragment(StartScreen(this))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Проверяем смену темы глобально
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        val isNowDark = themePrefs.getBoolean("is_dark_theme", false)
        val isNowMonet = themePrefs.getBoolean("use_monet", false)

        if (isNowDark != isCurrentThemeDark || isNowMonet != isCurrentUseMonet) {
            recreate()
            return
        }

        // 2. ПРОБРОС СОБЫТИЯ В ТЕКУЩИЙ ЭКРАН
        navigationLayout.getVisibleScreen()?.onResume()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Находим текущий активный экран в стеке
        val currentScreen = navigationLayout.getVisibleScreen()

        navigationLayout.getVisibleScreen()?.handleActivityResult(requestCode, resultCode, data)

    }

    // В MainActivity.kt

    fun applyAppTheme(isDark: Boolean, useMonet: Boolean) {
        // 1. Сохраняем в префсы
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        themePrefs.edit()
            .putBoolean("is_dark_theme", isDark)
            .putBoolean("use_monet", useMonet)
            .apply()

        // 2. Обновляем локальные флаги, чтобы onResume не дергал recreate()
        this.isCurrentThemeDark = isDark
        this.isCurrentUseMonet = useMonet

        // 3. ПРИМЕНЯЕМ ЦВЕТА В ДВИЖОК
        DesignEngine.Theme.apply(isDark, useMonet)

        // 4. ОБНОВЛЯЕМ СИСТЕМНЫЕ БАРЫ (Время, батарея, навигация)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !isDark
        controller.isAppearanceLightNavigationBars = !isDark

        // 5. Помечаем стек на обновление
        navigationLayout.updateThemeInStack()
    }

    override fun onBackPressed() {
        if (!navigationLayout.onBackPressed()) {
            super.onBackPressed()
        }
    }
}
