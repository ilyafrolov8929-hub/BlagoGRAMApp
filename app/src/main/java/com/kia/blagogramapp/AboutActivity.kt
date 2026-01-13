package com.kia.blagogramapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.kia.blagogramapp.utils.DevModeManager
import java.util.*

/**
 * Экран "О приложении" с подробной технической информацией.
 */
class AboutActivity : AppCompatActivity() {
    private var isCurrentThemeDark: Boolean = false
    private var isCurrentUseMonet: Boolean = false

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Инициализируем экран и получаем готовый корневой FrameLayout
        val rootLayout = DesignEngine.initializeScreenWithFrameLayout(this)
        setContentView(rootLayout)

        // 2. Обновляем локальную переменную темы, так как она нужна в коде этого Activity
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        isCurrentThemeDark = themePrefs.getBoolean("is_dark_theme", false)
        isCurrentUseMonet = themePrefs.getBoolean("use_monet", false)

        val scrollView = ScrollView(this)
        scrollView.isFillViewport = true
        scrollView.overScrollMode = View.OVER_SCROLL_NEVER
        
        val contentLayout = LinearLayout(this)
        contentLayout.orientation = LinearLayout.VERTICAL
        contentLayout.setPadding(0, 0, 0, LayoutHelper.dp(40))
        scrollView.addView(contentLayout)
        
        // Отступ под тулбар
        contentLayout.addView(SpacerCell(this, LayoutHelper.dp(50)))

        // --- Логотип (Центрируем) ---
        val logoView = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
        }
        val logoParams = LinearLayout.LayoutParams(LayoutHelper.dp(90), LayoutHelper.dp(90)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = LayoutHelper.dp(12)
        }
        contentLayout.addView(logoView, logoParams)

        // --- Название (Центрируем) ---
        val appNameView = TextView(this).apply {
            text = "Lume" // Убираем старое название, оставляем новое
            textSize = 32f // Мультяшные шрифты лучше смотрятся крупнее
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            // Используем системный закругленный шрифт
            typeface = Typeface.create("sans-serif-rounded", Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        contentLayout.addView(appNameView, LinearLayout.LayoutParams(-1, -2))

        // --- Версия (Центрируем) ---
        val versionTextView = TextView(this).apply {
            textSize = 16f
            setTextColor(DesignEngine.Theme.colorTextSecondary)
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, LayoutHelper.dp(4), 0, LayoutHelper.dp(40))
            
            try {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                val vName = packageInfo.versionName ?: "0.0.0"
                val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else packageInfo.versionCode
                text = "Версия $vName ($vCode)"
            } catch (e: Exception) {
                text = "Версия 1.0.0"
            }

            // Секретный вход в дамп базы
            setOnLongClickListener {
                if (DevModeManager.isEnabled(this@AboutActivity)) {
                    startActivity(Intent(this@AboutActivity, DatabaseDumpActivity::class.java))
                    true
                } else {
                    false
                }
            }
        }
        contentLayout.addView(versionTextView, LinearLayout.LayoutParams(-1, -2))

        // --- Секции ---
        
        // 1. ИНФОРМАЦИЯ
        addSectionHeader(contentLayout, "ИНФОРМАЦИЯ")
        val mainCard = ContainerCell(this)
        addInfoRow(mainCard, "Имя пакета", packageName)
        addInfoRow(mainCard, "Сайт проекта", "blagogram.ru", true) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://blagogram.ru")))
        }
        addInfoRow(mainCard, "Разработчик", "BlagoGRAM Team", isLast = true)
        contentLayout.addView(mainCard, getCardParams())

        // 2. СИСТЕМА
        contentLayout.addView(SpacerCell(this, LayoutHelper.dp(24)))
        addSectionHeader(contentLayout, "СИСТЕМНЫЕ ДАННЫЕ")
        val systemCard = ContainerCell(this)
        addInfoRow(systemCard, "Android версия", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        addInfoRow(systemCard, "SDK Min / Target", "${applicationInfo.minSdkVersion} / ${applicationInfo.targetSdkVersion}")
        addInfoRow(systemCard, "Устройство", "${Build.MANUFACTURER} ${Build.MODEL}", isLast = true)
        contentLayout.addView(systemCard, getCardParams())

        // Copyright
        val copyrightView = TextView(this).apply {
            val year = Calendar.getInstance().get(Calendar.YEAR)
            text = "© $year BlagoGRAM Inc.\nСделано с любовью для Kia"
            textSize = 13f
            setLineSpacing(0f, 1.2f)
            setTextColor(DesignEngine.Theme.colorTextSecondary)
            gravity = Gravity.CENTER
            setPadding(0, LayoutHelper.dp(40), 0, LayoutHelper.dp(20))
        }
        contentLayout.addView(copyrightView, LinearLayout.LayoutParams(-1, -2))

        rootLayout.addView(scrollView)

        setupToolbar(rootLayout)
    }

    private fun addSectionHeader(container: LinearLayout, title: String) {
        val header = TextView(this).apply {
            text = title
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.Theme.colorHeader)
            setPadding(LayoutHelper.dp(32), 0, LayoutHelper.dp(16), LayoutHelper.dp(8))
        }
        container.addView(header)
    }

    private fun addInfoRow(card: ContainerCell, label: String, value: String, isLink: Boolean = false, isLast: Boolean = false, onClick: (() -> Unit)? = null) {
        val frame = FrameLayout(this)
        if (onClick != null) {
            val outValue = android.util.TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            frame.foreground = getDrawable(outValue.resourceId)
            frame.isClickable = true
            frame.setOnClickListener { onClick() }
        }
        
        val labelView = TextView(this).apply {
            text = label
            textSize = 16f
            setTextColor(DesignEngine.Theme.colorTextPrimary)
        }
        frame.addView(labelView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.START, 16, 0, 0, 0))
        
        val valueView = TextView(this).apply {
            text = value
            textSize = 15f
            setTextColor(if (isLink) Color.parseColor("#007AFF") else DesignEngine.Theme.colorTextSecondary)
            gravity = Gravity.END
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        frame.addView(valueView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.END, 16, 0, 16, 0))
        
        card.addView(frame, LinearLayout.LayoutParams(-1, LayoutHelper.dp(54)))
        
        if (!isLast) {
            val divider = View(this).apply { setBackgroundColor(DesignEngine.COLOR_DIVIDER) }
            card.addView(divider, LayoutHelper.createLinear(-1, 1, 0f, 0, 16, 0, 0, 0))
        }
    }

    private fun setupToolbar(root: FrameLayout) {

        val toolbar = DesignEngine.createToolBar(
            context = this,
            title = "О приложении",
            leftButtonIconRes = R.drawable.ic_back,
            leftButtonClickListener = { finish() },
            isDark = isCurrentThemeDark
        )
        root.addView(toolbar)
    }

    private fun getCardParams() = LinearLayout.LayoutParams(-1, -2).apply { leftMargin = LayoutHelper.dp(16); rightMargin = LayoutHelper.dp(16) }
    private fun getStatusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 0
    }

    override fun onResume() {
        super.onResume()
        val p = getSharedPreferences("app_theme", MODE_PRIVATE)
        if (p.getBoolean("is_dark_theme", false) != isCurrentThemeDark || p.getBoolean("use_monet", false) != isCurrentUseMonet) recreate()
    }
}
