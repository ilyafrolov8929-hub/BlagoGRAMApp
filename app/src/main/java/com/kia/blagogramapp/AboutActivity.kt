package com.kia.blagogramapp

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.kia.blagogramapp.utils.IconManager
import java.io.File
import java.util.*
import kotlin.math.log10
import kotlin.math.pow

/**
 * Экран "О приложении" с подробной технической информацией.
 */
class AboutActivity : AppCompatActivity() {
    private var isCurrentThemeDark: Boolean = false
    private var isCurrentUseMonet: Boolean = false

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DesignEngine.init(this)
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        val isDarkTheme = themePrefs.getBoolean("is_dark_theme", false)
        val useMonet = themePrefs.getBoolean("use_monet", false)

        isCurrentThemeDark = isDarkTheme
        isCurrentUseMonet = useMonet
        DesignEngine.Theme.apply(isDarkTheme, useMonet)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        if (!isDarkTheme) {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        window.statusBarColor = Color.TRANSPARENT

        val rootLayout = FrameLayout(this)
        rootLayout.setBackgroundColor(DesignEngine.Theme.colorBackground)
        setContentView(rootLayout)

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
            setImageResource(IconManager.getSeasonalIconRes())
        }
        val logoParams = LinearLayout.LayoutParams(LayoutHelper.dp(90), LayoutHelper.dp(90)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = LayoutHelper.dp(12)
        }
        contentLayout.addView(logoView, logoParams)

        // --- Название (Центрируем) ---
        val appNameView = TextView(this).apply {
            text = "BlagoGRAM"
            textSize = 28f
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            typeface = Typeface.DEFAULT_BOLD
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

        // 2. ПАМЯТЬ
        contentLayout.addView(SpacerCell(this, LayoutHelper.dp(24)))
        addSectionHeader(contentLayout, "ИСПОЛЬЗОВАНИЕ ПАМЯТИ")
        val memoryCard = ContainerCell(this)
        addInfoRow(memoryCard, "Размер кэша", getCacheSize())
        addInfoRow(memoryCard, "Размер приложения", getAppSize())
        addInfoRow(memoryCard, "Свободно в системе", getFreeInternalSpace(), isLast = true)
        contentLayout.addView(memoryCard, getCardParams())

        // 3. СИСТЕМА
        contentLayout.addView(SpacerCell(this, LayoutHelper.dp(24)))
        addSectionHeader(contentLayout, "СИСТЕМНЫЕ ДАННЫЕ")
        val systemCard = ContainerCell(this)
        addInfoRow(systemCard, "Android версия", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        addInfoRow(systemCard, "SDK Min / Target", "${applicationInfo.minSdkVersion} / ${applicationInfo.targetSdkVersion}")
        addInfoRow(systemCard, "Устройство", "${Build.MANUFACTURER} ${Build.MODEL}")
        addInfoRow(systemCard, "ОЗУ всего", getTotalRAM(), isLast = true)
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
        val statusBarHeight = getStatusBarHeight()
        val toolbarLayout = RelativeLayout(this).apply {
            setPadding(LayoutHelper.dp(16), statusBarHeight + LayoutHelper.dp(10), LayoutHelper.dp(16), LayoutHelper.dp(10))
        }
        
        val backBtn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_stat_pill)
            backgroundTintList = android.content.res.ColorStateList.valueOf(if (isCurrentThemeDark) Color.parseColor("#333333") else Color.parseColor("#E0E0E0"))
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(6), LayoutHelper.dp(12), LayoutHelper.dp(6))
            setOnClickListener { finish() }
        }
        backBtn.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_back)
            setColorFilter(if (isCurrentThemeDark) Color.WHITE else Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(LayoutHelper.dp(20), LayoutHelper.dp(20))
        })
        
        val titleView = TextView(this).apply {
            text = "О приложении"
            textSize = 20f
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val toolbarContent = RelativeLayout(this).apply {
            layoutParams = RelativeLayout.LayoutParams(-1, -2)
        }
        toolbarContent.addView(backBtn, RelativeLayout.LayoutParams(-2, -2).apply { addRule(RelativeLayout.ALIGN_PARENT_START); addRule(RelativeLayout.CENTER_VERTICAL) })
        toolbarContent.addView(titleView, RelativeLayout.LayoutParams(-2, -2).apply { addRule(RelativeLayout.CENTER_IN_PARENT) })
        toolbarLayout.addView(toolbarContent)
        
        root.addView(toolbarLayout, FrameLayout.LayoutParams(-1, -2, Gravity.TOP))
    }

    private fun getCardParams() = LinearLayout.LayoutParams(-1, -2).apply { leftMargin = LayoutHelper.dp(16); rightMargin = LayoutHelper.dp(16) }
    private fun getStatusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 0
    }

    private fun getCacheSize(): String {
        var s = getFolderSize(cacheDir) + getFolderSize(externalCacheDir)
        return formatSize(s)
    }

    private fun getAppSize(): String {
        var s: Long = 0
        try {
            s = File(packageManager.getApplicationInfo(packageName, 0).publicSourceDir).length() + getFolderSize(filesDir)
        } catch (e: Exception) {}
        return formatSize(s)
    }

    private fun getFreeInternalSpace() = formatSize(StatFs(Environment.getDataDirectory().path).let { it.blockSizeLong * it.availableBlocksLong })

    private fun getTotalRAM(): String {
        val mi = ActivityManager.MemoryInfo()
        (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
        return formatSize(mi.totalMem)
    }

    private fun getFolderSize(f: File?): Long {
        if (f == null || !f.exists()) return 0
        if (!f.isDirectory) return f.length()
        return f.listFiles()?.sumOf { getFolderSize(it) } ?: 0
    }

    private fun formatSize(size: Long): String {
        if (size <= 0) return "0 Б"
        val units = arrayOf("Б", "КБ", "МБ", "ГБ", "ТБ")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return String.format(Locale.getDefault(), "%.2f %s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
    }

    override fun onResume() {
        super.onResume()
        val p = getSharedPreferences("app_theme", MODE_PRIVATE)
        if (p.getBoolean("is_dark_theme", false) != isCurrentThemeDark || p.getBoolean("use_monet", false) != isCurrentUseMonet) recreate()
    }
}
