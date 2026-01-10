package com.kia.blagogramapp

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.kia.blagogramapp.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AiSettingsActivity : AppCompatActivity() {
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
        window.navigationBarColor = Color.TRANSPARENT // Make nav bar transparent like in ChatsActivity

        // Root Layout
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(DesignEngine.Theme.colorBackground) // Theme color
        }
        setContentView(root)

        // Toolbar from DesignEngine
        val toolbar = DesignEngine.createToolBar(
            context = this,
            title = "Поиск",
            leftButtonIconRes = R.drawable.ic_back,
            leftButtonClickListener = { finish() },
            isDark = isDarkTheme
        )
        root.addView(toolbar)

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(20), LayoutHelper.dp(20), LayoutHelper.dp(20), LayoutHelper.dp(20))
        }
        scroll.addView(content)
        root.addView(scroll)

        val aiPrefs = getSharedPreferences("ai_settings", MODE_PRIVATE)

        content.addView(HeaderCell(this).apply { text = "API CONFIGURATION" })

        val container = ContainerCell(this)

        val urlInput = createInputCell("API URL", aiPrefs.getString("api_url", "https://api.openai.com/v1/chat/completions") ?: "https://api.openai.com/v1/chat/completions")
        container.addView(urlInput)
        
        val tokenInput = createInputCell("API Token", aiPrefs.getString("api_token", "") ?: "")
        container.addView(tokenInput)

        val modelInput = createInputCell("Model (only for OpenAI)", aiPrefs.getString("api_model", "gpt-4o-mini") ?: "gpt-4o-mini")
        container.addView(modelInput)

        content.addView(container)

        val saveBtn = Button(this).apply {
            text = "Сохранить"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorBlue)
                cornerRadius = LayoutHelper.dp(8).toFloat()
            }
            setOnClickListener {
                aiPrefs.edit()
                    .putString("api_url", urlInput.findViewById<EditText>(2).text.toString().trim())
                    .putString("api_token", tokenInput.findViewById<EditText>(2).text.toString().trim())
                    .putString("api_model", modelInput.findViewById<EditText>(2).text.toString().trim())
                    .apply()
                Toast.makeText(context, "Настройки сохранены", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        content.addView(SpacerCell(this, LayoutHelper.dp(20)))
        content.addView(saveBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50))

        // Пресеты
        val presetsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, LayoutHelper.dp(10), 0, 0)
        }

        val openAiBtn = Button(this).apply {
            text = "Сбросить на OpenAI"
            setTextColor(DesignEngine.Theme.colorTextSecondary)
            textSize = 13f
            background = null
            setOnClickListener {
                urlInput.findViewById<EditText>(2).setText("https://api.openai.com/v1/chat/completions")
                modelInput.findViewById<EditText>(2).setText("gpt-4o-mini")
            }
        }
        presetsLayout.addView(openAiBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        val geminiBtn = Button(this).apply {
            text = "Сбросить на Google Gemini (Native)"
            setTextColor(DesignEngine.Theme.colorTextSecondary)
            textSize = 13f
            background = null
            setOnClickListener {
                urlInput.findViewById<EditText>(2).setText("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent")
                modelInput.findViewById<EditText>(2).setText("gemini-1.5-flash")
            }
        }
        presetsLayout.addView(geminiBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        
        content.addView(presetsLayout)
        
        content.addView(SpacerCell(this, LayoutHelper.dp(40)))
        val clearBtn = Button(this).apply {
            text = "Очистить историю чата"
            setTextColor(DesignEngine.Theme.colorLogout)
            background = GradientDrawable().apply {
                setStroke(LayoutHelper.dp(1), DesignEngine.Theme.colorLogout)
                cornerRadius = LayoutHelper.dp(8).toFloat()
            }
            setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    AppDatabase.getDatabase(this@AiSettingsActivity).aiMessageDao().clearAll()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AiSettingsActivity, "История очищена", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        content.addView(clearBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50))
    }

    private fun createInputCell(hint: String, value: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(8), LayoutHelper.dp(16), LayoutHelper.dp(8))
            
            val label = TextView(context).apply {
                text = hint
                textSize = 12f
                setTextColor(DesignEngine.Theme.colorTextSecondary)
            }
            addView(label)

            val input = EditText(context).apply {
                id = 2
                setText(value)
                setTextColor(DesignEngine.Theme.colorTextPrimary)
                background = null
                setPadding(0, LayoutHelper.dp(4), 0, LayoutHelper.dp(4))
            }
            addView(input)
            
            val divider = View(context).apply {
                setBackgroundColor(DesignEngine.Theme.colorDivider)
            }
            addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1))
        }
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }
}
