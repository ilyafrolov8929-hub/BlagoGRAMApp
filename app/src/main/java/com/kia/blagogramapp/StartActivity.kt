package com.kia.blagogramapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat

class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Инициализация тем
        DesignEngine.init(this)
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        val isDarkTheme = themePrefs.getBoolean("is_dark_theme", false)
        DesignEngine.Theme.apply(isDarkTheme)
        
        // 2. Настройка окна
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val window = window
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !isDarkTheme
        controller.isAppearanceLightNavigationBars = !isDarkTheme

        // 3. Проверка токена
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val savedToken = prefs.getString("token", "")

        if (!savedToken.isNullOrEmpty()) {
            startActivity(Intent(this, ChatsActivity::class.java))
            finish()
            return
        }
        
        // 4. Верстка UI кодом
        val rootLayout = FrameLayout(this)
        rootLayout.setBackgroundColor(DesignEngine.Theme.colorBackground)
        
        // Центральный контейнер
        val centerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(LayoutHelper.dp(32), LayoutHelper.dp(32), LayoutHelper.dp(32), LayoutHelper.dp(32))
        }
        
        // Логотип
        val logoView = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
        }
        centerLayout.addView(logoView, LinearLayout.LayoutParams(LayoutHelper.dp(120), LayoutHelper.dp(120)).apply {
            bottomMargin = LayoutHelper.dp(24)
        })
        
        // Название (Градиент)
        // Используем существующий класс GradientTextView, так как он найден в проекте
        // Если он не наследуется от View программно, придется использовать TextView и красить Shader'ом
        // Попробуем создать его программно.
        val titleView = com.kia.blagogramapp.views.GradientTextView(this).apply {
            text = "BlagoGRAM"
            textSize = 40f
            // font/orbitron_black
            try {
                typeface = ResourcesCompat.getFont(context, R.font.orbitron_black)
            } catch (e: Exception) {
                typeface = Typeface.DEFAULT_BOLD
            }
            gravity = Gravity.CENTER
            letterSpacing = 0.05f
        }
        centerLayout.addView(titleView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = LayoutHelper.dp(16)
        })
        
        // Подзаголовок
        val subtitleView = TextView(this).apply {
            text = "Быстрый. Безопасный. Твой."
            textSize = 16f
            setTextColor(Color.parseColor("#808080")) // Серый всегда
            gravity = Gravity.CENTER
        }
        centerLayout.addView(subtitleView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = LayoutHelper.dp(48)
        })
        
        // Кнопка "Начать общение"
        val startButton = Button(this).apply {
            text = "Начать общение"
            textSize = 18f
            setTextColor(Color.WHITE)
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            // Фон градиент из drawable
            setBackgroundResource(R.drawable.btn_gradient)
            // Убираем backgroundTint, если он вдруг применится
            backgroundTintList = null
            
            setOnClickListener {
                startActivity(Intent(this@StartActivity, LoginActivity::class.java))
            }
        }
        centerLayout.addView(startButton, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LayoutHelper.dp(56)))
        
        // Добавляем центральный контейнер в корень (по центру)
        val centerParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
        }
        rootLayout.addView(centerLayout, centerParams)
        
        // Копирайт (внизу)
        val copyrightView = TextView(this).apply {
            text = "© 2025 BlagoGRAM Inc."
            textSize = 12f
            setTextColor(Color.parseColor("#A0A0A0"))
        }
        val copyrightParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = LayoutHelper.dp(32)
        }
        rootLayout.addView(copyrightView, copyrightParams)
        
        setContentView(rootLayout)
    }
}
