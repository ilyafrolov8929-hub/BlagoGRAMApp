package com.kia.blagogramapp

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class ThemeSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = DesignEngine.initializeScreen(this)
        setContentView(rootLayout)

        val prefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        val useMonet = prefs.getBoolean("use_monet", false)
        val isDark = prefs.getBoolean("is_dark_theme", false)
        val useBottomNav = prefs.getBoolean("use_bottom_navigation", false)

        // Toolbar
        val toolbar = DesignEngine.createToolBar(
            context = this,
            title = "Кастомизация",
            leftButtonIconRes = R.drawable.ic_back,
            leftButtonClickListener = { finish() },
            isDark = isDark
        )
        rootLayout.addView(toolbar)

        // --- SECTION: THEME ---
        rootLayout.addView(HeaderCell(this).apply { text = "ТЕМА" }, 
            LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 16, 16, 16, 4))

        val scrollView = HorizontalScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
            scrollBarSize = 0
        }

        val cardsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(LayoutHelper.dp(16), 0, LayoutHelper.dp(16), 0)
        }

        cardsContainer.addView(ThemePreviewCell(this, "Светлая", 0xFFF5F5F5.toInt(), 0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF007AFF.toInt()).apply {
            isSelectedTheme = !isDark
            setOnClickListener { if (isDark) { prefs.edit().putBoolean("is_dark_theme", false).apply(); recreateActivity() } }
        }, LayoutHelper.createLinear(110, 180, 0f, 0, 8, 0, 8, 0))

        cardsContainer.addView(ThemePreviewCell(this, "Тёмная", 0xFF1A1A1A.toInt(), 0xFF2C2C2E.toInt(), 0xFFFFFFFF.toInt(), 0xFF0A84FF.toInt()).apply {
            isSelectedTheme = isDark
            setOnClickListener { if (!isDark) { prefs.edit().putBoolean("is_dark_theme", true).apply(); recreateActivity() } }
        }, LayoutHelper.createLinear(110, 180, 0f, 0, 8, 0, 8, 0))

        scrollView.addView(cardsContainer)
        rootLayout.addView(scrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 8, 0, 24))

        // --- SECTION: NAVIGATION ---
        rootLayout.addView(HeaderCell(this).apply { text = "ИНТЕРФЕЙС" }, 
            LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 16, 0, 16, 4))

        val navSwitch = createSwitchItem("Нижняя навигация", "Как в VK / Telegram iOS", useBottomNav) { isChecked ->
            prefs.edit().putBoolean("use_bottom_navigation", isChecked).apply()
            recreateActivity()
        }
        rootLayout.addView(navSwitch, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 16, 8, 16, 8))

        // Monet Switch
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val monetSwitch = createSwitchItem("Динамические цвета", "Использовать цвета обоев (Monet)", useMonet) { isChecked ->
                prefs.edit().putBoolean("use_monet", isChecked).apply()
                recreateActivity()
            }
            rootLayout.addView(monetSwitch, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 16, 8, 16, 0))
        }
    }

    private fun createSwitchItem(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorCardBackground)
                cornerRadius = LayoutHelper.dp(12).toFloat()
            }
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(12), LayoutHelper.dp(16), LayoutHelper.dp(12))

            val textLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }
            textLayout.addView(TextView(context).apply {
                text = title
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(DesignEngine.Theme.colorTextPrimary)
            })
            textLayout.addView(TextView(context).apply {
                text = subtitle
                textSize = 13f
                setTextColor(DesignEngine.Theme.colorTextSecondary)
            })
            addView(textLayout, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f))

            addView(Switch(context).apply {
                isChecked = checked
                thumbTintList = android.content.res.ColorStateList.valueOf(DesignEngine.Theme.colorBlue)
                setOnCheckedChangeListener { _, isChecked -> onChecked(isChecked) }
            })
        }
    }

    private fun recreateActivity() {
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        startActivity(intent)
    }
}

class ThemePreviewCell(
    context: Context, 
    title: String,
    previewBg: Int,
    previewCard: Int,
    previewText: Int,
    previewAccent: Int
) : LinearLayout(context) {

    var isSelectedTheme: Boolean = false
        set(value) {
            field = value
            updateSelection()
        }

    private val container: FrameLayout
    private val checkIcon: ImageView
    private val strokeColor = DesignEngine.Theme.colorBlue

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL

        container = FrameLayout(context).apply {
            elevation = LayoutHelper.dp(4).toFloat()
        }
        
        val screen = LinearLayout(context).apply {
            orientation = VERTICAL
            background = GradientDrawable().apply {
                setColor(previewBg)
                cornerRadius = LayoutHelper.dp(12).toFloat()
                setStroke(LayoutHelper.dp(2), if(isSelectedTheme) strokeColor else Color.TRANSPARENT)
            }
            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
        }
        
        val miniHeader = View(context).apply {
            background = GradientDrawable().apply {
                setColor(previewCard)
                cornerRadius = LayoutHelper.dp(4).toFloat()
            }
        }
        screen.addView(miniHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 12, 0f, 0, 0, 0, 0, 8))
        
        val bubbleRight = View(context).apply {
            background = GradientDrawable().apply {
                setColor(previewAccent)
                cornerRadii = floatArrayOf(20f, 20f, 20f, 20f, 0f, 0f, 20f, 20f)
            }
        }
        screen.addView(bubbleRight, LayoutHelper.createLinear(60, 20, 0f, Gravity.END, 0, 0, 0, 4))
        
        val bubbleLeft = View(context).apply {
            background = GradientDrawable().apply {
                setColor(previewCard)
                cornerRadii = floatArrayOf(20f, 20f, 20f, 20f, 20f, 20f, 0f, 0f)
            }
        }
        screen.addView(bubbleLeft, LayoutHelper.createLinear(50, 20, 0f, Gravity.START, 0, 0, 0, 8))

        val bottomBar = View(context).apply {
             background = GradientDrawable().apply {
                setColor(previewCard)
                cornerRadius = LayoutHelper.dp(10).toFloat()
            }
        }
        screen.addView(bottomBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 16, 0f, Gravity.BOTTOM, 0, 20, 0, 0))

        container.addView(screen, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))
        
        checkIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_edit)
            setColorFilter(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(strokeColor)
                shape = GradientDrawable.OVAL
            }
            setPadding(4,4,4,4)
            visibility = View.GONE
            elevation = LayoutHelper.dp(6).toFloat()
        }
        container.addView(checkIcon, LayoutHelper.createFrame(24, 24, Gravity.BOTTOM or Gravity.END, 0, 0, 6, 6))

        addView(container, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 150))

        val titleView = TextView(context).apply {
            text = title
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            gravity = Gravity.CENTER
        }
        addView(titleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 12, 0, 0))
    }

    private fun updateSelection() {
        val bg = (container.getChildAt(0) as View).background as GradientDrawable
        if (isSelectedTheme) {
            bg.setStroke(LayoutHelper.dp(3), strokeColor)
            checkIcon.visibility = View.VISIBLE
            alpha = 1.0f
        } else {
            bg.setStroke(LayoutHelper.dp(1), 0xFFB0B0B0.toInt())
            checkIcon.visibility = View.GONE
        }
    }
}
