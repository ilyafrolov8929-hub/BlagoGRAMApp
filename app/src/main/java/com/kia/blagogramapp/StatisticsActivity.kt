package com.kia.blagogramapp

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.AnticipateOvershootInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import java.io.File
import kotlin.concurrent.thread

/**
 * Исправленный экран статистики с «живой» анимацией пентаграммы (хоп туда, хоп обратно).
 */
class StatisticsActivity : AppCompatActivity() {
    private var isCurrentThemeDark: Boolean = false
    
    private lateinit var donutChart: DonutChartView
    private lateinit var totalSizeText: TextView
    private lateinit var totalSizeUnitText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var progressFill: View
    private lateinit var listCard: ContainerCell
    private lateinit var clearBtn: Button

    private var currentStats = StorageStats()
    private val selectedCategories = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Инициализируем экран и получаем готовый корневой FrameLayout
        val rootLayout = DesignEngine.initializeScreenWithFrameLayout(this)
        setContentView(rootLayout)

// 2. Обновляем локальную переменную темы, так как она нужна в коде этого Activity
        val themePrefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        isCurrentThemeDark = themePrefs.getBoolean("is_dark_theme", false)

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, getStatusBarHeight(), 0, LayoutHelper.dp(40))
        }
        scrollView.addView(contentLayout)

        // --- 1. Круговая диаграмма ---
        val chartContainer = FrameLayout(this)
        donutChart = DonutChartView(this)
        // Увеличиваем верхний отступ с 20 до 40, чтобы опустить диаграмму
        chartContainer.addView(donutChart, LayoutHelper.createFrame(240, 240, Gravity.CENTER, 0, 60, 0, 60))
        val centerText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (isCurrentThemeDark) 0xFF121212.toInt() else 0xFF222222.toInt())
            }
            val p = LayoutHelper.dp(44)
            setPadding(p, p, p, p)
        }
        totalSizeText = TextView(this).apply {
            text = "0.0"
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        totalSizeUnitText = TextView(this).apply {
            text = "MB"
            gravity = Gravity.CENTER_HORIZONTAL
            textSize = 14f
            setTextColor(Color.parseColor("#88FFFFFF"))
        }
        centerText.addView(totalSizeText)
        centerText.addView(totalSizeUnitText)
        chartContainer.addView(centerText, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER))

        contentLayout.addView(chartContainer)

        // --- 2. Заголовок ---
        contentLayout.addView(TextView(this).apply {
            text = "Использование памяти"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(DesignEngine.Theme.colorTextPrimary)
            gravity = Gravity.CENTER
            setPadding(0, LayoutHelper.dp(20), 0, 0)
        })

        descriptionText = TextView(this).apply {
            text = "Загрузка данных..."
            textSize = 14f
            setTextColor(DesignEngine.Theme.colorTextSecondary)
            gravity = Gravity.CENTER
            setPadding(0, LayoutHelper.dp(8), 0, LayoutHelper.dp(16))
        }
        contentLayout.addView(descriptionText)

        // Горизонтальный прогресс
        val progressContainer = FrameLayout(this)
        val progressBar = View(this).apply {
            background = GradientDrawable().apply {
                cornerRadius = LayoutHelper.dp(2).toFloat()
                setColor(if (isCurrentThemeDark) 0xFF333333.toInt() else 0xFFE0E0E0.toInt())
            }
        }
        progressFill = View(this).apply {
            background = GradientDrawable().apply {
                cornerRadius = LayoutHelper.dp(2).toFloat()
                setColor(Color.parseColor("#2196F3"))
            }
            alpha = 0f
        }
        progressContainer.addView(progressBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 4, Gravity.CENTER_VERTICAL))
        progressContainer.addView(progressFill, LayoutHelper.createFrame(0, 4, Gravity.CENTER_VERTICAL))
        contentLayout.addView(progressContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 20, 0f, 0, 40, 0, 40, 0))

        // --- 3. Список категорий ---
        listCard = ContainerCell(this).apply {
            setPadding(0, LayoutHelper.dp(8), 0, LayoutHelper.dp(8))
        }
        contentLayout.addView(listCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 16, 24, 16, 0))

        // Кнопка очистки
        clearBtn = Button(this).apply {
            text = "Очистить"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = LayoutHelper.dp(10).toFloat()
                setColor(Color.parseColor("#40A7E3"))
            }
            setOnClickListener { clearSelectedData() }
        }
        contentLayout.addView(clearBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50, 0f, 0, 32, 16, 32, 8))

        // Футер
        contentLayout.addView(TextView(this).apply {
            text = "Все медиа останутся в облаке Lume, при необходимости Вы сможете загрузить их снова."
            textSize = 13f
            setTextColor(DesignEngine.Theme.colorTextSecondary)
            gravity = Gravity.CENTER
            setPadding(LayoutHelper.dp(32), LayoutHelper.dp(16), LayoutHelper.dp(32), 0)
        })

        rootLayout.addView(scrollView)

        val toolbar = DesignEngine.createToolBar(
            context = this,
            title = "Данные и память",
            leftButtonIconRes = R.drawable.ic_back,
            isDark = isCurrentThemeDark,
            leftButtonClickListener = { finish() }
        )
        rootLayout.addView(toolbar, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP))


        loadStatistics()
    }

    private fun loadStatistics() {
        thread {
            currentStats = calculateStorageStats()
            runOnUiThread {
                if (currentStats.videoSize > 0) selectedCategories.add("Видео")
                if (currentStats.photoSize > 0) selectedCategories.add("Фото")
                if (currentStats.filesSize > 0) selectedCategories.add("Файлы")
                if (currentStats.otherSize > 0) selectedCategories.add("Другое")
                updateUI()
            }
        }
    }

    private fun updateUI() {
        var selectedTotal: Long = 0
        if (selectedCategories.contains("Видео")) selectedTotal += currentStats.videoSize
        if (selectedCategories.contains("Фото")) selectedTotal += currentStats.photoSize
        if (selectedCategories.contains("Файлы")) selectedTotal += currentStats.filesSize
        if (selectedCategories.contains("Другое")) selectedTotal += currentStats.otherSize

        val formattedTotal = formatSize(selectedTotal)
        totalSizeText.text = formattedTotal.split(" ")[0]
        totalSizeUnitText.text = formattedTotal.split(" ")[1]

        val appTotalSize = currentStats.videoSize + currentStats.photoSize + currentStats.filesSize + currentStats.otherSize
        val deviceTotal = getDeviceTotalSize()
        val percentOfDevice = if (deviceTotal > 0) (appTotalSize * 100f / deviceTotal).toInt() else 0
        descriptionText.text = "Lume занимает $percentOfDevice% места на устройстве."

        // Обновляем прогресс-бар
        progressFill.alpha = 1f
        val screenWidth = resources.displayMetrics.widthPixels - LayoutHelper.dp(80)
        progressFill.layoutParams = (progressFill.layoutParams as FrameLayout.LayoutParams).apply {
            width = (screenWidth * (percentOfDevice.coerceIn(1, 100) / 100f)).toInt()
        }

        // Данные для анимированного чарта (всегда передаем все 4 типа, но с 0%, если не выбрано)
        val sections = mutableListOf<DonutSection>()
        val totalForChart = if (selectedTotal > 0) selectedTotal else 1 // Избегаем деления на 0

        sections.add(DonutSection("Видео", if (selectedCategories.contains("Видео")) currentStats.videoSize * 100f / totalForChart else 0f, Color.parseColor("#2196F3")))
        sections.add(DonutSection("Фото", if (selectedCategories.contains("Фото")) currentStats.photoSize * 100f / totalForChart else 0f, Color.parseColor("#4CAF50")))
        sections.add(DonutSection("Файлы", if (selectedCategories.contains("Файлы")) currentStats.filesSize * 100f / totalForChart else 0f, Color.parseColor("#FFC107")))
        sections.add(DonutSection("Другое", if (selectedCategories.contains("Другое")) currentStats.otherSize * 100f / totalForChart else 0f, Color.parseColor("#00BCD4")))

        donutChart.animateTo(sections)

        // Обновление списка
        listCard.removeAllViews()
        addSelectionRow(listCard, "Видео", currentStats.videoSize, appTotalSize, Color.parseColor("#2196F3"))
        addSelectionRow(listCard, "Фото", currentStats.photoSize, appTotalSize, Color.parseColor("#4CAF50"))
        addSelectionRow(listCard, "Файлы", currentStats.filesSize, appTotalSize, Color.parseColor("#FFC107"))
        addSelectionRow(listCard, "Другое", currentStats.otherSize, appTotalSize, Color.parseColor("#00BCD4"), isLast = true)

        clearBtn.text = if (selectedTotal > 0) "Очистить выбранное $formattedTotal" else "Ничего не выбрано"
        clearBtn.isEnabled = selectedTotal > 0
        clearBtn.alpha = if (selectedTotal > 0) 1f else 0.5f
    }

    private fun addSelectionRow(container: ContainerCell, title: String, size: Long, total: Long, color: Int, isLast: Boolean = false) {
        if (size <= 0) return
        val frame = FrameLayout(this).apply {
            val outValue = android.util.TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            foreground = getDrawable(outValue.resourceId)
            isClickable = true
            setOnClickListener {
                if (selectedCategories.contains(title)) selectedCategories.remove(title)
                else selectedCategories.add(title)
                updateUI()
            }
        }

        val isSelected = selectedCategories.contains(title)
        val checkView = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (isSelected) setColor(color) else setStroke(LayoutHelper.dp(2), color)
            }
        }
        frame.addView(checkView, LayoutHelper.createFrame(22, 22, Gravity.CENTER_VERTICAL or Gravity.START, 16, 0, 0, 0))

        if (isSelected) {
            frame.addView(ImageView(this).apply {
                setImageResource(R.drawable.ic_check)
                setColorFilter(Color.WHITE)
                setPadding(LayoutHelper.dp(4), LayoutHelper.dp(4), LayoutHelper.dp(4), LayoutHelper.dp(4))
            }, LayoutHelper.createFrame(22, 22, Gravity.CENTER_VERTICAL or Gravity.START, 16, 0, 0, 0))
        }

        val percent = calculatePercent(size, total)
        val titleView = TextView(this).apply {
            text = "$title $percent"
            textSize = 16f
            setTextColor(DesignEngine.Theme.colorTextPrimary)
        }
        frame.addView(titleView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.START, 54, 0, 0, 0))

        val sizeView = TextView(this).apply {
            text = formatSize(size)
            textSize = 15f
            setTextColor(DesignEngine.Theme.colorTextSecondary)
        }
        frame.addView(sizeView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.END, 0, 0, 16, 0))

        container.addCell(frame, isLast)
    }

    private fun clearSelectedData() {
        thread {
            val scanDirs = listOfNotNull(cacheDir, filesDir, externalCacheDir, getExternalFilesDir(null))
            scanDirs.forEach { deleteSelectedFiles(it) }
            runOnUiThread {
                Toast.makeText(this, "Выбранные данные удалены", Toast.LENGTH_SHORT).show()
                selectedCategories.clear()
                loadStatistics()
            }
        }
    }

    private fun deleteSelectedFiles(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteSelectedFiles(it) }
        } else {
            val ext = file.extension.lowercase()
            val category = when {
                ext in listOf("mp4", "mkv", "mov", "avi") -> "Видео"
                ext in listOf("jpg", "jpeg", "png", "webp", "gif") -> "Фото"
                ext in listOf("pdf", "doc", "docx", "txt", "zip", "rar") -> "Файлы"
                else -> "Другое"
            }
            if (selectedCategories.contains(category)) {
                if (file.name != "datastore" && !file.absolutePath.contains("shared_prefs")) file.delete()
            }
        }
    }

    private fun calculateStorageStats(): StorageStats {
        val stats = StorageStats()
        val scanDirs = listOfNotNull(cacheDir, filesDir, externalCacheDir, getExternalFilesDir(null))
        scanDirs.forEach { scanFile(it, stats) }
        return stats
    }

    private fun scanFile(file: File, stats: StorageStats) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { scanFile(it, stats) }
        } else {
            val size = file.length()
            val ext = file.extension.lowercase()
            when {
                ext in listOf("mp4", "mkv", "mov", "avi") -> stats.videoSize += size
                ext in listOf("jpg", "jpeg", "png", "webp", "gif") -> stats.photoSize += size
                ext in listOf("pdf", "doc", "docx", "txt", "zip", "rar") -> stats.filesSize += size
                else -> stats.otherSize += size
            }
        }
    }

    private fun calculatePercent(part: Long, total: Long): String {
        if (total <= 0) return "0%"
        val p = (part * 100f / total).toInt()
        return if (p < 1 && part > 0) "<1%" else "$p%"
    }

    private fun getDeviceTotalSize(): Long {
        val stat = StatFs(Environment.getDataDirectory().path)
        return stat.blockCountLong * stat.blockSizeLong
    }

    private fun formatSize(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            else -> String.format("%.1f MB", mb)
        }
    }

    private fun setupToolbar(root: FrameLayout) {
        // Основной контейнер тулбара
        val toolbarLayout = RelativeLayout(this).apply {
            setPadding(LayoutHelper.dp(16), getStatusBarHeight() + LayoutHelper.dp(10), LayoutHelper.dp(16), LayoutHelper.dp(10))
        }

        // Внутренний контейнер для центрирования
        val toolbarContent = RelativeLayout(this).apply {
            layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        }

        // Кнопка "назад" со стилем
        val backBtn = LinearLayout(this).apply {
            setBackgroundResource(R.drawable.bg_stat_pill)
            backgroundTintList = android.content.res.ColorStateList.valueOf(if (isCurrentThemeDark) Color.parseColor("#333333") else Color.parseColor("#E0E0E0"))
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(6), LayoutHelper.dp(12), LayoutHelper.dp(6))
            setOnClickListener { finish() }
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_back)
                setColorFilter(if (isCurrentThemeDark) Color.WHITE else Color.BLACK)
                layoutParams = LinearLayout.LayoutParams(LayoutHelper.dp(20), LayoutHelper.dp(20))
            })
        }

        val titleView = LinearLayout(this).apply {
            setBackgroundResource(R.drawable.bg_stat_pill)
            backgroundTintList = android.content.res.ColorStateList.valueOf(if (isCurrentThemeDark) Color.parseColor("#333333") else Color.parseColor("#E0E0E0"))
            setPadding(LayoutHelper.dp(12), LayoutHelper.dp(6), LayoutHelper.dp(12), LayoutHelper.dp(6))
            gravity = Gravity.CENTER // Центрируем текст внутри
            addView(TextView(context).apply {
                text = "Статистика" // Добавляем заголовок
                textSize = 20f
                setTextColor(DesignEngine.Theme.colorTextPrimary)
                typeface = Typeface.DEFAULT_BOLD
            })
        }

        // Добавляем кнопку и заголовок во внутренний контейнер
        toolbarContent.addView(backBtn, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_START)
            addRule(RelativeLayout.CENTER_VERTICAL)
        })
        toolbarContent.addView(titleView, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.CENTER_IN_PARENT)
        })

        // Добавляем внутренний контейнер в основной тулбар
        toolbarLayout.addView(toolbarContent)

        // Добавляем тулбар на экран
        root.addView(toolbarLayout, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP))
    }

    private fun getStatusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 0
    }

    class StorageStats {
        var videoSize: Long = 0
        var photoSize: Long = 0
        var filesSize: Long = 0
        var otherSize: Long = 0
    }

    data class DonutSection(val id: String, val targetPercent: Float, val color: Int, var currentPercent: Float = 0f)

    class DonutChartView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
    ) : View(context, attrs, defStyleAttr) {
        private var sections = mutableListOf<DonutSection>()
        private var animator: ValueAnimator? = null
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = LayoutHelper.dp(48).toFloat()
            strokeCap = Paint.Cap.BUTT
        }

        fun animateTo(newSections: List<DonutSection>) {
            animator?.cancel()
            val oldData = sections.associateBy { it.id }
            val merged = newSections.map { target ->
                target.copy(currentPercent = oldData[target.id]?.currentPercent ?: 0f)
            }
            
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 500
                // «Живой» интерполятор для эффекта «хоп»
                interpolator = AnticipateOvershootInterpolator(0.8f)
                addUpdateListener { anim ->
                    val fraction = anim.animatedValue as Float
                    merged.forEach { s ->
                        val start = oldData[s.id]?.currentPercent ?: 0f
                        s.currentPercent = start + (s.targetPercent - start) * fraction
                    }
                    sections = merged.toMutableList()
                    invalidate()
                }
                start()
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            // Рисуем фоновый серый круг, если ничего не выбрано
            paint.color = if (DesignEngine.Theme.colorBackground == Color.WHITE) 0xFFEEEEEE.toInt() else 0xFF282828.toInt()
            val rect = RectF(
                paint.strokeWidth / 2 + LayoutHelper.dp(8),
                paint.strokeWidth / 2 + LayoutHelper.dp(8),
                width - paint.strokeWidth / 2 - LayoutHelper.dp(8),
                height - paint.strokeWidth / 2 - LayoutHelper.dp(8)
            )
            canvas.drawCircle(width / 2f, height / 2f, (width - paint.strokeWidth) / 2f - LayoutHelper.dp(8), paint)

            var startAngle = -90f
            sections.forEach { section ->
                paint.color = section.color
                val sweepAngle = (section.currentPercent / 100f) * 360f
                if (Math.abs(sweepAngle) > 0.1f) {
                    canvas.drawArc(rect, startAngle, sweepAngle, false, paint)
                }
                startAngle += sweepAngle
            }
        }
    }
}
