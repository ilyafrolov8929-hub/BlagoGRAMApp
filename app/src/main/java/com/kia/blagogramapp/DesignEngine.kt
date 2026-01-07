package com.kia.blagogramapp

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import coil.load
import coil.transform.CircleCropTransformation
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

    fun createRelative(width: Int, height: Int, left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0): RelativeLayout.LayoutParams {
        val layoutParams = RelativeLayout.LayoutParams(getSize(width), getSize(height))
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
    var context: Context? = null

    fun init(ctx: Context) {
        density = ctx.resources.displayMetrics.density
        context = ctx.applicationContext
    }

    // Геттеры для цветов (ссылаются на текущие значения в Theme)
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
    val COLOR_BLUE get() = Theme.colorBlue

    enum class PlayerTheme {
        DEFAULT, DYNAMIC, BLUR
    }

    /**
     * Движок тем.
     */
    object Theme {
        // Значения по умолчанию
        var colorBackground: Int = 0xFFf7f7f7.toInt()
        var colorSheetBackground: Int = 0xFFE6E6E6.toInt()
        var colorCardBackground: Int = 0xFFFFFFFF.toInt()
        var colorProfileName: Int = 0xFF000000.toInt()
        var colorProfileStatus: Int = 0xFF8E8E93.toInt()
        var colorHeader: Int = 0xFF707070.toInt()
        var colorTextPrimary: Int = 0xFF000000.toInt()
        var colorTextSecondary: Int = 0xFF8E8E93.toInt()
        var colorIconPrimary: Int = 0xFF333333.toInt()
        var colorChevron: Int = 0xFFB0B0B0.toInt()
        var colorDivider: Int = 0xFFF0F0F0.toInt()
        var colorLogout: Int = 0xFFF44336.toInt()
        var colorBlue: Int = 0xFF007AFF.toInt()

        var playerTheme: PlayerTheme = PlayerTheme.DEFAULT

        fun apply(isDark: Boolean, isMonet: Boolean = false) {
            val ctx = DesignEngine.context
            
            val prefs = ctx?.getSharedPreferences("app_theme", Context.MODE_PRIVATE)
            val pThemeStr = prefs?.getString("player_theme", "DEFAULT") ?: "DEFAULT"
            playerTheme = try { PlayerTheme.valueOf(pThemeStr) } catch (e: Exception) { PlayerTheme.DEFAULT }

            // Проверка на Monet (Android 12+)
            if (isMonet && ctx != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isDark) {
                    // --- MONET DARK ---
                    colorBackground = ContextCompat.getColor(ctx, android.R.color.system_neutral1_900)
                    colorSheetBackground = ContextCompat.getColor(ctx, android.R.color.system_neutral1_800)
                    colorCardBackground = ContextCompat.getColor(ctx, android.R.color.system_neutral1_700)
                    colorProfileName = ContextCompat.getColor(ctx, android.R.color.system_accent1_100)
                    colorProfileStatus = ContextCompat.getColor(ctx, android.R.color.system_accent2_200)
                    colorHeader = ContextCompat.getColor(ctx, android.R.color.system_accent2_300)
                    colorTextPrimary = ContextCompat.getColor(ctx, android.R.color.system_accent1_50)
                    colorTextSecondary = ContextCompat.getColor(ctx, android.R.color.system_neutral2_300)
                    colorIconPrimary = ContextCompat.getColor(ctx, android.R.color.system_accent1_100)
                    colorChevron = ContextCompat.getColor(ctx, android.R.color.system_neutral2_500)
                    colorDivider = ContextCompat.getColor(ctx, android.R.color.system_neutral1_700)
                    colorLogout = 0xFFFF453A.toInt() 
                    colorBlue = ContextCompat.getColor(ctx, android.R.color.system_accent1_300) // Яркий акцент
                } else {
                    // --- MONET LIGHT ---
                    colorBackground = ContextCompat.getColor(ctx, android.R.color.system_neutral1_50)
                    colorSheetBackground = ContextCompat.getColor(ctx, android.R.color.system_neutral1_100)
                    colorCardBackground = ContextCompat.getColor(ctx, android.R.color.system_accent1_10) // Чуть-чуть оттенок
                    colorProfileName = 0xFF000000.toInt()
                    colorProfileStatus = ContextCompat.getColor(ctx, android.R.color.system_neutral2_600)
                    colorHeader = ContextCompat.getColor(ctx, android.R.color.system_neutral2_700)
                    colorTextPrimary = ContextCompat.getColor(ctx, android.R.color.system_accent1_900)
                    colorTextSecondary = ContextCompat.getColor(ctx, android.R.color.system_neutral2_600)
                    colorIconPrimary = ContextCompat.getColor(ctx, android.R.color.system_accent1_800)
                    colorChevron = ContextCompat.getColor(ctx, android.R.color.system_neutral2_300)
                    colorDivider = ContextCompat.getColor(ctx, android.R.color.system_neutral1_200)
                    colorLogout = 0xFFF44336.toInt()
                    colorBlue = ContextCompat.getColor(ctx, android.R.color.system_accent1_600) // Насыщенный акцент
                }
            } else if (isDark) {
                // --- STANDART DARK ---
                colorBackground = 0xFF1A1A1A.toInt()
                colorSheetBackground = 0xFF202020.toInt()
                colorCardBackground = 0xFF2C2C2E.toInt()
                colorProfileName = 0xFFFFFFFF.toInt()
                colorProfileStatus = 0xFFEBEBF5.toInt()
                colorHeader = 0xFF8E8E93.toInt()
                colorTextPrimary = 0xFFFFFFFF.toInt()
                colorTextSecondary = 0xFF8E8E93.toInt()
                colorIconPrimary = 0xFFFFFFFF.toInt()
                colorChevron = 0xFF8E8E93.toInt()
                colorDivider = 0xFF38383A.toInt()
                colorLogout = 0xFFFF453A.toInt()
                colorBlue = 0xFF0A84FF.toInt()
            } else {
                // --- STANDART LIGHT ---
                colorBackground = 0xFFF2F2F7.toInt()
                colorSheetBackground = 0xFFFFFFFF.toInt()
                colorCardBackground = 0xFFFFFFFF.toInt()
                colorProfileName = 0xFF000000.toInt()
                colorProfileStatus = 0xFF8E8E93.toInt()
                colorHeader = 0xFF8E8E93.toInt()
                colorTextPrimary = 0xFF000000.toInt()
                colorTextSecondary = 0xFF8E8E93.toInt()
                colorIconPrimary = 0xFF333333.toInt()
                colorChevron = 0xFFC7C7CC.toInt()
                colorDivider = 0xFFC6C6C8.toInt()
                colorLogout = 0xFFF44336.toInt()
                colorBlue = 0xFF007AFF.toInt()
            }
        }
    }
}

/**
 * Менеджер подписки
 */
object SubscriptionManager {
    fun isPremium(context: Context): Boolean {
        val prefs = context.getSharedPreferences("app", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_premium", false)
    }
}

// ==========================================
//              CELLS (ЯЧЕЙКИ)
// ==========================================

class MusicCell(context: Context) : FrameLayout(context) {
    val coverView: ShapeableImageView
    val titleView: TextView
    val artistView: TextView
    val moreBtn: ImageView
    val loadingContainer: LinearLayout
    val progressBar: ProgressBar
    val progressText: TextView

    init {
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        foreground = context.getDrawable(outValue.resourceId)
        isClickable = true
        
        setPadding(LayoutHelper.dp(16), LayoutHelper.dp(8), LayoutHelper.dp(16), LayoutHelper.dp(8))

        coverView = ShapeableImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.LTGRAY)
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(LayoutHelper.dp(8).toFloat())
                .build()
        }
        addView(coverView, LayoutHelper.createFrame(48, 48, Gravity.CENTER_VERTICAL or Gravity.START))

        val textContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        titleView = TextView(context).apply {
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.COLOR_TEXT_PRIMARY)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        artistView = TextView(context).apply {
            textSize = 14f
            setTextColor(DesignEngine.COLOR_TEXT_SECONDARY)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        textContainer.addView(titleView)
        textContainer.addView(artistView)
        addView(textContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 64, 0, 80, 0))

        moreBtn = ImageView(context).apply {
            setImageResource(R.drawable.ic_more)
            setColorFilter(DesignEngine.COLOR_ICON_PRIMARY, PorterDuff.Mode.SRC_IN)
            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
        }
        addView(moreBtn, LayoutHelper.createFrame(40, 40, Gravity.CENTER_VERTICAL or Gravity.END))

        loadingContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        
        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            progressDrawable.setColorFilter(DesignEngine.Theme.colorBlue, PorterDuff.Mode.SRC_IN)
            max = 100
        }
        progressText = TextView(context).apply {
            textSize = 10f
            setTextColor(DesignEngine.Theme.colorBlue)
            typeface = Typeface.DEFAULT_BOLD
        }
        loadingContainer.addView(progressBar, LayoutHelper.createLinear(60, 4))
        loadingContainer.addView(progressText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 2, 0, 0))
        
        addView(loadingContainer, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.END, 0, 0, 8, 0))
    }

    fun setData(title: String, artist: String, imageUrl: String?) {
        titleView.text = title
        artistView.text = artist
        coverView.load(imageUrl) {
            placeholder(R.drawable.ic_music)
            error(R.drawable.ic_music)
        }
    }
}

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
        layoutParams = LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 16, 0, 16, 8)
    }
}

class ContainerCell(context: Context) : LinearLayout(context) {
    init {
        orientation = VERTICAL
        background = GradientDrawable().apply {
            setColor(DesignEngine.Theme.colorCardBackground)
            cornerRadius = LayoutHelper.dp(12).toFloat()
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
    val imageView: ImageView = ImageView(context)
    val textView: TextView = TextView(context)
    val chevronView: ImageView = ImageView(context)
    val deleteView: ImageView = ImageView(context)
    
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
        addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP or Gravity.START, 56, 0, 80, 0))
        
        chevronView.setImageResource(R.drawable.ic_chevron_right)
        chevronView.colorFilter = PorterDuffColorFilter(DesignEngine.COLOR_CHEVRON, PorterDuff.Mode.SRC_IN)
        addView(chevronView, LayoutHelper.createFrame(24, 24, Gravity.CENTER_VERTICAL or Gravity.END, 0, 0, 16, 0))

        deleteView.setImageResource(R.drawable.ic_logout) // Используем иконку логаута как крестик
        deleteView.colorFilter = PorterDuffColorFilter(DesignEngine.COLOR_LOGOUT, PorterDuff.Mode.SRC_IN)
        deleteView.visibility = View.GONE
        deleteView.setPadding(LayoutHelper.dp(4), LayoutHelper.dp(4), LayoutHelper.dp(4), LayoutHelper.dp(4))
        val deleteParams = LayoutHelper.createFrame(32, 32, Gravity.CENTER_VERTICAL or Gravity.END, 0, 0, 12, 0)
        addView(deleteView, deleteParams)
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
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(LayoutHelper.dp(50), MeasureSpec.EXACTLY))
    }
}

class SpacerCell(context: Context, height: Int) : View(context) {
    init {
        layoutParams = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, height)
    }
}

/**
 * Ячейка чата
 */
class ChatCell(context: Context) : FrameLayout(context) {
    val avatarView: ShapeableImageView
    val nameView: TextView
    val messageView: TextView
    val timeView: TextView
    val unreadBadge: TextView
    val pinView: ImageView

    init {
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        foreground = context.getDrawable(outValue.resourceId)
        isClickable = true
        
        setPadding(LayoutHelper.dp(16), LayoutHelper.dp(10), LayoutHelper.dp(16), LayoutHelper.dp(10))

        // 1. Аватар
        avatarView = ShapeableImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.LTGRAY)
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(LayoutHelper.dp(25).toFloat())
                .build()
        }
        addView(avatarView, LayoutHelper.createFrame(50, 50, Gravity.CENTER_VERTICAL or Gravity.START))

        // 2. Имя и сообщение
        val textContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        
        nameView = TextView(context).apply {
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.COLOR_TEXT_PRIMARY)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        
        messageView = TextView(context).apply {
            textSize = 15f
            setTextColor(DesignEngine.COLOR_TEXT_SECONDARY)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        
        textContainer.addView(nameView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))
        textContainer.addView(messageView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 2, 0, 0))
        
        addView(textContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.START, 66, 0, 60, 0))

        // 3. Мета
        val metaContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END or Gravity.TOP
        }
        
        timeView = TextView(context).apply {
            textSize = 13f
            setTextColor(DesignEngine.COLOR_TEXT_SECONDARY)
        }
        
        val badgeContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
        }

        pinView = ImageView(context).apply {
            setImageResource(R.drawable.ic_pin)
            setColorFilter(DesignEngine.COLOR_TEXT_SECONDARY, PorterDuff.Mode.SRC_IN)
            visibility = View.GONE
        }

        unreadBadge = TextView(context).apply {
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(LayoutHelper.dp(6), LayoutHelper.dp(2), LayoutHelper.dp(6), LayoutHelper.dp(2))
            background = GradientDrawable().apply {
                setColor(DesignEngine.Theme.colorBlue)
                cornerRadius = LayoutHelper.dp(14).toFloat()
            }
            visibility = View.GONE
        }
        
        badgeContainer.addView(pinView, LayoutHelper.createLinear(20, 20, 0f, 0, 0, 0, 4, 0))
        badgeContainer.addView(unreadBadge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))
        
        metaContainer.addView(timeView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))
        metaContainer.addView(badgeContainer, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 4, 0, 0))
        
        addView(metaContainer, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.END))
    }

    fun setData(name: String, message: String, time: String, unread: Int, avatar: String?, pinned: Boolean = false) {
        nameView.text = name
        messageView.text = message
        timeView.text = time
        
        if (unread > 0) {
            unreadBadge.text = unread.toString()
            unreadBadge.visibility = View.VISIBLE
            timeView.setTextColor(DesignEngine.COLOR_BLUE)
        } else {
            unreadBadge.visibility = View.GONE
            timeView.setTextColor(DesignEngine.COLOR_TEXT_SECONDARY)
        }
        
        pinView.visibility = if (pinned) View.VISIBLE else View.GONE
        setBackgroundColor(if (pinned) DesignEngine.COLOR_DIVIDER else Color.TRANSPARENT)
        
        if (avatar == "action_saved") {
            avatarView.setPadding(0, 0, 0, 0) // Убираем падинги, чтобы фон не сжимался
            avatarView.scaleType = ImageView.ScaleType.CENTER
            avatarView.setImageResource(R.drawable.ic_saved_messages)
            avatarView.setBackgroundResource(R.drawable.bg_saved_messages_circle)
        } else {
            avatarView.scaleType = ImageView.ScaleType.CENTER_CROP
            avatarView.setPadding(0, 0, 0, 0)
            avatarView.setBackgroundResource(0)
            val avatarUrl = if (!avatar.isNullOrEmpty()) "https://blagogram.ru/avatars/$avatar" else null
            avatarView.load(avatarUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_user_placeholder)
                error(R.drawable.ic_user_placeholder)
                transformations(CircleCropTransformation())
            }
        }
    }
}

/**
 * Заголовок секции чатов (как на скриншоте)
 */
class ChatSectionHeaderCell(context: Context) : FrameLayout(context) {
    private val textView: TextView
    init {
        setPadding(0, LayoutHelper.dp(20), 0, LayoutHelper.dp(12))
        textView = TextView(context).apply {
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(LayoutHelper.dp(24), LayoutHelper.dp(6), LayoutHelper.dp(24), LayoutHelper.dp(6))
            background = GradientDrawable().apply {
                setColor(0xFFBCBCBC.toInt()) // Солидный серый как на скриншоте
                cornerRadius = LayoutHelper.dp(20).toFloat()
            }
        }
        addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER))
    }
    fun setTitle(title: String) {
        textView.text = title
    }
}
