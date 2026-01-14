package com.kia.blagogramapp.ui.modules

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kia.blagogramapp.*
import com.kia.blagogramapp.R

class BottomNavigationModule(val context: Context) {

    // Оставляем 3 основных раздела
    enum class Screen { CHATS, MUSIC, SETTINGS, MENU }

    private lateinit var wrapper: FrameLayout
    private lateinit var rootLayout: LinearLayout

    fun create(root: FrameLayout, currentScreen: Screen): BottomNavigationModule {
        if (!DesignEngine.useBottomNavigation) return this

        wrapper = FrameLayout(context).apply {
            setPadding(LayoutHelper.dp(24), 0, LayoutHelper.dp(24), LayoutHelper.dp(24))
            translationZ = LayoutHelper.dp(30).toFloat()
            tag = "bottom_nav_wrapper" // Добавляем тег для поиска в активити
        }

        rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            background = GradientDrawable().apply {
                val bgColor = DesignEngine.Theme.colorSheetBackground
                setColor(bgColor)
                cornerRadius = LayoutHelper.dp(32).toFloat()
                setStroke(LayoutHelper.dp(1), Color.parseColor("#1AFFFFFF"))
            }

            elevation = LayoutHelper.dp(12).toFloat()
        }

        addStandardItem(R.drawable.ic_chat, Screen.CHATS, currentScreen)
        addStandardItem(R.drawable.ic_music, Screen.MUSIC, currentScreen)
        addStandardItem(R.drawable.ic_menu, Screen.MENU, currentScreen)
        addStandardItem(R.drawable.ic_settings, Screen.SETTINGS, currentScreen)

        wrapper.addView(rootLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 60))
        root.addView(wrapper, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM))

        return this
    }

    private fun addStandardItem(iconRes: Int, screen: Screen, currentScreen: Screen) {
        val isSelected = screen == currentScreen

        val itemLayout = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LayoutHelper.MATCH_PARENT, 1f)
            setOnClickListener {
                if (isSelected) return@setOnClickListener
                val intent = when (screen) {
                    Screen.CHATS -> Intent(context, ChatsActivity::class.java)
                    Screen.MUSIC -> Intent(context, MusicActivity::class.java)
                    Screen.MENU -> Intent(context, MenuActivity::class.java)
                    Screen.SETTINGS -> Intent(context, SettingsActivity::class.java)
                }
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                context.startActivity(intent)
            }
        }

        if (isSelected) {
            val selectedPill = View(context).apply {
                background = GradientDrawable().apply {
                    val isDark = DesignEngine.Theme.colorBackground == 0xFF1A1A1A.toInt()
                    setColor(if (isDark) 0x26FFFFFF else 0x1A000000)
                    cornerRadius = LayoutHelper.dp(18).toFloat()
                }
            }
            itemLayout.addView(selectedPill, LayoutHelper.createFrame(56, 40, Gravity.CENTER))
        }

        // Контейнер иконки (всегда в центре)
        val iconContainer = FrameLayout(context)
        val iconView = ImageView(context).apply {
            setImageResource(iconRes)
            setColorFilter(
                if (isSelected) DesignEngine.Theme.colorBlue else DesignEngine.Theme.colorIconPrimary,
                PorterDuff.Mode.SRC_IN
            )
        }

        itemLayout.addView(iconView, LayoutHelper.createFrame(24, 24, Gravity.CENTER))
        rootLayout.addView(itemLayout)
    }

    fun setVisibility(visible: Boolean) {
        if (!::wrapper.isInitialized) return
        val translationY = if (visible) 0f else LayoutHelper.dp(150).toFloat()
        wrapper.animate()
            .translationY(translationY)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }
}
