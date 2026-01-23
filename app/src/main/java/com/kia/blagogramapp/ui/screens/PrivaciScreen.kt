package com.kia.blagogramapp.ui.screens

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.kia.blagogramapp.BaseScreen
import com.kia.blagogramapp.DesignEngine
import com.kia.blagogramapp.HeaderCell
import com.kia.blagogramapp.LayoutHelper
import com.kia.blagogramapp.R

class PrivaciScreen(context: Context) : BaseScreen(context) {
    val activity = context as? AppCompatActivity
    private var isCurrentThemeDark: Boolean = false
    private var isCurrentUseMonet: Boolean = false

    override fun createView(): View {
        val root = FrameLayout(context).apply {
            setBackgroundColor(DesignEngine.COLOR_BACKGROUND)
        }

        // 2. Получаем тему, так как она нужна для логики тулбара и кнопки "Разработчик"
        val themePrefs = context.getSharedPreferences("app_theme", MODE_PRIVATE)
        isCurrentThemeDark = themePrefs.getBoolean("is_dark_theme", false)
        isCurrentUseMonet = themePrefs.getBoolean("use_monet", false)

        // Тулбар
        val toolbar = DesignEngine.createToolBar(
            context = context,
            title = "Приватность",
            leftButtonIconRes = R.drawable.ic_back,
            leftButtonClickListener = { finishFragment() },
            isDark = isCurrentThemeDark
        )
        root.addView(toolbar)

        val scroll = ScrollView(context)
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16))
        }
        scroll.addView(content)
        root.addView(scroll)

        content.addView(HeaderCell(context).apply {
            text = "Кто может отправлять мне сообщения?"
        })

        // Контейнер для настроек
        val container = LinearLayout(context).apply {
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
            val optionLayout = LinearLayout(context).apply {
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
            val textView = TextView(context).apply {
                text = optionText
                setTextColor(DesignEngine.Theme.colorTextPrimary)
                textSize = 16f
                // Вес 1 заставляет его занять все свободное место
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            // Сама радио-кнопка (без текста)
            radioButton = RadioButton(context).apply {
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
        val descriptionText = TextView(context).apply {
            text = "Вы можете запретить входящие сообщения от пользователей, которых нет в Вашем списке контактов."
            textSize = 14f
            setTextColor(DesignEngine.Theme.colorTextSecondary)
            setPadding(LayoutHelper.dp(20), LayoutHelper.dp(8), LayoutHelper.dp(20), LayoutHelper.dp(8))
        }
        content.addView(descriptionText)

        return root
    }
}