package com.kia.blagogramapp

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

class PrivaciActivity : AppCompatActivity() {
    private var isCurrentThemeDark: Boolean = false
    private var isCurrentUseMonet: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DesignEngine.init(this)
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        val isDarkTheme = themePrefs.getBoolean("is_dark_theme", false)
        val useMonet = themePrefs.getBoolean("use_monet", false)

        isCurrentThemeDark = isDarkTheme
        isCurrentUseMonet = useMonet

        // Enable Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        // Рут экран
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(DesignEngine.Theme.colorBackground)
        }
        setContentView(root)

        // Тулбар
        val toolbar = DesignEngine.createToolBar(
            context = this,
            title = "Приватность",
            leftButtonIconRes = R.drawable.ic_back,
            leftButtonClickListener = { finish() },
            isDark = isDarkTheme
        )
        root.addView(toolbar)

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16))
        }
        scroll.addView(content)
        root.addView(scroll)

        content.addView(HeaderCell(this).apply {
            text = "Кто может отправлять мне сообщения?"
        })

        // Контейнер для настроек
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorCardBackground)
                cornerRadius = LayoutHelper.dp(12).toFloat()
            }
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16))
        }
        content.addView(container)


        // --- НАЧАЛО БЛОКА С РАДИО-КНОПКАМИ (ИСПРАВЛЕННЫЙ КОД) ---

        // Список для хранения ссылок на радио-кнопки
        val radioButtons = mutableListOf<RadioButton>()

        val options = listOf("Все", "Контакты и Infinity-подписчики")
        options.forEachIndexed { index, optionText ->
            // Объявляем radioButton здесь, чтобы она была доступна в setOnClickListener
            lateinit var radioButton: RadioButton

            // Создаем горизонтальный контейнер для текста и кнопки
            val optionLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = if (index > 0) LayoutHelper.dp(16) else 0
                }

                // Устанавливаем слушатель кликов на весь layout
                setOnClickListener {
                    // Сначала снимаем выбор со всех кнопок в нашем списке
                    radioButtons.forEach { it.isChecked = false }
                    // Затем выбираем ту, на которую нажали
                    radioButton.isChecked = true
                }
            }

            // Текст опции
            val textView = TextView(this).apply {
                text = optionText
                setTextColor(DesignEngine.Theme.colorTextPrimary)
                textSize = 16f
                // Вес 1 заставляет его занять все свободное место
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            // Сама радио-кнопка (без текста)
            radioButton = RadioButton(this).apply {
                // Клик обрабатывается родительским LinearLayout, поэтому на самой кнопке его отключаем
                isClickable = false
                // Стилизация кружка
                buttonTintList = ColorStateList(
                    arrayOf(
                        intArrayOf(-android.R.attr.state_checked), // не выбрана
                        intArrayOf(android.R.attr.state_checked)   // выбрана
                    ),
                    intArrayOf(
                        DesignEngine.Theme.colorTextSecondary, // Цвет неактивной кнопки
                        DesignEngine.Theme.colorBlue           // Цвет активной кнопки
                    )
                )
            }
            // Добавляем созданную кнопку в наш список для управления
            radioButtons.add(radioButton)

            // Добавляем текст и кнопку в горизонтальный layout
            optionLayout.addView(textView)
            optionLayout.addView(radioButton)

            // Добавляем готовый layout опции в главный серый контейнер
            container.addView(optionLayout)

            // Выбираем первую опцию по умолчанию
            if (index == 0) {
                radioButton.isChecked = true
            }
        }

        // --- КОНЕЦ БЛОКА С РАДИО-КНОПКАМИ ---


        // Текст с описанием под контейнером
        val descriptionText = TextView(this).apply {
            text = "Вы можете запретить входящие сообщения от пользователей, которых нет в Вашем списке контактов."
            textSize = 14f
            setTextColor(DesignEngine.Theme.colorTextSecondary)
            setPadding(LayoutHelper.dp(20), LayoutHelper.dp(8), LayoutHelper.dp(20), LayoutHelper.dp(8))
        }
        content.addView(descriptionText)
    }
}
