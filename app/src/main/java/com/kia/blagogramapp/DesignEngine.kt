package com.kia.blagogramapp

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import coil.load
import com.google.android.material.imageview.ShapeableImageView

/**
 * Аналог класса LayoutHelper из Telegram.
 * Упрощает создание LayoutParams кодом.
 */
object LayoutHelper {
    const val MATCH_PARENT = -1
    const val WRAP_CONTENT = -2

    fun createLinear(width: Int, height: Int, weight: Float = 0f, gravity: Int = 0, left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0): LinearLayout.LayoutParams {
        val layoutParams = LinearLayout.LayoutParams(getSize(width), getSize(height))
        layoutParams.weight = weight
        layoutParams.gravity = gravity
        layoutParams.setMargins(dp(left), dp(top), dp(right), dp(bottom))
        return layoutParams
    }

    fun createFrame(width: Int, height: Int, gravity: Int = Gravity.TOP or Gravity.START, left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0): FrameLayout.LayoutParams {
        val layoutParams = FrameLayout.LayoutParams(getSize(width), getSize(height))
        layoutParams.gravity = gravity
        layoutParams.setMargins(dp(left), dp(top), dp(right), dp(bottom))
        return layoutParams
    }

    private fun getSize(size: Int): Int {
        return if (size < 0) size else dp(size)
    }

    fun dp(value: Int): Int {
        if (value == 0) return 0
        return (value * DesignEngine.density).toInt()
    }
}

/**
 * Глобальные настройки темы и утилиты.
 */
object DesignEngine {
    var density: Float = 1f

    fun init(context: Context) {
        density = context.resources.displayMetrics.density
    }

    // Совместимость со старым кодом: эти константы теперь перенаправляют на текущую тему
    val COLOR_BACKGROUND get() = Theme.colorBackground
    val COLOR_SHEET_BACKGROUND get() = Theme.colorSheetBackground
    val COLOR_CARD_BACKGROUND get() = Theme.colorCardBackground
    val COLOR_PROFILE_NAME get() = Theme.colorProfileName
    val COLOR_PROFILE_STATUS get() = Theme.colorProfileStatus
    val COLOR_HEADER get() = Theme.colorHeader
    val COLOR_TEXT_PRIMARY get() = Theme.colorTextPrimary
    val COLOR_TEXT_SECONDARY get() = Theme.colorTextSecondary
    val COLOR_ICON_PRIMARY get() = Theme.colorIconPrimary
    val COLOR_CHEVRON get() = Theme.colorChevron
    val COLOR_DIVIDER get() = Theme.colorDivider
    val COLOR_LOGOUT get() = Theme.colorLogout
    
    // Новая константа для акцентного цвета
    val COLOR_BLUE get() = Theme.colorBlue

    /**
     * Движок тем. Хранит текущие цвета и умеет их переключать.
     */
    object Theme {
        // Значения по умолчанию (Светлая тема)
        var colorBackground: Int = 0xFFf7f7f7.toInt()
        var colorSheetBackground: Int = 0xFFE6E6E6.toInt()
        var colorCardBackground: Int = 0xFFFFFFFF.toInt()
        var colorProfileName: Int = 0xFFFFFFFF.toInt()
        var colorProfileStatus: Int = 0xFFB0B0B0.toInt()
        var colorHeader: Int = 0xFF707070.toInt()
        var colorTextPrimary: Int = 0xFF000000.toInt()
        var colorTextSecondary: Int = 0xFF8E8E93.toInt()
        var colorIconPrimary: Int = 0xFF333333.toInt()
        var colorChevron: Int = 0xFFB0B0B0.toInt()
        var colorDivider: Int = 0xFFF0F0F0.toInt()
        var colorLogout: Int = 0xFFF44336.toInt()
        var colorBlue: Int = 0xFF007AFF.toInt() // Стандартный синий iOS/Telegram

        fun apply(isDark: Boolean) {
            if (isDark) {
                // --- Цвета темной темы ---
                colorBackground = 0xFF1A1A1A.toInt()
                colorSheetBackground = 0xFF000000.toInt()
                colorCardBackground = 0xFF1C1C1E.toInt()
                colorProfileName = 0xFFFFFFFF.toInt()
                colorProfileStatus = 0xFFEBEBF5.toInt() // Можно добавить альфу
                colorHeader = 0xFF8E8E93.toInt()
                colorTextPrimary = 0xFFFFFFFF.toInt()
                colorTextSecondary = 0xFF8E8E93.toInt()
                colorIconPrimary = 0xFFFFFFFF.toInt()
                colorChevron = 0xFF8E8E93.toInt()
                colorDivider = 0xFF38383A.toInt()
                colorLogout = 0xFFFF453A.toInt()
                colorBlue = 0xFF0A84FF.toInt() // Синий для темной темы (чуть ярче)
            } else {
                // --- Цвета светлой темы ---
                colorBackground = 0xFFf7f7f7.toInt()
                colorSheetBackground = 0xFFE6E6E6.toInt()
                colorCardBackground = 0xFFFFFFFF.toInt()
                colorProfileName = 0xFFFFFFFF.toInt()
                colorProfileStatus = 0xFFB0B0B0.toInt()
                colorHeader = 0xFF707070.toInt()
                colorTextPrimary = 0xFF000000.toInt()
                colorTextSecondary = 0xFF8E8E93.toInt()
                colorIconPrimary = 0xFF333333.toInt()
                colorChevron = 0xFFB0B0B0.toInt()
                colorDivider = 0xFFF0F0F0.toInt()
                colorLogout = 0xFFF44336.toInt()
                colorBlue = 0xFF007AFF.toInt()
            }
        }
    }
}

// ==========================================
//              CELLS (ЯЧЕЙКИ)
// ==========================================

class ProfileHeaderCell(context: Context) : FrameLayout(context) {
    val avatarView: ShapeableImageView
    val nameView: TextView
    val statusView: TextView

    init {
        val linearLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        avatarView = ShapeableImageView(context).apply {
            setShapeAppearanceModel(
                this.shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes(LayoutHelper.dp(32).toFloat())
                    .build()
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.GRAY)
        }
        linearLayout.addView(avatarView, LayoutHelper.createLinear(64, 64))

        val textContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        nameView = TextView(context).apply {
            textSize = 18f
            setTextColor(DesignEngine.COLOR_PROFILE_NAME)
            typeface = Typeface.DEFAULT_BOLD
        }
        statusView = TextView(context).apply {
            textSize = 14f
            setTextColor(DesignEngine.COLOR_PROFILE_STATUS)
        }
        textContainer.addView(nameView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))
        textContainer.addView(statusView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))
        linearLayout.addView(textContainer, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 16, 0, 0, 0))

        addView(linearLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP or Gravity.START, 24, 100, 24, 0))
    }

    fun setData(name: String, status: String, avatarUrl: String) {
        nameView.text = name
        statusView.text = status
        avatarView.load(avatarUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_user_placeholder)
            error(R.drawable.ic_user_placeholder)
        }
    }
}

class HeaderCell(context: Context) : TextView(context) {
    init {
        textSize = 12f
        isAllCaps = true
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(DesignEngine.COLOR_HEADER)
        layoutParams = LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 8, 0, 8, 8)
    }
}

class ContainerCell(context: Context) : LinearLayout(context) {
    init {
        orientation = VERTICAL
        background = GradientDrawable().apply {
            setColor(DesignEngine.COLOR_CARD_BACKGROUND)
            cornerRadius = LayoutHelper.dp(16).toFloat()
        }
    }

    fun addCell(cell: View, isLast: Boolean = false) {
        addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        if (!isLast) {
            val divider = View(context)
            divider.setBackgroundColor(DesignEngine.COLOR_DIVIDER)
            addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0f, 0, 56, 0, 0, 0))
        }
    }
}

class TextCell(context: Context) : FrameLayout(context) {
    private val imageView: ImageView = ImageView(context)
    private val textView: TextView = TextView(context)
    private val chevronView: ImageView = ImageView(context)
    
    init {
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        foreground = context.getDrawable(outValue.resourceId)
        isClickable = true

        imageView.scaleType = ImageView.ScaleType.CENTER
        imageView.colorFilter = PorterDuffColorFilter(DesignEngine.COLOR_ICON_PRIMARY, PorterDuff.Mode.SRC_IN)
        addView(imageView, LayoutHelper.createFrame(24, 24, Gravity.CENTER_VERTICAL or Gravity.START, 16, 0, 0, 0))

        textView.textSize = 16f
        textView.setTextColor(DesignEngine.COLOR_TEXT_PRIMARY)
        textView.maxLines = 1
        textView.ellipsize = TextUtils.TruncateAt.END
        textView.gravity = Gravity.CENTER_VERTICAL or Gravity.START
        addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP or Gravity.START, 56, 0, 56, 0))
        
        chevronView.setImageResource(R.drawable.ic_chevron_right)
        chevronView.colorFilter = PorterDuffColorFilter(DesignEngine.COLOR_CHEVRON, PorterDuff.Mode.SRC_IN)
        addView(chevronView, LayoutHelper.createFrame(24, 24, Gravity.CENTER_VERTICAL or Gravity.END, 0, 0, 16, 0))
    }

    fun setData(text: String, iconResId: Int) {
        textView.text = text
        imageView.setImageResource(iconResId)
    }
    
    fun setTextColor(color: Int) {
        textView.setTextColor(color)
    }
    
    fun setIconColor(color: Int) {
        imageView.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(LayoutHelper.dp(54), MeasureSpec.EXACTLY))
    }
}

class SpacerCell(context: Context, height: Int) : View(context) {
    init {
        layoutParams = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, height)
    }
}

/**
 * Ячейка чата в списке чатов (аналог item_chat.xml).
 */
class ChatCell(context: Context) : FrameLayout(context) {
    val avatarView: ShapeableImageView
    val nameView: TextView
    val messageView: TextView
    val timeView: TextView
    val unreadBadge: TextView

    init {
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        foreground = context.getDrawable(outValue.resourceId)
        isClickable = true
        
        // Фон самой ячейки (можно сделать прозрачным, так как список имеет фон, но для надежности можно задать)
        // setBackgroundColor(DesignEngine.COLOR_CARD_BACKGROUND) // По желанию

        // Паддинги самой ячейки
        setPadding(LayoutHelper.dp(20), LayoutHelper.dp(10), LayoutHelper.dp(20), LayoutHelper.dp(10))

        // 1. Аватар
        avatarView = ShapeableImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.LTGRAY)
            // Круглая форма
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(LayoutHelper.dp(25).toFloat()) // 50dp / 2
                .build()
        }
        addView(avatarView, LayoutHelper.createFrame(50, 50, Gravity.CENTER_VERTICAL or Gravity.START))

        // 2. Контейнер текстов (Имя + Сообщение)
        val textContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        
        nameView = TextView(context).apply {
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.COLOR_TEXT_PRIMARY)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        
        messageView = TextView(context).apply {
            textSize = 14f
            setTextColor(DesignEngine.COLOR_TEXT_SECONDARY)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        
        textContainer.addView(nameView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))
        textContainer.addView(messageView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 2, 0, 0))
        
        // Добавляем контейнер с отступом слева (50 + 16 = 66)
        addView(textContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.START, 66, 0, 60, 0))

        // 3. Контейнер Мета (Время + Badge)
        val metaContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
        }
        
        timeView = TextView(context).apply {
            textSize = 12f
            setTextColor(DesignEngine.COLOR_TEXT_SECONDARY)
        }
        
        unreadBadge = TextView(context).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(LayoutHelper.dp(6), LayoutHelper.dp(2), LayoutHelper.dp(6), LayoutHelper.dp(2))
            background = GradientDrawable().apply {
                setColor(DesignEngine.COLOR_BLUE)
                cornerRadius = LayoutHelper.dp(12).toFloat()
            }
            visibility = View.GONE
        }
        
        metaContainer.addView(timeView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))
        metaContainer.addView(unreadBadge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 4, 0, 0))
        
        addView(metaContainer, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.END))
    }

    fun setData(name: String, message: String, time: String, unread: Int, avatarUrl: String?) {
        nameView.text = name
        messageView.text = message
        timeView.text = time
        
        if (unread > 0) {
            unreadBadge.text = unread.toString()
            unreadBadge.visibility = View.VISIBLE
            timeView.setTextColor(DesignEngine.COLOR_BLUE) // Время синим, если есть непрочитанные
        } else {
            unreadBadge.visibility = View.GONE
            timeView.setTextColor(DesignEngine.COLOR_TEXT_SECONDARY)
        }
        
        avatarView.load(avatarUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_user_placeholder)
            error(R.drawable.ic_user_placeholder)
        }
    }
}
