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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.setPadding
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

    // Настройки чата
    var fontSize: Int = 16
    var bubbleRadius: Int = 17
    var chatWallpaper: String? = null
    var wallpaperBlur: Boolean = false
    var wallpaperMotion: Boolean = false

    // Навигация
    var useBottomNavigation: Boolean = false

    fun init(ctx: Context) {
        density = ctx.resources.displayMetrics.density
        context = ctx.applicationContext

        val prefs = ctx.getSharedPreferences("app_theme", Context.MODE_PRIVATE)
        fontSize = prefs.getInt("font_size", 16)
        bubbleRadius = prefs.getInt("bubble_radius", 17)
        chatWallpaper = prefs.getString("chat_wallpaper", null)
        wallpaperBlur = prefs.getBoolean("wallpaper_blur", false)
        wallpaperMotion = prefs.getBoolean("wallpaper_motion", false)
        useBottomNavigation = prefs.getBoolean("use_bottom_navigation", false)
    }

    fun saveChatSettings(context: Context) {
        val prefs = context.getSharedPreferences("app_theme", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("font_size", fontSize)
            putInt("bubble_radius", bubbleRadius)
            putString("chat_wallpaper", chatWallpaper)
            putBoolean("wallpaper_blur", wallpaperBlur)
            putBoolean("wallpaper_motion", wallpaperMotion)
            apply()
        }
    }

    /**
     * Создает настроенный ImageView для премиум-значка
     */
    fun createPremiumBadge(context: Context): ImageView {
        return ImageView(context).apply {
            setImageResource(R.drawable.infinity)
            setColorFilter(Theme.colorBlue, PorterDuff.Mode.SRC_IN)
            visibility = View.GONE
        }
    }

    fun createChatHeader(
        context: Context,
        onBackClick: View.OnClickListener,
        onMenuClick: View.OnClickListener,onHeaderClick: View.OnClickListener
    ): Quadruple<FrameLayout, ShapeableImageView, TextView, ImageView> {
        val isDark = context.getSharedPreferences("app_theme", Context.MODE_PRIVATE)
            .getBoolean("is_dark_theme", false)
        val pillColor = if (isDark) Color.parseColor("#333333") else Color.parseColor("#E0E0E0")
        // Цвет для обводки. Полупрозрачный черный. Можете поменять на "#1AFFFFFF" для белой.
        val strokeColor = if (isDark) Color.parseColor("#33FFFFFF") else Color.parseColor("#26000000")
        // Толщина обводки в dp
        val strokeWidth = 1
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0

        val root = FrameLayout(context)

        // Левая кнопка "назад"
        val backButton = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                setColor(pillColor)
                cornerRadius = LayoutHelper.dp(18).toFloat()
                setStroke(LayoutHelper.dp(strokeWidth), strokeColor)
            }
            setOnClickListener(onBackClick)
            val icon = ImageView(context).apply {
                setImageResource(R.drawable.ic_back)
                setColorFilter(Theme.colorIconPrimary, PorterDuff.Mode.SRC_IN)
            }
            addView(icon, LayoutHelper.createFrame(24, 24, Gravity.CENTER))
        }
        root.addView(backButton, LayoutHelper.createFrame(36, 36, Gravity.START or Gravity.CENTER_VERTICAL))

        // Правая кнопка "меню"
        val menuButton = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                setColor(pillColor)
                cornerRadius = LayoutHelper.dp(18).toFloat()
                setStroke(LayoutHelper.dp(strokeWidth), strokeColor)
            }
            setOnClickListener(onMenuClick)
            val icon = ImageView(context).apply {
                setImageResource(R.drawable.ic_menu)
                setColorFilter(Theme.colorIconPrimary, PorterDuff.Mode.SRC_IN)
            }
            addView(icon, LayoutHelper.createFrame(24, 24, Gravity.CENTER))
        }
        root.addView(menuButton, LayoutHelper.createFrame(36, 36, Gravity.END or Gravity.CENTER_VERTICAL))


        // Центральная часть с информацией
        val centerPill = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(pillColor)
                cornerRadius = LayoutHelper.dp(28).toFloat()
                setStroke(LayoutHelper.dp(strokeWidth), strokeColor)
            }
            setPadding(LayoutHelper.dp(6), LayoutHelper.dp(6), LayoutHelper.dp(16), LayoutHelper.dp(6))
            setOnClickListener(onHeaderClick)
        }

        val avatarView = ShapeableImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(LayoutHelper.dp(22).toFloat()).build()
        }
        centerPill.addView(avatarView, LayoutHelper.createLinear(44, 44))

        val infoContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        val nameRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val nameView = TextView(context).apply {
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Theme.colorTextPrimary)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }

        val premiumBadge = createPremiumBadge(context).apply {
            val p = LayoutHelper.createLinear(16, 16)
            p.marginStart = LayoutHelper.dp(4)
            layoutParams = p
        }

        nameRow.addView(nameView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 0))
        nameRow.addView(premiumBadge)

        val statusView = TextView(context).apply {
            text = "был(а) недавно" // Placeholder
            textSize = 10f
            setTextColor(Theme.colorTextSecondary)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        infoContainer.addView(nameRow, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))
        infoContainer.addView(statusView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))

        centerPill.addView(infoContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 1f, 0, 10, 0, 0, 0))

        root.addView(centerPill, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 56, Gravity.CENTER_VERTICAL, 48, 0, 48, 0))

        return Quadruple(root, avatarView, nameView, premiumBadge)
    }

    /**
     * Инициализирует базовые параметры экрана: тему, Edge-to-Edge и создает корневой layout.
     * @param activity Текущая Activity.
     * @return Возвращает корневой LinearLayout, готовый для добавления контента.
     */
    fun initializeScreen(activity: AppCompatActivity): LinearLayout {
        // 1. Инициализация самого DesignEngine (если еще не сделано)
        init(activity)

        // 2. Получение и применение настроек темы
        val themePrefs = activity.getSharedPreferences("app_theme", Context.MODE_PRIVATE)
        val isDarkTheme = themePrefs.getBoolean("is_dark_theme", false)
        val useMonet = themePrefs.getBoolean("use_monet", false)

        Theme.apply(isDarkTheme, useMonet)

        // 3. Включение режима Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        activity.window.statusBarColor = Color.TRANSPARENT
        activity.window.navigationBarColor = Color.TRANSPARENT

        // --- НОВЫЙ БЛОК ---
        // 4. Настройка цвета иконок системных баров в зависимости от темы
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        controller.isAppearanceLightStatusBars = !isDarkTheme
        controller.isAppearanceLightNavigationBars = !isDarkTheme
        // --- КОНЕЦ НОВОГО БЛОКА ---

        // 5. Создание и возврат корневого контейнера
        return LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.colorBackground)
        }
    }

    /**
     * Инициализирует базовые параметры экрана и возвращает готовый FrameLayout.
     * @param activity Текущая Activity.
     * @return Возвращает корневой FrameLayout, готовый для setContentVew().
     */
    fun initializeScreenWithFrameLayout(activity: AppCompatActivity): FrameLayout {
        // Вызываем основную логику из первой функции
        initializeScreen(activity)

        // Создаем и возвращаем FrameLayout with correct background
        return FrameLayout(activity).apply {
            setBackgroundColor(Theme.colorBackground)
        }
    }

    /**
     * Создает "таблеточный" тулбар для PhotoViewerActivity.
     * @param context Контекст для создания View.
     * @param onBackClick Слушатель нажатия на кнопку "назад".
     * @return Triple, содержащий:
     *         1. Корневой FrameLayout тулбара.
     *         2. TextView для имени отправителя.
     *         3. TextView для даты.
     */
    fun createPhotoViewerToolbar(
        context: Context,
        onBackClick: View.OnClickListener
    ): Triple<FrameLayout, TextView, TextView> {
        // Получаем высоту системного статус-бара
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
        val strokeColor = Color.parseColor("#33FFFFFF")
        // Толщина обводки в dp
        val strokeWidth = 1

        // Основной контейнер, который будет позиционироваться сверху
        val toolbarContainer = FrameLayout(context).apply {
            // Добавляем отступ сверху, равный высоте статус-бара + 8dp
            setPadding(LayoutHelper.dp(8), statusBarHeight + LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
        }

        // Фон для всех элементов
        val pillBackground = GradientDrawable().apply {
            setColor(0x80333333.toInt()) // Полупрозрачный темный цвет
            cornerRadius = LayoutHelper.dp(28).toFloat()
        }

        // 1. Кнопка "Назад"
        val backButton = FrameLayout(context).apply {
            val drawable = ContextCompat.getDrawable(context, R.drawable.bg_stat_pill) as GradientDrawable
            drawable.setColor( Color.parseColor("#333333"))
            drawable.setStroke(LayoutHelper.dp(strokeWidth), strokeColor)
            background = drawable
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(6), LayoutHelper.dp(12), LayoutHelper.dp(6))

            setOnClickListener(onBackClick)
            // Добавляем ripple-эффект при нажатии
            val outValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
            foreground = ContextCompat.getDrawable(context, outValue.resourceId)
        }
        val backIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_back)
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        }
        backButton.addView(backIcon, LayoutHelper.createFrame(24, 24, Gravity.CENTER))
        toolbarContainer.addView(backButton, LayoutHelper.createFrame(48, 48, Gravity.START or Gravity.CENTER_VERTICAL))

        // 2. Центральная часть (Имя + Дата)
        val centerPill = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val drawable = ContextCompat.getDrawable(context, R.drawable.bg_stat_pill) as GradientDrawable
            drawable.setColor( Color.parseColor("#333333"))
            drawable.setStroke(LayoutHelper.dp(strokeWidth), strokeColor)
            background = drawable
            setPadding(LayoutHelper.dp(20), LayoutHelper.dp(8), LayoutHelper.dp(20), LayoutHelper.dp(8))
        }

        val nameView = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        }
        val dateView = TextView(context).apply {
            setTextColor(0xFFD3D3D3.toInt()) // Светло-серый
            textSize = 12f
        }

        centerPill.addView(nameView)
        centerPill.addView(dateView)
        toolbarContainer.addView(centerPill, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL))

        return Triple(toolbarContainer, nameView, dateView)
    }

    /**
     * Создает контейнер для подписи к фото.
     * @return Pair, содержащий:
     *         1. Корневой FrameLayout контейнера.
     *         2. TextView для текста подписи.
     */
    fun createPhotoCaptionView(context: Context): Pair<FrameLayout, TextView> {
        val captionContainer = FrameLayout(context).apply {
            val strokeColor = Color.parseColor("#33FFFFFF")
            val strokeWidth = 1

            val drawable = ContextCompat.getDrawable(context, R.drawable.bg_stat_pill) as GradientDrawable
            drawable.setColor( Color.parseColor("#333333"))
            drawable.setStroke(LayoutHelper.dp(strokeWidth), strokeColor)
            background = drawable

            setPadding(LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8), LayoutHelper.dp(8))
        }

        val textView = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(8), LayoutHelper.dp(12), LayoutHelper.dp(8))
            maxLines = 8
            ellipsize = TextUtils.TruncateAt.END
        }
        captionContainer.addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        return captionContainer to textView
    }


    fun createToolBar(
        context: Context,
        title: String,
        leftButtonIconRes: Int? = null,
        rightButtonIconRes: Int? = null,
        isDark: Boolean = false,
        leftButtonClickListener: View.OnClickListener? = null,
        rightButtonClickListener: View.OnClickListener? = null
    ): RelativeLayout {
        // Используем цвета из текущей темы DesignEngine.Theme
        val pillBackgroundColor = Theme.colorCardBackground

        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
        val strokeColor = if (isDark) Color.parseColor("#33FFFFFF") else Color.parseColor("#26000000")
        // Толщина обводки в dp
        val strokeWidth = 1
        // Главный контейнер тулбара, он будет прозрачным
        val toolbarLayout = RelativeLayout(context).apply {
            setPadding(LayoutHelper.dp(16), statusBarHeight + LayoutHelper.dp(10), LayoutHelper.dp(16), LayoutHelper.dp(10))
        }

        // Внутренний контейнер для центрирования
        val toolbarContent = RelativeLayout(context).apply {
            layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        }

        // Левая кнопка (например, "Назад")
        if (leftButtonIconRes != null) {
            val leftButton = LinearLayout(context).apply {
                val drawable = ContextCompat.getDrawable(context, R.drawable.bg_stat_pill) as GradientDrawable
                drawable.setColor(if (isDark) Color.parseColor("#333333") else Color.parseColor("#E0E0E0"))
                drawable.setStroke(LayoutHelper.dp(strokeWidth), strokeColor)
                background = drawable

                setPadding(LayoutHelper.dp(12), LayoutHelper.dp(6), LayoutHelper.dp(12), LayoutHelper.dp(6))

                setOnClickListener(leftButtonClickListener)

                addView(ImageView(context).apply {
                    setImageResource(leftButtonIconRes)
                    // Используем цвет иконки из темы
                    setColorFilter(Theme.colorIconPrimary, PorterDuff.Mode.SRC_IN)
                    layoutParams = LinearLayout.LayoutParams(LayoutHelper.dp(20), LayoutHelper.dp(20))
                })
            }

            val leftParams = LayoutHelper.createRelative(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT).apply {
                addRule(RelativeLayout.ALIGN_PARENT_START)
                addRule(RelativeLayout.CENTER_VERTICAL)
                // Боковой отступ уже есть в родительском toolbarLayout, поэтому marginStart не нужен
            }
            toolbarContent.addView(leftButton, leftParams)
        }

        // Заголовок по центру
        val titleLayout = LinearLayout(context).apply {
            val drawable = ContextCompat.getDrawable(context, R.drawable.bg_stat_pill) as GradientDrawable
            drawable.setColor(if (isDark) Color.parseColor("#333333") else Color.parseColor("#E0E0E0"))
            drawable.setStroke(LayoutHelper.dp(strokeWidth), strokeColor)
            background = drawable
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(6), LayoutHelper.dp(12), LayoutHelper.dp(6))
            gravity = Gravity.CENTER // Центрируем текст внутри

            addView(TextView(context).apply {
                text = title
                typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
                textSize = 20f
                // Используем цвет текста из темы
                setTextColor(Theme.colorTextPrimary)
            })
        }

        val titleParams = LayoutHelper.createRelative(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT).apply {
            addRule(RelativeLayout.CENTER_IN_PARENT)
        }
        toolbarContent.addView(titleLayout, titleParams)

        // Правая кнопка (например, "Сохранить")
        if (rightButtonIconRes != null) {
            val rightButton = LinearLayout(context).apply {
                tag = "toolbar_right_icon" // Добавляем тег
                val drawable = ContextCompat.getDrawable(context, R.drawable.bg_stat_pill) as GradientDrawable
                drawable.setColor(if (isDark) Color.parseColor("#333333") else Color.parseColor("#E0E0E0"))
                drawable.setStroke(LayoutHelper.dp(strokeWidth), strokeColor)
                background = drawable
                setPadding(LayoutHelper.dp(12), LayoutHelper.dp(6), LayoutHelper.dp(12), LayoutHelper.dp(6))

                setOnClickListener(rightButtonClickListener)

                addView(ImageView(context).apply {
                    setImageResource(rightButtonIconRes)
                    // Используем цвет иконки из темы
                    setColorFilter(Theme.colorIconPrimary, PorterDuff.Mode.SRC_IN)
                    layoutParams = LinearLayout.LayoutParams(LayoutHelper.dp(20), LayoutHelper.dp(20))
                })
            }

            val rightParams = LayoutHelper.createRelative(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.CENTER_VERTICAL)
                // Боковой отступ уже есть в родительском toolbarLayout, поэтому marginEnd не нужен
            }
            toolbarContent.addView(rightButton, rightParams)

        }
        toolbarLayout.addView(toolbarContent)
        return toolbarLayout
    }


    /**
     * Простое обновление видимости
     */
    fun setPremiumStatus(badge: ImageView, isPremium: Boolean) {
        badge.visibility = if (isPremium) View.VISIBLE else View.GONE
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

// В файле DesignEngine.kt
class ProfileHeaderCell(context: Context) : FrameLayout(context) {
    val avatarView: ShapeableImageView
    val nameView: TextView
    val premiumBadge: ImageView
    val statusView: TextView

    init {
        background = GradientDrawable().apply {
            setColor(DesignEngine.Theme.colorCardBackground)
            cornerRadius = LayoutHelper.dp(12).toFloat()
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(LayoutHelper.dp(24), LayoutHelper.dp(24), LayoutHelper.dp(24), LayoutHelper.dp(24))
        }

        // Большой круглый аватар по центру
        avatarView = ShapeableImageView(context).apply {
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(LayoutHelper.dp(50).toFloat())
                .build()
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.LTGRAY)
        }
        container.addView(avatarView, LayoutHelper.createLinear(100, 100, 0f, Gravity.CENTER_HORIZONTAL, 0, 0, 0))

        // Имя и значок премиума
        val nameRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        nameView = TextView(context).apply {
            textSize = 22f
            setTextColor(DesignEngine.COLOR_PROFILE_NAME)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        premiumBadge = DesignEngine.createPremiumBadge(context).apply {
            val p = LayoutHelper.createLinear(20, 20)
            p.marginStart = LayoutHelper.dp(6)
            layoutParams = p
        }

        nameRow.addView(nameView)
        nameRow.addView(premiumBadge)
        container.addView(nameRow, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, Gravity.CENTER_HORIZONTAL, 0, 12, 0, 0))

        // Статус по центру
        statusView = TextView(context).apply {
            textSize = 15f
            setTextColor(DesignEngine.COLOR_PROFILE_STATUS)
            gravity = Gravity.CENTER
        }
        container.addView(statusView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 0))

        addView(container, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
    }

    fun setData(name: String, status: String, avatarUrl: String, isPremium: Boolean = false) {
        nameView.text = name
        statusView.text = status
        premiumBadge.visibility = if (isPremium) View.VISIBLE else View.GONE
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
            addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0f, 0, 16, 0, 0, 0))
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

    // Обновленный метод setData
    fun setData(text: String, iconResId: Int = 0) {
        textView.text = text
        val params = textView.layoutParams as FrameLayout.LayoutParams
        if (iconResId != 0) {
            imageView.visibility = View.VISIBLE
            imageView.setImageResource(iconResId)
            params.leftMargin = LayoutHelper.dp(56) // Отступ для текста при наличии иконки
        } else {
            imageView.visibility = View.GONE
            params.leftMargin = LayoutHelper.dp(16) // Уменьшенный отступ, если иконки нет
        }
        textView.layoutParams = params
    }

    // Дополнительно можно добавить метод для скрытия стрелочки
    fun setChevronVisible(visible: Boolean) {
        chevronView.visibility = if (visible) View.VISIBLE else View.GONE
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

class TextDetailCell(context: Context) : FrameLayout(context) {
    val textView: TextView = TextView(context)
    val valueView: TextView = TextView(context)

    init {
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        foreground = context.getDrawable(outValue.resourceId)
        isClickable = true

        val linearLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(10), LayoutHelper.dp(16), LayoutHelper.dp(10))
        }

        textView.textSize = 16f
        textView.typeface = Typeface.DEFAULT_BOLD
        textView.setTextColor(DesignEngine.COLOR_TEXT_PRIMARY)
        linearLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        valueView.textSize = 13f
        valueView.setTextColor(DesignEngine.COLOR_TEXT_SECONDARY)
        linearLayout.addView(valueView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 2, 0, 0))

        addView(linearLayout)
    }

    fun setData(text: String, value: String) {
        textView.text = text
        valueView.text = value
    }
}

class SpacerCell(context: Context, height: Int) : View(context) {
    init {
        layoutParams = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, height)
    }
}

/**
 * Quadruple helper for createChatHeader
 */
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Ячейка чата
 */
class ChatCell(context: Context) : FrameLayout(context) {
    val avatarView: ShapeableImageView
    val nameView: TextView
    val premiumBadge: ImageView
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
        
        val nameRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        
        nameView = TextView(context).apply {
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.COLOR_TEXT_PRIMARY)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        
        premiumBadge = DesignEngine.createPremiumBadge(context).apply {
            val p = LayoutHelper.createLinear(14, 14)
            p.marginStart = LayoutHelper.dp(4)
            layoutParams = p
        }
        
        nameRow.addView(nameView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 0))
        nameRow.addView(premiumBadge)
        
        messageView = TextView(context).apply {
            textSize = 15f
            setTextColor(DesignEngine.COLOR_TEXT_SECONDARY)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        
        textContainer.addView(nameRow, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))
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

    fun setData(name: String, message: String, time: String, unread: Int, avatar: String?, pinned: Boolean = false, isPremium: Boolean = false) {
        nameView.text = name
        messageView.text = message
        timeView.text = time
        premiumBadge.visibility = if (isPremium) View.VISIBLE else View.GONE
        
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
        } else if (avatar == "ai_bot") {
            avatarView.scaleType = ImageView.ScaleType.CENTER
            avatarView.setImageResource(R.drawable.ic_ai) // Или любая иконка робота
            avatarView.setColorFilter(Color.WHITE)
            avatarView.setBackgroundResource(R.drawable.bg_saved_messages_circle)
            avatarView.backgroundTintList = android.content.res.ColorStateList.valueOf(DesignEngine.Theme.colorBlue)
        } else {
            avatarView.scaleType = ImageView.ScaleType.CENTER_CROP
            avatarView.setPadding(0, 0, 0, 0)
            avatarView.setBackgroundResource(0)
            avatarView.clearColorFilter()
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

/**
 * Ячейка поста из ленты новостей
 */
class PostCell(context: Context) : FrameLayout(context) {
    val authorPill: LinearLayout
    val avatarView: ShapeableImageView
    val nameView: TextView
    val timeView: TextView
    val contentText: TextView
    val postImage: ShapeableImageView
    val likePill: LinearLayout
    val likeButton: ImageView
    val likesCount: TextView
    val sharePill: LinearLayout
    
    init {
        val isDark = context.getSharedPreferences("app_theme", Context.MODE_PRIVATE).getBoolean("is_dark_theme", false)
        val pillColor = if (isDark) Color.parseColor("#333333") else Color.parseColor("#E0E0E0")
        
        background = GradientDrawable().apply {
            setColor(DesignEngine.Theme.colorCardBackground)
            cornerRadius = LayoutHelper.dp(30).toFloat()
        }
        setPadding(LayoutHelper.dp(16))
        
        val mainContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        addView(mainContainer)
        
        // Header
        authorPill = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(if (isDark) Color.parseColor("#3D3D3F") else Color.parseColor("#BCBCBC"))
                cornerRadius = LayoutHelper.dp(25).toFloat()
            }
            setPadding(LayoutHelper.dp(6), LayoutHelper.dp(6), LayoutHelper.dp(16), LayoutHelper.dp(6))
        }
        
        avatarView = ShapeableImageView(context).apply {
            shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(LayoutHelper.dp(20).toFloat()).build()
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        authorPill.addView(avatarView, LayoutHelper.createLinear(40, 40))
        
        val nameTimeBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        nameView = TextView(context).apply {
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.Theme.colorTextPrimary)
        }
        timeView = TextView(context).apply {
            textSize = 11f
            setTextColor(DesignEngine.Theme.colorTextSecondary)
        }
        nameTimeBox.addView(nameView)
        nameTimeBox.addView(timeView)
        authorPill.addView(nameTimeBox, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 8, 0, 0, 0))
        
        mainContainer.addView(authorPill, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 0, 0, 16))
        
        // Content
        contentText = TextView(context).apply {
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.Theme.colorTextPrimary)
        }
        mainContainer.addView(contentText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 8, 0, 8, 12))

        postImage = ShapeableImageView(context).apply {
            shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(LayoutHelper.dp(12).toFloat()).build()
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = View.GONE
        }
        mainContainer.addView(postImage, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 180, 0f, 0, 0, 0, 0, 12))

        // --- ФУТЕР (Like & Share Pills) ---
        val footer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        likePill = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(pillColor)
                cornerRadius = LayoutHelper.dp(20).toFloat()
            }
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(6), LayoutHelper.dp(12), LayoutHelper.dp(6))

            likeButton = ImageView(context).apply {
                setImageResource(R.drawable.ic_heart_filled)
                setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
            }
            likesCount = TextView(context).apply {
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.BLACK)
            }
            addView(likeButton, LayoutHelper.createLinear(20, 20))
            addView(likesCount, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 8, 0, 0, 0))
        }
        footer.addView(likePill)

        sharePill = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(pillColor)
                cornerRadius = LayoutHelper.dp(20).toFloat()
            }
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(6), LayoutHelper.dp(12), LayoutHelper.dp(6))

            val shareIcon = ImageView(context).apply {
                setImageResource(R.drawable.ic_send)
                setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
            }
            addView(shareIcon, LayoutHelper.createLinear(20, 20))
        }
        footer.addView(sharePill, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 8, 0, 0, 0))

        mainContainer.addView(footer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
    }
    
    fun setUserInfo(name: String, avatar: String?) {
        nameView.text = name
        if (!avatar.isNullOrEmpty()) {
            avatarView.load("https://blagogram.ru/avatars/$avatar") {
                placeholder(R.drawable.ic_user_placeholder)
                error(R.drawable.ic_user_placeholder)
                transformations(CircleCropTransformation())
            }
        } else {
            avatarView.setImageResource(R.drawable.ic_user_placeholder)
        }
    }

    fun setLiked(liked: Boolean) {
        likeButton.setImageResource(if (liked) R.drawable.ic_heart_filled else R.drawable.ic_like_outline)
        if (liked) {
            likeButton.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)
        } else {
            likeButton.setColorFilter(DesignEngine.Theme.colorIconPrimary, PorterDuff.Mode.SRC_IN)
        }
    }
}

class ServiceItemCell(context: Context) : FrameLayout(context) {
    init {
        val isDark = context.getSharedPreferences("app_theme", Context.MODE_PRIVATE).getBoolean("is_dark_theme", false)
        background = GradientDrawable().apply {
            setColor(if (isDark) Color.parseColor("#3D3D3F") else Color.parseColor("#BCBCBC"))
            cornerRadius = LayoutHelper.dp(12).toFloat()
        }
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }
}
