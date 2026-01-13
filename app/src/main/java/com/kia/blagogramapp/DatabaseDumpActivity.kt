package com.kia.blagogramapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.GsonBuilder
import com.kia.blagogramapp.api.ApiClient
import com.kia.blagogramapp.api.DatabaseDumpResponse
import com.kia.blagogramapp.api.DatabaseDumpResult
import kotlinx.coroutines.launch

class DatabaseDumpActivity : AppCompatActivity() {

    private lateinit var contentLayout: LinearLayout
    private lateinit var tabsLayout: LinearLayout
    private lateinit var progressBar: ProgressBar
    private var isDarkTheme: Boolean = false
    private var dumpResult: DatabaseDumpResult? = null
    private var currentTable: String? = "INFO"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = DesignEngine.initializeScreenWithFrameLayout(this)
        setContentView(root)

        val prefs = getSharedPreferences("app_theme", MODE_PRIVATE)
        isDarkTheme = prefs.getBoolean("is_dark_theme", false)

        // 1. Тулбар
        val toolbar = DesignEngine.createToolBar(
            context = this,
            title = "База данных",
            leftButtonIconRes = R.drawable.ic_back,
            leftButtonClickListener = { finish() },
            isDark = isDarkTheme
        )
        root.addView(toolbar)

        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
        // Опускаем табы значительно ниже, чтобы не перекрывали кнопки тулбара
        val offsetTop = statusBarHeight + LayoutHelper.dp(85)

        // 2. Вкладки (Табы)
        val tabsScroll = HorizontalScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
            isHorizontalScrollBarEnabled = false
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(8), LayoutHelper.dp(16), LayoutHelper.dp(8))
            clipToPadding = false
            background = GradientDrawable().apply { setColor(DesignEngine.Theme.colorBackground) }
        }
        val tabsParams = LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT)
        tabsParams.topMargin = offsetTop
        root.addView(tabsScroll, tabsParams)

        tabsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        tabsScroll.addView(tabsLayout)

        // 3. Контент
        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val scrollParams = LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT)
        scrollParams.topMargin = offsetTop + LayoutHelper.dp(60)
        root.addView(scrollView, scrollParams)

        contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(LayoutHelper.dp(16), 0, LayoutHelper.dp(16), LayoutHelper.dp(40))
        }
        scrollView.addView(contentLayout)

        progressBar = ProgressBar(this).apply { visibility = View.VISIBLE }
        root.addView(progressBar, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER))

        loadDump()
    }

    private fun loadDump() {
        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""
        lifecycleScope.launch {
            try {
                val response = ApiClient.api.getDatabaseDump(token = token)
                progressBar.visibility = View.GONE
                if (response.ok && response.result != null) {
                    dumpResult = response.result
                    setupTabs()
                    showTable("INFO")
                } else {
                    showError(response.error ?: "Ошибка доступа")
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                showError("Ошибка загрузки")
            }
        }
    }

    private fun setupTabs() {
        tabsLayout.removeAllViews()
        addTab("INFO", "Общее")
        dumpResult?.dump?.keys?.sorted()?.forEach { addTab(it, it) }
    }

    private fun addTab(id: String, title: String) {
        val tabView = TextView(this).apply {
            text = title
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(8), LayoutHelper.dp(16), LayoutHelper.dp(8))

            val bg = GradientDrawable().apply {
                cornerRadius = LayoutHelper.dp(18).toFloat()
                setColor(if (id == currentTable) DesignEngine.Theme.colorBlue else (if (isDarkTheme) 0xFF333333.toInt() else 0xFFE0E0E0.toInt()))
            }
            background = bg
            setTextColor(if (id == currentTable) Color.WHITE else DesignEngine.Theme.colorTextPrimary)

            setOnClickListener {
                if (currentTable != id) {
                    currentTable = id
                    setupTabs()
                    showTable(id)
                }
            }
        }
        val params = LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT)
        params.marginEnd = LayoutHelper.dp(8)
        tabsLayout.addView(tabView, params)
    }

    private fun showTable(id: String) {
        contentLayout.removeAllViews()
        val result = dumpResult ?: return

        if (id == "INFO") {
            contentLayout.addView(HeaderCell(this).apply { text = "СТАТИСТИКА" })
            val infoCard = ContainerCell(this)
            addDetailRow(infoCard, "Таблиц", result.tables_count.toString())
            addDetailRow(infoCard, "Сгенерировано", result.generated_at, isLast = true)
            contentLayout.addView(infoCard)
        } else {
            val rows = result.dump?.get(id) ?: emptyList()
            contentLayout.addView(HeaderCell(this).apply { text = "ТАБЛИЦА: $id" })
            val tableCard = ContainerCell(this)

            if (rows.isEmpty()) {
                tableCard.addView(TextView(this).apply {
                    text = "Пусто"
                    setPadding(LayoutHelper.dp(16), LayoutHelper.dp(20), LayoutHelper.dp(16), LayoutHelper.dp(20))
                    gravity = Gravity.CENTER
                })
            } else {
                val gson = GsonBuilder().setPrettyPrinting().create()
                rows.forEachIndexed { index, rowData ->
                    val rowLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16), LayoutHelper.dp(16))
                        setOnLongClickListener {
                            val clip = ClipData.newPlainText("db", gson.toJson(rowData))
                            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                            Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
                            true
                        }
                    }

                    rowData.forEach { (key, value) ->
                        val item = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

                        // Ключ (фиксированная ширина 100dp)
                        item.addView(TextView(this).apply {
                            text = "$key:"
                            textSize = 13f
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(DesignEngine.Theme.colorTextSecondary)
                        }, LayoutHelper.createLinear(100, LayoutHelper.WRAP_CONTENT))

                        // Значение (занимает всё остальное место)
                        item.addView(TextView(this).apply {
                            text = value?.toString() ?: "null"
                            textSize = 14f
                            // Явно задаем цвет для видимости
                            setTextColor(if (isDarkTheme) Color.WHITE else Color.BLACK)
                        }, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f))

                        rowLayout.addView(item, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0, 0, 4, 0, 0))
                    }
                    tableCard.addCell(rowLayout, index == rows.size - 1)
                }
            }
            contentLayout.addView(tableCard)
        }
    }

    private fun addDetailRow(container: ContainerCell, label: String, value: String, isLast: Boolean = false) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(LayoutHelper.dp(16), LayoutHelper.dp(14), LayoutHelper.dp(16), LayoutHelper.dp(14))
        }
        row.addView(TextView(this).apply { text = label; setTextColor(DesignEngine.Theme.colorTextSecondary) }, LayoutHelper.createLinear(0, -2, 1f))
        row.addView(TextView(this).apply { text = value; typeface = Typeface.DEFAULT_BOLD; setTextColor(if (isDarkTheme) Color.WHITE else Color.BLACK) })
        container.addCell(row, isLast)
    }

    private fun showError(message: String) {
        contentLayout.addView(TextView(this).apply {
            text = message
            setTextColor(Color.RED)
            gravity = Gravity.CENTER
            setPadding(0, LayoutHelper.dp(100), 0, 0)
        })
    }
}