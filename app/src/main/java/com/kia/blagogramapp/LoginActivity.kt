package com.kia.blagogramapp

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.kia.blagogramapp.api.ApiClient
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

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

        // 3. Верстка UI кодом
        val rootLayout = FrameLayout(this)
        rootLayout.id = View.generateViewId()
        rootLayout.setBackgroundColor(DesignEngine.Theme.colorBackground)
        
        // Обработка Insets (клавиатура и бары)
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(
                view.paddingLeft,
                0,
                view.paddingRight,
                insets.bottom
            )
            WindowInsetsCompat.CONSUMED
        }

        setContentView(rootLayout)

        // --- Хедер (Кнопка назад) ---
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(LayoutHelper.dp(16), getStatusBarHeight() + LayoutHelper.dp(10), 0, 0)
        }
        
        val backBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_back)
            setColorFilter(DesignEngine.Theme.colorIconPrimary, PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
            
            val outValue = android.util.TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
            setBackgroundResource(outValue.resourceId)
            
            setOnClickListener { finish() }
        }
        headerLayout.addView(backBtn, LinearLayout.LayoutParams(LayoutHelper.dp(40), LayoutHelper.dp(40)))
        
        rootLayout.addView(headerLayout, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP
        })

        // --- Центральный контент ---
        val centerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // Убрали Gravity.CENTER_HORIZONTAL, чтобы элементы по умолчанию прижимались влево
            setPadding(LayoutHelper.dp(32), 0, LayoutHelper.dp(32), 0)
        }
        
        // Вход
        val titleView = TextView(this).apply {
            text = "Вход"
            textSize = 32f
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.START // Выравнивание текста влево
        }
        centerLayout.addView(titleView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = LayoutHelper.dp(8)
        })

        // Введите данные
        val subtitleView = TextView(this).apply {
            text = "Введите данные для входа"
            textSize = 16f
            setTextColor(Color.parseColor("#808080"))
            gravity = Gravity.START // Выравнивание текста влево
        }
        centerLayout.addView(subtitleView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = LayoutHelper.dp(32)
        })

        // Поле ЛОГИН
        val loginContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_chat_input_rounded)
            val inputBgColor = if (isDarkTheme) Color.parseColor("#2C2C2E") else Color.parseColor("#F5F5F5")
            backgroundTintList = android.content.res.ColorStateList.valueOf(inputBgColor)
            setPadding(LayoutHelper.dp(4), LayoutHelper.dp(4), LayoutHelper.dp(4), LayoutHelper.dp(4))
        }
        
        val userIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_user)
            setColorFilter(Color.parseColor("#808080"), PorterDuff.Mode.SRC_IN)
        }
        loginContainer.addView(userIcon, LinearLayout.LayoutParams(LayoutHelper.dp(24), LayoutHelper.dp(24)).apply {
            marginStart = LayoutHelper.dp(16)
        })
        
        val loginInput = EditText(this).apply {
            hint = "Логин"
            inputType = InputType.TYPE_CLASS_TEXT
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            setHintTextColor(Color.parseColor("#A0A0A0"))
            textSize = 18f
            background = null
            setPadding(LayoutHelper.dp(16), 0, LayoutHelper.dp(16), 0)
        }
        loginContainer.addView(loginInput, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LayoutHelper.dp(56)))
        
        centerLayout.addView(loginContainer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = LayoutHelper.dp(16)
        })

        // Поле ПАРОЛЬ
        val passwordContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_chat_input_rounded)
            val inputBgColor = if (isDarkTheme) Color.parseColor("#2C2C2E") else Color.parseColor("#F5F5F5")
            backgroundTintList = android.content.res.ColorStateList.valueOf(inputBgColor)
            setPadding(LayoutHelper.dp(4), LayoutHelper.dp(4), LayoutHelper.dp(4), LayoutHelper.dp(4))
        }
        
        val editIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_edit)
            setColorFilter(Color.parseColor("#808080"), PorterDuff.Mode.SRC_IN)
        }
        passwordContainer.addView(editIcon, LinearLayout.LayoutParams(LayoutHelper.dp(24), LayoutHelper.dp(24)).apply {
            marginStart = LayoutHelper.dp(16)
        })
        
        val passwordInput = EditText(this).apply {
            hint = "Пароль"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            setHintTextColor(Color.parseColor("#A0A0A0"))
            textSize = 18f
            background = null
            setPadding(LayoutHelper.dp(16), 0, LayoutHelper.dp(16), 0)
        }
        passwordContainer.addView(passwordInput, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LayoutHelper.dp(56)))
        
        centerLayout.addView(passwordContainer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        // Кнопка ВОЙТИ
        val btnLogin = Button(this).apply {
            text = "Войти"
            isAllCaps = false
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.cover_gradient)
            backgroundTintList = null 
        }
        centerLayout.addView(btnLogin, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LayoutHelper.dp(56)).apply {
            topMargin = LayoutHelper.dp(32)
        })
        
        // Ссылка РЕГИСТРАЦИЯ
        val txtRegister = TextView(this).apply {
            text = "Нет аккаунта? Зарегистрироваться"
            textSize = 14f
            setTextColor(Color.parseColor("#2196F3"))
            gravity = Gravity.CENTER
            setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://blagogram.ru/register.php?fromApp=1"))
                startActivity(intent)
            }
        }
        centerLayout.addView(txtRegister, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = LayoutHelper.dp(24)
            gravity = Gravity.CENTER_HORIZONTAL // Оставляем по центру
        })

        // Добавляем центральный лайаут в корень по центру
        rootLayout.addView(centerLayout, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
        })

        // 4. Логика (Deep link check)
        val data = intent?.data
        if (data != null && data.toString() == "blagogram://register_success") {
            Toast.makeText(this, "Регистрация прошла успешно!", Toast.LENGTH_LONG).show()
        }

        // 5. Логика входа
        btnLogin.setOnClickListener {
            val l = loginInput.text.toString().trim()
            val p = passwordInput.text.toString().trim()

            if (l.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Введите логин и пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val response = ApiClient.api.login(
                        login = l,
                        password = p
                    )

                    if (!response.ok) {
                        Toast.makeText(this@LoginActivity, response.error ?: "Ошибка", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    val result = response.result
                    if (result == null) {
                        Toast.makeText(this@LoginActivity, "Пустой ответ сервера", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    val token = result.token
                    val userId = result.user_id
                    val username = result.username
                    val displayName = result.display_name
                    val status = result.status

                    if (token.isEmpty() || userId == null) {
                        Toast.makeText(this@LoginActivity, "Некорректный ответ сервера", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    getSharedPreferences("app", MODE_PRIVATE).edit {
                        putString("token", token)
                        putInt("user_id", userId)
                        putString("avatar", result.avatar)
                        putString("login", username)
                        putString("display_name", displayName)
                        putString("status", status)
                    }

                    startActivity(Intent(this@LoginActivity, ChatsActivity::class.java))
                    finish()

                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "Ошибка сети: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
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
